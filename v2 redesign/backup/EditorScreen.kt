package com.personalnotes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showTagInput by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }
    var showGeminiPrompt by remember { mutableStateOf(false) }

    val settings = com.personalnotes.theme.LocalAppSettings.current
    val isIndo = settings.language == "id"

    BackHandler {
        vm.saveNow()
        onBack()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (state.isPreview)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (state.isPreview) "PREVIEW" else "EDITOR",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isPreview)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.saveNow()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            if (isIndo) "Kembali" else "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { vm.togglePreview() }) {
                        Text(
                            if (state.isPreview) (if (isIndo) "Sunting" else "Edit") else "Preview",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showGeminiPrompt = true }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            "Gemini",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showTagInput = !showTagInput }) {
                        Icon(
                            if (showTagInput) Icons.Filled.Label else Icons.AutoMirrored.Outlined.Label,
                            "Tag",
                            tint = if (showTagInput) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(
                                Icons.Outlined.Share, "Export",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isIndo) "Ekspor ke HTML" else "Export to HTML") },
                                onClick = {
                                    vm.saveNow()
                                    vm.exportToHtml(context)
                                    showExportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Code, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (showTagInput) {
                    TagPanel(
                        currentTags = state.tags,
                        allTags = state.allTags,
                        input = tagInput,
                        onInputChange = { tagInput = it },
                        onAdd = {
                            vm.addTag(tagInput)
                            tagInput = ""
                        },
                        onRemove = { vm.removeTag(it) },
                        isIndo = isIndo
                    )
                }
                if (!state.isPreview) {
                    MarkdownToolbar(onInsert = { vm.insertSnippet(it) })
                }
                EditorStatusBar(
                    wordCount = state.wordCount,
                    charCount = state.content.length,
                    isSaved = state.isSaved,
                    isIndo = isIndo
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                BasicTextField(
                    value = state.title,
                    onValueChange = { vm.onTitleChange(it) },
                    enabled = !state.isPreview,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            if (state.title.isEmpty()) {
                                Text(
                                    if (isIndo) "Judul..." else "Title...",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "#$tag",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (state.isPreview) {
                    MarkdownPreview(
                        content = state.content,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    BasicTextField(
                        value = state.content,
                        onValueChange = { vm.onContentChange(it) },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 26.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                if (state.content.isEmpty()) {
                                    Text(
                                        if (isIndo) "Mulai menulis...\n\n# Judul\n**tebal** *miring*\n- daftar\n[[tautan]]"
                                        else "Start writing...\n\n# Heading\n**bold** *italic*\n- list item\n[[backlink]]",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 26.sp
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Gemini Response Overlay
            if (state.geminiResult != null || state.isGeminiLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { if (!state.isGeminiLoading) vm.dismissGemini() },
                    contentAlignment = Alignment.Center
                ) {
                    GeminiResponseCard(
                        result = state.geminiResult,
                        isLoading = state.isGeminiLoading,
                        onDismiss = { vm.dismissGemini() },
                        onCopy = {
                            val cm =
                                context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "Gemini",
                                    state.geminiResult
                                )
                            )
                        },
                        onRegenerate = { vm.regenerateGemini() },
                        onInsert = {
                            vm.insertSnippet(state.geminiResult ?: "")
                            vm.dismissGemini()
                        },
                        onOpenInNew = {
                            vm.createNewNoteFromGemini { newId ->
                                vm.dismissGemini()
                                onNavigateToNote(newId)
                            }
                        },
                        isIndo = isIndo
                    )
                }
            }
        }
    }

    if (showGeminiPrompt) {
        GeminiPromptDialog(
            onDismiss = { showGeminiPrompt = false },
            onConfirm = {
                vm.askGemini(it)
                showGeminiPrompt = false
            },
            isIndo = isIndo
        )
    }
}

@Composable
private fun GeminiPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isIndo: Boolean
) {
    var text by remember { mutableStateOf("") }
    val suggestions = if (isIndo) listOf(
        "Rangkum catatan ini",
        "Perbaiki tata bahasa",
        "Lanjutkan tulisan saya",
        "Buat daftar poin penting"
    ) else listOf(
        "Summarize this note",
        "Fix grammar",
        "Continue my writing",
        "Make a bullet point list"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(if (isIndo) "Tanya Gemini" else "Ask Gemini")
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(if (isIndo) "Berikan instruksi..." else "Enter instructions...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isIndo) "Saran cepat:" else "Quick suggestions:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { s ->
                        SuggestionChip(
                            onClick = { onConfirm(s) },
                            label = { Text(s, fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isIndo) "Kirim" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isIndo) "Batal" else "Cancel")
            }
        }
    )
}

@Composable
private fun GeminiResponseCard(
    result: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onInsert: () -> Unit,
    onOpenInNew: () -> Unit,
    isIndo: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable(enabled = false) {}, // Prevent clicks through to background
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
        ),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Gemini AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onOpenInNew, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (result != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onInsert,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isIndo) "Masukkan ke Catatan" else "Insert into Note")
                }
            }
        }
    }
}

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    AndroidView(
        factory = { ctx ->
            android.widget.ScrollView(ctx).apply {
                addView(android.widget.TextView(ctx).apply {
                    textSize = 16f
                    setLineSpacing(0f, 1.6f)
                    setPadding(60, 32, 60, 64)
                    tag = "md_tv"
                })
            }
        },
        update = { sv ->
            val tv = sv.findViewWithTag<android.widget.TextView>("md_tv")
            tv.setTextColor(textColor)
            tv.setLinkTextColor(linkColor)
            markwon.setMarkdown(tv, content.ifBlank { "*Start writing to see preview...*" })
        },
        modifier = modifier
    )
}

@Composable
private fun TagPanel(
    currentTags: List<String>,
    allTags: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    isIndo: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tags", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (currentTags.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { onRemove(tag) },
                            label = { Text(tag, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close, "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            if (isIndo) "Tambah tag..." else "Add tag...",
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    textStyle = TextStyle(fontSize = 13.sp)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onAdd,
                    enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }

            val suggestions = allTags
                .filter { it !in currentTags && (input.isBlank() || it.contains(input, true)) }
                .take(5)
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { s ->
                        SuggestionChip(
                            onClick = {
                                onInputChange(s)
                                onAdd()
                            },
                            label = { Text(s, fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownToolbar(onInsert: (String) -> Unit) {
    val items = listOf(
        "B" to "**teks**",
        "I" to "*teks*",
        "~~" to "~~teks~~",
        "H1" to "# ",
        "H2" to "## ",
        "H3" to "### ",
        "`" to "`kode`",
        "```" to "\n```\nkode\n```\n",
        ">" to "\n> ",
        "-" to "\n- ",
        "☑" to "\n- [ ] ",
        "[[" to "[[",
        "🔗" to "[judul](url)",
        "—" to "\n---\n"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, syntax) ->
                TextButton(
                    onClick = { onInsert(syntax) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EditorStatusBar(wordCount: Int, charCount: Int, isSaved: Boolean, isIndo: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isIndo) "$wordCount kata · $charCount karakter" else "$wordCount words · $charCount characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isSaved) Icons.Default.CloudDone else Icons.Default.Edit,
                    null,
                    modifier = Modifier.size(13.dp),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
                Text(
                    if (isSaved) (if (isIndo) "Tersimpan" else "Saved") else (if (isIndo) "Menyunting..." else "Editing..."),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSaved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
