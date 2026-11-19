# Patchs Morphe pour Deezer Android

Le projet cible **Deezer Android 9.0.26.3** (code `9002603`, Android 8.0 minimum). Morphe applique les patchs à l’archive APKM d’origine de Deezer (`deezer.android.app`). L’APK de sortie porte l’identifiant `deezer.android.app.morphe`. Les deux versions s’installent côte à côte et Android stocke leurs données séparément.

## Ajouter la source dans Morphe Manager

1. Ouvrir [Ajouter la source dans Morphe](https://morphe.software/add-source?github=AudioPatchLab/deezer-morphe-patches). On peut aussi ouvrir **Sources → + → Distant** dans Morphe Manager et coller `https://github.com/AudioPatchLab/deezer-morphe-patches`.
2. Confirmer l’ajout de la source, puis sélectionner Deezer et fournir l’archive APKM **9.0.26.3**.
3. Choisir les patchs à appliquer, puis lancer le patchage dans Morphe.

Morphe lit [`patches-bundle.json`](https://raw.githubusercontent.com/AudioPatchLab/deezer-morphe-patches/main/patches-bundle.json) pour télécharger le bundle et détecter les mises à jour. Le fichier `.mpp` est également disponible dans les [releases GitHub](https://github.com/AudioPatchLab/deezer-morphe-patches/releases/latest) pour un import local. [Documentation de Morphe Manager](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md)

Le fichier [`patches-list.json`](patches-list.json) décrit les huit patchs pour l’[annuaire communautaire](https://morphe-patches.software/?app=deezer.android.app). Il suit le format du [générateur officiel](https://github.com/MorpheApp/morphe-patches-template/blob/main/patches/src/main/kotlin/util/PatchListGenerator.kt). Sa présence prépare le référencement ; l’ajout à l’annuaire dépend de la validation de son équipe.

| Chemin | Contenu |
| --- | --- |
| [`morphe/src/`](morphe/src/) | Sources Kotlin des huit réglages et des dépendances internes |
| [`morphe/VERSION`](morphe/VERSION) | Version du bundle utilisée par les scripts de construction et de test |
| [`morphe/DIAGNOSTIC.md`](morphe/DIAGNOSTIC.md) | Observations techniques, limites et essais précédents |
| `morphe/build/` | Bundle MPP généré localement |
| `.local/` | APK, outils, clé et résultats de test privés |

## Fonctionnalités et validation

Morphe affiche huit réglages. Chaque réglage active les composants internes nécessaires à la fonctionnalité.

| Entrée Morphe | Fonction et parcours à vérifier |
| --- | --- |
| **Choose tracks on demand** | Sélection d’un titre dans les albums, les playlists, les pages d’artistes, les favoris de la bibliothèque, les « Top titres » et les recommandations des profils, ainsi que les playlists Deezer générées. |
| **Enable player navigation** | Boutons précédent et suivant, sauts de titres et déplacement sur la barre de progression. Vérifier dans un album, une playlist et un mix. |
| **Manage the playback queue** | Accès à la file et actions « Ajouter à la file d’attente » et « Écouter juste après ». Vérifier dans le menu à trois points d’un titre et contrôler sa position dans la file. |
| **Search and sort playlists** | Loupe dans l’en-tête et action « Trier » dans le menu de playlist. Vérifier la recherche et le changement d’ordre sur une playlist personnelle. |
| **Block ads** | Bandeaux et placements natifs, interstitiels avant la lecture et spots audio. Vérifier la reprise du titre après chaque type d’interruption. |
| **Hide Premium offers** | Fenêtres d’offre, onglet et boutons Premium, offres du menu contextuel et qualités audio verrouillées. Vérifier les trois onglets de qualité et le menu d’un titre. |
| **Open lyrics from the player** | Ouverture des paroles depuis le lecteur. Vérifier avec un titre qui contient des paroles et un titre qui n’en contient pas. |
| **Enable offline downloads** | Téléchargement local, selon les flux fournis par Deezer. Vérifier en redémarrant en mode avion. Le test précédent a mesuré du MP3 à 128 kbit/s sur le compte gratuit. |

Le maintien de la session est une dépendance interne de chaque réglage, y compris lorsque seul le téléchargement ou seules les paroles sont activés. Morphe l’applique une fois par construction. Il désactive les journaux d’écoute `log.listen` et `log.batch_listen`, ce qui peut affecter l’historique d’écoute ou les recommandations. Le [diagnostic de déconnexion](morphe/DIAGNOSTIC.md#déconnexion-après-des-changements-de-titre) décrit les essais précédents.

Les « Top titres » de l’onglet Musique d’un profil et le bloc « Their music, meant for you » de l’onglet de compatibilité utilisent deux parcours de lecture. Les titres à la demande des recommandations et des favoris de la bibliothèque sont sélectionnables à partir du bundle 1.1.3. Voir [le diagnostic du profil](morphe/DIAGNOSTIC.md#affichage-et-sélection-du-morceau).

Le téléchargement et le masquage des offres restent indépendants. Le filtre d’offres du menu contextuel s’applique à la finalisation pour conserver la logique de téléchargement lorsque les deux réglages sont activés. Tester le bundle complet sur Android et les réglages seuls à la compilation sert à contrôler ces deux usages.

La version 1.1.0 a été construite, signée et ouverte sur l’émulateur avec les huit réglages. Douze sélections successives dans les « Top titres » de Daft Punk ont lancé le morceau choisi sans déconnexion. Les constructions séparées avec les paroles, le téléchargement et le masquage des offres ont également abouti.

Après réorganisation du dossier, le bundle 1.1.2 a été reconstruit depuis les sources. Avec les huit réglages, l’APK obtenu a les mêmes fichiers internes que l’APK 1.1.2 précédent, hors métadonnées de signature. Il a été installé puis lancé sur un émulateur où l’officiel était déjà présent. La construction de `Search and sort playlists` seul a également abouti.

Avec les huit réglages du bundle 1.1.3 sur Android 14, l’accueil s’est affiché environ sept secondes après la première connexion. « One More Time » depuis la bibliothèque et « Ma sœur » depuis un profil ont dépassé 30 secondes de lecture. Après douze sélections sur la playlist « Simon », son bouton a affiché l’icône Pause pendant la lecture puis l’icône Aléatoire après la mise en pause.

## Reconstruction sous Linux

Les scripts utilisent un JDK 21, le compilateur Kotlin fourni par Gradle 9.8.2, le SDK Android avec la plateforme 34, les Build Tools 36.1.0 et Platform Tools. Placer le JAR de [Morphe Desktop 2.2.0](https://github.com/MorpheApp/morphe-desktop/releases/tag/v2.2.0) dans `.local/tools/morphe-desktop.jar`. Il fournit Morphe Patcher 2.4.0. Placer l’entrée officielle `deezer.android.app_9.0.26.3.apkm` dans `.local/apks/stock/`, ou définir `DEEZER_ARCHIVE` avec son chemin. Les commandes suivantes s’exécutent depuis la racine.

```bash
export JAVA_HOME=/chemin/vers/jdk-21
export ANDROID_HOME=/chemin/vers/Android/Sdk
./morphe/build-bundle.sh .local/tools/morphe-desktop.jar /chemin/vers/gradle-9.8.2
```

Si `.local/test/test-keystore.p12` est absent, créer une clé de test une seule fois. Conserver la même clé pour installer les versions suivantes sur l’émulateur.

```bash
mkdir -p .local/test
keytool -genkeypair -keystore .local/test/test-keystore.p12 -storetype PKCS12 \
  -alias Morphe -storepass Morphe -keyalg RSA -keysize 2048 \
  -validity 3650 -dname 'CN=Morphe Local Test'
chmod 600 .local/test/test-keystore.p12
```

`test-bundle.sh` fusionne les APK split, applique le bundle, conserve les bibliothèques `x86_64` et vérifie la signature. Il produit `deezer.apk`, `patch.log` et `result.json` dans un nouveau dossier `.local/test/run.*`. Sans `--patch`, il applique les huit réglages. L’installation est facultative et exige le numéro de série explicite de l’émulateur.

```bash
adb devices -l
./morphe/test-bundle.sh --patch 'Choose tracks on demand'
./morphe/test-bundle.sh --install emulator-5558 --open https://www.deezer.com/album/302127
```

L’AVD de test sur cette machine est `Deezer_Patch_API_34` sous Android 14, généralement sur `emulator-5558`. `Deezer_API_34` sert de témoin pour l’APK d’origine. Pour un appareil ARM, importer le bundle dans Morphe Manager et charger l’archive APKM. Pour une construction ARM en ligne de commande, utiliser Morphe Desktop avec l’ABI cible. Le script de test du projet produit des APK x86_64. Installer des APK signés avec la même clé pour garder les données de test entre deux builds. [Morphe Patches](https://github.com/MorpheApp/morphe-patches) · [Import local dans Morphe Manager](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md)

Sur cette machine, l’option `-gpu host` a activé le rendu sur la Radeon RX 6600. Le démarrage en ligne de commande peut utiliser `$ANDROID_HOME/emulator/emulator -avd Deezer_Patch_API_34 -port 5558 -gpu host -no-snapshot`. Vérifier le moteur actif avec `adb -s emulator-5558 shell dumpsys SurfaceFlinger` en cherchant la ligne `GLES:`. [Accélération graphique Android](https://developer.android.com/studio/run/emulator-acceleration)

Pour écouter Deezer depuis l’émulateur, lancer la copie locale de scrcpy avec la capture directe de la lecture Android. Sur cette machine, le mode audio par défaut envoyait un signal très faible. La commande ci-dessous a rétabli un niveau normal sur la sortie audio du PC.

```bash
.local/tools/scrcpy-linux-x86_64-v5.0.1/scrcpy -s emulator-5558 \
  --audio-source=playback --audio-codec=raw --require-audio
```

## Rejouer la déconnexion

1. Ouvrir un album ou une playlist en restant connecté au compte de test. Lancer un morceau et rabattre le lecteur si nécessaire.
2. Toucher des titres différents toutes les deux ou trois secondes. Comparer le titre sélectionné aux métadonnées de la session média Android. Répéter au moins dix fois et vérifier qu’une sélection supplémentaire fonctionne après une pause.
3. Le script ci-dessous automatise ces vérifications sur des lignes déjà visibles. Adapter les coordonnées à l’écran Android avec `adb shell wm size`. Les coordonnées de cet exemple correspondent à l’album *Discovery* sur l’émulateur 1080 × 2400, selon le défilement.

```bash
python3 morphe/verify-playback.py emulator-5558 --rounds 4 \
  '390,1470,One More Time' '390,1635,Aerodynamic' '390,1795,Digital Love'
```

## Reprise du projet

Partager le README et le dossier `morphe/` sans la sortie de construction. La personne qui reprend le projet fournit son APK d’origine et son compte de test. Le dossier `.local/` est ignoré par Git. Sur cette machine, il contient aussi la référence APK d’origine tierce dans `.local/apks/reference/` et les anciens rapports dans `.local/archive/test-run-reports/`. Les journaux avec des données de session restent privés.

### Publier une mise à jour des patchs

1. Incrémenter `morphe/VERSION`, reconstruire et tester le bundle avec les scripts ci-dessus, puis pousser les sources.
2. Créer une release GitHub stable avec le tag `v<version>` et joindre `morphe/build/deezer-local-<version>.mpp`.
3. Mettre à jour `patches-bundle.json` sur `main` avec la version sans préfixe, le lien direct vers le fichier de la release, la date et l’heure UTC au format `AAAA-MM-JJTHH:MM:SS`, et la description. Ce fichier doit pointer vers un téléchargement déjà accessible.
4. Vérifier le succès de l’action **Sync community patches list**. Elle télécharge le bundle publié, en extrait les métadonnées et met à jour `patches-list.json` sur `main`. Elle refuse une version différente de celle de `patches-bundle.json`. On peut aussi la relancer depuis **Actions → Sync community patches list → Run workflow**.

La liste est générée depuis le `.mpp` publié, et non depuis des sources qui pourraient déjà préparer une version suivante. Les noms, descriptions, options et compatibilités proviennent des objets chargés par Morphe Patcher. Ne pas modifier `patches-list.json` à la main. Pour reproduire la génération localement avec JDK 21, télécharger [Gson 2.13.2](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar) dans `.local/tools/gson-2.13.2.jar`, puis lancer :

```bash
"$JAVA_HOME/bin/java" --class-path '.local/tools/morphe-desktop.jar:.local/tools/gson-2.13.2.jar' \
  morphe/GeneratePatchesList.java morphe/build/deezer-local-1.1.4.mpp \
  patches-bundle.json patches-list.json
```

Adapter le nom du `.mpp` à la version publiée. Les versions des outils et leurs empreintes SHA-256 sont fixées dans le workflow.

## Suite

Achever les essais des offres en cours de session, des autres formats publicitaires et du lecteur de paroles sur un titre sans paroles. Pour chaque format publicitaire supplémentaire observé, cibler son point d’entrée et vérifier qu’il disparaît avec le bundle complet. Avant de cibler une autre version de Deezer, retrouver les méthodes par leurs références et leurs chaînes constantes, puis vérifier la nouvelle version sur l’émulateur.
