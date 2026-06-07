# 九财花 App 开发 Roadmap

## Context

基于对九财花 Android App 全面的 code review，发现了 7 个需要修复的 Bug 和多项可优化的功能缺陷，同时识别了若干高价值新功能可增强产品体验。本 Roadmap 按 **Bug修复 → 功能优化 → 新功能** 的顺序编排，确保先修复数据准确性问题，再完善用户体验，最后拓展功能边界。

---

## Phase 1: Bug 修复（P0 级 — 数据准确性） ✅ 已完成

### 1.1 美股持仓行情获取 ✅

**问题**: `GetPortfolioUseCase` 中 `MarketType.US_STOCK` 分支直接返回 `holding`，不注入行情数据。美股持仓的 `currentPrice`、`changePercent`、`marketValue`、`earnings` 全部为0或创建时的值。

**涉及文件**:
- `domain/usecase/GetPortfolioUseCase.kt` — 添加 `fetchUSStockQuotes()` 方法，在 `getPortfolioWithQuotes()` 和 `getPortfolioFromCache()` 中并行获取美股行情并注入持仓
- `domain/usecase/CheckAlertsUseCase.kt` — 已有美股行情获取逻辑，可作为参考

**实现要点**:
- 新增 `fetchUSStockQuotes(codes: List<String>)` 私有方法，调用 `stockRepository.getUSStockQuotes()` + fallback cache
- 在 `coroutineScope` 中新增 `usDeferred = async { fetchUSStockQuotes(usStockCodes) }`
- 在持仓更新逻辑中处理 US_STOCK 分支：注入 `currentPrice` 和 `changePercent`
- 在 `calcTodayEarnings()` 中为 US_STOCK 分支添加实际计算（需考虑USD→CNY汇率）
- 添加 USD→CNY 汇率获取逻辑（类似 HKD→CNY）

### 1.2 基金持仓市值/收益计算逻辑统一 ✅

**问题**: `Holding.kt` 中 FUND 类型的 `marketValue` 计算与 `earnings` 计算逻辑矛盾：
- `marketValue = currentPrice * holdingShares` (份额×净值)
- `earnings = (currentPrice / costPrice) * holdingAmount - holdingAmount` (收益率×投入金额)
- 两者对"成本"的基准不一致，当 `costPrice ≠ 1.0` 时市值和收益矛盾

**涉及文件**:
- `domain/model/Holding.kt` — 修正 FUND 的 `earnings` 和 `marketValue` 计算
- `domain/usecase/GetPortfolioUseCase.kt` — 确保 `calcCostCNY()` 与 `Holding` 逻辑一致

**实现要点**:
- 确认语义: `holdingShares` = 份额, `costPrice` = 买入时单位净值, `holdingAmount` = 投入金额 (= costPrice × holdingShares)
- 修正 `earnings`: `(currentPrice - costPrice) * holdingShares`（与股票逻辑一致）
- 修正 `earningsPercent`: `(currentPrice - costPrice) / costPrice * 100`
- 保持 `marketValue`: `currentPrice * holdingShares`（份额×当前净值）
- 确保 `calcCostCNY()` 中 FUND 使用 `costPrice * holdingShares * exchangeRate`（而非 `holdingAmount * exchangeRate`）

### 1.3 黄金K线品种symbol硬编码 ✅

**问题**: `StockRepositoryImpl.getGoldKLineData()` 和 `MarketRepositoryImpl.getGoldKLineData()` 中 `symbol = "au0"` 硬编码，所有黄金品种都返回AU0主力合约的K线数据。

**涉及文件**:
- `data/repository/StockRepositoryImpl.kt` — `getGoldKLineData()` 方法
- `data/repository/MarketRepositoryImpl.kt` — `getGoldKLineData()` 方法
- `domain/model/MarketIndex.kt` — 在 `MarketIndexCodes` 中添加 `GOLD_KLINE_SYMBOLS` 映射

