package com.personalnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.personalnotes.app.presentation.MainViewModel
import com.personalnotes.app.presentation.NotesNavHost
import com.personalnotes.app.ui.theme.PersonalNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val driveSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        task.addOnSuccessListener { account ->
            viewModel.onDriveConnected(account.email ?: "", account.account?.name ?: "")
        }.addOnFailureListener {
            viewModel.onDriveConnectionFailed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsState()
            PersonalNotesTheme(darkTheme = settings.darkMode) {
                NotesNavHost(
                    onRequestDriveSignIn = { requestDriveSignIn() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppClose()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppOpen()
    }

    private fun requestDriveSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        driveSignInLauncher.launch(client.signInIntent)
    }
}
