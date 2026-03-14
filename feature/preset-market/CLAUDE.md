# feature:preset-market

Firebase 기반 프리셋 마켓. 목록/검색/상세/업로드, Google 로그인, AdMob.

## 패키지 구조

```
org.comon.streamlauncher.preset_market
├── PresetMarketContract.kt   # PresetMarketState / Intent / SideEffect
├── PresetDetailContract.kt
├── MarketSearchContract.kt
├── PresetMarketViewModel.kt
├── PresetDetailViewModel.kt
├── MarketSearchViewModel.kt
├── navigation/
│   └── MarketRoute.kt        # HOME, SEARCH(?query=), DETAIL/{id}
├── ui/
│   ├── MarketHomeScreen.kt
│   ├── MarketSearchResultScreen.kt
│   ├── PresetDetailScreen.kt
│   ├── MarketPresetCard.kt
│   ├── MarketPresetListItem.kt
│   ├── AdmobBanner.kt
│   ├── GoogleSignInHandler.kt
│   └── UploadPresetDialog.kt
└── download/
    └── ...
```

## 네비게이션

| 라우트 | 화면 |
|--------|------|
| `MarketRoute.HOME` | 마켓 홈 (Paging 목록) |
| `MarketRoute.SEARCH` | 검색 결과 (query 파라미터) |
| `MarketRoute.DETAIL` | 프리셋 상세 (presetId 파라미터) |

- MainActvity NavHost 에서 `preset_market` 라우트 진입 → 내부 NavHost 로 처리

## Firebase 연동

- `MarketPresetRepositoryImpl` (core:data): Firestore 페이징, Storage 업로드/다운로드
- Paging: `MarketPresetPagingSource`, `SearchMarketPresetPagingSource`
- Auth: Google Sign-In → Firebase Auth 연동 (`GoogleSignInHandler`)
- BOM 34.x 주의: `-ktx` 접미사 없는 아티팩트 사용

## AdMob

- `AdmobBanner` Composable: `AndroidView` 로 `AdView` 래핑
- `MobileAds.initialize()` 는 `StreamLauncherApplication` 에서 최초 1회 실행

## Google 로그인 (Credentials API)

- `CredentialManager` API 사용 (play-services-auth + credentials + googleid)
- `local.properties` 의 `google.web.client.id` → BuildConfig 경유
- 로그인 상태: `GetCurrentMarketUserUseCase` 로 확인

## Paging 3 규칙

- `PagingSource` 는 `core:data/paging/` 에 위치
- ViewModel 에서 `Pager` 생성 → `cachedIn(viewModelScope)`
- UI: `LazyPagingItems` + `collectAsLazyPagingItems()`

## 주의사항

- 패키지명 언더스코어: `org.comon.streamlauncher.preset_market`
- 업로드는 `feature:settings` 의 `UploadPresetToMarketUseCase` 경유 (중복 구현 금지)
- 다운로드: `PresetDownloadService` (app 모듈의 Foreground Service) 경유
