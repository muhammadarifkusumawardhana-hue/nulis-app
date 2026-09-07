package com.nu.lis.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.client.http.FileContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveService(private val context: Context) {

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Nulis Notes").build()
    }

    suspend fun backup(dbFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext false
        val service = getDriveService(account)

        val fileMetadata = File().apply {
            name = "nulis_backup.zip"
            parents = Collections.singletonList("appDataFolder")
        }
        val mediaContent = FileContent("application/zip", dbFile)

        // Cari file lama untuk dihapus/update (opsional: kita timpa saja dengan delete lalu insert)
        val filesResult: FileList = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'nulis_backup.zip' or name = 'nulis_backup.db'")
            .execute()
        val files = filesResult.files

        for (file in files) {
            service.files().delete(file.id).execute()
        }

        service.files().create(fileMetadata, mediaContent).execute()
        true
    }

    suspend fun restore(targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext false
        val service = getDriveService(account)

        val filesResult: FileList = service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = 'nulis_backup.zip' or name = 'nulis_backup.db'")
            .execute()
        val files = filesResult.files?.sortedByDescending { it.name.endsWith(".zip") } // Prefer zip if both exist

        if (files.isNullOrEmpty()) return@withContext false

        val fileId = files[0].id
        val outputStream = FileOutputStream(targetFile)
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
        true
    }
}
