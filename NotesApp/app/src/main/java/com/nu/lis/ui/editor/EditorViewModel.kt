package com.nu.lis.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import com.nu.lis.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime

// ── Editor State ───────────────────────────────────────────

data class EditorState(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val contentValue: TextFieldValue = TextFieldValue(""),
    val folderId: Long = -1L,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false, // Status apakah catatan ada di Trash
    val isLocked: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = true,
    val isPreview: Boolean = false,
    val wordCount: Int = 0,
    val allTags: List<String> = emptyList(),
    val isSaved: Boolean = true,
    val isImageMenuExpanded: Boolean = false
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
                            contentValue = TextFieldValue(entity.content),
                            folderId = entity.folderId,
                            tags = entity.tags().map { it.trim().lowercase() }.distinct(),
                            isPinned = entity.isPinned,
                            isArchived = entity.isArchived,
                            isDeleted = entity.isDeleted,
                            isLocked = entity.isLocked,
                            isAuthenticated = false,
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
            .onEach { tags -> 
                val uniqueNames = tags.map { it.name.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .distinct()
                _state.update { it.copy(allTags = uniqueNames) } 
            }
            .launchIn(viewModelScope)
    }

    fun onTitleChange(v: String) {
        if (_state.value.isDeleted) return // Jangan edit jika di trash
        _state.update { it.copy(title = v, isSaved = false) }
        scheduleAutoSave()
    }

    fun onContentChange(v: TextFieldValue) {
        if (_state.value.isDeleted) return // Jangan edit jika di trash
        _state.update { it.copy(contentValue = v, content = v.text, isSaved = false, wordCount = countWords(v.text)) }
        scheduleAutoSave()
    }

    fun togglePreview() {
        _state.update { it.copy(isPreview = !it.isPreview) }
    }

    fun setAuthenticated(auth: Boolean) {
        _state.update { it.copy(isAuthenticated = auth) }
    }

    fun toggleLock() {
        if (_state.value.isDeleted) return
        val newLock = !_state.value.isLocked
        _state.update { it.copy(isLocked = newLock, isAuthenticated = true, isSaved = false) }
        scheduleAutoSave()
    }

    fun addTag(tag: String) {
        if (_state.value.isDeleted) return
        val trimmed = tag.trim().lowercase().removePrefix("#")
        if (trimmed.isBlank() || trimmed in _state.value.tags) return
        _state.update { it.copy(tags = (it.tags + trimmed).distinct(), isSaved = false) }
        viewModelScope.launch { db.tagDao().insert(TagEntity(name = trimmed)) }
        scheduleAutoSave()
    }

    fun removeTag(tag: String) {
        if (_state.value.isDeleted) return
        _state.update { it.copy(tags = _state.value.tags - tag, isSaved = false) }
        scheduleAutoSave()
    }

    fun renameTag(oldTag: String, newTag: String) {
        if (_state.value.isDeleted) return
        val trimmed = newTag.trim().lowercase().removePrefix("#")
        if (trimmed.isBlank() || trimmed == oldTag) return
        
        _state.update { state ->
            val updatedTags = state.tags.map { if (it == oldTag) trimmed else it }.distinct()
            state.copy(tags = updatedTags, isSaved = false)
        }
        viewModelScope.launch { db.tagDao().insert(TagEntity(name = trimmed)) }
        scheduleAutoSave()
    }

    fun insertSnippet(snippet: String) {
        if (_state.value.isDeleted) return
        val current = _state.value.contentValue
        val selection = current.selection
        val text = current.text
        
        val start = selection.min
        val end = selection.max
        
        val newContentValue = if (selection.length > 0) {
            // Jika ada seleksi, bungkus dengan snippet
            val selectedText = text.substring(start, end)
            
            // Logika pembungkus (wrapping) vs prefix
            val isWrapping = snippet in listOf("**", "*", "`", "~~", "==", "[", "(")
            
            val newText = if (isWrapping) {
                val endSymbol = when(snippet) {
                    "[" -> "]"
                    "(" -> ")"
                    else -> snippet
                }
                text.replaceRange(start, end, "$snippet$selectedText$endSymbol")
            } else {
                // Prefix logic (misal "# ", "- ")
                text.replaceRange(start, end, "$snippet$selectedText")
            }
            
            val newSelection = if (isWrapping) {
                TextRange(start + snippet.length, end + snippet.length)
            } else {
                TextRange(end + snippet.length)
            }
            
            current.copy(text = newText, selection = newSelection)
        } else {
            // Jika tidak ada seleksi, masukkan di posisi kursor
            val newText = text.replaceRange(start, end, snippet)
            val newCursorPos = start + snippet.length
            current.copy(text = newText, selection = TextRange(newCursorPos))
        }
        
        onContentChange(newContentValue)
    }

    fun toggleImageMenu() {
        _state.update { it.copy(isImageMenuExpanded = !it.isImageMenuExpanded) }
    }

    fun insertImage(uri: String) {
        if (_state.value.isDeleted) return
        val currentContent = _state.value.content
        val pattern = Regex("!\\[image-(\\d+)\\]")
        val matches = pattern.findAll(currentContent)
        val nextIndex = (matches.map { it.groupValues[1].toInt() }.maxOrNull() ?: 0) + 1
        
        val markdown = "\n![image-$nextIndex]($uri)\n"
        insertSnippet(markdown)
    }

    // Simpan segera (dipanggil saat back/close)
    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch { doSave() }
    }

    private fun scheduleAutoSave() {
        if (_state.value.isDeleted) return // Jangan auto-save jika di trash
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            doSave()
        }
    }

    private suspend fun doSave() {
        val s = _state.value
        
        // BUG FIX: Jika catatan ada di Trash (isDeleted = true), JANGAN lakukan update normal.
        // Update normal di sini secara tidak sengaja mengatur 'isDeleted = false' 
        // yang membuat catatan "keluar" dari trash tapi datanya mungkin tidak konsisten atau hilang dari view Trash.
        if (s.isDeleted) return 

        val now = LocalDateTime.now().toString()
        if (s.id == 0L) {
            // INSERT baru
            if (s.title.isBlank() && s.content.isBlank()) return
            val newId = db.noteDao().insert(
                NoteEntity(
                    title = s.title,
                    content = s.content,
                    folderId = s.folderId,
                    tagsJson = s.tags.toTagsJson(),
                    isPinned = s.isPinned,
                    isArchived = s.isArchived,
                    isLocked = s.isLocked,
                    isDeleted = false,
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
                    tagsJson = s.tags.toTagsJson(),
                    isPinned = s.isPinned,
                    isArchived = s.isArchived,
                    isLocked = s.isLocked,
                    isDeleted = false, // Tetap false karena kita sudah return jika s.isDeleted true
                    updatedAt = now
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }


    private fun countWords(text: String): Int =
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size

    fun exportToPdf(context: android.content.Context) {
        val s = _state.value
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "${context.getString(com.nu.lis.R.string.app_name)} - ${s.title}"
        
        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
        
        val html = buildHtml(s.title, s.content, s.tags)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
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

