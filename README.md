# 九财花 (Jiucaihua)

一款个人投资管理 Android App，帮助用户跟踪股票、基金持仓和实时行情。

## 功能

- **持仓管理** - 记录股票、基金的成本价和持仓数量
- **实时行情** - A股、港股、基金的实时报价
- **盈亏计算** - 自动计算浮动盈亏、收益率
- **K线图表** - 日K/周K/月K 蜡烛图
- **价格预警** - 设置价格阈值，推送通知提醒
- **市场资讯** - 浏览市场新闻资讯
- **AI助手** - 智能投资问答助手（支持多种 LLM）
- **深色模式** - 支持跟随系统主题

## 技术栈

- Kotlin + Jetpack Compose
- Clean Architecture + MVVM
- Hilt (依赖注入)
- Room (本地数据库)
- Retrofit + OkHttp (网络请求)
- WorkManager (后台任务)

## 构建

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 要求

- Android 8.0+ (API 26)
- JDK 17

## 文档

- [快速上手](docs/quick-start.md) - 开发环境搭建
- [架构概览](docs/architecture-overview.md) - 项目架构说明
- [数据源](docs/data-sources.md) - 行情数据接口
- [FAQ](docs/faq.md) - 常见问题

## License

MIT