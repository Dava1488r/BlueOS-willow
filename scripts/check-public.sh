#!/bin/bash
# This tree is public as it is: run this before every commit (a git pre-commit hook is the easy way).
# It refuses a game file, a big file, a picture inside a patch or a private path — your own builds may have game files
# in media/ after tools/import-assets.py, and those must not end up in the repository.
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."
# Lock sounds that are not from the game: «Старт» and «Огонь»
OWN_SOUNDS="media/audio/ui/BlueOS_Lock_Start.ogg media/audio/ui/BlueOS_Unlock_Start.ogg media/audio/ui/BlueOS_Lock_Fire.ogg media/audio/ui/BlueOS_Unlock_Fire.ogg"
bad=0
game=$(find . -path ./.git -prune -o -type f \( -name '*.skel' -o -name '*.atlas' -o -name '*.wav' -o -name '*.ogg' \) -print \
    | sed 's|^\./||' | while read -r f; do case " $OWN_SOUNDS " in *" $f "*) ;; *) echo "$f" ;; esac; done)
if [ -n "$game" ]; then
    echo "CHECK: a game file is in the tree:"; echo "$game"; bad=1
fi
if find . -path ./.git -prune -o -type f -size +50M -print | grep -q .; then
    echo "CHECK: files over 50 MB:"; find . -path ./.git -prune -o -type f -size +50M -print; bad=1
fi
# binary files inside patches: only the recovery logo, the Moe digits and LineageOS' own deleted wallpapers
others=$(grep -l 'GIT binary patch' -r patches | grep -vE 'bootable/recovery/|vendor/lineage/0003-|frameworks/base/001[01]-BlueOS-SystemUI-Moe' || true)
if [ -n "$others" ]; then echo "CHECK: pictures/binaries inside these patches — look at them:"; echo "$others"; bad=1; fi
# Windows/WSL paths, plus your own ids (phone serial, other e-mails…) as extended regexes, one per line, in
# ~/.config/blueos/private-patterns — kept out of the repository so the check itself doesn't publish them
PRIVATE='/mnt/[a-z]/|[A-Z]:\\(Users|Documents)'
OWN=~/.config/blueos/private-patterns
[ -s $OWN ] && PRIVATE="$PRIVATE|$(grep -v '^#' $OWN | paste -sd'|')"
if grep -rIlE "$PRIVATE" --exclude-dir=.git --exclude=check-public.sh . | grep -q .; then
    echo "CHECK: private paths or ids:"; grep -rInE "$PRIVATE" --exclude-dir=.git --exclude=check-public.sh . | head; bad=1
fi
[ $bad = 0 ] || { echo "Fix the above first."; exit 1; }
echo "check-public: OK"
