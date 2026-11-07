# BlueOS GSI: for other phones (experimental)

**English** | [Русский](GSI.ru.md)

A GSI is one system image that can run on many Android phones: it replaces only the system itself, and everything
that belongs to the hardware stays from the phone's own firmware. The file is on the **Releases** page, in the
`v0.8.1-gsi-treble` release.

The image is built on **TrebleDroid**: a set of fixes for GSIs that help the system boot and work on phones of
different brands. The previous image (`v0.8.1-gsi`) didn't have them, and for testers it hung on boot and the camera
and fingerprint didn't work.

## ⚠️ Read before you start
- **Checked on one phone only:** a Pixel 6a, started through DSU (method 1). There boot, Wi-Fi, mobile data, sound,
  camera, fingerprint, Google Play and adding the character files work. On other phones it may not boot or may work
  only partly.
- **Unofficial fan ROM, no warranty of any kind.** Everything is at your own risk. Report bugs here (Issues), not to
  LineageOS or TrebleDroid.
- **Find the stock firmware for your exact phone beforehand and learn how to flash it.** That is the only way back.
- **Method 2 erases all data.** Unlocking the bootloader, if it is still locked, erases the phone too.
- **The GSI will be updated rarely**, only with big BlueOS versions. The «System update» item in the GSI doesn't know
  about BlueOS updates: watch the Releases page.

## Will it fit your phone
You need an unlockable bootloader and firmware on **Android 13 or newer**. Turn on USB debugging and check:
```
adb shell getprop ro.treble.enabled
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.boot.dynamic_partitions
adb shell getprop ro.boot.flash.locked
```
The first three must print `true`, `arm64-v8a`, `true`. If the answer is different, this image won't fit. The last
line shows the bootloader: `0` means unlocked, `1` locked. With a locked bootloader the image won't start with either
of the methods below.

## If something doesn't work
Settings has a **TrebleDroid Settings** section (Settings → System). It turns on fixes for particular phone brands,
for example for sound, brightness or the fingerprint sensor. If one of them helped, say so in Issues.

## What may not work
- **On phones whose firmware is far from plain Android** (often Samsung, Xiaomi, MediaTek phones), sound, camera,
  fingerprint, brightness or calls can still break.
- **Blur and glass** depend on the phone's firmware: some phones won't have them.
- **Weather.** On the Pixel 6a, Google didn't provide a location in the GSI. If the widget says «No weather data»,
  pick a city: Wallpaper & style → Weather → City.
- **Widgets and the grid** are tuned for the Pixel 6a screen and may look worse on other screens.
- **Tap to pay, some banking apps and games** may refuse to work (Play Integrity).
- **The kernel, recovery and boot logo** stay from your firmware: the GSI doesn't change them.

Google Play is included (MindTheGapps). The image has no Blue Archive characters or sounds; you can add the game files
right on the phone: Wallpaper & style → Character → Character files ([ASSETS.md](ASSETS.md)). Where to get them: the
link in the description of the author's TikTok profile, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).

## What to download
| File | What for |
|---|---|
| `BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz` | the image itself: 1.7 GB, 3.5 GB unpacked |
| `SHA256SUMS` | the checksum, to check that the file downloaded completely |

Check: Windows (PowerShell) `Get-FileHash .\BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz`, Linux/macOS
`sha256sum -c SHA256SUMS`. The sums must match.

You'll need a computer, a cable and [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
(`adb` and `fastboot`).

## Method 1. Try it without erasing anything (DSU): start with this one
The GSI runs next to your system, and your data stays. The next reboot takes the phone back to its usual system.
You **don't need** to unpack the file.

