# BlueOS

[English](README.md) | **Русский**

Фанатская прошивка на основе **LineageOS 24.0 (Android 17)**, вдохновлённая **Blue Archive**. В ней есть:

- свой логотип и анимация загрузки;
- живой нимб на часах экрана блокировки;
- живые виджеты;
- «Цвета», где каждая деталь берёт цвет из обоев по своей роли;
- живая погода и эквалайзер прямо на обоях;
- живой персонаж на экране блокировки и в виджете: Арона или Плана (нужны свои файлы, см. ниже).

BlueOS разрабатывается на **Google Pixel 6a (`bluejay`)**. Готовые сборки есть ещё для **Pixel 6**, **Pixel 6 Pro**
и **Galaxy S20 FE на Snapdragon**, их проверили тестеры. Под другие телефоны BlueOS можно собрать самому (для этого
исходники и выложены) или попробовать экспериментальный GSI ([GSI.ru.md](GSI.ru.md)).

📣 **Новости и новые версии: Telegram-канал [t.me/BlueOSfirmware](https://t.me/BlueOSfirmware).**

> BlueOS — неофициальный фанатский проект. Он не связан с Nexon, Yostar и NEXON Games. **Файлов из игры здесь нет**:
> в исходники не входят модели персонажей, их голоса, музыка, звуки и обои из игры. Без них прошивка полностью
> работает, просто без персонажа и этих звуков. Файлы игры можно добавить **прямо на телефоне**, без сборки:
> «Обои и стиль» → «Персонаж» → «Файлы персонажа» ([ASSETS.ru.md](ASSETS.ru.md)). Где их взять: ссылка в описании профиля
> автора в TikTok, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).

> ⚠️ **Без гарантий.** Установка любой прошивки стирает телефон и требует разблокированного загрузчика. Часть
> приложений может не работать (оплата телефоном, некоторые банки). Всё на ваш страх и риск. Подробно:
> [INSTALL.ru.md](INSTALL.ru.md), раздел «Прочитайте до начала».

## Как это выглядит
<table>
<tr>
<td align="center" width="25%"><img src="docs/screenshots/lock-plana.jpg" alt="Плана и живые часы*"><br><sub>Плана и живые часы*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-widgets.jpg" alt="Виджеты и эквалайзер"><br><sub>Виджеты и эквалайзер</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/lock-arona.jpg" alt="Арона на экране блокировки*"><br><sub>Арона на экране блокировки*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-plana.jpg" alt="Плана в виджете*"><br><sub>Плана в виджете*</sub></td>
</tr>
<tr>
<td align="center" width="25%"><img src="docs/screenshots/lock-moe.jpg" alt="Пиксельные часы Moe*"><br><sub>Пиксельные часы Moe*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/home-arona.jpg" alt="Арона за партой*"><br><sub>Арона за партой*</sub></td>
<td align="center" width="25%"><img src="docs/screenshots/about.jpg" alt="О телефоне"><br><sub>О телефоне</sub></td>
</tr>
</table>

<sub>* Персонажи и их голоса появляются, только если добавить свои файлы. В прошивку они не входят, как и обои с этих
скриншотов (кроме стандартных LineageOS). Снято на Pixel 6a.</sub>

## С чего начать
| Что нужно | Где |
|---|---|
| **Поставить готовую BlueOS на Pixel 6a** | **Releases** + [INSTALL.ru.md](INSTALL.ru.md) |
| **Поставить на Pixel 6** | **Releases** (`v0.8.1-oriole`) + [INSTALL-Pixel6.ru.md](INSTALL-Pixel6.ru.md) |
| **Поставить на Pixel 6 Pro** | **Releases** (`v0.8.1-raven`) + [INSTALL-Pixel6Pro.ru.md](INSTALL-Pixel6Pro.ru.md) |
| **Поставить на Galaxy S20 FE (Snapdragon)** | **Releases** (`v0.8.1-r8q`) + [INSTALL-S20FE.ru.md](INSTALL-S20FE.ru.md) |
| **Попробовать на другом телефоне** (GSI, экспериментально, проверен только на Pixel 6a) | **Releases** + [GSI.ru.md](GSI.ru.md) |
| Собрать BlueOS под свой телефон | [BUILDING.ru.md](BUILDING.ru.md) |
| Телефона нет в списке / что подстроить под другой экран | [PORTING.ru.md](PORTING.ru.md) |
| Вернуть персонажа и звуки (на телефоне, без сборки) | [ASSETS.ru.md](ASSETS.ru.md) |
| Собирать с помощью ИИ-агента (Claude Code и др.) | [AGENTS.md](AGENTS.md) |

**Проще всего с агентом.** Откройте папку с исходниками LineageOS в Claude Code (или другом агенте, который читает
`AGENTS.md`) и скажите: «собери BlueOS для моего телефона». В `AGENTS.md` описано, что агенту проверить, в каком
порядке всё делать и где сборка обычно ломается.

## Что внутри
- `config/blueos.mk` — всё подключение BlueOS к сборке (одна строка в `vendor/extra/product.mk`).
- `patches/<проект>/*.patch` — изменения кода LineageOS/AOSP: SystemUI, лаунчер, «Обои и стиль», Настройки,
  Часы, Календарь, Галерея, recovery. Ставятся сами при `source build/envsetup.sh`.
- `libs/arona` — общая библиотека: настройки BlueOS, роли цветов, движение телефона, погода, персонажи.
- `overlay/` — ресурсы поверх стоковых (названия, значения по умолчанию).
- `fonts/`, `media/` (логотип, анимации загрузки), `sepolicy/`.
- `tools/` — скрипты, которыми сделаны логотип, анимации, шрифты и часы Moe.

## Лицензии
- **Если берёте что-то из BlueOS, укажите автора.** Код и материалы BlueOS — © 2026 GDmio: патчи, `libs`, `tools`,
  `config`, `overlay`, логотип, анимации загрузки и свои звуки. Они распространяются под Apache License 2.0 с одним
  дополнительным условием ([LICENSE](LICENSE)).
  - Условие действует, когда вы распространяете BlueOS или работу на его основе: прошивку, GSI, порт, форк, код,
    патчи или материалы отсюда.
  - Сохраните файл [NOTICE](NOTICE).
  - Напишите в описании, примечаниях к релизу или README своего проекта, что он основан на BlueOS от GDmio, со
    ссылкой на https://github.com/GDmio/BlueOS .
  - Без этого распространение не разрешено, и автор может потребовать его удалить.
  - Ставить BlueOS на свой телефон и пользоваться им можно без всяких условий.
- Код LineageOS/AOSP, который меняют патчи, остаётся под своей лицензией (Apache License 2.0).
- Шрифты в `fonts/` — SIL Open Font License (файлы `OFL.txt` рядом).
- Цифры часов Moe — проект [Moe-Counter](https://github.com/journey-ad/Moe-Counter), MIT
  (`media/moe/src/LICENSE-Moe-Counter`).
- `prebuilts/spine` — [Spine Runtimes](https://esotericsoftware.com/spine-runtimes-license) (Esoteric Software).
  По их лицензии каждому, кто использует рантайм Spine, нужна своя лицензия Spine Editor. Подробнее в
  [NOTICE.md](NOTICE.md).
