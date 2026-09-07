package com.nu.lis.ui.task

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import androidx.work.*
import com.nu.lis.data.*
import com.nu.lis.util.CalendarHelper
import com.nu.lis.worker.ReminderWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import java.util.UUID

data class TaskItemState(
    val id: Long = 0L,
    val localId: String = UUID.randomUUID().toString(),
    val textValue: TextFieldValue,
    val spans: List<StyledSpan> = emptyList(),
    val isChecked: Boolean,
    val position: Int = 0
)

data class TaskEditorState(
    val id: Long = 0L,
    val title: String = "",
    val description: String = "",
    val descriptionValue: TextFieldValue = TextFieldValue(""),
    val descriptionSpans: List<StyledSpan> = emptyList(),
    val items: List<TaskItemState> = emptyList(),
    val dueDate: Long? = null,
    val reminderDate: Long? = null,
    val calendarEventId: Long? = null,
    val addToCalendar: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = true
)

class TaskEditorViewModel(
    private val db: AppDatabase,
    private val taskId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditorState())
    val state: StateFlow<TaskEditorState> = _state.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            if (taskId > 0L) {
                val entity = db.taskDao().getById(taskId)
                if (entity != null) {
                    val descSpans = entity.descriptionSpans()
                    val items = db.taskItemDao().getItemsForTask(taskId).first()
                    _state.update {
                        it.copy(
                            id = entity.id,
                            title = entity.title,
                            description = entity.description,
                            descriptionValue = TextFieldValue(entity.description),
                            descriptionSpans = descSpans,
                            items = items.map { item ->
                                TaskItemState(
                                    item.id,
                                    UUID.randomUUID().toString(),
                                    TextFieldValue(item.text),
                                    item.spans(),
                                    item.isChecked,
                                    item.position
                                )
                            },
                            dueDate = entity.dueDate,
                            reminderDate = entity.reminderDate,
                            calendarEventId = entity.calendarEventId,
                            addToCalendar = entity.calendarEventId != null,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }
            _state.update {
                it.copy(
                    items = listOf(TaskItemState(0L, UUID.randomUUID().toString(), TextFieldValue(""), emptyList(), false, 0)),
                    isLoading = false
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _state.update { it.copy(title = newTitle, isSaved = false) }
        scheduleAutoSave()
    }

    fun onDescriptionChange(newValue: TextFieldValue) {
        _state.update { it.copy(descriptionValue = newValue, description = newValue.text, isSaved = false) }
        scheduleAutoSave()
    }

    fun setDueDate(date: Long?) {
        _state.update { it.copy(dueDate = date, isSaved = false) }
        scheduleAutoSave()
    }

    fun setReminderDate(date: Long?) {
        _state.update { it.copy(reminderDate = date, isSaved = false) }
        scheduleAutoSave()
    }

    fun setAddToCalendar(add: Boolean) {
        _state.update { it.copy(addToCalendar = add, isSaved = false) }
        scheduleAutoSave()
    }

    fun onItemTextChange(localId: String, newValue: TextFieldValue) {
        _state.update { currentState ->
            val updatedItems = currentState.items.map { item ->
                if (item.localId == localId) {
                    item.copy(textValue = newValue)
                } else item
            }
            currentState.copy(items = updatedItems, isSaved = false)
        }
        scheduleAutoSave()
    }

    fun toggleStyle(focusedLocalId: String?, type: String) {
        _state.update { currentState ->
            if (focusedLocalId == null) {
                // Apply to description
                val selection = currentState.descriptionValue.selection
                if (selection.collapsed) return@update currentState
                val newSpans = toggleSpan(currentState.descriptionSpans, selection.min, selection.max, type)
                currentState.copy(descriptionSpans = newSpans, isSaved = false)
            } else {
                val updatedItems = currentState.items.map { item ->
                    if (item.localId == focusedLocalId) {
                        val selection = item.textValue.selection
                        if (selection.collapsed) return@map item
                        val newSpans = toggleSpan(item.spans, selection.min, selection.max, type)
                        item.copy(spans = newSpans)
                    } else item
                }
                currentState.copy(items = updatedItems, isSaved = false)
            }
        }
        scheduleAutoSave()
    }

    private fun toggleSpan(spans: List<StyledSpan>, start: Int, end: Int, type: String): List<StyledSpan> {
        val existing = spans.find { it.start == start && it.end == end && it.type == type }
        return if (existing != null) spans.filter { it != existing } else spans + StyledSpan(start, end, type)
    }

    fun toggleItemCheck(localId: String) {
        _state.update { currentState ->
            val updatedItems = currentState.items.map {
                if (it.localId == localId) it.copy(isChecked = !it.isChecked) else it
            }
            currentState.copy(items = updatedItems, isSaved = false)
        }
        scheduleAutoSave()
    }

    fun addItem() {
        _state.update { currentState ->
            val newItem = TaskItemState(0L, UUID.randomUUID().toString(), TextFieldValue(""), emptyList(), false, currentState.items.size)
            currentState.copy(items = currentState.items + newItem, isSaved = false)
        }
        scheduleAutoSave()
    }

    fun removeItem(localId: String) {
        _state.update { currentState ->
            val updatedItems = currentState.items.filter { it.localId != localId }
            currentState.copy(items = updatedItems, isSaved = false)
        }
        scheduleAutoSave()
    }

    fun saveNow(context: Context) {
        autoSaveJob?.cancel()
        viewModelScope.launch { doSave(context) }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            // We can't easily get context here for calendar sync, 
            // but we can save the DB state. Calendar sync can be triggered on manual save or exit.
            doSave(null)
        }
    }

    private suspend fun doSave(context: Context?) {
        val s = _state.value
        val isCompleted = s.items.isNotEmpty() && s.items.all { it.isChecked }
        
        var currentEntity = TaskEntity(
            id = s.id,
            title = s.title,
            description = s.description,
            descriptionSpansJson = s.descriptionSpans.toSpansJson(),
            itemsJson = "[]", // Deprecated but kept empty for safety
            isCompleted = isCompleted,
            dueDate = s.dueDate,
            reminderDate = s.reminderDate,
            calendarEventId = s.calendarEventId,
            updatedAt = System.currentTimeMillis()
        )

        if (context != null) {
            // Calendar Sync
            if (s.addToCalendar && s.dueDate != null) {
                val eventId = CalendarHelper.syncToCalendar(context, currentEntity)
                currentEntity = currentEntity.copy(calendarEventId = eventId)
            } else if (!s.addToCalendar && s.calendarEventId != null) {
                CalendarHelper.removeFromCalendar(context, s.calendarEventId)
                currentEntity = currentEntity.copy(calendarEventId = null)
            }

            // Reminder Scheduling
            scheduleReminder(context, currentEntity)
        }

        val taskId = if (s.id == 0L) {
            if (s.title.isBlank() && s.items.all { it.textValue.text.isBlank() }) return
            db.taskDao().insert(currentEntity)
        } else {
            db.taskDao().update(currentEntity)
            s.id
        }

        // Save items to task_items table
        db.taskItemDao().deleteByTaskId(taskId)
        s.items.forEachIndexed { index, itemState ->
            val itemEntity = TaskItemEntity(
                taskId = taskId,
                text = itemState.textValue.text,
                spansJson = itemState.spans.toSpansJson(),
                isChecked = itemState.isChecked,
                position = index
            )
            db.taskItemDao().insert(itemEntity)
        }

        _state.update { it.copy(id = taskId, isSaved = true, calendarEventId = currentEntity.calendarEventId) }
    }

    private fun scheduleReminder(context: Context, task: TaskEntity) {
        val workManager = WorkManager.getInstance(context)
        val tag = "task_reminder_${task.id}"
        workManager.cancelAllWorkByTag(tag)

        val reminderTime = task.reminderDate ?: return
        val delay = reminderTime - System.currentTimeMillis()

        if (delay > 0) {
            val data = workDataOf(
                "task_id" to task.id,
                "task_title" to task.title,
                "task_desc" to task.description
            )
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build()
            workManager.enqueue(request)
        }
    }
}

class TaskEditorViewModelFactory(
    private val db: AppDatabase,
    private val taskId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TaskEditorViewModel(db, taskId) as T
    }
}
