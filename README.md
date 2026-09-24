# Couchy Apps Menu

Небольшое **remote-first** меню всех установленных приложений для Android TV в визуальном стиле Couchy Launcher. Это отдельное приложение-компаньон: после установки оно появляется в Couchy как плитка `Приложения Couchy` и открывает быстрый полный список приложений.

Проект рассчитан в том числе на слабые приставки уровня **Mi TV Stick первого поколения**:

- нативные Android `View` без Compose, сервисов и сетевых зависимостей;
- один фоновый поток для сканирования приложений;
- 16:9 плитки: Leanback-баннер приложения, а при его отсутствии — иконка на спокойном цветном фоне;
- полноэкранный фон в палитре Couchy, мягкий focus-ring и увеличение выбранной плитки;
- полностью управляется крестовиной и кнопкой **OK**;
- список пересканируется после установки, удаления или обновления приложения;
- интерфейс переведён на русский и английский.

Приложение не делает сетевых запросов, не содержит рекламы, аккаунтов или accessibility service.

## Управление

| Пульт | Действие |
| --- | --- |
| ◀ ▲ ▼ ▶ | Перемещение по сетке |
| OK | Запустить приложение |
| Долгое OK | Открыть системную страницу сведений о приложении |
| Menu / Info / App switch | Обновить список |
| Home | Вернуться в Couchy |

## Установка

### Готовая debug-сборка

Для сборки нужны Android SDK с Platform 34 и JDK 17:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

После этого откройте **Приложения Couchy** в Couchy Launcher и переместите плитку туда, где меню должно быть всегда под рукой.

### Открытие кнопкой или автоматизацией

Android TV не даёт обычному приложению глобально перехватить штатную кнопку с девятью точками: это решает прошивка приставки. Меню можно назначить на свободную клавишу через Button Mapper / автоматизацию или открыть явным intent:

```bash
adb shell am start -a com.rubcut.couchyappsmenu.OPEN
```

Также подойдёт запуск компонента:

```bash
adb shell am start -n com.rubcut.couchyappsmenu/.MainActivity
```

Действие `com.rubcut.couchyappsmenu.OPEN` намеренно стабильно — его можно использовать в ярлыках и мапперах кнопок.

## Стабильно подписанный APK в GitHub Actions

В репозитории есть ручной workflow **Signed APK**. Он создаёт `app-release.apk`, подписывает его одним и тем же release-ключом и публикует файл в отдельном GitHub prerelease. Это не использует ограниченную квоту GitHub Actions Artifacts. Запуск: **Actions → Signed APK → Run workflow**; после завершения APK находится во вкладке **Releases**.

Приватный ключ в репозиторий не добавляется. Один раз создайте и сохраните keystore в надёжном месте, затем добавьте в **Settings → Secrets and variables → Actions** следующие repository secrets:

| Secret | Значение |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | содержимое keystore, закодированное одной строкой Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | пароль хранилища |
| `ANDROID_KEY_ALIAS` | alias ключа |
| `ANDROID_KEY_PASSWORD` | пароль ключа |

Например, на машине с JDK 17:

```bash
keytool -genkeypair -keystore couchy-apps-release.p12 -storetype PKCS12 \
  -alias couchy-apps -keyalg RSA -keysize 4096 -validity 10000
base64 --wrap=0 couchy-apps-release.p12 > couchy-apps-release.base64
```

Содержимое `couchy-apps-release.base64` добавляется в первый secret. Не теряйте исходный `.p12` и не отправляйте его или пароли в чат: только этот ключ позволит выпускать обновления, которые Android TV установит поверх предыдущей версии. Workflow использует номер запуска как `versionCode`, поэтому новые APK корректно обновляют старые.

## Технические детали

- **minSdk 21** (Android 5.0), `targetSdk` / `compileSdk` 34.
- Нужна системная возможность `QUERY_ALL_PACKAGES`: без неё на новых Android TV невозможно честно показать весь список установленных приложений.
- Вначале запрашиваются TV-активности `LEANBACK_LAUNCHER`, затем обычные `LAUNCHER`; TV entry point выбирается первым, если приложение содержит оба.
- Собственный пакет не показывается в меню, поэтому в сетке нет рекурсивной плитки самого меню.

## Структура

```text
app/src/main/java/com/rubcut/couchyappsmenu/
  MainActivity.java              экран, D-pad, запуск и обновление каталога
  data/AppCatalog.java           поиск launchable-приложений через PackageManager
  ui/AppTileView.java            16:9 плитка с баннером/иконкой
  ui/RoundedCardView.java         focus-ring и анимация без внешних UI-библиотек
  ui/CouchyBackgroundView.java   лёгкий градиентный фон
```

## Лицензия

[MIT](LICENSE)
