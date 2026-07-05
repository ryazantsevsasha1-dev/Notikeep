# Notikeep — Архитектура (эталонный фундамент)

> Этот проект — **эталон для всех последующих Android-проектов**. Поэтому
> фундамент важнее скорости. Правила ниже обязательны и переносятся в новые проекты.
>
> Продукт — [PLAN.md](./PLAN.md). Обоснования UX — [RESEARCH.md](./RESEARCH.md).

---

## 1. Текущее состояние (реализовано)
- `namespace = com.notikeep`, `minSdk 24`, `targetSdk/compileSdk 36`.
- Скелет переведён на **Kotlin + Jetpack Compose + Room(+FTS) + Hilt + DataStore + WorkManager**.
- Слои разложены (см. §3). Проект собирается (`assembleDebug`, `assembleRelease` с R8) и
  проходит unit-тесты.

> **Важно про версии тулчейна.** Скелет пришёл на превью **AGP 9.2.1**, но плагин
> **Hilt 2.57 несовместим с AGP 9** (ищет удалённый `Android BaseExtension`).
> Поэтому эталон закреплён на стабильной связке:
> **AGP 8.9.2, Gradle 8.11.1, Kotlin 2.0.21, Compose BOM 2024.12, Room 2.6.1,
> Hilt 2.52**. Когда Hilt выпустит поддержку AGP 9 — поднимаем одним изменением
> version catalog. `local.properties` указывает на Android SDK; сборка требует
> **JDK 17+** (использовал JBR из Android Studio).

---

## 2. Архитектурный стиль
**Clean Architecture (3 слоя) + MVVM в presentation + однонаправленный поток данных (UDF).**

Почему так:
- Разделение слоёв = слабая связанность, тестируемость, замена реализаций без переписывания (SOLID — D, «зависим от абстракций»).
- MVVM — родной для Compose (`ViewModel` + `StateFlow` → `State`).
- UDF (state вниз, events вверх) убирает рассинхрон UI и делает экраны предсказуемыми.

```
┌───────────────────────── presentation ─────────────────────────┐
│  Compose Screens  ──state──►  собирают UiState из ViewModel      │
│                   ◄─events──  шлют Intent/Action во ViewModel     │
│  ViewModel: держит UiState (StateFlow), вызывает UseCase          │
└───────────────────────────────┬─────────────────────────────────┘
                                 │ зависит от абстракций domain
┌───────────────────────── domain (чистый Kotlin) ────────────────┐
│  Модели (Notification, AppRule)                                  │
│  Репозитории-ИНТЕРФЕЙСЫ (NotificationRepository, RuleRepository) │
│  UseCase (по одной операции: ObserveArchive, SearchNotifications)│
│  Порты: Analytics, EntitlementProvider (заглушка биллинга)       │
└───────────────────────────────┬─────────────────────────────────┘
                                 │ реализуется в data
┌───────────────────────── data ─────────────────────────────────┐
│  Room (Entity/DAO/DB) + Repository-РЕАЛИЗАЦИИ + мапперы          │
│  NotificationListenerService → пишет через репозиторий           │
│  DataStore (настройки), реализация Analytics                     │
└─────────────────────────────────────────────────────────────────┘
```

**Правило зависимостей:** стрелки только внутрь. `domain` не знает про Android,
Room и Compose. `presentation` и `data` зависят от `domain`, но не друг от друга.

---

## 3. Модульная структура
Стартуем с **пакетов внутри `:app`** (по слоям), НЕ с Gradle-мультимодуля.
Причина: для команды из 2 человек мультимодуль сейчас — оверхед; но пакеты
разложены так, что вынести слой в отдельный модуль позже = перенос папки без переписывания.

```
com.notikeep
├── di/                      # Hilt-модули (связывают интерфейсы с реализациями)
├── domain/
│   ├── model/               # Notification, AppRule, RuleState, ServiceHealth
│   ├── repository/          # интерфейсы
│   ├── usecase/             # по одному классу на операцию
│   └── port/                # Analytics, EntitlementProvider
├── data/
│   ├── local/               # Room: entity, dao, NotikeepDatabase, mappers
│   ├── repository/          # реализации интерфейсов domain
│   ├── service/             # NotikeepListenerService (NotificationListenerService)
│   ├── settings/            # DataStore
│   └── analytics/           # реализация Analytics
├── presentation/
│   ├── archive/  search/  rules/  settings/  onboarding/
│   │   └── (каждый: Screen.kt, ViewModel.kt, UiState.kt)
│   ├── navigation/          # NavHost, маршруты
│   └── theme/               # Material3 тема
└── NotikeepApp.kt           # @HiltAndroidApp
```

---

## 4. Ключевые технические решения

