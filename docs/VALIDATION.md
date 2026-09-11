# Validation report — version 0.1.5

English | [Français](VALIDATION.fr.md)

## Version 0.1.5 — English and French app interface

- Main screen, setup, diagnostics, trial instructions, floating controls, accessibility description and app-defined relay errors are available in English and French. Both resource sets contain the same 192 entries, with English as the default.
- Android's per-app language settings are accessible from the main screen. The Shizuku relay receives the selected resource language for its error messages. Language changes do not alter the hardware profile, inversion preferences or validation stamp.
- THIRD_PARTY_NOTICES.md contains complete English and French sections. Official license texts and Required Notice lines are unchanged; the APK includes the updated document.
- Android instrumentation: 12 tests passed on an Android 16 emulator, including four localization tests for language selection and fallback, formatting, relay error language and preservation of settings/validation stamps.
- Visual checks on the signed APK: main screen, diagnostic screen and report in both languages; the English license menu and bilingual third-party document; changing from English to French through Android settings while retaining the two switch states; overlay labels in both languages over a local test fixture. The fixture is not Guild Wars and these checks do not validate controller transmission.
- The remapping engine, HID report format, trigger thresholds and stored preference keys are unchanged. Language handling was added to the accessibility service and relay message context.

Release checks: signed build successful, same signing certificate, all nine legal assets matched to the repository, local documentation links valid, Android Lint with no errors and five warnings. Gradle reused the 17 successful engine test results because the engine is unchanged. The final targeted scan found no personal email, local username, private key or GitHub token in the APK and documents; Git metadata remains excluded from the APK.

No OnePlus or in-game controller test was performed for this localization update. User-confirmed gameplay still refers to 0.1.2.

## Version 0.1.4 — English and French distribution documents

The README, user guide and validation report are available in English and French. Attribution explanations in NOTICE and THIRD_PARTY_NOTICES.md are bilingual, including the copies bundled in the APK. Official license texts and both Required Notice lines are unchanged. The app interface remains in French; the English user guide includes the corresponding French button labels.

- Signed release build completed; APK signature verified with the same certificate as 0.1.3.
- All nine embedded legal documents match their repository files. Official license texts and the two Required Notice lines were checked against 0.1.3.
- Local links in both sets of documents resolve correctly.
- Android Lint: no errors, 17 existing warnings. Gradle reused the 17 successful engine test results; engine sources are unchanged.
- No personal email, local username, private key or GitHub token was detected by the targeted scan of the APK and distribution documents. Git metadata remains excluded from the APK.
- No application Kotlin code was changed. The app version was incremented to distribute the updated embedded notices; preferences and validation stamps are unchanged.

No new test on the OnePlus has been performed for this documentation update. The user-confirmed gameplay results below concern version 0.1.2.

## Version 0.1.3 — licensing and attribution

- Remapping engine, HID relay and settings storage unchanged from 0.1.2.
- Signed build completed using the same personal key; APK signature verified.
- All 17 engine tests remained successful; Gradle reused their results because their inputs had not changed.
- Android Lint: no errors, 17 existing warnings.
- All nine legal documents in the APK matched their repository files; all nine interface entries pointed to an included document.
- PolyForm Noncommercial 1.0.0 matched the official text; two `Required Notice:` lines retained Maugrey's attribution and the repository link.
- Git metadata was no longer added to the APK. No personal email or personal profile path was detected in its contents.
- No new OnePlus test was performed for this license update.

## Version 0.1.2 — device tests on September 11, 2026

Version 0.1.1 captured motions and reinjected them with Android's virtual device ID -1. Testing on the OnePlus CPH2449 confirmed this identity change while Guild Wars retained focus and input queues were not blocked. The controls produced no visible action in the game.

A separate prototype then read the physical Kishi device, acquired EVIOCGRAB without root and forwarded controls through UHID. Android registered that controller with its own ID and assigned both motions and buttons to it. **The user confirmed that everything worked during a 45-second test in Guild Wars.** The prototype counted 4,126 raw events and 1,613 HID reports, then released the Kishi automatically.

After installing APK 0.1.2 and being asked to check passthrough, skills and the right stick in turn, **the user again confirmed that everything worked.** Passthrough and both inversions are therefore user-confirmed in the integrated application, in Guild Wars on the OnePlus CPH2449, with the Kishi V2 Pro and Shizuku without root.

### Checks for version 0.1.2

- Personally signed build: successful, using the same key as the previous version.
- Kotlin tests: 17 passed, including five new HID encoding tests covering the eight combinations, face buttons alone, mapping collisions, simultaneous analog/button ordering, neutral and full range, other axes, deferred changes and repetitions.
- Android Lint: no errors.
- Update installed over 0.1.1 on the OnePlus: successful.
- Historical Android instrumentation tests: eight passed with 0.1.1 on an emulator. These tests were not run on the personal phone because they clear test preferences.
- Gameplay with APK 0.1.2: passthrough, skills inversion and right stick vertical inversion confirmed by the user.

### Required architecture change

UID-targeted InputManager injection is no longer used for the game. The Shizuku UserService reads only the identified physical controller, suspends its native delivery with EVIOCGRAB and feeds a UHID controller. Other devices, including the keyboard, are not captured.

UHID uses Android's normal input dispatch, with no per-event UID target. Binder caller checks and the allowed package check remain in place. Accessibility stops the relay when the foreground window changes, the keyboard appears or the phone locks. A time-limited control lease and client death also stop the relay. The 250 ms connection check does not poll for the foreground app.

Axes retain their native 8-bit format, range and precision. RZ is inverted as 255 minus its raw value; both central values, 127 and 128, stay within Android's neutral zone. An experimental 16-bit format was discarded after Android applied different precision filtering. Changes within each report are grouped before choosing button mappings.

M2 remains unavailable in this version; use the two overlay switches. No global XYAB swapping or modification of the Guild Wars client was introduced.

### Scenarios without individually recorded results

- Simultaneous movement and camera control, different press/release orders, and small right stick deflections.
- All four switch combinations, bubble position and deferred changes.
- The usual keyboard in the game and another app.
- Switching apps, locking, unplugging the controller and stopping Shizuku.
- Restarting the phone and starting Shizuku again.
- A session of at least twenty minutes under OxygenOS.

The user's overall confirmation does not provide separate results for these scenarios; they are not presented as individually verified.

Raw Android logs and game APKs used for read-only diagnosis remain local, excluded from the repository and distributed archives.
