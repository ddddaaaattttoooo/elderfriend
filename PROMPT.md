# Set up Elderfriend with a coding assistant

Copy everything below into Claude Code, Codex, Gemini CLI or any assistant that
can run shell commands on your computer. It will walk you through installing
Elderfriend on an Android phone over USB, in the same spirit as
[CleanYourTV](https://github.com/Gathin23/cleanyourtv): nothing is uninstalled,
every change has an undo, and it asks before touching anything.

---

You are helping me set up **Elderfriend**, a simple big-button home screen for
an older person, on an Android 10+ phone. Work step by step, explain each
command in one sentence before running it, and stop and ask me before anything
that changes the phone. Never disable a package I have not approved. Never
factory reset anything.

**Safety rules**

- Nothing on the phone gets uninstalled. Bluetooth, Wi-Fi, camera, phone calls,
  SMS, the Play Store and the keyboard must keep working.
- Do not remove accounts, do not change screen lock, do not touch `pm clear` on
  system apps.
- If a command fails, stop and show me the error instead of trying random fixes.
- The phone owner must be able to make and receive calls at the end. Test that.

**Step 1 — connect**

1. Ask me to enable Developer options: Settings → About phone → tap "Build
   number" seven times.
2. Ask me to turn on Settings → System → Developer options → **USB debugging**,
   then connect the phone by USB and tap **Allow** on the phone.
3. Run `adb devices` and confirm exactly one device shows `device` (not
   `unauthorized`). If it is unauthorized, ask me to accept the prompt.

**Step 2 — install Elderfriend**

1. Download the latest APK from
   `https://github.com/ddddaaaattttoooo/elderfriend/releases/latest` into a
   temporary folder, or use a local file if I have one.
2. Run `adb install -r <apk>`.
3. Grant the permissions the app asks for:
   `adb shell pm grant ge.elderfriend.launcher android.permission.READ_CONTACTS`
   and the same for `READ_CALL_LOG`, `CALL_PHONE`, `READ_PHONE_STATE`.

**Step 3 — make it the home screen**

1. Run `adb shell cmd package set-home-activity ge.elderfriend.launcher/.MainActivity`.
2. If that fails on my phone, open Settings → Apps → Default apps → Home app
   and pick Elderfriend, or let me do it on the phone.
3. Press Home and confirm Elderfriend opens. Ask me to open the first-run setup
   screen and choose the buttons the person actually uses.

**Step 4 — optional comfort tweaks (ask first, one at a time)**

Only do these if I say yes. Each has an undo in parentheses.

- Bigger interface: `adb shell wm density 500`
  (undo: `adb shell wm density reset`). Try 440 first on small phones.
- Larger text: `adb shell settings put system font_scale 1.3`
  (undo: `adb shell settings put system font_scale 1.0`).
- Longer screen timeout: `adb shell settings put system screen_off_timeout 300000`
  (undo: set your preferred value).

Do not set a density so high that the camera screen or settings get clipped:
open Elderfriend's Camera tile and the system Settings and check both.

**Step 5 — optional debloat (only with explicit approval)**

1. First back up what is installed:
   `adb shell pm list packages -f > packages-before.txt`.
2. Identify *only* obvious vendor junk (ads, duplicate stores, unused
   preinstalled games). Do not guess from names.
3. For each candidate, show me the package name and what it is, then wait for my
   approval before running
   `adb shell pm disable-user --user 0 <package>`.
4. Never disable: `com.android.*`, `com.google.android.dialer`,
   `com.google.android.apps.messaging`, the keyboard, `com.android.vending`
   (Play Store), or anything whose purpose is unclear.
5. To undo one: `adb shell pm enable <package>`.

**Step 6 — final check**

Walk me through this on the phone: call the phone from another number, answer
it, open Contacts and call someone back, send and receive an SMS, take a photo,
open the missed-calls list, and restart the phone to confirm Elderfriend comes
back by itself. If any step fails, help me fix it before finishing.

---

## Doing it without an assistant

`tools/setup.sh` performs steps 1–3 (install, permissions, home app). Read it
first — it only touches Elderfriend and asks for the APK path.
