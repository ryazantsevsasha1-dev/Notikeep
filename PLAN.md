# План изменений Notikeep

Три фичи: (1) удаление уведомлений из архива долгим удержанием, (2) сводное уведомление-статистика с переключателем в настройках, (3) настраиваемый антидубль с несколькими реализациями для A/B-теста.

Архитектура проекта — Clean Architecture + MVVM (Hilt, Room, DataStore, Compose). Порядок слоёв: `domain` (модели, репозитории-интерфейсы, use-case) → `data` (Room, DataStore, service, work) → `presentation` (Compose + ViewModel). Изменения ниже упорядочены так, чтобы не ломать сборку между слоями.

---

## Фича 1 — Удаление из архива долгим удержанием

Работает на двух экранах: сводка по приложениям (`ArchiveScreen`/`FavoritesScreen` через `AppSummaryListItem`) и список уведомлений приложения (`AppNotificationsScreen`). В сводке удаляется **всё приложение** (все его записи в диапазоне), в списке — **одна запись**.

### Data / DAO
- [NotificationDao.kt](app/src/main/java/com/notikeep/data/local/dao/NotificationDao.kt): добавить
  - `@Query("DELETE FROM notifications WHERE packageName = :packageName") suspend fun deleteByPackage(packageName: String)`
  - (опц.) `deleteByPackageInRange(packageName, from, to)` — если хотим удалять только видимый диапазон дат в архиве. **Рекомендация:** удалять всё приложение целиком (проще и предсказуемее для пользователя), диапазон — на будущее.
- `delete(id)` уже есть — используем для поштучного удаления.

### Domain
- [NotificationRepository.kt](app/src/main/java/com/notikeep/domain/repository/NotificationRepository.kt): добавить `suspend fun deleteByPackage(packageName: String)`.
- [NotificationRepositoryImpl.kt](app/src/main/java/com/notikeep/data/repository/NotificationRepositoryImpl.kt): реализовать делегированием в DAO.
- (Use-case не обязателен — операция тривиальна; можно звать репозиторий напрямую из VM, как уже сделано с `delete`/`clearAll`.)

### Presentation
- [AppSummaryListItem.kt](app/src/main/java/com/notikeep/presentation/common/AppSummaryListItem.kt): добавить `onLongClick` → обернуть в `Modifier.combinedClickable(onClick, onLongClick)` (`@OptIn(ExperimentalFoundationApi::class)`).
- [ArchiveScreen.kt](app/src/main/java/com/notikeep/presentation/archive/ArchiveScreen.kt) и [FavoritesScreen.kt](app/src/main/java/com/notikeep/presentation/favorites/FavoritesScreen.kt): по long-click открывать `AlertDialog` подтверждения → `viewModel.deleteApp(packageName)`.
- [AppNotificationsScreen.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsScreen.kt): у `NotificationRow` добавить long-click → диалог подтверждения → `viewModel.delete(record.id)` (метод `delete` в VM уже есть).
- ViewModel:
  - [ArchiveViewModel.kt](app/src/main/java/com/notikeep/presentation/archive/ArchiveViewModel.kt) и [FavoritesViewModel.kt](app/src/main/java/com/notikeep/presentation/favorites/FavoritesViewModel.kt): `fun deleteApp(pkg: String) = viewModelScope.launch { repository.deleteByPackage(pkg) }`.
  - `AppNotificationsViewModel.delete` уже готов.
- Строки в `strings.xml`: `archive_delete_app_title/message/confirm/cancel`, `appdetail_delete_notification_*`.

**Проверка:** long-press на приложении в архиве → диалог → удаление; список обновляется реактивно (Flow из Room). То же в избранном и в списке приложения.

---

## Фича 2 — Сводное уведомление со статистикой за сегодня

Приложение постит собственное уведомление: «Сегодня сохранено N уведомлений (из них M скрытых/непоказанных)». «Скрытые/непоказанные» = записи с `wasSilenced = true` (правило `ARCHIVE_ONLY` отменяет их из шторки). Обновление — **в реальном времени** при каждом сохранении.

