# 开发者快速上手指南

## 环境要求

| 项目 | 版本要求 |
|------|----------|
| Android Studio | Hedgehog (2023.1) 或更高 |
| JDK | 17 |
| Android SDK | API 35 (compileSdk) |
| 最低运行设备 | Android 8.0 (API 26) |
| Gradle | 8.x (由 gradlew 管理) |

## 项目克隆与构建

```bash
# 克隆项目
git clone <项目地址>
cd Jiucaihua

# 构建 Debug APK
./gradlew assembleDebug

# 安装到已连接设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 目录结构

```
app/src/main/java/com/jiucaihua/app/
│
├── Jiucaihualication.kt      # Hilt 应用入口
├── MainActivity.kt         # 单 Activity，承载 Compose 导航
│
├── data/                   # 数据层
│   ├── local/              #   Room 数据库
│   │   ├── AppDatabase.kt
│   │   ├── dao/            #   数据访问对象
│   │   └── entity/         #   数据库实体
│   ├── remote/             #   远程数据源
│   │   ├── api/            #   Retrofit 接口
│   │   ├── dto/            #   数据传输对象
│   │   └── interceptor/    #   OkHttp 拦截器
│   └── repository/         #   Repository 实现
│
├── domain/                 # 领域层
│   ├── model/              #   领域模型
│   ├── repository/         #   Repository 接口
│   └── usecase/            #   UseCase
│
├── presentation/           # 展示层
│   ├── navigation/         #   导航定义
│   ├── theme/              #   Material 3 主题
│   ├── portfolio/          #   投资组合页
│   ├── detail/             #   详情页（K线）
│   ├── holdings/           #   持仓管理页
│   ├── alerts/             #   预警管理页
│   ├── settings/           #   设置页
│   ├── ai/                 #   AI 助手页
│   └── article/            #   资讯阅读页
│
├── ai/                     # AI Agent 层
│   ├── config/             #   AI 配置
│   ├── orchestrator/       #   Agent 调度器
│   ├── tool/               #   ToolExecutor 工具集
│   └── usecase/            #   AI 专用 UseCase
│
├── worker/                 # 后台任务
│   ├── QuoteRefreshWorker.kt
│   └── AlertCheckWorker.kt
│
└── di/                     # 依赖注入模块
    ├── AppModule.kt
    ├── NetworkModule.kt
    └── RepositoryModule.kt
```

## 常用命令

```bash
# 编译
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK（需签名配置）

# 清理
./gradlew clean

# Lint 检查
./gradlew lint

# 测试
./gradlew test                   # 单元测试
./gradlew connectedAndroidTest   # 设备测试

# 安装运行
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jiucaihua.app/.MainActivity
```

## 关键技术点

### 1. 数据源编码处理

| 数据源 | 编码 | 处理方式 |
|--------|------|----------|
| 新浪 A 股 | GB18030 | OkHttp 拦截器解码 |
| 腾讯港股 | GBK | OkHttp 拦截器解码 |
| 天天基金 | UTF-8 | JSONP 正则提取 |
| 东方财富 | UTF-8 | JSON 直接解析 |

相关文件：`data/remote/interceptor/GBKResponseInterceptor.kt`

### 2. 盈亏计算公式

```kotlin
// 股票盈亏
浮动盈亏 = (当前价 - 成本价) * 持仓股数
收益率 = (当前价 - 成本价) / 成本价 * 100

// 港股（需汇率折算）
浮动盈亏(CNY) = (当前价 - 成本价) * 持仓股数 * HKD/CNY汇率

// 基金盈亏
浮动盈亏 = (估算净值 - 成本净值) / 成本净值 * 投入金额
```

相关文件：`domain/usecase/GetPortfolioUseCase.kt`

### 3. 交易时段检测

A股交易时段：
- 上午：09:15 - 11:30（含集合竞价）
- 午休：11:30 - 13:00（停止刷新）
- 下午：13:00 - 15:00

港股交易时段：
- 上午：09:00 - 12:00
- 午休：12:00 - 13:00
- 下午：13:00 - 16:00

相关文件：`domain/usecase/IsMarketOpenUseCase.kt`

### 4. 刷新策略

| 场景 | 刷新间隔 |
|------|----------|
| 前台 + 开盘 | 10 秒 |
| 前台 + 收盘 | 停止 |
| 后台 + 开盘 | 15 分钟（WorkManager） |
| 后台 + 收盘 | 跳过 |

### 5. 数据缓存

冷启动时优先显示 Room 缓存数据，不阻塞界面：
- `StockCacheEntity`：股票行情缓存
- `FundCacheEntity`：基金估值缓存

## 调试技巧

### 查看网络请求日志

```kotlin
// NetworkModule.kt 中已配置 OkHttp LoggingInterceptor
// 日志级别：BODY（调试用），生产环境改为 HEADERS 或 BASIC
```

### 强制刷新行情

在 Portfolio 页下拉刷新，或：
```kotlin
// PortfolioViewModel.kt
viewModel.refreshPortfolio()
```

### 模拟非交易时段

修改 `IsMarketOpenUseCase.kt` 中的时段判断逻辑，或手动设置设备时间。

### 检查数据库

```bash
# 导出数据库
adb exec-out run-as com.jiucaihua.app cat /data/data/com.jiucaihua.app/databases/app_database > app_database.db

# 使用 SQLite Browser 查看
```

## 依赖版本

关键依赖见 `gradle/libs.versions.toml`：
- Compose BOM：2024.x
- Hilt：2.x
- Room：2.x
- Retrofit：2.x
- MPAndroidChart：3.1.0

## 扩展阅读

- [架构概览](architecture-overview.md)
- [数据源接口说明](data-sources.md)
- [AI 数据模型](ai-data-model.md)
- [开发计划详版](development-plan.md)