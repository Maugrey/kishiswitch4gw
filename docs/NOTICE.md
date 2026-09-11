# Notice — Kishi Switch 0.1.1

## Mise à jour après le blocage des commandes

1. Désactiver temporairement le service d’accessibilité Kishi Switch dans Android.
2. Installer `KishiSwitch-0.1.1.apk` par-dessus la version précédente, sans la désinstaller. Le profil de la Kishi et les préférences sont conservés ; les essais devront être validés à nouveau.
3. Ouvrir Kishi Switch et réactiver son service de manette. Ouvrir Guild Wars **sans lancer d’essai** : vérifier d’abord que tous les boutons et les sticks fonctionnent normalement.
4. Si les commandes normales fonctionnent, essayer ensuite « Transmission intacte ». Si elles se bloquent, revenir dans Kishi Switch, toucher « Arrêter entièrement le service » et conserver « Afficher le compte rendu ». Ne pas confirmer l’essai.

La version 0.1.1 retire la demande permanente de filtrage des boutons. Le test « Transmission intacte » intercepte uniquement les mouvements ; les boutons physiques restent natifs. Le service remet également à zéro les interceptions héritées d’une précédente exécution. Cela ne garantit pas encore que Guild Wars acceptera les mouvements réinjectés.

## Première installation

1. Installer l’APK `KishiSwitch-0.1.1.apk` sur le OnePlus 11. Android peut demander d’autoriser ponctuellement l’installation depuis l’application utilisée pour ouvrir ce fichier.
2. Installer [Shizuku depuis son site officiel](https://shizuku.rikka.app/download/). Suivre son [guide de démarrage sans root](https://shizuku.rikka.app/guide/setup/) avec les options développeur et le débogage sans fil. Cela laisse le port USB-C disponible pour la Kishi.
3. Ouvrir Kishi Switch, toucher « Autoriser la connexion Shizuku » et accepter la demande de Shizuku.
4. Toucher « Activer le service de manette », puis activer Kishi Switch dans les paramètres d’accessibilité. Si Android bloque cette étape, les informations de l’application peuvent proposer, dans le menu ⋮, « Autoriser les paramètres restreints ».
5. Brancher la Kishi, garder le clavier habituel sélectionné et ouvrir « Identifier la Kishi et faire les essais ».

Après un redémarrage du téléphone, Shizuku doit être relancé. Les préférences et le profil restent mémorisés. L’app affiche simplement l’état de connexion ; elle n’envoie pas de rappels.

## Identification et essais indispensables

L’identification décrit une seule manette : ce n’est pas un éditeur général de remappage.

1. Appuyer sur un bouton de la Kishi pour la reconnaître.
2. Identifier successivement **LT**, **RT**, puis le **vertical du stick droit** : toucher le bouton d’identification, fermer l’explication avec « Compris », faire le geste demandé, puis toucher « Enregistrer la commande observée ». L’app affiche les axes réellement reçus.
3. Facultatif : tester M2. Si Android lui attribue une touche standard ou déjà observée, il ne sert pas d’interrupteur. La pastille flottante reste disponible.
4. « Tester la réception ici » vérifie la liaison Shizuku vers Kishi Switch. Une réception locale réussie ne garantit pas la compatibilité du jeu.
5. Effectuer **Transmission intacte** dans Guild Wars : mouvements, caméra, gâchettes, boutons, actions simultanées et retour au repos doivent fonctionner normalement, sans inversion. Revenir dans Kishi Switch et confirmer seulement le résultat réellement observé.
6. Effectuer **Compétences** : tester les quatre boutons avec LT, puis les quatre avec RT, ainsi que les boutons seuls et les relâchements. Confirmer après réussite.
7. Facultatif : effectuer **Stick droit**, à petite et grande amplitude. Confirmer uniquement si son vertical est inversé, le neutre conservé et tous les autres axes inchangés.

Chaque essai dure au maximum trois minutes et s’arrête lorsqu’on quitte Guild Wars. La pastille GW permet aussi de l’arrêter. Un compteur d’événements transmis n’est pas une preuve que le jeu a effectué les actions.

Le seuil LT/RT est initialement à 50 % de leur course. Si la bascule arrive plus tôt ou plus tard que dans le jeu, ajuster ce seuil dans le diagnostic puis refaire les essais. Un relâchement utilise une petite marge de 5 points pour éviter les oscillations autour du seuil. Les événements numériques L2/R2 éventuels ne prennent pas le dessus sur les axes calibrés.

Un échec de **Transmission intacte** bloque les inversions qui en dépendent. Si seul **Stick droit** échoue, laisser cette capacité non validée : les compétences déjà validées restent utilisables. Le compte rendu local permet de conserver le constat par un partage manuel.

## Pendant le jeu

- Toucher la pastille **GW** pour ouvrir les deux interrupteurs. Elle se déplace en la faisant glisser et conserve sa position.
- **Compétences** échange A avec X, B avec Y, uniquement pendant LT ou RT.
- **Vertical stick droit** inverse haut/bas du stick droit, même sans gâchette.
- Les deux préférences sont activées par défaut puis mémorisées indépendamment. Les fonctions non validées restent indisponibles dans la pastille.
- Si M2 a été identifié comme une touche indépendante, il bascule les compétences.
- Relâcher les commandes pour appliquer un changement en attente. Pour arrêter complètement l’interception, laisser aussi les sticks revenir au repos.

L’overlay reste affiché dans Guild Wars avec les deux interrupteurs éteints. En quittant le jeu, au verrouillage ou pendant la saisie au clavier, les transformations sont suspendues. L’overlay ne prend pas le focus clavier et ne couvre que sa petite zone.

## Arrêter et dépanner

Pour suspendre les inversions, éteindre les deux interrupteurs et relâcher les commandes. Pour arrêter entièrement Kishi Switch, toucher « Arrêter entièrement le service » dans l’app, ou désactiver son service d’accessibilité dans Android. Le retour dans Kishi Switch termine aussi l’essai temporaire en cours.

Si Shizuku est arrêté, rouvrir Shizuku, le démarrer, puis utiliser « Autoriser / reconnecter Shizuku ». En cas de coupure pendant un appui, relâcher les commandes ; si le jeu conserve une commande, revenir à l’accueil puis au jeu, ou reconnecter la Kishi.

Si OxygenOS interrompt régulièrement Shizuku ou Kishi Switch, vérifier leurs réglages de fonctionnement en arrière-plan. N’ajuster d’autres options développeur qu’en fonction d’un échec constaté et du guide Shizuku. Android et Shizuku peuvent conserver leurs indications système ; Kishi Switch n’émet pas de notifications récurrentes.

Si Guild Wars n’est pas trouvé, confirmer son paquet sur le téléphone : cette version cible uniquement `net.arena.guildwars.reforged`. Une mise à jour du système, du jeu, du seuil ou du profil nécessite de refaire les essais.

Pour installer une mise à jour, ouvrir le nouvel APK signé avec la même clé, sans désinstaller l’ancienne version, afin de conserver les réglages.
