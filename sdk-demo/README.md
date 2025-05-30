# Siro Android SDK Demo

This repository showcases how to install and launch the Siro Android SDK

## Installation

1. Add credentials required to import the SDK via github packages:

```
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Siro-ai/AndroidSdkDist")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    ...
}
```

2. Import the SDK in your `build.gradle` file

```
dependencies {
    implementation("com.siro.recorder:siro-sdk:<version>")
    ...
}
```

3. Requirements before launching the SDK

Ensure the following permissions are accepted before launching the SDK:

```
Manifest.permission.POST_NOTIFICATIONS
Manifest.permission.RECORD_AUDIO
Manifest.permission.READ_PHONE_STATE
```

4. Launching the SDK and observing relevant state

Retrieve an OAuth app token; ie login to Siro using our app token.

To launch the SDK call the below function and pass your activity and auth token.

```
startRecorderService(activity = activity, token = token, settings = Settings(...)

// where

data class Settings(
    val buildVariant: BuildVariant, // Prod or Staging
    val uploadOnWifiOnly: Boolean = false,
)
```

All functions and flows are exposed via `RecorderService` companion object.

Once launched, there are 3 mains flows that can be collected to render any required UI.

```
// login state - if the user is logged in or not
val loginState = loginState.collectAsState(LoginState.Loading)

// recorder state - status of any ongoing recordings to display a waveform and buttons etc
val recorderState = recorderEvents.collectAsState(RecordingStatus(RecorderState.Stopped))

// recordings - db backed list of past recordings and their current sync state (device vs uploaded)
val recordings = getRecordings(applicationContext).collectAsState(emptyList())
```

There is an additional flow that can be collected to react to analytics events.

```
val analyticsEvents = analyticsEvents.collectLatest {
    // log events as needed
}
```

Finally to communicate events to the `RecorderService` and react to user input use: 

```
sendViewEvent(viewEvent: ViewEvent)

// where

sealed class ViewEvent {
    // recorder events
    data object StartRecorder : ViewEvent()
    data object TogglePauseRecorder : ViewEvent()
    data object StopRecorder : ViewEvent()
    data object CancelRecorder : ViewEvent()

    // login and config events
    data class LoginWithToken(val token: String) : ViewEvent()
    data object Logout : ViewEvent()
    data class SelectConversationType(val conversation: Conversation) : ViewEvent()
    data class SetRecordingTitle(val title: String?) : ViewEvent() // null or empty will default to UUID

    // upload events
    data class UploadRecording(val recording: RecordingEntity) : ViewEvent()
    data object UploadAllLocalRecordings : ViewEvent()
}
```

You can see an example of how this is all used in [`MainActivity`](/src/main/com/siro/demo/MainActivity.kt)
