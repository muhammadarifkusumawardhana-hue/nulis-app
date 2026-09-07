package com.nu.lis.ui.home

import androidx.lifecycle.*
import com.nu.lis.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeViewModel(private val db: AppDatabase) : ViewModel() {

    // ── Filters ──────────────────────────────────────────
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    private val _selectedTag     = MutableStateFlow<String?>(null)
    private val _searchQuery     = MutableStateFlow("")
    private val _showDrafts      = MutableStateFlow(false)
    private val _showArchived    = MutableStateFlow(false)
    private val _showTrash       = MutableStateFlow(false)
    private val _isSearchVisible = MutableStateFlow(false)

    // Selection State
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds

    val selectedFolderId: StateFlow<Long?> = _selectedFolderId
    val selectedTag:      StateFlow<String?> = _selectedTag
    val searchQuery:      StateFlow<String>  = _searchQuery
    val showDrafts:       StateFlow<Boolean> = _showDrafts
    val showArchived:     StateFlow<Boolean> = _showArchived
    val showTrash:        StateFlow<Boolean> = _showTrash
    val isSearchVisible:  StateFlow<Boolean> = _isSearchVisible

    init {
        purgeTrash()
    }

    private fun purgeTrash() = viewModelScope.launch {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30).toString()
        db.noteDao().purgeOldDeletedNotes(thirtyDaysAgo)
    }

    // ── Notes (reactive, berubah sesuai filter) ──────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = combine(
        _selectedFolderId, _selectedTag, _searchQuery, _showDrafts, _showArchived, _showTrash
    ) { args ->
        Hexuple(
            fId = args[0] as Long?,
            tag = args[1] as String?,
            q = args[2] as String,
            drafts = args[3] as Boolean,
            archived = args[4] as Boolean,
            trash = args[5] as Boolean
        )
    }
        .flatMapLatest { h ->
            when {
                h.trash          -> db.noteDao().getDeletedNotes()
                h.archived       -> db.noteDao().getArchivedNotes()
                h.drafts         -> db.noteDao().getDrafts()
                h.q.isNotBlank() -> db.noteDao().search(h.q.trim())
                h.tag != null    -> db.noteDao().getNotesByTag(h.tag)
                h.fId != null    -> db.noteDao().getNotesByFolder(h.fId)
                else           -> db.noteDao().getAllNotes()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class Hexuple(
        val fId: Long?,
        val tag: String?,
        val q: String,
        val drafts: Boolean,
        val archived: Boolean,
        val trash: Boolean
    )

    // ── Folders & Tags ────────────────────────────────────
    val folders: StateFlow<List<FolderEntity>> = db.folderDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = db.noteDao().getAllNotes()
        .map { allNotes ->
            allNotes.flatMap { it.tags() }
                .map { it.trim().lowercase() }
                .distinct()
                .filter { it.isNotBlank() }
                .sorted()
                .mapIndexed { index, name -> TagEntity(id = index.toLong(), name = name) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val quotesEn = listOf(
        "The best way to predict the future is to invent it.",
        "Your mind is for having ideas, not holding them.",
        "Simplicity is the ultimate sophistication.",
        "Write it down, make it happen.",
        "Creativity is intelligence having fun.",
        "The secret of getting ahead is getting started."
    )
    private val quotesId = listOf(
        "Cara terbaik untuk memprediksi masa depan adalah dengan menciptakannya.",
        "Pikiranmu adalah untuk menghasilkan ide, bukan untuk menyimpannya.",
        "Kesederhanaan adalah kecanggihan tertinggi.",
        "Tuliskanlah, dan wujudkanlah.",
        "Kreativitas adalah kecerdasan yang sedang bersenang-senang.",
        "Rahasia untuk maju adalah dengan memulai."
    )

    private val _currentQuoteIndex = MutableStateFlow((0 until quotesEn.size).random())
    val currentQuoteIndex = _currentQuoteIndex.asStateFlow()
    
    fun getQuote(index: Int, lang: String): String {
        return if (lang == "id") quotesId[index] else quotesEn[index]
    }

    fun getCurrentQuote(lang: String): String {
        val index = _currentQuoteIndex.value
        return if (lang == "id") quotesId[index] else quotesEn[index]
    }

    fun nextQuote() {
        val size = quotesEn.size
        var next = (0 until size).random()
        while (next == _currentQuoteIndex.value) next = (0 until size).random()
        _currentQuoteIndex.value = next
    }

    // ── Filter actions ───────────────────────────────────
    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            _searchQuery.value = ""
        } else {
            // Reset filters when starting search
            _selectedFolderId.value = null
            _selectedTag.value = null
            _showDrafts.value = false
            _showArchived.value = false
            _showTrash.value = false
        }
    }

    fun selectFolder(id: Long?) {
        _selectedFolderId.value = id
        _selectedTag.value = null
        _searchQuery.value = ""
        _showDrafts.value = false
        _showArchived.value = false
        _showTrash.value = false
        _isSearchVisible.value = false
        clearSelection()
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
        _selectedFolderId.value = null
        _searchQuery.value = ""
        _showDrafts.value = false
        _showArchived.value = false
        _showTrash.value = false
        _isSearchVisible.value = false
        clearSelection()
    }

    fun setSearch(q: String) {
        _searchQuery.value = q
    }

    fun showDrafts() {
        _showDrafts.value = true
        _selectedFolderId.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
        _showArchived.value = false
        _showTrash.value = false
        _isSearchVisible.value = false
        clearSelection()
    }

    fun showArchived() {
        _showArchived.value = true
        _showDrafts.value = false
        _showTrash.value = false
        _selectedFolderId.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
        _isSearchVisible.value = false
        clearSelection()
    }

    fun showTrash() {
        _showTrash.value = true
        _showArchived.value = false
        _showDrafts.value = false
        _selectedFolderId.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
        _isSearchVisible.value = false
        clearSelection()
    }

    fun clearAll() {
        _selectedFolderId.value = null
        _selectedTag.value = null
        _searchQuery.value = ""
        _showDrafts.value = false
        _showArchived.value = false
        _showTrash.value = false
        _isSearchVisible.value = false
        clearSelection()
    }

    // ── Selection Actions ────────────────────────────────
    fun toggleSelection(id: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedNoteIds.value = current
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    // ── CRUD ─────────────────────────────────────────────
    fun deleteNote(id: Long) = viewModelScope.launch {
        db.noteDao().getById(id)?.let { note ->
            if (note.isDeleted) {
                db.noteDao().deleteById(id)
            } else {
                db.noteDao().update(note.copy(isDeleted = true, deletedAt = LocalDateTime.now().toString()))
            }
        }
    }

    fun restoreNote(id: Long) = viewModelScope.launch {
        db.noteDao().getById(id)?.let { note ->
            db.noteDao().update(note.copy(isDeleted = false, deletedAt = null))
        }
    }

    fun deleteSelectedNotes() = viewModelScope.launch {
        _selectedNoteIds.value.forEach { id ->
            db.noteDao().getById(id)?.let { note ->
                if (note.isDeleted) {
                    db.noteDao().deleteById(id)
                } else {
                    db.noteDao().update(note.copy(isDeleted = true, deletedAt = LocalDateTime.now().toString()))
                }
            }
        }
        clearSelection()
    }

    fun restoreSelectedNotes() = viewModelScope.launch {
        _selectedNoteIds.value.forEach { id ->
            db.noteDao().getById(id)?.let { note ->
                db.noteDao().update(note.copy(isDeleted = false, deletedAt = null))
            }
        }
        clearSelection()
    }

    fun archiveSelectedNotes() = viewModelScope.launch {
        _selectedNoteIds.value.forEach { id ->
            db.noteDao().getById(id)?.let { note ->
                db.noteDao().update(note.copy(isArchived = true))
            }
        }
        clearSelection()
    }

    fun unarchiveSelectedNotes() = viewModelScope.launch {
        _selectedNoteIds.value.forEach { id ->
            db.noteDao().getById(id)?.let { note ->
                db.noteDao().update(note.copy(isArchived = false))
            }
        }
        clearSelection()
    }

    fun moveSelectedToFolder(folderId: Long) = viewModelScope.launch {
        _selectedNoteIds.value.forEach { id ->
            db.noteDao().getById(id)?.let { note ->
                db.noteDao().update(note.copy(folderId = folderId))
            }
        }
        clearSelection()
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) = viewModelScope.launch {
        db.noteDao().getById(noteId)?.let { note ->
            db.noteDao().update(note.copy(folderId = folderId))
        }
    }

    fun archiveNote(noteId: Long) = viewModelScope.launch {
        db.noteDao().getById(noteId)?.let { note ->
            db.noteDao().update(note.copy(isArchived = true))
        }
    }

    fun unarchiveNote(noteId: Long) = viewModelScope.launch {
        db.noteDao().getById(noteId)?.let { note ->
            db.noteDao().update(note.copy(isArchived = false))
        }
    }

    fun toggleArchive(note: NoteEntity) = viewModelScope.launch {
        db.noteDao().update(note.copy(isArchived = !note.isArchived))
    }

    fun togglePin(note: NoteEntity) = viewModelScope.launch {
        db.noteDao().update(note.copy(isPinned = !note.isPinned))
    }

    fun toggleLock(noteId: Long) = viewModelScope.launch {
        db.noteDao().getById(noteId)?.let { note ->
            db.noteDao().update(note.copy(isLocked = !note.isLocked))
        }
    }

    fun createFolder(name: String, color: String, icon: String? = null) = viewModelScope.launch {
        db.folderDao().insert(FolderEntity(name = name, colorHex = color, icon = icon))
    }

    fun updateFolder(id: Long, name: String, color: String, icon: String? = null) = viewModelScope.launch {
        db.folderDao().update(FolderEntity(id = id, name = name, colorHex = color, icon = icon))
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

