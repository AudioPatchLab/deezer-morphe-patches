# Diagnostic technique

## Déconnexion après des changements de titre

L’APK de test du 17 novembre 2026 avant correction appliquait déjà **Spoof playlog signature**. En lançant successivement des titres de *Discovery*, la trace a enregistré plusieurs réponses HTTP 200 à `log.listen`, puis un `EXPIRED_SESSION` dans cette même requête. L’application a ensuite reçu son événement d’identifiants invalides et appelé sa procédure de déconnexion. Le HTTP 200 seul ne suffit donc pas pour valider un lot de lectures.

Le correctif **Keep session during playback** touche uniquement la classe qui construit les requêtes du journal d’écoute (`LIIIlIllllIIllII;`). Les méthodes qui portent les constantes `log.listen` et `log.batch_listen` renvoient désormais la valeur nulle que le répartiteur gère déjà. Cela couvre un changement de piste immédiat et le renvoi d’un lot mis en cache par une version antérieure. Les chemins de connexion, de renouvellement de session et de déconnexion conservent leur code d’origine. Le correctif remplace le patch de signature précédent.

Pour reprendre l’analyse, voir [le patch de session](src/PlaybackSessionPatch.kt) et [le script de vérification](verify-playback.py). Sur la machine de travail, `.local/test/logout-baseline.log` contient la séquence avant correction, `.local/test/logout-fixed.log` la trace du nouvel essai et `tools/trace-playback-session.js` fournit le script Frida qui n’enregistre que le nom des méthodes, les types d’erreurs et la pile de déconnexion. La reproduction utilise Android 14 avec l’APK original de départ et le compte de test existant.

## Publicité audio avant un titre

L’essai avant correction a ouvert `AudioAdsActivity` dans une playlist malgré le patch de l’interstitiel de prélecture. Le contrôleur audio `LIIIIlllIllllIlI;` récupère des annonces via `mobile_getAudioBreak` ou `mobile_getAudioBreakMultiple`. Selon la réponse, il peut insérer un élément dans la file. `src/AudioAdsPatch.kt` fait reprendre la lecture par sa continuation avant la planification du spot. Le chemin traite aussi l’appel sans continuation utilisé pour précharger des annonces.

## Affichage et sélection du morceau

Le modèle de ligne d’origine marque grisés les titres ne disposant pas du droit de lecture à la demande (`NO_MOD`). Les patchs convertissent cet état pour l’affichage et conservent les états indisponibles. L’album utilise son numéro de piste natif dans le texte du titre pour que l’emplacement de la pochette reste disponible. Le classement d’un artiste utilise la position dans la liste, y compris dans « Voir tout ». `src/NumberedTrackRows.kt` partage le formatage « numéro. titre ». Les gestionnaires de toucher transmettent l’identifiant de la piste à l’action de lecture.

Sur une page d’artiste, les trois titres visibles et la liste « Voir tout » passent par deux appels distincts au même convertisseur de ligne. Le correctif applique la même conversion d’affichage à ces deux appels. La copie de présentation conserve les autres états des titres et laisse le modèle utilisé par le lecteur intact.

Une recommandation Deezer comme *French Pop* s’ouvre dans `SmartTrackListActivity`. La liste développée combine une autorisation de compte avec les vérifications du morceau avant de choisir l’état d’affichage. `src/SmartTrackListSelectionPatch.kt` change la décision liée au compte dans ce constructeur. Il utilise aussi le chemin de lecture directe qui transmet l’identifiant du morceau choisi. Une action provenant du bouton aléatoire continue à suivre son parcours natif. `src/TrackCellDisplayRights.kt` partage la conversion des lignes qui utilisent le même format de données, notamment sur le profil.

Dans « Their music, meant for you », la trace sur le profil de test confirme que « Comme Caroline », « Ma sœur » et « Mauvais Garçon » ont l’état `NO_MOD` dans les lignes grisées. Le composant crée alors des lignes désactivées pour le toucher et l’accessibilité. Le parcours de profil produit aussi des lignes intermédiaires `READABLE` pour les mêmes titres. C’est l’état des lignes finales qui explique le grisé visible. L’observation porte sur la décision locale de Deezer et ne mesure pas la disponibilité du flux côté serveur.

