package com.nu.lis.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.nu.lis.data.FolderEntity
import com.nu.lis.data.NoteEntity
import com.nu.lis.data.TagEntity
import com.nu.lis.data.tags
import com.nu.lis.theme.LocalAppSettings
import com.nu.lis.theme.MonoBlack
import com.nu.lis.theme.MonoWhite
import com.nu.lis.theme.Sage600
import com.nu.lis.theme.Terracotta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.nu.lis.ui.settings.LockPinDialog
import androidx.compose.ui.platform.LocalContext

// ── Helpers ──────────────────────────────────────────────────────
fun parseColor(hex: String): Color = try {
    Color(hex.toColorInt())
} catch (_: Exception) { Sage600 }

private fun greeting(isIndo: Boolean): String {
    val hour = java.time.LocalTime.now().hour
    return if (isIndo) {
        when {
            hour < 11 -> "Selamat pagi."
            hour < 15 -> "Selamat siang."
            hour < 18 -> "Selamat sore."
            else      -> "Selamat malam."
        }
    } else {
        when {
            hour < 12 -> "Good morning."
            hour < 17 -> "Good afternoon."
            else      -> "Good evening."
        }
    }
}

private fun fmtDate(iso: String): String = try {
    LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("MMM dd"))
} catch (_: Exception) { "" }

private fun getDaysRemaining(deletedAt: String?): Long {
    if (deletedAt == null) return 30
    return try {
        val deletedDate = LocalDateTime.parse(deletedAt)
        val now = LocalDateTime.now()
        val daysPassed = java.time.Duration.between(deletedDate, now).toDays()
        (30 - daysPassed).coerceAtLeast(0)
    } catch (_: Exception) {
        30
    }
}

enum class DragAnchors {
    Settled,
    Actions,
    Archive
}

