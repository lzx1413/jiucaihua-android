# 九财花

<img src="docs/icon.svg" width="120" height="120" alt="九财花图标">

[English](README.md) | 中文

九财花是一款个人投资管理 Android App，帮助用户跟踪股票、基金持仓、投资组合表现和实时行情。

## 功能

- **持仓管理** - 记录 A 股、港股、美股、基金和黄金的成本价与持仓数量
- **实时行情** - A 股、港股、美股、基金的实时报价
- **盈亏计算** - 自动计算浮动盈亏、收益率
- **K线图表** - 日K/周K/月K 蜡烛图
- **价格预警** - 设置价格阈值，推送通知提醒
- **多源资讯** - 6 大渠道聚合新闻（财联社/选股宝/华尔街见闻/金十/东方财富/证券时报），支持按源筛选
- **自选关注** - 关注证券，快速查看行情
- **大盘概览** - A 股/港股/美股指数、K 线图、资金流向
- **黄金持仓** - 黄金价格监控与持仓管理
- **AI助手** - 智能投资问答助手（支持多种 LLM），可调用投资数据工具
- **CETP Tool Provider** - 通过 ContentProvider 向兼容客户端暴露只读投资数据
- **深色模式** - 支持跟随系统主题
- **数据备份** - 本地数据备份与恢复

## 技术栈

- Kotlin + Jetpack Compose
- Clean Architecture + MVVM
- Hilt（依赖注入）
- Room（本地数据库）
- Retrofit + OkHttp（网络请求）
- WorkManager（后台任务）

## 构建

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 要求

- Android 8.0+（API 26）
- JDK 17

## 文档

- [快速上手](docs/quick-start.md) - 开发环境搭建
- [架构概览](docs/architecture-overview.md) - 项目架构说明
- [数据源](docs/data-sources.md) - 行情数据接口
- [Tool Provider](docs/tool-provider.md) - CETP 工具提供者接口
- [FAQ](docs/faq.md) - 常见问题

## License

MIT
