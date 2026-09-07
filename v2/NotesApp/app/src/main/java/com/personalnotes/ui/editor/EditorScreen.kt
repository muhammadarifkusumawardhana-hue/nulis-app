package com.personalnotes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showTagInput by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }

    // Simpan saat back
    BackHandler {
        vm.saveNow()
        onBack()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isSaved) "Tersimpan" else "Mengetik...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.saveNow(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    // Preview toggle
                    IconButton(onClick = { vm.togglePreview() }) {
                        Icon(
                            if (state.isPreview) Icons.Default.EditNote else Icons.Default.Visibility,
                            contentDescription = if (state.isPreview) "Mode edit" else "Mode preview",
                            tint = if (state.isPreview) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Tags
                    IconButton(onClick = { showTagInput = !showTagInput }) {
                        Icon(Icons.Default.Label, "Tag",
                            tint = if (showTagInput) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface)
                    }
                    // Export
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export ke HTML") },
                                onClick = {
                                    vm.saveNow()
                                    vm.exportToHtml(context)
                                    showExportMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Code, null) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Tag panel
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
                        onRemove = { vm.removeTag(it) }
                    )
                }
                // Markdown toolbar (hanya di mode edit)
                if (!state.isPreview) {
                    MarkdownToolbar(onInsert = { vm.insertSnippet(it) })
                }
                // Status bar
                Surface(tonalElevation = 1.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${state.wordCount} kata · ${state.content.length} karakter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            if (state.isSaved) Icons.Default.Cloud else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (state.isSaved) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            // Title
            BasicTextField(
                value = state.title,
                onValueChange = { vm.onTitleChange(it) },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { inner ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (state.title.isEmpty()) {
                            Text(
                                "Judul...",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Content: Editor atau Preview
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
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (state.content.isEmpty()) {
                                Text(
                                    "Mulai menulis dalam Markdown...\n\n" +
                                    "# Judul besar\n## Judul kecil\n**tebal** *miring*\n- item list\n[[catatan lain]]",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
                    textSize = 15f
                    setPadding(48, 32, 48, 32)
                    tag = "markwon_tv"
                })
            }
        },
        update = { scrollView ->
            val tv = scrollView.findViewWithTag<android.widget.TextView>("markwon_tv")
            markwon.setMarkdown(tv, content.ifBlank { "*Preview akan muncul di sini...*" })
            // Sesuaikan warna teks dengan tema
            val nightMode = scrollView.context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val textColor = if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1C1B1F")
            tv.setTextColor(textColor)
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
    onRemove: (String) -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))

            // Current tags
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
                                    Icons.Default.Close, "Hapus",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Tambah tag...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = TextStyle(fontSize = 13.sp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onAdd, enabled = input.isNotBlank()) {
                    Icon(Icons.Default.Add, "Tambah")
                }
            }

            // Suggestions
            val suggestions = allTags
                .filter { it !in currentTags && (input.isBlank() || it.contains(input, ignoreCase = true)) }
                .take(5)
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { s ->
                        SuggestionChip(
                            onClick = {
                                onInputChange(s)
                                onAdd()
                            },
                            label = { Text(s, fontSize = 11.sp) }
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
        "H1" to "# ",
        "H2" to "## ",
        "H3" to "### ",
        "B" to "**teks**",
        "I" to "*teks*",
        "~~" to "~~teks~~",
        "`" to "`kode`",
        "```" to "\n```\nkode\n```\n",
        ">" to "\n> ",
        "—" to "\n---\n",
        "-" to "\n- item",
        "☑" to "\n- [ ] tugas",
        "[[" to "[[",
        "🔗" to "[judul](url)"
    )

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { (label, syntax) ->
                TextButton(
                    onClick = { onInsert(syntax) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