`src/CollectionButtonsPatch.kt` règle le démarrage via le bouton d’album ou de playlist. Le modèle de la playlist fournit le même mode au rendu du bouton et à son action. `src/UnlimitedSkipsPatch.kt` modifie le masque de navigation et les cibles qui suivent les vérifications du début et de la fin de file. ART peut rejeter une méthode si une instruction ajoutée écrase un registre de paramètres dont le chemin original se sert. Après compilation, relire les registres autour des branches et ouvrir le parcours dans l’émulateur.

## Offre Premium dans le menu du titre

Le menu à trois points utilise la clé `CONTEXTUAL_MENU_ADS_FREE` pour afficher « Listen without ads ». `src/PremiumTabPatch.kt` filtre cette clé pendant la lecture du registre local des offres. Le filtre porte sur la clé de campagne et couvre les différentes langues ainsi que le menu du lecteur et celui de la liste. Les autres actions continuent à suivre leur chemin d’origine.

## Insertion dans la file depuis le menu d’un morceau

Le menu distingue `ADD_TRACK_TO_QUEUE` et `ADD_TRACK_AFTER_CURRENT`. Les deux composants du réglage **Manage the playback queue** rétablissent chacun leur action dans le menu principal et l’ajoutent aux actions du lecteur plein écran. Les deux actions partagent le traitement de la file et le filtre des offres `try_to_edit_queue_list` et `TRY_TO_EDIT_QUEUE_LIST`. La première vérification peut ouvrir un écran par elle-même, donc le patch saute son appel. Les insertions restent gérées par Deezer et les titres indisponibles sont contrôlés par le chemin natif.

Sur Android avec les quatorze patchs, l’ajout de *Instant Crush* depuis la liste de Daft Punk a fait passer la file de 14 à 15 titres. La demande « Écouter juste après » sur *Around the World* a porté la file à 16 titres. L’ordre affiché dans la file était *One More Time*, *Around the World*, *Instant Crush*, puis les titres suivants de *Discovery*. Les deux insertions depuis le lecteur plein écran ont aussi porté une file de 14 à 16 titres, sans fenêtre Premium.

## APK modifié fourni comme référence

L’archive `.local/apks/reference/Deezer_9.0.25.4_9012504_bee119.apk` annonce le paquet `com.deezer.pro` et la version **9.0.25.4**. Son manifeste pointe vers `com.sagittarius.v6.StubApplication` et son archive contient `libbaiduprotect.so` ainsi que `assets/origin359.apk`. Ce dernier déclare le paquet `deezer.android.app` en version **9.0.11.4** (`9001104`). Le contrôle d’intégrité de la signature de l’APK imbriqué échoue. L’enveloppe ne contient que des bibliothèques ARM, tandis que l’émulateur de travail ne fournit que `x86_64` et aucun pont ARM.

