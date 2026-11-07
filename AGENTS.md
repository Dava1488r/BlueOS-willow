# BlueOS — guide for AI coding agents

You are helping someone build or port **BlueOS** (a fan ROM: LineageOS 24.0 = Android 17, SDK 37, + `vendor/blueos`).
They are often beginners at Android/Linux. Answer in their language, explain *why* before each step, and say how to
check that it worked. Long steps (sync, build) take hours — run them in the background and keep watching.

## Ground rules
- **No game files.** Never download Blue Archive assets for the user, never point them to other collections of ripped
  files, never add such files to a public repository (`scripts/check-public.sh` refuses them: run it before committing
  here). The only pointer to give is the one in [ASSETS.md](ASSETS.md), the author's profile. The files can be added on
  the phone (Wallpaper & style → Character → Add character files, no build) or into the user's own build
  (`tools/import-assets.py <any folder>`); everything works without them.
- **Credit BlueOS.** BlueOS is Apache 2.0 with an attribution condition ([LICENSE](LICENSE)): if the user will share
  their build, port or fork (or code taken from here), it must keep [NOTICE](NOTICE) and say in its description or
  release notes that it is based on BlueOS by GDmio, with a link to https://github.com/GDmio/BlueOS . Tell them so
  before they publish.
- **Never publish or commit signing keys** (`vendor/lineage-priv/keys`, `~/.android-certs`). Back them up.
- **The phone is the source of truth.** "It's in the apk" proves nothing — check on the device (`adb`, screenshots,
  `dumpsys`). Before flashing: `adb devices`; after: `adb shell getprop ro.build.date` (a dropped connection once made
  a flash silently not happen). Downgrades are refused by sideload: fixes go forward only.
- Unlocking the bootloader and installing from another ROM **wipe the phone** — say so before.

## Order of work
1. Is the device supported by LineageOS 24.0? (`wiki.lineageos.org/devices/<codename>`, or an unofficial lineage-24.0
   tree). If not, BlueOS is not the first problem — LineageOS for the device is.
2. Check the machine: free disk (`df -h`; ~350 GB), RAM (32 GB + swap recommended), Ubuntu/WSL2.
3. Sync LineageOS 24.0, set up the device per its wiki page, **build and boot plain LineageOS first**.
4. Add BlueOS ([BUILDING.md](BUILDING.md)): local manifest for `vendor/blueos`, the hook line in
   `vendor/extra/product.mk`, optional MindTheGapps (`vendor/gapps`, branch `cinnamonbun`).
5. `source build/envsetup.sh` → read the output: `BlueOS: applied …` / `BlueOS: FAILED …`. Fix failed patches before
   building: either port the patch to the new code (below), or — simpler for a beginner — pin the patched projects
   to the revisions the patches were made on: `manifests/blueos-pinned.xml` → `.repo/local_manifests/`, `repo sync`
   (BUILDING.md §4). `breakfast <device>`, `m bacon`.
6. Flash, then go through the checklist in [PORTING.md](PORTING.md) on the phone.

## How the code is organised
- `config/blueos.mk` — the product config (overlays, boot animations, fonts, sounds, characters, sepolicy, GApps).
  Optional files are copied with `$(wildcard)`: a missing file must never break the build. Don't turn them into
  Android.bp modules — Soong fails on a missing `src`.
- `vendorsetup.sh` — `BLUEOS_VERSION` + `TARGET_UNOFFICIAL_BUILD_ID`, then `scripts/apply-patches.sh`. The name shown
  in «About phone» and the zip's name: `BlueOS-<version>-<device>-<date>` (`vendor/lineage` patch).
- `patches/<project path>/*.patch` — BlueOS commits on top of LineageOS projects. `apply-patches.sh` applies them with
  `git am --3way` and skips any whose subject is already in the project's last 200 commits (idempotent, re-run after
  `repo sync`). To change code: edit in the project, commit with a subject starting `BlueOS:`, run
  `scripts/export-patches.sh` (base `m/lineage-24.0`; add new projects to its list), commit `vendor/blueos`.
