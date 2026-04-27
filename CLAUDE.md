# CLAUDE.md - Jiucaihua Development Guide

## Project

Jiucaihua is a personal investment management Android app built with Kotlin + Jetpack Compose.

## Architecture

- Clean Architecture + MVVM
- Hilt for dependency injection
- Room for local database
- Retrofit for network requests

## Development Rules

1. **Propose before execute** - Always propose a solution before implementing changes
2. **Build and test** - After completing changes, build and install to device:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Ask before ending** - Use AskUserQuestion when finished or encountering issues

## Documentation

| Document | Content |
|----------|---------|
| `docs/quick-start.md` | Development environment setup |
| `docs/architecture-overview.md` | Architecture overview |
| `docs/data-sources.md` | Data source interfaces |
| `docs/ai-data-model.md` | AI Agent data model |

## Reference

`work_dirs/leek-fund` contains reference source code.