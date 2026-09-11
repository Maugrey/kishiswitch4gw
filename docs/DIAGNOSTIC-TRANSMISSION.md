# Transmission intacte — diagnostic du 11 septembre 2026

## Résultat sur le téléphone avec 0.1.1

Le problème persiste sur le OnePlus CPH2449 sous Android 16 : aucune action de la manette n’est visible dans Guild Wars pendant l’essai. L’expiration de l’essai rétablit les commandes natives.

Le compte rendu fourni par l’utilisateur établit les points suivants :

- Shizuku est connecté ; la réception locale donne deux événements de boutons et un mouvement. Le regroupement des mouvements par Android est possible.
- Profil enregistré : LT = `AXIS_LTRIGGER` (17), RT = `AXIS_RTRIGGER` (18), vertical droit = `AXIS_RZ` (14).
- Pendant l’essai, l’interception des mouvements est activée et le filtrage des boutons est désactivé.
- 1 117 mouvements de la Kishi ont été capturés ; 1 117 injections de mouvements ont été acceptées par Android.
- Aucune injection de bouton n’a été effectuée, ce qui est attendu pour la transmission intacte.
- Aucun échec technique d’injection n’est enregistré. L’interception est désactivée à la fin de l’essai.

**Conclusion limitée :** le rapport ne montre ni absence de capture ni refus d’injection par Android. Il ne prouve pas que Guild Wars reçoit ou traite ces événements, et ne suffit pas à établir la cause du blocage des boutons physiques.

## Essai supplémentaire sur émulateur Android 16

Le parcours complet a été testé avec une manette HID simulée, enregistrée par la commande Android `hid`, et un récepteur Android séparé. Ce récepteur utilise le paquet cible uniquement dans l’émulateur afin d’exercer le code non modifié de Kishi Switch 0.1.1. Ce n’est pas le client Guild Wars et il n’est pas distribué dans les releases.

La manette simulée est identifiée dans le diagnostic de Kishi Switch. Ses axes LT, RT et RZ sont calibrés par les commandes HID. Les essais utilisent Shizuku avec l’identité shell et le service d’accessibilité de l’app.

| Situation | Boutons reçus par le récepteur | Mouvements reçus par le récepteur |
|---|---|---|
| Commandes natives | Identifiant de manette `16` | Identifiant de manette `16` |
| Transmission intacte active | Identifiant `16`, appui et relâchement reçus, y compris après les mouvements injectés | Identifiant virtuel `-1`, valeurs X, LT et RZ reçues, retours au neutre compris |
| Retour dans Kishi Switch puis dans le récepteur | Identifiant `16` | Identifiant `16` restauré |

L’identifiant `16` appartient uniquement au périphérique de test. Sur le téléphone, la Kishi avait l’identifiant `15` au moment du compte rendu ; ces identifiants peuvent changer lors d’une reconnexion.

Le récepteur a reçu les sept mouvements de la séquence X, LT et RZ pendant la transmission intacte. Le blocage de tous les boutons observé dans Guild Wars n’est pas reproduit dans ce récepteur sur l’émulateur.

Les journaux et le code du récepteur expérimental sont conservés localement dans `artifacts/transport-fixture/`, hors du dépôt et des archives distribuées. L’APK livré n’a pas été modifié pour cet essai.

## Interprétation et suite

Le changement d’identité des mouvements est confirmé par l’essai sur émulateur et correspond au code Android 16 : l’injection ordinaire attribue l’identifiant virtuel `-1`. Les boutons natifs et les mouvements injectés n’appartiennent donc plus au même périphérique pour l’application destinataire.

Cela constitue une hypothèse de travail sérieuse pour la compatibilité avec Guild Wars. Le comportement interne du jeu n’a cependant pas été inspecté, et une particularité d’OxygenOS reste possible. Il n’est pas établi que Guild Wars utilise une bibliothèque particulière ni qu’il rejette tous les événements synthétiques par principe.

La prochaine vérification doit se faire sur le OnePlus : état du répartiteur d’entrées et du périphérique pendant l’essai, fenêtres ciblées, traces d’injection et réception des boutons. Une connexion ADB sans fil au PC est nécessaire pour ces lectures ; aucun téléphone n’est encore connecté au moment de ce compte rendu.

Aucune nouvelle version corrective n’est certifiée à ce stade. Une modification du mode de transmission devra préserver les axes analogiques, le clavier habituel et le fonctionnement limité au jeu ; un échange global XYAB ne résout pas ce problème.

Références : [commande HID Android 16](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/cmds/hid/README.md), [identité des événements injectés dans InputDispatcher](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/android16-release/services/inputflinger/dispatcher/InputDispatcher.cpp), [capture des sources par onMotionEvent](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onMotionEvent(android.view.MotionEvent)).
