# Installing ready-made BlueOS on a Samsung Galaxy S20 FE (Snapdragon)

**English** | [Русский](INSTALL-S20FE.ru.md)

This guide and this zip are **only for the Galaxy S20 FE with a Snapdragon 865** (codename `r8q`); the build is on the
**Releases** page (`v0.8.1-r8q`). Testers checked the build on their own S20 FE: it boots and works. If you find a
bug, open an Issue.

Check your model: Settings → About phone → Model number.

| Model | Does it fit? |
|---|---|
| SM-G780G, SM-G780G/DS (S20 FE) | ✅ |
| SM-G781B, SM-G781B/DS, SM-G781N, SM-G7810 (S20 FE 5G) | ✅ |
| **SM-G780F** and other Exynos models | ❌ **no**, that is a different phone |

Official LineageOS for the S20 FE only comes with Android 16 so far. BlueOS for it is built on Android 17: the device
tree is from 16, the common part and the kernel are from 17, and what was missing was ported from Samsung tablets with
the same chip ([ports/r8q](ports/r8q/README.md)).

## ⚠️ Read before you start
- **Unofficial fan ROM, no warranty of any kind.** Whatever you do to your phone is at your own risk. This is not a
  LineageOS build: don't report BlueOS bugs to LineageOS, report them here (Issues).
- **Unlocking the bootloader breaks Samsung Knox for good:** Samsung Pay, Secure Folder and some banking apps stop
  working, even if you go back to the stock firmware later. The warranty may be void too.
- **Everything on the phone will be erased**, more than once. Save your photos, chats and **two-factor codes**
  (Google Authenticator and the like). Remove your Google and Samsung accounts from the phone so that it doesn't ask
  for the old ones after the reset.
- **Don't lock the bootloader while BlueOS is installed**: the phone will stop booting. The unlocked-bootloader
  warning on every boot is normal and can't be removed.
- **This build has no Blue Archive character or sounds.** Everything else works. You can add the game files right
  on the phone: Wallpaper & style → Character → Character files ([ASSETS.md](ASSETS.md)). Where to get them: the link
  in the description of the author's TikTok profile, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play is included** (MindTheGapps).

## What to download
| File | What for |
|---|---|
| `BlueOS-0.8.1-r8q-20261105.zip` | the ROM itself, don't unpack it |
| `recovery.img` | the BlueOS recovery; you need exactly this one, the LineageOS recovery won't accept this ROM |
| `vbmeta.img` | for the first install |
| `SHA256SUMS` | checksums, to check that the files downloaded completely |

Check: Windows (PowerShell) `Get-FileHash .\BlueOS-0.8.1-r8q-20261105.zip`, Linux/macOS `sha256sum -c SHA256SUMS`.
The sums must match. If they don't, download again and don't flash.

## Before you start
- The phone must run **Samsung's stock firmware on Android 13**, its latest version. This is required:
  Settings → About phone → Software information → Android version (look at the Android version, not One UI).
- Check that calls and SMS work on the stock firmware.
- Charge the phone to at least 50 %.
- A computer, a USB cable and:
  - [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) (`adb`);
  - **samloader-rs**, a tool for flashing Samsung phones; the link to it is on the
    [LineageOS page for the S20 FE](https://wiki.lineageos.org/devices/r8q/install/variant1/);
  - on Windows, the [Samsung driver](https://developer.samsung.com/android-usb-driver).

The steps below are the same as [LineageOS' steps for this phone](https://wiki.lineageos.org/devices/r8q/install/variant1/),
just with the BlueOS files.

## 1. Unlock the bootloader (erases the phone)
1. Connect the phone to Wi-Fi.
2. Settings → About phone → Software information → tap «Build number» 7 times.
3. Settings → Developer options: turn on **OEM unlocking** and **USB debugging**.
4. Turn the phone off. Hold both volume buttons and, while holding them, plug the cable into the computer: Download
   mode opens.
5. Follow the prompts on the screen: «Device unlock mode» / «Unlock Bootloader», confirm. The phone is erased and
   reboots.
6. Turn developer options on again and check that «OEM unlocking» is still on.
7. Turn the phone off and enter Download mode again with the same buttons. On the computer, in the folder with the
   files:
```
samloader flash --partition VBMETA vbmeta.img
```
8. The phone reboots and asks for a reset: do it as the prompts say. Go through the initial setup, skipping everything
   you can, connect to Wi-Fi and check once more that «OEM unlocking» is on.

## 2. Flash the BlueOS recovery
1. Turn the phone off, enter Download mode with the same buttons, pick «Continue» on the screen and plug in the cable.
2. On the computer:
```
samloader flash --partition RECOVERY recovery.img --no-reboot
```
3. The phone keeps showing «Downloading... Do not turn off target!!» even after it has finished: that's normal.
4. Hold **volume down + power** for 8–10 seconds. As soon as the screen goes dark, let go and at once hold
   **volume up + power** (the cable must be connected to the computer): recovery opens.
   You have to get into recovery **right away**: otherwise the stock firmware replaces it with its own, and you'll
   have to repeat step 2.
5. A screen with the BlueOS logo should open. If it doesn't, stop here.

## 3. Install BlueOS
1. In recovery: **Factory reset → Format data / factory reset** → confirm.
2. Back → **Apply update → Apply from ADB**.
3. On the computer:
```
adb -d sideload BlueOS-0.8.1-r8q-20261105.zip
```
   It may stop at 47 % or print «failed to read command»: that's normal. The phone should say that the install is
   complete.
4. If recovery asks whether to reboot to install add-ons, answer «No»: Google Play is already included.
5. **Reboot system now.** The first boot can take up to 15 minutes.

## Updates
New BlueOS versions for the S20 FE install **without a wipe**: recovery (volume up + power with the cable connected)
→ Apply update → Apply from ADB → `adb -d sideload <new zip>`. Your data stays.

## If something goes wrong
- Stuck on the logo for more than 15 minutes: go to recovery (volume down + power until the screen goes dark, then at
  once volume up + power), do Format data and install the zip again.
- **Going back to stock:** Download mode + Samsung's stock files for your model (AP, BL, CP, CSC); guides are easy to
  find by the model number. Your data is erased, and Knox stays broken.
- Found a bug? Open an Issue: what you did and what you saw, ideally with a screenshot.
