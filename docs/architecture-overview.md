# Jiucaihua 架构概览

本文档从 `development-plan.md` 抽取核心架构要点，帮助快速理解项目结构。

## 分层架构

```
┌─────────────────────────────────────────────┐
│              Presentation 展示层             │
│   Screen (Compose) + ViewModel (MVVM)       │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────┴────────────────────────┐
│              Domain 领域层                   │
│   UseCase + Repository Interface + Model    │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────┴────────────────────────┐
│              Data 数据层                     │
│   Repository Impl + Remote API + Room DB    │
└─────────────────────────────────────────────┘
```

## 核心模块

### 数据层 (data)

| 子模块 | 负责内容 |
|--------|----------|
| local | Room 数据库、DAO、Entity |
| remote | Retrofit API、DTO、拦截器 |
| repository | Repository 接口实现 |

### 题域层 (domain)

| 子模块 | 负责内容 |
|--------|----------|
| model | 领域模型（Holding、StockQuote、FundQuote 等） |
| repository | Repository 接口定义 |
| usecase | 业务用例（GetPortfolio、GetKLine 等） |

### 展示层 (presentation)

| 子模块 | 页面 |
|--------|------|
| portfolio | 投资组合页（首页） |
| detail | 详情页（K 线图） |
| holdings | 持仓管理页（新增/编辑） |
| alerts | 预警管理页 |
| settings | 设置页 |
| ai | AI 助手聊天页 |
| article | 资讯阅读页 |

### AI Agent 层 (ai)

| 子模块 | 负责内容 |
|--------|----------|
| config | AI 配置（API Key 加密存储） |
| orchestrator | Agent 调度器（Tool Use 循环） |
| tool | 工具执行器（10+ 个工具） |
| usecase | AI 专用 UseCase |

---

## 数据流向

```
用户操作
    │
    ▼
Screen (Compose UI)
    │
    ▼
ViewModel
    │
    ▼
UseCase
    │
    ├─► Repository Interface
    │       │
    │       ├─► Remote (API) ──► 第三方数据源
    │       │                      │
    │       │                      ▼
    │       │                   Room Cache
    │       │
    │       └─► Local (Room DB) ──► 本地持久化
    │
    ▼
UI State (Flow)
    │
    ▼
Screen 渲染
```

---

## 关键技术

| 技术 | 用途 | 关键文件 |
|------|------|----------|
| Hilt | 依赖注入 | `di/` 各模块 |
| Room | 本地数据库 | `data/local/AppDatabase.kt` |
| Retrofit | 网络请求 | `data/remote/api/` |
| OkHttp 拦截器 | 编码处理、Referer | `data/remote/interceptor/` |
| Coroutines + Flow | 异步数据流 | ViewModel、UseCase |
| WorkManager | 后台任务 | `worker/` |
| MPAndroidChart | K 线图表 | `presentation/detail/` |

---

## 核心业务流程

### 1. 投资组合加载

```
PortfolioViewModel.init()
    │
    ▼
GetPortfolioUseCase.execute()
    │
    ├─► HoldingRepository.getAll() ──► Room 持仓表
    │
    ├─► StockRepository.getQuotes() ──► 新浪/腾讯 API
    │       │
    │       └─► 缓存到 StockCacheEntity
    │
    ├─► FundRepository.getQuotes() ──► 天天基金 API
    │       │
    │       └─► 缓存到 FundCacheEntity
    │
    ├─► ExchangeRateRepository.getRate() ──► 港股汇率
    │
    ▼
计算盈亏 + 汇率折算
    │
    ▼
PortfolioSummary (UI State)
```

### 2. 行情自动刷新

```
IsMarketOpenUseCase.execute()
    │
    ├─► MarketCalendarRepository
    │       │
    │       ├─► HolidayApi（节假日判断）
    │       ├─► 本地时段计算（午休检测）
    │
    ▼
Map<MarketType, MarketSession>

PortfolioViewModel 按开盘市场启动轮询：
    │
    ├─► A 股开盘 → 10 秒轮询新浪
    ├─► 港股开盘 → 10 秒轮询腾讯
    ├─► 全部收盘 → 停止轮询
```

### 3. AI Agent 工具调用

```
用户提问
    │
    ▼
AiChatViewModel
    │
    ▼
AiAgentOrchestrator
    │
    ├─► 构建请求：system prompt + tools 定义
    │
    ▼
LlmApiClient.sendMessage()
    │
    ▼
LLM 返回 tool_use
    │
    ▼
ToolRegistry.execute(name, args)
    │
    ├─► GetPortfolioSummaryTool
    ├─► GetKLineDataTool
    ├─► ...
    │
    ▼
返回工具结果
    │
    ▼
再次请求 LLM（带 tool_result）
    │
    ▼
LLM 返回最终回复
```

---

## 数据模型关系

```
HoldingEntity (Room)
    │
    ▼ 映射
Holding (领域模型)
    │
    ├─► 关联 StockQuote (实时行情)
    ├─► 关联 FundQuote (基金估值)
    │
    ▼ 计算
PortfolioSummary (组合汇总)
    │
    ├─► totalMarketValue
    ├─► totalEarnings
    ├─► todayEarnings
    └─► holdings: List<Holding>
```

---

## 导航结构

```
MainActivity (单 Activity)
    │
    ▼
AppNavHost (Navigation Compose)
    │
    ├─► Portfolio (首页，底部栏 Tab 1)
    ├─► News (资讯页，底部栏 Tab 2)
    ├─► AI (助手页，底部栏 Tab 3)
    │
    ├─► Detail (从持仓点击进入)
    ├─► AddEditHolding (从 FAB 或编辑进入)
    ├─► Settings (从菜单进入)
    └─► Alerts (从菜单进入)
```

---

## 扩展阅读

- [开发计划详版](development-plan.md) - 完整设计文档
- [数据源接口说明](data-sources.md) - API 详细说明
- [AI 数据模型](ai-data-model.md) - Agent 数据规范
- [开发者快速上手](quick-start.md) - 实操指南