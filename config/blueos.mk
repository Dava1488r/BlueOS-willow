#
# BlueOS - main product config.
# Loaded via vendor/extra/product.mk, i.e. before LineageOS's own config.
# Note: redefining a property LineageOS already sets is a build error (duplicate sysprop)
# unless LineageOS marks it optional (?=); see patches/vendor/lineage.
#

# Set in vendor/blueos/vendorsetup.sh
BLUEOS_VERSION ?= 0.1

PRODUCT_PRODUCT_PROPERTIES += \
    ro.blueos.version=$(BLUEOS_VERSION)

# Version string in About phone / zip name: see vendorsetup.sh

# Resource overlays: LineageOS -> BlueOS strings (tools/rebrand.py), wallpapers, defaults
PRODUCT_PACKAGE_OVERLAYS += vendor/blueos/overlay
PRODUCT_ENFORCE_RRO_EXCLUDED_OVERLAYS += vendor/blueos/overlay

# Boot animations: the default light one through LineageOS' own hook, the dark one and the
# release marker from Android.bp. (LineageOS Updater is removed by patches/vendor/lineage.)
TARGET_BOOTANIMATION := vendor/blueos/media/bootanimation/bootanimation.zip
PRODUCT_PACKAGES += \
    blueos_bootanimation_dark \
    blueos-release

# Everything below marked «optional» comes from the builder's own files (ASSETS.md): a public
# build has none of it and works without it — every copy takes only the files that are there.
# (Not Android.bp modules: a module with a missing source stops the whole build.)
blueos_copy = $(foreach f,$(notdir $(wildcard $(1)/$(2))),$(1)/$(f):$(TARGET_COPY_OUT_PRODUCT)/$(3)/$(f))

# Optional: the classic boot animations (tools/import-assets.py); bootanimation falls back to ours
PRODUCT_COPY_FILES += $(call blueos_copy,vendor/blueos/media/bootanimation,bootanimation-classic*.zip,media/blueos)

# Optional: sounds (tools/import-assets.py). Every file shows up in Settings > Sound pickers.
BLUEOS_AUDIO := vendor/blueos/media/audio
PRODUCT_COPY_FILES += \
    $(foreach d,notifications alarms ringtones ui,$(call blueos_copy,$(BLUEOS_AUDIO)/$(d),*.ogg,media/audio/$(d)))
# The default ringtone, notification and alarm stay LineageOS' own (the user, 2026-10-30);
# lock/unlock default to «Старт» (overlay SettingsProvider blueos_defaults.xml).

# Optional: the live characters (0.7): Spine models, voice lines, emotion SFX — one copy on /product,
# read by SystemUI (lock screen) and Launcher3 (widget) through vendor/blueos/libs/arona. Without a
# model the character is off everywhere (org.blueos.arona.Character.available).
BLUEOS_ARONA := vendor/blueos/media/arona
BLUEOS_PLANA := vendor/blueos/media/plana
PRODUCT_COPY_FILES += \
    $(call blueos_copy,$(BLUEOS_ARONA)/spine,arona_spr.*,media/blueos/spine/arona) \
    $(call blueos_copy,$(BLUEOS_ARONA)/voice,*.ogg,media/blueos/arona/voice) \
    $(call blueos_copy,$(BLUEOS_ARONA)/sfx,*.wav,media/blueos/arona/sfx) \
    $(call blueos_copy,$(BLUEOS_ARONA)/desk,arona_desk.*,media/blueos/spine/arona_desk) \
    $(call blueos_copy,$(BLUEOS_PLANA)/spine,NP0035_spr.*,media/blueos/spine/plana) \
    $(call blueos_copy,$(BLUEOS_PLANA)/voice,*.ogg,media/blueos/plana/voice)

# Optional: the gallery's built-in sticker for its photo widget
PRODUCT_COPY_FILES += $(call blueos_copy,vendor/blueos/media/stickers,*.png,media/blueos/stickers)

# System fonts: Settings > Display > Font (tools/make-fonts.py, fonts/Android.bp)
PRODUCT_PACKAGES += \
    MPLUSRounded1c-Regular.ttf \
    MPLUSRounded1c-Medium.ttf \
    MPLUSRounded1c-Bold.ttf \
    Nunito-VF.ttf \
    Comfortaa-VF.ttf \
    Manrope-VF.ttf \
    Outfit-VF.ttf \
    fonts_customization_mplusrounded1c.xml \
    fonts_customization_googlesansflex.xml \
    fonts_customization_nunito.xml \
    fonts_customization_comfortaa.xml \
    fonts_customization_manrope.xml \
    fonts_customization_rubik.xml
$(call inherit-product, external/google-fonts/rubik/fonts.mk)

# Runtime permissions granted by default (SystemUI: calendar for the lock screen card)
PRODUCT_COPY_FILES += \
    vendor/blueos/config/default-permissions-blueos.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/default-permissions/default-permissions-blueos.xml

# SELinux: boot animation and font settings
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += vendor/blueos/sepolicy/private

# Google Play / GMS (MindTheGapps, synced via .repo/local_manifests/gapps.xml)
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)
