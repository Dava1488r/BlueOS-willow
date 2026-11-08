# Google Pixel 7 series (`panther`, `cheetah`, `lynx`): how the BlueOS build is made

The Google Pixel 7 series is based on the Google Tensor G2 (`gs201`) platform, succeeding Tensor G1 (`bluejay`/`oriole`/`raven`) on which BlueOS was originally designed. Official LineageOS has active `lineage-24.0` (Android 17) branches for:
- **Pixel 7** (`panther`)
- **Pixel 7 Pro** (`cheetah`)
- **Pixel 7a** (`lynx`)

## Manifest
Add `.repo/local_manifests/pixel7.xml` (from [manifests/pixel7.xml](../../manifests/pixel7.xml)) to your LineageOS 24.0 tree:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <project path="device/google/pantah" remote="github" name="LineageOS/android_device_google_pantah" revision="lineage-24.0" />
  <project path="device/google/panther" remote="github" name="LineageOS/android_device_google_panther" revision="lineage-24.0" />
  <project path="device/google/cheetah" remote="github" name="LineageOS/android_device_google_cheetah" revision="lineage-24.0" />
  <project path="device/google/lynx" remote="github" name="LineageOS/android_device_google_lynx" revision="lineage-24.0" />
  <project path="kernel/google/gs201/private/gs-google" remote="github" name="LineageOS/android_kernel_google_gs201" revision="lineage-24.0" />
  <project path="vendor/google/pantah" remote="github" name="TheMuppets/proprietary_vendor_google_pantah" revision="lineage-24.0" clone-depth="1" />
  <project path="vendor/google/lynx" remote="github" name="TheMuppets/proprietary_vendor_google_lynx" revision="lineage-24.0" clone-depth="1" />
</manifest>
```

## Build
Like any other Google Pixel build:
```bash
source build/envsetup.sh

# For Pixel 7:
breakfast panther
m bacon -j$(nproc)

# For Pixel 7 Pro:
breakfast cheetah
m bacon -j$(nproc)

# For Pixel 7a:
breakfast lynx
m bacon -j$(nproc)
```

The build generates the flashable OTA zip in `out/target/product/<codename>/`, along with `boot.img`, `dtbo.img`, `init_boot.img`, and `vendor_boot.img`.

See the install guides:
- Pixel 7: [INSTALL-Pixel7.md](../../INSTALL-Pixel7.md) ([Русский](../../INSTALL-Pixel7.ru.md))
- Pixel 7 Pro: [INSTALL-Pixel7Pro.md](../../INSTALL-Pixel7Pro.md) ([Русский](../../INSTALL-Pixel7Pro.ru.md))
- Pixel 7a: [INSTALL-Pixel7a.md](../../INSTALL-Pixel7a.md) ([Русский](../../INSTALL-Pixel7a.ru.md))
