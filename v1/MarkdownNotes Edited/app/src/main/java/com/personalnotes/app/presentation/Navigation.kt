package com.personalnotes.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personalnotes.app.presentation.editor.EditorScreen
import com.personalnotes.app.presentation.home.HomeScreen
import com.personalnotes.app.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor?noteId={noteId}&folderId={folderId}") {
        fun createRoute(noteId: Long? = null, folderId: Long? = null) =
            "editor?noteId=${noteId ?: -1L}&folderId=${folderId ?: -1L}"
    }
    object Settings : Screen("settings")
}

@Composable
fun NotesNavHost(onRequestDriveSignIn: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.createRoute(noteId = noteId))
                },
                onNewNote = { folderId ->
                    navController.navigate(Screen.Editor.createRoute(folderId = folderId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("folderId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStack ->
            val noteId = backStack.arguments?.getLong("noteId") ?: -1L
            val folderId = backStack.arguments?.getLong("folderId") ?: -1L
            EditorScreen(
                noteId = if (noteId == -1L) null else noteId,
                defaultFolderId = if (folderId == -1L) null else folderId,
                onBack = { navController.popBackStack() },
                onNavigateToNote = { id ->
                    navController.navigate(Screen.Editor.createRoute(noteId = id))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onRequestDriveSignIn = onRequestDriveSignIn
            )
        }
    }
}
