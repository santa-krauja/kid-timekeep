# Emulator QA pass (Task 27)

Device: `emulator-5554` (Medium_Phone_API_37.0, host GPU), 1080x2400 phone; tablet simulated with `wm size 2560x1600` + `wm density 320`. Build: debug (`./gradlew installDebug`), branch `mvp/07-quality`. First launch after `pm clear`.
Screenshots are in this folder (resized to <= 900 px tall).

## 1. Wireframe flows

| Screen | EN light | EN dark | LV light | Result |
|---|---|---|---|---|
| A Home, first launch (3 example presets) | `en-light-A-home-first.png` | - | - | PASS |
| B Home with timers (running, paused, Done!) | `en-light-B-home-timers.png` | `en-dark-B-home-timers.png` | `lv-light-B-home.png` | PASS |
| C Running timer | `en-light-C-running.png` | `en-dark-C-running.png` | `lv-light-C-running.png` | PASS (no edit pencil, see Open issues) |
| C2 Start over: confirm dialog, then flip | `en-light-C2-flip.png` (mid-flip, dialog fading) | - | - | PASS (rotation plays, restarts at 15:00, same look) |
| D Paused (frozen sand, "Paused" chip, dimmed glass, Go on) | `en-light-D-paused.png` | - | - | PASS |
| E1 New timer | `en-light-E1-new.png` | - | `lv-light-E1-new.png` | PASS (name required: inline error) |
| E2 Pictures & colours + favourite saved | `en-light-E2-favourite.png` | `en-dark-E2-pictures.png` | `lv-light-E2-pictures.png` | PASS |
| F Time's up overlay | `en-light-F-timesup.png` | `en-dark-F-timesup.png` | `lv-light-F-timesup.png` | PASS |
| G Settings; language + theme dropdowns | `en-light-G-settings.png` | (theme set to Dark via dropdown, whole app turned dark live) | `lv-light-G-settings.png` | PASS |
| H Tablet (list + detail, Time's up over both panes) | - | - | `lv-light-H-tablet.png` | PASS |
| H Phone landscape (side-by-side glass + digits) | - | - | `lv-light-H-landscape-timer.png` | PASS |

Other observations:
- Notification-permission rationale ("Let Kid Timekeep tell you when time is up?") appears on first Start, then the system dialog; both work, Settings shows "Allowed".
- Latvian plurals seen live on a running app: "1 minūte 55 sekundes", "14 minūšu 47 sekundes", "1 minūte 12 sekunžu" (all correct). Language switch applies immediately, no restart.
- Reopening a finished timer shows Time's up again without re-ringing (one `TimeUpFeedback` line per finish).
- Tablet and landscape: layouts adapt; nothing clipped.

## 2. Device checks

| # | Check | Result | Evidence |
|---|---|---|---|
| a | Dead-process alarm | PASS | 30 s timer, Home, `am kill` (`pidof` empty), `deviceidle force-idle`; alarm was `RTC_WAKEUP ... TimeUpReceiver exactAllowReason=policy_permission`. At +30 s the notification "Test - Time's up!" was present (`en-light-2a-deadprocess-notification.png`); process was re-spawned by the receiver. Tapping it opened the Test timer with the Time's up overlay. `deviceidle unforce` done. |
| b | Visible timer finish | PASS | Test timer left open for 30 s: overlay shown, exactly one `TimeUpFeedback: play sound=true vibrate=true` line, and the number of `NotificationRecord`s for our package did not change (no notification for the visible timer). |
| c | Reboot | PASS | Timer started (alarm origWhen 09:51:34), `adb reboot`: after boot `dumpsys alarm` lists the same `RTC_WAKEUP ... lv.zarin.timekeep/.alarm.TimeUpReceiver` with `origWhen=2026-10-07 09:51:34`. Note: on the emulator the BOOT_COMPLETED broadcast reaches the app 1-2 min after `sys.boot_completed` (system busy, load avg 16); in a first attempt the 3 min timer expired during that window, and a Time's up notification was posted on boot (the "already expired" branch also works). |
| d | Pseudo-locale en-XA | PASS | `adb shell cmd locale set-app-locales lv.zarin.timekeep --locales en-XA` works on this image (`en-xa-B-home.png`, `en-xa-C-timer.png`). Text expands ~30-40 %; chip rows wrap, the "+1 min" button wraps to a second row on the timer screen, titles do not clip. User-entered names stay untranslated (expected). RTL pseudo-locale (ar-XB) not tried. |
| e | Jank / ANR, 8 running timers on Home for 30 s | PASS (emulator caveat) | `dumpsys gfxinfo` after `reset` + 30 s: 1752 frames, janky 46 (2.63 %), p50 28 ms, p90 34 ms, p95 38 ms, p99 61 ms; GPU p90 20 ms; missed vsync 5. A later reading was 3.02 % janky. The emulator was running at 30 fps (frame-rate category throttling), so the 28 ms p50 is one 33 ms vsync interval, not slow frames. `am_anr` events for our package: 0. See `en-light-2e-eight-running.png`. |
| f | Cold start (`am start -W` after force-stop, debug build) | PASS | TotalTime 905 / 814 / 776 ms (3 runs, LaunchState COLD). |

## 3. Accessibility quick pass (uiautomator dumps, TalkBack not enabled)

- Icon-only buttons all carry content descriptions: Settings, New timer, Back, Close, Start over, Pause/Go on, "Start <preset>", "New look every time".
- Hourglass exposes a spoken summary: "Reading: 14 minutes 47 seconds left of 15 minutes" (LV: "...pauzēts"); cards read "Get dressed, 6 minutes 3 seconds left" / "Brush teeth: time's up".
- Colour and picture swatches are `RadioButton`s with `checked` state and names (Mint, Rocket...). Preview hourglass describes both looks.
- Digits have separate spoken text ("14 minutes 47 seconds").

## Fixes made

None needed: no clear, contained bug found.

## Open issues (design questions / minor)

1. Timer screen has no edit pencil (wireframes C and D show "<- Name ✎"). Not implemented in `TimerScreen.kt`; editing is only via long-press on a preset. Decide whether to add (one-off timers cannot be edited at all).
2. "Save as preset" is ON by default on New timer, so a quick one-off timer silently becomes a preset (the Home list gained "Test" after a test timer). Wireframe E1 text says "+ makes a one-off timer or a new preset". Consider default OFF.
3. Pictures & colours: the sand-colour rows do not scroll the selected swatch into view; when "Night" is selected it is clipped at the right edge (`lv-light-E2-pictures.png`). Minor.
4. A timer that finishes while the user is on Home (not on its own screen) posts a heads-up notification, as designed; just noting it differs from the "visible timer" case in 2b.
5. Frame stats were measured on an emulator limited to 30 fps; re-profile 8 cards on a real mid-range device.
6. TalkBack and an RTL pseudo-locale were not exercised.
