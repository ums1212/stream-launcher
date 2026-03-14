# feature:widget

홈 화면 위젯 지원. `AppWidgetHost` 기반 위젯 추가/제거/배치.

## 패키지 구조

```
org.comon.streamlauncher.widget
├── WidgetContract.kt   # WidgetState / WidgetIntent / WidgetSideEffect
├── WidgetViewModel.kt
└── ui/
    └── WidgetScreen.kt
```

## WidgetViewModel 책임

- `AppWidgetHost` 와 연동하여 위젯 목록 관리
- 위젯 추가/제거 intent 처리
- `BaseViewModel<WidgetState, WidgetIntent, WidgetSideEffect>` 상속

## AppWidgetHost 연동

- `AppWidgetHost` 인스턴스는 `MainActivity` 에서 생성 및 수명주기 관리
  - `onStart()`: `appWidgetHost.startListening()`
  - `onStop()`: `appWidgetHost.stopListening()`
- WidgetViewModel 에는 Host 참조를 직접 주입하지 않음 (ViewModel 레이어 분리)

## 주의사항

- 위젯은 Android `AppWidget` API 사용 → `core:domain` 에 Android 타입 노출 금지
- 위젯 상태(추가된 위젯 목록 등) 영속화 필요 시 `SettingsRepository` 또는 별도 DataStore 키 사용
