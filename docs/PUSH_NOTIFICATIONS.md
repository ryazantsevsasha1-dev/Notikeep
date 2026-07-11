# Push-уведомления пользователям — инструкция

Цель: отправлять кампании («что нового», реактивация «вы давно не заходили») всем или
сегментам пользователей.

## Рекомендуемый стек: AppMetrica Push SDK

Почему именно он:
- бесплатный, кампании запускаются из того же кабинета AppMetrica, где уже есть аналитика —
  можно слать push по сегментам («не открывал приложение 7 дней», «версия < X»);
- поддерживает сразу несколько транспортов: **Firebase (FCM)** для устройств с Google-сервисами
  и **RuStore Push** для устройств без них (Huawei — HMS, опционально);
- статистика доставок/открытий сразу в отчётах AppMetrica.

## Что уже подготовлено в коде (сделано)

- Разрешение `POST_NOTIFICATIONS` добавлено в манифест (обязательно с Android 13).
- `MainActivity` запрашивает это разрешение при первом запуске — без него push просто
  не показываются на Android 13+.
- AppMetrica уже инициализируется в `NotikeepApp` — Push SDK цепляется к ней автоматически.

## Что нужно сделать вам (кабинеты)

### Шаг 1. Firebase (транспорт для устройств с Google-сервисами)
1. console.firebase.google.com → создать проект → добавить Android-приложение `com.notikeep`.
2. Скачать `google-services.json` → положить в `app/`.
3. В Firebase Console → Project Settings → Cloud Messaging — ничего дополнительно не нужно,
   AppMetrica заберёт токен сама.

### Шаг 2. RuStore Push (устройства без Google, обязательно для RuStore-аудитории)
1. console.rustore.ru → ваше приложение → Push-уведомления → включить.
2. Получить `project id` RuStore.

### Шаг 3. Связка в AppMetrica
1. appmetrica.yandex.ru → приложение → Настройки → Push-уведомления.
2. Загрузить серверный ключ FCM (Service Account JSON из Firebase) и ключ RuStore.

## Что после этого добавить в код (скажите — сделаю)

1. Зависимости:
   ```toml
   appmetrica-push = { group = "io.appmetrica.analytics", name = "push", version = "4.x" }
   appmetrica-push-firebase = { group = "io.appmetrica.analytics", name = "push-provider-firebase", version = "4.x" }
   appmetrica-push-rustore = { group = "io.appmetrica.analytics", name = "push-provider-rustore", version = "4.x" }
   ```
   плюс плагин `com.google.gms.google-services` в `app/build.gradle.kts`.
2. Инициализация после `AppMetrica.activate`:
   ```kotlin
   AppMetricaPush.activate(applicationContext)
   ```
3. Готово: канал уведомлений SDK создаёт сам, иконку возьмёт из манифеста
   (добавим `appmetrica_notification_icon` — маленькая монохромная иконка-колокольчик).

Без файлов из шагов 1–2 сборка с этими зависимостями упадёт, поэтому код добавляем после
того, как появятся `google-services.json` и ключи.

## Как отправить первый push
Кабинет AppMetrica → Push-кампании → «Создать кампанию»:
выбрать сегмент (например, «все») → текст/заголовок/deeplink → «тихие часы» →
тест на своё устройство (по appmetrica_device_id) → запуск. Статистика доставок и открытий —
на странице кампании.

## Важные ограничения/этика
- Push работает даже при закрытом приложении (это серверные push, не наш listener).
- Не путать с локальными уведомлениями самого Notikeep — их приложение не шлёт.
- Частота: для утилиты 1–2 кампании в месяц максимум, иначе отписки/удаления.
