# Аналитика (AppMetrica) и реклама — инструкция

## Текущее состояние в коде

- SDK подключён: `io.appmetrica.analytics:analytics` ([libs.versions.toml](../gradle/libs.versions.toml)).
- API-ключ читается из `gradle.properties` (`notikeep.appmetrica.apiKey`) → `BuildConfig.APPMETRICA_API_KEY`.
- Инициализация: `AppMetricaAnalytics.init()` в `NotikeepApp.onCreate()`.
- Согласие пользователя: переключатель «Помогать улучшать приложение» в настройках управляет
  `AppMetrica.setDataSendingEnabled(...)` — выключил, данные не уходят.
- События уже летят: `archive_opened`, `rule_changed`, `onboarding_step_viewed`,
  `notification_detail_opened` и др. (см. `domain/port/Analytics.kt`).

⚠️ Ключ лежит в `gradle.properties`, который коммитится в git. Ключ AppMetrica не критично секретный
(он «пишущий», а не «читающий»), но для чистоты лучше перенести строку в `local.properties`
или переменную окружения CI.

## Что смотреть в кабинете AppMetrica (appmetrica.yandex.ru)

| Что нужно знать | Где в кабинете |
|---|---|
| Сколько пользователей (DAU/WAU/MAU), новые/вернувшиеся | Отчёты → Аудитория |
| Удержание (сколько возвращаются на 1/7/30-й день) | Отчёты → Retention (когорты) |
| Какие экраны/функции используют | Отчёты → События (наши `archive_opened` и т.д.) |
| Проходят ли онбординг до конца | События `onboarding_step_viewed` по шагам → воронка (Отчёты → Воронки) |
| Крэши и ANR | Отчёты → Крэши (собираются автоматически) |
| Устройства, версии Android, версии приложения | Отчёты → Технологии |
| География | Отчёты → Аудитория → География |

Рекомендую собрать один дашборд: DAU, Retention D1/D7, воронка онбординга, крэши.
Данные появляются в кабинете через ~10–30 минут после первых сессий; для мгновенной
проверки используйте «Отчёты → Профили» или включите в приложении лог `NotikeepAnalytics` (logcat).

### Какие события стоит добавить дальше (по мере надобности)
- `favorites_opened`, `favorite_added` — насколько востребовано избранное;
- `backfill_completed(count)` — работает ли сбор существующих уведомлений;
- `reconnect_clicked` — как часто пользователи чинят службу вручную (индикатор проблемы!).
Каждое — одна строка в `AnalyticsEvent` + вызов `analytics.track(...)`.

## Реклама: что подключать

**Рекомендация: Яндекс Mobile Ads SDK** (`com.yandex.android:mobileads`). Почему:
- работает без Google Play Services и в RuStore (AdMob для аккаунтов РФ фактически недоступен);
- один SDK = и медиация, и прямые кампании Яндекса; лучший eCPM на RU-аудитории;
- нативно связан с AppMetrica (сквозная статистика доходов в одном кабинете).

### Формат под Notikeep
1. **Якорный баннер (adaptive banner) внизу экрана «Архив»** — основной формат.
   Ненавязчиво, стабильный доход, не мешает главному сценарию.
2. **Межстраничная (interstitial) — НЕ рекомендую** на старте: приложение утилитарное,
   пользователь заходит на секунды; interstitial убьёт retention.
3. Позже, при premium-подписке: `EntitlementProvider` уже есть в коде — у платящих баннер скрываем.

### Пошагово
1. Зарегистрируйтесь в Рекламной сети Яндекса (РСЯ, partner.yandex.ru), добавьте приложение
   (нужна публикация в RuStore — сначала выпустите приложение, потом включайте монетизацию).
2. Создайте рекламный блок «Banner» → получите `adUnitId` вида `R-M-XXXXXX-Y`.
3. В `libs.versions.toml`: `yandex-mobileads = { group = "com.yandex.android", name = "mobileads", version = "7.x.x" }`,
   в `app/build.gradle.kts`: `implementation(libs.yandex.mobileads)`.
4. Инициализация один раз: `MobileAds.initialize(context) {}` в `NotikeepApp.onCreate()`.
5. Composable-обёртка `BannerAd(adUnitId)` через `AndroidView { BannerAdView(...) }`,
   показать внизу `ArchiveScreen` при `entitlement == FREE`.
6. `adUnitId` — в `gradle.properties`/BuildConfig, как и ключ метрики.

Когда дойдёте до этого шага — скажите, подключу и сверстаю баннер.

## Доход + аналитика вместе
В кабинете AppMetrica включите интеграцию с РСЯ (Настройки приложения → Партнёры/Revenue) —
доход от рекламы появится рядом с DAU/Retention, и будет видна «выручка на пользователя».
