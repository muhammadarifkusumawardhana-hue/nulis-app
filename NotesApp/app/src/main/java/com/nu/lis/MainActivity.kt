package com.nu.lis

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.nu.lis.data.AppContainer
import com.nu.lis.theme.*
import com.nu.lis.ui.editor.EditorScreen
import com.nu.lis.ui.editor.EditorViewModel
import com.nu.lis.ui.editor.EditorViewModelFactory
import com.nu.lis.ui.home.HomeScreen
import com.nu.lis.ui.home.HomeViewModel
import com.nu.lis.ui.home.HomeViewModelFactory
import com.nu.lis.ui.task.TaskScreen
import com.nu.lis.ui.task.TaskViewModel
import com.nu.lis.ui.task.TaskViewModelFactory
import com.nu.lis.ui.settings.SettingsDialog

import com.nu.lis.ui.home.SideDrawer
import com.nu.lis.ui.home.NewFolderDialog
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState

class MainActivity : FragmentActivity() {

    private val db by lazy { AppContainer.getDatabase(this) }
    private var lastHomeExitTime: Long = 0L

    private val homeVm: HomeViewModel by viewModels {
        HomeViewModelFactory(db)
    }
    
    private val taskVm: TaskViewModel by viewModels {
        TaskViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        setContent {
            val appSettings = remember {
                AppSettingsState(
                    darkMode = prefs.getBoolean("darkMode", false),
                    language = prefs.getString("language", "en") ?: "en",
                    accentColor = Color(prefs.getInt("accentColor", Color.Black.toArgb())),
                    nickname = prefs.getString("nickname", "") ?: "",
                    isGridView = prefs.getBoolean("isGridView", false),
                    fontTheme = prefs.getString("fontTheme", "Modern") ?: "Modern",
                    isNoteLockEnabled = prefs.getBoolean("isNoteLockEnabled", false),
                    noteLockPin = prefs.getString("noteLockPin", "") ?: "",
                    isBiometricEnabled = prefs.getBoolean("isBiometricEnabled", false),
                    securityQuestion = prefs.getString("securityQuestion", "") ?: "",
                    securityAnswer = prefs.getString("securityAnswer", "") ?: "",
                    lockTimeoutSeconds = prefs.getInt("lockTimeoutSeconds", 0),
                    autoBackupCloud = prefs.getBoolean("autoBackupCloud", false),
                    autoBackupLocal = prefs.getBoolean("autoBackupLocal", false)
                )
            }

            // Persistence observer
            LaunchedEffect(
                appSettings.darkMode, 
                appSettings.language, 
                appSettings.accentColor, 
                appSettings.nickname, 
                appSettings.isGridView, 
                appSettings.fontTheme,
                appSettings.isNoteLockEnabled,
                appSettings.noteLockPin,
                appSettings.isBiometricEnabled,
                appSettings.securityQuestion,
                appSettings.securityAnswer,
                appSettings.lockTimeoutSeconds,
                appSettings.autoBackupCloud,
                appSettings.autoBackupLocal
            ) {
                prefs.edit().apply {
                    putBoolean("darkMode", appSettings.darkMode)
                    putString("language", appSettings.language)
                    putInt("accentColor", appSettings.accentColor.toArgb())
                    putString("nickname", appSettings.nickname)
                    putBoolean("isGridView", appSettings.isGridView)
                    putString("fontTheme", appSettings.fontTheme)
                    putBoolean("isNoteLockEnabled", appSettings.isNoteLockEnabled)
                    putString("noteLockPin", appSettings.noteLockPin)
                    putBoolean("isBiometricEnabled", appSettings.isBiometricEnabled)
                    putString("securityQuestion", appSettings.securityQuestion)
                    putString("securityAnswer", appSettings.securityAnswer)
                    putInt("lockTimeoutSeconds", appSettings.lockTimeoutSeconds)
                    putBoolean("autoBackupCloud", appSettings.autoBackupCloud)
                    putBoolean("autoBackupLocal", appSettings.autoBackupLocal)
                    apply()
                }
            }

            CompositionLocalProvider(LocalAppSettings provides appSettings) {
                NotesTheme {
                    NotesApp(
                        db = db,
                        homeVm = homeVm,
                        taskVm = taskVm,
                        lastHomeExitTime = lastHomeExitTime,
                        onHomeExit = { lastHomeExitTime = System.currentTimeMillis() }
                    )
                }
            }
        }
    }
}

// ── Navigasi sederhana tanpa library navigation-compose ──────────
sealed class Screen {
    object Home : Screen()
    object Tasks : Screen()
    data class Editor(val noteId: Long, val folderId: Long, val key: String) : Screen()
    data class TaskEditor(val taskId: Long, val key: String) : Screen()
}


