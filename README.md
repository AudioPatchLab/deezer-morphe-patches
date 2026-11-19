# Morphe patches for Deezer Android

Eight patches for **Deezer Android 9.0.26.3** (Android 8.0+). Use the original APKM archive with package name `deezer.android.app`. The patched app uses `deezer.android.app.morphe` and can be installed alongside the official app.

## Install

1. Open [Add source to Morphe](https://morphe.software/add-source?github=AudioPatchLab/deezer-morphe-patches), or add `https://github.com/AudioPatchLab/deezer-morphe-patches` as a remote source in Morphe Manager.
2. Select Deezer and provide the original **9.0.26.3 APKM** archive.
3. Choose your patches and start patching.

For local import, download the `.mpp` bundle from [GitHub Releases](https://github.com/AudioPatchLab/deezer-morphe-patches/releases/latest). Other Deezer versions are not supported by this bundle.

## Regional availability

**The patches do not bypass Deezer Free’s country restrictions.** If Deezer Free is unavailable in your country, you may be blocked by a subscription screen after signing in and unable to use the app with a free account.

A user in **Japan** reported this behavior: account creation and sign-in succeeded, but the app then required a subscription with no way to continue. The current bundle does not resolve that restriction. We have not reproduced it locally or verified the behavior in every affected country.

Check [Deezer’s official country availability list](https://support.deezer.com/hc/en-gb/articles/115003749449-List-Of-Countries-Deezer-Is-Available-In) before installing.

## Patches

| Patch | Purpose |
| --- | --- |
| **Choose tracks on demand** | Select tracks in albums, playlists, artist pages, library favorites, listening history, and profile recommendations. |
| **Enable player navigation** | Previous and next controls, unlimited skips, and seeking. |
| **Manage the playback queue** | Open the queue and use “Add to queue” and “Play Next”. |
| **Search and sort playlists** | Search from playlist headers and sort from playlist menus. |
| **Block ads** | Hide banner and native ads and skip visual pre-rolls and audio ad breaks. |
| **Hide Premium offers** | Hide offer pop-ups, the Premium tab, upgrade buttons, and locked audio quality options. |
| **Open lyrics from the player** | Open the lyrics view from the player. |
| **Enable offline downloads** | Enable download actions, subject to server-side stream availability. |

## Known limitations

- Downloads and audio quality depend on the streams Deezer provides. A previous free-account test received MP3 at 128 kbit/s; hiding quality offers does not unlock higher-quality streams.
- The shared session-persistence component disables `log.listen` and `log.batch_listen`, which may affect listening history and recommendations.
- Behavior can vary with the account, region, and server responses. Not every ad format or playback scenario has been verified.

Previous investigations and test details are in [`morphe/DIAGNOSTIC.md`](morphe/DIAGNOSTIC.md) (in French).

## Development

Sources are in [`morphe/src/`](morphe/src/), with the bundle version in [`morphe/VERSION`](morphe/VERSION). The build requires **JDK 21**, **Gradle 9.8.2**, Android SDK platform **34**, Build Tools **36.1.0**, and [Morphe Desktop 2.2.0](https://github.com/MorpheApp/morphe-desktop/releases/tag/v2.2.0) (Morphe Patcher 2.4.0).

Place the Desktop JAR at `.local/tools/morphe-desktop.jar`, then run from the repository root:

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
./morphe/build-bundle.sh .local/tools/morphe-desktop.jar /path/to/gradle-9.8.2
```

The bundle is written to `morphe/build/`. For device testing, import it into Morphe Manager. The optional [`morphe/test-bundle.sh`](morphe/test-bundle.sh) helper builds signed x86_64 APKs and applies all eight patches by default; it requires the original APKM, a local test signing key, and Android Platform Tools. Validate changes with all eight patches enabled together. Keep accounts, APKs, signing keys, and session logs out of Git; `.local/` is ignored.

## Publishing

1. Increment `morphe/VERSION`, build and test the bundle, then push the sources.
2. Create a stable release tagged `v<version>` and attach `morphe/build/deezer-local-<version>.mpp`.
3. Update [`patches-bundle.json`](patches-bundle.json) with the version, accessible release download URL, UTC timestamp, and description.
4. Verify that **Sync community patches list** succeeds in GitHub Actions. It generates [`patches-list.json`](patches-list.json) from the published bundle for the [community directory](https://morphe-patches.software/?app=deezer.android.app). Do not edit that generated list manually.
