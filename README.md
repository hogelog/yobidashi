# yobidashi

Android app that plays arbitrary audio pushed via [ntfy](https://ntfy.sh).

Publish an audio file as an ntfy attachment, and this app plays it on the phone:

```sh
curl -T sound.mp3 -H "Filename: sound.mp3" ntfy.sh/<topic>
```

## How it works

The [ntfy Android app](https://github.com/binwiederhier/ntfy-android) handles all
subscription, connection, and delivery, and rebroadcasts every received message as an
implicit `io.heckel.ntfy.MESSAGE_RECEIVED` intent. This app:

1. Runs a minimal foreground service that dynamically registers a receiver for that
   intent (manifest-declared receivers cannot receive it due to the implicit broadcast
   restriction since API 26).
2. On receive, streams `attachment_url` with MediaPlayer when the attachment is audio
   (by MIME type or file extension). Playback runs inside the service, so it is not
   bound by the receiver's execution time limit.

No network or subscription code of its own — ntfy is the entire delivery
infrastructure.

## Setup

- Install the ntfy app, subscribe to your topic, and keep "Broadcast messages" enabled
  (default).
- Mute the topic in the ntfy app: the broadcast is still delivered (with `muted=true`),
  but ntfy's own notification sound won't double up with playback.
- Attachments on ntfy.sh expire after a few hours and are capped at ~15 MB; the app
  plays immediately on receive. Self-host ntfy to lift the limits.
- On an access-controlled self-hosted server, attachment downloads under `/file/` also
  require auth. Set the server URL and an [access token](https://docs.ntfy.sh/config/#access-tokens)
  in the app; it sends `Authorization: Bearer` only for attachment URLs on that server.
- Playback uses the media volume stream.

## Install

Download the debug APK from the [main-debug prerelease](https://github.com/hogelog/yobidashi/releases/tag/main-debug),
built automatically from the latest main.

Open the app and tap Start to launch the listener foreground service. Received
messages and playback results show up in the in-app event log.

## Build

```sh
./gradlew assembleDebug
```

Requires JDK 21 and an Android SDK (`sdk.dir` in `local.properties` or `ANDROID_HOME`).
