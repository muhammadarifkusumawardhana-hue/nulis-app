package com.personalnotes.app.domain.model

import java.time.LocalDateTime

data class Folder(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val color: String = "#6200EE",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val noteCount: Int = 0
)

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String = "#03DAC6"
)

data class SyncMode(
    val mode: SyncModeType = SyncModeType.MANUAL,
    val scheduledHour: Int = 2,
    val scheduledMinute: Int = 0
)

enum class SyncModeType {
    AUTO_ON_CHANGE,
    ON_SAVE,
    ON_OPEN_CLOSE,
    SCHEDULED,
    MANUAL
}

data class AppSettings(
    val darkMode: Boolean = false,
    val syncMode: SyncMode = SyncMode(),
    val isDriveConnected: Boolean = false,
    val driveAccountEmail: String = "",
    val editorFontSize: Int = 14,
    val autoSave: Boolean = true,
    val showWordCount: Boolean = true
)
