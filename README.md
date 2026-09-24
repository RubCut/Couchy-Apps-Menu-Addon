# Couchy Apps Menu

Небольшое **remote-first** меню всех установленных приложений для Android TV в аутентичном стиле бокового оверлея (Apps Drawer) с полупрозрачным чёрным фоном, увеличенным скруглением плиток и возможностью скрывать приложения. Это отдельное приложение-компаньон: после установки оно открывает быстрый боковой оверлей приложений прямо поверх экрана.

Проект рассчитан в том числе на слабые приставки уровня **Mi TV Stick первого поколения**:

- нативные Android `View` без Compose, сервисов и сетевых зависимостей;
- один фоновый поток для сканирования приложений;
- правая панель (~67% ширины) с полупрозрачным чёрным фоном (`windowIsTranslucent`); слева — свободная затемнённая область, закрывающая меню по клику или нажатию «Влево»;
- верхняя панель действий: компактная круглая иконка скрытых приложений, капсульные кнопки «Ещё приложения» (Google Play Store) и «Ещё игры» (Google Play Games);
- 4-колоночная сетка 16:9 плиток с увеличенным современным скруглением углов (**20dp**);
- поддержка скрытия приложений через долгое нажатие **OK** и удобный просмотр/восстановление скрытых приложений;
- мягкий белый focus-ring и плавная анимация карточек и кнопок при наведении;
- отдельное активити `OpenAppsActivity` для привязки к кнопке с 9 точками на пультах Android TV через Button Mapper, настройки лаунчеров или ADB;
- перехват кнопки 9 точек (`KEYCODE_ALL_APPS`) для мгновенного закрытия/открытия меню;
- полностью управляется пультом (крестовиной D-pad и кнопкой **OK**);
- список пересканируется после установки, удаления или обновления приложения;
- интерфейс переведён на русский, английский и нидерландский.

Приложение не делает сетевых запросов, не содержит рекламы, аккаунтов или accessibility service.

## Управление

| Пульт | Действие |
| --- | --- |
| ◀ ▲ ▼ ▶ | Перемещение по сетке и верхним кнопкам |
| OK | Запустить приложение |
| Долгое OK | Меню плитки: скрыть приложение, сведения или открыть |
| ◀ из левой колонки | Закрыть боковое меню |
| Кнопка 9 точек / ALL APPS | Открыть / закрыть меню |
| Menu / Info / App switch | Меню обновления и скрытых приложений |
| Back / Home | Закрыть меню / вернуться назад |

## Установка

### Готовая debug-сборка

Для сборки нужны Android SDK с Platform 34 и JDK 17:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Открытие кнопкой 9 точек пульта или автоматизацией

В приложении предусмотрено отдельное легковесное активити **`OpenAppsActivity`**, которое мгновенно открывает боковое меню без задержек и мерцания экрана. На кастомных лаунчерах (Couchy, Projectivy, ATV Launcher и др.) вы можете назначить его на кнопку 9 точек через Button Mapper / tvQuickActions или открыть явным Intent:

```bash
adb shell am start -n com.rubcut.couchyappsmenu/.OpenAppsActivity
```

Также поддерживается стандартное действие `com.rubcut.couchyappsmenu.OPEN`:

```bash
adb shell am start -a com.rubcut.couchyappsmenu.OPEN
```

И стандартный системный action кнопки 9 точек:

```bash
adb shell am start -a android.intent.action.ALL_APPS
```

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
  MainActivity.java              боковой экран, D-pad, диалоги и навигация
  OpenAppsActivity.java          отдельное активити для кнопки 9 точек и ярлыков
  data/AppCatalog.java           поиск launchable-приложений через PackageManager
  data/HiddenAppsManager.java    сохранение и управление скрытыми приложениями
  ui/AppTileView.java            16:9 Leanback-плитка с баннером/иконкой
  ui/RoundedCardView.java         focus-ring и скругление углов 20dp
  ui/CouchyBackgroundView.java   полупрозрачный чёрный фон со сплитом панели
```

## Лицензия

[MIT](LICENSE)