**实现要点**:
- 在 `MarketIndexCodes` 中新增黄金品种→新浪期货symbol映射:
  - `hf_XAU` → 无直接K线API（可跳过或使用国际金价API）
  - `hf_GC` → `gc0` (COMEX主力)
  - `gds_AU9999` → `au0` (上海金主力)
  - `gds_AUTD` → `autd0` (黄金T+D)
  - `hf_XAG` → `ag0` (白银主力)
- 修改两个Repository的 `getGoldKLineData()` 方法，根据 `code` 参数从映射中获取对应symbol
- 对无K线数据的品种（如 hf_XAU），返回空 `KLineData` 或尝试降级到相近品种

---

## Phase 2: Bug 修复（P1 级 — 设置/交互缺陷） ✅ 已完成

### 2.1 行情刷新间隔设置不生效 ✅

**问题**: `PortfolioViewModel` 和 `DetailViewModel` 硬编码 `REFRESH_INTERVAL_MS = 10_000L`，忽略用户在设置页选择的5/10/15/30秒。

**涉及文件**:
- `presentation/portfolio/PortfolioViewModel.kt` — 从 SharedPreferences 读取刷新间隔
- `presentation/detail/DetailViewModel.kt` — 同上
- `presentation/settings/SettingsViewModel.kt` — KEY_REFRESH_INTERVAL = "refresh_interval_seconds"

**实现要点**:
- 在两个ViewModel中注入 `@Named("appPrefs") SharedPreferences`
- `startAutoRefresh()` 中: `val intervalMs = prefs.getInt("refresh_interval_seconds", 10) * 1000L`
- 用 `intervalMs` 替换硬编码 `REFRESH_INTERVAL_MS`

### 2.2 港币汇率降级值不一致 ✅

**问题**: `getPortfolioWithQuotes()` 汇率失败时默认值 1.0，`getPortfolioFromCache()` 降级值 0.92。离线/在线切换时港股市值跳变。

**涉及文件**:
- `domain/usecase/GetPortfolioUseCase.kt` — 两处汇率获取逻辑
- `presentation/detail/DetailViewModel.kt` — 汇率降级值
- `data/repository/ExchangeRateRepositoryImpl.kt` — DEFAULT_HKD_RATE 常量

**实现要点**:
- 统一降级值为 `0.92`（当前HKD/CNY约0.88-0.92区间，0.92是合理上限估算）
- `getPortfolioWithQuotes()` 中为 `rateDeferred` 添加 try-catch: `try { ... } catch { 0.92 }`
- 同时检查 `DetailViewModel.loadHolding()` 中的汇率降级值，确保三处一致

### 2.3 预警通知无跳转Intent ✅

**问题**: `AlertCheckWorker.sendNotification()` 构建的 Notification 没有 `contentIntent`，用户点击通知不会打开App。

**涉及文件**:
- `worker/AlertCheckWorker.kt` — `sendNotification()` 方法
- `MainActivity.kt` — 处理通知跳转Intent
- `presentation/navigation/AppNavHost.kt` — 支持初始导航目标

**实现要点**:
- 创建 `Intent` 打开 `MainActivity`，携带 `code` 参数导航到个股详情页
- 构建 PendingIntent: `PendingIntent.getActivity()` with `FLAG_IMMUTABLE`
- 在 Notification Builder 中添加 `.setContentIntent(pendingIntent)`
- `MainActivity` 在 `onCreate`/`onNewIntent` 中解析 Intent extras，通过 `pendingNavDestination` 传递给 `AppNavHost`

### 2.4 新闻 observeAllNews cutoff 时间冻结 ✅

**问题**: `observeAllNews()` 和 `observeNewsBySource()` 在创建 Flow 时计算 cutoff，随时间推移不会更新，导致超过24小时的旧新闻仍显示。

**涉及文件**:
- `data/local/dao/NewsFlashDao.kt` — Flow 方法去掉 cutoff 参数
- `data/repository/NewsRepositoryImpl.kt` — 在 `.map {}` 中动态计算 cutoff

