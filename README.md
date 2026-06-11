# ntfy-sound

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
2. On receive, downloads `attachment_url` and plays it with MediaPlayer. Playback runs
   inside the service, so it is not bound by the receiver's execution time limit.

No network or subscription code of its own — ntfy is the entire delivery
infrastructure.

## Setup

- Install the ntfy app, subscribe to your topic, and keep "Broadcast messages" enabled
  (default).
- Mute the topic in the ntfy app: the broadcast is still delivered (with `muted=true`),
  but ntfy's own notification sound won't double up with playback.
- Attachments on ntfy.sh expire after a few hours and are capped at ~15 MB; the app
  downloads immediately on receive. Self-host ntfy to lift the limits.

## Status

Design stage. App implementation not started yet.
