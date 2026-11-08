# BlueOS

**English** | [Русский](README.ru.md)

A fan-made Android ROM based on **LineageOS 24.0 (Android 17)** and inspired by **Blue Archive**. It has:

- its own logo and boot animation;
- a live halo on the lock-screen clock;
- live widgets;
- «Colors», where every detail takes its colour from the wallpaper according to its role;
- live weather and a music equalizer drawn right on the wallpaper;
- a live character on the lock screen and in a widget: Arona or Plana (you need your own files, see below).

BlueOS is developed on the **Google Pixel 6a (`bluejay`)**. Ready-made builds also exist for the **Pixel 6**,
**Pixel 6 Pro** and **Galaxy S20 FE (Snapdragon)**, and testers have checked them. For other phones you can build
BlueOS yourself (that is why these sources are public) or try the experimental GSI ([GSI.md](GSI.md)).

📣 **News and new versions: the Telegram channel [t.me/BlueOSfirmware](https://t.me/BlueOSfirmware).**

> BlueOS is an unofficial fan project. It is not affiliated with Nexon, Yostar or NEXON Games. **There are no game
> files here**: the sources do not include the characters' models, their voices, or the game's music, sounds and
> wallpapers. The ROM works fully without them, just without the character and those sounds. You can add the game
> files **right on the phone**, no build needed: Wallpaper & style → Character → Character files
> ([ASSETS.md](ASSETS.md)). Where to get them: the link in the description of the author's TikTok profile,
> [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).

> ⚠️ **No warranty.** Installing any custom ROM wipes the phone and needs an unlocked bootloader. Some apps may not
> work (tap to pay, some banking apps). Use it at your own risk. Details: [INSTALL.md](INSTALL.md), section
> «Read before you start».

## What it looks like
<table>
<tr>
<td align="center" width="25%"><img src="docs/screenshots/lock-plana.jpg" alt="Plana and the live clock*"><br><sub>Plana and the live clock*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-widgets.jpg" alt="Widgets and the equalizer"><br><sub>Widgets and the equalizer</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/lock-arona.jpg" alt="Arona on the lock screen*"><br><sub>Arona on the lock screen*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-plana.jpg" alt="Plana in the widget*"><br><sub>Plana in the widget*</sub></td>
</tr>
<tr>
<td align="center" width="25%"><img src="docs/screenshots/lock-moe.jpg" alt="Moe pixel clock*"><br><sub>Moe pixel clock*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-arona.jpg" alt="Arona at her desk*"><br><sub>Arona at her desk*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/about.jpg" alt="About phone"><br><sub>About phone</sub></td>
</tr>
</table>

<sub>* The characters and their voices appear only after you add your own files. They are not part of the ROM, and
neither are the wallpapers in these screenshots (except the stock LineageOS ones). Taken on a Pixel 6a with the
interface in Russian; BlueOS is in English too.</sub>

## Where to start
| What you need | Where |
|---|---|
| **Install ready-made BlueOS on a Pixel 6a** | **Releases** + [INSTALL.md](INSTALL.md) |
| **Install on a Pixel 6** | **Releases** (`v0.8.1-oriole`) + [INSTALL-Pixel6.md](INSTALL-Pixel6.md) |
| **Install on a Pixel 6 Pro** | **Releases** (`v0.8.1-raven`) + [INSTALL-Pixel6Pro.md](INSTALL-Pixel6Pro.md) |
| **Install on a Galaxy S20 FE (Snapdragon)** | **Releases** (`v0.8.1-r8q`) + [INSTALL-S20FE.md](INSTALL-S20FE.md) |
| **Install on a Pixel 7** | **Releases** (`v0.8.1-panther`) + [INSTALL-Pixel7.md](INSTALL-Pixel7.md) |
| **Install on a Pixel 7 Pro** | **Releases** (`v0.8.1-cheetah`) + [INSTALL-Pixel7Pro.md](INSTALL-Pixel7Pro.md) |
| **Install on a Pixel 7a** | **Releases** (`v0.8.1-lynx`) + [INSTALL-Pixel7a.md](INSTALL-Pixel7a.md) |
| **Install on a Redmi Note 8 / 8T** | **Releases** (`v0.8.1-willow`) + [INSTALL-RedmiNote8T.md](INSTALL-RedmiNote8T.md) |
| **Try it on another phone** (GSI, experimental, checked only on a Pixel 6a) | **Releases** + [GSI.md](GSI.md) |
| Build BlueOS for your own phone | [BUILDING.md](BUILDING.md) |
| Your phone isn't listed / what to adjust for another screen | [PORTING.md](PORTING.md) |
| Bring back the character and sounds (on the phone, no build) | [ASSETS.md](ASSETS.md) |
| Build with an AI coding agent (Claude Code and others) | [AGENTS.md](AGENTS.md) |

Every guide is in English and in Russian: the Russian version sits next to it as `*.ru.md`, and each page has a
language switch at the top.

**The easiest way is with an agent.** Open the LineageOS source folder in Claude Code (or another agent that reads
`AGENTS.md`) and say "build BlueOS for my phone". `AGENTS.md` tells the agent what to check, in what order to do
things and where builds usually break.

## What's inside
- `config/blueos.mk` — everything that hooks BlueOS into the build (one line in `vendor/extra/product.mk`).
- `patches/<project>/*.patch` — changes to LineageOS/AOSP code: SystemUI, the launcher, Wallpaper & style,
  Settings, Clock, Calendar, Gallery, recovery. They apply by themselves on `source build/envsetup.sh`.
- `libs/arona` — the shared library: BlueOS settings, colour roles, phone motion, weather, characters.
- `overlay/` — resources over the stock ones (names, default values).
- `fonts/`, `media/` (logo, boot animations), `sepolicy/`.
- `tools/` — the scripts that made the logo, the animations, the fonts and the Moe clocks.

## Licences
- **If you take something from BlueOS, credit the author.** BlueOS' own code and media are © 2026 GDmio: the
  patches, `libs`, `tools`, `config`, `overlay`, the logo, the boot animations and its own sounds. They are under the
  Apache License 2.0 with one added condition ([LICENSE](LICENSE)).
  - The condition applies when you distribute BlueOS or work based on it: a ROM, GSI, port or fork, or code, patches
    or media from here.
  - Keep the [NOTICE](NOTICE) file.
  - Say in your project's description, release notes or README that it is based on BlueOS by GDmio, with a link to
    https://github.com/GDmio/BlueOS .
  - Distribution without that is not licensed, and the author may ask for it to be taken down.
  - Installing and using BlueOS on your own phone needs nothing at all.
- The LineageOS/AOSP code that the patches change keeps its own licence (Apache License 2.0).
- The fonts in `fonts/` are under the SIL Open Font License (`OFL.txt` files next to them).
- The Moe clock digits come from [Moe-Counter](https://github.com/journey-ad/Moe-Counter), MIT
  (`media/moe/src/LICENSE-Moe-Counter`).
- `prebuilts/spine` — [Spine Runtimes](https://esotericsoftware.com/spine-runtimes-license) by Esoteric Software.
  Under their licence everyone who uses the Spine runtime needs their own Spine Editor licence. More in
  [NOTICE.md](NOTICE.md).
