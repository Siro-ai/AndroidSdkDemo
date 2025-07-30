package com.siro.demo

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.GsonBuilder
import com.siro.demo.MainActivity.Companion.PREFS_PATH
import com.siro.demo.MainActivity.Companion.PREFS_TOKEN_KEY
import com.siro.recorder.database.RecordingEntity
import com.siro.recorder.database.SyncState
import com.siro.recorder.models.LoginState
import com.siro.recorder.models.RecorderState
import com.siro.recorder.models.RecordingMetadata
import com.siro.recorder.models.ViewEvent
import com.siro.recorder.services.RecorderService.Companion.sendViewEvent
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainActivityContent(
    loginState: LoginState,
    recorderState: RecorderState,
    durationMs: Long,
    amplitudeValues: List<Int>,
    recordings: List<RecordingEntity>,
    modifier: Modifier = Modifier,
    onViewEvent: (ViewEvent) -> Unit,
) {
    val context = LocalContext.current
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val preferences = context.getSharedPreferences(PREFS_PATH, Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(preferences.getString(PREFS_TOKEN_KEY, null)) }
    var titleOverride by remember { mutableStateOf("") }
    var metadata: RecordingMetadata by remember { mutableStateOf(RecordingMetadata()) }

    // top level container
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Auth")
        // auth container
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        )
        {
            when (loginState) {
                is LoginState.Loading -> {
                    CircularProgressIndicator()
                }

                is LoginState.Success -> {
                    Row {
                        Text(
                            "Logged In: ${loginState.user.email}",
                            modifier = Modifier.stadiumBg(Sentiment.Positive),
                        )

                        Spacer(Modifier.width(16.dp))

                        Button(
                            onClick = {
                                token = ""
                                onViewEvent(ViewEvent.Logout)
                                preferences.edit { putString(PREFS_TOKEN_KEY, "") }
                            },
                            enabled = !token.isNullOrBlank(),
                        ) {
                            Text("Logout")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val conversations = loginState.conversations
                    if (conversations == null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (conversations.isNotEmpty()) {
                        var selectedOptionIndex by remember { mutableIntStateOf(0) }

                        Text("Conversation Types:", Modifier.padding(vertical = 4.dp))

                        conversations.forEachIndexed { index, conversation ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = index == selectedOptionIndex,
                                        onClick = {
                                            selectedOptionIndex = index
                                            onViewEvent(ViewEvent.SelectConversationType(conversation))
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = index == selectedOptionIndex,
                                    onClick = null, // handled by row
                                )
                                Text(
                                    text = conversation.displayName,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }
                    } else {
                        Text("No conversations found", modifier = Modifier.stadiumBg(Sentiment.Negative))
                    }
                }

                is LoginState.LoggedOut,
                is LoginState.Error,
                    -> {
                    Text(
                        "Logged Out",
                        modifier = Modifier.stadiumBg(Sentiment.Negative),
                    )

                    Spacer(Modifier.height(8.dp))

                    Row {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = token.orEmpty(),
                            placeholder = { Text("Enter Auth Token") },
                            maxLines = 1,
                            onValueChange = { token = it },
                        )

                        Spacer(Modifier.width(16.dp))

                        Button(
                            onClick = {
                                token?.let {
                                    onViewEvent(ViewEvent.LoginWithToken(it))
                                    preferences.edit { putString(PREFS_TOKEN_KEY, it) }
                                }

                            },
                            enabled = !token.isNullOrBlank(),
                        ) { Text("Login") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recorder")
        // recorder container
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            var metadataExpanded by remember { mutableStateOf(false) }
            val caretIconDrawable = if (metadataExpanded) R.drawable.caret_up_24 else R.drawable.caret_down_24
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        metadataExpanded = !metadataExpanded
                    },
            ) {
                Text("Attach Metadata")
                Icon(painter = painterResource(caretIconDrawable), contentDescription = null)
            }

            if (metadataExpanded) {
                // title entry
                TextField(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    value = titleOverride,
                    placeholder = { Text("Set Recording Title") },
                    maxLines = 1,
                    onValueChange = {
                        titleOverride = it
                        onViewEvent(ViewEvent.SetRecordingTitle(it))
                    },
                )

                // crm metadata
                TextField(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    value = metadata.crmObjectId.orEmpty(),
                    placeholder = { Text("CRM Object ID") },
                    maxLines = 1,
                    onValueChange = {
                        metadata = metadata.copy(crmObjectId = it.takeIf { it.isNotBlank() })
                        onViewEvent(ViewEvent.SetRecordingMetadata(metadata))
                    },
                )

                TextField(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    value = metadata.crmObjectType.orEmpty(),
                    placeholder = { Text("CRM Object Type") },
                    maxLines = 1,
                    onValueChange = {
                        metadata = metadata.copy(crmObjectType = it.takeIf { it.isNotBlank() })
                        onViewEvent(ViewEvent.SetRecordingMetadata(metadata))
                    },
                )

                TextField(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    value = metadata.crmTenantId.orEmpty(),
                    placeholder = { Text("CRM Tenant ID") },
                    maxLines = 1,
                    onValueChange = {
                        metadata = metadata.copy(crmTenantId = it.takeIf { it.isNotBlank() })
                        onViewEvent(ViewEvent.SetRecordingMetadata(metadata))
                    },
                )

                TextField(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    value = metadata.crmPlatform.orEmpty(),
                    placeholder = { Text("CRM Platform") },
                    maxLines = 1,
                    onValueChange = {
                        metadata = metadata.copy(crmPlatform = it.takeIf { it.isNotBlank() })
                        onViewEvent(ViewEvent.SetRecordingMetadata(metadata))
                    },
                )
            }

            // wave form ui
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val prefillValues = 50 - amplitudeValues.size
                val values = (1..prefillValues).map { 1 } + amplitudeValues
                values.map {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(
                                min(
                                    100,
                                    sqrt(
                                        it
                                            .coerceAtLeast(1)
                                            .toDouble(),
                                    ).roundToInt(),
                                ).dp,
                            )
                            .background(color = Color.Magenta, shape = RoundedCornerShape(1.dp)),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // recording duration display
            val hms = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d:%03d",
                TimeUnit.MILLISECONDS.toHours(durationMs),
                TimeUnit.MILLISECONDS.toMinutes(durationMs) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) % TimeUnit.MINUTES.toSeconds(1),
                durationMs % TimeUnit.SECONDS.toMillis(1),
            )
            Text(hms)

            Spacer(modifier = Modifier.height(16.dp))

            // buttons (stop / record / pause)
            Row {
                Button(
                    enabled = recorderState != RecorderState.Stopped,
                    onClick = {
                        sendViewEvent(ViewEvent.StopRecorder)
                    },
                ) {
                    Text("Stop")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    enabled = recorderState == RecorderState.Stopped,
                    onClick = {
                        sendViewEvent(ViewEvent.StartRecorder)
                    },
                ) {
                    Text("Record")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    enabled = recorderState != RecorderState.Stopped,
                    onClick = {
                        sendViewEvent(ViewEvent.TogglePauseRecorder)
                    },
                ) {
                    val text = if (recorderState == RecorderState.Recording) "Pause" else "Resume"
                    Text(text)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recordings")
        // recording sync container
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                enabled = recorderState != RecorderState.Recording && recordings.isNotEmpty(),
                onClick = {
                    sendViewEvent(ViewEvent.UploadAllLocalRecordings)
                },
            ) {
                Text("Upload All")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recordings.isEmpty()) {
                Text("No Recordings")
            }

            recordings.forEach {
                var expanded by remember { mutableStateOf(false) }
                val caretIconDrawable = if (expanded) R.drawable.caret_up_24 else R.drawable.caret_down_24
                ListItem(
                    modifier = Modifier.clickable {
                        expanded = !expanded
                    },
                    headlineContent = {
                        Column {
                            Text(
                                Date.from(Instant.ofEpochMilli(it.createdAtMs)).toString(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                it.syncState.name,
                            )
                        }
                    },
                    supportingContent = if (expanded) {
                        {
                            Column(modifier.wrapContentHeight()) {
                                Text(gson.toJson(it))
                                Button(
                                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0xFFE57373)),
                                    enabled = true,
                                    onClick = { sendViewEvent(ViewEvent.DeleteLocalRecording(it)) },
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    } else null,
                    leadingContent = {
                        Icon(painter = painterResource(caretIconDrawable), contentDescription = null)
                    },
                    trailingContent = {
                        when (it.syncState) {
                            SyncState.Local,
                            SyncState.ReadyForUpload,
                                -> Button(
                                shape = CircleShape,
                                onClick = { onViewEvent(ViewEvent.UploadRecording(it)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_cloud_upload_24),
                                    contentDescription = null,
                                )
                            }

                            SyncState.UploadInProgress -> CircularProgressIndicator()
                            SyncState.UploadSuccess -> Icon(
                                painter = painterResource(R.drawable.baseline_check_circle_24),
                                contentDescription = null,
                                tint = Color.Green,
                            )

                            SyncState.UploadFailed -> Icon(
                                painter = painterResource(R.drawable.baseline_error_24),
                                contentDescription = null,
                                tint = Color.Red,
                            )
                        }
                    },
                )
                if (it != recordings.lastOrNull()) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

private enum class Sentiment {
    Positive,
    Caution,
    Negative,
    Neutral,
    Informative,
}

@Composable
private fun Modifier.stadiumBg(sentiment: Sentiment = Sentiment.Neutral) = this
    .background(
        color = when (sentiment) {
            Sentiment.Positive -> Color(0xFF81C784)
            Sentiment.Caution -> Color(0xFFFFF176)
            Sentiment.Negative -> Color(0xFFE57373)
            Sentiment.Neutral -> Color.LightGray
            Sentiment.Informative -> Color.Cyan
        },
        shape = RoundedCornerShape(percent = 50),
    )
    .padding(horizontal = 32.dp, vertical = 16.dp)
