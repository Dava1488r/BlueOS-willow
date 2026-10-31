#!/bin/bash
# Makes the public BlueOS sources: vendor/blueos without the game's files (ASSETS.md) and the private notes,
# in a git repository of its own (its own history — this repository's history has those files in it).
#
#   bash vendor/blueos/scripts/make-public.sh [out dir, default ~/blueos-public]
#
# Run it again for every release: it syncs the out dir and commits the changes there. It refuses to commit if the
# checks at the end find a game file, a big file or a private path.
set -e
SRC=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DST=${1:-$HOME/blueos-public}
VERSION=$(sed -n 's/^export BLUEOS_VERSION=//p' "$SRC/vendorsetup.sh")
mkdir -p "$DST"

# What is not ours stays out (it goes back in from the builder's own files: tools/import-assets.py)
rsync -a --delete --exclude=/.git \
    --exclude=/public/ --exclude=/README.md --exclude=__pycache__/ \
    --exclude=/media/arona/ --exclude=/media/plana/ --exclude=/media/audio/ --exclude=/media/stickers/ \
    --exclude='/media/bootanimation/bootanimation-classic*.zip' --exclude=/media/bootanimation/preview.mp4 \
    --exclude=/overlay/packages/apps/Backgrounds/ \
    --exclude='/overlay/frameworks/base/core/res/res/drawable-*/default_wallpaper.png' \
    "$SRC/" "$DST/"
cp -a "$SRC/public/." "$DST/"
cp /usr/share/common-licenses/Apache-2.0 "$DST/LICENSE"
# Lock sounds that are not from the game: «Старт» and «Огонь» stay in the public tree
OWN_SOUNDS="media/audio/ui/BlueOS_Lock_Start.ogg media/audio/ui/BlueOS_Unlock_Start.ogg media/audio/ui/BlueOS_Lock_Fire.ogg media/audio/ui/BlueOS_Unlock_Fire.ogg"
mkdir -p "$DST/media/audio/ui"
for f in $OWN_SOUNDS; do cp -a "$SRC/$f" "$DST/$f"; done

# The empty slots, each with a pointer to ASSETS.md
for d in media/arona/spine media/arona/voice media/arona/sfx media/arona/desk media/plana/spine media/plana/voice \
         media/audio/notifications media/audio/alarms media/audio/ringtones media/audio/ui media/stickers; do
    mkdir -p "$DST/$d"
    echo "Your own files go here — see ASSETS.md (tools/import-assets.py puts them in place)." > "$DST/$d/README.md"
done

# Names (and checksums) of every optional file this BlueOS uses, so a builder can tell they have the right ones
{
    echo "# The optional files BlueOS $VERSION uses (ASSETS.md): path in vendor/blueos, size, sha256."
    echo "# Sounds are listed with their names after tools/import-assets.py (Emoticon_*, Theme_*, …)."
    cd "$SRC"
    find media/arona media/plana media/audio media/stickers -type f | sort | while read -r f; do
        case " $OWN_SOUNDS " in *" $f "*) continue ;; esac
        echo "$f  $(stat -c %s "$f")  $(sha256sum "$f" | cut -c1-64)"
    done
} > "$DST/ASSETS-files.txt"

# Checks: nothing of the game, nothing big, nothing private
cd "$DST"
bad=0
game=$(find . -path ./.git -prune -o -type f \( -name '*.skel' -o -name '*.atlas' -o -name '*.wav' -o -name '*.ogg' \) -print \
    | sed 's|^\./||' | while read -r f; do case " $OWN_SOUNDS " in *" $f "*) ;; *) echo "$f" ;; esac; done)
if [ -n "$game" ]; then
    echo "CHECK: a game file is in the public tree:"; echo "$game"; bad=1
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
if grep -rIlE "$PRIVATE" --exclude-dir=.git --exclude=make-public.sh . | grep -q .; then
    echo "CHECK: private paths or ids:"; grep -rInE "$PRIVATE" --exclude-dir=.git --exclude=make-public.sh . | head; bad=1
fi
[ $bad = 0 ] || { echo "Not committed: fix the above first."; exit 1; }

[ -d .git ] || git init -q -b main
git add -A
if git diff --cached --quiet; then
    echo "No changes."
else
    git commit -q -m "BlueOS $VERSION"
    echo "Committed: $(git log --oneline -1)"
fi
du -sh --exclude=.git . | sed 's/\t.*/ public tree/'
