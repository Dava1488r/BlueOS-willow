#!/usr/bin/env python3
"""Imports the optional BlueOS media (your own files, see ASSETS.md) into vendor/blueos.

usage: python3 vendor/blueos/tools/import-assets.py <assets folder>

Every part is optional — a missing folder is skipped and what is already imported stays:
  notification/, clockAlarm/, "Unlock, lock"/, touch/ -> media/audio/{notifications,alarms,ringtones,ui}/*.ogg
                                     (any format ffmpeg reads; Vorbis with a title tag)
  boot/bootanimation/{light,dark}/bootanimation.zip -> media/bootanimation/bootanimation-classic{,-dark}.zip
  wallpaper/*                     -> overlay for the Backgrounds app ("BlueOS" collection) + default wallpaper
  characters/arona/{spine,voice,sfx,desk}/, characters/plana/{spine,voice}/ -> media/{arona,plana}/... as they are
                                     (0.8.1: no characters/ folder → the files are looked for by name anywhere in it,
                                     the desk cut out of the work page scene if needed)
  stickers/*.png                  -> media/stickers/
Re-run it after adding or changing files in the assets folder; it rewrites its outputs.
"""
import os
import re
import shutil
import subprocess
import sys
import zipfile

from PIL import Image

if len(sys.argv) < 2:
    sys.exit(__doc__)
SRC = sys.argv[1]
TOP = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # vendor/blueos
MEDIA = os.path.join(TOP, "media")
AUDIO = os.path.join(MEDIA, "audio")
BG_RES = os.path.join(TOP, "overlay/packages/apps/Backgrounds/res")
FW_RES = os.path.join(TOP, "overlay/frameworks/base/core/res/res")
# LineageOS overlays default_wallpaper in all of these; for a 420 dpi phone Android picks
# drawable-xxhdpi over drawable-nodpi, so ours has to be in every one of them
FW_WALLPAPER_DIRS = ["drawable-nodpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi",
                     "drawable-xxxhdpi", "drawable-sw600dp-nodpi", "drawable-sw720dp-nodpi"]

DEFAULT_WALLPAPER = "25"

# source file stem -> (output name, title) where the generic rule isn't good enough
NAME_OVERRIDES = {
    "Aswer_edit": ("Answer_Edit", "Answer (edit)"),
    "SE_Guitar_1a": ("Guitar_1", "Guitar 1"),
    "SE_Guitar_a2": ("Guitar_2", "Guitar 2"),
    "SE_Guitar_03b": ("Guitar_3", "Guitar 3"),
    "SE_ClockAlarm_01": ("Clock_Alarm", "Clock Alarm"),
}

# lock/unlock packs: pack id -> (lock source, unlock source)
LOCK_PACKS = {
    "Fade": ("SE_Fade_Lock", "SE_Fade_Unlock"),
    "Door": ("SE_DoorClose", "SE_DoorOpen"),
    "Flick": ("SE_Flick_Lock", "SE_Flick_Unlock"),
    "Fire": ("Amazon_fire_lock", "Amazon_fire_Unlock"),
    "Start": ("RenderSnd", "StartSnd"),
}
TOUCH_SOUNDS = {"touch2": "BlueOS_Touch", "SE_Chess_01": "BlueOS_Touch_Chess"}


def stem(path):
    name = os.path.basename(path)
    while True:
        base, ext = os.path.splitext(name)
        if ext.lower() not in (".ogg", ".mp3", ".wav", ".wv"):
            return name
        name = base


def nice_name(s):
    if s in NAME_OVERRIDES:
        return NAME_OVERRIDES[s]
    n = re.sub(r"^SFX_Emoticon_Motion_", "Emoticon_", s)
    n = re.sub(r"^SE_", "", n)
    n = re.sub(r"_0*\d+$", "", n) if not n.startswith("Theme_") else n
    return n, n.replace("_", " ")


def to_ogg(src, dst, title):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", src, "-vn", "-map_metadata", "-1",
                    "-metadata", f"title={title}", "-c:a", "libvorbis", "-q:a", "6", dst], check=True)


def files_in(sub):
    d = os.path.join(SRC, sub)
    if not os.path.isdir(d):
        return []
    return sorted(os.path.join(d, f) for f in os.listdir(d) if os.path.isfile(os.path.join(d, f)))


