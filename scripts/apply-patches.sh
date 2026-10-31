#!/bin/bash
# Applies vendor/blueos/patches/<project path>/*.patch to the source tree.
# Safe to run any number of times: a patch whose subject is already in the project's recent
# history is skipped. Run automatically from vendorsetup.sh (source build/envsetup.sh);
# after `repo sync` it puts the BlueOS changes back.

TOP=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
PATCHES="$TOP/vendor/blueos/patches"
rc=0

while IFS= read -r dir; do
    project=${dir#"$PATCHES"/}
    for patch in "$dir"/*.patch; do
        # mailinfo unfolds long Subject: headers and drops the "[PATCH n/m]" prefix
        subject=$(git mailinfo /dev/null /dev/null < "$patch" | sed -n 's/^Subject: //p')
        if git -C "$TOP/$project" log -n 200 --format=%s | grep -qxF "$subject"; then
            continue
        fi
        if git -C "$TOP/$project" -c user.name=BlueOS -c user.email=blueos@localhost \
                am -q --3way "$patch"; then
            echo "BlueOS: applied $project: $subject"
        else
            git -C "$TOP/$project" am --abort 2>/dev/null
            echo "BlueOS: FAILED to apply $patch" >&2
            rc=1
        fi
    done
done < <(find "$PATCHES" -name '*.patch' -printf '%h\n' | sort -u)

exit $rc
