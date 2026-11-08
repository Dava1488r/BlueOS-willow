# Redmi Note 8 / 8T (`ginkgo` / `willow`): how the BlueOS build is made

Redmi Note 8 (`ginkgo`) and Redmi Note 8T (`willow`) share the same Qualcomm Snapdragon 665 (`sm6125`) platform,
with `willow` adding NXP PN553 NFC support. Official LineageOS has active `lineage-24.0` (Android 17) branches
across the unified device tree, SM6125 common tree, kernel, and TheMuppets vendor repositories.
BlueOS integrates on top of this official LineageOS 24.0 tree as a clean `vendor/blueos` product layer.

## Sources
`.repo/local_manifests/ginkgo.xml` (or add to your local manifest), on top of a LineageOS 24.0 tree:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <project path="device/xiaomi/ginkgo" remote="github" name="LineageOS/android_device_xiaomi_ginkgo" revision="lineage-24.0" />
  <project path="device/xiaomi/sm6125-common" remote="github" name="LineageOS/android_device_xiaomi_sm6125-common" revision="lineage-24.0" />
  <project path="kernel/xiaomi/sm6125" remote="github" name="LineageOS/android_kernel_xiaomi_sm6125" revision="lineage-24.0" />
  <project path="hardware/xiaomi" remote="github" name="LineageOS/android_hardware_xiaomi" revision="lineage-24.0" />
  <project path="vendor/xiaomi/ginkgo" remote="github" name="TheMuppets/proprietary_vendor_xiaomi_ginkgo" revision="lineage-24.0" clone-depth="1" />
  <project path="vendor/xiaomi/sm6125-common" remote="github" name="TheMuppets/proprietary_vendor_xiaomi_sm6125-common" revision="lineage-24.0" clone-depth="1" />
</manifest>
```

## Hardware specifics
- **Screen & Cutout:** 1080×2340 (19.5:9), 409 DPI, waterdrop (teardrop) camera cutout. The BlueOS live halo clock
  and status bar padding are tailored to stay centered right below the notch.
- **Launcher Grid:** Defaults to 4×5, matching the display aspect ratio.
- **NFC (`willow`):** Auto-detected at runtime through bootloader SKU (`ro.boot.product.hardware.sku=willow`).
  On `ginkgo` (no NFC chip), the NFC HAL is safely disabled by the common tree without crashes.
- **Fingerprint:** Goodix / FPC rear capacitive sensor works natively on the `sm6125-common` HAL.
- **Audio:** High-res Qualcomm WCD937x codec via ALSA audio HAL.

## Build
Like any other device ([BUILDING.md](../../BUILDING.md)):
```bash
source build/envsetup.sh
./vendor/blueos/ports/ginkgo/apply-ginkgo-fixes.sh
breakfast ginkgo
m bacon -j$(nproc)
```

The install generates `recovery.img` and the flashable ROM zip in `out/target/product/ginkgo/`.
See [INSTALL-ginkgo.ru.md](../../INSTALL-ginkgo.ru.md) for step-by-step flashing instructions.
