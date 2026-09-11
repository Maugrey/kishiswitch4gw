# Notice — Kishi Switch

[English](NOTICE.md) | Français

## Installation ou mise à jour

Télécharger l’APK dans les [releases](https://github.com/Maugrey/kishiswitch4gw/releases) et l’installer par-dessus la version précédente, sans désinstaller l’app. Le profil matériel et les deux préférences restent mémorisés. Une mise à jour depuis la 0.1.2 conserve également les validations. En venant de la 0.1.1, les essais doivent être refaits car le transport a changé.

Shizuku doit être démarré et Kishi Switch autorisé dans Shizuku. Activer ensuite « Kishi Switch — manette » dans les paramètres d’accessibilité. Le [guide Shizuku](https://shizuku.rikka.app/guide/setup/) explique le démarrage sans root par débogage sans fil.

Depuis la version 0.1.2, l'application utilise un relais de manette HID : l’accessibilité sert à reconnaître la fenêtre du jeu, le clavier et à afficher la pastille. Elle n’intercepte plus les boutons ni les mouvements. La 0.1.3 ajoute la licence et les attributions ; elle conserve ce transport.

## Choisir la langue de l'application

La version 0.1.5 propose toute l'interface en français et en anglais. Elle suit la langue du téléphone, avec l'anglais comme langue de repli pour les autres langues. **Langue de l'application** sur l'écran principal ouvre les paramètres Android, où choisir le français, l'anglais ou la langue du système. Ce choix conserve le profil matériel, les préférences d'inversion et les validations effectuées.

Le diagnostic et les messages du relais utilisent la langue choisie. Les anciennes lignes du journal conservent leur langue d'origine ; les messages techniques fournis par Android ou Shizuku peuvent avoir leur propre langue. Les textes officiels des licences restent dans leur langue d'origine. Les mentions des composants tiers comprennent une section anglaise et une section française complètes.

## Vérifier les inversions

Il n’est pas nécessaire de refaire une identification déjà enregistrée avec LT = AXIS_LTRIGGER, RT = AXIS_RTRIGGER et vertical droit = AXIS_RZ.

1. Ouvrir le diagnostic et lancer **Transmission intacte**. Relâcher boutons, gâchettes et sticks pendant la connexion. Vérifier les commandes dans le jeu, revenir dans Kishi Switch et confirmer seulement le résultat observé.
2. Lancer **Compétences**. Tester LT + chacun des quatre boutons, puis RT + chacun des quatre boutons. A et X sont échangés, ainsi que B et Y. Les boutons seuls restent identiques.
3. Lancer **Stick droit**. Vérifier l’inversion haut/bas à faible et forte amplitude, le retour au neutre, le stick gauche et l’horizontal droit. Confirmer si tout fonctionne.

Les essais durent au maximum trois minutes et s’arrêtent en quittant Guild Wars. La pastille GW permet aussi de les arrêter. Les deux fonctions sont activées par défaut après leur confirmation, puis suivent les derniers choix mémorisés.

Si le relais demande de relâcher une commande, relâcher la manette puis quitter et rouvrir le jeu, ou recommencer l’essai. Le seuil LT/RT reste réglable dans le diagnostic, initialement à 50 % de la course, avec une marge de relâchement de 5 points.

## Utilisation

- Toucher **GW** pour accéder à **Compétences** et **Vertical stick droit**. Faire glisser la pastille pour la déplacer.
- Les réglages attendent le relâchement des boutons concernés ou le retour au neutre du stick droit.
- Pour arrêter le relais, éteindre les deux interrupteurs et relâcher toutes les commandes, y compris le stick gauche.
- Le clavier habituel reste sélectionné. Le relais s’arrête hors de Guild Wars, pendant la saisie et au verrouillage.
- Cette version utilise l’overlay. Le raccourci M2 n’est pas disponible avec le nouveau relais.

## Arrêter ou reprendre

« Arrêter entièrement le service » dans Kishi Switch désactive son service d’accessibilité. Le relais libère la Kishi et retire sa manette virtuelle.

Après un redémarrage du téléphone, relancer Shizuku. Si la connexion est interrompue, utiliser « Autoriser / reconnecter Shizuku ». Un contrôle périodique de connexion libère la Kishi si l’application ne répond plus ; aucune notification répétée n’est envoyée.

En cas de problème, revenir dans Kishi Switch et consulter « Afficher le compte rendu ». Ne pas confirmer un essai qui échoue. Si seule l’inversion verticale échoue, laisser cette capacité non validée : les compétences validées restent utilisables.

Le profil HID de cette version est limité à la Kishi V2 Pro 1532:0717 et aux axes observés sur le OnePlus. Les mises à jour du jeu ou du système nécessitent de refaire les essais. Android et Shizuku peuvent conserver leurs propres indications système.

## Licence et attribution

« Licences et attribution » sur l'écran principal donne accès hors ligne à la
licence PolyForm Noncommercial 1.0.0, à l'attribution de Maugrey et aux licences
des composants tiers. Les liens vers le code d'origine s'ouvrent dans le navigateur.
La consultation des mentions n'active aucun relais et ne demande aucune permission.
