package com.siro.demo

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.siro.recorder.database.RecordingEntity
import com.siro.recorder.models.LoginState
import com.siro.recorder.models.RecorderState
import com.siro.recorder.models.RecordingStatus
import com.siro.recorder.models.ViewEvent
import com.siro.recorder.notification.NotificationProvider
import com.siro.recorder.notification.RecorderPendingIntentProvider
import com.siro.recorder.notification.UploadNotificationSettings
import com.siro.recorder.services.BuildVariant
import com.siro.recorder.services.RecorderService.Companion.getRecordings
import com.siro.recorder.services.RecorderService.Companion.loginState
import com.siro.recorder.services.RecorderService.Companion.recorderEvents
import com.siro.recorder.services.RecorderService.Companion.sendViewEvent
import com.siro.recorder.services.RecorderService.Companion.startRecorderService
import com.siro.recorder.services.Settings
import com.siro.recorder.services.UploadSettings

@RequiresApi(Build.VERSION_CODES.O)
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
                startRecorderService(context = this, token = token.orEmpty())
            } else {
                Toast.makeText(this, "Missing permissions. Please add in settings", Toast.LENGTH_LONG).show()
            }
        }

    // TODO should be mutable state held in view model etc
    private var consentGranted = false

    private fun startRecorderWithPermissionCheck() {
        checkNotificationPermission {
            startRecorderService(
                context = this,
                token = token.orEmpty(),
                settings = Settings(
                    buildVariant = BuildVariant.Staging,
                    uploadSettings = UploadSettings.Always,
                    // how to customize notifications
                    notificationProvider = object : NotificationProvider {
                        override fun createRecorderNotification(
                            notificationBuilder: NotificationCompat.Builder,
                            recorderState: RecorderState,
                            recorderPendingIntentProvider: RecorderPendingIntentProvider,
                        ): Notification = notificationBuilder
                            .setContentTitle(
                                if (!consentGranted) {
                                    "Consent Required to Record"
                                } else {
                                    "Custom Recorder Notification"
                                },
                            )
                            .setSmallIcon(R.drawable.baseline_mic_24)
                            .setOngoing(true)
                            .setAutoCancel(false)
                            .setSilent(true)
                            .build()

                        override fun uploadNotificationSettings(recordingEntity: RecordingEntity): UploadNotificationSettings =
                            UploadNotificationSettings(
                                uploadQueuedDesc = recordingEntity.title,
                                uploadInProgressDesc = recordingEntity.title,
                                uploadSuccessDesc = recordingEntity.title,
                                uploadFailedDesc = recordingEntity.title,
                            )
                    },
                ),
            )
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

            var consentGranted by remember { mutableStateOf(consentGranted) }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                if (!consentGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Button(
                            onClick = {
                                consentGranted = true
                                this@MainActivity.consentGranted = true
                                sendViewEvent(ViewEvent.UpdateRecorderNotification)
                            },
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Text("Consent to Record")
                        }
                    }
                } else {
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
}
