package com.personalnotes.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personalnotes.app.domain.model.Folder
import com.personalnotes.app.domain.model.Note
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSyncMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                folders = uiState.folders,
                tags = uiState.tags,
                selectedFolderId = uiState.selectedFolderId,
                selectedTag = uiState.selectedTag,
                onFolderClick = { viewModel.selectFolder(it) },
                onTagClick = { viewModel.selectTag(it) },
                onAllNotesClick = { viewModel.selectFolder(null); viewModel.selectTag(null) },
                onNewFolder = { showNewFolderDialog = true },
                onDeleteFolder = { viewModel.deleteFolder(it) }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (uiState.isSearching) {
                    SearchTopBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onClose = { viewModel.clearSearch() }
                    )
                } else {
                    HomeTopBar(
                        title = currentTitle(uiState),
                        onMenuClick = { /* open drawer */ },
                        onSearchClick = { viewModel.setSearchQuery(" ") },
                        onViewToggle = { viewModel.toggleViewMode() },
                        onSyncClick = { viewModel.manualSync() },
                        onSettingsClick = onSettingsClick,
                        viewMode = uiState.viewMode,
                        isSyncing = uiState.isSyncing,
                        drawerState = drawerState
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNewNote(uiState.selectedFolderId) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catatan baru")
                }
            },
            snackbarHost = {
                uiState.syncMessage?.let { msg ->
                    Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Tag chips row
                if (uiState.tags.isNotEmpty() && !uiState.isSearching) {
                    TagChipsRow(
                        tags = uiState.tags.map { it.name },
                        selectedTag = uiState.selectedTag,
                        onTagClick = { viewModel.selectTag(it) }
                    )
                }

                if (uiState.notes.isEmpty()) {
                    EmptyState(
                        isSearching = uiState.isSearching,
                        onNewNote = { onNewNote(uiState.selectedFolderId) }
                    )
                } else {
                    if (uiState.viewMode == ViewMode.LIST) {
                        NotesList(
                            notes = uiState.notes,
                            onNoteClick = onNoteClick,
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onPinNote = { viewModel.togglePinNote(it) }
                        )
                    } else {
                        NotesGrid(
                            notes = uiState.notes,
                            onNoteClick = onNoteClick,
                            onDeleteNote = { viewModel.deleteNote(it) }
                        )
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name, color ->
                viewModel.createFolder(name, color)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onViewToggle: () -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewMode: ViewMode,
    isSyncing: Boolean,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch { drawerState.open() }
            }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Cari")
            }
            IconButton(onClick = onViewToggle) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Ganti tampilan"
                )
            }
            IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = "Sinkronisasi")
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query.trim(),
                onValueChange = onQueryChange,
                placeholder = { Text("Cari catatan...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Tutup pencarian")
            }
        }
    )
}

@Composable
private fun SideDrawer(
    folders: List<Folder>,
    tags: List<com.personalnotes.app.domain.model.Tag>,
    selectedFolderId: Long?,
    selectedTag: String?,
    onFolderClick: (Long?) -> Unit,
    onTagClick: (String?) -> Unit,
    onAllNotesClick: () -> Unit,
    onNewFolder: () -> Unit,
    onDeleteFolder: (Folder) -> Unit
) {
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "PersonalNotes",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Divider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Notes, contentDescription = null) },
            label = { Text("Semua Catatan") },
            selected = selectedFolderId == null && selectedTag == null,
            onClick = onAllNotesClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FOLDER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNewFolder, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Folder baru", modifier = Modifier.size(16.dp))
            }
        }

        folders.forEach { folder ->
            var showMenu by remember { mutableStateOf(false) }
            NavigationDrawerItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseColor(folder.color))
                    )
                },
                label = { Text(folder.name) },
                selected = selectedFolderId == folder.id,
                onClick = { onFolderClick(folder.id) },
                badge = {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Hapus folder") },
                            onClick = { onDeleteFolder(folder); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("TAG", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

            tags.forEach { tag ->
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = null, tint = parseColor(tag.color)) },
                    label = { Text(tag.name) },
                    selected = selectedTag == tag.name,
                    onClick = { onTagClick(tag.name) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}

@Composable
private fun TagChipsRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTagClick(if (selectedTag == tag) null else tag) },
                label = { Text(tag) },
                leadingIcon = if (selectedTag == tag) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onPinNote: (Note) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteListItem(note = note, onClick = { onNoteClick(note.id) },
                onDelete = { onDeleteNote(note) }, onPin = { onPinNote(note) })
        }
    }
}

@Composable
private fun NotesGrid(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            NoteGridItem(note = note, onClick = { onNoteClick(note.id) }, onDelete = { onDeleteNote(note) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit, onPin: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(note.title.ifBlank { "Tanpa judul" }, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall)
                }
                if (note.content.isNotBlank()) {
                    Text(note.content.take(100), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.tags.take(3).forEach { tag ->
                        SuggestionChip(onClick = {}, label = { Text(tag, fontSize = 10.sp) },
                            modifier = Modifier.height(20.dp))
                    }
                }
                Text(
                    note.updatedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (note.isPinned) "Lepas pin" else "Pin catatan") },
                        onClick = { onPin(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteGridItem(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(note.title.ifBlank { "Tanpa judul" }, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall)
            if (note.content.isNotBlank()) {
                Text(note.content.take(80), maxLines = 3, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                note.updatedAt.format(DateTimeFormatter.ofPattern("dd MMM")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean, onNewNote: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearching) Icons.Outlined.SearchOff else Icons.Outlined.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isSearching) "Catatan tidak ditemukan" else "Belum ada catatan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNewNote) { Text("Buat catatan pertama") }
        }
    }
}

@Composable
private fun NewFolderDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#6200EE", "#03DAC6", "#FF6200", "#4CAF50", "#2196F3", "#FF4081")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder baru") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama folder") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Text("Warna", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColor(color))
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()) { Text("Buat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun currentTitle(state: HomeUiState): String = when {
    state.selectedTag != null -> "#${state.selectedTag}"
    else -> "PersonalNotes"
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6200EE)
}
