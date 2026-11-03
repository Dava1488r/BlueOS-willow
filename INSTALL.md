# Установка готовой BlueOS на Google Pixel 6a

Готовая сборка есть только для **Pixel 6a** (кодовое имя `bluejay`) — она на странице **Releases** этого репозитория.
На другой телефон её ставить нельзя: не загрузится. Для других телефонов BlueOS собирают сами — [BUILDING.md](BUILDING.md).

## ⚠️ Прочитайте до начала
- **Неофициальная фанатская прошивка, без каких-либо гарантий.** Всё, что вы делаете с телефоном, — на ваш страх и
  риск. Это не сборка LineageOS: об ошибках BlueOS не пишите в LineageOS, пишите сюда (Issues).
- **Все данные на телефоне сотрутся** — при разблокировке загрузчика и ещё раз при установке. Сохраните фото,
  переписки, **коды двухфакторной защиты** (Google Authenticator и т. п.) заранее.
- **Загрузчик останется разблокированным.** При каждом включении телефон покажет предупреждение об этом — так и
  должно быть. **Не блокируйте загрузчик, пока стоит BlueOS** (`fastboot flashing lock`): телефон перестанет
  загружаться. Заблокировать можно только после возврата на стоковую прошивку (ниже).
- **Некоторые приложения могут не работать:** оплата телефоном (Google Wallet), часть банковских приложений и игр
  проверяют, что прошивка «заводская» (Play Integrity), и с BlueOS откажутся работать. Проверьте, нужны ли вам они.
- **Персонажа и звуков из Blue Archive в этой сборке нет.** Всё остальное работает. Файлы игры можно добавить
  прямо на телефоне: «Обои и стиль» → «Персонаж» → «Добавить файлы персонажа» ([ASSETS.md](ASSETS.md)). Где их взять:
  ссылка в описании профиля автора в TikTok — [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play внутри** (MindTheGapps).
- **Обновления** ставятся без сброса, но только сборки с этой страницы: они подписаны одним ключом. Сборка, которую
  вы сделали сами, подписана вашим ключом — поверх неё эти обновления не встанут (и наоборот) без сброса данных.
- Телефон, купленный у оператора с блокировкой, загрузчик не разблокирует — тогда установить нельзя.

## Что скачать
| Файл | Зачем |
|---|---|
| `BlueOS-0.8.1-bluejay-20261103.zip` | сама прошивка |
| `boot.img`, `dtbo.img`, `vendor_boot.img` | для первой установки: загрузочный раздел и recovery BlueOS |
| `SHA256SUMS` | контрольные суммы — проверить, что файлы скачались целиком |

Проверка: Windows (PowerShell) — `Get-FileHash .\BlueOS-0.8.1-bluejay-20261103.zip`, Linux/macOS — `sha256sum -c SHA256SUMS`.
Суммы должны совпасть. Не совпали — скачайте заново, не прошивайте.

## Перед началом
- Обновите стоковую прошивку до **Android 17, обновление октября 2026 или новее** («Настройки» → «Система» →
  «Обновление системы»). На старой прошивке BlueOS может не загрузиться.
- Компьютер, кабель USB и [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
  (`adb` и `fastboot`). На Windows ещё [Google USB Driver](https://developer.android.com/studio/run/win-usb).
- Зарядите телефон хотя бы до 50 %.

## 1. Разблокировать загрузчик (один раз)
1. «Настройки» → «О телефоне» → 7 раз нажать «Номер сборки» — включится режим разработчика.
2. «Настройки» → «Система» → «Для разработчиков»: включить **«Заводская разблокировка» (OEM unlocking)** и
   **«Отладка по USB»**.
3. Подключить к компьютеру, разрешить отладку на телефоне. В папке platform-tools:
```
adb reboot bootloader
fastboot flashing unlock
```
4. На телефоне кнопками громкости выбрать «Unlock the bootloader», подтвердить кнопкой питания. Телефон сотрётся.
   После загрузки снова включите «Отладку по USB» (шаг 2).

## 2. Поставить recovery BlueOS
```
adb reboot bootloader
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot flash vendor_boot vendor_boot.img
```
Затем кнопками громкости выбрать **Recovery mode** и нажать питание — откроется recovery BlueOS (логотип с нимбом).

## 3. Установить BlueOS
1. В recovery: **Factory reset → Format data / factory reset** → подтвердить. (Обязательно при переходе со стока
   или другой прошивки.)
2. Назад → **Apply update → Apply from ADB**.
3. На компьютере:
```
adb sideload BlueOS-0.8.1-bluejay-20261103.zip
```
   Дойдёт до 47 % или 100 % и напишет «Total xfer» — это нормально. На телефоне должно быть «Install completed».
4. **Reboot system now.** Первая загрузка — несколько минут.

## Обновления
Новые версии BlueOS ставятся **без сброса**: recovery → Apply update → Apply from ADB → `adb sideload <новый zip>`
(или `adb reboot sideload` из системы с включённой отладкой по USB). Данные остаются. Откатиться на более старую
версию так нельзя — только со сбросом.

## Если что-то пошло не так
- Не загружается дальше логотипа больше 10 минут — зайдите в recovery (питание + громкость вниз, затем Recovery mode),
  сделайте Format data и поставьте zip ещё раз.
- **Вернуть стоковую прошивку:** [flash.android.com](https://flash.android.com) (Android Flash Tool, Chrome/Edge) —
  он прошьёт заводскую систему и, если хотите, снова заблокирует загрузчик. Данные сотрутся.
- Пропало размытие в шторке сразу после установки или на зарядке — телефон нагрелся, Android сам отключает размытие;
  остынет — вернётся.
- Нашли ошибку — откройте Issue: что делали, что увидели, лучше со скриншотом.

---

## English (short)
Ready-made builds exist **only for the Pixel 6a (`bluejay`)** — see **Releases**. Unofficial fan ROM, **no warranty,
use at your own risk**; not a LineageOS build — don't report its bugs to LineageOS. Unlocking the bootloader and
installing **wipe the phone**. **Never relock the bootloader while BlueOS is installed** (the phone won't boot); go back
to stock with flash.android.com first. Tap-to-pay and some banking apps/games may refuse to work (Play Integrity).
No Blue Archive files are included (no character, no game sounds); to add them on the phone see [ASSETS.md](ASSETS.md),
where to get them: the link in the author's TikTok profile description, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun). Google Play is included (MindTheGapps).
Updates install without a wipe only from builds on this page (same signing key).

Steps: update stock to Android 17 (October 2026 or newer) → `fastboot flashing unlock` → `fastboot flash boot boot.img`,
`dtbo dtbo.img`, `vendor_boot vendor_boot.img` → Recovery mode → Factory reset → Format data → Apply update → Apply
from ADB → `adb sideload BlueOS-0.8.1-bluejay-20261103.zip` → Reboot. Check files against `SHA256SUMS` first.
