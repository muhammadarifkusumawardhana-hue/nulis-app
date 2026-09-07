package com.personalnotes.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personalnotes.app.domain.model.SyncModeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRequestDriveSignIn: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showScheduleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- TAMPILAN ---
            SettingsSection(title = "Tampilan") {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Mode Gelap",
                    subtitle = "Tema gelap untuk nyaman di malam hari",
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.updateDarkMode(it) }
                )
                SettingsToggleItem(
                    icon = Icons.Default.TextFields,
                    title = "Tampilkan Jumlah Kata",
                    subtitle = "Hitung kata dan karakter di editor",
                    checked = settings.showWordCount,
                    onCheckedChange = { viewModel.updateShowWordCount(it) }
                )
            }

            // --- EDITOR ---
            SettingsSection(title = "Editor") {
                SettingsToggleItem(
                    icon = Icons.Default.Save,
                    title = "Auto Simpan",
                    subtitle = "Simpan otomatis saat mengetik (1.5 detik)",
                    checked = settings.autoSave,
                    onCheckedChange = { viewModel.updateAutoSave(it) }
                )
                SettingsSliderItem(
                    icon = Icons.Default.FormatSize,
                    title = "Ukuran Font Editor",
                    subtitle = "${settings.editorFontSize}sp",
                    value = settings.editorFontSize.toFloat(),
                    valueRange = 12f..24f,
                    onValueChange = { viewModel.updateFontSize(it.toInt()) }
                )
            }

            // --- GOOGLE DRIVE ---
            SettingsSection(title = "Google Drive") {
                if (settings.isDriveConnected) {
                    SettingsInfoItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Terhubung sebagai",
                        subtitle = settings.driveAccountEmail
                    )
                    SettingsActionItem(
                        icon = Icons.Default.LinkOff,
                        title = "Putus koneksi Drive",
                        subtitle = "Catatan tetap tersimpan di perangkat",
                        onClick = { viewModel.disconnectDrive() }
                    )
                } else {
                    SettingsActionItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Hubungkan Google Drive",
                        subtitle = "Sinkronisasi catatan antar perangkat",
                        onClick = onRequestDriveSignIn
                    )
                }
            }

            // --- MODE SINKRONISASI ---
            if (settings.isDriveConnected) {
                SettingsSection(title = "Mode Sinkronisasi") {
                    val modes = listOf(
                        SyncModeType.AUTO_ON_CHANGE to Pair("Otomatis setiap perubahan", "Sync 30 detik setelah ada perubahan"),
                        SyncModeType.ON_SAVE to Pair("Saat disimpan", "Sync setiap kali catatan disimpan"),
                        SyncModeType.ON_OPEN_CLOSE to Pair("Saat buka/tutup aplikasi", "Sync ketika aplikasi dibuka atau ditutup"),
                        SyncModeType.SCHEDULED to Pair("Terjadwal", "Sync pada jam yang kamu tentukan"),
                        SyncModeType.MANUAL to Pair("Manual", "Sync hanya saat kamu tekan tombol sync")
                    )

                    modes.forEach { (mode, info) ->
                        val (title, subtitle) = info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.syncMode.mode == mode,
                                onClick = {
                                    if (mode == SyncModeType.SCHEDULED) {
                                        showScheduleDialog = true
                                    } else {
                                        viewModel.updateSyncMode(mode)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    if (mode == SyncModeType.SCHEDULED && settings.syncMode.mode == SyncModeType.SCHEDULED)
                                        "Setiap hari jam ${settings.syncMode.scheduledHour.toString().padStart(2, '0')}:${settings.syncMode.scheduledMinute.toString().padStart(2, '0')}"
                                    else subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            // --- TENTANG ---
            SettingsSection(title = "Tentang") {
                SettingsInfoItem(
                    icon = Icons.Default.Info,
                    title = "Versi Aplikasi",
                    subtitle = "1.0.0"
                )
                SettingsInfoItem(
                    icon = Icons.Default.Storage,
                    title = "Database",
                    subtitle = "SQLite lokal (Room)"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showScheduleDialog) {
        ScheduledSyncDialog(
            currentHour = settings.syncMode.scheduledHour,
            currentMinute = settings.syncMode.scheduledMinute,
            onConfirm = { hour, minute ->
                viewModel.updateSyncMode(SyncModeType.SCHEDULED, hour, minute)
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        content()
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange,
            steps = ((valueRange.endInclusive - valueRange.start).toInt() - 1),
            modifier = Modifier.padding(start = 38.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ScheduledSyncDialog(
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(currentHour.toString().padStart(2, '0')) }
    var minute by remember { mutableStateOf(currentMinute.toString().padStart(2, '0')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Waktu Sync") },
        text = {
            Column {
                Text("Sync otomatis akan berjalan setiap hari pada waktu yang kamu tentukan.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hour = it },
                        label = { Text("Jam (0-23)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minute = it },
                        label = { Text("Menit (0-59)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 2
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                onConfirm(h, m)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
