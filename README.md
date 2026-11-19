# Coconut Jungle Rush Auto Player (prototype)

This is an experimental Android 17 project based on the supplied Coconut Jungle Rush gameplay recording.

## What is included

- Android app with Start/Stop controls.
- User-approved screen capture using MediaProjection.
- User-enabled AccessibilityService that can dispatch continuous gesture strokes.
- Lightweight color/shape heuristic for the brown coconut and orange horizontal bars.
- A conservative steering heuristic that only attempts to move when it detects a nearby bar intersecting the coconut's lane.
- GitHub Actions workflow that builds a debug APK in the cloud.

## Important limitations

This is a **prototype, not a proven game-winning bot**. The detector is heuristic and has not been validated on the phone or against every game scene. It can miss obstacles, mistake background objects for the coconut, or choose an unsafe route. Test only while watching the phone, and stop immediately if it steers incorrectly. The current steering logic is intentionally simple; robust play will require tuning with additional gameplay captures and on-device testing.

The game listing says the coconut can be moved in all four directions and the supplied recording shows orange horizontal bars. The code uses those visual cues; it does not access the game's internal state.

## Build the APK using GitHub Actions (no Android Studio needed)

1. On your laptop or phone, create a new **public or private** GitHub repository, for example `coconut-jungle-auto-player`.
2. Download this ZIP and extract it.
3. Upload the *contents* of the extracted folder to the repository root (not the enclosing folder). The `.github/workflows/build-apk.yml` file must be present in the repository.
4. Commit the files to the `main` branch.
5. Open the repository on GitHub and select **Actions**.
6. Open **Build Coconut Auto Player APK**. If it did not start automatically, choose **Run workflow**.
7. Wait for the green successful run. Open that run, scroll to **Artifacts**, and download `coconut-auto-player-debug-apk`.
8. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Transfer it to your Android 17 phone and install it. Android may ask you to allow installation from that browser/file manager.

## First launch

1. Open Coconut Auto Player.
2. Tap **Enable gesture accessibility** and enable the service named "Coconut Auto Player gesture control".
3. Return to the app.
4. Tap **Grant capture & start** and approve Android's screen-sharing prompt.
5. Switch to Coconut Jungle Rush. Keep the phone visible and watch the first tests.
6. Use **Stop automation** to stop capture and gestures.

Android may show a persistent notification while screen capture is active. This is expected.

## Technical notes

- `compileSdk = 37`, `targetSdk = 37` (Android 17 / API 37).
- Minimum supported Android version is API 26.
- The APK produced by GitHub Actions is a debug-signed test build, not a Play Store release.
- No credentials, analytics, or network requests are included in the app.
