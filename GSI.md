# BlueOS GSI: для других телефонов (экспериментально)

GSI — это один образ системы, который можно запустить на многих телефонах с Android: он заменяет только саму систему,
а всё, что относится к железу, остаётся от прошивки телефона. Файл лежит на странице **Releases**, в релизе
`v0.8.1-gsi-treble`.

Образ собран на **TrebleDroid**: это набор исправлений для GSI, с которыми система лучше загружается и работает на
телефонах разных марок. Прошлый образ (`v0.8.1-gsi`) был без них, и у тестеров он зависал на загрузке, не работали камера
и отпечаток.

## ⚠️ Прочитайте до начала
- **Проверено только на одном телефоне:** Pixel 6a, запуск через DSU (способ 1). Там работают загрузка, Wi-Fi, мобильная
  сеть, звук, камера, отпечаток, Google Play и добавление файлов персонажа. На других телефонах он может не загрузиться
  или работать частично.
- **Неофициальная фанатская прошивка, без каких-либо гарантий.** Всё — на ваш страх и риск. Об ошибках пишите сюда
  (Issues), а не в LineageOS или TrebleDroid.
- **Заранее найдите заводскую прошивку именно для своего телефона и узнайте, как её поставить.** Только так можно будет
  вернуться назад.
- **Способ 2 сотрёт все данные.** Разблокировка загрузчика, если он ещё заблокирован, тоже стирает телефон.
- **Обновляться GSI будет редко**, только с крупными версиями BlueOS. Пункт «Обновление системы» в GSI об обновлениях
  BlueOS не знает: следите за страницей Releases.

## Подойдёт ли телефон
Нужны разблокируемый загрузчик и прошивка на **Android 13 или новее**. Включите отладку по USB и проверьте:
```
adb shell getprop ro.treble.enabled
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.boot.dynamic_partitions
adb shell getprop ro.boot.flash.locked
```
Должно быть `true`, `arm64-v8a`, `true`. Если ответ другой, этот образ не подойдёт. Последняя строка показывает загрузчик:
`0` — разблокирован, `1` — заблокирован. С заблокированным загрузчиком образ не запустится ни одним из способов ниже.

## Если что-то не работает
В настройках есть раздел **TrebleDroid Settings** («Настройки» → «Система»). В нём включаются исправления для отдельных
марок телефонов, например для звука, яркости или отпечатка. Если какой-то пункт помог, напишите об этом в Issues.

## Что может не работать
- **На телефонах, прошивка которых сильно отличается от чистого Android** (часто это Samsung, Xiaomi, телефоны на MediaTek),
  всё ещё могут ломаться звук, камера, отпечаток, яркость или звонки.
- **Размытие и стекло** зависят от прошивки телефона: на некоторых их не будет.
- **Погода.** На Pixel 6a в GSI Google не выдавал местоположение. Если виджет пишет «Нет данных о погоде», укажите город:
  «Обои и стиль» → «Погода» → «Город».
- **Виджеты и сетка** подобраны под экран Pixel 6a, на других экранах могут выглядеть хуже.
- **Оплата телефоном, часть банков и игр** могут отказаться работать (Play Integrity).
- **Ядро, recovery и логотип при включении** остаются от вашей прошивки: GSI их не меняет.

Google Play внутри (MindTheGapps). Персонажей и звуков из Blue Archive в образе нет, файлы игры можно добавить прямо на
телефоне: «Обои и стиль» → «Персонаж» → «Добавить файлы персонажа» ([ASSETS.md](ASSETS.md)). Где их взять: ссылка в
описании профиля автора в TikTok — [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).

## Что скачать
| Файл | Зачем |
|---|---|
| `BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz` | сам образ: 1,7 ГБ, распакованный 3,5 ГБ |
| `SHA256SUMS` | контрольная сумма, чтобы проверить, что файл скачался целиком |

Проверка: Windows (PowerShell) — `Get-FileHash .\BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz`, Linux/macOS —
`sha256sum -c SHA256SUMS`. Суммы должны совпасть.