### Настройка (вкл/выкл, как аналитика)
- [UserSettings.kt](app/src/main/java/com/notikeep/domain/model/UserSettings.kt): поле `val dailySummaryEnabled: Boolean = true`.
- [SettingsRepository.kt](app/src/main/java/com/notikeep/domain/repository/SettingsRepository.kt) + [Impl](app/src/main/java/com/notikeep/data/settings/SettingsRepositoryImpl.kt): `setDailySummaryEnabled(enabled)` + ключ `DAILY_SUMMARY_ENABLED` (default true).
- [SettingsScreen.kt](app/src/main/java/com/notikeep/presentation/settings/SettingsScreen.kt): в секции privacy/notifications добавить `Switch` рядом с analytics; [SettingsViewModel.kt](app/src/main/java/com/notikeep/presentation/settings/SettingsViewModel.kt): `setDailySummaryEnabled`.

### DAO — подсчёт за сегодня
- [NotificationDao.kt](app/src/main/java/com/notikeep/data/local/dao/NotificationDao.kt):
  - `@Query("SELECT COUNT(*) FROM notifications WHERE postedAt BETWEEN :from AND :to") fun observeCountInRange(from, to): Flow<Int>`
  - `@Query("SELECT COUNT(*) FROM notifications WHERE wasSilenced = 1 AND postedAt BETWEEN :from AND :to") fun observeSilencedCountInRange(from, to): Flow<Int>`
  - (или одним запросом вернуть `data class DailyCounts(total, silenced)`.)

### Публикация уведомления
- Новый класс `data/notification/DailySummaryNotifier.kt`: создаёт NotificationChannel (low importance, `setOnlyAlertOnce(true)`, `setOngoing(false)`), метод `update(total, silenced)` / `clear()`. Фиксированный notification id.
- **Требуется разрешение `POST_NOTIFICATIONS`** (Android 13+): добавить в `AndroidManifest.xml` и запросить runtime-permission (в онбординге или при включении тумблера). Проверить наличие манифеста и онбординга — см. [OnboardingScreen.kt](app/src/main/java/com/notikeep/presentation/onboarding/OnboardingScreen.kt).
- Где обновлять в реальном времени: сервис-скоуп уже есть в [NotikeepListenerService.kt](app/src/main/java/com/notikeep/data/service/NotikeepListenerService.kt) (`@Inject scope`). Вариант: отдельный «наблюдатель», подписанный на `dailySummaryEnabled` + оба Flow-счётчика (`combine`), и на каждый эмит вызывает `notifier.update(...)` либо `clear()` если выключено. Запускать этот наблюдатель из `NotikeepApp.onCreate()` (в app-scope корутине) — тогда он живёт независимо от экранов.
  - Границы «сегодня» вычислять от начала локального дня; при пересечении полуночи счётчик естественно сбросится, т.к. `from` двигается (проще всего — пересчитывать `startOfToday()` в момент эмита; для мгновенного сброса ровно в полночь можно позже добавить будильник, но это не обязательно для realtime-режима).
- `RemoteViews`/иконка: использовать `R.mipmap` launcher icon; текст брать из `strings.xml` с плейсхолдерами (`daily_summary_title`, `daily_summary_text`).

### Реакция на тумблер
- При выключении — `notifier.clear()`; при включении — немедленный `update` текущими счётчиками. Наблюдатель на `combine(enabledFlow, counts)` покрывает оба случая.

**Проверка:** сохранить уведомление → системное уведомление появляется/растёт; выключить тумблер → уведомление исчезает; после полуночи счётчик стартует заново.

---

## Фича 3 — Антидубль с несколькими реализациями (экспериментальный A/B)

Проблема: VPN/Telegram переобновляют или дублируют уведомления → архив засоряется копиями. Нужно несколько стратегий, переключаемых в настройках, чтобы сравнить.

### Модель стратегии
- Новый enum в domain: `DedupStrategy { OFF, EXACT_TEXT_WINDOW, BY_KEY, TITLE_ONLY_WINDOW, COMBINED }` (набор — под эксперимент; финально сократим).
  - `OFF` — текущее поведение (сохранять всё; сейчас есть только жёсткий unique-index на `packageName+postedAt+title+text`).
  - `EXACT_TEXT_WINDOW` — не сохранять, если у пакета уже есть запись с тем же `title+text` за последние `windowMs` (напр. 10 сек). Ловит спам-переобновления VPN.
  - `BY_KEY` — использовать `sbn.key` ОС: обновления одного уведомления схлопывать в одну запись (обновлять существующую вместо вставки новой). Требует хранить `sbnKey` в сущности.
  - `TITLE_ONLY_WINDOW` — как EXACT, но сравнивать только `title` в окне (ловит Telegram-дубли, где текст слегка меняется).
  - `COMBINED` — `BY_KEY` + `EXACT_TEXT_WINDOW`.