**实现要点**:
- DAO 的 `getAll()` 和 `getBySourceType()` 改为不带 cutoff 参数（查询所有数据）
- 在 Repository 的 `.map {}` 内部动态计算 cutoff: `System.currentTimeMillis() - TWENTY_FOUR_HOURS`
- 在 `.map` 中过滤 `epochMillis >= cutoff`
- `refreshNews()` 中的 `deleteOlderThan()` 保留，确保旧数据被清理

---

## Phase 3: 功能优化（P2 级 — 体验完善）

### 3.1 个股详情页集成相关新闻

**现状**: `StockNewsSection` 组件已实现但未集成到 `DetailScreen`。`NewsRepository.getStockNews()` 已就绪（韭研公社API）。

**涉及文件**:
- `presentation/detail/DetailViewModel.kt` — 注入 `NewsRepository`，添加新闻加载逻辑和 UiState
- `presentation/detail/DetailScreen.kt` — 在 KLine/HoldingInfoCard 下方添加 `StockNewsSection`
- `domain/model/NewsFlash.kt` — 将 `StockArticle` 转换为 `NewsFlash` 以兼容现有组件

**实现要点**:
- `DetailUiState` 添加 `newsArticles: List<NewsFlash>`, `isNewsLoading`, `newsError`
- `DetailViewModel` 注入 `NewsRepository`，在 `loadData()` 中并行加载新闻
- `DetailScreen` 在 `HoldingInfoCard` 后添加 `StockNewsSection`
- 将 `StockArticle` → `NewsFlash` 的转换方法（adapter/extension）
- 点击新闻跳转到 `ArticleDetailScreen`

### 3.2 资讯搜索功能

**现状**: `NewsFlashDao.searchOnce()` 已实现 SQL LIKE 搜索，但 UI 没有搜索入口。

**涉及文件**:
- `presentation/portfolio/PortfolioScreen.kt` — NewsTabContent 中添加搜索栏
- `presentation/portfolio/PortfolioViewModel.kt` — 添加搜索状态和方法
- `domain/repository/NewsRepository.kt` — `searchNews()` 方法已存在

**实现要点**:
- 在 `PortfolioUiState` 中添加 `newsSearchQuery: String`, `searchedNews: List<NewsFlash>`
- 在新闻Tab顶部添加搜索框（SearchBar 或 OutlinedTextField）
- 搜索输入触发 `newsRepository.searchNews(query)` (debounce 300ms)
- 搜索结果覆盖原新闻列表显示
- 清空搜索时恢复原始观察列表

### 3.3 K线图叠加均线指标

**现状**: `TechnicalIndicators` 已计算 MA/MACD/RSI/布林带，`GetKLineDataTool` 已返回指标数据，但 `KLineChartView` 只绘制蜡烛图。

**涉及文件**:
- `presentation/detail/components/KLineChartView.kt` — 在蜡烛图上叠加MA5/MA10/MA20均线
- `domain/usecase/GetKLineDataUseCase.kt` 或 `TechnicalIndicators` — 确保返回的 KLinePoint 包含均线数据
- `domain/model/KLinePoint.kt` — 可选：添加 `ma5`, `ma10`, `ma20` 字段

**实现要点**:
- 在 KLinePoint 中添加可选均线字段: `ma5: Double?`, `ma10: Double?`, `ma20: Double?`
- 在 `GetKLineDataUseCase` 或 KLine数据获取后，用 `TechnicalIndicators.calculateMA()` 计算均线并填充
- `KLineChartView` 在蜡烛图上方绘制3条均线（不同颜色）
- 添加均线开关按钮（显示/隐藏均线）

### 3.4 STCN 新闻批量详情请求优化

**问题**: 每条STCN新闻都单独请求详情页（100条=100个HTTP请求），严重影响性能。

**涉及文件**:
- `data/repository/NewsRepositoryImpl.kt` — `fetchStcnDetail()` 和 `parseStcnMarketNewsResponse()`

**实现要点**:
- 初始加载时只使用列表数据（title/summary），不请求详情页
- 用户点击某条新闻进入 `ArticleDetailScreen` 时，再请求该条新闻的详情
- 或改为批量并发请求（限制并发数5），但仍会有延迟问题
- 最佳方案: 延迟加载详情，ArticleDetailScreen 层面才获取完整 content

