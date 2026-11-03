#!/bin/bash
# Writes config/assets-files.txt: every optional file this BlueOS uses (path in vendor/blueos, size, sha256).
# Run it after import-assets.py changed media/ (it does so itself), commit the list. The phone's importer
# (Settings → «Файлы персонажа») and the public ASSETS-files.txt use it to find and check the files.
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."
VERSION=$(sed -n 's/^export BLUEOS_VERSION=//p' vendorsetup.sh)
{
    echo "# The optional files BlueOS $VERSION uses (ASSETS.md): path in vendor/blueos, size, sha256."
    echo "# Sounds are listed with their names after tools/import-assets.py (Emoticon_*, Theme_*, …)."
    find media/arona media/plana media/audio media/stickers -type f ! -name 'README*' | sort | while read -r f; do
        echo "$f  $(stat -c %s "$f")  $(sha256sum "$f" | cut -c1-64)"
    done
} > config/assets-files.txt
echo "config/assets-files.txt: $(grep -vc '^#' config/assets-files.txt) files"
