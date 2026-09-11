# Compte rendu — livraison 0.1.1

**Statut : correction du filtrage et du retour aux commandes natives ; compatibilité avec Guild Wars encore à valider.** Aucun téléphone physique n’est connecté à l’environnement de développement.

## Retours du téléphone

L’utilisateur a identifié le vertical du stick droit comme `AXIS_RZ` et LT comme `AXIS_LTRIGGER`. Pendant « Transmission intacte », toutes les commandes du jeu, boutons compris, se bloquent. Il a ensuite précisé que le blocage dépend de l’essai, et non du simple lancement de Kishi Switch : les commandes reviennent automatiquement à l’expiration des trois minutes. Ce constat remplace l’hypothèse initiale d’un blocage persistant hors essai. La retransmission dans le jeu est donc **en échec**, tandis que l’arrêt temporisé rétablit les commandes natives. La version concernée par cette dernière observation (0.1.0 ou 0.1.1) reste à confirmer. Aucun compte rendu technique de cet essai n’a encore été reçu.

## Correction 0.1.1

- Suppression de la demande permanente de filtrage des boutons dans la configuration du service. Elle n’est activée que pour les compétences ou un M2 validé, dans le jeu. La transmission intacte et l’essai du stick droit ne demandent pas ce filtrage.
- Réinitialisation explicite des abonnements aux mouvements et aux boutons lors de la connexion du service et de chaque arrêt, même sans moteur actif en mémoire.
- Priorité à la fenêtre réellement focalisée ; suspension explicite dans l’interface de Kishi Switch et arrêt de l’essai en y revenant.
- Bouton « Arrêter entièrement le service » sur les deux écrans ; il désactive l’accessibilité de l’app.
- Compte rendu enrichi : mouvements interceptés, injections acceptées par Android, état du filtrage et détail des refus d’injection. Acceptation par Android ne signifie pas action reçue dans Guild Wars.
- Profil matériel et préférences conservés lors de la mise à jour ; anciennes validations invalidées pour refaire les essais avec le nouveau protocole.

La demande permanente de filtrage et le nettoyage incomplet ont été constatés dans le code. La précision apportée par l’utilisateur ne démontre pas qu’ils causent son blocage : l’investigation porte désormais sur la capture et la retransmission pendant l’essai. Les compteurs de la version 0.1.1 doivent permettre de distinguer une absence de mouvements capturés, un refus d’injection par Android et une injection acceptée sans action visible dans le jeu. La cause exacte du blocage de tous les boutons reste à établir ; aucune incompatibilité définitive de Guild Wars n’est conclue.

## Contrôles effectués

| Contrôle | Résultat |
|---|---|
| Compilation Android 36 avec JDK 21 | Réussie en debug et release |
| Moteur Kotlin | 12 tests réussis : huit combinaisons, boutons seuls, maintien/répétitions, appuis simultanés, ordres de relâchement, seuil/hystérésis, indépendance des réglages, neutre/amplitude et réinitialisation |
| Tests Android sur AVD API 36.1 / Android 16 | 8 tests réussis : les 5 tests existants (préférences, profil, validations, copie analogique, permissions/clavier) et 3 régressions sur les abonnements d’interception : nettoyage après redémarrage, boutons natifs pendant transmission intacte, activation explicite du filtrage |
| Analyse Android Lint | Aucune erreur ; avertissements notamment sur l’API interne d’injection, les textes exclusivement français et les versions des dépendances de test |
| Liaison Shizuku v13.6.0 sur l’émulateur | Recontrôlée avec l’APK signé 0.1.1 : autorisation par le dialogue Shizuku, connexion au UserService avec identité shell réussie |
| Injection de boutons dans le récepteur de Kishi Switch | Recontrôlée en 0.1.1 : appui et relâchement reçus par l’activité au premier plan |
| Injection analogique dans le récepteur de Kishi Switch | Recontrôlée en 0.1.1 : deux mouvements reçus via Shizuku. Cela ne valide pas la retransmission depuis une Kishi ni la réception dans le jeu |
| Service d’accessibilité 0.1.1 sur l’émulateur | Service lié et activé ; `dumpsys input` indique `InputFilterEnabled: false` hors essai. Après appui sur « Arrêter entièrement le service », aucun service lié ou activé dans `dumpsys accessibility` |
| Interface | Affichage et navigation inspectés ; deux préférences activées initialement et capacités non validées |
| Signature de l’APK release | Signature APK v2 vérifiée par `apksigner`, certificat RSA 3072 bits, clé privée conservée hors du dépôt |
| Installation de l’APK signé | Mise à jour 0.1.0 → 0.1.1 réussie sur l’émulateur ; choix « Compétences » éteint et vertical allumé conservés. Certificat identique à 0.1.0. Le profil de la Kishi physique reste à vérifier sur le téléphone |

