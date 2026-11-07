# Building BlueOS

**English** | [Русский](BUILDING.ru.md)

BlueOS is LineageOS 24.0 plus one folder, `vendor/blueos`. If LineageOS 24.0 builds for your phone, BlueOS will build
too: first build and install plain LineageOS following its guide, then add BlueOS.

## 1. What you need
- **A phone that LineageOS 24.0 supports officially**: check on [wiki.lineageos.org/devices](https://wiki.lineageos.org/devices)
  (your device's «Build for yourself» section). Not on the list? See [PORTING.md](PORTING.md).
- An unlocked bootloader (unlocking erases the phone).
- A computer with Linux (Ubuntu 22.04/24.04) or Windows with WSL2. **RAM: 32 GB** (with a 32–48 GB swap file; with
  16 GB the build works, but very slowly). **Disk: ~350 GB free on an SSD** (sources ~150 GB, the build ~150 GB).
- Time: downloading the sources takes a few hours, the first build 2–6 hours, later ones 20–60 minutes.

## 2. LineageOS 24.0 sources
Do everything as your device's «Build for yourself» section on the LineageOS wiki says: install the packages, `repo`,
```
repo init -u https://github.com/LineageOS/android.git -b lineage-24.0 --git-lfs --no-clone-bundle
repo sync -j8
```
then `breakfast <device>` and the maker's closed files (blobs: `extract-files.py` from the phone, or ready-made ones
from TheMuppets). **Build and install plain LineageOS first**: then you know the device tree is fine, and any error
after that comes from BlueOS.

## 3. Add BlueOS
In `.repo/local_manifests/blueos.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <remote name="blueos" fetch="https://github.com/GDmio" />
  <project path="vendor/blueos" name="BlueOS" remote="blueos" revision="main" />
</manifest>
```
`revision="main"` is the newest BlueOS. For exactly the version of a release, use `revision="refs/tags/v0.8.1"`.

Hooking BlueOS into the build takes one line:
```
mkdir -p vendor/extra
echo '$(call inherit-product, vendor/blueos/config/blueos.mk)' >> vendor/extra/product.mk
```
**Google Play (optional).** BlueOS doesn't contain Google services itself. If you want them, add MindTheGapps for
Android 17 in `.repo/local_manifests/gapps.xml`:
```xml
<manifest>
  <project name="MindTheGapps/vendor_gapps" path="vendor/gapps" remote="github" revision="cinnamonbun" clone-depth="1" />
</manifest>
```
`config/blueos.mk` hooks them in by itself if the `vendor/gapps` folder exists. Then `repo sync vendor/blueos vendor/gapps`.

## 4. Build
```
source build/envsetup.sh
breakfast <device>
m bacon -j$(nproc)
```
`source build/envsetup.sh` applies the BlueOS patches by itself (`vendor/blueos/vendorsetup.sh` →
`scripts/apply-patches.sh`). **Look at the output:** there should be `BlueOS: applied …` lines (on the first run) and
**not a single** `BlueOS: FAILED`. Running it again is safe: patches that are already applied are skipped.

**If a patch didn't apply** (`BlueOS: FAILED`): LineageOS has moved on from the version the patches were made on.
There are two ways:
1. Adjust the patch to the new code: PORTING.md, «Common problems» (a good task for an AI agent).
2. Simpler: pin the patched projects to the LineageOS revisions where the patches surely apply. The rest of LineageOS
   stays fresh (`repo sync` resets these projects to clean revisions, `envsetup.sh` applies the patches again):
```
cp vendor/blueos/manifests/blueos-pinned.xml .repo/local_manifests/
repo sync -j8 --force-sync
source build/envsetup.sh
```
   To go back to the fresh LineageOS, delete this file from `.repo/local_manifests/` and run `repo sync` again.

The finished package: `out/target/product/<device>/BlueOS-<version>-<device>-<date>.zip`.

**Signing.** Sign your builds with your own keys (LineageOS wiki: «Signing builds»). Never give your keys to anyone or
publish them; if you lose them, later builds won't install over the old ones without a wipe.

## 5. Install
Like plain LineageOS for your device: the recovery from the same build, then `adb sideload <package>`. Coming from
another ROM needs a data wipe. Updating BlueOS over BlueOS (same keys) doesn't.

## 6. After the first build
- **Character, sounds, wallpapers**: optional, from your own files: [ASSETS.md](ASSETS.md), then rebuild.
- **«LineageOS» → «BlueOS» in the texts** of your device's apps: `python3 vendor/blueos/tools/rebrand.py` (from the
  top of the source tree, after a full build), then rebuild.
- **Updating the sources:** `repo sync`, then `source build/envsetup.sh` again: the patches apply again.

## 7. How to check that it is BlueOS
- The boot animation: a halo and the word «BlueOS».
- Settings → About phone: the BlueOS version.
- Wallpaper & style: the «Live clock», «Widgets and icons», «Colors», «Weather», «Phone motion» pages.
- BlueOS widgets on the home screen (the clock with a halo, «Summary», equalizers, weather).

## GSI
An image for many phones builds from the same tree, without a device tree or its files:
```
source build/envsetup.sh
breakfast gsi_arm64
m systemimage
```
The result: `out/target/product/generic_arm64/system.img`. In the same `out/` folder it takes about 65 GB on top of
the phone build; in a separate one, as much as a full build. Switching between the GSI and a phone rebuilds the shared
part. How to install it and what may not work: [GSI.md](GSI.md).
