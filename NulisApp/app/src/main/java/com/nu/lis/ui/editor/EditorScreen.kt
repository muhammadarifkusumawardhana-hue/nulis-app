package com.nu.lis.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: EditorViewModel,
    lastHomeExitTime: Long = 0L,
    onBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    
    val settings = com.nu.lis.theme.LocalAppSettings.current
    val isIndo = settings.language == "id"
    
    var showPinDialog by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showExportMenu by remember { mutableStateOf(false) }
    var showAiMaintenanceDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<String?>(null) }
    var showTagOptions by remember { mutableStateOf(false) }
    var showImageMenu by remember { mutableStateOf(false) }
    var showUnlockActionPinDialog by remember { mutableStateOf(false) }
    var showLockSettingsRedirectDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }

    // Logic for initial lock state based on settings
    LaunchedEffect(Unit) {
        if (state.isLocked) {
            val shouldBeLocked = when {
                settings.lockTimeoutSeconds == -1 -> {
                    // "App Closed" - always lock on entry (since VM is newly created or app restarted)
                    true
                }
                settings.lockTimeoutSeconds == 0 -> {
                    // "Note Closed" - always lock when opening
                    true
                }
                settings.lockTimeoutSeconds > 0 -> {
                    // Time based - check if last exit from Home was long enough ago
                    val elapsedSeconds = (System.currentTimeMillis() - lastHomeExitTime) / 1000
                    elapsedSeconds >= settings.lockTimeoutSeconds
                }
                else -> true
            }
            
            if (shouldBeLocked) {
                vm.setAuthenticated(false)
                showPinDialog = true
            } else {
                vm.setAuthenticated(true)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.insertImage(it.toString()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val path = android.provider.MediaStore.Images.Media.insertImage(context.contentResolver, it, "NoteImage_${System.currentTimeMillis()}", null)
            if (path != null) {
                vm.insertImage(path)
            }
        }
    }

    LaunchedEffect(state.isLocked) {
        if (state.isLocked && !state.isAuthenticated) {
            showPinDialog = true
        }
    }

    if (showPinDialog) {
        LockPinDialog(
            isIndo = isIndo,
            correctPin = settings.noteLockPin,
            onSuccess = {
                vm.setAuthenticated(true)
                showPinDialog = false
            },
            onDismiss = {
                onBack()
            }
        )
    }

    BackHandler {
        vm.saveNow()
        onBack()
    }

    if (state.isLoading || (state.isLocked && !state.isAuthenticated)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    // lastInteractionTime = System.currentTimeMillis()
                }
            },
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
                            style = MaterialTheme.typography.labelLarge,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.togglePreview() }) {
                        Icon(if (state.isPreview) Icons.Default.Edit else Icons.Default.Visibility, null)
                    }
                    IconButton(onClick = { 
                        if (settings.isNoteLockEnabled && settings.noteLockPin.isNotBlank()) {
                            if (state.isLocked) {
                                showUnlockActionPinDialog = true
                            } else {
                                vm.toggleLock() 
                            }
                        } else {
                            showLockSettingsRedirectDialog = true
                        }
                    }) {
                        Icon(if (state.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null)
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isIndo) "Ekspor ke PDF" else "Export to PDF") },
                                onClick = {
                                    showExportMenu = false
                                    showPdfExportDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!state.isPreview) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showTagInput = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Label, null)
                        }
                        
                        VerticalDivider(
                            modifier = Modifier.padding(vertical = 16.dp).height(20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Shortcuts
                        IconButton(onClick = { vm.insertSnippet("**") }) { Icon(Icons.Default.FormatBold, null) }
                        IconButton(onClick = { vm.insertSnippet("*") }) { Icon(Icons.Default.FormatItalic, null) }
                        IconButton(onClick = { vm.insertSnippet("# ") }) { Icon(Icons.Default.Title, null) }
                        IconButton(onClick = { vm.insertSnippet("\n- ") }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null) }
                        IconButton(onClick = { vm.insertSnippet("\n1. ") }) { Icon(Icons.Default.FormatListNumbered, null) }
                        IconButton(onClick = { vm.insertSnippet("> ") }) { Icon(Icons.Default.FormatQuote, null) }
                        IconButton(onClick = { vm.insertSnippet("`") }) { Icon(Icons.Default.Code, null) }
                        IconButton(onClick = { vm.insertSnippet("---") }) { Icon(Icons.Default.HorizontalRule, null) }
                        IconButton(onClick = { vm.insertSnippet("[") }) { Icon(Icons.Default.Link, null) }
                        IconButton(onClick = { showImageMenu = true }) { Icon(Icons.Default.Image, null) }
                        
                        VerticalDivider(
                            modifier = Modifier.padding(vertical = 16.dp).height(20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Tombol AI Gemini (Coming Soon)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text(if (isIndo) "Fitur AI (Segera Hadir)" else "AI Feature (Coming Soon)")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                    ) {
                                        Text(
                                            "Soon", 
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showAiMaintenanceDialog = true },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, "Ask AI")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tags Chip list
            if (state.tags.isNotEmpty() || showTagInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tags.forEach { tag ->
                        AssistChip(
                            onClick = { 
                                tagToEdit = tag
                                showTagOptions = true
                            },
                            label = { Text(tag) },
                            leadingIcon = { Icon(Icons.Default.Tag, null, Modifier.size(16.dp)) }
                        )
                    }
                    if (showTagInput) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            modifier = Modifier.width(120.dp),
                            placeholder = { Text("Tag...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (tagInput.isNotBlank()) {
                                        vm.addTag(tagInput.trim())
                                        tagInput = ""
                                        showTagInput = false
                                    }
                                }) {
                                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }

            if (state.isPreview) {
                MarkdownPreview(
                    content = state.content,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = state.title,
                        onValueChange = { vm.onTitleChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (state.title.isEmpty()) {
                                Text(
                                    if (isIndo) "Judul" else "Title",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            innerTextField()
                        }
                    )

                    BasicTextField(
                        value = state.contentValue,
                        onValueChange = { vm.onContentChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (state.content.isEmpty()) {
                                Text(
                                    if (isIndo) "Mulai menulis..." else "Start writing...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }

    if (showUnlockActionPinDialog) {
        LockPinDialog(
            isIndo = isIndo,
            correctPin = settings.noteLockPin,
            onSuccess = {
                showUnlockActionPinDialog = false
                vm.toggleLock()
            },
            onDismiss = { showUnlockActionPinDialog = false }
        )
    }

    if (showLockSettingsRedirectDialog) {
        AlertDialog(
            onDismissRequest = { showLockSettingsRedirectDialog = false },
            title = { Text(if (isIndo) "Kunci Catatan Mati" else "Note Lock Disabled") },
            text = {
                Text(
                    if (isIndo) 
                        "Anda harus mengaktifkan 'Kunci Catatan' dan mengatur PIN di Pengaturan terlebih dahulu untuk menggunakan fitur ini."
                    else 
                        "You must enable 'Note Lock' and set a PIN in Settings first to use this feature."
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showLockSettingsRedirectDialog = false
                }) { Text("OK") }
            }
        )
    }

    if (showPdfExportDialog) {
        AlertDialog(
            onDismissRequest = { showPdfExportDialog = false },
            title = { Text(if (isIndo) "Ekspor ke PDF" else "Export to PDF") },
            text = { 
                Text(
                    if (isIndo) "Untuk saat ini ekspor ke PDF hanya support teks saja tanpa gambar. Apakah kamu ingin lanjut?" 
                    else "Currently, exporting to PDF only supports text without images. Do you want to continue?"
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    showPdfExportDialog = false
                    vm.exportToPdf(context)
                }) {
                    Text(if (isIndo) "Saya Mengerti" else "I Understand")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfExportDialog = false }) {
                    Text(if (isIndo) "Batalkan" else "Cancel")
                }
            }
        )
    }

    if (showImageMenu) {
        AlertDialog(
            onDismissRequest = { showImageMenu = false },
            title = { Text(if (isIndo) "Tambah Gambar" else "Add Image") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (isIndo) "Galeri" else "Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showImageMenu = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    ListItem(
                        headlineContent = { Text(if (isIndo) "Kamera" else "Camera") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showImageMenu = false
                            cameraLauncher.launch(null)
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (showAiMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showAiMaintenanceDialog = false },
            title = { Text(if (isIndo) "Fitur AI" else "AI Feature") },
            text = {
                Text(
                    if (isIndo)
                        "Fitur belum tersedia saat ini. Dukung developer untuk tetap terus mengembangkan produknya, ya!"
                    else
                        "This feature is not available yet. Please support the developer to keep developing this product!"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAiMaintenanceDialog = false }) {
                    Text(if (isIndo) "Siap!" else "Got it!")
                }
            }
        )
    }

    if (showTagOptions && tagToEdit != null) {
        AlertDialog(
            onDismissRequest = { showTagOptions = false },
            title = { Text(if (isIndo) "Opsi Tag: #$tagToEdit" else "Tag Options: #$tagToEdit") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (isIndo) "Hapus" else "Remove") },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            vm.removeTag(tagToEdit!!)
                            showTagOptions = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagOptions = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(object : LinkResolver {
                        override fun resolve(view: android.view.View, link: String) {
                            // Intercept image or other links here if needed
                        }
                    })
                }
            })
            .build()
    }

    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        AndroidView(
            factory = { ctx ->
                val textView = android.widget.TextView(ctx).apply {
                    textSize = 14f
                    setLineSpacing(0f, 1.5f)
                    setPadding(60, 32, 60, 64)
                    movementMethod = android.text.method.LinkMovementMethod.getInstance()
                }
                android.widget.ScrollView(ctx).apply {
                    addView(textView)
                    clipToPadding = true
                }
            },
            update = { scrollView ->
                val tv = scrollView.getChildAt(0) as android.widget.TextView
                tv.setTextColor(textColor)
                tv.setLinkTextColor(linkColor)
                markwon.setMarkdown(tv, content.ifBlank { "*Start writing to see preview...*" })
            },
            modifier = Modifier.fillMaxSize().clipToBounds()
        )
    }
}

@Composable
private fun LockPinDialog(
    isIndo: Boolean,
    correctPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = com.nu.lis.theme.LocalAppSettings.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val biometricManager = remember { androidx.biometric.BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isIndo) "Verifikasi Biometrik" else "Biometric Verification")
            .setSubtitle(if (isIndo) "Gunakan sidik jari untuk membuka catatan" else "Use fingerprint to unlock note")
            .setNegativeButtonText(if (isIndo) "Gunakan PIN" else "Use PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    LaunchedEffect(Unit) {
        if (settings.isBiometricEnabled && canAuthenticate) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isIndo) "Masukkan PIN" else "Enter PIN",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isIndo) "Catatan ini dikunci" else "This note is locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(24.dp))
                
                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < pin.length)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                
                if (error) {
                    Text(
                        if (isIndo) "PIN salah" else "Incorrect PIN",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Number Pad
                val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    numbers.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { num ->
                                if (num.isEmpty()) {
                                    Spacer(Modifier.size(64.dp))
                                } else {
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (num == "del") {
                                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                    error = false
                                                } else if (pin.length < 4) {
                                                    pin += num
                                                    if (pin.length == 4) {
                                                        if (pin == correctPin) {
                                                            onSuccess()
                                                        } else {
                                                            error = true
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (num == "del") {
                                            Icon(Icons.AutoMirrored.Filled.Backspace, null)
                                        } else {
                                            Text(num, style = MaterialTheme.typography.titleLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                TextButton(onClick = onDismiss) {
                    Text(if (isIndo) "Batal" else "Cancel")
                }
            }
        }
    }
}
