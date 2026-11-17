# Installing Elderfriend

Elderfriend works on any phone running **Android 10 or newer** (Android 10–14
tested). It needs no root, no computer and no Google account.

## 1. Put the APK on the phone

Download `Elderfriend-1.0.0.apk` from the project's releases page and open it
from the phone's notification shade or **Files → Downloads**.

Android will ask to allow installing from this source (the browser or the file
manager). Allow it once, then tap **Install**.

## 2. Make it the home screen

Press the **Home** button (or swipe up from the bottom). Android shows a choice
of home apps:

1. Pick **Elderfriend**.
2. Tap **Always** (not "Just once").

If the choice does not appear, open **Settings → Apps → Default apps → Home
app** and choose Elderfriend.

## 3. Allow the permissions

On first start the screen asks for three permissions. They are used only on the
phone itself:

| Permission | What it enables |
|---|---|
| Contacts | the contact list and calling by name |
| Phone | placing the call after the confirmation |
| Call log | the Missed and Recent screens and the red missed-call badge |

If a permission was declined, open **Settings → Apps → Elderfriend →
Permissions** and allow it there. The two call tiles stay usable; the lists
simply stay empty until access is granted.

## 4. Optional, but recommended

### Click-to-answer phone calls

Many Android dialers require a swipe to answer an incoming call, which is hard
with shaky hands. [Fossify Phone](https://fossify.org) has a setting for that:

1. Install Fossify Phone and set it as the default **Phone** app.
2. In Fossify Phone open **Settings → Calls**.
3. Turn on **"Replace swiping at responding to incoming calls with clicking"**.
4. Turn on **"Always display incoming calls on full screen"**.

### Tiles for messaging apps

If WhatsApp, Viber, Facebook or Messenger are installed, their tiles appear on
the home screen automatically. If they are not installed, the tiles stay
hidden — nothing to configure or remove.

### Caregiver settings

Hold the clock at the top of the home screen for about **3 seconds**. The
caregiver screen lets you hide tiles, reset the tile order and open Android
settings. A quick tap on the clock does nothing, so it is hard to open by
accident.

## 5. Install with ADB (optional)

For a phone without a browser, or for a family member setting it up remotely:

```bash
adb install -r Elderfriend-1.0.0.apk
adb shell cmd package set-home-activity ge.elderfriend.launcher/.MainActivity
adb shell pm grant ge.elderfriend.launcher android.permission.READ_CONTACTS
adb shell pm grant ge.elderfriend.launcher android.permission.READ_CALL_LOG
adb shell pm grant ge.elderfriend.launcher android.permission.CALL_PHONE
adb shell pm grant ge.elderfriend.launcher android.permission.READ_PHONE_STATE
```

## Making the text bigger

Elderfriend is designed to be large, but if it still feels small, the system
setting helps:

**Settings → Display → Display size** (larger) and **Font size** (larger).

On some phones the same thing is available over ADB. Note that on Android a
*higher* value makes the interface *bigger*:

```bash
adb shell wm density 500     # bigger than the usual 400–480
adb shell wm density reset   # back to the phone's default
```

## Uninstalling

**Settings → Apps → Elderfriend → Uninstall**. Before that, set another app as
the Home app (Settings → Apps → Default apps → Home app), otherwise the phone
has nowhere to go when you press Home.
