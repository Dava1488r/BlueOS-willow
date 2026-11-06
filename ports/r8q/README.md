# Galaxy S20 FE Snapdragon (`r8q`): how the BlueOS build is made

Official LineageOS for `r8q` stops at 23.2 (Android 16) so far: the device tree's `lineage-24.0` branch is the same as
23.2, and TheMuppets has `r8q` blobs only on `lineage-23.2`. The parts it shares with the Galaxy Tab S7 / S7+
(`sm8250-common` tree and blobs, the kernel) do have real `lineage-24.0` branches, because the tablets build on them.
The BlueOS build takes the device tree and blobs from 23.2, everything else from 24.0, and ports from the tablets what
`r8q` lacks for 24.0. Tested by testers on their own S20 FE: it boots and works.

## Sources
`.repo/local_manifests/r8q.xml` (or add the lines to your own local manifest), on top of a LineageOS 24.0 tree:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <project path="device/samsung/r8q" remote="github" name="LineageOS/android_device_samsung_r8q" revision="lineage-24.0" />
  <project path="device/samsung/sm8250-common" remote="github" name="LineageOS/android_device_samsung_sm8250-common" revision="lineage-24.0" />
  <project path="kernel/samsung/sm8250" remote="github" name="LineageOS/android_kernel_samsung_sm8250" revision="lineage-24.0" />
  <project path="hardware/samsung" remote="github" name="LineageOS/android_hardware_samsung" revision="lineage-24.0" />
  <project path="hardware/samsung_slsi/nfc" remote="github" name="LineageOS/android_hardware_samsung_slsi_nfc" revision="lineage-24.0" />
  <project path="vendor/samsung/r8q" remote="github" name="TheMuppets/proprietary_vendor_samsung_r8q" revision="lineage-23.2" clone-depth="1" />
  <project path="vendor/samsung/sm8250-common" remote="github" name="TheMuppets/proprietary_vendor_samsung_sm8250-common" revision="lineage-24.0" clone-depth="1" />
</manifest>
```

Revisions the release `v0.8.1-r8q` was built from: device `r8q` `27bf887` (`lineage-24.0`) + the fixes below, `sm8250-common`
tree `d204502`, kernel `8fd4a4892d63` (`4.19.325`), `hardware/samsung_slsi/nfc` `88490b4`, blobs `r8q` `lineage-23.2`
(`ab3ecab`), `sm8250-common` blobs `9991284`.

## Fixes ported from the tablets
`apply-r8q-fixes.sh` (run from the top of the tree after `repo sync`; idempotent, commits locally in the two projects):
- **HiFi speaker routes.** 24.0 plays music through the HiFi path (gts7xl `7b5a19c14c`); `r8q`'s `mixer_paths.xml`
  has it for everything but the speaker. Added `hifi-playback speaker` on `PRI_TDM_RX_0` (MultiMedia2), `r8q`'s own
  speaker backend.
- **Recording stack.** `sm8250-common` 24.0 (`c01c5839de`, «Decommonize audio recording stack») dropped
  `lib_SamsungRec_07020` and made `librecordalive` a dummy, since the tablets ship their own. `r8q` used these two from
  the common blobs on 23.2, so the script fetches the same two files from TheMuppets `sm8250-common` `lineage-23.2`
  (checked by SHA-256) into `vendor/samsung/r8q` and adds them as `prefer: true` modules.

Not ported: `mercury.json` (Samsung's skin temperature prediction; only `r8q`'s own firmware has it), eSE and keyboard
changes (tablets only).

## Build
Like any other device ([BUILDING.md](../../BUILDING.md)): `source build/envsetup.sh`, `breakfast r8q`, `brunch r8q`.
The install needs `recovery.img` and `vbmeta.img` from `out/target/product/r8q` next to the zip
([INSTALL-S20FE.md](../../INSTALL-S20FE.md)).

Note for a tree that also builds Pixels: `r8q`'s CPU variant (arm64 `armv8-2a-dotprod`, arm `armv8-2a`) differs from
theirs, so all native code is compiled again into separate directories (~50 GB more in `out/`, ccache doesn't help).
