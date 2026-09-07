package com.personalnotes.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.personalnotes.data.FolderEntity
import com.personalnotes.data.NoteEntity
import com.personalnotes.data.TagEntity
import com.personalnotes.data.tags
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long) -> Unit   // folderId, -1L = no folder
) {
    val notes         by vm.notes.collectAsState()
    val folders       by vm.folders.collectAsState()
    val tags          by vm.tags.collectAsState()
    val selectedFolder by vm.selectedFolderId.collectAsState()
    val selectedTag   by vm.selectedTag.collectAsState()
    val searchQuery   by vm.searchQuery.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    var showNewFolderDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                folders = folders,
                tags = tags,
                selectedFolder = selectedFolder,
                selectedTag = selectedTag,
                onAllNotes = { vm.clearAll() },
                onFolder = { vm.selectFolder(it) },
                onTag = { vm.selectTag(it) },
                onNewFolder = { showNewFolderDialog = true },
                onDeleteFolder = { vm.deleteFolder(it) }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when {
                            selectedTag != null -> "#$selectedTag"
                            selectedFolder != null ->
                                folders.find { it.id == selectedFolder }?.name ?: "Folder"
                            else -> "PersonalNotes"
                        }
                        Text(title, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* search handled below */ }) {
                            Icon(Icons.Default.Search, "Cari")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNewNote(selectedFolder ?: -1L) }
                ) {
                    Icon(Icons.Default.Add, "Catatan baru")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vm.setSearch(it) },
                    placeholder = { Text("Cari catatan...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { vm.setSearch("") }) {
                                Icon(Icons.Default.Clear, "Hapus")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                // Tag chips
                if (tags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag.name,
                                onClick = {
                                    vm.selectTag(if (selectedTag == tag.name) null else tag.name)
                                },
                                label = { Text("#${tag.name}", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Notes list
                if (notes.isEmpty()) {
                    EmptyState(onNewNote = { onNewNote(selectedFolder ?: -1L) })
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onOpenNote(note.id) },
                                onDelete = { vm.deleteNote(note.id) },
                                onPin = { vm.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name, color ->
                vm.createFolder(name, color)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val date = try {
        LocalDateTime.parse(note.updatedAt).format(fmt)
    } catch (e: Exception) { "" }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        note.title.ifBlank { "Tanpa judul" },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.content.isNotBlank()) {
                    Text(
                        note.content.take(120),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Tags
                val tagList = note.tags()
                if (tagList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tagList.take(4).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "#$tag",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (note.isPinned) "Lepas pin" else "Pin") },
                        onClick = { onPin(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PushPin, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
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
    onAllNotes: () -> Unit,
    onFolder: (Long) -> Unit,
    onTag: (String) -> Unit,
    onNewFolder: () -> Unit,
    onDeleteFolder: (Long) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            "PersonalNotes",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Notes, null) },
            label = { Text("Semua Catatan") },
            selected = selectedFolder == null && selectedTag == null,
            onClick = onAllNotes,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FOLDER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNewFolder, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(18.dp))
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
                            .background(parseColor(folder.colorHex))
                    )
                },
                label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = selectedFolder == folder.id,
                onClick = { onFolder(folder.id) },
                badge = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hapus folder") },
                                onClick = { onDeleteFolder(folder.id); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "TAG",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            tags.forEach { tag ->
                NavigationDrawerItem(
                    icon = {
                        Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    label = { Text("#${tag.name}") },
                    selected = selectedTag == tag.name,
                    onClick = { onTag(tag.name) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onNewNote: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Belum ada catatan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNewNote) { Text("Buat catatan baru") }
    }
}

@Composable
private fun NewFolderDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colorOptions = listOf("#6650A4","#B23A48","#2D6A4F","#1565C0","#E65100","#6D4C41")
    var selectedColor by remember { mutableStateOf(colorOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama folder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Pilih warna", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(parseColor(c))
                                .clickable { selectedColor = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == c) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Buat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6650A4)
}
