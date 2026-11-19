# Morphe patches for Deezer Android

This project targets **Deezer Android 9.0.26.3** (version code `9002603`, Android 8.0 or later). Morphe applies the patches to the original Deezer APKM archive (`deezer.android.app`). The output APK uses the package name `deezer.android.app.morphe`. Both versions can be installed side by side, with separate app data.

## Add the source to Morphe Manager

1. Open [Add source to Morphe](https://morphe.software/add-source?github=AudioPatchLab/deezer-morphe-patches). You can also open **Sources → + → Remote** in Morphe Manager and paste `https://github.com/AudioPatchLab/deezer-morphe-patches`.
2. Confirm the source, select Deezer, and provide the **9.0.26.3** APKM archive.
3. Select the patches to apply, then start patching in Morphe.

Morphe reads [`patches-bundle.json`](https://raw.githubusercontent.com/AudioPatchLab/deezer-morphe-patches/main/patches-bundle.json) to download the bundle and detect updates. The `.mpp` file is also available in the [GitHub releases](https://github.com/AudioPatchLab/deezer-morphe-patches/releases/latest) for local import. [Morphe Manager documentation](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md)

[`patches-list.json`](patches-list.json) describes the eight patches for the [community directory](https://morphe-patches.software/?app=deezer.android.app). It follows the format of the [official generator](https://github.com/MorpheApp/morphe-patches-template/blob/main/patches/src/main/kotlin/util/PatchListGenerator.kt). This file prepares the repository for indexing; inclusion in the directory depends on approval by its maintainers.

| Path | Contents |
| --- | --- |
| [`morphe/src/`](morphe/src/) | Kotlin sources for the eight options and their internal dependencies |
| [`morphe/VERSION`](morphe/VERSION) | Bundle version used by the build and test scripts |
| [`morphe/DIAGNOSTIC.md`](morphe/DIAGNOSTIC.md) | Technical observations, limitations, and previous tests (in French) |
| `morphe/build/` | Locally generated MPP bundle |
| `.local/` | Private APKs, tools, signing key, and test results |

## Features and validation

Morphe displays eight options. Each option enables the internal components required for that feature.

| Morphe entry | Feature and checks |
| --- | --- |
| **Choose tracks on demand** | Select tracks in albums, playlists, artist pages, library favorites, profile top tracks and recommendations, and generated Deezer playlists. |
| **Enable player navigation** | Previous and next controls, track skips, and seeking. Check playback in an album, a playlist, and a mix. |
| **Manage the playback queue** | Queue access and the “Add to queue” and “Play Next” actions. Check a track’s three-dot menu and its position in the queue. |
| **Search and sort playlists** | Search shortcut in playlist headers and “Sort” in playlist menus. Check searching and reordering in a personal playlist. |
| **Block ads** | Banner and native ads, pre-roll interstitials, and audio ads. Check that track playback resumes after each type of interruption. |
| **Hide Premium offers** | Offer pop-ups, the Premium tab and buttons, context-menu offers, and locked audio quality options. Check all three quality tabs and a track’s menu. |
| **Open lyrics from the player** | Open lyrics from the player. Check a track with lyrics and one without lyrics. |
| **Enable offline downloads** | Local downloads, depending on the streams provided by Deezer. Check playback after restarting in airplane mode. A previous test measured MP3 at 128 kbit/s on the free account. |

Session persistence is an internal dependency of every option, including when only downloads or lyrics are enabled. Morphe applies it once per build. It disables the `log.listen` and `log.batch_listen` listening logs, which may affect listening history or recommendations. The [disconnection diagnostic](morphe/DIAGNOSTIC.md#déconnexion-après-des-changements-de-titre) describes previous tests.

The top tracks in a profile’s Music tab and the “Their music, meant for you” section in the compatibility tab use separate playback paths. On-demand tracks in profile recommendations and library favorites have been selectable since bundle 1.1.3. See the [profile diagnostic](morphe/DIAGNOSTIC.md#affichage-et-sélection-du-morceau).

Downloads and offer hiding remain independent. The context-menu offer filter runs during finalization to preserve download logic when both options are enabled. Testing the full bundle on Android and building individual options checks both use cases.

Version 1.1.0 was built, signed, and launched on the emulator with all eight options. Twelve consecutive selections in Daft Punk’s top tracks played the selected track without signing out. Separate builds with lyrics, downloads, and offer hiding also succeeded.

After the directory was reorganized, bundle 1.1.2 was rebuilt from source. With all eight options, the resulting APK contained the same internal files as the previous 1.1.2 APK, except for signing metadata. It was installed and launched on an emulator where the official app was already installed. Building `Search and sort playlists` alone also succeeded.

With all eight options in bundle 1.1.3 on Android 14, the home screen appeared about seven seconds after the first login. “One More Time” from the library and “Ma sœur” from a profile both played for more than 30 seconds. After twelve selections in the “Simon” playlist, its button showed Pause during playback and Shuffle after pausing.

## Build on Linux

The scripts use JDK 21, the Kotlin compiler included with Gradle 9.8.2, and the Android SDK with platform 34, Build Tools 36.1.0, and Platform Tools. Place the [Morphe Desktop 2.2.0](https://github.com/MorpheApp/morphe-desktop/releases/tag/v2.2.0) JAR at `.local/tools/morphe-desktop.jar`. It includes Morphe Patcher 2.4.0. Place the original `deezer.android.app_9.0.26.3.apkm` archive in `.local/apks/stock/`, or set `DEEZER_ARCHIVE` to its path. Run the following commands from the repository root.

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
./morphe/build-bundle.sh .local/tools/morphe-desktop.jar /path/to/gradle-9.8.2
```

If `.local/test/test-keystore.p12` does not exist, create a test signing key once. Keep using the same key to install subsequent versions on the emulator.

```bash
mkdir -p .local/test
keytool -genkeypair -keystore .local/test/test-keystore.p12 -storetype PKCS12 \
  -alias Morphe -storepass Morphe -keyalg RSA -keysize 2048 \
  -validity 3650 -dname 'CN=Morphe Local Test'
chmod 600 .local/test/test-keystore.p12
```

`test-bundle.sh` merges the split APKs, applies the bundle, retains the `x86_64` libraries, and verifies the signature. It writes `deezer.apk`, `patch.log`, and `result.json` to a new `.local/test/run.*` directory. Without `--patch`, it applies all eight options. Installation is optional and requires the emulator’s explicit serial number.

```bash
adb devices -l
./morphe/test-bundle.sh --patch 'Choose tracks on demand'
./morphe/test-bundle.sh --install emulator-5558 --open https://www.deezer.com/album/302127
```

The test AVD on the development machine is `Deezer_Patch_API_34`, running Android 14, usually at `emulator-5558`. `Deezer_API_34` is the control device for the original APK. For an ARM device, import the bundle into Morphe Manager and load the APKM archive. For an ARM command-line build, use Morphe Desktop with the target ABI. This project’s test script produces x86_64 APKs. Install APKs signed with the same key to preserve test data between builds. [Morphe Patches](https://github.com/MorpheApp/morphe-patches) · [Local import in Morphe Manager](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md)

On the development machine, `-gpu host` enabled rendering on the Radeon RX 6600. To start the emulator from the command line, use `$ANDROID_HOME/emulator/emulator -avd Deezer_Patch_API_34 -port 5558 -gpu host -no-snapshot`. Check the active renderer with `adb -s emulator-5558 shell dumpsys SurfaceFlinger` and look for the `GLES:` line. [Android graphics acceleration](https://developer.android.com/studio/run/emulator-acceleration)

To listen to Deezer from the emulator, launch the local scrcpy copy with direct Android playback capture. On the development machine, the default audio mode produced a very quiet signal. The command below restored a normal level on the PC’s audio output.

```bash
.local/tools/scrcpy-linux-x86_64-v5.0.1/scrcpy -s emulator-5558 \
  --audio-source=playback --audio-codec=raw --require-audio
```

## Reproduce the disconnection

1. Open an album or playlist while signed in to the test account. Play a track and minimize the player if needed.
2. Tap different tracks every two or three seconds. Compare the selected track with the Android media session metadata. Repeat at least ten times and check that another selection still works after a pause.
3. The script below automates these checks for rows already visible on screen. Adjust the coordinates to the Android screen using `adb shell wm size`. The example coordinates correspond to the *Discovery* album on the 1080 × 2400 emulator, depending on the scroll position.

```bash
python3 morphe/verify-playback.py emulator-5558 --rounds 4 \
  '390,1470,One More Time' '390,1635,Aerodynamic' '390,1795,Digital Love'
```

## Project handover

Share the README and the `morphe/` directory without build output. Anyone taking over the project provides their own original APK and test account. Git ignores `.local/`. On the development machine, it also contains the third-party reference APK in `.local/apks/reference/` and older reports in `.local/archive/test-run-reports/`. Logs containing session data remain private.

### Publish a patch update

1. Increment `morphe/VERSION`, rebuild and test the bundle with the scripts above, then push the sources.
2. Create a stable GitHub release tagged `v<version>` and attach `morphe/build/deezer-local-<version>.mpp`.
3. Update `patches-bundle.json` on `main` with the version without a prefix, the direct release asset URL, the UTC date and time in `YYYY-MM-DDTHH:MM:SS` format, and a description. This file must point to an already accessible download.
4. Check that the **Sync community patches list** action succeeds. It downloads the published bundle, extracts its metadata, and updates `patches-list.json` on `main`. It rejects a bundle whose version differs from `patches-bundle.json`. You can also rerun it from **Actions → Sync community patches list → Run workflow**.

The list is generated from the published `.mpp`, rather than sources that might already be preparing the next version. Names, descriptions, options, and compatibility information come from the objects loaded by Morphe Patcher. Do not edit `patches-list.json` manually. To reproduce generation locally with JDK 21, download [Gson 2.13.2](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar) to `.local/tools/gson-2.13.2.jar`, then run:

```bash
"$JAVA_HOME/bin/java" --class-path '.local/tools/morphe-desktop.jar:.local/tools/gson-2.13.2.jar' \
  morphe/GeneratePatchesList.java morphe/build/deezer-local-1.1.4.mpp \
  patches-bundle.json patches-list.json
```

Adjust the `.mpp` filename to match the published version. Tool versions and their SHA-256 checksums are pinned in the workflow.

## Next steps

Complete testing of offers during a session, other ad formats, and the lyrics player on a track without lyrics. For each additional ad format observed, target its entry point and verify that it disappears with the full bundle. Before targeting another Deezer version, locate the methods using their references and string constants, then verify the new version on the emulator.
