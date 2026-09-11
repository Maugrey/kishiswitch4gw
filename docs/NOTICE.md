# Notice — Kishi Switch 0.1.2

## Installation ou mise à jour

Installer `KishiSwitch-0.1.2.apk` par-dessus la version précédente, sans désinstaller l’app. Le profil matériel et les deux préférences restent mémorisés. Les validations précédentes sont remises à zéro car le transport a changé.

Shizuku doit être démarré et Kishi Switch autorisé dans Shizuku. Activer ensuite « Kishi Switch — manette » dans les paramètres d’accessibilité. Le [guide Shizuku](https://shizuku.rikka.app/guide/setup/) explique le démarrage sans root par débogage sans fil.

La version 0.1.2 utilise un relais de manette HID : l’accessibilité sert à reconnaître la fenêtre du jeu, le clavier et à afficher la pastille. Elle n’intercepte plus les boutons ni les mouvements.

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
