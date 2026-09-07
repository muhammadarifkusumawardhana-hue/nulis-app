package com.personalnotes.ui.editor

import androidx.lifecycle.*
import com.personalnotes.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime

data class EditorState(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val folderId: Long = -1L,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isSaved: Boolean = true,
    val isLoading: Boolean = true,
    val isPreview: Boolean = false,
    val wordCount: Int = 0,
    val allTags: List<String> = emptyList()
)

class EditorViewModel(
    private val db: AppDatabase,
    private val noteId: Long,       // -1L = catatan baru
    private val defaultFolderId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        loadNote()
        loadTags()
    }

    private fun loadNote() {
        viewModelScope.launch {
            if (noteId > 0L) {
                val entity = db.noteDao().getById(noteId)
                if (entity != null) {
                    _state.update {
                        it.copy(
                            id = entity.id,
                            title = entity.title,
                            content = entity.content,
                            folderId = entity.folderId,
                            tags = entity.tags(),
                            isPinned = entity.isPinned,
                            isLoading = false,
                            wordCount = countWords(entity.content)
                        )
                    }
                    return@launch
                }
            }
            // Catatan baru
            _state.update { it.copy(folderId = defaultFolderId, isLoading = false) }
        }
    }

    private fun loadTags() {
        db.tagDao().getAll()
            .onEach { tags -> _state.update { it.copy(allTags = tags.map { t -> t.name }) } }
            .launchIn(viewModelScope)
    }

    fun onTitleChange(v: String) {
        _state.update { it.copy(title = v, isSaved = false) }
        scheduleAutoSave()
    }

    fun onContentChange(v: String) {
        _state.update { it.copy(content = v, isSaved = false, wordCount = countWords(v)) }
        scheduleAutoSave()
    }

    fun togglePreview() {
        _state.update { it.copy(isPreview = !it.isPreview) }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank() || trimmed in _state.value.tags) return
        _state.update { it.copy(tags = it.tags + trimmed, isSaved = false) }
        viewModelScope.launch { db.tagDao().insert(TagEntity(name = trimmed)) }
        scheduleAutoSave()
    }

    fun removeTag(tag: String) {
        _state.update { it.copy(tags = _state.value.tags - tag, isSaved = false) }
        scheduleAutoSave()
    }

    fun insertSnippet(snippet: String) {
        val current = _state.value.content
        onContentChange("$current$snippet")
    }

    // Simpan segera (dipanggil saat back/close)
    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch { doSave() }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            doSave()
        }
    }

    private suspend fun doSave() {
        val s = _state.value
        if (s.title.isBlank() && s.content.isBlank()) return

        val now = LocalDateTime.now().toString()
        if (s.id == 0L) {
            // INSERT baru
            val newId = db.noteDao().insert(
                NoteEntity(
                    title = s.title,
                    content = s.content,
                    folderId = s.folderId,
                    tagsJson = s.tags.toJson(),
                    isPinned = s.isPinned,
                    updatedAt = now
                )
            )
            _state.update { it.copy(id = newId, isSaved = true) }
        } else {
            // UPDATE
            db.noteDao().update(
                NoteEntity(
                    id = s.id,
                    title = s.title,
                    content = s.content,
                    folderId = s.folderId,
                    tagsJson = s.tags.toJson(),
                    isPinned = s.isPinned,
                    updatedAt = now
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    private fun countWords(text: String): Int =
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size

    fun exportToHtml(context: android.content.Context) {
        val s = _state.value
        val html = buildHtml(s.title, s.content, s.tags)
        // Gunakan cache dir — tidak butuh permission di semua versi Android
        val dir = java.io.File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = java.io.File(dir, "${safeName(s.title)}.html")
        file.writeText(html)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan / Simpan HTML"))
    }

    private fun buildHtml(title: String, content: String, tags: List<String>): String {
        val body = content
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
            .replace(Regex("`(.+?)`"), "<code>$1</code>")
            .replace(Regex("^> (.+)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")
            .replace(Regex("^- (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
            .replace(Regex("\\[\\[(.+?)\\]\\]"), "<a href='#'>$1</a>")
            .replace("\n\n", "</p><p>")
        return """
<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8">
<title>${title.replace("<","&lt;")}</title>
<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.6}
code{background:#f4f4f4;padding:2px 6px;border-radius:3px}
blockquote{border-left:4px solid #6650A4;margin:0;padding-left:16px;color:#666}</style>
</head><body>
<h1>${title.replace("<","&lt;")}</h1>
${if(tags.isNotEmpty()) "<p>${tags.joinToString(" ") { "<code>#$it</code>" }}</p>" else ""}
<p>$body</p>
</body></html>""".trimIndent()
    }

    private fun safeName(name: String) =
        name.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().replace(" ", "_")
            .take(40).ifBlank { "catatan_${System.currentTimeMillis()}" }
}

class EditorViewModelFactory(
    private val db: AppDatabase,
    private val noteId: Long,
    private val defaultFolderId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EditorViewModel(db, noteId, defaultFolderId) as T
    }
}
