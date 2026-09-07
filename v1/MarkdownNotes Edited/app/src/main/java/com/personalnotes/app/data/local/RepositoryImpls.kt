package com.personalnotes.app.data.local

import com.personalnotes.app.domain.model.Folder
import com.personalnotes.app.domain.model.Note
import com.personalnotes.app.domain.model.Tag
import com.personalnotes.app.domain.repository.FolderRepository
import com.personalnotes.app.domain.repository.NoteRepository
import com.personalnotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override fun getNotesByFolder(folderId: Long): Flow<List<Note>> =
        noteDao.getNotesByFolder(folderId).map { list -> list.map { it.toDomain() } }

    override fun getNotesWithoutFolder(): Flow<List<Note>> =
        noteDao.getNotesWithoutFolder().map { list -> list.map { it.toDomain() } }

    override suspend fun getNoteById(id: Long): Note? =
        noteDao.getNoteById(id)?.toDomain()

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    override fun getNotesByTag(tag: String): Flow<List<Note>> =
        noteDao.getNotesByTag(tag).map { list -> list.map { it.toDomain() } }

    override fun getBacklinks(noteTitle: String): Flow<List<Note>> =
        noteDao.getBacklinks(noteTitle).map { list -> list.map { it.toDomain() } }

    override suspend fun saveNote(note: Note): Long {
        val entity = note.copy(updatedAt = LocalDateTime.now(), isSynced = false).toEntity()
        return if (note.id == 0L) noteDao.insertNote(entity)
        else { noteDao.updateNote(entity); note.id }
    }

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note.toEntity())

    override suspend fun getUnsyncedNotes(): List<Note> =
        noteDao.getUnsyncedNotes().map { it.toDomain() }

    override suspend fun markAsSynced(id: Long, driveFileId: String) =
        noteDao.markAsSynced(id, driveFileId)
}

class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getAllFolders().map { list -> list.map { it.toDomain() } }

    override fun getRootFolders(): Flow<List<Folder>> =
        folderDao.getRootFolders().map { list -> list.map { it.toDomain() } }

    override fun getSubfolders(parentId: Long): Flow<List<Folder>> =
        folderDao.getSubfolders(parentId).map { list -> list.map { it.toDomain() } }

    override suspend fun getFolderById(id: Long): Folder? =
        folderDao.getFolderById(id)?.toDomain()

    override suspend fun saveFolder(folder: Folder): Long {
        val entity = folder.toEntity()
        return if (folder.id == 0L) folderDao.insertFolder(entity)
        else { folderDao.updateFolder(entity); folder.id }
    }

    override suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder.toEntity())
}

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { list -> list.map { it.toDomain() } }

    override suspend fun saveTag(tag: Tag) {
        tagDao.insertTag(tag.toEntity())
    }

    override suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag.toEntity())
}
