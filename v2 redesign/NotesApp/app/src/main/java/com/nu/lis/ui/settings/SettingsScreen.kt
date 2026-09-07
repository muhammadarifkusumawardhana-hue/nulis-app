package com.nu.lis.ui.settings

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.nu.lis.theme.LocalAppSettings
import com.nu.lis.theme.MonoBlack
import com.nu.lis.theme.MonoWhite
import com.nu.lis.theme.Sage600
import com.nu.lis.theme.Terracotta
import com.nu.lis.util.GoogleDriveService

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    isIndo: Boolean
) {
    val vm: SettingsViewModel = viewModel()
    val context = LocalContext.current
    val driveService = remember { GoogleDriveService(context) }
    val appSettings = LocalAppSettings.current
    
    val isLoading by vm.isLoading.collectAsState()
    val lastBackup by vm.lastBackup.collectAsState()
    val status by vm.backupStatus.collectAsState()
    
    var showRestoreConfirm by remember { mutableStateOf(false) }
    
    // States for settings dialogs
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showLockSettingsDialog by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, if (isIndo) "Terhubung ke Google Drive" else "Connected to Google Drive", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(status) {
        status?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearStatus()
        }
    }

    if (showCredits) {
        CreditsDialog(onDismiss = { showCredits = false }, isIndo = isIndo)
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(if (isIndo) "Pulihkan Data?" else "Restore Data?") },
            text = { Text(if (isIndo) "Data lokal Anda akan digantikan dengan data dari backup. Aplikasi akan otomatis memuat ulang. Lanjutkan?" else "Your local data will be replaced by backup data. The app will automatically restart. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        vm.performRestore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isIndo) "Ya, Pulihkan" else "Yes, Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text(if (isIndo) "Batal" else "Cancel")
                }
            }
        )
    }

    if (isLoading) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(if (isIndo) "Memproses..." else "Processing...")
                }
            },
            confirmButton = {}
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = {
            Text(if (isIndo) "Pengaturan" else "Settings")
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- GENERAL SETTINGS ---
                Text(
                    if (isIndo) "UMUM" else "GENERAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                // Font Theme
                SettingItem(
                    title = if (isIndo) "Tema Font" else "Font Theme",
                    subtitle = appSettings.fontTheme,
                    onClick = { showFontDialog = true }
                )

                // Dark Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isIndo) "Mode Gelap" else "Dark Mode", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = appSettings.darkMode, onCheckedChange = { appSettings.darkMode = it })
                }
                
                // Note Lock
                SettingItem(
                    title = if (isIndo) "Kunci Catatan" else "Note Lock",
                    subtitle = if (appSettings.isNoteLockEnabled) (if (isIndo) "Aktif" else "Enabled") else (if (isIndo) "Nonaktif" else "Disabled"),
                    onClick = { showLockSettingsDialog = true },
                    trailing = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline) }
                )

                // Language
                SettingItem(
                    title = if (isIndo) "Bahasa" else "Language",
                    subtitle = if (appSettings.language == "en") "English" else "Indonesia",
                    onClick = { showLanguageDialog = true }
                )

                // Nickname
                SettingItem(
                    title = if (isIndo) "Nama Panggilan" else "Nickname",
                    subtitle = appSettings.nickname.ifBlank { if (isIndo) "Belum diatur" else "Not set" },
                    onClick = { showNicknameDialog = true }
                )

                Spacer(Modifier.height(16.dp))
                
                // --- APPEARANCE ---
                Text(
                    if (isIndo) "TAMPILAN" else "APPEARANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                // Accent Color
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isIndo) "Warna Aksen" else "Accent Color", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { showCustomColorDialog = true }) {
                            Text(if (isIndo) "Custom" else "Custom")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(Sage600, Terracotta, Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFF9800), Color.Black).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (color == Color.Black) Color.Black else color)
                                    .border(
                                        width = if (appSettings.accentColor == color) 3.dp else 1.dp,
                                        color = if (appSettings.accentColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { appSettings.accentColor = color }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(24.dp))

                // --- CLOUD BACKUP SECTION ---
                Text(
                    if (isIndo) "CADANGAN AWAN" else "CLOUD BACKUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(16.dp))

                val account = GoogleSignIn.getLastSignedInAccount(context)
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Account Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Cloud, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.primary, 
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isIndo) "Google Drive" else "Google Drive", 
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    account?.email ?: (if (isIndo) "Belum terhubung" else "Not connected"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (account == null) {
                                TextButton(
                                    onClick = { signInLauncher.launch(driveService.getSignInIntent()) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isIndo) "Hubungkan" else "Connect", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    null, 
                                    tint = Color(0xFF4CAF50), 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Auto Backup Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isIndo) "Backup Otomatis (Awan)" else "Auto Backup (Cloud)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (isIndo) "Setiap 24 jam saat HP tidak digunakan" else "Every 24h when device is idle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = appSettings.autoBackupCloud,
                                onCheckedChange = { 
                                    if (account != null) {
                                        appSettings.autoBackupCloud = it
                                        vm.toggleAutoBackup("CLOUD", it)
                                    } else {
                                        Toast.makeText(context, if (isIndo) "Hubungkan Google Drive terlebih dahulu" else "Connect Google Drive first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                scale = 0.8f
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Backup Actions Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isIndo) "Terakhir Backup" else "Last Backup", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    lastBackup, 
                                    style = MaterialTheme.typography.bodySmall, 
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { vm.performBackup() },
                                    enabled = account != null,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isIndo) "Cadangkan" else "Backup", style = MaterialTheme.typography.labelMedium)
                                }
                                
                                FilledTonalIconButton(
                                    onClick = { showRestoreConfirm = true },
                                    enabled = account != null,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isIndo) "Catatan: Backup disimpan ke folder aplikasi tersembunyi di Google Drive Anda." 
                    else "Note: Backups are stored in a hidden app folder on your Google Drive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(24.dp))

                // --- LOCAL BACKUP SECTION ---
                Text(
                    if (isIndo) "PENYIMPANAN LOKAL" else "LOCAL STORAGE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                var showLocalRestoreConfirm by remember { mutableStateOf(false) }
                var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
                ) { uri ->
                    uri?.let { vm.exportDatabase(it) }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        selectedRestoreUri = it
                        showLocalRestoreConfirm = true
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auto Backup Local Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isIndo) "Backup Otomatis (Lokal)" else "Auto Backup (Local)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    if (isIndo) "Simpan 5 backup terakhir secara internal" else "Keep last 5 backups internally",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = appSettings.autoBackupLocal,
                                onCheckedChange = { 
                                    appSettings.autoBackupLocal = it
                                    vm.toggleAutoBackup("LOCAL", it)
                                },
                                scale = 0.8f
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        
                        SettingItem(
                            title = if (isIndo) "Ekspor ke Penyimpanan Internal" else "Export to Internal Storage",
                            subtitle = if (isIndo) "Simpan backup (.zip) ke folder HP" else "Save backup (.zip) to phone folder",
                            icon = Icons.Default.Save,
                            onClick = { 
                                val date = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
                                exportLauncher.launch("Nulis_Backup_$date.zip") 
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        SettingItem(
                            title = if (isIndo) "Impor dari Penyimpanan Internal" else "Import from Internal Storage",
                            subtitle = if (isIndo) "Pulihkan dari file .zip atau .db" else "Restore from .zip or .db file",
                            icon = Icons.Default.FileOpen,
                            onClick = { importLauncher.launch("*/*") }
                        )
                    }
                }

                if (showLocalRestoreConfirm) {
                    AlertDialog(
                        onDismissRequest = { showLocalRestoreConfirm = false },
                        title = { Text(if (isIndo) "Pulihkan Data Lokal?" else "Restore Local Data?") },
                        text = { Text(if (isIndo) "Data saat ini akan digantikan. Aplikasi akan otomatis memuat ulang. Lanjutkan?" else "Current data will be replaced. The app will automatically restart. Continue?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLocalRestoreConfirm = false
                                    selectedRestoreUri?.let { vm.importDatabase(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isIndo) "Ya, Pulihkan" else "Yes, Restore")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLocalRestoreConfirm = false }) {
                                Text(if (isIndo) "Batal" else "Cancel")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    if (isIndo) "Catatan: Gunakan fitur ini untuk memindahkan data antar perangkat secara manual." 
                    else "Note: Use this feature to manually move data between devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                // Credits Item
                SettingItem(
                    title = if (isIndo) "Kredit & Tentang" else "Credits & About",
                    icon = Icons.Default.Info,
                    onClick = { showCredits = true }
                )

                Spacer(Modifier.height(32.dp))
                
                // App Version
                Text(
                    text = "Nu.lis v${com.nu.lis.BuildConfig.VERSION_NAME}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isIndo) "Tutup" else "Close")
            }
        }
    )

    // Dialogs
    if (showNicknameDialog) NicknameDialog(currentName = appSettings.nickname, onConfirm = { appSettings.nickname = it; showNicknameDialog = false }, onDismiss = { showNicknameDialog = false }, isIndo = isIndo)
    if (showLanguageDialog) LanguageDialog(currentLanguage = appSettings.language, onConfirm = { appSettings.language = it; showLanguageDialog = false }, onDismiss = { showLanguageDialog = false }, isIndo = isIndo)
    if (showFontDialog) FontDialog(currentFont = appSettings.fontTheme, onConfirm = { appSettings.fontTheme = it; showFontDialog = false }, onDismiss = { showFontDialog = false }, isIndo = isIndo)
    if (showCustomColorDialog) CustomColorDialog(onDismiss = { showCustomColorDialog = false }, isIndo = isIndo)
    if (showLockSettingsDialog) LockSettingsDialog(onDismiss = { showLockSettingsDialog = false }, isIndo = isIndo)
}


@Composable
fun SettingItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

// ── Dialogs & Screens ──────────────────────────────────────────

@Composable
fun CreditsDialog(onDismiss: () -> Unit, isIndo: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Kredit & Tentang" else "Credits & About") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Notes,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text("Nu.lis", style = MaterialTheme.typography.headlineMedium)
                Text("v1.0.5 (beta)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(Modifier.height(32.dp))
                Text(if (isIndo) "Dikembangkan oleh" else "Developed by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("areev", style = MaterialTheme.typography.titleMedium)
                
                Spacer(Modifier.height(40.dp))
                Text(
                    "Not an amazing app, just an app that keeps your creativity alive. Think about new features or improvements or just wanna buy me a coffee? Sure! DM @heiareev",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}


@Composable
private fun LockSettingsDialog(onDismiss: () -> Unit, isIndo: Boolean) {
    val settings = LocalAppSettings.current
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var verifyAction by remember { mutableStateOf({}) }
    
    val context = LocalContext.current

    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                             androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        biometricManager.canAuthenticate(authenticators) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Pengaturan Kunci" else "Lock Settings") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isIndo) "Aktifkan Kunci" else "Enable Lock", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.isNoteLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (settings.noteLockPin.isEmpty()) {
                                    showSetPinDialog = true
                                } else {
                                    settings.isNoteLockEnabled = true
                                }
                            } else {
                                // Verify before disabling
                                verifyAction = { settings.isNoteLockEnabled = false }
                                showVerifyDialog = true
                            }
                        }
                    )
                }
                
                if (canAuthenticate) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isIndo) "Gunakan Biometrik" else "Use Biometric", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.isBiometricEnabled,
                            onCheckedChange = { settings.isBiometricEnabled = it },
                            enabled = settings.isNoteLockEnabled
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = { 
                            verifyAction = { showSetPinDialog = true }
                            showVerifyDialog = true
                        },
                        enabled = settings.isNoteLockEnabled || settings.noteLockPin.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isIndo) "Ganti PIN" else "Change PIN")
                    }

                    if (settings.noteLockPin.isNotEmpty()) {
                        TextButton(
                            onClick = { 
                                verifyAction = { showDeleteConfirm = true }
                                showVerifyDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isIndo) "Hapus PIN" else "Delete PIN")
                        }
                    }
                }

                if (settings.noteLockPin.isNotEmpty()) {
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isIndo) "Lupa PIN?" else "Forgot PIN?")
                    }
                }

                if (settings.isNoteLockEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isIndo) "Kunci Kembali Saat:" else "Lock Again When:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val lockOptions = if (isIndo) {
                        listOf(
                            "Catatan ditutup" to 0,
                            "10 detik" to 10,
                            "30 detik" to 30,
                            "60 detik" to 60,
                            "120 detik" to 120,
                            "300 detik" to 300,
                            "Aplikasi ditutup" to -1
                        )
                    } else {
                        listOf(
                            "Note closed" to 0,
                            "10 seconds" to 10,
                            "30 seconds" to 30,
                            "60 seconds" to 60,
                            "120 seconds" to 120,
                            "300 seconds" to 300,
                            "App closed" to -1
                        )
                    }

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            val currentLabel = lockOptions.find { it.second == settings.lockTimeoutSeconds }?.first ?: lockOptions.first().first
                            Text(currentLabel)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            lockOptions.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        settings.lockTimeoutSeconds = value
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isIndo) "Selesai" else "Done") }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isIndo) "Hapus PIN?" else "Delete PIN?") },
            text = { Text(if (isIndo) "Ini akan menonaktifkan fitur kunci dan menghapus PIN Anda secara permanen." else "This will disable the lock feature and permanently delete your PIN.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settings.noteLockPin = ""
                        settings.isNoteLockEnabled = false
                        settings.isBiometricEnabled = false
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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

    if (showResetDialog) {
        var answerInput by remember { mutableStateOf("") }
        var errorReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (isIndo) "Pemulihan PIN" else "PIN Recovery") },
            text = {
                Column {
                    Text(if (isIndo) "Jawab pertanyaan keamanan Anda:" else "Answer your security question:")
                    Spacer(Modifier.height(8.dp))
                    Text(settings.securityQuestion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it; errorReset = false },
                        label = { Text(if (isIndo) "Jawaban" else "Answer") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorReset
                    )
                    if (errorReset) {
                        Text(if (isIndo) "Jawaban salah" else "Incorrect answer", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (answerInput.trim().equals(settings.securityAnswer.trim(), ignoreCase = true)) {
                        showResetDialog = false
                        showSetPinDialog = true // Let them set a new PIN
                    } else {
                        errorReset = true
                    }
                }) {
                    Text(if (isIndo) "Lanjut" else "Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(if (isIndo) "Batal" else "Cancel")
                }
            }
        )
    }

    if (showVerifyDialog) {
        LockPinDialog(
            isIndo = isIndo,
            correctPin = settings.noteLockPin,
            onSuccess = {
                showVerifyDialog = false
                verifyAction()
            },
            onDismiss = { showVerifyDialog = false }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onConfirm = { pin, question, answer ->
                settings.noteLockPin = pin
                settings.securityQuestion = question
                settings.securityAnswer = answer
                settings.isNoteLockEnabled = true
                showSetPinDialog = false
            },
            onDismiss = { showSetPinDialog = false },
            isIndo = isIndo
        )
    }
}

