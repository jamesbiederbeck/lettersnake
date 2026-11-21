# Letter Snake

Swipe to steer the snake and eat the letters of the target word in order. The next letter is highlighted in the word at the top; the board always holds that letter plus three decoys. A wrong letter or a crash costs one of three lives. For early readers.

Built with Make — no Gradle, no IDE. Package `design.antics.lettersnake`,
minSdk 24 / targetSdk 33.

## Dependencies

```
sudo apt install \
  google-android-platform-33-installer \
  google-android-build-tools-34.0.0-installer \
  openjdk-17-jdk \
  apksigner \
  zipalign
```

`adb` comes from `../platform-tools/adb`.

## Build

```bash
make keystore              # one-time: generate debug.keystore
make                       # build signed APK (aligned.apk)
make install               # build + adb install
make run                   # install + launch
make logcat                # tail this app's log
make clean
```

`DEVICE` auto-detects the first device `adb` sees. Override with
`make run DEVICE=<ip>:<port>` or by exporting `ANDROID_DEVICE`.

## Word packs and parents menu

Packs (`Packs.java`): alphabet runs, short words (cat, dog), sight words, four-
and five-letter words. Hamburger top-left -> PIN (default **1234**) -> toggle
packs, reset the best score, change the PIN. Grid logic is in `Board.java` (no
Android dependencies, so it can be tested on the JVM). Shared look: see `DESIGN.md`.
