# Installing ready-made BlueOS on a Google Pixel 7

**English** | [Русский](INSTALL-Pixel7.ru.md)

This guide and this zip are **only for the Pixel 7** (codename `panther`); the build is on the **Releases** page (`v0.8.1-panther`). Don't install it on a Pixel 7 Pro or a Pixel 7a: it won't boot. For the Pixel 7 Pro see [INSTALL-Pixel7Pro.md](INSTALL-Pixel7Pro.md), for the Pixel 7a [INSTALL-Pixel7a.md](INSTALL-Pixel7a.md).

## ⚠️ Read before you start
- **Unofficial fan ROM, no warranty of any kind.** Whatever you do to your phone is at your own risk. This is not a LineageOS build: don't report BlueOS bugs to LineageOS, report them here (Issues).
- **Everything on the phone will be erased**: once when you unlock the bootloader and again when you install. Save your photos, chats and **two-factor codes** (Google Authenticator and the like) beforehand. Remove your Google account from the phone so that it doesn't ask for the old one after the reset.
- **The bootloader stays unlocked.** The phone shows a warning about it on every boot, and that is expected. **Don't lock the bootloader while BlueOS is installed**: the phone will stop booting.
- **Some apps may not work:** tap to pay (Google Wallet), some banking apps and games.
- **This build has no Blue Archive character or sounds.** Everything else works. You can add the game files right on the phone: Wallpaper & style → Character → Character files ([ASSETS.md](ASSETS.md)). Where to get them: the link in the description of the author's TikTok profile, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play is included** (MindTheGapps).
- A carrier-locked phone can't unlock its bootloader, and then BlueOS can't be installed.

## What to download
| File | What for |
|---|---|
| `BlueOS-0.8.1-panther-20261108.zip` | the ROM itself, don't unpack it |
| `boot.img`, `dtbo.img`, `init_boot.img`, `vendor_boot.img` | for the first install: the boot partitions and the BlueOS recovery |
| `SHA256SUMS` | checksums, to check that the files downloaded completely |

Check: Windows (PowerShell) `Get-FileHash .\BlueOS-0.8.1-panther-20261108.zip`, Linux/macOS `sha256sum -c SHA256SUMS`. The sums must match.

## Before you start
- Update the stock firmware to **Android 17, the October 2026 update or newer**. BlueOS may not boot on older firmware.
- Check that calls and SMS work on the stock firmware.
- A computer, a USB cable and [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) (`adb` and `fastboot`). On Windows also the [Google USB Driver](https://developer.android.com/studio/run/win-usb).
- Charge the phone to at least 50 %.

## 1. Unlock the bootloader (once)
1. Settings → About phone → tap «Build number» 7 times: developer mode turns on.
2. Settings → System → Developer options: turn on **OEM unlocking** and **USB debugging**.
3. Connect the phone to the computer and allow debugging on the phone. In the platform-tools folder:
```bash
adb reboot bootloader
fastboot flashing unlock
```
4. On the phone, pick «Unlock the bootloader» with the volume buttons and confirm with the power button. The phone is erased. After it boots, turn on USB debugging again.

## 2. Flash the BlueOS recovery
```bash
adb reboot bootloader
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot flash init_boot init_boot.img
fastboot flash vendor_boot vendor_boot.img
```
Then pick **Recovery mode** with the volume buttons and press power: the BlueOS recovery opens (the logo with a halo).

## 3. Install BlueOS
1. In recovery: **Factory reset → Format data / factory reset** → confirm.
2. Back → **Apply update → Apply from ADB**.
3. On the computer:
```bash
adb sideload BlueOS-0.8.1-panther-20261108.zip
```
   It may stop at 47 % or print «failed to read command»: that's normal. The phone should say «Install completed».
4. If recovery asks whether to reboot to install add-ons, answer «No»: Google Play is already included.
5. **Reboot system now.** The first boot takes a few minutes.
