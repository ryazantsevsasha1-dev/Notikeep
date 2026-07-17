# План оптимизации при большом количестве уведомлений

Цель: приложение остаётся лёгким и быстрым при десятках тысяч сохранённых уведомлений (VPN/мессенджеры спамят). Без искусственных ограничений пользователя — данные не режем, делаем доступ к ним «ленивым» и индексируем.

## Диагностика (узкие места)

1. **Детальный экран приложения** ([AppNotificationsViewModel.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsViewModel.kt)) — `observeByPackage`/`observeFavoritesByPackage` грузят ВСЕ строки приложения в память и в `LazyColumn`. Главная проблема памяти.
2. **Антидубль на каждое сохранение** — `countRecentByText`/`countRecentByTitle` фильтруют по `packageName+title[+text]`, но индекс есть только на `packageName`; `findBySbnKey` — вообще без индекса → скан на каждое сохранение при стратегиях BY_KEY/COMBINED.
3. **`COUNT(*)`** в антидубле считает все совпадения, хотя нужен факт «есть ли хоть одно».
4. **Поиск** (`LIKE '%q%'`) грузит все совпадения в память разом.
5. **`NotificationRepository.observeAll()`** — объявлен, нигде не используется (мёртвый код, грузил бы всю таблицу).

---

## Правки

### 1. Зависимости — Paging 3 (Room + Compose)
- [gradle/libs.versions.toml](gradle/libs.versions.toml): добавить `paging = "3.3.5"` (в рамках AGP 8.9), библиотеки:
  - `androidx-paging-runtime` (`androidx.paging:paging-runtime`)
  - `androidx-paging-compose` (`androidx.paging:paging-compose`)
  - `androidx-room-paging` (`androidx.room:room-paging`, версия из `room`)
- [app/build.gradle.kts](app/build.gradle.kts): добавить три `implementation(...)` в блок Room/Compose.

### 2. Индексы БД — миграция v5 → v6
- [NotificationEntity.kt](app/src/main/java/com/notikeep/data/local/entity/NotificationEntity.kt): добавить в `indices`:
  - `Index(value = ["packageName", "title"])` — ускоряет `countRecentByText`/`countRecentByTitle` (антидубль).
  - `Index("sbnKey")` — ускоряет `findBySbnKey` (BY_KEY/COMBINED).
- [NotikeepDatabase.kt](app/src/main/java/com/notikeep/data/local/NotikeepDatabase.kt): `version = 6`, `MIGRATION_5_6`:
  - `CREATE INDEX IF NOT EXISTS index_notifications_packageName_title ON notifications(packageName, title)`
  - `CREATE INDEX IF NOT EXISTS index_notifications_sbnKey ON notifications(sbnKey)`
- [DataModule.kt](app/src/main/java/com/notikeep/di/DataModule.kt): добавить `MIGRATION_5_6` в `addMigrations(...)`.
- Примечание: имена индексов должны совпадать с тем, что генерирует Room по `@Index`, иначе `validateMigration` упадёт. Сверю по сгенерированной схеме/ожиданиям Room (`index_notifications_<col1>_<col2>`).

### 3. Антидубль: `COUNT(*)` → `EXISTS`/`LIMIT 1`
- [NotificationDao.kt](app/src/main/java/com/notikeep/data/local/dao/NotificationDao.kt): заменить два `countRecent*` на возврат `Boolean` через `EXISTS(...)`:
  - `existsRecentByText(pkg, title, text, since): Boolean` → `SELECT EXISTS(SELECT 1 FROM notifications WHERE ... LIMIT 1)`
  - `existsRecentByTitle(pkg, title, since): Boolean`
- Обновить интерфейс/impl репозитория ([NotificationRepository.kt](app/src/main/java/com/notikeep/domain/repository/NotificationRepository.kt), [NotificationRepositoryImpl.kt](app/src/main/java/com/notikeep/data/repository/NotificationRepositoryImpl.kt)) и [DeduplicateUseCase.kt](app/src/main/java/com/notikeep/domain/usecase/DeduplicateUseCase.kt) (методы `hasRecentSameText/Title` теперь зовут `exists*`).
- Обновить [DeduplicateUseCaseTest.kt](app/src/test/java/com/notikeep/domain/usecase/DeduplicateUseCaseTest.kt): моки `countRecent*` → `existsRecent*` (Boolean).

### 4. Детальный экран → Paging 3
- **DAO** ([NotificationDao.kt](app/src/main/java/com/notikeep/data/local/dao/NotificationDao.kt)): рядом с существующими добавить пагинируемые версии:
  - `pagingByPackage(packageName): PagingSource<Int, NotificationEntity>`
  - `pagingFavoritesByPackage(packageName): PagingSource<Int, NotificationEntity>`
  - (старые `observeByPackage`/`observeFavoritesByPackage` пока оставить — вдруг где-то в тестах; но из VM уйдут.)
