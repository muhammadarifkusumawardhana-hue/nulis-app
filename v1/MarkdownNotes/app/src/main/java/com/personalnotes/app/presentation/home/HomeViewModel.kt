package com.personalnotes.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalnotes.app.data.local.SettingsDataStore
import com.personalnotes.app.data.remote.SyncManager
import com.personalnotes.app.data.remote.SyncResult
import com.personalnotes.app.domain.model.Folder
import com.personalnotes.app.domain.model.Note
import com.personalnotes.app.domain.model.Tag
import com.personalnotes.app.domain.repository.FolderRepository
import com.personalnotes.app.domain.repository.NoteRepository
import com.personalnotes.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedFolderId: Long? = null,
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val viewMode: ViewMode = ViewMode.LIST
)

enum class ViewMode { LIST, GRID }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
    private val syncManager: SyncManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val selectedFolderId = MutableStateFlow<Long?>(null)
    private val selectedTag = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    init {
        observeNotes()
        observeFolders()
        observeTags()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeNotes() {
        combine(selectedFolderId, selectedTag, searchQuery) { folderId, tag, query ->
            Triple(folderId, tag, query)
        }.flatMapLatest { (folderId, tag, query) ->
            when {
                query.isNotBlank() -> noteRepository.searchNotes(query)
                tag != null -> noteRepository.getNotesByTag(tag)
                folderId != null -> noteRepository.getNotesByFolder(folderId)
                else -> noteRepository.getNotesWithoutFolder()
            }
        }.onEach { notes ->
            _uiState.update { it.copy(notes = notes) }
        }.launchIn(viewModelScope)
    }

    private fun observeFolders() {
        folderRepository.getRootFolders().onEach { folders ->
            _uiState.update { it.copy(folders = folders) }
        }.launchIn(viewModelScope)
    }

    private fun observeTags() {
        tagRepository.getAllTags().onEach { tags ->
            _uiState.update { it.copy(tags = tags) }
        }.launchIn(viewModelScope)
    }

    fun selectFolder(folderId: Long?) {
        selectedFolderId.value = folderId
        selectedTag.value = null
        _uiState.update { it.copy(selectedFolderId = folderId, selectedTag = null) }
    }

    fun selectTag(tag: String?) {
        selectedTag.value = tag
        selectedFolderId.value = null
        _uiState.update { it.copy(selectedTag = tag, selectedFolderId = null) }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun clearSearch() {
        searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "", isSearching = false) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun createFolder(name: String, color: String = "#6200EE") {
        viewModelScope.launch {
            folderRepository.saveFolder(Folder(name = name, color = color, parentId = selectedFolderId.value))
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { folderRepository.deleteFolder(folder) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.deleteNote(note) }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            noteRepository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = syncManager.syncAllPendingNotes()
            val message = when (result) {
                is SyncResult.Success -> if (result.synced == 0) "Semua catatan sudah tersinkron" else "Tersinkron ${result.synced} catatan"
                is SyncResult.NotConnected -> "Google Drive belum terhubung"
                is SyncResult.Error -> "Sinkronisasi gagal: ${result.message}"
            }
            _uiState.update { it.copy(isSyncing = false, syncMessage = message) }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}
