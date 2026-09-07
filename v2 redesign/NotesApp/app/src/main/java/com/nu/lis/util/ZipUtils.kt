package com.nu.lis.util

import android.content.Context
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ZipUtils {

    fun zipAppInternalData(context: Context, zipFile: File) {
        ZipArchiveOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Databases
            val dbDir = context.getDatabasePath("notes.db").parentFile
            if (dbDir != null && dbDir.exists()) {
                dbDir.listFiles()?.forEach { file ->
                    // Backup notes.db and related files (shm, wal)
                    if (file.name.startsWith("notes.db")) {
                        addToZip(zos, file, "databases/${file.name}")
                    }
                }
            }

            // 2. Shared Preferences
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.listFiles()?.forEach { file ->
                    addToZip(zos, file, "shared_prefs/${file.name}")
                }
            }

            // 3. DataStore (usually in files/datastore)
            val datastoreDir = File(context.filesDir, "datastore")
            if (datastoreDir.exists()) {
                datastoreDir.listFiles()?.forEach { file ->
                    addToZip(zos, file, "datastore/${file.name}")
                }
            }
        }
    }

    private fun addToZip(zos: ZipArchiveOutputStream, file: File, zipPath: String) {
        val entry = ZipArchiveEntry(file, zipPath)
        zos.putArchiveEntry(entry)
        FileInputStream(file).use { fis ->
            IOUtils.copy(fis, zos)
        }
        zos.closeArchiveEntry()
    }

    fun unzipAppInternalData(context: Context, zipFile: File) {
        ZipArchiveInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipArchiveEntry? = zis.nextZipEntry
            while (entry != null) {
                val targetFile = when {
                    entry.name.startsWith("databases/") -> {
                        File(context.getDatabasePath("notes.db").parentFile, entry.name.substringAfter("databases/"))
                    }
                    entry.name.startsWith("shared_prefs/") -> {
                        File(File(context.applicationInfo.dataDir, "shared_prefs"), entry.name.substringAfter("shared_prefs/"))
                    }
                    entry.name.startsWith("datastore/") -> {
                        File(File(context.filesDir, "datastore"), entry.name.substringAfter("datastore/"))
                    }
                    else -> null
                }

                targetFile?.let {
                    it.parentFile?.mkdirs()
                    FileOutputStream(it).use { fos ->
                        IOUtils.copy(zis, fos)
                    }
                }
                entry = zis.nextZipEntry
            }
        }
    }
}
