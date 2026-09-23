# Itera — Android (Kotlin + Jetpack Compose)

A practical trainer for focus, planning, learning, habits, and reflection.

UI-прототип по дизайну с холста Itera: все основные экраны, навигация, светлая/тёмная тема,
4 языка (EN · RU · DE · ES). Бэкенда и хранения данных пока нет — состояние живёт в памяти
(`AppViewModel`), чтобы UX можно было прокликать целиком.

## Как открыть

1. Android Studio Ladybug (2024.2) или новее → **File › Open** → папка `itera-android`.
2. Дождаться Gradle Sync (JDK 17+, Android SDK 35 скачаются автоматически).
3. Запустить конфигурацию `app` на эмуляторе или устройстве (Android 8.0+, minSdk 26).

Шрифты (необязательно): положите TTF-файлы в `app/src/main/assets/fonts/` —
список имён в `README.txt` там же. Без них используется системный шрифт.

## Версии

AGP 8.7.3 · Kotlin 2.0.21 (Compose compiler plugin) · Compose BOM 2024.12.01 ·
Navigation Compose 2.8.5 · compileSdk/targetSdk 35 · minSdk 26.
Studio может предложить обновить версии — это безопасно.

## Структура

```
app/src/main/java/com/itera/app/
├── MainActivity.kt          AppCompatActivity (нужна для смены языка на всех версиях Android)
├── data/AppViewModel.kt     состояние: программа, шаги дня, журнал практики, мастерство, демо-данные
├── model/Model.kt           Skill, Technique (14 техник), Program (разблокировка по дням), Mastery
└── ui/
    ├── IteraApp.kt          NavHost, маршруты, нижняя навигация Today | Train | Progress | You
    ├── theme/               токены цвета (светлые/тёмные), типографика, IteraTheme
    ├── components/          кнопки, карточки, чипы, шкала мастерства, иконки (ImageVector)
    └── screens/
        ├── Onboarding.kt    Welcome, цели, ритм, первая неделя, выбор языка (bottom sheet)
        ├── Today.kt         главный экран: одно действие + 3 шага дня
        ├── Exercise.kt      интро упражнения, 2-minute rule, экран результата
        ├── Focus.kt         таймер Pomodoro / Deep Work (всегда тёмный, экран не гаснет)
        ├── Reflection.kt    вечерняя рефлексия (3 вопроса), «день завершён»
        ├── Practice.kt      комбинация, Eisenhower 2×2, Feynman (+ слот для AI), повторение,
        │                    Premortem, Habit stacking
        ├── Train.kt         программа недели, библиотека, карточка техники
        ├── Progress.kt      5 навыков, регулярность, история с календарём
        └── Profile.kt       настройки: ритм, темп, язык, тема, уведомления
```

## Что работает

- Полный путь дня: онбординг → упражнение → результат → фокус-сессия → рефлексия →
  «день завершён» → следующий день. Ответ «что изменю завтра» появляется утром на Today.
- Программа: новая техника каждый день (дни 1–13), день 14 — комбинация. День программы
  двигается, когда пользователь тренируется, а не по календарю.
- Мастерство техники: Met → Practiced (3 разных дня) → Applied (6 практик) →
  Integrated (использована в дне комбинаций). Никаких очков за вход.
- **You › Load demo data** или «Explore with demo data» на Welcome — сразу день 9 с историей.
- Языки: переключатель на Welcome и в You › Language. Используется
  `AppCompatDelegate.setApplicationLocales` + `locales_config.xml`, поэтому язык также
  доступен в системных настройках приложения (Android 13+). Даты и время форматируются
  по локали, множественные формы — через `plurals` (в RU: one/few/many).
- Тема: System / Light / Dark в You › Appearance.

## Что дальше

- Хранение: Room (журнал, ответы) + DataStore (настройки) вместо in-memory ViewModel.
- Настоящие time picker'ы, уведомления (WorkManager / AlarmManager), экспорт журнала.
- AI-обратная связь для Feynman: разметка слота уже готова (`FeynmanFeedbackScreen`).
- Экраны для 5-second rule, Information diet, 1% improvement, Two-list сейчас используют
  общий шаблон упражнения («I did it»); их можно сделать интерактивными по образцу Practice.kt.

Проект собран без запуска Gradle (в среде генерации не было доступа к Android SDK),
поэтому при первой синхронизации возможны мелкие правки импортов — Studio подскажет.
