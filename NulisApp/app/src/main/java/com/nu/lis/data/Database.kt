package com.nu.lis.data

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

// ── Entities ──────────────────────────────────────────────

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val contentSpansJson: String = "[]", // Rich Text support
    val folderId: Long = -1L,   // -1 = no folder
    val tagsJson: String = "[]",
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6650A4",
    val icon: String? = null
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val descriptionSpansJson: String = "[]",
    val itemsJson: String = "[]", // Serialized List<TaskCheckItem>
    val isCompleted: Boolean = false,
    val priority: Int = 0,
    val dueDate: Long? = null,
    val reminderDate: Long? = null,
    val calendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskCheckItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val spansJson: String = "[]", // Serialized List<StyledSpan>
    val isChecked: Boolean = false
)

data class StyledSpan(
    val start: Int,
    val end: Int,
    val type: String // "BOLD", "ITALIC", "UNDERLINE"
)

fun applySpans(text: String, spans: List<StyledSpan>, isChecked: Boolean = false): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        if (isChecked) {
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), 0, text.length)
        }
        spans.forEach { span ->
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(0, text.length)
            if (start < end) {
                when (span.type) {
                    "BOLD" -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    "ITALIC" -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    "UNDERLINE" -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
        }
    }
}

// ── DAOs ──────────────────────────────────────────────────

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = -1 AND isArchived = 0 AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesWithoutFolder(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' OR tagsJson LIKE '%' || :q || '%') AND isArchived = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE tagsJson LIKE '%\"' || :tag || '\"%' AND isArchived = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getNotesByTag(tag: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title = '' AND content = '' AND isArchived = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getDrafts(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :threshold")
    suspend fun purgeOldDeletedNotes(threshold: String)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAll(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(folder: FolderEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteByName(name: String)
}

data class TaskWithItems(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val items: List<TaskItemEntity>
)

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, updatedAt DESC")
    fun getAllTasksWithItems(): Flow<List<TaskWithItems>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Entity(
    tableName = "task_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val text: String = "",
    val spansJson: String = "[]",
    val isChecked: Boolean = false,
    val position: Int = 0
)

@Dao
interface TaskItemDao {
    @Query("SELECT * FROM task_items WHERE taskId = :taskId ORDER BY position ASC")
    fun getItemsForTask(taskId: Long): Flow<List<TaskItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaskItemEntity): Long

    @Update
    suspend fun update(item: TaskItemEntity)

    @Delete
    suspend fun delete(item: TaskItemEntity)

    @Query("DELETE FROM task_items WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}

// ── Database ──────────────────────────────────────────────

@Database(
    entities = [NoteEntity::class, FolderEntity::class, TagEntity::class, TaskEntity::class, TaskItemEntity::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun taskItemDao(): TaskItemDao
}

val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `description` TEXT NOT NULL, 
                `descriptionSpansJson` TEXT NOT NULL, 
                `itemsJson` TEXT NOT NULL, 
                `isCompleted` INTEGER NOT NULL, 
                `priority` INTEGER NOT NULL, 
                `dueDate` INTEGER, 
                `reminderDate` INTEGER, 
                `calendarEventId` INTEGER, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // 1. Tambah kolom contentSpansJson ke tabel notes
        db.execSQL("ALTER TABLE notes ADD COLUMN contentSpansJson TEXT NOT NULL DEFAULT '[]'")
        
        // 2. Tambah tabel task_items (dengan FK)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `task_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `taskId` INTEGER NOT NULL, 
                `text` TEXT NOT NULL, 
                `spansJson` TEXT NOT NULL, 
                `isChecked` INTEGER NOT NULL, 
                `position` INTEGER NOT NULL,
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        // 3. Konversi data Markdown lama ke StyledSpan (Rich Text)
        val cursor = db.query("SELECT id, content FROM notes")
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val content = cursor.getString(1) ?: ""
                    
                    if (content.contains("*") || content.contains("_")) {
                        val (cleanText, spans) = convertMarkdownToSpans(content)
                        val spansJson = gson.toJson(spans)
                        
                        val stmt = db.compileStatement("UPDATE notes SET content = ?, contentSpansJson = ? WHERE id = ?")
                        stmt.bindString(1, cleanText)
                        stmt.bindString(2, spansJson)
                        stmt.bindLong(3, id)
                        stmt.executeUpdateDelete()
                    }
                }
            } finally {
                cursor.close()
            }
        }
    }
}

// Migration example for 1 to 2 as requested by user
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `descriptionSpansJson` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `priority` INTEGER NOT NULL, `dueDate` INTEGER, `reminderDate` INTEGER, `calendarEventId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `text` TEXT NOT NULL, `spansJson` TEXT NOT NULL, `isChecked` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Ensure tasks table has all needed columns (if somehow missing from manual additions)
        // Note: SQLite doesn't support IF NOT EXISTS for ADD COLUMN, so we use a check.
        val columns = mutableListOf<String>()
        val cursor = db.query("PRAGMA table_info(`tasks`)")
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1))
        }
        cursor.close()

        if (!columns.contains("description")) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
        }
        if (!columns.contains("dueDate")) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dueDate` INTEGER")
        }
        if (!columns.contains("reminderDate")) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderDate` INTEGER")
        }
    }
}

// ── Helpers ───────────────────────────────────────────────

private val gson = Gson()

fun NoteEntity.tags(): List<String> =
    gson.fromJson(tagsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()

fun List<String>.toTagsJson(): String = gson.toJson(this)

fun TaskEntity.items(): List<TaskCheckItem> =
    gson.fromJson(itemsJson, object : TypeToken<List<TaskCheckItem>>() {}.type) ?: emptyList()

fun List<TaskCheckItem>.toTaskJson(): String = gson.toJson(this)

fun TaskCheckItem.spans(): List<StyledSpan> =
    gson.fromJson(spansJson, object : TypeToken<List<StyledSpan>>() {}.type) ?: emptyList()

fun TaskItemEntity.spans(): List<StyledSpan> =
    gson.fromJson(spansJson, object : TypeToken<List<StyledSpan>>() {}.type) ?: emptyList()

fun List<StyledSpan>.toSpansJson(): String = gson.toJson(this)

fun TaskEntity.descriptionSpans(): List<StyledSpan> =
    gson.fromJson(descriptionSpansJson, object : TypeToken<List<StyledSpan>>() {}.type) ?: emptyList()

fun convertMarkdownToSpans(content: String): Pair<String, List<StyledSpan>> {
    val spans = mutableListOf<StyledSpan>()
    var cleanText = content
    
    // Simplistic Markdown parsing for Bold (**), Italic (*), Underline (__)
    // This is a basic implementation; for production, use a proper MD parser.
    val patterns = listOf(
        "\\*\\*(.*?)\\*\\*" to "BOLD",
        "\\*(.*?)\\*" to "ITALIC",
        "__(.*?)__" to "UNDERLINE"
    )

    patterns.forEach { (regexStr, type) ->
        val regex = Regex(regexStr)
        var match = regex.find(cleanText)
        while (match != null) {
            val start = match.range.first
            val innerText = match.groupValues[1]
            val end = start + innerText.length
            
            spans.add(StyledSpan(start, end, type))
            cleanText = cleanText.replaceRange(match.range, innerText)
            
            // Adjust existing spans
            val diff = match.value.length - innerText.length
            spans.forEachIndexed { index, s ->
                if (index < spans.size - 1) { // skip current
                    if (s.start > start) {
                        spans[index] = s.copy(start = s.start - diff, end = s.end - diff)
                    }
                }
            }
            
            match = regex.find(cleanText)
        }
    }

    return cleanText to spans
}
