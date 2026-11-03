# Sourced automatically by build/envsetup.sh.
# BlueOS version lives here: LineageOS reads TARGET_UNOFFICIAL_BUILD_ID before
# any product makefile of ours, so it has to come from the environment.
export BLUEOS_VERSION=0.8.1
export TARGET_UNOFFICIAL_BUILD_ID=BlueOS-$BLUEOS_VERSION

# Put BlueOS changes to LineageOS/AOSP code back in place (no-op if already applied)
bash vendor/blueos/scripts/apply-patches.sh