// ── Main Screen ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long) -> Unit,
    onGoToSettings: () -> Unit
) {
    val notes          by vm.notes.collectAsState()
    val folders        by vm.folders.collectAsState()
    val tags           by vm.tags.collectAsState()
    val selectedFolder by vm.selectedFolderId.collectAsState()
    val selectedTag    by vm.selectedTag.collectAsState()
    val showArchived   by vm.showArchived.collectAsState()
    val showTrash      by vm.showTrash.collectAsState()
    val selectedNoteIds by vm.selectedNoteIds.collectAsState()
    val searchQuery    by vm.searchQuery.collectAsState()
    val isSearchVisible by vm.isSearchVisible.collectAsState()
    val quoteIndex      by vm.currentQuoteIndex.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteIdToDelete by remember { mutableStateOf<Long?>(null) } // null means selected notes
    val appSettings = LocalAppSettings.current
    val isIndo = appSettings.language == "id"

    val isSelectionMode = selectedNoteIds.isNotEmpty()

    // Handle Back Button
    BackHandler(enabled = isSelectionMode) {
        when {
            isSelectionMode -> vm.clearSelection()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                folders        = folders,
                tags           = tags,
                selectedFolder = selectedFolder,
                selectedTag    = selectedTag,
                showArchived   = showArchived,
                showTrash      = showTrash,
                onAllNotes     = { 
                    scope.launch {
                        drawerState.close()
                        vm.clearAll()
                    }
                },
                onFolder       = { id ->
                    scope.launch {
                        drawerState.close()
                        vm.selectFolder(id)
                    }
                },
                onTag          = { tag ->
                    scope.launch {
                        drawerState.close()
                        vm.selectTag(tag)
                    }
                },
                onNewFolder    = { 
                    scope.launch {
                        drawerState.close()
                        showNewFolderDialog = true
                    }
                },
                onEditFolder   = { folder ->
                    scope.launch {
                        drawerState.close()
                        editingFolder = folder
                    }
                },
                onDeleteFolder = { vm.deleteFolder(it) },
                onArchived     = { 
                    scope.launch {
                        drawerState.close()
                        vm.showArchived()
                    }
                },
                onTrash        = {
                    scope.launch {
                        drawerState.close()
                        vm.showTrash()
                    }
                },
                onSettings     = { 
                    scope.launch {
                        drawerState.close()
                        onGoToSettings()
                    }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            topBar = {
                Column {
                    AnimatedContent(
                        targetState = isSelectionMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "SelectionModeTransition"
                    ) { selection ->
                        if (selection) {
                            SelectionTopBar(
                                count = selectedNoteIds.size,
                                onClear = { vm.clearSelection() },
                                onDelete = { 
                                    noteIdToDelete = null
                                    showDeleteConfirm = true 
                                },
                                onArchive = {
                                    val ids = selectedNoteIds.toList()
                                    vm.archiveSelectedNotes()
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (isIndo) "${ids.size} Catatan diarsip" else "${ids.size} Notes archived",
                                            actionLabel = if (isIndo) "Urungkan" else "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            ids.forEach { vm.unarchiveNote(it) }
                                        }
                                    }
                                },
                                onUnarchive = {
                                    val ids = selectedNoteIds.toList()
                                    vm.unarchiveSelectedNotes()
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (isIndo) "${ids.size} Catatan dikembalikan" else "${ids.size} Notes unarchived",
                                            actionLabel = if (isIndo) "Urungkan" else "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            ids.forEach { vm.archiveNote(it) }
                                        }
                                    }
                                },
                                onRestore = { vm.restoreSelectedNotes() },
                                showRestore = showTrash,
                                showUnarchive = showArchived,
                                folders = folders,
                                onMoveToFolder = { vm.moveSelectedToFolder(it) },
                                isIndo = isIndo
                            )
                        } else {
                            TopAppBar(
                                title = {
                                    when {
                                        showTrash -> Text(if (isIndo) "Sampah" else "Trash", style = MaterialTheme.typography.titleLarge)
                                        showArchived -> Text(if (isIndo) "Arsip" else "Archive", style = MaterialTheme.typography.titleLarge)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Menu",
                                            tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { vm.toggleSearch() }) {
                                        Icon(if (isSearchVisible) Icons.Default.Close else Icons.Default.Search, 
                                            if (isSearchVisible) (if (isIndo) "Tutup" else "Close") else (if (isIndo) "Cari" else "Search"),
                                            tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                    
                    // ── Always on top Search bar ────────────────────
                    if (isSearchVisible && !isSelectionMode) {
                        HomeSearchBar(
                            query    = searchQuery,
                            onChange = { vm.setSearch(it) },
                            onClear  = { vm.setSearch("") },
                            isIndo   = isIndo
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isSelectionMode && !showArchived && !showTrash,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick  = { onNewNote(selectedFolder ?: -1L) },
                        shape    = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, if (isIndo) "Catatan baru" else "New note", modifier = Modifier.size(24.dp))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Greeting header ─────────────────────────────
                if (!isSearchVisible && !showArchived && !showTrash) {
                    item {
                        GreetingHeader(
                            noteCount = notes.size,
                            nickname = appSettings.nickname,
                            isIndo = isIndo
                        )
                    }
                }

                // ── Layout Toggle & Tag chips ──────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { appSettings.isGridView = !appSettings.isGridView },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (appSettings.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isIndo) "Ganti tampilan" else "Switch view",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        if (tags.isNotEmpty() && !isSearchVisible && !showArchived && !showTrash) {
                            Spacer(Modifier.width(8.dp))
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(tags) { tag ->
                                    val selected = selectedTag == tag.name
                                    FilterChip(
                                        selected = selected,
                                        onClick  = { vm.selectTag(if (selected) null else tag.name) },
                                        label    = { Text(tag.name, fontSize = 12.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                            labelColor             = MaterialTheme.colorScheme.onSurfaceVariant,
                                            containerColor         = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = Color.Transparent,
                                            selectedBorderColor = Color.Transparent,
                                            borderWidth = 0.dp,
                                            selectedBorderWidth = 0.dp
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── Notes ───────────────────────────────────────
                if (notes.isEmpty()) {
                    item { 
                        EmptyState(
                            showArchived = showArchived,
                            showTrash = showTrash,
                            onNewNote = { onNewNote(selectedFolder ?: -1L) }, 
                            isIndo = isIndo
                        ) 
                    }
                } else {
                    if (appSettings.isGridView) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                notes.chunked(2).forEach { rowNotes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowNotes.forEach { note ->
                                            val isSelected = selectedNoteIds.contains(note.id)
                                            Box(modifier = Modifier.weight(1f)) {
                                                SwipeableNoteItem(
                                                    note = note,
                                                    folders = folders,
                                                    isSelected = isSelected,
                                                    isIndo = isIndo,
                                                    isGrid = true,
                                                    showFolderName = selectedFolder == null,
                                                    onClick = {
                                                        if (isSelectionMode) vm.toggleSelection(note.id)
                                                        else onOpenNote(note.id)
                                                    },
                                                    onLongClick = { vm.toggleSelection(note.id) },
                                                    onDelete = { 
                                                        noteIdToDelete = note.id
                                                        showDeleteConfirm = true
                                                    },
                                                    onPin = { vm.togglePin(note) },
                                                    onArchive = { 
                                                        scope.launch {
                                                            val noteId = note.id
                                                            val wasArchived = note.isArchived
                                                            vm.toggleArchive(note)
                                                            val result = snackbarHostState.showSnackbar(
                                                                message = if (isIndo) (if (wasArchived) "Catatan dikembalikan" else "Catatan diarsip") 
                                                                          else (if (wasArchived) "Note unarchived" else "Note archived"),
                                                                actionLabel = if (isIndo) "Urungkan" else "Undo",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            if (result == SnackbarResult.ActionPerformed) {
                                                                if (wasArchived) vm.archiveNote(noteId) else vm.unarchiveNote(noteId)
                                                            }
                                                        }
                                                    },
                                                    onRestore = { vm.restoreNote(note.id) },
                                                    onLock = { vm.toggleLock(note.id) },
                                                    onShowSettings = { onGoToSettings() },
                                                    onMoveToFolder = { folderId -> vm.moveNoteToFolder(note.id, folderId) }
                                                )
                                            }
                                        }
                                        if (rowNotes.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            val isSelected = selectedNoteIds.contains(note.id)
                            SwipeableNoteItem(
                                note = note,
                                folders = folders,
                                isSelected = isSelected,
                                isIndo = isIndo,
                                isGrid = false,
                                showFolderName = selectedFolder == null,
                                onClick = {
                                    if (isSelectionMode) vm.toggleSelection(note.id)
                                    else onOpenNote(note.id)
                                },
                                onLongClick = { vm.toggleSelection(note.id) },
                                onDelete = { 
                                    noteIdToDelete = note.id
                                    showDeleteConfirm = true
                                },
                                onPin = { vm.togglePin(note) },
                                onArchive = { 
                                    scope.launch {
                                        val noteId = note.id
                                        val wasArchived = note.isArchived
                                        vm.toggleArchive(note)
                                        val result = snackbarHostState.showSnackbar(
                                            message = if (isIndo) (if (wasArchived) "Catatan dikembalikan" else "Catatan diarsip") 
                                                      else (if (wasArchived) "Note unarchived" else "Note archived"),
                                            actionLabel = if (isIndo) "Urungkan" else "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            if (wasArchived) vm.archiveNote(noteId) else vm.unarchiveNote(noteId)
                                        }
                                    }
                                },
                                onRestore = { vm.restoreNote(note.id) },
                                onLock = { vm.toggleLock(note.id) },
                                onShowSettings = { onGoToSettings() },
                                onMoveToFolder = { folderId -> vm.moveNoteToFolder(note.id, folderId) }
                            )
                        }
                    }
                }

                // ── Motivational quote footer ───────────────────
                if (notes.size > 2 && !isSearchVisible && !showArchived && !showTrash && !isSelectionMode) {
                    item { 
                        QuoteCard(
                            quote = vm.getQuote(quoteIndex, appSettings.language),
                            onClick = { vm.nextQuote() }
                        ) 
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            folder = null,
            onConfirm = { name, color, icon ->
                vm.createFolder(name, color, icon)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
            isIndo = isIndo
        )
    }

    if (editingFolder != null) {
        NewFolderDialog(
            folder = editingFolder,
            onConfirm = { name, color, icon ->
                vm.updateFolder(editingFolder!!.id, name, color, icon)
                editingFolder = null
            },
            onDismiss = { editingFolder = null },
            isIndo = isIndo
        )
    }

    if (showDeleteConfirm) {

        val isPermanent = showTrash
        val title = if (isPermanent) (if (isIndo) "Hapus Permanen" else "Delete Permanently") else (if (isIndo) "Pindah ke Sampah" else "Move to Trash")
        val message = if (isPermanent) 
            (if (isIndo) "Apakah Anda yakin ingin menghapus permanen? Tindakan ini tidak dapat dibatalkan." else "Are you sure you want to delete this permanently? This action cannot be undone.")
            else (if (isIndo) "Apakah Anda yakin? Catatan akan dipindahkan ke sampah." else "Are you sure you want to delete this? Notes will moved to trash.")

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = noteIdToDelete
                        val wasSelected = id == null
                        val selectedIdsSnapshot = if (wasSelected) selectedNoteIds.toList() else emptyList()
                        
                        if (wasSelected) vm.deleteSelectedNotes() else vm.deleteNote(id!!)
                        showDeleteConfirm = false
                        
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = if (isIndo) "Catatan dihapus" else "Note deleted",
                                actionLabel = if (isIndo) "Urungkan" else "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                if (wasSelected) {
                                    selectedIdsSnapshot.forEach { vm.restoreNote(it) }
                                } else {
                                    vm.restoreNote(id!!)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (isIndo) "Hapus" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isIndo) "Batal" else "Cancel")
                }
            }
        )
    }
}

// ── Selection Top Bar ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onRestore: () -> Unit,
    showRestore: Boolean,
    showUnarchive: Boolean,
    folders: List<FolderEntity>,
    onMoveToFolder: (Long) -> Unit,
    isIndo: Boolean
) {
    var showFolderMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("$count " + (if (isIndo) "dipilih" else "selected")) },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, null)
            }
        },
        actions = {
            if (showRestore) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = if (isIndo) "Pulihkan" else "Restore")
                }
            } else if (showUnarchive) {
                IconButton(onClick = onUnarchive) {
                    Icon(Icons.Default.Unarchive, contentDescription = if (isIndo) "Keluarkan dari arsip" else "Unarchive")
                }
            } else {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, contentDescription = if (isIndo) "Arsip" else "Archive")
                }
            }
            if (folders.isNotEmpty()) {
                IconButton(onClick = { showFolderMenu = true }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = if (isIndo) "Pindah ke folder" else "Move to folder")
                    DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = { 
                                    onMoveToFolder(folder.id)
                                    showFolderMenu = false
                                },
                                leadingIcon = {
                                    if (!folder.icon.isNullOrBlank()) {
                                        Text(folder.icon, fontSize = 14.sp)
                                    } else {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(parseColor(folder.colorHex)))
                                    }
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text(if (isIndo) "Keluarkan dari folder" else "Remove from folder") },
                            onClick = { 
                                onMoveToFolder(-1L)
                                showFolderMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = if (isIndo) "Hapus" else "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    )
}

// ── Swipeable Note Item ────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableNoteItem(
    note: NoteEntity,
    folders: List<FolderEntity>,
    isSelected: Boolean,
    isIndo: Boolean,
    isGrid: Boolean,
    showFolderName: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onLock: () -> Unit,
    onShowSettings: () -> Unit,
    onMoveToFolder: (Long) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Settled,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween()
        )
    }

    val actionsWidth = with(density) { 124.dp.toPx() }
    val archiveWidth = with(density) { 80.dp.toPx() }
    
    SideEffect {
        anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                DragAnchors.Archive at archiveWidth
                DragAnchors.Settled at 0f
                DragAnchors.Actions at -actionsWidth
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isGrid) 0.dp else 4.dp)
    ) {
        // Background Actions
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = if (isGrid) 6.dp else 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (anchoredDraggableState.offset < 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer),
            horizontalArrangement = if (anchoredDraggableState.offset < 0) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (anchoredDraggableState.offset > 0) {
                Box(
                    modifier = Modifier.fillMaxHeight().width(80.dp).clickable {
                        if (note.isDeleted) onRestore() else onArchive()
                        scope.launch { anchoredDraggableState.animateTo(DragAnchors.Settled) }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (note.isDeleted) Icons.Default.Restore else (if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive), 
                        null, 
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else if (anchoredDraggableState.offset < 0) {
                Row(modifier = Modifier.padding(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!note.isDeleted) {
                        IconButton(
                            onClick = {
                                onPin()
                                scope.launch { anchoredDraggableState.animateTo(DragAnchors.Settled) }
                            },
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        ) {
                            Icon(if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, null, tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = {
                            onDelete()
                            scope.launch { anchoredDraggableState.animateTo(DragAnchors.Settled) }
                        },
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Foreground Note Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = anchoredDraggableState.offset.roundToInt(), y = 0) }
                .anchoredDraggable(state = anchoredDraggableState, orientation = Orientation.Horizontal)
                .background(MaterialTheme.colorScheme.background)
        ) {
            NoteCard(
                note = note,
                folders = folders,
                onClick = {
                    if (anchoredDraggableState.currentValue == DragAnchors.Settled) onClick()
                    else scope.launch { anchoredDraggableState.animateTo(DragAnchors.Settled) }
                },
                onLongClick = onLongClick,
                onDelete = onDelete,
                onPin = onPin,
                onLock = onLock,
                onShowSettings = onShowSettings,
                onMoveToFolder = onMoveToFolder,
                isIndo = isIndo,
                isGrid = isGrid,
                isSelected = isSelected,
                showFolderName = showFolderName
            )
        }
    }
}

@Composable
private fun GreetingHeader(noteCount: Int, nickname: String, isIndo: Boolean) {
    val displayName = if (nickname.isNotBlank()) ", $nickname" else ""
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            greeting(isIndo).replace(".", "") + displayName + ".",
            style     = MaterialTheme.typography.displaySmall,
            color     = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isIndo) "Kamu sudah punya total $noteCount catatan." else "You have $noteCount notes so far.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HomeSearchBar(query: String, onChange: (String) -> Unit, onClear: () -> Unit, isIndo: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value        = query,
            onValueChange = onChange,
            textStyle    = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine   = true,
            modifier     = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(if (isIndo) "Cari catatan atau tag..." else "Search notes or tags...",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Clear, if (isIndo) "Hapus" else "Clear", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    folders: List<FolderEntity>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onLock: () -> Unit,
    onShowSettings: () -> Unit,
    onMoveToFolder: (Long) -> Unit,
    isIndo: Boolean,
    isGrid: Boolean = false,
    isSelected: Boolean = false,
    showFolderName: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    val appSettings = LocalAppSettings.current

    if (showPinDialog) {
        LockPinDialog(
            isIndo = isIndo,
            correctPin = appSettings.noteLockPin,
            onSuccess = {
                showPinDialog = false
                onLock()
            },
            onDismiss = { showPinDialog = false }
        )
    }

    val tagList  = note.tags()
    val date     = fmtDate(note.updatedAt)
    val folder = folders.find { it.id == note.folderId }
    val folderName = folder?.name
    val folderColor = folder?.let { parseColor(it.colorHex) } ?: MaterialTheme.colorScheme.primaryContainer
    val onFolderColor = if (folderColor.luminance() > 0.5f) Color.Black else Color.White
    
    val settings = LocalAppSettings.current
    val isMono = settings.accentColor == Color.Black || settings.accentColor == Color.White || settings.accentColor == MonoBlack || settings.accentColor == MonoWhite
    val showStroke = isMono && !settings.darkMode

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isGrid) 6.dp else 20.dp, vertical = 6.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else if (showStroke) Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (note.isLocked && appSettings.isNoteLockEnabled) {
                        // showPinDialog = true
                        onClick() // Go directly to EditorScreen, it will handle locking
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(if (isGrid) 16.dp else 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    if (tagList.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tagList.take(2).forEach { tag ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    val textColor = if (MaterialTheme.colorScheme.secondaryContainer.luminance() > 0.5f) Color.Black else Color.White
                                    Text(
                                        tag.uppercase(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style    = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color    = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (tagList.size > 2) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        "...",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isGrid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isPinned) {
                            Icon(Icons.Default.PushPin, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) (if (isIndo) "Lepas pin" else "Unpin") else (if (isIndo) "Sematkan" else "Pin")) },
                                    onClick = { onPin(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.PushPin, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isIndo) "Pindah ke folder" else "Move to folder") },
                                    onClick = { showMoveMenu = true; showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                                )
                                if (note.folderId != -1L) {
                                    DropdownMenuItem(
                                        text = { Text(if (isIndo) "Keluarkan dari folder" else "Remove from folder") },
                                        onClick = { onMoveToFolder(-1L); showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.FolderOff, null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (isIndo) "Hapus" else "Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = { onDelete(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                            
                            DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                                folders.filter { it.id != note.folderId }.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = { onMoveToFolder(folder.id); showMoveMenu = false },
                                        leadingIcon = { 
                                            Box(
                                                modifier = Modifier.size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!folder.icon.isNullOrBlank()) {
                                                    Text(folder.icon, fontSize = 16.sp)
                                                } else {
                                                    Box(
                                                        Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(parseColor(folder.colorHex))
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (note.isPinned) {
                    Icon(Icons.Default.PushPin, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(if (isGrid) 6.dp else 10.dp))
            Text(
                note.title.ifBlank { if (isIndo) "Tanpa judul" else "Untitled" },
                style    = if (isGrid) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isGrid) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.isDeleted) {
                val days = getDaysRemaining(note.deletedAt)
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        if (isIndo) "$days hari tersisa" else "$days days left",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(if (isGrid) 4.dp else 8.dp))
                if (note.isLocked && appSettings.isNoteLockEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isIndo) "Catatan dikunci" else "Note is locked",
                            style = if (isGrid) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val preview = note.content.replace(Regex("^#+\\s+", RegexOption.MULTILINE), "").replace(Regex("\\*{1,2}(.+?)\\*{1,2}"), "$1").trim()
                    Text(
                        preview.take(140),
                        style    = if (isGrid) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isGrid) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = if (isGrid) 18.sp else 22.sp
                    )
                }
            }
            
            val showBottomRow = isGrid || (showFolderName && folderName != null)
            if (showBottomRow) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGrid) {
                        Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    
                    if (showFolderName && folderName != null) {
                        Surface(
                            shape = CircleShape,
                            color = folderColor,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!folder.icon.isNullOrBlank()) {
                                        Text(folder.icon, fontSize = 10.sp)
                                    } else {
                                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(10.dp), tint = onFolderColor)
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    folderName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = onFolderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u201C", 
                fontSize = 48.sp, 
                color = MaterialTheme.colorScheme.secondary, 
                modifier = Modifier.offset(y = (-8).dp)
            )
            Text(
                quote, 
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                ), 
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(
    showArchived: Boolean = false,
    showTrash: Boolean = false,
    onNewNote: () -> Unit,
    isIndo: Boolean
) {
    val icon = when {
        showTrash -> Icons.Outlined.DeleteForever
        showArchived -> Icons.Outlined.Archive
        else -> Icons.AutoMirrored.Outlined.NoteAdd
    }
    
    val title = when {
        showTrash -> if (isIndo) "Sampah Kosong" else "Trash is Empty"
        showArchived -> if (isIndo) "Tidak ada arsip" else "No Archived Notes"
        else -> if (isIndo) "" else ""
    }
    
    val subtitle = when {
        showTrash -> if (isIndo) "Catatan akan dihapus permanen dalam 30 hari." else "Notes will permanently deleted soon in 30 days."
        showArchived -> if (isIndo) "Belum ada catatan yang diarsip." else "There is no archived notes."
        else -> if (isIndo) "Mulai catatan pertamamu." else "Start your first note."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(icon, null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(20.dp))
        Text(title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        
        if (!showArchived && !showTrash) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onNewNote,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isIndo) "Catatan Baru" else "New Note")
            }
        }
    }
}

@Composable
private fun SideDrawer(
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    selectedFolder: Long?,
    selectedTag: String?,
    showArchived: Boolean,
    showTrash: Boolean,
    onAllNotes: () -> Unit,
    onFolder: (Long) -> Unit,
    onTag: (String) -> Unit,
    onNewFolder: () -> Unit,
    onEditFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onArchived: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit
) {
    val settings = LocalAppSettings.current
    ModalDrawerSheet(modifier = Modifier.width(300.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(24.dp))
        Text("Nu.lis", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                label = { Text(if (settings.language == "id") "Semua Catatan" else "All Notes") },
                selected = selectedFolder == null && selectedTag == null && !showArchived && !showTrash,
                onClick = onAllNotes,
                colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedTextColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (settings.language == "id") "FOLDER" else "FOLDERS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                IconButton(onClick = onNewFolder, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            folders.forEach { folder ->
                var showMenu by remember { mutableStateOf(false) }
                NavigationDrawerItem(
                    icon = { 
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!folder.icon.isNullOrBlank()) {
                                Text(folder.icon, fontSize = 16.sp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parseColor(folder.colorHex))
                                )
                            }
                        }
                    },
                    label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = selectedFolder == folder.id,
                    onClick = { onFolder(folder.id) },
                    badge = {
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(14.dp)) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { onEditFolder(folder); showMenu = false })
                                DropdownMenuItem(text = { Text("Hapus") }, onClick = { onDeleteFolder(folder.id); showMenu = false })
                            }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedTextColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            Spacer(Modifier.height(12.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Archive, null) },
                label = { Text(if (settings.language == "id") "Arsip" else "Archive") },
                selected = showArchived,
                onClick = onArchived,
                colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedTextColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Delete, null) },
                label = { Text(if (settings.language == "id") "Sampah" else "Trash") },
                selected = showTrash,
                onClick = onTrash,
                colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedTextColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(if (settings.language == "id") "TAG" else "TAGS", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                tags.forEach { tag ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(18.dp)) },
                        label = { Text("#${tag.name}") },
                        selected = selectedTag == tag.name,
                        onClick = { onTag(tag.name) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationDrawerItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text(if (settings.language == "id") "Pengaturan" else "Settings") }, selected = false, onClick = onSettings, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NewFolderDialog(folder: FolderEntity? = null, onConfirm: (String, String, String?) -> Unit, onDismiss: () -> Unit, isIndo: Boolean) {
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var iconInput by remember { mutableStateOf(folder?.icon ?: "") }
    val colors = listOf("#3D7A3D","#B85C38","#2C5F8A","#7A5C3D","#5C3D7A","#3D7A6A")
    var selectedColor by remember { mutableStateOf(folder?.colorHex ?: colors[0]) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(if (folder == null) (if (isIndo) "Folder Baru" else "New Folder") else (if (isIndo) "Edit Folder" else "Edit Folder")) }, 
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = iconInput, 
                        onValueChange = { if (it.length <= 2) iconInput = it }, 
                        label = { Text("Icon") }, 
                        placeholder = { Text("📁") },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text(if (isIndo) "Nama" else "Name") }, 
                        singleLine = true, 
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { c -> Box(Modifier.size(32.dp).clip(CircleShape).background(parseColor(c)).border(if (selectedColor == c) 2.dp else 0.dp, Color.Black, CircleShape).clickable { selectedColor = c }) }
                }
            }
        }, 
        confirmButton = { 
            Button(onClick = { onConfirm(name, selectedColor, iconInput.ifBlank { null }) }, enabled = name.isNotBlank()) {
                Text(if (isIndo) "Simpan" else "Save") 
            } 
        }, 
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text(if (isIndo) "Batal" else "Cancel") 
            } 
        }
    )
}

