# Porting BlueOS to another phone

**English** | [Русский](PORTING.ru.md)

## What doesn't depend on the phone
`vendor/blueos` knows nothing about any particular phone: it is hooked in with one line in `vendor/extra/product.mk`
and patches only the common LineageOS/AOSP projects (SystemUI, the launcher, Settings, Wallpaper & style, Clock,
Calendar, Gallery, the setup wizard, recovery, `vendor/lineage`). Device trees, the kernel and blobs are not touched.

So a "port" is usually one of these:
1. **Your phone is supported by LineageOS 24.0** (officially or with an unofficial lineage-24.0 tree): build it with
   [BUILDING.md](BUILDING.md) and check the points below.
2. **It isn't supported**: first you need LineageOS 24.0 itself for it (device tree, kernel, blobs). That is a
   separate, big job unrelated to BlueOS: look for a tree on XDA / GitHub by the phone's codename. BlueOS goes on top
   of a build that already works.
3. **It exists only for the previous version (23.2)**: sometimes you can take the device tree from 23.2 and the common
   part from 24.0, and port what is missing from related devices. That is how the Galaxy S20 FE was done:
   [ports/r8q](ports/r8q/README.md).

## What to check on a different screen
Everything below was tuned and measured on a Pixel 6a (1080×2400, 420 dpi, a 4×5 home screen grid).
- **The grid and the default layout.** The launcher patch makes `4_by_5` the default grid, and the layout of a fresh
  install is `packages/apps/Launcher3/res/xml/default_workspace_4x5.xml`. On a tablet or a phone with another default
  grid, the LineageOS layout for that grid is used instead: copy ours to `default_workspace_<grid>.xml` and adjust the
  cells.
- **Widget sizes.** BlueOS widgets pick their layout by their size in dp (for example, «Music + equalizer» is wide
  from 250 dp, the toggles with labels from 300×110 dp; the thresholds are `SizeF(...)` in
  `frameworks/base/packages/SystemUI/src/com/android/systemui/blueos/widgets/*.kt`). The thresholds include the
  widget's 8 dp outer margin. On a Pixel 6a a 4×5 grid cell is 98.7×145.5 dp. On another screen check that the 2×1
  and 4×1 widgets get the right layout (`adb shell dumpsys appwidget` shows the sizes).
- **The character on the lock screen** (if you added the files, see [ASSETS.md](ASSETS.md)): her positions are set as
  fractions of the screen, so other aspect ratios may need «Custom» (Wallpaper & style → Character).
- **The boot animation** is a 640×336 frame in the centre and fits any screen. The recovery logo exists for every
  density.

## Common problems
- **`BlueOS: FAILED to apply …`** on `source build/envsetup.sh`: the patch didn't apply, because LineageOS has moved
  on from the version it was made on. Go into the project (`cd frameworks/base`), run
  `git am --3way <path to the patch>`, resolve the conflict (`git status` shows the files), `git add`,
  `git am --continue`. Fitting the change to the new code is a good task for an AI agent ([AGENTS.md](AGENTS.md)).
- **`duplicate sysprop`**: the device tree sets the same property (`ro.*`) as BlueOS. In the device tree make the
  assignment optional (`?=`) or remove the property.
- **Our defaults don't take effect** (dark theme, AOD off, lock sound…): the device tree or LineageOS adds its own
  overlays, they become RROs and can override ours. Check `adb shell cmd overlay list | grep -i auto_generated` and look
  at the overlays in `device/<maker>/<phone>/overlay*`.
- **The build spends ~1 hour on "analysis"** after a change to `Android.bp`/`.mk` or new files: that's normal, it is
  faster after that. **Out of memory** (`ninja: build stopped`, OOM): increase swap or build with `-j4`.

## Checking on the phone
1. The BlueOS boot animation; About phone says BlueOS; the setup wizard shows the BlueOS screen with a halo.
2. Lock screen: the clock with a halo, the greeting, the date; AOD is grey.
3. Home screen: BlueOS widgets; Wallpaper & style → «Widgets and icons», «Colors», «Weather», «Phone motion».
4. Changing the wallpaper and dark/light theme: the colours rebuild, and there is no dark text on the lock screen.
5. If you added the character: she is on the lock screen and in the «Character» widget.

If it worked on a new phone, share it (an issue / pull request with what you had to fix): it will help the next people.

## You can try the GSI first
Before building for your phone, you can start the ready-made experimental image: it isn't tied to a device, and
through DSU it doesn't erase anything ([GSI.md](GSI.md)). It has been checked only on a Pixel 6a.
