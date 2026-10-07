# Kid Timekeep

Local-only Android app (phone + tablet): hourglass timers drawn as coloured sand with emoji pictures,
presets, time's-up alarms, English and Latvian. Single `:app` module, package `lv.zarin.timekeep`.
Kotlin, Jetpack Compose, Room (KSP), DataStore, no DI framework (manual wiring in `AppContainer`).

## Build and test

Gradle needs the JDK bundled with Android Studio (the repo requires toolchain 25):

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug            # build
./gradlew testDebugUnitTest        # unit tests (JUnit4, Robolectric)
./gradlew lintDebug                # lint (abortOnError; HardcodedText is an error)
```

CI (`.github/workflows/android.yml`) runs all three on pull requests and pushes to `main`, using JDK 25.
Run them from the repo root before opening a PR.

## Release

`versionName` is `1.0.0` (bump `versionCode` for every upload). R8 minify + resource shrinking are on for `release`.
Signing reads an untracked `keystore.properties` at the repo root (never commit it or any `*.jks`):

```
storeFile=/absolute/or/repo-relative/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

```
./gradlew assembleRelease   # app/build/outputs/apk/release/ (app-release-unsigned.apk if no keystore.properties)
./gradlew bundleRelease     # .aab for Play
```

Play Console: the app declares `USE_EXACT_ALARM` (a countdown timer is a permitted core use), so the
exact-alarm permission declaration form must be completed for the listing.

## Package layout (`app/src/main/java/lv/zarin/timekeep/`)

- `domain/`: pure Kotlin. Models, time maths, look picker, hourglass geometry, ports (interfaces), `TimerService`.
- `data/`: Room (`db/`, `repo/`) and DataStore (`settings/`) adapters implementing domain ports.
- `alarm/`: AlarmManager scheduler, receivers, notifications.
- `ui/`: Compose screens and ViewModels (`theme`, `nav`, `hourglass`, `common`, `home`, `timer`, `edit`, `settings`).

## Rules

- Domain is pure Kotlin: no `android.*`, `androidx.*` (except `androidx.annotation`) or `java.*` imports.
  Use `kotlin.uuid.Uuid` and `kotlin.random.Random`. A unit test enforces this.
- Strings only in resources: all UI text goes in `strings.xml` (and its translations), never in code.
  Lint treats `HardcodedText` as an error. The app name "Kid Timekeep" is `translatable="false"`.
- Time is derived from stored timestamps, never counted. Remaining time rounds up, elapsed rounds down.
- No network permission and no foreground service. Alarms use `USE_EXACT_ALARM` + `setExactAndAllowWhileIdle`.
- minSdk 30, targetSdk/compileSdk 37. `applicationId` and namespace stay `lv.zarin.timekeep`.

## Git and PRs

- Work is split into stacked PRs on branches named `mvp/NN-name` (e.g. `mvp/01-foundation`).
  Each branch is based on the previous one; each PR targets the previous branch, not `main`.
- One commit per task, conventional style (`build:`, `feat:`, `test:`, ...).
- Never push to `main`, force-push, or merge PRs without review.
