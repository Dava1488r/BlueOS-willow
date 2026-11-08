# Installing ready-made BlueOS on a Redmi Note 8 / Redmi Note 8T

**English** | [Русский](INSTALL-RedmiNote8T.ru.md)

This guide and this zip are for **Redmi Note 8T** (codename `willow`) and **Redmi Note 8** (codename `ginkgo`); the build is on the **Releases** page (`v0.8.1-willow`).

### Device Compatibility:

| Model | Codename | Features | Support |
|---|---|---|---|
| **Redmi Note 8T** | `willow` | NFC supported (NXP PN553) | ✅ Fully Supported |
| **Redmi Note 8** | `ginkgo` | No NFC | ✅ Fully Supported |

BlueOS is based on **LineageOS 24.0 (Android 17)** with the BlueOS layer (live halo clock, Blue Archive widgets, Arona library, custom animations and UI).

---

## ⚠️ Read before you start
- **Unlocked bootloader:** Required. Unlocking your bootloader wipes all data.
- **Backup EFS and Persist:** In recovery, always make a backup of **EFS** and **Persist** partitions to a MicroSD card or PC. These store your modem calibrations, Wi-Fi MAC and IMEI.
- **Clean wipe:** Coming from MIUI or another custom ROM requires formatting data.
- **Characters & Sounds:** In compliance with licensing, game assets from Nexon/Yostar are not pre-packaged. You can add the character (Arona / Plana) directly on the phone via: *Settings → Wallpaper & style → Character → Add character files* ([ASSETS.md](ASSETS.md)).
- **Google Play:** Included out-of-the-box (MindTheGapps for Android 17).

---

## Requirements
1. PC with `adb` and `fastboot`.
2. USB cable (use USB 2.0 port).
3. **Recovery:** Recommended **OrangeFox Recovery R11.1+** with Retrofit Dynamic Partitions support for `ginkgo/willow`.
4. ROM package: `BlueOS-0.8.1-ginkgo-*.zip`.

---

## Installation Steps

### Step 1. Fastboot Mode
1. Power off the phone.
2. Hold **Volume Down** + **Power** until the FASTBOOT screen appears.
3. Connect to PC.

### Step 2. Flash Recovery
```bash
fastboot flash recovery recovery.img
fastboot reboot recovery
```
*(Hold Volume Up while rebooting to enter recovery).*

### Step 3. Wipe
In OrangeFox / TWRP:
1. Go to **Wipe**.
2. Select **Format Data** and type `yes`.
3. Go to **Advanced Wipe**: check *Dalvik / ART Cache*, *Cache*, *System*, *Vendor*.
4. Swipe to wipe.

### Step 4. Flash BlueOS
1. In recovery, select **Menu** → **ADB Sideload**.
2. On PC:
```bash
adb sideload BlueOS-0.8.1-ginkgo.zip
```
3. Wait for the installation to finish.

### Step 5. Reboot
1. Tap **Reboot System**.
2. First boot takes 2–3 minutes with the BlueOS boot animation and halo.
