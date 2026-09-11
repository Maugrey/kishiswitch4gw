# Kishi Switch pour Guild Wars

Application personnelle pour Android 16, écrite en Kotlin et Android Views. Elle utilise Shizuku, installé séparément, sans root ni remplacement du clavier.

**Version 0.1.2 : nouveau relais de manette HID.** La réinjection Android de la 0.1.1 ne fonctionnait pas dans Guild Wars. Un prototype utilisant EVIOCGRAB et UHID via l’identité shell a ensuite retransmis les sticks et les boutons correctement sur le OnePlus, résultat confirmé par l’utilisateur. Ce mécanisme est intégré à la 0.1.2 ; les inversions restent soumises aux essais guidés dans le jeu. Voir le [diagnostic détaillé](docs/DIAGNOSTIC-TRANSMISSION.md) et le [compte rendu actuel](docs/VALIDATION.md).

## Installation et utilisation

Télécharger l’APK personnel signé dans les [releases du dépôt privé](https://github.com/Maugrey/kishiswitch4gw/releases). La connexion au compte GitHub ayant accès au dépôt est nécessaire. La version 0.1.2 est une préversion avec nouveau transport.

Voir la [notice française](docs/NOTICE.md), puis le [compte rendu et les essais restants](docs/VALIDATION.md).

Le comportement programmé est A ↔ X et B ↔ Y pendant LT/RT, et l’inversion haut/bas du **stick droit** indépendamment des gâchettes. Les boutons seuls gardent leur rôle. Les deux préférences démarrent activées, mais ne deviennent effectives qu’après leur validation séparée. Le paquet `net.arena.guildwars.reforged` et les axes LT/RTRIGGER/RZ sont confirmés sur le OnePlus. Le relais cible le profil matériel Kishi V2 Pro 1532:0717 ; les commandes passent par l’overlay, M2 restant indisponible dans cette version.

## Compiler sous Windows

Versions fixées : Gradle Wrapper 8.13 (archive vérifiée par SHA-256), AGP 8.13.2, Kotlin 2.2.21, JDK 21 d’Android Studio, SDK 36 et Build Tools 36.1.0. `minSdk`, `compileSdk` et `targetSdk` valent 36.

```powershell
# APK de développement + tests du moteur + lint
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -Check

# APK personnel signé + contrôles
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -Release -Check

# Réunir l’APK, les sources, la notice et les empreintes dans dist/
powershell -ExecutionPolicy Bypass -File .\scripts\package.ps1

# Tests Android, avec UN émulateur/appareil de test connecté
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_SERIAL = 'emulator-5580'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Les scripts acceptent `-Jdk` et `-Sdk` si les chemins diffèrent. Android Studio peut aussi ouvrir directement ce dossier. Pour compiler ailleurs, renseigner `ANDROID_HOME` ou un `local.properties` contenant `sdk.dir=...`, utiliser Java 21 et exécuter `./gradlew :engine:test :app:assembleDebug :app:lintDebug`.

La première compilation signée crée la clé et ses propriétés **hors du dépôt**, dans `%LOCALAPPDATA%\KishiSwitch\signing`, avec des droits limités au compte Windows et à SYSTEM. Sauvegarder ce dossier en lieu sûr : la même clé est nécessaire pour installer les mises à jour sans désinstaller l’app. Les mots de passe ne sont pas affichés ni inclus dans les sources. Pour une clé existante, définir `KISHI_SIGNING_PROPERTIES` vers le fichier privé contenant `storeFile`, `storePassword`, `keyAlias` et `keyPassword`.

Sorties Gradle : `app/build/outputs/apk/debug/app-debug.apk` et `app/build/outputs/apk/release/app-release.apk`. Le paquet de livraison est dans `dist/`. Les dépendances sont fixées ; les octets de l’APK peuvent varier avec l’environnement de compilation.

## Organisation

- `engine/` : machine à états et codage HID indépendants d’Android, seuil analogique avec hystérésis, correspondance figée entre appui et relâchement, changements différés au repos.
- `app/.../input/` : accessibilité pour le premier plan, le clavier, le verrouillage et l’overlay ; aucune interception des commandes.
- `app/.../bridge/` : AIDL interne, UserService Shizuku, lecture exclusive de la Kishi et transmission à une manette UHID ; contrôle de l’appelant et arrêt automatique sur perte de connexion.
- `app/.../data/` : préférences, profil matériel et validations liées au profil, au seuil, à la version du jeu et au système.
- `app/.../ui/` : écran principal, identification guidée, essais temporaires et overlay sans focus clavier.

Le service d’accessibilité consulte le paquet de la fenêtre applicative et la présence du clavier, sans lire le texte saisi. Pas de permission Internet, pas de serveur, compte, télémétrie, service de clavier ou notification propre à l’app. Le journal local borné ne contient que les étapes de diagnostic ; son partage exige une action explicite dans l’interface.

## Limite technique déterminante

La capture par l’accessibilité et la réinjection InputManager ont échoué dans Guild Wars. Le nouveau relais lit le périphérique Linux identifié et crée une manette UHID reconnue par Android. Il dépend des permissions shell d’OxygenOS et du format matériel de cette Kishi. Une mise à jour système peut imposer une nouvelle validation.

UHID ne permet pas de cibler un UID par événement : la distribution normale d’Android est utilisée. Le service d’accessibilité commande l’arrêt sur changement de fenêtre, saisie et verrouillage. La Kishi est également libérée sur erreur, mort du client ou expiration de la liaison de contrôle. Les essais expirent après trois minutes. Aucun échange global XYAB n’est utilisé comme remplacement.

Références : [onMotionEvent et interception des sources](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onMotionEvent(android.view.MotionEvent)), [InputDispatcher Android 16](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/android16-release/services/inputflinger/dispatcher/InputDispatcher.cpp), [Shizuku API](https://github.com/RikkaApps/Shizuku-API), [compatibilité AGP](https://developer.android.com/build/releases/agp-8-13-0-release-notes).