- `libs/arona` (`BlueOSAronaLib`, linked by SystemUI, Launcher3, ThemePicker): `BlueOSSettings` (every
  `Settings.Secure` key `blueos_*` and its default), `BlueOSColorRoles` (the only place colours are decided —
  never compute a colour elsewhere), `BlueOSMotion` (sensors) + `BlueOSOnScreen` (live views stop off screen),
  `Character` / `AronaModel` / `AronaView` (Spine characters; `Character.available` = model files on /product or
  added by the user), `BlueOSMedia` (0.8.1: /product/media/blueos first, else `/data/misc/blueos` — the files the user
  added on the phone with Settings `blueos/BlueOSAssetsActivity`, sepolicy `blueos_media_file`: Settings writes,
  platform apps read; the importer finds files by name using `config/assets-files.txt`, cuts the desk with
  `BlueOSDeskCutter`; `tools/import-assets.py` does the same on the PC for a build),
  `Weather`, `WeatherIconView`.
- SystemUI (`frameworks/base/packages/SystemUI/src/com/android/systemui/blueos/`): lock screen character section,
  widgets (`widgets/`), weather, wallpaper look + live weather + wallpaper equalizer (`wallpaper/`, used by
  `wallpapers/ImageWallpaper.java`), `BlueOSDefaults` (fresh-phone defaults). Clocks:
  `packages/SystemUI/customization/.../shared/clocks/` (Halo, Moe).
- Launcher3 `src/com/android/launcher3/blueos/`: `BlueOSWidgetDecorator` (live layer over BlueOS widgets),
  materials/blur, the launcher's own widgets («Погода», «Персонаж»). Default layout `res/xml/default_workspace_4x5.xml`
  (`packageName="custom-widget"` entries are the launcher's own widgets).
- ThemePicker (Wallpaper & style) `src/com/android/customization/blueos/BlueOSSettingsActivity.kt` — all BlueOS
  settings pages. Strings: `res/values{,-ru}/blueos_*_strings.xml`.

## Build-system lessons (each one cost a build)
- Changing `Android.bp`/`.mk` files or adding/removing files under globbed dirs → 40–60 min of re-analysis; editing
  existing files only → a much faster incremental build. Add strings/files before starting a build, batch changes.
- Redefining a `ro.*` property set elsewhere → "duplicate sysprop": make the other one `?=`.
- Device/LineageOS overlays become runtime RROs (`*__auto_generated_rro_*`) and can override BlueOS defaults —
  check `cmd overlay list`. Defaults that overlays reset at runtime are set from code on a fresh phone (`BlueOSDefaults`).
- Kotlin: Java setters returning non-void aren't properties. RemoteViews: only `@RemotableViewMethod` setters work;
  never bake colours into widget RemoteViews — use `setColorStateList(id, method, R.color.x)`.
- With GApps the setup wizard is Google's SUW driving LineageOS' wizard script; it starts before the user unlocks.

## WSL2 notes (Windows hosts)
- Keep the source inside the WSL filesystem (ext4), not on `/mnt/c`. Give WSL enough memory and swap in `.wslconfig`.
- From PowerShell, don't inline bash with quotes, `$` or pipes into `wsl -- …`; write a script file and run it.
- Don't edit files inside WSL through `\\wsl.localhost\…` with tools that rewrite whole files carelessly — one such
  write wiped a file to 0 bytes. Prefer editing inside WSL.
- A background build dies with the terminal/session that started it: an empty exit code + "cancelled" in the log
  means it was killed, not a code error — just restart it (compiled work is kept).

## Checking on the phone
- Screenshots: `adb shell screencap -p /sdcard/x.png && adb pull /sdcard/x.png`. Exact view bounds:
  `adb shell uiautomator dump`. Settings: `adb shell settings list secure | grep blueos`.
- Smoothness: `adb shell dumpsys gfxinfo com.android.launcher3 reset`, use the phone, then
  `dumpsys gfxinfo com.android.launcher3` (janky frames, percentiles). Idle home screen should draw only a few frames.
- Blur gone in the shade / All apps right after flashing or while charging = Android's thermal protection
  (`dumpsys thermalservice` status ≥ 3 → `dumpsys window` `mBlurEnabled=false`), not a bug.
- After a change to colours, check light **and** dark theme and several wallpapers.

## GSI
A prebuilt experimental GSI exists for phones other than the Pixel 6a: [GSI.md](GSI.md) (tested only on a Pixel 6a through DSU; say so).
To build one: `breakfast gsi_arm64`, `m systemimage` (BUILDING.md).
