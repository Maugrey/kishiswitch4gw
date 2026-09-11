# Kishi Switch pour Guild Wars

[English](README.md) | Français

Application personnelle pour Android 16, écrite en Kotlin et Android Views. Elle utilise Shizuku, installé séparément, sans root ni remplacement du clavier.

**Version 0.1.4 : documentation en anglais et en français, explications des attributions bilingues dans l'APK.** L'interface de l'application reste en français. Le fonctionnement de la manette est celui de la 0.1.2 ; la licence PolyForm Noncommercial et les attributions sont consultables dans l'application depuis la 0.1.3.

**Version 0.1.2 : transmission et deux inversions confirmées par l’utilisateur dans Guild Wars sur le OnePlus.** La réinjection Android de la 0.1.1 a été remplacée par un relais EVIOCGRAB/UHID via Shizuku sans root. Après les essais de l’application intégrée, l’utilisateur a confirmé : « C’est bon, tout fonctionne ! » Les essais guidés restent requis sur une nouvelle installation ou après un changement de configuration. Voir le [diagnostic détaillé](docs/DIAGNOSTIC-TRANSMISSION.md) et le [compte rendu actuel](docs/VALIDATION.md).

## Installation et utilisation

Télécharger l’APK signé dans les [releases](https://github.com/Maugrey/kishiswitch4gw/releases). La version 0.1.4 est une préversion.

Voir la [notice française](docs/NOTICE.fr.md), puis le [compte rendu et les essais restants](docs/VALIDATION.fr.md).

Le comportement programmé est A ↔ X et B ↔ Y pendant LT/RT, et l’inversion haut/bas du **stick droit** indépendamment des gâchettes. Les boutons seuls gardent leur rôle. Les deux préférences démarrent activées, mais ne deviennent effectives qu’après leur validation séparée. Le paquet `net.arena.guildwars.reforged` et les axes LT/RTRIGGER/RZ sont confirmés sur le OnePlus. Le relais cible le profil matériel Kishi V2 Pro 1532:0717 ; les commandes passent par l’overlay, M2 restant indisponible dans cette version.

## Compiler sous Windows

Versions fixées : Gradle Wrapper 8.13 (archive vérifiée par SHA-256), AGP 8.13.2, Kotlin 2.2.21, JDK 21 d’Android Studio, SDK 36 et Build Tools 36.1.0. `minSdk`, `compileSdk` et `targetSdk` valent 36.

```powershell
# APK de développement + tests du moteur + lint
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -Check

# APK personnel signé + contrôles
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -Release -Check

# Depuis un clone Git sans changement en attente : réunir la livraison dans dist/
powershell -ExecutionPolicy Bypass -File .\scripts\package.ps1

# Tests Android : utiliser un émulateur jetable, car ils effacent les préférences
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_SERIAL = 'emulator-5580'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Les scripts acceptent `-Jdk` et `-Sdk` si les chemins diffèrent. Android Studio peut aussi ouvrir directement ce dossier. Pour compiler ailleurs, renseigner `ANDROID_HOME` ou un `local.properties` contenant `sdk.dir=...`, utiliser Java 21 et exécuter `./gradlew :engine:test :app:assembleDebug :app:lintDebug`.

La première compilation signée crée la clé et ses propriétés **hors du dépôt**, dans `%LOCALAPPDATA%\KishiSwitch\signing`, avec des droits limités au compte Windows et à SYSTEM. Sauvegarder ce dossier en lieu sûr : la même clé est nécessaire pour installer les mises à jour sans désinstaller l’app. Les mots de passe ne sont pas affichés ni inclus dans les sources. Pour une clé existante, définir `KISHI_SIGNING_PROPERTIES` vers le fichier privé contenant `storeFile`, `storePassword`, `keyAlias` et `keyPassword`.

Sorties Gradle : `app/build/outputs/apk/debug/app-debug.apk` et `app/build/outputs/apk/release/app-release.apk`. Le paquet de livraison est dans `dist/`. Les dépendances sont fixées ; les octets de l’APK peuvent varier avec l’environnement de compilation.

L'APK intègre automatiquement `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md` et `licenses/` depuis la racine du projet. Les archives de livraison proviennent du commit Git courant : elles n'incluent pas les fichiers locaux non suivis. Un clone Git est nécessaire pour `package.ps1` ; la compilation reste possible depuis l'archive de sources.

## Licence et attribution

Le code propre et la documentation de Kishi Switch sont disponibles sous
**[PolyForm Noncommercial 1.0.0](LICENSE)**. La modification et la redistribution
sont autorisées pour les usages prévus par cette licence, sans obligation de
publier les sources modifiées. Les conditions détaillées, dont les usages
expressément autorisés pour certains organismes, figurent dans le texte intégral.

Lors d'une redistribution, conserver ces mentions, présentes dans [NOTICE](NOTICE),
ainsi que la licence ou son URL :

```text
Required Notice: Kishi Switch - Copyright (c) 2026 Maugrey.
Required Notice: Original source: https://github.com/Maugrey/kishiswitch4gw
```

Ce projet propose un code source disponible pour un usage non commercial ; il
n'est pas présenté comme open source au sens de la définition de l'OSI.
Les bibliothèques tierces conservent leurs propres licences, détaillées dans
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Dans l'application, ouvrir
**« Licences et attribution »** pour consulter les textes et accéder au dépôt.

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