@Composable
fun NotesApp(
    db: com.nu.lis.data.AppDatabase,
    homeVm: HomeViewModel,
    taskVm: TaskViewModel,
    lastHomeExitTime: Long,
    onHomeExit: () -> Unit
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showSettings by remember { mutableStateOf(false) }
    val appSettings = LocalAppSettings.current
    val isIndo = appSettings.language == "id"
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Folder dialog states shifted here for global access
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<com.nu.lis.data.FolderEntity?>(null) }

    val folders by homeVm.folders.collectAsState()
    val tags by homeVm.tags.collectAsState()
    val selectedFolder by homeVm.selectedFolderId.collectAsState()
    val selectedTag by homeVm.selectedTag.collectAsState()
    val showArchived by homeVm.showArchived.collectAsState()
    val showTrash by homeVm.showTrash.collectAsState()

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onRestart = { 
                showSettings = false
                homeVm.clearAll() // Triggers refresh
            },
            isIndo = isIndo
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = screen is Screen.Home || screen is Screen.Tasks,
        drawerContent = {
            SideDrawer(
                folders = folders,
                tags = tags,
                selectedFolder = if (screen is Screen.Home) selectedFolder else -2L, // -2 to indicate not in home context
                selectedTag = if (screen is Screen.Home) selectedTag else null,
                showArchived = if (screen is Screen.Home) showArchived else false,
                showTrash = if (screen is Screen.Home) showTrash else false,
                onAllNotes = {
                    scope.launch {
                        drawerState.close()
                        homeVm.clearAll()
                        screen = Screen.Home
                    }
                },
                onTasks = {
                    scope.launch {
                        drawerState.close()
                        screen = Screen.Tasks
                    }
                },
                onFolder = { id ->
                    scope.launch {
                        drawerState.close()
                        homeVm.selectFolder(id)
                        screen = Screen.Home
                    }
                },
                onTag = { tag ->
                    scope.launch {
                        drawerState.close()
                        homeVm.selectTag(tag)
                        screen = Screen.Home
                    }
                },
                onNewFolder = {
                    scope.launch {
                        drawerState.close()
                        showNewFolderDialog = true
                    }
                },
                onEditFolder = { folder ->
                    scope.launch {
                        drawerState.close()
                        editingFolder = folder
                    }
                },
                onDeleteFolder = { homeVm.deleteFolder(it) },
                onArchived = {
                    scope.launch {
                        drawerState.close()
                        homeVm.showArchived()
                        screen = Screen.Home
                    }
                },
                onTrash = {
                    scope.launch {
                        drawerState.close()
                        homeVm.showTrash()
                        screen = Screen.Home
                    }
                },
                onSettings = {
                    scope.launch {
                        drawerState.close()
                        showSettings = true
                    }
                }
            )
        }
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith 
                fadeOut(animationSpec = tween(300))
            },
            label = "screen_transition"
        ) { targetScreen ->
            when (val s = targetScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        vm = homeVm,
                        onOpenNote = { noteId ->
                            onHomeExit()
                            screen = Screen.Editor(noteId = noteId, folderId = -1L, key = "note_$noteId")
                        },
                        onNewNote = { folderId ->
                            onHomeExit()
                            screen = Screen.Editor(noteId = -1L, folderId = folderId, key = "new_${System.currentTimeMillis()}")
                        },
                        onGoToSettings = { showSettings = true },
                        onGoToTasks = { screen = Screen.Tasks },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        showNewFolderDialog = showNewFolderDialog,
                        onDismissNewFolder = { showNewFolderDialog = false },
                        editingFolder = editingFolder,
                        onDismissEditFolder = { editingFolder = null }
                    )
                }

                is Screen.Tasks -> {
                    TaskScreen(
                        vm = taskVm,
                        onBack = { screen = Screen.Home },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenTask = { taskId ->
                            screen = Screen.TaskEditor(taskId = taskId, key = "task_$taskId")
                        },
                        onNewTask = {
                            screen = Screen.TaskEditor(taskId = -1L, key = "new_task_${System.currentTimeMillis()}")
                        }
                    )
                }

                is Screen.Editor -> {
                    val editorVm: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = s.key,
                        factory = EditorViewModelFactory(db, s.noteId, s.folderId)
                    )
                    EditorScreen(
                        vm = editorVm,
                        lastHomeExitTime = lastHomeExitTime,
                        onBack = { screen = Screen.Home },
                        onNavigateToNote = { id ->
                            screen = Screen.Editor(noteId = id, folderId = -1L, key = "note_$id")
                        }
                    )
                }

                is Screen.TaskEditor -> {
                    val taskEditorVm: com.nu.lis.ui.task.TaskEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = s.key,
                        factory = com.nu.lis.ui.task.TaskEditorViewModelFactory(db, s.taskId)
                    )
                    com.nu.lis.ui.task.TaskEditorScreen(
                        vm = taskEditorVm,
                        onBack = { screen = Screen.Tasks }
                    )
                }
            }
        }
    }
}


