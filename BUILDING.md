# Сборка BlueOS

BlueOS — это LineageOS 24.0 плюс одна папка `vendor/blueos`. Если LineageOS 24.0 для вашего телефона собирается,
BlueOS тоже соберётся: сначала соберите и поставьте обычную LineageOS по её инструкции, потом добавьте BlueOS.

## 1. Что нужно
- **Телефон, который официально поддерживает LineageOS 24.0** — проверьте на [wiki.lineageos.org/devices](https://wiki.lineageos.org/devices)
  (раздел «Build for yourself» вашего устройства). Нет в списке — см. [PORTING.md](PORTING.md).
- Разблокированный загрузчик (это стирает телефон).
- Компьютер с Linux (Ubuntu 22.04/24.04) или Windows с WSL2. **ОЗУ: 32 ГБ** (с файлом подкачки 32–48 ГБ;
  с 16 ГБ сборка идёт, но очень медленно). **Диск: ~350 ГБ свободных на SSD** (исходники ~150 ГБ, сборка ~150 ГБ).
- Время: скачивание исходников — несколько часов, первая сборка — 2–6 часов, следующие — 20–60 минут.

## 2. Исходники LineageOS 24.0
Делайте всё по разделу «Build for yourself» вашего устройства на wiki LineageOS: установка пакетов, `repo`,
```
repo init -u https://github.com/LineageOS/android.git -b lineage-24.0 --git-lfs --no-clone-bundle
repo sync -j8
```
затем `breakfast <устройство>` и закрытые файлы производителя (блобы: `extract-files.py` с телефона или
готовые из TheMuppets). **Соберите и поставьте сначала чистую LineageOS** — так вы будете знать, что дерево
устройства в порядке, и любые ошибки после этого будут от BlueOS.

## 3. Добавить BlueOS
В `.repo/local_manifests/blueos.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <remote name="blueos" fetch="https://github.com/GDmio" />
  <project path="vendor/blueos" name="BlueOS" remote="blueos" revision="main" />
</manifest>
```
`revision="main"` — самая свежая BlueOS. Нужна ровно версия из релиза — `revision="refs/tags/v0.8.1"`.

Подключить BlueOS к сборке — одна строка:
```
mkdir -p vendor/extra
echo '$(call inherit-product, vendor/blueos/config/blueos.mk)' >> vendor/extra/product.mk
```
**Google Play (необязательно).** BlueOS сам Google-сервисы не содержит. Если они нужны, добавьте MindTheGapps
для Android 17 в `.repo/local_manifests/gapps.xml`:
```xml
<manifest>
  <project name="MindTheGapps/vendor_gapps" path="vendor/gapps" remote="github" revision="cinnamonbun" clone-depth="1" />
</manifest>
```
`config/blueos.mk` подключит их сам, если папка `vendor/gapps` есть. Затем `repo sync vendor/blueos vendor/gapps`.

## 4. Сборка
```
source build/envsetup.sh
breakfast <устройство>
m bacon -j$(nproc)
```
`source build/envsetup.sh` сам накладывает патчи BlueOS (`vendor/blueos/vendorsetup.sh` → `scripts/apply-patches.sh`).
**Посмотрите вывод:** должны быть строки `BlueOS: applied …` (при первом запуске) и **ни одной** `BlueOS: FAILED`.
Повторный запуск безопасен — уже наложенные патчи пропускаются.

**Если патч не встал** (`BlueOS: FAILED`): LineageOS ушла вперёд от версии, на которой делались патчи. Два пути:
1. Поправить патч под новый код — PORTING.md, «Частые проблемы» (хорошая задача для ИИ-агента).
2. Проще: закрепить патчимые проекты на тех версиях LineageOS, где патчи точно встают. Остальная LineageOS остаётся
   свежей (`repo sync` вернёт эти проекты к чистым версиям, `envsetup.sh` наложит патчи заново):
```
cp vendor/blueos/manifests/blueos-pinned.xml .repo/local_manifests/
repo sync -j8 --force-sync
source build/envsetup.sh
```
   Чтобы вернуться на свежую LineageOS — удалите этот файл из `.repo/local_manifests/` и снова `repo sync`.

Готовый архив: `out/target/product/<устройство>/BlueOS-<версия>-<устройство>-<дата>.zip`.

**Подпись.** Подписывайте сборки своими ключами (wiki LineageOS: «Signing builds»). Ключи никому не отдавайте и не
выкладывайте; потеряете их — следующие сборки не встанут поверх без сброса.

## 5. Установка
Как обычная LineageOS для вашего устройства: recovery из этой же сборки, затем `adb sideload <архив>`.
С другой прошивки — со сбросом данных. Обновление BlueOS поверх BlueOS (те же ключи) — без сброса.

## 6. После первой сборки
- **Персонаж, звуки, обои** — по желанию, из своих файлов: [ASSETS.md](ASSETS.md), потом пересобрать.
- **«LineageOS» → «BlueOS» в текстах** приложений вашего устройства: `python3 vendor/blueos/tools/rebrand.py`
  (из корня исходников, после полной сборки), потом пересобрать.
- **Обновление исходников:** `repo sync`, затем снова `source build/envsetup.sh` — патчи встанут заново.

## 7. Как проверить, что это BlueOS
- Анимация загрузки — нимб и надпись «BlueOS».
- «Настройки» → «О телефоне»: версия BlueOS.
- «Обои и стиль»: страницы «Живые часы», «Виджеты и значки», «Цвета», «Погода», «Движение телефона».
- На рабочем столе — виджеты BlueOS (часы с нимбом, «Сводка», эквалайзеры, погода).