La comparaison avec l’original **9.0.11.4** confirme l’identité octet pour octet des douze DEX imbriqués et des six bibliothèques du split ARM64. Le certificat de l’APK de comparaison a été vérifié et correspond au certificat du 9.0.26.3 fourni. Sur 4 105 fichiers communs au split de base et à l’APK imbriqué, seuls le manifeste et la table de ressources diffèrent. Les contrôles `music_on_demand` et `disable_lyrics_feature` visibles dans cet APK font partie du code d’origine 9.0.11.4. Pour attribuer les changements fonctionnels au repack, il faut observer les DEX et bibliothèques effectivement chargés par l’enveloppe sur un appareil ARM. La copie de comparaison et les notes de l’analyse sont dans `.local/analysis/`. [Archive 9.0.11.4 utilisée pour la comparaison](https://apkpure.net/deezer-people-android-music/deezer.android.app/download/9.0.11.4)

`SORT_PLAYLIST` se trouve dans `classes6.dex` de l’APK imbriqué, identique au DEX officiel 9.0.11.4. Les DEX visibles à la racine de l’enveloppe ne contiennent ni ce libellé ni `sortPlaylistCallback`. Les seize archives `baiduprotect*.i.dex` contiennent des DEX de remplissage sans classes et les fichiers `baiduprotect*.jar` portent l’extension JAR mais leur contenu n’est pas lisible comme une archive ZIP. La signature de l’enveloppe diffère de celle des deux versions officielles comparées.

Pour « Their music, meant for you », la comparaison de l’APK imbriqué avec l’original 9.0.11.4 a retrouvé les mêmes valeurs par défaut pour les 117 ressources communes du profil et de la compatibilité musicale. Le libellé et son sous-titre sont présents dans les deux APK. Le comportement signalé reste à attribuer à l’exécution. L’ABI de l’enveloppe empêche sa vérification sur l’émulateur x86_64 utilisé pour les patchs.

## Tri des titres d’une playlist

Dans `src/PlaylistSortPatch.kt`, le composant de tri de **Search and sort playlists** rétablit l’entrée du menu `SORT_PLAYLIST`. Il cible uniquement le contrôle de lecture à la demande de cette entrée. L’application conserve sa liste de critères et son gestionnaire de tri, qui enregistre la préférence d’affichage dans `plst.sorter`.

## Fenêtre Premium signalée autour d’un Mix

Sur la version historique 1.0.3, l’inspection de la fenêtre visible a identifié `WebViewDialogActivity`, le chemin `/premiumtab` et le paramètre `origin=try_to_play_mod`. La version 1.0.4 ajoute ce cas à **Hide offer pop-ups** et utilise le chemin de fermeture natif avant l’initialisation de la WebView. Le rejeu instrumenté a confirmé la fermeture avant affichage et le filtre laisse passer les autres motifs testés. Avec les quinze patchs activés, le mix d’*Aerodynamic* a atteint l’état `PLAYING`. Le mix de *One More Time* est resté en chargement lors d’un autre essai.

## Barre de progression du lecteur

Dans la version historique 1.0.4, le lecteur pouvait déplacer temporairement le curseur alors que le masque des actions musicales ne contenait pas le bit `0x100` utilisé par Deezer pour la recherche dans le morceau. La session média conservait sa position et l’interface revenait au temps de lecture réel. **Enable seeking in music tracks** remet ce bit dans les trois contextes musicaux du contrôleur. Le moteur de lecture applique ensuite la position par le chemin natif.

## Outils pour itérer rapidement

- **APK Analyzer** et **apksigner** déterminent l’identité, la version et la signature de chaque couche. Commencer par les comparer avant de décompiler un gros APK ou d’attribuer les différences à un patch. [Android APK Analyzer](https://developer.android.com/tools/apkanalyzer) · [Android apksigner](https://developer.android.com/tools/apksigner)
- **JADX** permet de retrouver les utilisations d’une méthode et les chaînes stables malgré les noms obfusqués. Pour une coroutine mal reconstruite, lire directement les instructions avec `tools/DexInspect.java`, qui utilise dexlib2 fourni par Morphe Desktop. [JADX](https://github.com/skylot/jadx)
- **ADB** vérifie les titres en cours via `dumpsys media_session`, **scrcpy** rend l’émulateur pilotable et **Frida** permet de confirmer quel chemin est exécuté et où survient l’erreur. Ce trio suffit pour la boucle de lecture actuelle. [ADB](https://developer.android.com/tools/adb) · [scrcpy](https://github.com/Genymobile/scrcpy) · [Frida pour Android](https://frida.re/docs/android/)
- **Maestro** peut rejouer les parcours d’interface en YAML et repérer les éléments par leurs libellés. **Frida-DexDump** peut aider à retrouver les DEX chargés en mémoire lorsque l’archive de référence est lancée sur une architecture compatible. Ces deux outils ont été examinés pour la suite. [Maestro Android](https://docs.maestro.dev/get-started/supported-platform/android) · [Frida-DexDump](https://github.com/hluwa/frida-dexdump)