@Composable
fun SetPinDialog(onConfirm: (String, String, String) -> Unit, onDismiss: () -> Unit, isIndo: Boolean) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) } // 0: PIN, 1: Confirm PIN, 2: Security Question
    var error by remember { mutableStateOf(false) }

    var selectedQuestion by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var showQuestionMenu by remember { mutableStateOf(false) }

    val questions = listOf(
        if (isIndo) "Siapa nama band favoritmu?" else "What is your favorite band name?",
        if (isIndo) "Apa makanan favoritmu?" else "What is your favorite food?",
        if (isIndo) "Apa warna favoritmu?" else "What is your favorite color?",
        if (isIndo) "Berapa jumlah saudaramu?" else "How many siblings do you have?"
    )

    if (selectedQuestion.isEmpty()) selectedQuestion = questions[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when(step) {
                    0 -> if (isIndo) "Atur PIN" else "Set PIN"
                    1 -> if (isIndo) "Konfirmasi PIN" else "Confirm PIN"
                    else -> if (isIndo) "Pertanyaan Keamanan" else "Security Question"
                }
            ) 
        },
        text = {
            Column {
                when (step) {
                    0, 1 -> {
                        Text(
                            if (isIndo)
                                (if (step == 1) "Masukkan kembali PIN 4-digit" else "Masukkan PIN 4-digit baru")
                            else
                                (if (step == 1) "Re-enter 4-digit PIN" else "Enter new 4-digit PIN")
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = if (step == 1) confirmPin else pin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                    if (step == 1) confirmPin = it else pin = it
                                    error = false
                                }
                            },
                            label = { Text("PIN") },
                            singleLine = true,
                            isError = error,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        if (error) {
                            Text(
                                if (isIndo) "PIN tidak cocok" else "PINs do not match",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    2 -> {
                        Text(if (isIndo) "Pilih pertanyaan untuk memulihkan PIN jika lupa" else "Choose a question to recover PIN if forgotten")
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedQuestion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isIndo) "Pertanyaan" else "Question") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showQuestionMenu = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = showQuestionMenu,
                                onDismissRequest = { showQuestionMenu = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                questions.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q) },
                                        onClick = {
                                            selectedQuestion = q
                                            showQuestionMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text(if (isIndo) "Jawaban" else "Answer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (step) {
                        0 -> if (pin.length == 4) step = 1
                        1 -> {
                            if (confirmPin == pin) {
                                step = 2
                            } else {
                                error = true
                                confirmPin = ""
                            }
                        }
                        2 -> {
                            if (answer.isNotBlank()) {
                                onConfirm(pin, selectedQuestion, answer)
                            }
                        }
                    }
                },
                enabled = when(step) {
                    0 -> pin.length == 4
                    1 -> confirmPin.length == 4
                    else -> answer.isNotBlank()
                }
            ) {
                Text(
                    when(step) {
                        0 -> if (isIndo) "Lanjut" else "Next"
                        1 -> if (isIndo) "Lanjut" else "Next"
                        else -> if (isIndo) "Simpan" else "Save"
                    }
                )
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
fun LockPinDialog(isIndo: Boolean, correctPin: String, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSetNewPinDialog by remember { mutableStateOf(false) }
    
    var biometricFailCount by remember { mutableIntStateOf(0) }
    var forcePinOnly by remember { mutableStateOf(false) }

    val settings = LocalAppSettings.current
    val context = LocalContext.current

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
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        forcePinOnly = true
                    }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricFailCount++
                    if (biometricFailCount >= 3) {
                        forcePinOnly = true
                    }
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isIndo) "Verifikasi Biometrik" else "Biometric Verification")
            .setSubtitle(if (isIndo) "Gunakan sidik jari untuk membuka" else "Use fingerprint to unlock")
            .setNegativeButtonText(if (isIndo) "Gunakan PIN" else "Use PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    LaunchedEffect(Unit) {
        if (settings.isBiometricEnabled && canAuthenticate && !forcePinOnly) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Masukkan PIN" else "Enter PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = false
                            if (it == correctPin) onSuccess()
                            else if (it.length == 4) error = true
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                if (error) {
                    Column {
                        Text(
                            if (isIndo) "PIN salah" else "Wrong PIN",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (settings.securityQuestion.isNotEmpty()) {
                            TextButton(
                                onClick = { showResetDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isIndo) "Lupa PIN?" else "Forgot PIN?", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                if (settings.isBiometricEnabled) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { biometricPrompt.authenticate(promptInfo) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Fingerprint, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isIndo) "Gunakan Biometrik" else "Use Biometric")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (pin == correctPin) onSuccess() else error = true 
            }, enabled = pin.length == 4) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isIndo) "Batal" else "Cancel")
            }
        }
    )

    if (showResetDialog) {
        ResetPinDialog(
            question = settings.securityQuestion,
            correctAnswer = settings.securityAnswer,
            onSuccess = {
                showResetDialog = false
                showSetNewPinDialog = true
            },
            onDismiss = { showResetDialog = false },
            isIndo = isIndo
        )
    }

    if (showSetNewPinDialog) {
        SetPinDialog(
            onConfirm = { newPin, question, answer ->
                settings.noteLockPin = newPin
                settings.securityQuestion = question
                settings.securityAnswer = answer
                showSetNewPinDialog = false
                onSuccess()
            },
            onDismiss = { showSetNewPinDialog = false },
            isIndo = isIndo
        )
    }
}