---

## Phase 4: 新功能（高价值）

### 4.1 持仓收益历史曲线

**目标**: 按日记录持仓市值，画出收益走势曲线，对比大盘基准。

**新增文件**:
- `data/local/entity/PortfolioSnapshotEntity.kt` — 每日收盘时的持仓市值快照
- `data/local/dao/PortfolioSnapshotDao.kt` — 快照查询
- `domain/model/PortfolioSnapshot.kt` — 域模型
- `domain/repository/PortfolioSnapshotRepository.kt` — 仓储接口
- `data/repository/PortfolioSnapshotRepositoryImpl.kt` — 实现
- `presentation/portfolio/components/EarningsChartView.kt` — 收益曲线Compose组件
- `presentation/portfolio/PortfolioScreen.kt` — 在SummaryCard后添加收益曲线

**实现要点**:
- 每日收盘时（QuoteRefreshWorker 或新Worker），记录 `PortfolioSnapshot`: date, totalMarketValue, totalCost, earnings, categoryValues
- 在 `PortfolioSummaryCard` 下方或独立Tab展示收益曲线（7日/30日/90日/全部）
- 曲线数据从快照表查询，Canvas绘制折线图
- 可选叠加沪深300基准线（从MarketIndex快照表）

### 4.2 预警扩展 — 成交量/新高新低/均线交叉

**目标**: 增加更多预警类型，提升预警实用价值。

**涉及文件**:
- `domain/model/PriceAlert.kt` — 扩展 `AlertType` enum
- `domain/usecase/CheckAlertsUseCase.kt` — 添加新类型的判断逻辑
- `presentation/alerts/AlertsViewModel.kt` — UI适配新类型
- `presentation/alerts/components/AddAlertDialog.kt` — 添加新类型选项
- `data/local/entity/AlertEntity.kt` — 数据库迁移

**实现要点**:
- 新增 `AlertType`:
  - `VOLUME_ABOVE("成交量超过")` — 需K线数据中的volume字段
  - `NEW_HIGH("创N日新高")` — threshold=N日天数
  - `NEW_LOW("创N日新低")`
  - `MA_CROSS_ABOVE("均线金叉")` — MA5上穿MA20
  - `MA_CROSS_BELOW("均线死叉")` — MA5下穿MA20
- `CheckAlertsUseCase` 对新类型需要额外获取K线数据计算指标
- Room 数据库迁移: v8 → v9（AlertType enum 扩展）
- `AddAlertDialog` 根据选择的类型动态显示 threshold 输入提示

### 4.3 通知点击跳转完善

**目标**: 不仅是预警通知，所有通知（新闻推送等）都能点击跳转到对应页面。

**涉及文件**:
- `worker/AlertCheckWorker.kt` — 已在 2.3 处理
- `worker/NewsSyncWorker.kt` — 可选：重大新闻推送通知
- `presentation/navigation/AppNavHost.kt` — 确保deep link支持
- `MainActivity.kt` — 处理Intent参数导航

**实现要点**:
- 统一通知跳转框架: 所有通知的 `contentIntent` 使用同一 `Intent` 构建方式
- `MainActivity` 在 `onCreate`/`onNewIntent` 中解析 Intent extras，导航到目标页面
- 定义通知 extras 格式: `target_route`, `target_code`, `target_id` 等

### 4.4 桌面Widget

**目标**: 添加持仓概览/指数桌面Widget。

**新增文件**:
- `widget/PortfolioWidget.kt` — GlanceAppWidget 实现
- `widget/MarketIndexWidget.kt` — 指数Widget
- `widget/WidgetReceiver.kt` — 更新触发
- `res/xml/portfolio_widget_info.xml` — Widget配置XML
- `worker/WidgetUpdateWorker.kt` — 定时更新Worker

