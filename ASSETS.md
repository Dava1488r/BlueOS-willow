# Character, sounds and wallpapers: your own files

**English** | [Русский](ASSETS.ru.md)

The BlueOS sources and ready-made builds have **no files from Blue Archive**: no character models, voices, sounds,
music or pictures from the game, which belong to Nexon / Yostar. BlueOS works fully without them; only what is made
from them is missing. You can add the files **right on the phone, without a build**. Where to get them:
[below](#where-to-get-the-files) and in the description of the author's TikTok profile,
[tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun). **Don't add these files to this repository, its forks or
Issues.**

## What you get without the files
| Without the files | With the files |
|---|---|
| A lock screen without a character (the clock, halo, greeting, weather and AOD are there) | Arona or Plana on the lock screen and on AOD; she reacts to touches, tilting and shaking and talks in her own voice |
| No «Character» widget; Wallpaper & style → Character has only «Character files» | A widget with her on the home screen, the desk, her lines, all the character settings |
| Ringtone, notifications, alarm, charging sounds: the stock LineageOS ones; lock sounds: «Start», «Fire» and «Android» | Music and sounds from the game (MomoTalk and others) in the sound lists, the other lock sounds |
| Wallpapers: the stock LineageOS ones | (in your own build) a «BlueOS» set in Wallpaper & style |
| The BlueOS boot animation (a halo + the name) | (in your own build) plus «classic» animations to choose from |

## Method 1: on the phone (ready-made ROM, no computer)
1. Put the files on the phone as one folder (or one `.zip` archive), **in any layout**, just as they are after
   unpacking: BlueOS looks for the files it needs by name in all subfolders.
2. Wallpaper & style → Character → **Character files** → «Pick a folder» (or «Pick a .zip archive»).
3. BlueOS shows what it found: «Arona: 3 of 3», «Arona's voice: 47 of 47», … and how many files differ from the
   checked ones (usually another version of the game). Tap **«Restart the phone»**: after the restart the character
   appears. The «Character» widget is in the home screen's widget list.

What the import finds:
- **models**: by the names `arona_spr.*` (Arona) and `NP0035_spr.*` (Plana); names as Unity unpacks them work too:
  `arona_spr.skel.bytes`, `arona_spr.atlas.txt`;
- **the desk**: `arona_desk.*`, and if it isn't there, the import **cuts it out by itself** from the office scene
  `arona_workpage_daytime.*` (`.atlas`, `.skel` and both page pictures);
- **voices**: `.ogg` files named as on the wiki (`Arona_Work_In_1.ogg` or `Work_In_1.ogg`). BlueOS tells whose voice
  it is by the prefix in the name or by the folder: its path must contain `arona`/`арона` or `plana`/`np0035`/`плана`;
- **emotion sounds** `SFX_Emoticon_Motion_*.wav`; **sounds and tunes** named as in [ASSETS-files.txt](ASSETS-files.txt)
  (ringtones, notifications and alarms go to the phone's shared `Ringtones`/`Notifications`/`Alarms` → `BlueOS`
  folders).

The character files are kept in `/data/misc/blueos` and survive BlueOS updates. To delete them, use «Remove added
files» on the same page. You can also add game sounds as ringtones/notifications by hand: put `.ogg` files into the
`Ringtones`, `Notifications` or `Alarms` folder in internal storage, and they appear in the sound lists.

## Method 2: in a build (bake them into your own ROM)
1. Run this from the top of the source tree (you need `python3-pil` and `ffmpeg`), pointing it at **any** folder with
   the files:
```
python3 vendor/blueos/tools/import-assets.py /path/to/files
```
   Without a `characters/` folder inside, it looks for the character files by name everywhere, like the phone does,
   and cuts the desk out of the office scene by itself. Sounds, wallpapers and boot animations are taken from folders
   with exact names (below), since they need converting.
2. Rebuild (`m bacon`). The first build after adding the files takes longer than usual: that's normal.
3. Check on the phone: `adb shell ls /product/media/blueos/spine/arona` shows the three model files.

Folders for sounds, wallpapers and animations (missing parts are skipped):
```
<folder>/
  notification/  clockAlarm/  "Unlock, lock"/  touch/   ← sounds (any format ffmpeg can read)
  boot/bootanimation/light/bootanimation.zip  boot/bootanimation/dark/bootanimation.zip   ← boot animations
  wallpaper/     *.jpg / *.png                ← your own wallpapers (the file «25.*» becomes the default wallpaper)
  stickers/      arona.png                    ← the sticker for Gallery's photo widget
  characters/arona/{spine,voice,sfx,desk}/  characters/plana/{spine,voice}/   ← or like this, if you prefer
```
The exact names of all files BlueOS uses, with sizes and checksums, are in [ASSETS-files.txt](ASSETS-files.txt).

## Where to get the files
**The easiest way:** an archive with the files, at the link in the description of the author's TikTok profile,
[tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun). Download it to the phone and pick it in «Character files»
(method 1). There are no files in this repository or in the releases.

**Yourself:** these are files of the game itself (Unity): the characters' Spine 4.2 models (`arona_spr`,
`NP0035_spr`), the office scene `Arona_workpage_daytime` (the desk comes from it), the characters' voices, interface
and emotion sounds, music themes. They are taken from **your own** installed copy of the game with tools that unpack
Unity assets (for example AssetStudio or AssetRipper): the Spine models are stored there as text assets
(`.skel.bytes`, `.atlas.txt`) and textures (`.png`). The voices are named as on the game's wiki. How your version of
the game stores the files changes with updates: if something isn't found, write in Issues what is where.

## Default sounds
By default the ringtone, notification, alarm, charging and touch sounds are the stock LineageOS ones, and the lock and
unlock sound is «Start» (it is in the sources). Added sounds appear in the lists in Settings → Sound
(ringtone, notification, alarm, «Charging sounds», «Screen lock sound», «Touch sound»), and you pick them there.
