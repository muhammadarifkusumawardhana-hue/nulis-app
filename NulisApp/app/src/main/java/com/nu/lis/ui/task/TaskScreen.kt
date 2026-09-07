package com.nu.lis.ui.task

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nu.lis.data.*
import com.nu.lis.theme.LocalAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    vm: TaskViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit
) {
    val tasks by vm.tasks.collectAsState()
    val appSettings = LocalAppSettings.current
    val isIndo = appSettings.language == "id"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isIndo) "Tugas" else "Tasks") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = tasks.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = onNewTask,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyTaskState(
                onNewTask = onNewTask,
                isIndo = isIndo,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(tasks, key = { it.task.id }) { taskWithItems ->
                    TaskCard(
                        taskWithItems = taskWithItems,
                        onToggleItem = { itemId -> vm.toggleCheckItem(taskWithItems, itemId) },
                        onDelete = { vm.deleteTask(taskWithItems.task.id) },
                        onClick = { onOpenTask(taskWithItems.task.id) },
                        isIndo = isIndo
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(
    onNewTask: () -> Unit,
    isIndo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Assignment,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (isIndo) "Belum ada tugas" else "No tasks yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isIndo) "Kelola harimu dengan mencatat tugas." else "Organize your day by listing your tasks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onNewTask,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isIndo) "Tugas Baru" else "New Task")
        }
    }
}

@Composable
fun TaskCard(
    taskWithItems: TaskWithItems,
    onToggleItem: (Long) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    isIndo: Boolean
) {
    val task = taskWithItems.task
    val items = taskWithItems.items
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.title.isNotBlank()) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            if (task.title.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
            }

            items.take(5).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleItem(item.id) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = applySpans(
                            item.text.ifBlank { if (isIndo) "Item tugas" else "Task item" },
                            item.spans(),
                            item.isChecked
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (items.size > 5) {
                Text(
                    text = "+${items.size - 5} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                )
            }
        }
    }
}
