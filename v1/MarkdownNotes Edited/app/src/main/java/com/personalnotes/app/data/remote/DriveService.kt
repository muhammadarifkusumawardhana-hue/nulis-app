package com.personalnotes.app.data.remote

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.personalnotes.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var driveClient: Drive? = null
    private var accountEmail: String = ""

    fun initialize(accountName: String) {
        accountEmail = accountName
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccountName = accountName }

        driveClient = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("PersonalNotes").build()
    }

    fun isConnected() = driveClient != null

    suspend fun syncNote(note: Note): String? = withContext(Dispatchers.IO) {
        try {
            val drive = driveClient ?: return@withContext null
            val appFolder = getOrCreateAppFolder(drive)
            val content = note.toFileContent()

            if (note.driveFileId != null) {
                // Update existing file
                val fileMetadata = File().apply { name = "${note.id}.md" }
                drive.files().update(note.driveFileId, fileMetadata,
                    ByteArrayContent("text/markdown", content.toByteArray())
                ).execute()
                note.driveFileId
            } else {
                // Create new file
                val fileMetadata = File().apply {
                    name = "${note.id}.md"
                    parents = listOf(appFolder)
                }
                val result = drive.files().create(
                    fileMetadata,
                    ByteArrayContent("text/markdown", content.toByteArray())
                ).setFields("id").execute()
                result.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteNoteFromDrive(driveFileId: String) = withContext(Dispatchers.IO) {
        try {
            driveClient?.files()?.delete(driveFileId)?.execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOrCreateAppFolder(drive: Drive): String {
        val query = "mimeType='application/vnd.google-apps.folder' and name='PersonalNotes' and trashed=false"
        val result = drive.files().list().setQ(query).setFields("files(id)").execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            val folderMetadata = File().apply {
                name = "PersonalNotes"
                mimeType = "application/vnd.google-apps.folder"
            }
            drive.files().create(folderMetadata).setFields("id").execute().id
        }
    }

    private fun Note.toFileContent(): String {
        val tagsLine = if (tags.isNotEmpty()) "tags: [${tags.joinToString(", ")}]" else ""
        val folderLine = folderId?.let { "folderId: $it" } ?: ""
        return buildString {
            appendLine("---")
            appendLine("title: $title")
            appendLine("created: $createdAt")
            appendLine("updated: $updatedAt")
            if (tagsLine.isNotEmpty()) appendLine(tagsLine)
            if (folderLine.isNotEmpty()) appendLine(folderLine)
            appendLine("---")
            appendLine()
            append(content)
        }
    }
}
