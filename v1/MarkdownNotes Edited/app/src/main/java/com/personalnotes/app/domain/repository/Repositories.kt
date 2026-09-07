package com.personalnotes.app.domain.repository

import com.personalnotes.app.domain.model.Folder
import com.personalnotes.app.domain.model.Note
import com.personalnotes.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNotesByFolder(folderId: Long): Flow<List<Note>>
    fun getNotesWithoutFolder(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByTag(tag: String): Flow<List<Note>>
    fun getBacklinks(noteTitle: String): Flow<List<Note>>
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(note: Note)
    suspend fun getUnsyncedNotes(): List<Note>
    suspend fun markAsSynced(id: Long, driveFileId: String)
}

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    fun getRootFolders(): Flow<List<Folder>>
    fun getSubfolders(parentId: Long): Flow<List<Folder>>
    suspend fun getFolderById(id: Long): Folder?
    suspend fun saveFolder(folder: Folder): Long
    suspend fun deleteFolder(folder: Folder)
}

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun saveTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
}
