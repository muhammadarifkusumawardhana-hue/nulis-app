package com.personalnotes.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personalnotes.app.domain.model.Folder
import com.personalnotes.app.domain.model.Note
import com.personalnotes.app.domain.model.Tag
import java.time.LocalDateTime

private val gson = Gson()

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    folderId = folderId,
    tags = gson.fromJson(tagsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    driveFileId = driveFileId,
    isSynced = isSynced,
    isPinned = isPinned
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    folderId = folderId,
    tagsJson = gson.toJson(tags),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    driveFileId = driveFileId,
    isSynced = isSynced,
    isPinned = isPinned
)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    parentId = parentId,
    color = color,
    createdAt = LocalDateTime.parse(createdAt)
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    name = name,
    parentId = parentId,
    color = color,
    createdAt = createdAt.toString()
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)
fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, color = color)
