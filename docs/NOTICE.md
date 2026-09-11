# Kishi Switch — user guide

English | [Français](NOTICE.fr.md)

## Install or update

Download the APK from [Releases](https://github.com/Maugrey/kishiswitch4gw/releases) and install it over your existing version, without uninstalling the app. Your hardware profile and both preferences are retained. Updates from 0.1.2 also retain validations. When upgrading from 0.1.1, repeat the guided tests because the transport has changed.

Start Shizuku and authorize Kishi Switch in Shizuku. Then enable **Kishi Switch — manette** in Android's accessibility settings. The [Shizuku guide](https://shizuku.rikka.app/guide/setup/) explains starting it without root through wireless debugging.

Since version 0.1.2, the app uses a HID controller relay. Accessibility detects the game window and keyboard, and displays the floating controls. It no longer intercepts buttons or motion events. Version 0.1.3 added licensing and attribution while retaining this transport.

The app's controls currently have French labels, shown below alongside their English meanings.

## Verify the inversions

You do not need to repeat an identification already saved with LT = `AXIS_LTRIGGER`, RT = `AXIS_RTRIGGER` and right vertical = `AXIS_RZ`.

1. Open the diagnostic screen and start **Transmission intacte** (passthrough). Release all buttons, triggers and sticks while connecting. Try the controls in the game, return to Kishi Switch, and confirm only the result you actually observed.
2. Start **Compétences** (skills). Test LT with each face button, then RT with each face button. A and X are swapped, as are B and Y. Face buttons alone keep their usual behavior.
3. Start **Stick droit** (right stick). Check vertical inversion at small and large deflections, return to neutral, the left stick and the right stick's horizontal axis. Confirm if everything works.

Tests run for at most three minutes and stop when you leave Guild Wars. You can also stop them from the GW bubble. Both features are enabled by default after confirmation, then follow your last saved choices.

If the relay asks you to release a control, release the controller and leave and reopen the game, or restart the test. The LT/RT threshold can be adjusted in the diagnostic screen. It initially uses 50% of the trigger range, with a five-percentage-point release margin.

## Use the floating controls

- Tap **GW** to open **Compétences** (skills) and **Vertical stick droit** (right stick vertical). Drag the bubble to move it.
- Setting changes wait until the affected buttons are released or the right stick returns to neutral.
- To stop the relay, turn both switches off and release all controls, including the left stick.
- Your usual keyboard stays selected. The relay stops outside Guild Wars, while typing and when the phone locks.
- This version uses the overlay. The M2 shortcut is unavailable with the new relay.

## Stop or resume

**Arrêter entièrement le service** (stop the service completely) in Kishi Switch disables its accessibility service. The relay releases the Kishi and removes its virtual controller.

After restarting your phone, start Shizuku again. If the connection is interrupted, use **Autoriser / reconnecter Shizuku** (authorize / reconnect Shizuku). A periodic connection check releases the Kishi if the app stops responding. The app sends no recurring notifications.

If something fails, return to Kishi Switch and open **Afficher le compte rendu** (show report). Do not confirm a failed test. If only vertical inversion fails, leave it unvalidated: validated skills remapping remains usable.

This version's HID profile is limited to the Kishi V2 Pro 1532:0717 and the axes observed on the OnePlus. Game and system updates require repeating the tests. Android and Shizuku may still show their own system indicators.

## License and attribution

**Licences et attribution** (licenses and attribution) on the main screen provides offline access to PolyForm Noncommercial 1.0.0, Maugrey's attribution and third-party licenses. Links to the original source open in your browser. Viewing these texts does not activate the relay or require any permission.
