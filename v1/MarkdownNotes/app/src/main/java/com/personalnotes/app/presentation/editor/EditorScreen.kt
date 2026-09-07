package com.personalnotes.app.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long?,
    defaultFolderId: Long?,
    onBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, defaultFolderId) {
        viewModel.initialize(noteId, defaultFolderId)
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                isSaved = uiState.isSaved,
                isPreview = uiState.isPreviewMode,
                isSplit = uiState.isSplitMode,
                hasBacklinks = uiState.backlinks.isNotEmpty(),
                onBack = {
                    viewModel.saveNote()
                    onBack()
                },
                onTogglePreview = { viewModel.togglePreviewMode() },
                onToggleSplit = { viewModel.toggleSplitMode() },
                onToggleTags = { viewModel.toggleTagPanel() },
                onToggleBacklinks = { viewModel.toggleBacklinkPanel() },
                onExportClick = { showExportMenu = true },
                showExportMenu = showExportMenu,
                onDismissExport = { showExportMenu = false },
                onExportPdf = { viewModel.exportToPdf(context); showExportMenu = false },
                onExportHtml = { viewModel.exportToHtml(context); showExportMenu = false }
            )
        },
        bottomBar = {
            Column {
                // Tag panel
                if (uiState.showTagPanel) {
                    TagPanel(
                        currentTags = uiState.note.tags,
                        availableTags = uiState.availableTags,
                        onAddTag = { viewModel.addTag(it) },
                        onRemoveTag = { viewModel.removeTag(it) }
                    )
                }
                // Backlink panel
                if (uiState.showBacklinkPanel && uiState.backlinks.isNotEmpty()) {
                    BacklinkPanel(
                        backlinks = uiState.backlinks,
                        onNoteClick = onNavigateToNote
                    )
                }
                // Markdown toolbar
                if (!uiState.isPreviewMode) {
                    MarkdownToolbar(onInsert = { viewModel.insertMarkdown(it) })
                }
                // Status bar
                EditorStatusBar(
                    wordCount = uiState.wordCount,
                    charCount = uiState.charCount,
                    isSaved = uiState.isSaved,
                    exportStatus = uiState.exportStatus
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Title field
            BasicTextField(
                value = uiState.note.title,
                onValueChange = { viewModel.onTitleChange(it) },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (uiState.note.title.isEmpty()) {
                            Text("Judul catatan...", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Editor / Preview / Split
            when {
                uiState.isSplitMode -> SplitView(
                    content = uiState.note.content,
                    onContentChange = { viewModel.onContentChange(it) }
                )
                uiState.isPreviewMode -> MarkdownPreview(
                    content = uiState.note.content,
                    modifier = Modifier.fillMaxSize()
                )
                else -> MarkdownEditor(
                    content = uiState.note.content,
                    onContentChange = { viewModel.onContentChange(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MarkdownEditor(content: String, onContentChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box(modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())) {
                if (content.isEmpty()) {
                    Text("Mulai menulis dengan Markdown...", fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        },
        modifier = modifier
    )
}

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    AndroidView(
        factory = { ctx ->
            android.widget.TextView(ctx).apply {
                textSize = 15f
                setPadding(48, 24, 48, 24)
                setTextColor(android.graphics.Color.parseColor(
                    if (ctx.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES) "#FFFFFF" else "#1A1A1A"
                ))
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, content.ifBlank { "*Preview akan muncul di sini*" })
        },
        modifier = modifier.verticalScroll(rememberScrollState())
    )
}

@Composable
fun SplitView(content: String, onContentChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        MarkdownEditor(
            content = content,
            onContentChange = onContentChange,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        MarkdownPreview(content = content, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    isSaved: Boolean,
    isPreview: Boolean,
    isSplit: Boolean,
    hasBacklinks: Boolean,
    onBack: () -> Unit,
    onTogglePreview: () -> Unit,
    onToggleSplit: () -> Unit,
    onToggleTags: () -> Unit,
    onToggleBacklinks: () -> Unit,
    onExportClick: () -> Unit,
    showExportMenu: Boolean,
    onDismissExport: () -> Unit,
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit
) {
    TopAppBar(
        title = {
            if (!isSaved) {
                Text("Menyimpan...", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
        },
        actions = {
            // Split view
            IconButton(onClick = onToggleSplit) {
                Icon(
                    if (isSplit) Icons.Default.VerticalSplit else Icons.Outlined.VerticalSplit,
                    contentDescription = "Split view",
                    tint = if (isSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            // Preview toggle
            IconButton(onClick = onTogglePreview) {
                Icon(
                    if (isPreview) Icons.Default.EditNote else Icons.Default.Preview,
                    contentDescription = "Toggle preview",
                    tint = if (isPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            // Tags
            IconButton(onClick = onToggleTags) {
                Icon(Icons.Default.Label, contentDescription = "Tag")
            }
            // Backlinks
            IconButton(onClick = onToggleBacklinks) {
                BadgedBox(badge = {
                    // badge shown if there are backlinks
                }) {
                    Icon(Icons.Default.Link, contentDescription = "Backlink",
                        tint = if (hasBacklinks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
            // Export
            Box {
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                }
                DropdownMenu(expanded = showExportMenu, onDismissRequest = onDismissExport) {
                    DropdownMenuItem(
                        text = { Text("Export ke PDF") },
                        onClick = onExportPdf,
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export ke HTML") },
                        onClick = onExportHtml,
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                }
            }
        }
    )
}

@Composable
private fun MarkdownToolbar(onInsert: (String) -> Unit) {
    val tools = listOf(
        "H1" to "# ",
        "H2" to "## ",
        "B" to "**teks**",
        "I" to "*teks*",
        "~~" to "~~teks~~",
        "`" to "`kode`",
        "```" to "\n```\n\n```",
        ">" to "\n> ",
        "-" to "\n- ",
        "[]" to "- [ ] ",
        "🔗" to "[judul](url)",
        "[[" to "[[",
        "---" to "\n---\n"
    )

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tools.forEach { (label, syntax) ->
                TextButton(
                    onClick = { onInsert(syntax) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TagPanel(
    currentTags: List<String>,
    availableTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var newTag by remember { mutableStateOf("") }

    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // Current tags
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                currentTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // Add tag
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    placeholder = { Text("Tag baru...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = TextStyle(fontSize = 13.sp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(newTag.trim().lowercase())
                        newTag = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah tag")
                }
            }

            // Suggestions
            val suggestions = availableTags.filter { it !in currentTags && it.contains(newTag, ignoreCase = true) }.take(5)
            if (suggestions.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(onClick = { onAddTag(suggestion) }, label = { Text(suggestion, fontSize = 11.sp) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BacklinkPanel(backlinks: List<com.personalnotes.app.domain.model.Note>, onNoteClick: (Long) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Backlink (${backlinks.size})", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                backlinks.forEach { note ->
                    AssistChip(
                        onClick = { onNoteClick(note.id) },
                        label = { Text(note.title.ifBlank { "Tanpa judul" }, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorStatusBar(wordCount: Int, charCount: Int, isSaved: Boolean, exportStatus: String?) {
    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                exportStatus ?: "$wordCount kata · $charCount karakter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    if (isSaved) Icons.Default.CloudDone else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Text(
                    if (isSaved) "Tersimpan" else "Belum tersimpan",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
