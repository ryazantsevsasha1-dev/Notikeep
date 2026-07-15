# Notikeep — план: мультивыбор, реклама РСЯ, релиз в RuStore

Статус фич из PLAN.md: Фичи 1–3 (удаление из архива, антидубль, сводное уведомление) в этой сессии **реализованы и собираются** (`testReleaseUnitTest` + `lintRelease` зелёные). Прошлый совет «доделать Фичу 2/3» — уже неактуален.

Порядок (согласован): 1) мультивыбор → 2) РСЯ (тестовые блоки) → 3) релиз-конфиг + разрешения.

---

## ЭТАП 1. Мультивыбор + массовые действия

### 1A. Вкладка «Приложения» (RulesScreen) — выбор нескольких, смена правила
- **RulesViewModel** ([RulesViewModel.kt](app/src/main/java/com/notikeep/presentation/rules/RulesViewModel.kt)):
  - Состояние выбора: `selectionMode: Boolean`, `selected: Set<String>` (packageName). Держать в `MutableStateFlow`, влить в `state` через `combine`.
  - `enterSelection(pkg)` (по long-press), `toggleSelection(pkg)`, `clearSelection()`, `selectAll()`.
  - Массовые действия:
    - `applyRuleToSelected(state: RuleState)` — для каждого выбранного `ruleRepository.setState(pkg, label, state)`.
    - (по выбору) `setNotifyForSelected(notify)` — пересчёт RuleState с сохранением `saves`.
  - Лейблы для setState брать из текущих `rows` (map pkg→label).
- **RulesScreen** ([RulesScreen.kt](app/src/main/java/com/notikeep/presentation/rules/RulesScreen.kt)):
  - `RuleRow`: `combinedClickable` — long-press → вход в режим выбора и отметка; в режиме выбора обычный click тоже toggle. Чекбокс/галочка слева (анимированное появление).
  - Верхняя панель действий в режиме выбора: контекстный `TopAppBar` (кол-во выбранных, «Выбрать все», крестик-выход) + строка действий: «Сохранять», «Скрывать», «Игнорировать», «Уведомления вкл/выкл».
  - Реализовать через `Scaffold(topBar = if (selectionMode) SelectionBar else null)` или наложение — Rules сейчас без Scaffold; добавить.
  - Системная кнопка «назад» в режиме выбора → `clearSelection()` (`BackHandler`).
- **Строки:** `strings.xml` — `rules_selection_title` (%d), `rules_select_all`, `rules_bulk_save`, `rules_bulk_archive_only`, `rules_bulk_ignore`, `rules_bulk_notify_on`, `rules_bulk_notify_off`, `rules_selection_exit`.

### 1B. Вкладка уведомлений приложения (AppNotificationsScreen) — мультивыбор для удаления
- **AppNotificationsViewModel** ([AppNotificationsViewModel.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsViewModel.kt)):
  - `selected: Set<Long>` (id), `selectionMode`, `toggle(id)`, `clearSelection()`.
  - `deleteSelected()` — новый репозиторий-метод `deleteByIds(ids: List<Long>)` (одним запросом `DELETE ... WHERE id IN (...)`).
- **DAO/repo:** `@Query("DELETE FROM notifications WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)` + прокинуть в `NotificationRepository`.
- **AppNotificationsScreen** ([AppNotificationsScreen.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsScreen.kt)):
  - long-press строки → режим выбора; в режиме click = toggle; чекбокс в строке.
  - Контекстный TopAppBar: счётчик + кнопка «Удалить выбранные» (с подтверждением) + выход.
  - `BackHandler` для выхода из режима.
  - Работает поверх Paging (`LazyPagingItems`): выбор по id, удаление вызывает `deleteByIds`, список сам обновится.
- **Строки:** `appdetail_selection_title` (%d), `appdetail_delete_selected`, диалог подтверждения множественного удаления.

### 1C. Тесты
- Юнит-тест `deleteByIds` через репозиторий-мок в новом/существующем тесте ViewModel не обязателен (VM тонкие), но добавлю тест на массовое `applyRuleToSelected` в RulesViewModel логику, если она вынесется в use-case. Минимум — компиляция + lint.

---

## ЭТАП 2. Реклама РСЯ (Yandex Mobile Ads) — тестовые блоки

⚠️ Требует ваших действий: регистрация в РСЯ, создание блоков, получение боевых `blockId`. Пока — **демо/тестовые** blockId из доков Яндекса.