- **Repository**: добавить методы, возвращающие `Flow<PagingData<NotificationRecord>>`:
  - в [NotificationRepository.kt](app/src/main/java/com/notikeep/domain/repository/NotificationRepository.kt): `fun pagedByPackage(pkg, favoritesOnly): Flow<PagingData<NotificationRecord>>`
  - в [NotificationRepositoryImpl.kt](app/src/main/java/com/notikeep/data/repository/NotificationRepositoryImpl.kt): собрать `Pager(PagingConfig(pageSize=50, prefetchDistance=25, enablePlaceholders=false)) { dao.pagingByPackage(...) }`, `.flow.map { it.map(entity::toDomain) }`.
  - Замечание о слоях: `PagingData`/`Pager` — это `androidx.paging` (не Android UI, чистая JVM-библиотека), допустимо в domain-интерфейсе; это стандартная практика. Маппинг `PagingData.map` — в data-слое.
- **ViewModel** ([AppNotificationsViewModel.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsViewModel.kt)):
  - `val items: Flow<PagingData<NotificationRecord>> = repository.pagedByPackage(...).cachedIn(viewModelScope)`.
  - Заголовок приложения (`appLabel`) больше нельзя брать из `records.firstOrNull()` (список теперь пагинируется). Взять из отдельного лёгкого запроса: `dao`-метод `appLabelFor(pkg): String?` (`SELECT appLabel ... LIMIT 1`) через репозиторий, либо из `PackageManager` (уже есть resolve в других местах). Выберу лёгкий DB-запрос `observeAppLabel(pkg): Flow<String?>`.
  - `toggleFavorite`/`delete` остаются как есть (работают по id).
- **Экран** ([AppNotificationsScreen.kt](app/src/main/java/com/notikeep/presentation/appdetail/AppNotificationsScreen.kt)):
  - `val items = viewModel.items.collectAsLazyPagingItems()`.
  - `LazyColumn { items(items.itemCount) { index -> items[index]?.let { row(it) } } }` (или `items(count = items.itemCount, key = items.itemKey { it.id })`).
  - Обработать `loadState` (первичная загрузка → спиннер; пусто → текущее поведение).

### 5. Поиск → Paging 3
- **DAO**: `pagingSearch(query, from, to): PagingSource<Int, NotificationEntity>` (тот же `LIKE`, но `PagingSource`).
- **Repository/UseCase**: `search(...)` → `Flow<PagingData<NotificationRecord>>`; пустой запрос → `flowOf(PagingData.empty())`.
  - [SearchNotificationsUseCase.kt](app/src/main/java/com/notikeep/domain/usecase/SearchNotificationsUseCase.kt) и [SearchNotificationsUseCaseTest.kt](app/src/test/java/com/notikeep/domain/usecase/SearchNotificationsUseCaseTest.kt) обновить.
- **ArchiveViewModel/Screen** ([ArchiveViewModel.kt](app/src/main/java/com/notikeep/presentation/archive/ArchiveViewModel.kt), [ArchiveScreen.kt](app/src/main/java/com/notikeep/presentation/archive/ArchiveScreen.kt)): результаты поиска отрисовать через `collectAsLazyPagingItems()`. Сводки по приложениям (`observeAppSummaries`) НЕ трогаем — они уже агрегируются в БД и малы (одна строка на приложение).
  - Учесть: сейчас `state` объединяет summaries+searchResults в один `combine`. Вынести поток поиска отдельно, чтобы отдать `LazyPagingItems` в UI, а `combine` оставить для сводок/флагов.

### 6. Убрать мёртвый код
- Удалить `observeAll()` из [NotificationRepository.kt](app/src/main/java/com/notikeep/domain/repository/NotificationRepository.kt), [NotificationRepositoryImpl.kt](app/src/main/java/com/notikeep/data/repository/NotificationRepositoryImpl.kt) и `dao.observeAll()` из [NotificationDao.kt](app/src/main/java/com/notikeep/data/local/dao/NotificationDao.kt) (проверить, что не используется в тестах — grep показал: не используется).

---

## Порядок выполнения
1. Зависимости (Paging).
2. Индексы + миграция v6 (быстрый выигрыш, изолировано).
3. Антидубль EXISTS + правка тестов.
4. Детальный экран на Paging.
5. Поиск на Paging.
6. Удаление мёртвого кода.
7. Сборка `:app:testReleaseUnitTest` + `:app:lintRelease`.

## Не входит (осознанно)
- Не режем историю/поиск лимитами (пользователь просил не ограничивать).
- Retention оставляем пользовательским (7/30/90 дней) — без принудительного «максимум N строк».
- Границы «начала дня» в DailySummaryController — вне рамок этой задачи.

## Риски
- Совпадение имён индексов с ожиданиями Room в миграции — проверю сборкой (Room валидирует схему; при расхождении тест/ран упадёт с понятной ошибкой, поправлю имя).
- Paging меняет сигнатуры в цепочке repo→VM→screen для деталей и поиска — затрагивает несколько тестов, обновлю их.
