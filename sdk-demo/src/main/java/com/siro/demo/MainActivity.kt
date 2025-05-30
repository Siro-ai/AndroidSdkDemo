package com.siro.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.siro.recorder.models.LoginState
import com.siro.recorder.models.RecorderState
import com.siro.recorder.models.RecordingStatus
import com.siro.recorder.services.RecorderService
import com.siro.recorder.services.RecorderService.Companion.getRecordings
import com.siro.recorder.services.RecorderService.Companion.loginState
import com.siro.recorder.services.RecorderService.Companion.recorderEvents
import com.siro.recorder.services.RecorderService.Companion.sendViewEvent
import com.siro.recorder.services.RecorderService.Companion.startRecorderService

class MainActivity : ComponentActivity() {

    companion object {
        const val PREFS_PATH = "sdk-demo"
        const val PREFS_TOKEN_KEY = "token"
    }

    private val prefs by lazy { getSharedPreferences(PREFS_PATH, MODE_PRIVATE) }

    private val token
        get() = prefs.getString(PREFS_TOKEN_KEY, null)

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { data ->
            if (data.values.all { it }) {
                startRecorderService(activity = this, token = token.orEmpty())
            } else {
                Toast.makeText(this, "Missing permissions. Please add in settings", Toast.LENGTH_LONG).show()
            }
        }

    private fun startRecorderWithPermissionCheck() {
        checkNotificationPermission {
            startRecorderService(activity = this, token = token.orEmpty())
        }
    }

    private fun checkNotificationPermission(onGranted: () -> Unit) {
        val permissions = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
            Manifest.permission.RECORD_AUDIO,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.READ_PHONE_STATE else null,
        )
        when {
            permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED } -> {
                onGranted()
            }

            permissions.all { ActivityCompat.shouldShowRequestPermissionRationale(this, it) } -> {
                Toast.makeText(this, "Missing permissions. Please add in settings", Toast.LENGTH_LONG).show()
            }

            else -> {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startRecorderWithPermissionCheck()

        setContent {
            val loginState = loginState.collectAsState(LoginState.Loading)
            val recorderState = recorderEvents.collectAsState(RecordingStatus(RecorderState.Stopped))
            val recordings = getRecordings(applicationContext).collectAsState(emptyList())

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                MainActivityContent(
                    loginState = loginState.value,
                    recorderState = recorderState.value.state,
                    durationMs = recorderState.value.durationMs,
                    amplitudeValues = recorderState.value.amplitudeValues,
                    recordings = recordings.value,
                    modifier = Modifier.padding(innerPadding),
                    onViewEvent = { sendViewEvent(it) },
                )
            }
        }
    }
}