### 4.1 Захват уведомлений — `NotificationListenerService`
- Сердце продукта. Сервис ловит `onNotificationPosted`, извлекает пакет/заголовок/текст/время,
  пишет через `NotificationRepository`.
- Гашение: если для пакета правило `Только архив` или `Игнорировать` — вызвать
  `cancelNotification(key)` (после сохранения, если `Только архив`).
- **Сервис — тонкий адаптер:** никакой бизнес-логики в нём, только маппинг
  `StatusBarNotification → domain` и вызов репозитория/usecase. Логику решения о
  гашении держим в `ApplyRuleUseCase`, чтобы её можно было юнит-тестировать без Android.

### 4.2 «Сохраняем только с этого момента»
- Технически невозможно достать историю до выдачи доступа (корневая причина жалобы
  «удалило всё», см. RESEARCH §4.1).
- Архитектурно: `firstAccessGrantedAt` в DataStore; empty-state Архива читает его и
  показывает честный текст. **Никогда** не трогаем/не чистим системные уведомления при старте.

### 4.3 Здоровье сервиса
- `ServiceHealth` (enum: ACTIVE / NEEDS_ATTENTION) вычисляется из факта включённого
  Notification Access + статуса battery optimization. Экспонируется как `Flow` в UI.

### 4.4 Хранение
- **Room** — реляционные данные (уведомления, правила). Индекс по `packageName` и `postedAt`
  для группировки/поиска. FTS-таблица для поиска по тексту (быстро на большом архиве).
- **DataStore (Preferences)** — настройки (тема, срок хранения, флаг аналитики, `firstAccessGrantedAt`).
- Срок хранения — периодическая чистка через `WorkManager` (старше N дней).

### 4.5 Асинхронность
- **Coroutines + Flow** везде. Репозитории отдают `Flow` для реактивных списков.
  `viewModelScope` в presentation, `Dispatchers.IO` для БД в data.

### 4.6 Аналитика и биллинг — через порты
- `Analytics` и `EntitlementProvider` — интерфейсы в `domain/port`.
- Реализация `Analytics` сейчас локальная; биллинг — заглушка `FreeEntitlementProvider`
  (всё разблокировано). Подмена реализации через Hilt = ноль изменений в UI/логике.
  Это и есть «поправки по правилам ООП, а не костыль».

---

## 5. DI — Hilt
Почему Hilt, а не Koin/ручной DI: официальный от Google, компайл-тайм проверки,
меньше рантайм-сюрпризов, стандарт для эталона. Модули в `di/` биндят интерфейс→реализацию.
Замена реализации (напр. Analytics) = одна строка `@Binds`.

---

## 6. Принципы кода (обязательны, переносятся в новые проекты)
- **SOLID.** Особенно SRP (UseCase = 1 операция) и DIP (зависим от интерфейсов domain).
- **Immutable UiState** (`data class`, `val`), однонаправленный поток.
- **Никаких God-классов.** Сервис/ViewModel тонкие, логика — в UseCase.
- **Никаких костылей при доработке.** Новая фича = новый UseCase / расширение интерфейса,
  а не `if` в чужом классе.
- **Тесты с фундамента:** domain (UseCase) — чистые JUnit-тесты без Android; data — тесты DAO (in-memory Room).
- **Именование и стиль** — единообразны, самодокументируемый код, комментарии только там, где «почему», а не «что».

---

## 7. Подключённые библиотеки (сделано)
Всё в version catalog `gradle/libs.versions.toml`:
- Kotlin + **Compose** (BOM, material3, icons-extended, activity-/navigation-compose).
- **Room** (runtime, ktx, compiler через KSP) + **FTS4** для поиска.
- **Hilt** (android + compiler, hilt-navigation-compose, hilt-work).
- **DataStore** (preferences), **WorkManager**, **Coroutines**, lifecycle-runtime-compose.
- Тесты: JUnit, Turbine, Room-testing, MockK, coroutines-test.

Выполненные правки конфигурации:
- `namespace`/`applicationId` → `com.notikeep`; Java/Kotlin target 17; Compose в `buildFeatures`.
- `release { isMinifyEnabled = true }` + `proguard-rules.pro` (R8 проходит).
- `gradle.properties`: `android.useAndroidX=true`, `nonTransitiveRClass=true`.
- Manifest: listener-service, permissions, отключён дефолтный WorkManager-initializer
  (используем `Configuration.Provider` + HiltWorkerFactory).

---

## 8. Разрешения (минимум — анти-Notisave)
- `BIND_NOTIFICATION_LISTENER_SERVICE` (обязательно, суть приложения).
- `POST_NOTIFICATIONS` (для собственного foreground-статуса, если нужен).
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (через системный диалог, для стабильности listener).
- **Без** storage-прав, без интернета в MVP (всё локально → усиливает УТП приватности).
