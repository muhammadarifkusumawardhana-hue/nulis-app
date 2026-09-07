package com.personalnotes.app.domain.model

import java.time.LocalDateTime

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val driveFileId: String? = null,
    val isSynced: Boolean = false,
    val isPinned: Boolean = false
)
