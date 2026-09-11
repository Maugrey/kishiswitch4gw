# Compte rendu — version 0.1.2

## Diagnostic confirmé le 11 septembre 2026

La 0.1.1 capturait les mouvements puis les réinjectait avec l’identifiant Android virtuel -1. L’essai sur le OnePlus CPH2449 confirme ce changement d’identité, alors que Guild Wars reste focalisé et que les files d’entrée ne sont pas bloquées. Les commandes ne produisent pas d’action visible dans le jeu.

Un prototype distinct a ensuite lu le périphérique physique de la Kishi, obtenu EVIOCGRAB sans root et retransmis les commandes par UHID. Android a enregistré cette manette avec un identifiant propre et lui a attribué aussi bien les mouvements que les boutons. **L’utilisateur a répondu « Tout fonctionne » après l’essai de 45 secondes dans Guild Wars.** Le prototype a compté 4 126 événements bruts et 1 613 rapports HID, puis libéré la Kishi automatiquement.

Après installation de l’APK 0.1.2 et demande de vérifier successivement Transmission intacte, Compétences et Stick droit, l’utilisateur a confirmé : **« C’est bon, tout fonctionne ! »** La transmission et les deux inversions sont donc confirmées par l’utilisateur dans Guild Wars sur son OnePlus CPH2449, avec la Kishi V2 Pro et Shizuku sans root. Cette confirmation concerne désormais l’application intégrée.

## Contrôles de la nouvelle version

- Compilation personnelle signée 0.1.2 : réussie ; même clé que la version précédente.
- Tests Kotlin : 17 réussis, dont cinq nouveaux tests du codage HID (huit combinaisons, boutons seuls, collisions de correspondances, ordre analogique/boutons simultanés, neutre/amplitude, autres axes, changements différés et répétitions).
- Android Lint : aucune erreur.
- Installation par-dessus 0.1.1 sur le OnePlus : réussie.
- Essais Android instrumentés historiques : huit réussis en 0.1.1 sur émulateur ; ils n’ont pas été exécutés sur le téléphone personnel car ils effacent les préférences de test.
- Essais de l’APK 0.1.2 dans Guild Wars : transmission intacte, inversion des compétences et inversion verticale du stick droit confirmées par l’utilisateur.

## Écart d’architecture nécessaire

L’injection ciblée par UID avec InputManager n’est plus utilisée pour le jeu. Le UserService Shizuku lit le seul périphérique physique identifié, suspend sa transmission native par EVIOCGRAB et alimente une manette UHID. Les autres périphériques, notamment le clavier, ne sont pas capturés.

UHID passe par la distribution normale d’Android : il ne propose pas de cible UID par événement. Le contrôle de l’appelant Binder et du paquet autorisé reste en place ; l’accessibilité arrête le relais sur changement de fenêtre, apparition du clavier et verrouillage. Une liaison de durée limitée et la mort du client arrêtent également le relais. Le contrôle de connexion toutes les 250 ms ne surveille pas le premier plan par interrogation périodique.

Les axes gardent le format natif sur 8 bits, ses amplitudes et sa précision. RZ est inversé par 255 moins sa valeur brute ; les deux valeurs centrales 127 et 128 restent dans la zone neutre Android. Un format expérimental sur 16 bits a été écarté après avoir constaté qu’Android lui ajoutait un filtrage de précision différent. Les changements d’un même rapport sont regroupés avant de décider la correspondance des boutons.

M2 reste indisponible dans cette version ; utiliser les deux interrupteurs de l’overlay. Aucun échange global XYAB ni modification du client Guild Wars n’a été introduit.

## Vérifications complémentaires non détaillées dans ce retour

- Déplacement et caméra simultanés, appuis/relâchements dans différents ordres et petites amplitudes du stick droit.
- Les quatre combinaisons d’interrupteurs, position de la pastille et changements différés.
- Clavier habituel dans le jeu et une autre application.
- Changement d’application, verrouillage, débranchement et arrêt de Shizuku.
- Redémarrage du téléphone puis relance de Shizuku.
- Session d’au moins vingt minutes sous OxygenOS.

Le retour global de fonctionnement ne fournit pas de résultats séparés pour ces scénarios ; ils ne sont pas présentés comme vérifiés individuellement.

Les journaux Android bruts et les APK du jeu utilisés en lecture seule pour le diagnostic restent locaux, exclus du dépôt et des archives distribuées.