**实现要点**:
- 使用 Jetpack Glance (Compose for Widgets) API
- 持仓Widget: 显示总市值、今日收益、涨跌%
- 指数Widget: 显示上证/恒生/纳斯达克等核心指数
- 通过 WorkManager 定时更新（30分钟间隔）
- Widget 点击打开App对应页面

---

## Phase 5: 新功能（中价值）

### 5.1 自选分组/标签

**目标**: 自选股按行业/主题分组管理。

**涉及文件**:
- `data/local/entity/WatchlistEntity.kt` — 添加 `group` 字段
- `data/local/dao/WatchlistDao.kt` — 按组查询
- `domain/model/WatchlistItem.kt` — 添加 `group` 字段
- `presentation/watchlist/WatchlistTabContent.kt` — 添加分组UI
- `data/local/AppDatabase.kt` — 数据库迁移 v8→v9 或 v9→v10

### 5.2 资讯收藏功能

**目标**: 标记重要资讯收藏，独立查看收藏列表。

**新增文件**:
- `data/local/entity/NewsFlashEntity.kt` — 添加 `isBookmarked` 字段
- `data/local/dao/NewsFlashDao.kt` — 添加 `getBookmarked()` 查询
- `presentation/portfolio/PortfolioScreen.kt` — 新闻条目添加收藏按钮
- `presentation/portfolio/PortfolioViewModel.kt` — 收藏/取消收藏方法

### 5.3 组合对比大盘基准

**目标**: 在收益曲线中叠加沪深300走势，直观显示跑赢/跑输大盘。

**涉及文件**:
- 依赖 4.1 的 PortfolioSnapshot 体系
- `data/local/entity/PortfolioSnapshotEntity.kt` — 添加 `benchmarkValue` (沪深300点位)
- `presentation/portfolio/components/EarningsChartView.kt` — 双线绘制

### 5.4 AI对话历史持久化

**目标**: 保存AI对话历史到数据库，跨session保留。

**新增文件**:
- `data/local/entity/AiConversationEntity.kt` — 对话记录
- `data/local/entity/AiMessageEntity.kt` — 消息记录
- `data/local/dao/AiConversationDao.kt` / `AiMessageDao.kt`
- `presentation/ai/AiChatScreen.kt` — 显示历史对话列表入口
- `ai/model/ChatMessage.kt` — 确保与Entity兼容

---

## Phase 6: 新功能（低价值/锦上添花）

### 6.1 数据云端备份 (Google Drive / WebDAV)
### 6.2 分红/股息记录 (针对港股持仓)
### 6.3 多账户/组合管理
### 6.4 分享持仓截图 (带水印)
### 6.5 资讯关键词高亮

---

## 实施时间线建议

| Phase | 预估工时 | 优先级 | 状态 |
|-------|---------|--------|------|
| Phase 1 (P0 Bug修复) | 3-5天 | 🔴 立即 | ✅ 已完成 (v1.2.0) |
| Phase 2 (P1 Bug修复) | 2-3天 | 🟠 本周 | ✅ 已完成 (v1.2.0) |
| Phase 3 (功能优化) | 5-7天 | 🟡 第二周 | 待实施 |
| Phase 4 (高价值新功能) | 10-15天 | 🟢 第三-四周 | 待实施 |
| Phase 5 (中价值新功能) | 8-12天 | 🔵 后续迭代 | 待实施 |
| Phase 6 (锦上添花) | 按需 | ⚪ 长期 | 待实施 |

---

## 验证策略

每个Phase完成后:
1. `./gradlew assembleDebug` 编译通过
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk` 安装到设备
3. 真机验证:
   - Phase 1: 添加美股持仓 → 验证行情显示和收益计算；添加基金持仓 → 验证市值/收益一致性；切换不同黄金品种 → 验证K线数据不同
   - Phase 2: 设置页修改刷新间隔 → 验证持仓页实际刷新频率；断网查看港股持仓 → 验证汇率降级一致；触发预警 → 点击通知跳转验证
   - Phase 3: 进入个股详情 → 验证相关新闻显示；资讯Tab搜索 → 验证搜索结果；K线图 → 验证均线叠加