def import_sounds():
    if not any(files_in(s) for s in ("notification", "clockAlarm", "Unlock, lock", "touch")):
        print("sounds: none (skipped)")
        return
    shutil.rmtree(AUDIO, ignore_errors=True)
    for f in files_in("notification"):
        out, title = nice_name(stem(f))
        to_ogg(f, os.path.join(AUDIO, "notifications", out + ".ogg"), title)
    for f in files_in("clockAlarm"):
        out, title = nice_name(stem(f))
        to_ogg(f, os.path.join(AUDIO, "alarms", out + ".ogg"), title)
        if out.startswith("Theme_"):  # the music themes double as ringtones
            to_ogg(f, os.path.join(AUDIO, "ringtones", out + ".ogg"), title)
    by_stem = {stem(f): f for f in files_in("Unlock, lock") + files_in("touch")}
    for pack, (lock, unlock) in LOCK_PACKS.items():
        if lock in by_stem and unlock in by_stem:
            to_ogg(by_stem[lock], os.path.join(AUDIO, "ui", f"BlueOS_Lock_{pack}.ogg"), f"{pack} lock")
            to_ogg(by_stem[unlock], os.path.join(AUDIO, "ui", f"BlueOS_Unlock_{pack}.ogg"), f"{pack} unlock")
    for s, out in TOUCH_SOUNDS.items():
        if s in by_stem:
            to_ogg(by_stem[s], os.path.join(AUDIO, "ui", out + ".ogg"), out)
    for d in sorted(os.listdir(AUDIO)):
        print(f"sounds/{d}: {len(os.listdir(os.path.join(AUDIO, d)))}")


def import_bootanimations():
    # The user's "classic" animations; the default ones are generated by make-bootanimation.py
    d = os.path.join(MEDIA, "bootanimation")
    os.makedirs(d, exist_ok=True)
    for theme, out in (("light", "bootanimation-classic.zip"),
                       ("dark", "bootanimation-classic-dark.zip")):
        src = os.path.join(SRC, "boot/bootanimation", theme, "bootanimation.zip")
        if not os.path.isfile(src):
            continue
        with zipfile.ZipFile(src) as z:  # Android only plays uncompressed (stored) zips
            assert all(i.compress_type == zipfile.ZIP_STORED for i in z.infolist()), src + " is compressed"
        shutil.copyfile(src, os.path.join(d, out))
        print("bootanimation:", out)


def cover_resize(im, box_w, box_h):
    s = max(box_w / im.width, box_h / im.height)
    return im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS) if s < 1 else im


def center_crop(im, w, h):
    s = max(w / im.width, h / im.height)
    im = im.resize((max(w, round(im.width * s)), max(h, round(im.height * s))), Image.LANCZOS)
    x, y = (im.width - w) // 2, (im.height - h) // 2
    return im.crop((x, y, x + w, y + h))


def import_wallpapers():
    srcs = files_in("wallpaper")
    if not srcs:
        print("wallpapers: none (skipped)")
        return
    drawable = os.path.join(BG_RES, "drawable-nodpi")
    shutil.rmtree(drawable, ignore_errors=True)
    os.makedirs(drawable)
    key = lambda f: (stem_noext(f) != DEFAULT_WALLPAPER, not stem_noext(f).isdigit(), stem_noext(f))
    ids = []
    for f in sorted(srcs, key=key):
        wid = "blueos_wp_" + re.sub(r"[^a-z0-9_]", "_", stem_noext(f).lower().replace("wallhaven-", ""))
        im = Image.open(f).convert("RGB")
        cover_resize(im, 1440, 3120).save(os.path.join(drawable, wid + ".jpg"), quality=90)
        center_crop(im, 432, 768).save(os.path.join(drawable, wid + "_small.jpg"), quality=85)
        ids.append(wid)
        if stem_noext(f) == DEFAULT_WALLPAPER or (DEFAULT_WALLPAPER not in map(stem_noext, srcs) and len(ids) == 1):
            first = os.path.join(FW_RES, FW_WALLPAPER_DIRS[0], "default_wallpaper.png")
            os.makedirs(os.path.dirname(first), exist_ok=True)
            im.save(first, optimize=True)
            for d in FW_WALLPAPER_DIRS[1:]:
                os.makedirs(os.path.join(FW_RES, d), exist_ok=True)
                shutil.copyfile(first, os.path.join(FW_RES, d, "default_wallpaper.png"))
    write_wallpaper_xml(ids)
    print("wallpapers:", len(ids), "default:", ids[0])


def stem_noext(f):
    return os.path.splitext(os.path.basename(f))[0]


def write_wallpaper_xml(ids):
    os.makedirs(os.path.join(BG_RES, "xml"), exist_ok=True)
    items = "\n".join(
        f'        <static-wallpaper id="{w}" src="@drawable/{w}" thumbnail="@drawable/{w}_small" />'
        for w in ids)
    with open(os.path.join(BG_RES, "xml/wallpapers.xml"), "w") as fh:
        fh.write(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by vendor/blueos/tools/import-assets.py, do not edit. -->
<!-- System wallpaper categories read by WallpaperPicker2 from the partner app (Backgrounds).
     priority 1 puts the BlueOS collection first; LineageOS' own set stays as "On-device". -->
<wallpapers>
    <category id="blueos" title="@string/blueos_wallpapers_title"
        thumbnail="@drawable/{ids[0]}_small" featured="{ids[0]}" priority="1">
{items}
    </category>
</wallpapers>
""")
    # Static overlays may only replace resources unless new ones are declared with <add-resource>
    added = "\n".join(f'    <add-resource type="drawable" name="{w}{s}" />' for w in ids for s in ("", "_small"))
    os.makedirs(os.path.join(BG_RES, "values"), exist_ok=True)
    with open(os.path.join(BG_RES, "values/blueos_wallpapers.xml"), "w") as fh:
        fh.write(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by vendor/blueos/tools/import-assets.py, do not edit. -->
<resources>
    <add-resource type="xml" name="wallpapers" />
    <add-resource type="string" name="blueos_wallpapers_title" />
{added}
    <string name="blueos_wallpapers_title" translatable="false">BlueOS</string>
</resources>
""")