Понадобятся компьютер, кабель и [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
(`adb` и `fastboot`).

## Способ 1. Попробовать без стирания (DSU) — начните с него
GSI запускается рядом с вашей системой, данные остаются. Следующая перезагрузка вернёт телефон в обычную систему.
Распаковывать файл **не нужно**.

Нужны разблокированный загрузчик и **около 25 ГБ свободного места** в памяти телефона: 1,7 ГБ на сам файл, 3,5 ГБ на
систему, 8 ГБ на данные GSI, а на большинстве новых телефонов Android ещё требует оставить запас размером с системный
раздел телефона (обычно 8–14 ГБ). Если места меньше, установка закончится ошибкой. Можно обойтись меньшим: в последней
команде замените `8589934592` (8 ГБ под данные GSI) на `4294967296` (4 ГБ).

Команды выполняйте в папке, где лежит скачанный файл (или укажите полный путь к нему).

```
adb push BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img.gz /sdcard/Download/blueos_gsi.img.gz
adb shell setprop persist.sys.fflag.override.settings_dynamic_system true
adb shell am start-activity -n com.android.dynsystem/com.android.dynsystem.VerificationActivity -a android.os.image.action.START_INSTALL -d file:///storage/emulated/0/Download/blueos_gsi.img.gz --el KEY_SYSTEM_SIZE 3512172544 --el KEY_USERDATA_SIZE 8589934592
```
Число после `KEY_SYSTEM_SIZE` — точный размер распакованного образа в байтах, он свой у каждого образа. С другим числом установка закончится ошибкой, поэтому копируйте команду из этой инструкции для того образа, который скачали.

Появится уведомление с ходом установки. Когда она закончится, перезагрузите телефон в GSI кнопкой в этом уведомлении.
Там же есть кнопка, которая удалит установленный образ.

*На Pixel 6a установка проверена этими командами, а перезагрузка в GSI — через `gsi_tool` с правами root. Кнопку в
уведомлении мы не проверяли.*

В GSI свои, отдельные данные: снова будет первая настройка, а отладку по USB придётся включить ещё раз.

### Если установка закончилась ошибкой
Чаще всего причина одна из этих:
- **Мало свободного места** (см. выше, нужно около 25 ГБ). Удалите лишнее или уменьшите данные GSI до 4 ГБ.
- **Загрузчик заблокирован** (`ro.boot.flash.locked` = `1`).
- **Число в команде не от того образа.** Скопируйте команду из этой инструкции заново.
- **У телефона нет DSU.** Если после третьей команды не появилось уведомление или `adb` пишет `Activity not found`
  или `does not exist`, производитель убрал DSU (так бывает у Samsung и на части прошивок Xiaomi). Остаётся способ 2.

Если ничего не подошло, снимите журнал установки и приложите его к сообщению в Issues:
```
adb logcat -c
```
Затем снова выполните третью команду установки, дождитесь ошибки и сохраните журнал:
```
adb logcat -d > dsu-log.txt
```

### Если после перезагрузки снова открылась обычная система
Значит, GSI не смог загрузиться, и телефон сам вернулся в свою систему. Сразу после этого снимите отчёт: в нём часто
сохраняется журнал неудачной загрузки.
```
adb bugreport bugreport.zip
```
Приложите `bugreport.zip` к сообщению в Issues. Учтите: в отчёте много личного (названия сетей Wi-Fi, список приложений,
аккаунты), выкладывайте его, только если вы с этим согласны.

## Способ 2. Установить вместо системы (стирает данные)
**Этот способ на реальном телефоне не проверялся.** Это обычная установка GSI для Android, но отдельные телефоны требуют
своих шагов. Сначала попробуйте способ 1.

1. Распакуйте `.img.gz` (Windows — 7-Zip, Linux/macOS — `gunzip`), получится `BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img`.
2. Возьмите файл `vbmeta.img` из заводской прошивки своего телефона и выключите проверку системы:
```
adb reboot bootloader
fastboot flash --disable-verity --disable-verification vbmeta vbmeta.img
```
3. Поставьте образ и сотрите данные:
```
fastboot reboot fastboot
fastboot flash system BlueOS-0.8.1-gsi_arm64_bvN4-20261107-2.img
fastboot -w
fastboot reboot
```
Если `fastboot` пишет, что не хватает места, перед `flash system` выполните `fastboot delete-logical-partition product_a`
(на телефонах без слотов A/B — `fastboot delete-logical-partition product`).

**Не блокируйте загрузчик, пока стоит GSI:** телефон может перестать загружаться. Вернуться назад можно только заводской
прошивкой вашего телефона.

## Нашли ошибку или всё заработало
Напишите в Issues, это особенно важно: других телефонов у автора нет. Укажите модель телефона, версию Android на нём, способ
установки, точный текст ошибки и что работает, а что нет, лучше со скриншотом.

## Спасибо
Образ собран на [TrebleDroid](https://github.com/TrebleDroid) (phhusson и все участники), с переносом на Android 17 от
[RestlessOS](https://github.com/cawilliamson/treble_restlessos) (Christopher A. Williamson) и сборкой LineageOS как GSI от
[MisterZtr](https://github.com/MisterZtr/LineageOS_gsi).

---

## English (short)
An **experimental** Generic System Image for other phones, in the `v0.8.1-gsi-treble` release. It is built on
**TrebleDroid** (device-specific fixes), unlike the plain `v0.8.1-gsi`. **Tested only on a Pixel 6a through DSU** (boot,
Wi-Fi, mobile data, sound, camera, fingerprint, Google Play work); other phones may not boot or work only partly. Unofficial fan ROM,
**no warranty, use at your own risk**; get your phone's stock firmware first. Needs an unlockable bootloader, Android 13 or
newer, `ro.treble.enabled=true`, `arm64-v8a` and dynamic partitions. Per-device fixes: Settings → System → TrebleDroid
Settings. Sound, camera, fingerprint, brightness or calls may still break on heavily customised firmware. Blur depends on
the phone. If the widget says there is no weather data, pick a city: Wallpaper & style → Weather → City. Google Play is
included; no Blue Archive files are (see [ASSETS.md](ASSETS.md); where to get them: the link in the author's TikTok profile
description, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun)). The built-in "System update" screen doesn't know
about BlueOS updates: watch Releases.

**DSU, keeps your data:** needs an unlocked bootloader (`ro.boot.flash.locked` = `0`) and about **25 GB free**: on most
newer phones Android also keeps a reserve the size of the phone's `super` partition; with less free space the install fails
(or use `4294967296` instead of `8589934592` for 4 GB of GSI data). Run the three `adb` commands in «Способ 1» above, then use
the reboot button in the notification (the button itself is untested; the Pixel 6a test rebooted with `gsi_tool` as root).
The next reboot returns to your system. If the install fails, send `adb logcat -d` taken right after the error; if the phone
boots back into its own system, send `adb bugreport` taken right after that.
**Flashing, wipes the phone, untested:** the commands in «Способ 2» above. Don't relock the bootloader. Please report results in Issues.

Thanks to TrebleDroid (phhusson and contributors), RestlessOS (Christopher A. Williamson) for the Android 17 port and
MisterZtr for the LineageOS GSI targets.
