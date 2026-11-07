# Installing ready-made BlueOS on a Google Pixel 6a

**English** | [Русский](INSTALL.ru.md)

This guide and this zip are **only for the Pixel 6a** (codename `bluejay`); the build is on this repository's
**Releases** page. Don't install it on any other phone: it won't boot. The Pixel 6, Pixel 6 Pro and Galaxy S20 FE have
their own builds and guides: [INSTALL-Pixel6.md](INSTALL-Pixel6.md), [INSTALL-Pixel6Pro.md](INSTALL-Pixel6Pro.md),
[INSTALL-S20FE.md](INSTALL-S20FE.md). For other phones there is an experimental image ([GSI.md](GSI.md)), or you can
build BlueOS yourself ([BUILDING.md](BUILDING.md)).

## ⚠️ Read before you start
- **Unofficial fan ROM, no warranty of any kind.** Whatever you do to your phone is at your own risk. This is not a
  LineageOS build: don't report BlueOS bugs to LineageOS, report them here (Issues).
- **Everything on the phone will be erased**: once when you unlock the bootloader and again when you install. Save
  your photos, chats and **two-factor codes** (Google Authenticator and the like) beforehand.
- **The bootloader stays unlocked.** The phone shows a warning about it on every boot, and that is expected.
  **Don't lock the bootloader while BlueOS is installed** (`fastboot flashing lock`): the phone will stop booting. You
  can lock it only after going back to the stock firmware (below).
- **Some apps may not work:** tap to pay (Google Wallet) and some banking apps and games check that the firmware is
  "factory" (Play Integrity) and refuse to run on BlueOS. Check whether you need them.
- **This build has no Blue Archive character or sounds.** Everything else works. You can add the game files right
  on the phone: Wallpaper & style → Character → Character files ([ASSETS.md](ASSETS.md)). Where to get them: the link
  in the description of the author's TikTok profile, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play is included** (MindTheGapps).
- **Updates** install without a wipe, but only builds from this page: they are signed with the same key. A build you
  made yourself is signed with your key, so these updates won't install over it (and the other way round) without
  wiping the data.
- A carrier-locked phone can't unlock its bootloader, and then BlueOS can't be installed.

## What to download
| File | What for |
|---|---|
| `BlueOS-0.8.1-bluejay-20261105.zip` | the ROM itself |
| `boot.img`, `dtbo.img`, `vendor_boot.img` | for the first install: the boot partition and the BlueOS recovery |
| `SHA256SUMS` | checksums, to check that the files downloaded completely |

Check: Windows (PowerShell) `Get-FileHash .\BlueOS-0.8.1-bluejay-20261105.zip`, Linux/macOS `sha256sum -c SHA256SUMS`.
The sums must match. If they don't, download again and don't flash.

## Before you start
- Update the stock firmware to **Android 17, the October 2026 update or newer** (Settings → System → System update).
  BlueOS may not boot on older firmware.
- A computer, a USB cable and [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
  (`adb` and `fastboot`). On Windows also the [Google USB Driver](https://developer.android.com/studio/run/win-usb).
- Charge the phone to at least 50 %.

## 1. Unlock the bootloader (once)
1. Settings → About phone → tap «Build number» 7 times: developer mode turns on.
2. Settings → System → Developer options: turn on **OEM unlocking** and **USB debugging**.
3. Connect the phone to the computer and allow debugging on the phone. In the platform-tools folder:
```
adb reboot bootloader
fastboot flashing unlock
```
4. On the phone, pick «Unlock the bootloader» with the volume buttons and confirm with the power button. The phone is
   erased. After it boots, turn on USB debugging again (step 2).

## 2. Flash the BlueOS recovery
```
adb reboot bootloader
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot flash vendor_boot vendor_boot.img
```
Then pick **Recovery mode** with the volume buttons and press power: the BlueOS recovery opens (the logo with a halo).

## 3. Install BlueOS
1. In recovery: **Factory reset → Format data / factory reset** → confirm. (Required when you come from stock or
   another ROM.)
2. Back → **Apply update → Apply from ADB**.
3. On the computer:
```
adb sideload BlueOS-0.8.1-bluejay-20261105.zip
```
   It stops at 47 % or 100 % and prints «Total xfer»: that's normal. The phone should say «Install completed».
4. **Reboot system now.** The first boot takes a few minutes.

## Updates
New BlueOS versions install **without a wipe**: recovery → Apply update → Apply from ADB → `adb sideload <new zip>`
(or `adb reboot sideload` from the system with USB debugging on). Your data stays. You can't go back to an older
version this way, only with a wipe.

## If something goes wrong
- Stuck on the logo for more than 10 minutes: go to recovery (power + volume down, then Recovery mode), do Format data
  and install the zip again.
- **Going back to stock:** [flash.android.com](https://flash.android.com) (Android Flash Tool, Chrome/Edge). It flashes
  the factory system and, if you want, locks the bootloader again. Your data is erased.
- Blur in the shade disappears right after installing or while charging: the phone got warm and Android turned blur
  off by itself. It comes back when the phone cools down.
- Found a bug? Open an Issue: what you did and what you saw, ideally with a screenshot.
