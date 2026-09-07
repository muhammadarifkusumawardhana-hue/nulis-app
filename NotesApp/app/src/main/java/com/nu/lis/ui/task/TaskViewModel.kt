package com.nu.lis.ui.task

import androidx.lifecycle.*
import com.nu.lis.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val db: AppDatabase) : ViewModel() {

    val tasks: StateFlow<List<TaskWithItems>> = db.taskDao().getAllTasksWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTask(task: TaskEntity) = viewModelScope.launch {
        db.taskDao().update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    fun toggleTaskCompletion(task: TaskEntity) = viewModelScope.launch {
        db.taskDao().update(task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis()
        ))
    }

    fun toggleCheckItem(taskWithItems: TaskWithItems, itemId: Long) = viewModelScope.launch {
        val item = taskWithItems.items.find { it.id == itemId } ?: return@launch
        val updatedItem = item.copy(isChecked = !item.isChecked)
        db.taskItemDao().update(updatedItem)
        
        // Check if all items are now checked
        val allChecked = taskWithItems.items.all { 
            if (it.id == itemId) !it.isChecked else it.isChecked 
        }
        
        db.taskDao().update(taskWithItems.task.copy(
            isCompleted = allChecked,
            updatedAt = System.currentTimeMillis()
        ))
    }

    fun deleteTask(id: Long) = viewModelScope.launch {
        db.taskDao().deleteById(id)
        db.taskItemDao().deleteByTaskId(id)
    }
}

class TaskViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TaskViewModel(db) as T
    }
}