@Composable
fun ResetPinDialog(
    question: String,
    correctAnswer: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    isIndo: Boolean
) {
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Pulihkan PIN" else "Recover PIN") },
        text = {
            Column {
                Text(question, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { 
                        answer = it
                        error = false
                    },
                    label = { Text(if (isIndo) "Jawaban" else "Answer") },
                    singleLine = true,
                    isError = error,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text(
                        if (isIndo) "Jawaban salah" else "Wrong answer",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.trim().equals(correctAnswer.trim(), ignoreCase = true)) {
                        onSuccess()
                    } else {
                        error = true
                    }
                },
                enabled = answer.isNotBlank()
            ) {
                Text(if (isIndo) "Verifikasi" else "Verify")
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
private fun FontDialog(currentFont: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, isIndo: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Pilih Font" else "Select Font") },
        text = {
            Column {
                listOf("Modern", "Serif", "Monospace").forEach { font ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(font) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentFont == font, onClick = { onConfirm(font) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = font,
                            fontFamily = when (font) {
                                "Serif" -> FontFamily.Serif
                                "Monospace" -> FontFamily.Monospace
                                else -> FontFamily.SansSerif
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isIndo) "Batal" else "Cancel")
            }
        }
    )
}

@Composable
private fun CustomColorDialog(onDismiss: () -> Unit, isIndo: Boolean) {
    val settings = LocalAppSettings.current
    var hsv by remember { mutableStateOf(settings.customColorHsv.copyOf()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIndo) "Warna Kustom" else "Custom Color") },
        text = {
            Column {
                val backgroundColor = Color.hsv(hsv[0], hsv[1], hsv[2])
                val textColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isIndo) "Contoh Teks" else "Sample Text",
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(24.dp))
                
                Text(if (isIndo) "Warna (Hue)" else "Hue")
                Slider(
                    value = hsv[0],
                    onValueChange = { hsv = floatArrayOf(it, hsv[1], hsv[2]) },
                    valueRange = 0f..360f
                )

                Text(if (isIndo) "Kejenuhan (Saturation)" else "Saturation")
                Slider(
                    value = hsv[1],
                    onValueChange = { hsv = floatArrayOf(hsv[0], it, hsv[2]) },
                    valueRange = 0f..1f
                )

                Text(if (isIndo) "Kecerahan (Lightness)" else "Value/Lightness")
                Slider(
                    value = hsv[2],
                    onValueChange = { hsv = floatArrayOf(hsv[0], hsv[1], it) },
                    valueRange = 0.2f..0.8f // Limit range to keep UI readable
                )

                if (settings.recentColorsHsv.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (isIndo) "Warna Terbaru" else "Recent Colors", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        settings.recentColorsHsv.forEach { recentHsv ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.hsv(recentHsv[0], recentHsv[1], recentHsv[2]))
                                    .clickable { hsv = recentHsv.copyOf() }
                                    .border(
                                        width = if (hsv.contentEquals(recentHsv)) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                settings.addRecentColor(hsv)
                settings.customColorHsv = hsv
                settings.accentColor = Color.hsv(hsv[0], hsv[1], hsv[2])
                onDismiss()
            }) {
                Text(if (isIndo) "Terapkan" else "Apply")
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
private fun LanguageDialog(currentLanguage: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, isIndo: Boolean) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isIndo) "Pilih Bahasa" else "Select Language") }, text = {
        Column {
            Row(Modifier.fillMaxWidth().clickable { onConfirm("en") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = currentLanguage == "en", onClick = { onConfirm("en") })
                Spacer(Modifier.width(8.dp)); Text("English")
            }
            Row(Modifier.fillMaxWidth().clickable { onConfirm("id") }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = currentLanguage == "id", onClick = { onConfirm("id") })
                Spacer(Modifier.width(8.dp)); Text("Indonesia")
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text(if (isIndo) "Batal" else "Cancel") } })
}

@Composable
private fun NicknameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit, isIndo: Boolean) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isIndo) "Nama Panggilan" else "Nickname") }, text = {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isIndo) "Masukkan nama" else "Enter name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }, confirmButton = { Button(onClick = { onConfirm(name.trim()) }) { Text(if (isIndo) "Simpan" else "Save") } }, dismissButton = { TextButton(onClick = { onDismiss() }) { Text(if (isIndo) "Batal" else "Cancel") } })
}
