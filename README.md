# Jiucaihua

<img src="docs/icon.svg" width="120" height="120" alt="Jiucaihua icon">

English | [中文](RADME_zh.md)

Jiucaihua is a personal investment management Android app that helps users track stock and fund holdings, portfolio performance, and real-time market quotes.

## Features

- **Portfolio management** - Track cost prices and positions for A-shares, HK stocks, US stocks, funds, and gold
- **Real-time quotes** - Real-time prices for A-shares, HK stocks, US stocks, and funds
- **Profit and loss** - Automatically calculate floating P&L and return rates
- **K-line charts** - Daily, weekly, and monthly candlestick charts
- **Price alerts** - Set price thresholds and receive push notifications
- **Multi-source news** - Aggregate news from 6 channels (CLS, XuanGuBao, WallstreetCN, Jin10, EastMoney, STCN), with source filtering
- **Watchlist** - Follow securities and quickly view quotes
- **Market overview** - A-share, HK, and US market indices, K-line charts, and fund flow
- **Gold holdings** - Monitor gold prices and manage gold positions
- **AI assistant** - Intelligent investment Q&A assistant with multiple LLM providers and investment data tools
- **CETP Tool Provider** - Expose read-only investment data to compatible clients through ContentProvider
- **Dark mode** - Supports the system theme
- **Data backup** - Local data backup and restore

## Tech Stack

- Kotlin + Jetpack Compose
- Clean Architecture + MVVM
- Hilt (dependency injection)
- Room (local database)
- Retrofit + OkHttp (networking)
- WorkManager (background tasks)

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Requirements

- Android 8.0+ (API 26)
- JDK 17

## Documentation

- [Quick Start](docs/quick-start.md) - Development environment setup
- [Architecture Overview](docs/architecture-overview.md) - Project architecture
- [Data Sources](docs/data-sources.md) - Market data interfaces
- [Tool Provider](docs/tool-provider.md) - CETP Tool Provider interface
- [FAQ](docs/faq.md) - Frequently asked questions

## License

MIT