Les rapports Gradle sont générés sous `engine/build/reports/tests/test/` et `app/build/reports/lint-results-release.html`. Le résultat des 8 tests Android 0.1.1 lancés directement avec ADB est dans `artifacts/v011-android-tests.txt`. Les traces système et captures d’émulateur sont également dans `artifacts/` ; elles sont exclues de l’archive des sources.

Empreinte SHA-256 du certificat de signature : `2deb54d9f7e6249f2c5a1306b90cf078523b6f69fc7b94b3d4d85ff1e2eed071`. Ce certificat est propre à cette installation personnelle ; recréer une autre clé sur un autre ordinateur produira une empreinte différente.

## Essais requis sur le téléphone

À cocher seulement après observation réelle. Utiliser le débogage sans fil pour laisser la Kishi branchée.

- [ ] Avec 0.1.1, service actif mais aucun essai commencé : tous les boutons et sticks fonctionnent nativement dans Guild Wars.
- [ ] Après un essai interrompu, le retour dans Kishi Switch puis dans le jeu rétablit les commandes natives ; le bouton d’arrêt complet désactive effectivement le service.

- [ ] Paquet installé : `net.arena.guildwars.reforged` ; relever version du jeu et version OxygenOS.
- [ ] Kishi reconnue et retrouvée après débranchement/rebranchement.
- [ ] LT et RT analogiques identifiés ; seuil du jeu déterminé et absence de doublons numériques vérifiée.
- [ ] Axe vertical réel du stick droit identifié par son mouvement, sans supposer son nom.
- [ ] Transmission analogique intacte acceptée par le jeu, y compris mouvement gauche + caméra + gâchettes, diagonales et retours au neutre.
- [ ] Les huit combinaisons ci-dessous produisent la compétence voulue, sans action native supplémentaire.

| Commande physique | Commande transmise au jeu |
|---|---|
| LT + A | LT + X |
| LT + X | LT + A |
| LT + B | LT + Y |
| LT + Y | LT + B |
| RT + A | RT + X |
| RT + X | RT + A |
| RT + B | RT + Y |
| RT + Y | RT + B |

- [ ] A/B/X/Y sans gâchette gardent leurs actions habituelles.
- [ ] Répéter rapidement, maintenir et combiner plusieurs boutons ; relâcher la gâchette avant/après les boutons sans touche bloquée.
- [ ] Stick droit vertical : faible et forte amplitude, neutre, horizontal et stick gauche préservés.
- [ ] Essayer les quatre états des deux interrupteurs ; vérifier le changement différé au repos et la mémorisation.
- [ ] Pastille déplaçable, panneau utilisable, touches extérieures accessibles au jeu, pastille visible avec les inversions éteintes.
- [ ] M2 distinct utilisable, ou overlay retenu si M2 est un doublon/non identifiable.
- [ ] Saisie avec le clavier habituel dans Guild Wars et une autre application ; aucun événement synthétique envoyé dans cette dernière.
- [ ] Changement d’application, verrouillage et débranchement : suspension et reprise correctes.
- [ ] Arrêt de Shizuku pendant un mouvement/appui : retour aux commandes natives ; reconnexion manuelle opérationnelle.
- [ ] Redémarrage du téléphone puis relance de Shizuku : reprise avec les choix mémorisés.
- [ ] Session d’au moins vingt minutes sous OxygenOS : pas de perte de commande, interruption ni ralentissement perceptible.

## Décision après les essais

- Si la transmission de base échoue, ne pas certifier les inversions. Conserver le compte rendu et diagnostiquer l’acceptation du périphérique virtuel par Guild Wars. Il n’existe pas de remplacement automatique par un échange global XYAB.
- Si les compétences réussissent et que le vertical échoue, confirmer les compétences et laisser le vertical non validé. Cette option reste indisponible ; son échec ne désactive pas les compétences.
- Si les deux réussissent, confirmer les deux : elles deviennent actives par défaut, puis respectent les choix mémorisés.

Une compilation, les tests du moteur et la réception locale dans notre activité ne certifient pas la réception dans Guild Wars. Les latences, l’ordre effectif des événements sur la Kishi et le comportement d’OxygenOS doivent être constatés sur cet appareil.