You need an unlocked bootloader and **about 25 GB of free space** in the phone's storage: 1.7 GB for the file itself,
3.5 GB for the system, 8 GB for the GSI's data, and on most newer phones Android also wants a reserve the size of the
phone's system partition (usually 8–14 GB). With less space the install ends with an error. You can get by with less:
in the last command, replace `8589934592` (8 GB for the GSI's data) with `4294967296` (4 GB).

Run the commands in the folder where the downloaded file is (or give the full path to it).

```
adb push BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz /sdcard/Download/blueos_gsi.img.gz
adb shell setprop persist.sys.fflag.override.settings_dynamic_system true
adb shell am start-activity -n com.android.dynsystem/com.android.dynsystem.VerificationActivity -a android.os.image.action.START_INSTALL -d file:///storage/emulated/0/Download/blueos_gsi.img.gz --el KEY_SYSTEM_SIZE 3512172544 --el KEY_USERDATA_SIZE 8589934592
```
The number after `KEY_SYSTEM_SIZE` is the exact size of the unpacked image in bytes, and every image has its own.
With a different number the install ends with an error, so copy the command from this guide for the image you
downloaded.

A notification shows the install progress. When it finishes, reboot the phone into the GSI with the button in that
notification. The same notification has a button that removes the installed image.

*On the Pixel 6a the install was checked with these commands, and the reboot into the GSI was done with `gsi_tool` as
root. We didn't check the button in the notification.*

The GSI has its own, separate data: you'll get the first setup again, and you'll have to turn on USB debugging once
more.

### If the install ends with an error
Most often the reason is one of these:
- **Not enough free space** (see above, you need about 25 GB). Delete something or reduce the GSI's data to 4 GB.
- **The bootloader is locked** (`ro.boot.flash.locked` = `1`).
- **The number in the command is for a different image.** Copy the command from this guide again.
- **The phone has no DSU.** If no notification appeared after the third command, or `adb` prints
  `Activity not found` or `does not exist`, the maker removed DSU (this happens on Samsung and some Xiaomi firmware).
  Then only method 2 is left.

If none of these fits, take the install log and attach it to your message in Issues:
```
adb logcat -c
```
Then run the third install command again, wait for the error and save the log:
```
adb logcat -d > dsu-log.txt
```

### If the usual system opens again after the reboot
That means the GSI couldn't boot and the phone went back to its own system by itself. Take a report right after that:
it often keeps the log of the failed boot.
```
adb bugreport bugreport.zip
```
Attach `bugreport.zip` to your message in Issues. Keep in mind that the report has a lot of personal information
(Wi-Fi network names, the list of apps, accounts): publish it only if you are fine with that.

## Method 2. Install it in place of the system (erases data)
**This method hasn't been checked on a real phone.** It is the usual way to install an Android GSI, but some phones
need steps of their own. Try method 1 first.

1. Unpack the `.img.gz` (Windows: 7-Zip, Linux/macOS: `gunzip`) to get `BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img`.
2. Take the `vbmeta.img` file from your phone's stock firmware and turn off the system check:
```
adb reboot bootloader
fastboot flash --disable-verity --disable-verification vbmeta vbmeta.img
```
3. Flash the image and erase the data:
```
fastboot reboot fastboot
fastboot flash system BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img
fastboot -w
fastboot reboot
```
If `fastboot` says there isn't enough space, run `fastboot delete-logical-partition product_a` before `flash system`
(on phones without A/B slots, `fastboot delete-logical-partition product`).

**Don't lock the bootloader while the GSI is installed:** the phone may stop booting. The only way back is your
phone's stock firmware.

## Found a bug, or everything works
Write in Issues; this is especially important, since the author has no other phones. Give the phone model, its Android
version, the install method, the exact error text and what works and what doesn't, ideally with a screenshot.

## Thanks
The image is built on [TrebleDroid](https://github.com/TrebleDroid) (phhusson and all contributors), with the Android
17 port by [RestlessOS](https://github.com/cawilliamson/treble_restlessos) (Christopher A. Williamson) and the
LineageOS GSI build by [MisterZtr](https://github.com/MisterZtr/LineageOS_gsi).
