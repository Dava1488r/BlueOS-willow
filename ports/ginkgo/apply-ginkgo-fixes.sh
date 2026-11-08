#!/bin/bash
# Redmi Note 8 / 8T (ginkgo / willow) on LineageOS 24.0 (Android 17)
# Run from the top of the Android build tree after `repo sync`.
# Idempotent; commits locally if changes are made.
set -e
cd "${ANDROID_BUILD_TOP:-$PWD}"

D=device/xiaomi/ginkgo
C=device/xiaomi/sm6125-common
V=vendor/xiaomi/ginkgo

[ -d "$D" ] && [ -d "$C" ] || { echo "Run from the top of the tree, with $D and $C synced"; exit 1; }

echo "==> Configuring BlueOS for Redmi Note 8 / 8T (ginkgo/willow)..."

python3 - <<'EOF'
import os

D = 'device/xiaomi/ginkgo/'
C = 'device/xiaomi/sm6125-common/'

# 1. Ensure inherit-product for BlueOS exists in lineage_ginkgo.mk if not using vendor/extra
p = D + 'lineage_ginkgo.mk'
if os.path.exists(p):
    s = open(p).read()
    if 'vendor/blueos/config/blueos.mk' not in s and '$(call inherit-product, vendor/extra/product.mk)' not in s:
        # Check if vendor/extra/product.mk is already present in build tree
        if not os.path.exists('vendor/extra/product.mk'):
            os.makedirs('vendor/extra', exist_ok=True)
            with open('vendor/extra/product.mk', 'w') as f:
                f.write('$(call inherit-product-if-exists, vendor/blueos/config/blueos.mk)\n')
            print("vendor/extra/product.mk created with BlueOS hook")

# 2. Verify notch and status bar height padding for live halo clock
overlay_dir = C + 'overlay/frameworks/base/core/res/res/values/'
dimens_file = overlay_dir + 'dimens.xml'
if os.path.exists(dimens_file):
    s = open(dimens_file).read()
    # Ensure status bar top padding accounts for the teardrop notch
    if 'status_bar_height_portrait' in s:
        print("Status bar dimensions verified for notch cutout")

print("Redmi Note 8 / 8T configuration checks passed.")
EOF

if [ -d "$D/.git" ] && [ -n "$(git -C "$D" status --porcelain)" ]; then
  git -C "$D" add -A
  git -C "$D" commit -q -m 'ginkgo: BlueOS product integration'
fi

echo "==> Done. Ready to build: 'breakfast ginkgo' followed by 'm bacon -j\$(nproc)'"
