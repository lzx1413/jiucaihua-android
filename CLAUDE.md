# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Jiucaihua (九财花) is a personal investment management Android app. Kotlin 2.2.10 + Jetpack Compose, targeting API 36, min API 26, JDK 17.

## Build & Run

```bash
./gradlew assembleDebug                                    # Build debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk  # Install to device
./gradlew test                                             # Run all unit tests
./gradlew test --tests "com.jiucaihua.app.domain.usecase.CheckAlertsUseCaseTest"  # Single test class
./gradlew lint                                             # Lint check
./gradlew clean                                            # Clean build artifacts
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

**Clean Architecture + MVVM** — three layers with strict dependency direction:

```
Presentation (Compose UI + ViewModel + StateFlow)
    ↓ depends on
Domain (UseCase + Repository interfaces + Models)
    ↓ depends on
Data (Repository impls + Retrofit APIs + Room DAOs + Entities)
```

**Data flow:** Remote APIs → Retrofit Services → Repository impls (parse + cache to Room) → Repository interfaces → UseCases (orchestrate repos, apply business logic) → ViewModel (MutableStateFlow) → Compose UI collects StateFlow.

Package root: `app/src/main/java/com/jiucaihua/app/`

### DI (Hilt)

Four modules in `di/`:

| Module | Provides |
|--------|----------|
| `AppModule` | Room database (v13, with migration chain v4→v13), all DAOs, SharedPreferences (standard + encrypted) |
| `NetworkModule` | 11 Retrofit instances with specialized OkHttpClient configs (`@Named("sina")`, `@Named("tencent")`, etc.) |
| `RepositoryModule` | 11 `@Binds` mappings: interface → impl |
| `AiModule` | LLM client, config store, 14 tools as `@IntoSet Set<ToolExecutor>` |

### Room Database

Database name: `jiucaihua_database`, version 13. 8 entities (holdings, stock_cache, fund_cache, alerts, alert_records, watchlist, news_flash, portfolio_snapshots).

**Migration pattern:** All migrations defined inline in `AppModule.kt`. When adding a new column/table, add a `MIGRATION_N_N+1` and append it to the `addMigrations` list. Schema export is disabled.

### Network Layer

Multiple data sources with encoding quirks — handled by OkHttp interceptors in `data/remote/interceptor/`:

| Source | Encoding | Interceptor |
|--------|----------|-------------|
| Sina (A-shares) | GB18030 | `GBKResponseInterceptor` + `SinaRefererInterceptor` |
| Tencent (HK stocks) | GBK | `GBKResponseInterceptor` |
| 天天基金 (Funds) | UTF-8 | JSONP regex extraction in repository |
| 东方财富 | UTF-8 | Direct JSON parse |

Most Retrofit services use `ScalarsConverterFactory` (plain text responses). Moshi used for select JSON endpoints.

### P&L Calculation (in `GetPortfolioUseCase`)

```
Stock P&L = (currentPrice - costPrice) × shares
HK Stock P&L(CNY) = (currentPrice - costPrice) × shares × HKD/CNY rate
Fund P&L = (estimatedNAV - costNAV) / costNAV × investedAmount
```

### Refresh Strategy

| Condition | Interval |
|-----------|----------|
| Foreground + market open | 10 seconds |
| Foreground + market closed | stop |
| Background + market open | 15 min (WorkManager) |
| Background + market closed | skip |

Market session detection in `IsMarketOpenUseCase` — A-shares 09:15-11:30/13:00-15:00, HK 09:00-12:00/13:00-16:00.

### WorkManager Background Tasks

Three periodic workers (15-min intervals, require network):
- `QuoteRefreshWorker` — refresh quotes during market hours + record portfolio snapshot
- `AlertCheckWorker` — check price thresholds → send notifications
- `NewsSyncWorker` — fetch from 6 news sources → cache to Room → cleanup old articles (24h cutoff)

### AI Agent System

`AiAgentOrchestrator` runs an iterative tool-use loop (max 4 iterations):
1. Build system prompt → send to LLM with 14 tool definitions (JSON Schema)
2. If response contains `tool_call` → `ToolRegistry.execute(name, args)` → append result → re-query
3. Return final text answer

LLM client: `OpenAiCompatibleLlmAgentClient` — supports DeepSeek, OpenAI, or any OpenAI-compatible endpoint. API keys stored in `EncryptedAiConfigStore` (AES-256-GCM EncryptedSharedPreferences).

Tool pattern: implement `ToolExecutor` interface (`definition` + `execute`), register via `@IntoSet` in `AiModule`.

## Key Modules

- **Portfolio** — Holdings + real-time quotes (A-share, HK, US, fund, gold). Compose navigation start destination.
- **News** — 6-channel aggregation (CLS, XuanGuBao, WallstreetCN, Jin10, EastMoney, STCN). Topic filtering via `NewsTopic` enum.
- **AI** — LLM chat with 14 tool executors for portfolio analysis, K-line, news, alerts, what-if scenarios.
- **Alerts** — Price threshold rules + push notifications.
- **Watchlist** — Followed securities with quick quote view.
- **Market** — Indices overview + K-line + fund flow.

## Development Rules

1. **Propose before execute** — Always propose a solution before implementing changes
2. **Build and test** — After completing changes, build and install to device
3. **Ask before ending** — Use AskUserQuestion when finished or encountering issues
4. **Commit style** — Use Angular/Conventional Commits format (e.g. `feat(scope): summary`) and do not add co-author trailers

## Documentation

| Document | Content |
|----------|---------|
| `docs/quick-start.md` | Development environment setup |
| `docs/architecture-overview.md` | Architecture overview with data flow diagrams |
| `docs/data-sources.md` | Data source interfaces and encoding details |
| `docs/tool-provider.md` | CETP Tool Provider protocol |
| `docs/ai-data-model.md` | AI Agent data models |

## Reference

`work_dirs/leek-fund` contains reference source code from the Leek Fund IntelliJ plugin.
