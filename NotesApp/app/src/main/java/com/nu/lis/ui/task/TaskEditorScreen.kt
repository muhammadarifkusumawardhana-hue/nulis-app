package com.nu.lis.ui.task

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.nu.lis.data.applySpans
import com.nu.lis.data.StyledSpan
import com.nu.lis.theme.LocalAppSettings
import java.text.SimpleDateFormat
import java.util.*

class RichTextTransformation(private val spans: List<StyledSpan>, private val isChecked: Boolean = false) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotated = applySpans(text.text, spans, isChecked)
        return TransformedText(
            annotated,
            OffsetMapping.Identity
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    vm: TaskEditorViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val appSettings = LocalAppSettings.current
    val isIndo = appSettings.language == "id"
    val context = LocalContext.current
    
    var focusedLocalId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingForReminder by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            vm.setAddToCalendar(true)
        }
    }

    // Hoisted state for date picking flow
    var tempDate by remember { mutableLongStateOf(0L) }

    BackHandler {
        vm.saveNow(context)
        onBack()
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isIndo) "Edit Tugas" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.saveNow(context)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (!state.isSaved) {
                        Text(
                            if (isIndo) "Menyimpan..." else "Saving...",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.addItem() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                    
                    VerticalDivider(
                        modifier = Modifier.padding(vertical = 16.dp).height(20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Formatting shortcuts
                    val shortcuts = listOf(
                        Icons.Default.FormatBold to "BOLD",
                        Icons.Default.FormatItalic to "ITALIC",
                        Icons.Default.FormatUnderlined to "UNDERLINE"
                    )
                    
                    shortcuts.forEach { (icon, type) ->
                        IconButton(
                            onClick = { 
                                vm.toggleStyle(focusedLocalId, type)
                            }
                        ) {
                            Icon(icon, null)
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier.padding(vertical = 16.dp).height(20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    IconButton(onClick = { 
                        pickingForReminder = false
                        showDatePicker = true 
                    }) {
                        Icon(Icons.Default.Event, contentDescription = "Due Date")
                    }

                    IconButton(onClick = { 
                        pickingForReminder = true
                        showDatePicker = true 
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    BasicTextField(
                        value = state.title,
                        onValueChange = { vm.onTitleChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (state.title.isEmpty()) {
                                Text(
                                    if (isIndo) "Judul" else "Title",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    BasicTextField(
                        value = state.descriptionValue,
                        onValueChange = { vm.onDescriptionChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) focusedLocalId = null },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = RichTextTransformation(state.descriptionSpans),
                        decorationBox = { innerTextField ->
                            if (state.description.isEmpty()) {
                                Text(
                                    if (isIndo) "Deskripsi..." else "Description...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    if (state.dueDate != null || state.reminderDate != null) {
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.dueDate?.let { date ->
                                AssistChip(
                                    onClick = { vm.setDueDate(null) },
                                    label = { Text(formatDateTime(date)) },
                                    leadingIcon = { Icon(Icons.Default.Event, null, Modifier.size(16.dp)) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                                )
                            }
                            state.reminderDate?.let { date ->
                                AssistChip(
                                    onClick = { vm.setReminderDate(null) },
                                    label = { Text(formatDateTime(date)) },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, null, Modifier.size(16.dp)) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = state.addToCalendar,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                    val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (readGranted && writeGranted) {
                                        vm.setAddToCalendar(true)
                                    } else {
                                        permissionLauncher.launch(arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        ))
                                    }
                                } else {
                                    vm.setAddToCalendar(false)
                                }
                            }
                        )
                        Text(
                            if (isIndo) "Tambahkan ke Kalender HP" else "Add to Phone Calendar",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            itemsIndexed(state.items, key = { _, item -> item.localId }) { index, item ->
                TaskItemRow(
                    item = item,
                    onTextChange = { vm.onItemTextChange(item.localId, it) },
                    onCheckedChange = { vm.toggleItemCheck(item.localId) },
                    onRemove = { vm.removeItem(item.localId) },
                    onFocus = { focusedLocalId = item.localId },
                    isIndo = isIndo
                )
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedDate != null) {
                        showDatePicker = false
                        showTimePicker = true
                        // Temporary storage or pass to next picker
                        tempDate = selectedDate
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = tempDate
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    
                    if (pickingForReminder) {
                        vm.setReminderDate(cal.timeInMillis)
                    } else {
                        vm.setDueDate(cal.timeInMillis)
                    }
                    showTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

// Global or hoisted state for date picking flow - Removed as it's now inside the composable

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        text = { content() }
    )
}

@Composable
fun TaskItemRow(
    item: TaskItemState,
    onTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onCheckedChange: () -> Unit,
    onRemove: () -> Unit,
    onFocus: () -> Unit,
    isIndo: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange() }
        )
        
        BasicTextField(
            value = item.textValue,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .onFocusChanged { if (it.isFocused) onFocus() },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = RichTextTransformation(item.spans, item.isChecked),
            decorationBox = { innerTextField ->
                if (item.textValue.text.isEmpty()) {
                    Text(
                        if (isIndo) "Item tugas..." else "Task item...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }
                innerTextField()
            }
        )

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Remove",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatDateTime(millis: Long): String {
    if (millis <= 0) return "Belum disetel"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
