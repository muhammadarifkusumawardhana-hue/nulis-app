package com.personalnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.personalnotes.data.AppContainer
import com.personalnotes.theme.NotesTheme
import com.personalnotes.ui.editor.EditorScreen
import com.personalnotes.ui.editor.EditorViewModel
import com.personalnotes.ui.editor.EditorViewModelFactory
import com.personalnotes.ui.home.HomeScreen
import com.personalnotes.ui.home.HomeViewModel
import com.personalnotes.ui.home.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val db by lazy { AppContainer.getDatabase(this) }

    private val homeVm: HomeViewModel by viewModels {
        HomeViewModelFactory(db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NotesTheme {
                NotesApp(db = db, homeVm = homeVm)
            }
        }
    }
}

// ── Navigasi sederhana tanpa library navigation-compose ──────────
sealed class Screen {
    object Home : Screen()
    data class Editor(val noteId: Long, val folderId: Long) : Screen()
}

@Composable
fun NotesApp(
    db: com.personalnotes.data.AppDatabase,
    homeVm: HomeViewModel
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val s = screen) {
        is Screen.Home -> {
            HomeScreen(
                vm = homeVm,
                onOpenNote = { noteId ->
                    screen = Screen.Editor(noteId = noteId, folderId = -1L)
                },
                onNewNote = { folderId ->
                    screen = Screen.Editor(noteId = -1L, folderId = folderId)
                }
            )
        }

        is Screen.Editor -> {
            // Buat ViewModel baru untuk setiap catatan
            val editorVm: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = "editor_${s.noteId}_${s.folderId}",
                factory = EditorViewModelFactory(db, s.noteId, s.folderId)
            )
            EditorScreen(
                vm = editorVm,
                onBack = { screen = Screen.Home }
            )
        }
    }
}
