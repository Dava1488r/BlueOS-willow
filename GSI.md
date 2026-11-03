# BlueOS GSI: для других телефонов (экспериментально)

GSI — это один образ системы, который можно запустить на многих телефонах с Android: он заменяет только саму систему,
а всё, что относится к железу, остаётся от прошивки телефона. Файл лежит на странице **Releases**, в релизе `v0.8.1-gsi`.

## ⚠️ Прочитайте до начала
- **Проверено только на одном телефоне:** Pixel 6a, запуск через DSU (способ 1). На других телефонах образ никто не
  проверял. Он может не загрузиться или работать частично.
- **Неофициальная фанатская прошивка, без каких-либо гарантий.** Всё — на ваш страх и риск. Об ошибках пишите сюда
  (Issues), а не в LineageOS.
- **Заранее найдите заводскую прошивку именно для своего телефона и узнайте, как её поставить.** Только так можно будет
  вернуться назад.
- **Способ 2 сотрёт все данные.** Разблокировка загрузчика, если он ещё заблокирован, тоже стирает телефон.
- **Обновляться GSI будет редко**, только с крупными версиями BlueOS.

## Подойдёт ли телефон
Нужны разблокируемый загрузчик и прошивка на **Android 13 или новее**. Включите отладку по USB и проверьте:
```
adb shell getprop ro.treble.enabled
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.boot.dynamic_partitions
```
Должно быть `true`, `arm64-v8a`, `true`. Если ответ другой, этот образ не подойдёт.

## Что может не работать
- **На телефонах, прошивка которых сильно отличается от чистого Android** (часто это Samsung, Xiaomi, телефоны на MediaTek),
  могут ломаться звук, камера, отпечаток, яркость или звонки. Исправлений для отдельных моделей в образе нет.
- **Размытие и стекло** зависят от прошивки телефона: на некоторых их не будет.
- **Погода.** На Pixel 6a в GSI Google не выдавал местоположение. Если виджет пишет «Нет данных о погоде», укажите город:
  «Обои и стиль» → «Погода» → «Город».
- **Виджеты и сетка** подобраны под экран Pixel 6a, на других экранах могут выглядеть хуже.
- **Оплата телефоном, часть банков и игр** могут отказаться работать (Play Integrity). В настройках телефон называется
  «GSI on ARM64».
- **Ядро, recovery и логотип при включении** остаются от вашей прошивки: GSI их не меняет.

Google Play внутри (MindTheGapps). Персонажей и звуков из Blue Archive в образе нет, файлы игры можно добавить прямо на
телефоне: «Обои и стиль» → «Персонаж» → «Добавить файлы персонажа» ([ASSETS.md](ASSETS.md)). Где их взять: ссылка в
описании профиля автора в TikTok — [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).

## Что скачать
| Файл | Зачем |
|---|---|
| `BlueOS-0.8.1-gsi_arm64-20261103.img.gz` | сам образ: 1,6 ГБ, распакованный 3,1 ГБ |
| `SHA256SUMS` | контрольная сумма, чтобы проверить, что файл скачался целиком |

Проверка: Windows (PowerShell) — `Get-FileHash .\BlueOS-0.8.1-gsi_arm64-20261103.img.gz`, Linux/macOS —
`sha256sum -c SHA256SUMS`. Суммы должны совпасть.

Понадобятся компьютер, кабель и [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
(`adb` и `fastboot`).

## Способ 1. Попробовать без стирания (DSU) — начните с него
GSI запускается рядом с вашей системой, данные остаются. Следующая перезагрузка вернёт телефон в обычную систему. Нужен
разблокированный загрузчик и примерно 12 ГБ свободного места. Распаковывать файл **не нужно**.

```
adb push BlueOS-0.8.1-gsi_arm64-20261103.img.gz /sdcard/Download/blueos_gsi.img.gz
adb shell setprop persist.sys.fflag.override.settings_dynamic_system true
adb shell am start-activity -n com.android.dynsystem/com.android.dynsystem.VerificationActivity -a android.os.image.action.START_INSTALL -d file:///storage/emulated/0/Download/blueos_gsi.img.gz --el KEY_SYSTEM_SIZE 3145588736 --el KEY_USERDATA_SIZE 8589934592
```
Появится уведомление с ходом установки. Когда она закончится, перезагрузите телефон в GSI кнопкой в этом уведомлении.
Там же есть кнопка, которая удалит установленный образ.

*На Pixel 6a установка проверена этими командами, а перезагрузка в GSI — через `gsi_tool` с правами root. Кнопку в
уведомлении мы не проверяли.*

В GSI свои, отдельные данные: снова будет первая настройка, а отладку по USB придётся включить ещё раз.

## Способ 2. Установить вместо системы (стирает данные)
**Этот способ на реальном телефоне не проверялся.** Это обычная установка GSI для Android, но отдельные телефоны требуют
своих шагов. Сначала попробуйте способ 1.

1. Распакуйте `.img.gz` (Windows — 7-Zip, Linux/macOS — `gunzip`), получится `BlueOS-0.8.1-gsi_arm64-20261103.img`.
2. Возьмите файл `vbmeta.img` из заводской прошивки своего телефона и выключите проверку системы:
```
adb reboot bootloader
fastboot flash --disable-verity --disable-verification vbmeta vbmeta.img
```
3. Поставьте образ и сотрите данные:
```
fastboot reboot fastboot
fastboot flash system BlueOS-0.8.1-gsi_arm64-20261103.img
fastboot -w
fastboot reboot
```
Если `fastboot` пишет, что не хватает места, перед `flash system` выполните `fastboot delete-logical-partition product`.

**Не блокируйте загрузчик, пока стоит GSI:** телефон может перестать загружаться. Вернуться назад можно только заводской
прошивкой вашего телефона.

## Нашли ошибку или всё заработало
Напишите в Issues, это особенно важно: других телефонов у автора нет. Укажите модель телефона, версию Android на нём, способ
установки и что работает, а что нет, лучше со скриншотом.

---

## English (short)
An **experimental** Generic System Image for other phones, in the `v0.8.1-gsi` release. **Tested only on a Pixel 6a through
DSU; no other phone has been tried.** Unofficial fan ROM, **no warranty, use at your own risk**; get your phone's stock
firmware first. Needs an unlockable bootloader, Android 13 or newer, `ro.treble.enabled=true`, `arm64-v8a` and dynamic partitions.
Sound, camera, fingerprint, brightness or calls may break, especially on heavily customised firmware. Blur depends on the
phone. If the widget says there is no weather data, pick a city: Wallpaper & style → Weather → City. Google Play is included;
no Blue Archive files are (see [ASSETS.md](ASSETS.md); where to get them: the link in the author's TikTok profile description,
[tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun)).

**DSU, keeps your data:** the three `adb` commands in «Способ 1» above, then use the reboot button in the notification (the button
itself is untested; the Pixel 6a test rebooted with `gsi_tool` as root). The next reboot returns to your system.
**Flashing, wipes the phone, untested:** the commands in «Способ 2» above. Don't relock the bootloader. Please report results in Issues.