def import_characters():
    # The game's files as they are (Spine 4.2 binary .skel + .atlas + .png, voices .ogg, SFX .wav): ASSETS.md
    for who, subs in (("arona", ("spine", "voice", "sfx", "desk")), ("plana", ("spine", "voice"))):
        for sub in subs:
            files = files_in(os.path.join("characters", who, sub))
            if not files:
                continue
            out = os.path.join(MEDIA, who, sub)
            shutil.rmtree(out, ignore_errors=True)
            os.makedirs(out)
            for f in files:
                shutil.copyfile(f, os.path.join(out, os.path.basename(f)))
            print(f"characters/{who}/{sub}: {len(files)}")
    stickers = [f for f in files_in("stickers") if f.lower().endswith(".png")]
    if stickers:
        out = os.path.join(MEDIA, "stickers")
        os.makedirs(out, exist_ok=True)
        for f in stickers:
            shutil.copyfile(f, os.path.join(out, os.path.basename(f)))
        print("stickers:", len(stickers))


LIST = os.path.join(TOP, "config", "assets-files.txt")
# words in a file's name or folders that tell whose voice it is (both have lines named the same)
OWNERS = {"arona": ("arona", "арона"), "plana": ("plana", "np0035", "плана")}
SCENE = "arona_workpage_daytime"


def norm(name):
    """Unity exports text assets as name.skel.bytes / name.atlas.txt."""
    s = name.lower()
    for suffix in (".bytes", ".txt"):
        if s.endswith(suffix) and "." in s[:-len(suffix)]:
            s = s[:-len(suffix)]
    return s


def scan_characters():
    """0.8.1: any folder, as unpacked from the game — the characters' files are found by name (config/assets-files.txt,
    the same rules as the phone's importer, Settings → «Файлы персонажа»). The desk is cut out of the work page scene
    when the cut files aren't there."""
    base = os.path.dirname(os.path.abspath(SRC))
    by_name = {}
    for root, _, names in os.walk(SRC):
        for n in names:
            by_name.setdefault(norm(n), []).append(os.path.join(root, n))
    targets = []
    for line in open(LIST, encoding="utf-8"):
        parts = line.split()
        if len(parts) >= 2 and not line.startswith("#") and re.match(r"media/(arona|plana)/(spine|voice|sfx|desk)/", parts[0]):
            targets.append((parts[0], int(parts[1])))
    voice_owners = {}
    for p, _ in targets:
        if "/voice/" in p:
            voice_owners.setdefault(os.path.basename(p).lower(), set()).add(p.split("/")[1])
    found = {}
    for p, size in targets:
        n = os.path.basename(p).lower()
        if "/voice/" in p:
            owner = p.split("/")[1]
            mine = OWNERS[owner]
            others = [w for o, ws in OWNERS.items() if o != owner for w in ws]
            shared = len(voice_owners.get(n, ())) > 1
            cands = [f for w in mine for f in by_name.get(f"{w}_{n}", [])]
            for f in by_name.get(n, []):
                where = os.path.relpath(os.path.dirname(f), base).lower()
                # a line both of them have, in a folder that doesn't tell, is hers if it's exactly her file's size
                if any(w in where for w in mine) or (not any(w in where for w in others)
                                                     and (not shared or os.path.getsize(f) == size)):
                    cands.append(f)
        else:
            cands = by_name.get(n, [])
        if cands:
            found[p] = next((f for f in cands if os.path.getsize(f) == size), cands[0])
    groups = {}
    for p, f in found.items():
        out = os.path.join(TOP, p)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        shutil.copyfile(f, out)
        groups[os.path.dirname(p)] = groups.get(os.path.dirname(p), 0) + 1
    for g in sorted(groups):
        print(f"{g}: {groups[g]} of {sum(1 for p, _ in targets if os.path.dirname(p) == g)}")
    scene = [f for k, fs in by_name.items() if k.startswith(SCENE) for f in fs]
    if not any(p.startswith("media/arona/desk/") for p in found) and scene:
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            for f in scene:
                shutil.copyfile(f, os.path.join(tmp, norm(os.path.basename(f))))
            subprocess.run([sys.executable, os.path.join(TOP, "tools", "cut-workpage.py"), tmp,
                            os.path.join(MEDIA, "arona", "desk")], check=True)
        print("media/arona/desk: cut out of the work page scene")
    if not found and not scene:
        print("No character files found (arona_spr.skel, NP0035_spr.skel, … — config/assets-files.txt).")


if __name__ == "__main__":
    import_sounds()
    import_bootanimations()
    import_wallpapers()
    # the folder layout of ASSETS.md, or any folder as unpacked from the game
    if os.path.isdir(os.path.join(SRC, "characters")):
        import_characters()
    else:
        scan_characters()
