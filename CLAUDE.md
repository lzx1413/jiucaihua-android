# CLAUDE.md - 九财花开发指南

## 项目

九财花 (Jiucaihua) 是一款个人投资管理 Android App，使用 Kotlin + Jetpack Compose 开发。

## 架构

- Clean Architecture + MVVM
- Hilt 依赖注入
- Room 本地数据库
- Retrofit 网络请求

## 工作流程

1. **先提案再执行** - 每次任务先提案，确认后再执行
2. **构建后测试** - 完成后安装到真机测试：
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **结束前询问** - 完成或有疑问时，询问下一步

## 文档

| 文档 | 内容 |
|------|------|
| `docs/quick-start.md` | 开发环境搭建 |
| `docs/architecture-overview.md` | 架构说明 |
| `docs/data-sources.md` | 数据源接口 |
| `docs/ai-data-model.md` | AI Agent 数据规范 |

## 参考

`work_dirs/leek-fund` 是参考源码目录。