- [UserSettings.kt](app/src/main/java/com/notikeep/domain/model/UserSettings.kt): `val dedupStrategy: DedupStrategy = DedupStrategy.EXACT_TEXT_WINDOW` (или OFF по умолчанию — решить).
- Settings repo/impl: сохранять как строку (`.name`), read с fallback; `setDedupStrategy`.

### Сущность / БД (миграция v4 → v5)
- [NotificationEntity.kt](app/src/main/java/com/notikeep/data/local/entity/NotificationEntity.kt): добавить `val sbnKey: String? = null` (для стратегии `BY_KEY`).
- [NotikeepDatabase.kt](app/src/main/java/com/notikeep/data/local/NotikeepDatabase.kt): `version = 5`, `MIGRATION_4_5`: `ALTER TABLE notifications ADD COLUMN sbnKey TEXT`. Зарегистрировать миграцию в [DataModule.kt](app/src/main/java/com/notikeep/di/DataModule.kt) (проверить, где перечислены `addMigrations`).
- [NotikeepListenerService.kt](app/src/main/java/com/notikeep/data/service/NotikeepListenerService.kt): пробрасывать `sbn.key` в `NotificationRecord` (добавить поле в [NotificationRecord.kt](app/src/main/java/com/notikeep/domain/model/NotificationRecord.kt) и в `Mappers.kt`).

### DAO — поддержка стратегий
- `@Query("SELECT COUNT(*) FROM notifications WHERE packageName=:pkg AND title=:title AND text=:text AND postedAt >= :since") suspend fun countRecentDuplicate(...)`.
- `@Query("... WHERE packageName=:pkg AND title=:title AND postedAt >= :since")` — для TITLE_ONLY.
- Для `BY_KEY`: `findBySbnKey(sbnKey): NotificationEntity?` + `update(entity)` (обновить `text/postedAt` существующей записи).

### Логика — в ApplyRuleUseCase (или отдельный DedupUseCase)
- [ApplyRuleUseCase.kt](app/src/main/java/com/notikeep/domain/usecase/ApplyRuleUseCase.kt) уже единая точка перед `repository.save`. Перед сохранением спросить у нового `DeduplicateUseCase`/репозитория: «сохранять как новую / обновить существующую / пропустить», в зависимости от `settings.dedupStrategy`.
- Чистая логика окна тестируема без Android (в стиле существующих `*UseCaseTest`). Добавить тесты для каждой стратегии.

### Настройки — выбор стратегии
- [SettingsScreen.kt](app/src/main/java/com/notikeep/presentation/settings/SettingsScreen.kt): секция «Антидубль (эксперимент)» — `SingleChoiceSegmentedButtonRow` или список радиокнопок со всеми `DedupStrategy` + краткое описание каждой (это и есть A/B-переключатель для пользователя).
- `SettingsViewModel.setDedupStrategy(...)`.
- Строки: название стратегий и подписи.

**Проверка:** включить VPN/открыть Telegram с частыми апдейтами → при `EXACT_TEXT_WINDOW`/`BY_KEY` дубликаты не плодятся; переключение стратегии в настройках меняет поведение на лету (читается в момент `save`, как retention читается в момент запуска воркера).

---

## Сквозные задачи
- **strings.xml** — все новые тексты (ru + при наличии других локалей).
- **AndroidManifest.xml** — `POST_NOTIFICATIONS` (фича 2).
- **Тесты** — юнит-тесты стратегий антидубля и подсчёта дневной статистики (папка `app/src/test/...`, паттерн существующих тестов).
- **DI** — зарегистрировать `DailySummaryNotifier`, обновить `addMigrations` в `DataModule`.
- **Миграция БД** — только фича 3 (v5). Фичи 1 и 2 схему не меняют (кроме, опц., `DailyCounts` без изменения таблицы).

## Порядок реализации (рекомендуемый)
1. Фича 1 (изолирована, без миграций и разрешений) — быстрая победа.
2. Фича 3 (миграция БД + стратегии + тесты).
3. Фича 2 (разрешение POST_NOTIFICATIONS + канал + realtime-наблюдатель).

## Открытые вопросы
- Дефолт `dedupStrategy` — `OFF` или сразу `EXACT_TEXT_WINDOW`? (Для эксперимента логичнее `OFF`, чтобы видеть разницу при включении.)
- Размер окна дедупа (`windowMs`) — захардкодить 10 сек или тоже вынести в настройки? Пока хардкод, потом решим.
- В архиве удалять всё приложение или только видимый диапазон дат? План исходит из «всё приложение».
