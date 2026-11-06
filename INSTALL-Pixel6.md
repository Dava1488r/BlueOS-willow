# Установка готовой BlueOS на Google Pixel 6

Эта инструкция и этот zip — только для **Pixel 6** (кодовое имя `oriole`), сборка на странице **Releases**
(`v0.8.1-oriole`). На Pixel 6 Pro и Pixel 6a его ставить нельзя: не загрузится. Для Pixel 6a — [INSTALL.md](INSTALL.md).

Сборку проверили на своём Pixel 6 тестеры: загружается и работает. Если найдёте ошибку, откройте Issue.

## ⚠️ Прочитайте до начала
- **Неофициальная фанатская прошивка, без каких-либо гарантий.** Всё, что вы делаете с телефоном, — на ваш страх и
  риск. Это не сборка LineageOS: об ошибках BlueOS не пишите в LineageOS, пишите сюда (Issues).
- **Все данные на телефоне сотрутся** — при разблокировке загрузчика и ещё раз при установке. Сохраните фото,
  переписки, **коды двухфакторной защиты** (Google Authenticator и т. п.) заранее. Удалите с телефона аккаунт Google,
  чтобы после сброса он не спрашивал старый.
- **Загрузчик останется разблокированным.** При каждом включении телефон покажет предупреждение об этом — так и
  должно быть. **Не блокируйте загрузчик, пока стоит BlueOS**: телефон перестанет загружаться.
- **Некоторые приложения могут не работать:** оплата телефоном (Google Wallet), часть банковских приложений и игр.
- **Персонажа и звуков из Blue Archive в этой сборке нет.** Всё остальное работает. Файлы игры можно добавить
  прямо на телефоне: «Обои и стиль» → «Персонаж» → «Добавить файлы персонажа» ([ASSETS.md](ASSETS.md)). Где их взять:
  ссылка в описании профиля автора в TikTok — [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play внутри** (MindTheGapps).
- Телефон, купленный у оператора с блокировкой, загрузчик не разблокирует — тогда установить нельзя.

## Что скачать
| Файл | Зачем |
|---|---|
| `BlueOS-0.8.1-oriole-20261105.zip` | сама прошивка, его не распаковывать |
| `boot.img`, `dtbo.img`, `vendor_boot.img` | для первой установки: загрузочный раздел и recovery BlueOS |
| `SHA256SUMS` | контрольные суммы — проверить, что файлы скачались целиком |

Проверка: Windows (PowerShell) — `Get-FileHash .\BlueOS-0.8.1-oriole-20261105.zip`, Linux/macOS — `sha256sum -c SHA256SUMS`.
Суммы должны совпасть. Не совпали — скачайте заново, не прошивайте.

## Перед началом
- Обновите стоковую прошивку до **Android 17, обновление октября 2026 или новее**. На старой BlueOS может не загрузиться.
- Проверьте, что на стоковой прошивке работают звонки и SMS.
- Компьютер, кабель USB и [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
  (`adb` и `fastboot`). На Windows ещё [Google USB Driver](https://developer.android.com/studio/run/win-usb).
- Зарядите телефон хотя бы до 50 %.

## 1. Разблокировать загрузчик (один раз)
1. «Настройки» → «О телефоне» → 7 раз нажать «Номер сборки» — включится режим разработчика.
2. «Настройки» → «Система» → «Для разработчиков»: включить **«Заводская разблокировка»** и **«Отладка по USB»**.
3. Подключить к компьютеру, разрешить отладку на телефоне. В папке platform-tools:
```
adb reboot bootloader
fastboot flashing unlock
```
4. На телефоне кнопками громкости выбрать «Unlock the bootloader», подтвердить кнопкой питания. Телефон сотрётся.
   После загрузки снова включите «Отладку по USB».

## 2. Поставить recovery BlueOS
```
adb reboot bootloader
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot reboot bootloader
fastboot flash vendor_boot vendor_boot.img
```
Затем кнопками громкости выбрать **Recovery mode** и нажать питание — откроется recovery BlueOS (логотип с нимбом).
Если логотипа BlueOS нет, дальше не идите.

## 3. Установить BlueOS
1. В recovery: **Factory reset → Format data / factory reset** → подтвердить.
2. Назад → **Apply update → Apply from ADB**.
3. На компьютере:
```
adb sideload BlueOS-0.8.1-oriole-20261105.zip
```
   Может остановиться на 47 % или написать «failed to read command» — это нормально. На телефоне должно быть
   «Install completed».
4. Если recovery спросит про перезагрузку для дополнений — «No», Google Play уже внутри.
5. **Reboot system now.** Первая загрузка — несколько минут.

## Обновления
Новые версии BlueOS для Pixel 6 ставятся **без сброса**: recovery → Apply update → Apply from ADB →
`adb sideload <новый zip>`. Данные остаются.

## Если что-то пошло не так
- Не загружается дальше логотипа больше 10 минут — зайдите в recovery (питание + громкость вниз, затем Recovery mode),
  сделайте Format data и поставьте zip ещё раз.
- **Вернуть стоковую прошивку:** [flash.android.com](https://flash.android.com) (Chrome или Edge). Данные сотрутся.
  Загрузчик блокируйте только после возврата на стоковую прошивку.
- Нашли ошибку — откройте Issue: что делали, что увидели, лучше со скриншотом.

---

## English (short)
This zip is **only for the Pixel 6 (`oriole`)**, release `v0.8.1-oriole`; not for the Pixel 6 Pro or 6a. Tested by
testers on their Pixel 6. Unofficial fan ROM, **no warranty**; installing **wipes the phone**; never relock the
bootloader while BlueOS is installed. No Blue Archive files are included ([ASSETS.md](ASSETS.md)). Steps: update stock to
Android 17 (October 2026 or newer) → `fastboot flashing unlock` → `fastboot flash boot boot.img`, `dtbo dtbo.img`,
`fastboot reboot bootloader`, `fastboot flash vendor_boot vendor_boot.img` → Recovery mode → Format data → Apply from
ADB → `adb sideload BlueOS-0.8.1-oriole-20261105.zip` → Reboot. Check files against `SHA256SUMS` first.
