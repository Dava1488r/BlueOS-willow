#!/bin/bash
# Saves BlueOS commits from the patched projects into vendor/blueos/patches.
# Workflow for a new change: edit files in e.g. packages/apps/Settings, commit there with a
# subject starting with "BlueOS:", then run this script and commit vendor/blueos.

TOP=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
PROJECTS="frameworks/base packages/apps/Settings packages/apps/LineageParts
    packages/apps/SetupWizard packages/apps/ThemePicker packages/apps/DeskClock packages/apps/Etar packages/apps/Glimpse packages/apps/Launcher3
    bootable/recovery vendor/lineage frameworks/libs/systemui"

for project in $PROJECTS; do
    out="$TOP/vendor/blueos/patches/$project"
    rm -rf "$out"
    # commits on top of the revision repo checked out (m/<branch> points at it)
    base=$(git -C "$TOP/$project" rev-parse --verify -q "m/lineage-24.0" || echo "")
    [ -n "$base" ] || { echo "no m/lineage-24.0 in $project"; continue; }
    if [ -n "$(git -C "$TOP/$project" rev-list "$base..HEAD" --grep='^BlueOS:')" ]; then
        mkdir -p "$out"
        git -C "$TOP/$project" format-patch -q --binary --zero-commit --no-signature -o "$out" "$base..HEAD"
        echo "$project: $(ls "$out" | wc -l) patch(es)"
    fi
done
