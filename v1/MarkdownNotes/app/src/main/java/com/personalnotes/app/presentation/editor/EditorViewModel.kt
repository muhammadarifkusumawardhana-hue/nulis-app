package com.personalnotes.app.presentation.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalnotes.app.data.local.SettingsDataStore
import com.personalnotes.app.data.remote.SyncManager
import com.personalnotes.app.domain.model.Note
import com.personalnotes.app.domain.model.SyncModeType
import com.personalnotes.app.domain.repository.NoteRepository
import com.personalnotes.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import javax.inject.Inject

data class EditorUiState(
    val note: Note = Note(),
    val isLoading: Boolean = true,
    val isPreviewMode: Boolean = false,
    val isSplitMode: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val backlinks: List<Note> = emptyList(),
    val isSaved: Boolean = true,
    val showBacklinkPanel: Boolean = false,
    val showTagPanel: Boolean = false,
    val availableTags: List<String> = emptyList(),
    val exportStatus: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val syncManager: SyncManager,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var noteId: Long? = null
    private var defaultFolderId: Long? = null
    private var settings = com.personalnotes.app.domain.model.AppSettings()

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings = it }
        }
        observeTags()
    }

    fun initialize(noteId: Long?, folderId: Long?) {
        this.noteId = noteId
        this.defaultFolderId = folderId
        viewModelScope.launch {
            if (noteId != null) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    _uiState.update { it.copy(note = note, isLoading = false) }
                    updateCounts(note.content)
                    observeBacklinks(note.title)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update {
                    it.copy(note = Note(folderId = folderId), isLoading = false)
                }
            }
        }
    }

    private fun observeTags() {
        tagRepository.getAllTags().onEach { tags ->
            _uiState.update { it.copy(availableTags = tags.map { t -> t.name }) }
        }.launchIn(viewModelScope)
    }

    private fun observeBacklinks(title: String) {
        if (title.isBlank()) return
        noteRepository.getBacklinks(title).onEach { backlinks ->
            _uiState.update { it.copy(backlinks = backlinks) }
        }.launchIn(viewModelScope)
    }

    fun onTitleChange(title: String) {
        val updated = _uiState.value.note.copy(title = title)
        _uiState.update { it.copy(note = updated, isSaved = false) }
        scheduleAutoSave()
    }

    fun onContentChange(content: String) {
        val updated = _uiState.value.note.copy(content = content)
        _uiState.update { it.copy(note = updated, isSaved = false) }
        updateCounts(content)
        scheduleAutoSave()

        // Trigger on-change sync
        if (settings.syncMode.mode == SyncModeType.AUTO_ON_CHANGE) {
            viewModelScope.launch { syncManager.scheduleOnChangeSync() }
        }
    }

    private fun updateCounts(content: String) {
        val words = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
        _uiState.update { it.copy(wordCount = words, charCount = content.length) }
    }

    private fun scheduleAutoSave() {
        if (!settings.autoSave) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500) // 1.5s debounce
            saveNote()
        }
    }

    fun saveNote(onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            val note = _uiState.value.note
            if (note.title.isBlank() && note.content.isBlank()) return@launch

            val savedId = noteRepository.saveNote(note)
            val savedNote = note.copy(id = if (note.id == 0L) savedId else note.id, isSynced = false)
            _uiState.update { it.copy(note = savedNote, isSaved = true) }

            if (noteId == null) noteId = savedId

            // Sync on save if configured
            if (settings.syncMode.mode == SyncModeType.ON_SAVE) {
                syncManager.syncAllPendingNotes()
            }
            onSaved?.invoke()
        }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode, isSplitMode = false) }
    }

    fun toggleSplitMode() {
        _uiState.update { it.copy(isSplitMode = !it.isSplitMode, isPreviewMode = false) }
    }

    fun toggleTagPanel() {
        _uiState.update { it.copy(showTagPanel = !it.showTagPanel) }
    }

    fun toggleBacklinkPanel() {
        _uiState.update { it.copy(showBacklinkPanel = !it.showBacklinkPanel) }
    }

    fun addTag(tag: String) {
        val currentTags = _uiState.value.note.tags.toMutableList()
        if (!currentTags.contains(tag)) {
            currentTags.add(tag)
            val updated = _uiState.value.note.copy(tags = currentTags)
            _uiState.update { it.copy(note = updated, isSaved = false) }
            viewModelScope.launch {
                tagRepository.saveTag(com.personalnotes.app.domain.model.Tag(name = tag))
                scheduleAutoSave()
            }
        }
    }

    fun removeTag(tag: String) {
        val currentTags = _uiState.value.note.tags.toMutableList()
        currentTags.remove(tag)
        val updated = _uiState.value.note.copy(tags = currentTags)
        _uiState.update { it.copy(note = updated, isSaved = false) }
        scheduleAutoSave()
    }

    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportStatus = "Membuat PDF...") }
            try {
                val note = _uiState.value.note
                val exporter = NoteExporter(context)
                val path = exporter.exportToPdf(note)
                _uiState.update { it.copy(exportStatus = "PDF disimpan: $path") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportStatus = "Gagal export: ${e.message}") }
            }
            delay(3000)
            _uiState.update { it.copy(exportStatus = null) }
        }
    }

    fun exportToHtml(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportStatus = "Membuat HTML...") }
            try {
                val note = _uiState.value.note
                val exporter = NoteExporter(context)
                val path = exporter.exportToHtml(note)
                _uiState.update { it.copy(exportStatus = "HTML disimpan: $path") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportStatus = "Gagal export: ${e.message}") }
            }
            delay(3000)
            _uiState.update { it.copy(exportStatus = null) }
        }
    }

    fun insertMarkdown(syntax: String) {
        val current = _uiState.value.note.content
        onContentChange(current + syntax)
    }
}
