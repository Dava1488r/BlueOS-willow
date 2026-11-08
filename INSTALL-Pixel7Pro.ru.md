# Установка готовой BlueOS на Google Pixel 7 Pro

[English](INSTALL-Pixel7Pro.md) | **Русский**

Эта инструкция и этот zip — только для **Pixel 7 Pro** (кодовое имя `cheetah`), сборка на странице **Releases** (`v0.8.1-cheetah`). На Pixel 7 и Pixel 7a его ставить нельзя: не загрузится. Для Pixel 7 — [INSTALL-Pixel7.ru.md](INSTALL-Pixel7.ru.md), для Pixel 7a — [INSTALL-Pixel7a.ru.md](INSTALL-Pixel7a.ru.md).

## ⚠️ Прочитайте до начала
- **Неофициальная фанатская прошивка, без каких-либо гарантий.** Всё, что вы делаете с телефоном, вы делаете на свой страх и риск. Это не официальная сборка LineageOS: не сообщайте о багах BlueOS в трекер LineageOS, сообщайте сюда (Issues).
- **Все данные на телефоне сотрутся**: первый раз при разблокировке загрузчика, второй раз при установке. Заранее сохраните фотографии, чаты и **коды двухфакторной аутентификации** (Google Authenticator и др.). Удалите аккаунт Google из телефона, чтобы он не требовал старый после сброса.
- **Загрузчик остаётся разблокированным.** При каждом включении телефон будет показывать предупреждение — это нормально. **Не блокируйте загрузчик с установленной BlueOS**: телефон перестанет включаться.
- **Часть приложений может не работать:** бесконтактная оплата (Google Wallet), некоторые банковские приложения и игры.
- **В этой сборке нет персонажа и звуков из Blue Archive.** Всё остальное работает. Добавить файлы игры можно прямо на телефоне: *Обои и стиль → Персонаж → Файлы персонажа* ([ASSETS.ru.md](ASSETS.ru.md)). Ссылка на архив — в описании профиля автора в TikTok, [tiktok.com/@chozadaun](https://www.tiktok.com/@chozadaun).
- **Google Play уже встроен** (MindTheGapps).
- Операторские телефоны (с блокировкой под оператора) не позволяют разблокировать загрузчик — на них BlueOS поставить нельзя.

## Что скачать
| Файл | Зачем |
|---|---|
| `BlueOS-0.8.1-cheetah-20261108.zip` | сама прошивка, её не распаковывать |
| `boot.img`, `dtbo.img`, `init_boot.img`, `vendor_boot.img` | для первой установки: загрузочные разделы и BlueOS Recovery |
| `SHA256SUMS` | контрольные суммы — сверьте перед прошивкой |

Проверка: Windows (PowerShell) `Get-FileHash .\BlueOS-0.8.1-cheetah-20261108.zip`, Linux/macOS `sha256sum -c SHA256SUMS`. Суммы должны сойтись.

## Перед началом
- Обновите заводскую прошивку до **Android 17 (октябрьское обновление 2026 или новее)**. На старой прошивке BlueOS может не загрузиться.
- Проверьте звонки и SMS на стоковой прошивке.
- Компьютер, провод USB и [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) (`adb` и `fastboot`). На Windows также [Google USB Driver](https://developer.android.com/studio/run/win-usb).
- Зарядите телефон хотя бы до 50 %.

## 1. Разблокируйте загрузчик (один раз)
1. Настройки → О телефоне → нажмите на «Номер сборки» 7 раз: включится режим разработчика.
2. Настройки → Система → Для разработчиков: включите **Заводская разблокировка** (OEM unlocking) и **Отладка по USB**.
3. Подключите телефон к компьютеру и разрешите отладку на телефоне. В папке с platform-tools:
```bash
adb reboot bootloader
fastboot flashing unlock
```
4. На телефоне кнопками громкости выберите «Unlock the bootloader» и подтвердите кнопкой питания. Телефон сотрется. После первого включения снова включите отладку по USB.

## 2. Прошейте BlueOS Recovery
```bash
adb reboot bootloader
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot flash init_boot init_boot.img
fastboot flash vendor_boot vendor_boot.img
```
Затем кнопками громкости выберите **Recovery mode** и нажмите кнопку питания: откроется BlueOS Recovery (логотип с нимбом).

## 3. Установите BlueOS
1. В рекавери: **Factory reset → Format data / factory reset** → подтвердите.
2. Назад → **Apply update → Apply from ADB**.
3. На компьютере:
```bash
adb sideload BlueOS-0.8.1-cheetah-20261108.zip
```
   Процесс может остановиться на 47 % или выдать «failed to read command» — это нормально. На экране телефона должно быть «Install completed».
4. Если рекавери спросит, перезагружаться ли для установки дополнений, ответьте «No»: Google Play уже внутри.
5. **Reboot system now.** Первый запуск занимает пару минут.
