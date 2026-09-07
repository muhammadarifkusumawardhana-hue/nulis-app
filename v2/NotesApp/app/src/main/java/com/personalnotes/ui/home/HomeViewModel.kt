package com.personalnotes.ui.home

import androidx.lifecycle.*
import com.personalnotes.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeViewModel(private val db: AppDatabase) : ViewModel() {

    // ── Filters ──────────────────────────────────────────
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    private val _selectedTag     = MutableStateFlow<String?>(null)
    private val _searchQuery     = MutableStateFlow("")

    val selectedFolderId: StateFlow<Long?> = _selectedFolderId
    val selectedTag:      StateFlow<String?> = _selectedTag
    val searchQuery:      StateFlow<String>  = _searchQuery

    // ── Notes (reactive, berubah sesuai filter) ──────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = combine(
        _selectedFolderId, _selectedTag, _searchQuery
    ) { fId, tag, q -> Triple(fId, tag, q) }
        .flatMapLatest { (fId, tag, q) ->
            when {
                q.isNotBlank() -> db.noteDao().search(q.trim())
                tag != null    -> db.noteDao().getNotesByTag(tag)
                fId != null    -> db.noteDao().getNotesByFolder(fId)
                else           -> db.noteDao().getNotesWithoutFolder()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Folders & Tags ────────────────────────────────────
    val folders: StateFlow<List<FolderEntity>> = db.folderDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = db.tagDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Filter actions ───────────────────────────────────
    fun selectFolder(id: Long?) {
        _selectedFolderId.value = id
        _selectedTag.value = null
        _searchQuery.value = ""
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
        _selectedFolderId.value = null
        _searchQuery.value = ""
    }

    fun setSearch(q: String) {
        _searchQuery.value = q
        if (q.isNotBlank()) {
            _selectedFolderId.value = null
            _selectedTag.value = null
        }
    }

    fun clearAll() {
        _selectedFolderId.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
    }

    // ── CRUD ─────────────────────────────────────────────
    fun deleteNote(id: Long) = viewModelScope.launch {
        db.noteDao().deleteById(id)
    }

    fun togglePin(note: NoteEntity) = viewModelScope.launch {
        db.noteDao().update(note.copy(isPinned = !note.isPinned))
    }

    fun createFolder(name: String, color: String) = viewModelScope.launch {
        db.folderDao().insert(FolderEntity(name = name, colorHex = color))
    }

    fun deleteFolder(id: Long) = viewModelScope.launch {
        // Pindahkan catatan di folder ini ke "tanpa folder"
        db.noteDao().getNotesByFolder(id).first().forEach { note ->
            db.noteDao().update(note.copy(folderId = -1L))
        }
        db.folderDao().deleteById(id)
    }
}

class HomeViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(db) as T
    }
}