- **Зависимость:** добавить `com.yandex.android:mobileads:<verа>` в [libs.versions.toml](gradle/libs.versions.toml) + [build.gradle.kts](app/build.gradle.kts). Репозиторий Яндекса в `settings.gradle.kts` (maven `https://maven.yandex.net`... — по докам).
- **Инициализация:** `MobileAds.initialize(context)` — в `NotikeepApp.onCreate()` (после согласия на аналитику/рекламу; связать с приватностью).
- **Consent:** SDK требует установки consent на обработку данных. Отразить в политике конфиденциальности; API `MobileAds.setUserConsent(...)` (по докам актуальной версии).
- **Баннер:** компонент `AdBanner.kt` (Compose `AndroidView` c `BannerAdView`). Разместить **внизу** одного экрана (например, Архив), не поверх листенера/детального экрана. Не мешать основному сценарию.
- **Тестовые blockId:** демо-блоки Яндекса (например баннер `demo-banner-yandex`), заменить на боевые в release через BuildConfig.
- **ProGuard:** keep-правила Yandex Ads (из доков) в [proguard-rules.pro](app/proguard-rules.pro).
- **Оффлайн:** баннер не грузится без сети — не должен крашить; обработать `onAdFailedToLoad` тихо (скрыть контейнер).
- **Флаг:** показывать рекламу только в release или под `BuildConfig` (в debug — тестовые).

---

## ЭТАП 3. Релиз-конфиг + разрешения (RuStore)

### 3A. Единый runtime-запрос POST_NOTIFICATIONS (Android 13+)
- Сейчас разрешение только в манифесте; runtime-запрос отсутствует. Добавить в **онбординг** ([OnboardingScreen.kt](app/src/main/java/com/notikeep/presentation/onboarding/OnboardingScreen.kt)) шаг/кнопку запроса `POST_NOTIFICATIONS` через `rememberLauncherForActivityResult(RequestPermission)`.
  - Один запрос, переиспользуется и для Фичи 2 (сводка), и для будущих пушей.
  - Тихий отказ: если не дал — приложение работает, сводка/пуши просто не показываются (в `DailySummaryNotifier.canPost()` уже есть проверка — ок).
- Аудит всех разрешений в манифесте ([AndroidManifest.xml](app/src/main/AndroidManifest.xml)):
  - `QUERY_ALL_PACKAGES`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `BIND_NOTIFICATION_LISTENER_SERVICE` — каждое обосновать в онбординге/описании.
  - Проверить, что онбординг объясняет назначение ДО перехода в системные настройки (уже частично есть).

### 3B. Сборка/подпись
- **signingConfig:** `release` подписывать из `keystore.properties` (вне гита) или переменных окружения. Файл `.jks` — вне репозитория; в [.gitignore](.gitignore) добавить `*.jks`, `keystore.properties`, `local.properties`.
- **versionCode=1, versionName="1.0.0"** (в [build.gradle.kts](app/build.gradle.kts) уже `1`/`"1.0"` — обновить versionName).
- **release:** `isMinifyEnabled=true` (уже), добавить `isShrinkResources=true`, `isDebuggable=false` (дефолт), убрать лишние логи.
- **ProGuard-правила:** Room, Hilt (обычно ок), Yandex Ads, RuStore Push (когда добавим), AppMetrica — keep-правила из доков.
- **AAB:** `bundleRelease` — RuStore принимает `.aab`.

### 3C. Юридическое/контент (ваши действия, я готовлю черновики)
- **Политика конфиденциальности** (обязательна): черновик RU-текста — локальное хранение уведомлений, AppMetrica, Yandex Ads consent, (позже) RuStore push-токен. Разместить по публичному URL.
- Карточка RuStore: скриншоты, описание, декларация данных/разрешений, возрастной рейтинг — вне кода.

### 3D. Push (RuStore Push SDK) — ОТДЕЛЬНЫЙ ЭТАП (по желанию, после релиза MVP)
- `ru.rustore.sdk:pushclient` + получение токена + рассылки из консоли RuStore (без своего сервера).
- POST_NOTIFICATIONS переиспользуется из 3A.
- Не блокирует первый релиз — можно во второй версии.

---

## Что требует ВАШИХ действий (я не могу за вас)
- Аккаунт и блоки в РСЯ → боевые `blockId`.
- Release keystore (`.jks`) — сгенерировать, забэкапить в 2 местах.
- Хостинг политики конфиденциальности (публичный URL).
- Аккаунт RuStore, заполнение карточки, скриншоты, декларации.
- (Push) проект в консоли RuStore Push.

## Порядок реализации в коде (сейчас)
1. Этап 1A — мультивыбор в Приложениях.
2. Этап 1B — мультивыбор-удаление в уведомлениях.
3. Этап 2 — каркас РСЯ на тестовых блоках.
4. Этап 3A — runtime POST_NOTIFICATIONS + аудит разрешений.
5. Этап 3B — release-конфиг/подпись/ProGuard/AAB.
6. Черновик политики конфиденциальности (текст в репозиторий).
7. Сборка: `testReleaseUnitTest` + `lintRelease` + пробный `bundleRelease`.
