# 腾讯自选股（WeStock）全面接入与 CETP 工具开发计划

> 文档状态：方案评审稿，仅用于规划，不包含业务代码改动
> 调研日期：2026-09-09
> 适用项目：九财花 Android App
> 当前基线：Kotlin + Compose、Clean Architecture + MVVM、Room v17、19 个 CETP 工具

## 1. 目标与结论

本计划的目标不是把腾讯自选股做成另一个孤立的“新闻源”，而是把其匿名可访问的行情、个股资讯、公告、研报、资金流、公司资料和关联证券能力建设成九财花的共享证券信息底座，同时服务：

- 个股详情页、持仓、自选、市场和资讯页面；
- 后台同步、事件提醒和数据缓存；
- App 内 AI Agent；
- 对外 CETP Tool Provider。

调研结论：**可以作为 App 内部消息源，而且对“持仓/自选股的精准消息”价值很高**。腾讯接口能直接按证券代码返回新闻、公告、定期报告、研报和行业资讯，比当前按股票名称搜索本地快讯更准确。它不应在第一阶段取代现有 6 路全市场资讯，而应作为按证券代码获取内容的主源，现有来源继续承担全市场快讯和故障降级。

“无需授权”的实现口径定义为：不接腾讯登录、不读取用户腾讯自选列表、不使用 Cookie/QQ/微信身份、不调用券商交易能力，仅访问无需会话即可返回数据的公开网页接口。匿名可访问不等同于正式开放 API 或稳定 SLA，因此所有腾讯能力必须经过隔离、缓存、限流、监控和功能开关；如未来对外商业分发，再单独评估数据授权与展示要求，不阻塞本计划的本地开发。

## 2. 产品能力与实测范围

腾讯官方将自选股描述为覆盖沪深港美行情、全球指数、外汇和国际期货，并提供 7x24 资讯、自选资讯、风险因子、智能盯盘、基金、社区和交易。官方说明参考：

- [腾讯自选股产品页](https://www.tencent.net.cn/zh-cn/products/tencent-portfolio/)
- [腾讯自选股 App Store 页面](https://apps.apple.com/cn/app/id485653572)
- [腾讯自选股 Web 站点](https://zxgstock.com/)

以下矩阵是对网页端匿名接口的实际请求结果，不代表腾讯正式 API 承诺。

### 2.1 已实测可用的匿名能力

| 能力 | 接口族/示例 | 已见字段 | 市场 | 建议用途 | 优先级 |
|---|---|---|---|---|---|
| 实时行情 | `qt.gtimg.cn/q=sh600519` | 名称、现价、昨收、开高低、涨跌、成交量额、盘口、换手、PE/PB、市值、52 周高低、时间 | A/HK/US，代码规则不同 | 行情主源候选、详情页概览、AI 快照 | P1 |
| 日/周/月 K 线 | `web.ifzq.gtimg.cn/appstock/app/fqkline/get` | 前复权 OHLCV、行情快照、市场状态 | A/HK/US | 复用并加强现有 K 线 | P0 加固 |
| 分时走势 | `web.ifzq.gtimg.cn/appstock/app/minute/query` | 分钟时间、价格、累计量额 | A/HK，US 需继续回归 | 详情页分时图、盘中异动 | P2 |
| 证券搜索 | `proxy.finance.qq.com/ifzqgtimg/appstock/smartbox/search/get` | 股票、基金、关联基金、相关新闻 | A/HK/基金/US（需适配代码） | 搜索补全、证券规范化 | P0 加固 |
| 个股新闻 | `proxy.finance.qq.com/ifzqgtimg/appstock/news/info/search?type=2` | 腾讯 ID、标题、摘要、时间、原始媒体、URL、关联股票、重要度/提及度 | A/HK；US 需完整样本回归 | 个股详情、持仓消息、AI | P1 |
| 公告 | 同上，`type=0` | 标题、时间、URL、关联股票 | A/HK | 公司事件流、提醒 | P1 |
| 定期报告 | `appstock/news/noticeList/search?noticeType=0103` | 报告标题、时间、详情链接 | A/HK | 财报入口、事件提醒 | P1 |
| 机构研报 | `appstock/app/investRate/getReport` | 标题、时间、报告类型、投资评级 | A/HK | 详情页研报、AI 观点上下文 | P1 |
| 行业资讯 | `appstock/news/HyNews/getBySymbol` | 行业代码、相关新闻 | A 股样本可用 | 行业联动、AI 上下文 | P2 |
| 个股资金流 | `cgi/cgi-bin/fundflow/hsfundtab` | 主力/散户及超大、大、中、小单净流入和占比、分钟趋势、五日摘要 | A 股样本可用 | 资金页、异动提醒、AI | P1 |
| 流通股东 | `appstock/hs/ltgd/get?type=ltgd` | 报告期、股东名、持股数、股份性质、流通占比、上期持股 | A 股样本可用 | 股东变化、基本面上下文 | P2 |
| 所属板块 | `ifzqgtimg/stock/relate/data/plate` | 行业/概念板块及涨跌 | A 股样本可用 | 标签、板块联动 | P2 |
| 相关股票 | `ifzqgtimg/stock/relate/data/relate` | 关联证券及行情摘要 | A 股样本可用 | 横向比较、AI 对标 | P2 |
| 机构观点 | `appstock/hs/jggd/get` | 目标价、评级等 | A 股；部分样本为空 | 观点聚合，仅作补充 | P3 |
| 全球主要指数 | `ifzqgtimg/appstock/app/TencentNews/mainStock` | 道指、纳指、标普等行情、分时和状态 | 全球指数 | 市场页、市场状态 | P2 |

### 2.2 网页代码中已发现、尚需契约验证的能力

这些端点在当前腾讯网页资源中有调用痕迹，但尚未完成跨市场、字段稳定性和空数据验证。进入实现前必须为每类准备不少于 3 个有效样本和 1 个异常样本。

| 能力组 | 发现的接口方向 | 计划 |
|---|---|---|
| 公司资料 | `BASEINFO`、公司概况、管理层、股本结构、分红、重大事项 | P2 建立字段契约后接入 |
| 财务数据 | 财务报告、财务指标、报表、财务分析、业绩预告 | P2；与东方财富数据做口径对照 |
| 港股资料 | 派息、财务报告、投行评级、回顾与展望、权益持有人/董事 | P3；按港股专属模型设计 |
| 交易分析 | 波动率、换手分布、交易表现 | P3；先验证单位和时间窗口 |
| 市场排行 | 热门股、涨跌榜、成交榜、板块榜 | P2；用于市场发现，不用于持仓估值 |
| 市场要闻 | `snpgw_yaowen_recom.fcgi`、`snpgw_columnnews_comm.fcgi`、`open_news_list.fcgi` | 当前要求 `device_id`、`zappid/appid` 等参数；仅做参数画像，不伪造用户身份 |

### 2.3 明确不纳入的能力

- 沪深 Level 2 十档盘口、逐笔成交、委托队列等付费数据；
- 腾讯账号、微信或 QQ 登录，自选列表云同步；
- 在线开户、券商登录、交易和资产查询；
- 腾讯侧价格提醒的创建、删除和推送通道；
- 社区发帖、评论、用户画像及其他 UGC 读写；
- 依赖登录 Cookie、设备指纹或规避访问控制才能工作的接口。

这些能力与九财花的本地持仓、自选、提醒和交易流水边界冲突，且会显著增加账号、隐私和稳定性风险。九财花继续维护自己的持仓、自选和提醒，只把腾讯作为只读市场数据提供方。

## 3. 当前项目基础与缺口

### 3.1 已有腾讯能力

- `NetworkModule` 已配置 `qt.gtimg.cn`、`proxy.finance.qq.com`、`web.ifzq.gtimg.cn` 三个 Retrofit 实例；
- 港股实时行情已使用腾讯 `qt.gtimg.cn`；
- A 股、港股 K 线已使用腾讯前复权 K 线接口；
- 证券搜索已使用腾讯 Smartbox；
- 腾讯 GBK 响应已有 OkHttp 解码拦截器；
- CETP Provider 已通过白名单暴露 19 个工具，业务逻辑统一复用 App 内 `ToolRegistry`。

### 3.2 主要缺口

| 缺口 | 当前影响 | 本计划处理 |
|---|---|---|
| 个股资讯按“名称”搜索本地快讯 | 同名、简称和译名会漏报或误匹配 | 优先按规范化证券代码查询腾讯，名称搜索降级 |
| `get_stock_news` 实际调用 `searchNews` | 没有利用已有的个股 API 语义 | 保持兼容参数，增加 `code` 精确查询 |
| 腾讯能力分散在多个 Repository | 难以统一限流、来源和错误策略 | 新增腾讯远端数据源层，Repository 只依赖领域接口 |
| 证券代码存在多套格式 | 项目美股为 `usr_AAPL`，腾讯端点可能要求 `usAAPL`、`us.AAPL` 等 | 建立唯一 `SecurityId` 和端点级转换器 |
| `NewsFlash`/Room 的外部 ID 是 `Long` | 腾讯新闻 ID 可能是字符串，强转会碰撞 | 新建通用证券事件表，外部 ID 使用 `String` |
| 详情页文章跳转未完整传递 URL | 只能看缓存摘要，无法稳定进入原文 | 统一事件详情模型并保留来源 URL |
| 数据输出缺少来源和新鲜度 | UI/AI 无法判断缓存、过期和部分失败 | 所有新增快照携带 provenance/freshness |
| Agent 每轮最多调用 4 次工具 | 多个细粒度接口会耗尽工具轮次 | 新增一个复合 `get_stock_context` 工具 |
| 全市场腾讯新闻端点参数未稳定 | 不能可靠替换现有资讯聚合 | 保留现有 6 路源，腾讯先做个股事件主源 |

## 4. 目标架构

```text
Tencent anonymous endpoints
  ├─ quote / K-line / minute / search
  ├─ stock news / announcement / report / research
  ├─ fund flow / profile / shareholders / financials
  └─ sectors / related stocks / indices / rankings
                    │
                    ▼
TencentRemoteDataSource
  ├─ endpoint-specific Retrofit APIs
  ├─ SecurityCodeMapper
  ├─ typed response parsers + fixture contract tests
  └─ timeout / concurrency / retry / schema guard
                    │
                    ▼
Domain repositories
  ├─ StockRepository             行情、K 线、分时
  ├─ SecurityEventRepository     新闻、公告、报告、研报
  ├─ SecurityInsightRepository   资金、资料、财务、股东、关联
  └─ MarketRepository            指数、板块、排行
                    │
           ┌────────┴────────┐
           ▼                 ▼
      Room / memory cache    Use Cases
                              ├─ Compose ViewModels
                              ├─ Workers / local alerts
                              ├─ AI Agent tools
                              └─ CETP Provider whitelist
```

设计原则：

1. 腾讯接口细节只存在于 Data 层，UI、AI 和 CETP 不解析原始 JSON/文本。
2. App 与 CETP 共用领域 Use Case，避免出现两套字段口径和缓存策略。
3. 单端点失败不拖垮整页或复合工具，返回部分结果和明确警告。
4. 数据来源是模型字段，而不是 UI 写死的标签。
5. 保留新浪、东方财富和当前资讯源作为可观测的降级路径。

## 5. 统一证券标识与数据模型

### 5.1 SecurityId

新增统一值对象，内部始终使用九财花格式，只有 Data 层转换为腾讯格式。

| 市场 | 九财花规范码 | 腾讯行情/资讯码示例 | 备注 |
|---|---|---|---|
| 上交所 | `sh600519` | `sh600519` | 直接映射 |
| 深交所 | `sz000001` | `sz000001` | 直接映射 |
| 北交所 | `bj430047` | `bj430047` | 各端点逐项验证 |
| 港股 | `hk00700` | `hk00700` / 行情可能加 `r_` | 固定补足 5 位 |
| 美股 | `usr_AAPL` | `usAAPL` 或端点专属格式 | 不用全局字符串替换 |
| 基金 | `008763` | `jj008763` 或搜索端点格式 | 腾讯只作搜索/关联补充 |

`SecurityCodeMapper` 必须提供端点级方法，例如 `toQuoteSymbol`、`toKLineSymbol`、`toNewsSymbol`，并对不支持的市场显式返回错误，禁止静默拼接。

### 5.2 证券事件模型

用 `SecurityEvent` 统一承载消息，但保留事件类型差异：

- `externalId: String`
- `provider: TENCENT`
- `kind: NEWS | ANNOUNCEMENT | PERIODIC_REPORT | RESEARCH | INDUSTRY_NEWS`
- `title`、`summary`、`contentUrl`
- `publisher`、`publishedAt`
- `symbols: List<SecurityId>`
- `importance`、`titleMention`、`bodyMention`（有值才输出）
- `researchRating`、`reportType`（仅研报）
- `fetchedAt`、`isStale`

Room 新增 `security_event` 表，不复用当前 `news_flash` 表：后者的 `newsId: Long`、24 小时清理和全市场快讯语义不适合公告/研报。建议数据库迁移为当前实际版本 `17 -> 18`，唯一索引使用 `(provider, kind, externalId)`，并为 `(symbol, publishedAt)` 建查询索引。证券与事件是多对多关系时使用关联表，而不是把代码列表拼成字符串参与查询。

### 5.3 结构化快照

新增领域快照，按页面和工具需要裁剪输出：

- `RealtimeQuoteSnapshot`：现价、涨跌、OHLC、量额、换手、估值、市值、盘口摘要、52 周区间；
- `IntradaySeries`：分钟价格、累计量额、交易日和状态；
- `StockFundFlowSnapshot`：分档净流入、占比、分钟趋势、五日汇总；
- `CompanyProfileSnapshot`：公司概况、行业、上市信息、主营摘要；
- `FinancialSnapshot`：报告期、核心指标、同比/环比、单位和口径；
- `ShareholderSnapshot`：报告期、前十大流通股东及持股变化；
- `SecurityRelationsSnapshot`：所属行业/概念、相关股票；
- `MarketRankingSnapshot`：排行类型、市场、基准时间和证券列表。

所有快照统一包含：

```text
generatedAt        九财花生成快照的时间
fetchedAt          最近一次成功取得该数据的时间，读取缓存时不得重置
sourceUpdatedAt    腾讯数据自身的时间，缺失时为空
provider           TENCENT / SINA / EASTMONEY，缓存保留原始供应商
servedFrom         NETWORK / MEMORY_CACHE / ROOM_CACHE
isStale            缓存超过 TTL 或源数据滞后于该市场预期更新时间
warnings           过期、字段缺失、降级或部分失败说明
```

TTL 从 `fetchedAt` 计算，`generatedAt` 只表示输出时间；成功请求到旧行情也不能刷新其源数据新鲜度。源时间未知时必须标记 `SOURCE_TIME_UNKNOWN`，不得声称已验证实时性。休市、停牌及跨时区市场按交易日历判断最近应有的交易时点，避免把正常收盘快照误报为源异常；缓存刷新 TTL 与源数据是否符合预期分别判断。

金额必须同时给出 `value` 与 `currency`，比例统一使用百分数值，成交量保留原始单位说明。禁止让 AI 根据字段名猜测万元、亿元、股或手。

## 6. App 内容接入计划

### 6.1 个股详情页：最高优先级

详情页调整为三个紧凑的内容视图，避免纵向堆叠过长：

- **行情**：实时摘要、分时/日周月 K 线、成交量、换手、PE/PB、市值、52 周区间、个股资金流；
- **资讯**：新闻、公告、定期报告、研报四个筛选项，展示原始媒体、发布时间和关联度，点击进入详情或原文；
- **资料**：公司概况、所属板块、财务摘要、股东、关联股票。

首屏请求只加载行情和当前选中的内容；资料、股东、研报等按需加载。每个区块独立呈现加载、空数据、缓存和失败状态，不能因为股东接口失败导致行情页失败。

### 6.2 持仓与自选

- 每个标的显示最近重大事件计数：公告、财报、重要新闻；
- 支持“只看持仓/只看自选”的个股消息流；
- 增加事件未读状态，未读由本地维护，不依赖腾讯账号；
- 盘中可显示主力净流入摘要，但列表刷新不随行情 10 秒轮询全量请求资金流；
- 标的进入前台可按需刷新，后台只同步持仓和自选集合。

### 6.3 资讯页

- 保留现有 6 路全市场快讯聚合；
- 新增“自选动态”视图，来源为腾讯按代码拉取的事件；
- 来源筛选中可显示“腾讯聚合”，同时保留实际媒体 `publisher`；
- 相同文章按腾讯外部 ID、规范 URL、归一化标题和时间窗口去重；
- 不把公告、研报混入 24 小时快讯清理规则。

### 6.4 市场页

- 现有 A/HK/US/黄金指数继续可用；腾讯全球指数用于补充和交叉校验；
- 增加热门、涨幅、跌幅、成交额、板块排行；
- 排行数据仅用于发现和展示，不进入持仓估值计算；
- 腾讯市场要闻端点完成参数稳定性验证后，再决定是否成为第 7 路全市场源。

### 6.5 本地提醒

在现有价格提醒之外，逐步支持：

- 新公告/定期报告提醒；
- 持仓或自选股的重要新闻提醒；
- 主力净流入/流出阈值提醒；
- 研报评级变化提醒（仅在同一可比口径下）。

提醒规则、已读状态和通知发送均由九财花本地实现。后台任务只拉取增量，按 `(provider, kind, externalId)` 保证幂等，并设置每个标的/每天的通知上限，避免同一事件被多来源重复推送。

增量同步以 `(provider, symbol, kind)` 保存成功水位和补拉进度。Phase 0 必须验证每个端点的分页、排序、时间过滤及历史可达范围；不支持服务端游标时使用本地水位与重叠回拉窗口，不能只请求最新一页。持续翻页至覆盖上次成功水位及重叠窗口，或确认服务端已无更多记录。窗口长度按端点延迟样本确定，并配合定期历史补扫处理延迟入库；超出源可查询范围时返回完整性警告，不宣称无遗漏。

事件、证券关联与分页进度在事务中保存；完整覆盖本轮窗口后才推进成功水位。中途失败或达到单轮请求预算时保留补拉进度，下次续传。首次同步只建立历史基线，不发送历史事件通知；事件 ID 去重不能代替分页完整性检查。验收覆盖离线积压超过一页、同时间戳多事件、延迟发布及中断恢复。

## 7. AI 与 CETP 工具规划

### 7.1 原则

- App 内 AI 和 CETP 使用同一个 `ToolExecutor` 与 Use Case；
- 保持已有工具兼容，不更名、不删除已有字段；
- 细粒度工具用于精确查询，复合工具用于在 4 次迭代上限内完成分析；
- 工具只返回结构化摘要和原文 URL，不返回整篇版权内容或原始腾讯响应；
- 每个结果说明数据来源、时间、新鲜度、降级和部分失败。

### 7.2 扩展现有工具

| 工具 | 变更 | 兼容策略 |
|---|---|---|
| `get_stock_news` | 新增可选 `code`、`market_type`、`kinds`；有代码时按腾讯证券代码精确查询，无代码时保留名称搜索 | schema 将 `name` 与 `code` 均设为可选；执行层要求至少一个非空，均提供时以 `code` 为准；旧版仅传 `name` 的调用继续有效 |
| `get_kline_data` | 返回来源时间、是否复权、数据新鲜度；继续复用腾讯 K 线 | 原字段只增不删 |
| `search_securities` | 支持腾讯搜索中的美股和关联基金，统一代码格式 | 输出仍使用九财花代码 |
| `get_market_indices` | 可选腾讯全球指数补充，输出 provider 和降级信息 | 市场枚举保持不变 |
| `get_holding_analysis` | 相关新闻改为证券代码精确查询，加入公告/研报/资金流摘要 | 控制条数和输出大小 |

注意：现有 `get_fund_flow` 表示沪深港通北向/南向资金，不应改变语义。个股资金流必须使用新工具名。

### 7.3 新增工具

| 内部/CETP 工具名 | 核心参数 | 输出 | 对外优先级 |
|---|---|---|---|
| `get_stock_context` | `code` 必填；`sections`、`event_limit` 可选 | 行情、市场状态、技术摘要、事件、个股资金、资料摘要及完整 freshness/warnings | P1，首要复合工具 |
| `get_stock_events` | `code`；`kinds`；`from`；`to`；`limit` | 新闻、公告、报告、研报的统一事件列表 | P1 |
| `get_stock_fund_flow` | `code`；`period=intraday|5d` | 分档资金净流入、占比、趋势和更新时间 | P1 |
| `get_stock_profile` | `code`；`sections` | 公司概况、板块、股东、关联证券 | P2 |
| `get_stock_financials` | `code`；`period`；`metrics` | 财务指标及报告期、单位、口径 | P2 |
| `get_market_rankings` | `market`；`ranking`；`limit` | 热门/涨跌/成交/板块排行 | P2 |

第一批只对 CETP 白名单增加 `get_stock_context`、`get_stock_events`、`get_stock_fund_flow`。其余工具先在 App 内验证字段、体积和延迟，再对外开放。Provider 的 `scopes` 增加 `security_events` 和 `security_insights`。

### 7.4 `get_stock_context` 输出边界

该工具用于回答“分析贵州茅台最近发生了什么”一类问题，默认一次返回：

- 1 条最新行情和市场状态；
- 20～60 根 K 线计算后的技术摘要，不直接返回全部原始点；
- 最近 5 条新闻、3 条公告、2 条研报；
- 最新个股资金流摘要；
- 公司/行业标签；
- 每个 section 的 `status: ok | stale | unavailable` 和警告。

调用者可用 `sections` 缩小结果。单次序列化结果建议控制在 32 KB 内；超限时优先减少新闻摘要和时间序列，不删除来源与时间字段。

### 7.5 参数与错误规范

- `code` 始终接受九财花规范码；禁止调用者传腾讯内部码；
- `limit` 设置工具级上下限，事件默认 10、最大 50；
- 时间参数使用 ISO-8601；
- 不支持的市场返回 `UNSUPPORTED_MARKET`，不返回空列表伪装成功；
- 单个 section 失败时复合工具整体成功，并在 `warnings` 和 section 状态中说明；
- 全部数据源失败时返回 `PROVIDER_UNAVAILABLE`；
- 参数不合法返回 `INVALID_ARGS`，不在异常文本中泄露完整请求 URL 或响应体。

首批工具开放前必须完成以下 App/CETP 边界适配：

- Provider 当前使用 `JSONObject.get()`，数组、对象及空值会保留为 `JSONArray`、`JSONObject`、`JSONObject.NULL`；统一递归转换为 Kotlin `List`、`Map`、`null`，与 App 内 Moshi 参数解析一致。`sections`、`kinds` 等参数须验证类型和枚举值，不能因类型转换失败而静默采用默认值。
- 当前 `ToolResult` 仅有 `content`，Provider 将执行异常统一映射为 `INTERNAL_ERROR`。新增共享的类型化工具错误（错误码与可公开消息），明确 App Agent 将预期错误作为工具结果交给模型继续处理，CETP 则映射为现有 `status=error`、`error_code`、`error_message` Bundle。部分成功仍走成功结果；协程取消继续向上传播，未知异常使用脱敏的 `INTERNAL_ERROR`。
- 统一结果序列化规则，确保契约要求的 `null` 字段被保留；为同一参数经 App 和 CETP 两个入口的结果建立一致性测试，覆盖数组、嵌套值、空值、非法参数和数据源失败。旧工具的成功结构保持兼容。

### 7.6 首批工具返回契约草案

`get_stock_context`：

```json
{
  "code": "sh600519",
  "name": "贵州茅台",
  "generatedAt": "2026-09-09T10:30:00+08:00",
  "quote": { "status": "ok", "provider": "TENCENT", "sourceUpdatedAt": "..." },
  "technical": { "status": "ok", "period": "DAILY", "summary": {} },
  "events": { "status": "ok", "items": [] },
  "stockFundFlow": { "status": "unavailable", "reason": "..." },
  "profile": { "status": "stale", "data": {} },
  "warnings": []
}
```

`get_stock_events` 的每个 item 固定返回 `externalId`、`kind`、`title`、`summary`、`publisher`、`publishedAt`、`symbols`、`url`、`provider` 和 `isStale`。公告和研报的专属字段放在可选 `attributes` 中，避免所有类型出现大量无意义的空字段。

`get_stock_fund_flow` 固定返回 `currency`、`unit`、`sourceUpdatedAt`、`mainNet`、`retailNet`、`superLargeNet`、`largeNet`、`mediumNet`、`smallNet`、对应占比，以及可选 `intradayPoints`/`fiveDaySummary`。数值字段使用 JSON number，缺失值使用 `null`，不得用 `"--"` 或空字符串。

契约定稿时为每个工具保存 golden JSON，并在测试中验证字段名称、类型和可空性。新增字段允许，删除字段或改变类型视为 CETP 破坏性变更。

## 8. 网络、缓存与降级

### 8.1 请求策略

- 腾讯各 host 使用独立 Retrofit 配置，但共享匿名请求标识、指标和限流器；
- 连接/读取超时目标为 5～8 秒，页面首屏总等待不超过 2 秒，慢区块异步补齐；
- 每 host 最大并发建议为 4，后台批量同步按证券分批；
- 仅对超时、连接失败和 5xx 做至多 1 次带抖动重试；不重试 4xx 和解析错误；
- 收到限流或连续失败时指数退避，并由端点级熔断器暂时停用；
- Debug 日志只记录 host、能力名、耗时、状态和响应大小，不记录完整正文。

### 8.2 建议 TTL

| 数据 | 交易时段 TTL | 非交易时段 TTL | 离线保留 |
|---|---:|---:|---:|
| 实时行情 | 5～10 秒 | 5 分钟 | 最近一次 |
| 分时走势 | 30 秒 | 当日不再刷新 | 2 个交易日 |
| 日/周/月 K 线 | 15 分钟 | 4 小时 | 长期，按点更新 |
| 个股新闻 | 5 分钟 | 15 分钟 | 30 天 |
| 公告/定期报告 | 15 分钟 | 1 小时 | 长期 |
| 研报 | 1 小时 | 4 小时 | 180 天 |
| 个股资金流 | 1 分钟 | 当日收盘固定 | 10 个交易日 |
| 公司资料/板块 | 24 小时 | 24 小时 | 30 天 |
| 股东/财务 | 24 小时 | 24 小时 | 按报告期长期 |
| 市场排行 | 30 秒 | 15 分钟 | 最近一次 |

采用 stale-while-revalidate：先显示可用缓存并标注更新时间，再后台刷新。缓存超过离线保留期后不参与 AI 结论，但可在 UI 中作为历史记录显示。

当前 `StockRepositoryImpl.getKLineData` 直接请求远端，Room 尚无 K 线缓存。Phase 1 新增持久化 K 线缓存，缓存键至少包含规范证券代码、周期、复权方式与供应商，点按交易时间唯一保存，并记录抓取时间及源时间。缓存读取须检查请求区间和点数覆盖；不足时技术摘要标记不可用或数据不足，不以短序列冒充完整指标输入。前复权历史在除权等导致基准变化时须重新校验或重建，不能仅追加最新点而混用不同基准。

### 8.3 降级矩阵

| 能力 | 腾讯失败时 | 不允许的行为 |
|---|---|---|
| A 股行情 | 新浪行情 + Room 缓存 | 用 0 作为现价 |
| 港股行情 | Room 最近行情 | 静默改用昨收冒充现价 |
| 美股行情 | 现有新浪美股 + Room 缓存 | 混用未规范化代码 |
| K 线 | Phase 1 新建的持久化缓存；无有效缓存或已验证备用源时返回不可用 | 伪造缺失 K 线点或假定当前已有离线缓存 |
| 个股新闻 | 本地 6 路资讯按代码别名/名称搜索 | 把无关同名文章当精确结果 |
| 公告/研报 | 缓存并标记过期，无缓存则显示不可用 | 用普通新闻代替公告 |
| 个股资金流 | 缓存并标记过期 | 与北向/南向资金流混为一谈 |
| 公司/财务/股东 | 缓存；后续可引入东方财富备用 | 跨报告期拼接成同一快照 |

## 9. 数据质量与可观测性

### 9.1 契约保护

- 每个端点保存脱敏响应 fixture，解析器测试不直接依赖网络；
- 为位置型行情字段建立索引常量和最小长度校验；
- JSON 先校验状态码和核心节点，再映射可选字段；
- 为每类响应计算 schema fingerprint，只记录字段路径/类型，不保存用户数据；
- 每日或按 CI 手动运行匿名 canary，检测空响应、字段漂移和编码异常；
- “投资亮点”等历史样本明显陈旧的接口默认不上 UI，只在质量达标后启用。

### 9.2 内容去重与可信度

去重顺序：

1. 同 provider + kind + externalId；
2. 规范化后的原文 URL；
3. 归一化标题 + 发布者 + 30 分钟窗口；
4. 只在前三项不足时做相似标题判断。

腾讯是聚合方时，UI 和工具同时保留 `provider=TENCENT` 与实际 `publisher`。不自动把腾讯的 `importance` 映射为“利好/利空”；投资影响判断应由规则或 AI 基于正文上下文生成，并标记为分析结果而非源数据。

### 9.3 指标

- 端点成功率、P50/P95 延迟、空结果率、解析失败率；
- 缓存命中率、过期数据使用率、降级来源占比；
- 个股事件去重率、通知去重率、无关新闻反馈率；
- `get_stock_context` 成功率、部分失败率、序列化体积、总耗时；
- 各市场样本覆盖率和数据时间滞后。

### 9.4 风险登记

| 风险 | 等级 | 触发信号 | 控制措施 |
|---|---|---|---|
| 匿名端点字段或路径变化 | 高 | 解析失败率、schema fingerprint 变化 | fixture、canary、端点开关、旧缓存和备用源 |
| 批量同步触发限流 | 高 | 429/空响应、延迟突增 | 每 host 并发上限、分批、抖动、指数退避 |
| 市场代码映射错误 | 高 | 搜索可见但详情为空、串到另一证券 | 端点级 mapper、跨市场样本、返回代码反校验 |
| 旧数据被误判为实时 | 高 | 源时间缺失或早于预期 | `sourceUpdatedAt`、TTL、`isStale`、禁止 0/昨收伪装 |
| 聚合资讯重复或错误关联 | 中 | 同标题重复、同名公司误匹配 | 优先代码关联、四级去重、名称搜索标记低置信度 |
| 财务数据单位/报告期混乱 | 高 | 与备用源数量级不一致 | 单位入模、报告期必填、影子对比、异常值拒收 |
| Room 数据增长过快 | 中 | 事件表和时间序列持续膨胀 | 分类保留期、分页、定期清理、索引与体积监控 |
| CETP Binder 调用过慢或过大 | 中 | 超时、TransactionTooLarge | section 并发、总超时、32 KB 软上限、结果裁剪 |
| 后台同步耗电/耗流量 | 中 | Worker 时长、请求数和流量升高 | 仅持仓/自选、增量游标、网络约束、合并批次 |

## 10. 分阶段实施计划

### Phase 0：契约固化与基础设施

目标：把当前调研结果变成可回归的数据契约，不改变用户界面。

任务：

- 建立 `SecurityId` 与端点级 `SecurityCodeMapper`；
- 拆分腾讯 API 接口与 `TencentRemoteDataSource`；
- 为行情、K 线、搜索、新闻、公告、研报、资金流建立 fixture；
- 固化事件端点分页、排序、历史查询范围和延迟入库契约；
- 覆盖 A 股、港股、美股、北交所的成功/空数据/异常样本；
- 加入统一来源、新鲜度、超时、限流和端点开关；
- 修订 `docs/data-sources.md`，记录真实编码、字段和降级关系。

验收：所有解析器离线契约测试通过；线上 canary 可单独运行；关闭腾讯新增功能开关时 App 行为与当前版本一致。

### Phase 1：个股消息、资金流与首批 CETP

目标：优先解决“消息不够精准”和“Agent 上下文不足”。

任务：

- 新增 `security_event`、事件证券关联、同步状态与 K 线缓存表及 v17→v18 迁移；
- 接入个股新闻、公告、定期报告、研报和个股资金流；
- 详情页上线“资讯”分类和资金流摘要，完整传递原文 URL；
- 持仓/自选后台做增量事件同步与本地去重；
- 扩展 `get_stock_news`，新增 `get_stock_context`、`get_stock_events`、`get_stock_fund_flow`；
- 在 `AiModule` 注册，并将 3 个新工具加入 CETP 白名单；
- 完成 CETP 参数递归规范化、共享错误模型与 Bundle 映射、可空字段序列化和双入口一致性测试；
- `get_holding_analysis` 改用证券代码精确事件并支持部分失败。

验收：A/HK 各 5 个证券连续运行 7 天无重复事件；离线多页积压、延迟入库及中断恢复样本在已声明查询范围内无遗漏；精准个股新闻有明确代码关联；CETP 三个新工具在无网络、空数据和部分失败时均返回稳定结构，数组参数与错误码在双入口符合契约；K 线缓存不足时不输出误导性的技术摘要。

### Phase 2：行情深化、资料与市场发现

目标：丰富详情页和市场页，不影响核心持仓估值稳定性。

任务：

- A/HK/US 腾讯行情并行影子对比，达标后按市场决定主备顺序；
- 接入分时、盘口摘要、估值和 52 周区间；
- 接入公司资料、板块、股东、财务摘要、相关股票；
- 接入全球指数和市场排行；
- 新增 `get_stock_profile`、`get_stock_financials`、`get_market_rankings`，先仅 App 内开放；
- 完成 UI 空态、过期态、局部错误和来源说明。

验收：与现有行情源对比误差有明确阈值和报告；资料字段带报告期/单位；长列表和详情页在中低端设备上无明显卡顿。

### Phase 3：事件提醒与 CETP 全量开放

目标：把新增内容转化为持仓管理行动，但不替用户做交易判断。

任务：

- 上线公告、财报、重要新闻和个股资金阈值提醒；
- 增加按标的和事件类型的通知偏好、静默时段与日上限；
- 根据 Phase 2 的稳定性结果，将资料、财务和排行工具加入 CETP 白名单；
- 更新 Provider scopes、工具文档和 Consumer 示例；
- 加入工具输出大小与调用耗时监控。

验收：增量任务幂等；同一事件不重复通知；所有对外工具通过 `list_tools`/`execute_tool` 真机测试；旧版 CETP Consumer 仍能调用原有 19 个工具。

### Phase 4：可选探索

- 参数稳定后评估腾讯市场要闻是否作为第 7 路全市场资讯源；
- 验证港股专属资料、投行评级和派息数据；
- 基于多源数据生成“个股小报”，但生成内容与原始事实分层保存；
- 评估 ETF 估值、相关基金和溢折价能力，不与基金净值估算混用。

### 10.1 粗略工作量

以下为单名熟悉项目的开发者人日估算，不包含 7 天稳定性观察的自然等待时间：

| 阶段 | 开发与单测 | 联调/真机/文档 | 合计 |
|---|---:|---:|---:|
| Phase 0 | 3～5 人日 | 1～2 人日 | 4～7 人日 |
| Phase 1 | 7～10 人日 | 3～4 人日 | 10～14 人日 |
| Phase 2 | 8～12 人日 | 3～5 人日 | 11～17 人日 |
| Phase 3 | 5～8 人日 | 2～4 人日 | 7～12 人日 |
| Phase 4 | 按选中能力单独估算 | - | 不进入首版承诺 |

关键路径是 `SecurityId/解析契约 -> SecurityEvent 数据链路 -> get_stock_context -> UI/后台同步`。Phase 2 的资料类端点可以并行开发，但不得绕过 Phase 0 的代码映射和契约测试。

## 11. 预计代码影响面

| 层 | 主要位置 | 计划改动 |
|---|---|---|
| Data/API | `data/remote/api/`、`data/remote/dto/` | 腾讯端点接口、响应 DTO/解析器 |
| Data/Source | `data/remote/datasource/`（新增） | 统一请求策略、代码转换、端点隔离 |
| Data/Room | `data/local/entity/`、`dao/`、`AppDatabase.kt` | 证券事件和结构化缓存、v17→v18 迁移 |
| Data/Repository | `data/repository/` | 事件/资料 Repository 实现、现有行情与搜索加固 |
| Domain | `domain/model/`、`domain/repository/`、`domain/usecase/` | 统一标识、事件和快照模型、聚合 Use Case |
| DI | `di/NetworkModule.kt`、`RepositoryModule.kt`、`AiModule.kt` | Retrofit、Repository、工具注册 |
| UI | `presentation/detail/`、`portfolio/`、`market/` | 详情分类、自选动态、市场排行、局部状态 |
| Worker | `worker/NewsSyncWorker.kt` 及新事件同步逻辑 | 持仓/自选增量事件、清理和提醒 |
| AI/CETP | `ai/model/`、`ai/usecase/`、`ai/tool/`、`cetp/CetpToolProvider.kt` | 标准快照、复合工具、白名单和 scopes |
| Docs | `docs/data-sources.md`、`docs/tool-provider.md` | 接口字段、来源、工具和调用示例 |

是否新增独立 `SecurityEventSyncWorker` 在 Phase 1 实现时决定。倾向独立 Worker，原因是事件保留期、同步集合和重试策略都不同于 24 小时全市场快讯，强行塞进 `NewsSyncWorker` 会扩大耦合。

## 12. 测试与发布门槛

### 12.1 自动化测试

- 代码映射：所有市场、大小写、补零和非法代码；
- 解析器：正常、空、字段缺失、字段增删、乱码、HTML 错误页；
- Repository：缓存命中、过期刷新、熔断、重试、降级和协程取消；
- Room：v17→v18 迁移、唯一索引、同事件多证券关联、同步进度事务、K 线缓存键及增量清理；
- Use Case：复合工具部分成功、单位统一、输出裁剪；
- Tool：参数边界、App/CETP 数组及空值一致性、稳定 JSON、错误码映射、输出大小；
- ViewModel/Compose：首屏、分页、切换证券、离线、局部失败和重复点击；
- Worker：幂等、跨页完整性、延迟入库补扫、中断续传、首次同步不发历史通知、批量限流、通知去重和市场关闭行为。

### 12.2 人工与真机验证

每阶段都执行：

1. `./gradlew test`
2. `./gradlew lint`
3. `./gradlew assembleRelease`，使用与设备已安装应用一致的签名证书
4. 比较 APK 包名、签名证书及 versionCode，确认可更新后执行 `adb -s <device> install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk` 覆盖安装；禁止卸载应用或清除数据，签名不匹配时先解决签名问题
5. 用 A 股 `sh600519`、港股 `hk00700`、美股 `usr_AAPL` 和一个无效代码走完 UI/AI/CETP 流程
6. 使用 `content call` 验证 CETP `list_tools`、正常调用、非法参数、离线和部分失败
7. 覆盖安装前后核对应用 UID、持仓、自选及设置保留情况，确认 release APK 非 debuggable，并检查启动与页面切换无崩溃

发布采用远端能力开关分批开启：先开发者设备，再仅详情页，再持仓/自选后台同步，最后 CETP 白名单。任一端点异常可单独关闭，不回滚数据库和其他腾讯能力。

## 13. 完成定义

“腾讯自选股全面接入”不是指网页端所有接口都调用成功，而是满足以下可维护标准：

- 行情、K 线、搜索、个股事件、资金流、资料和市场发现形成统一领域模型；
- App 页面、AI 和 CETP 复用同一数据口径；
- 持仓和自选股消息能按证券代码准确关联，公告/研报与普通快讯分流；
- 每条关键数据可解释来源、时间、单位、新鲜度和降级状态；
- 腾讯端点变更不会导致持仓估值、整个详情页或 Agent 全面不可用；
- 原有 19 个 CETP 工具保持兼容，新工具具备稳定 schema 和文档；
- 匿名接入不引入腾讯账号、交易、付费 Level 2 或用户隐私依赖；
- 测试、真机安装、CETP 调用和至少 7 天稳定性观察全部通过。

## 14. 推荐实施顺序

建议批准后按以下顺序开始：

1. Phase 0 的代码映射、fixture 和契约测试；
2. Phase 1 的 `SecurityEvent` 数据链路与详情页；
3. 同期完成 `get_stock_context`、`get_stock_events`、`get_stock_fund_flow` 三个首批 CETP 工具；
4. 稳定后再扩充公司资料、财务、排行和提醒；
5. 最后评估参数依赖更强的全市场腾讯要闻。

这个顺序先交付对九财花最有价值的“持仓/自选精准消息 + Agent 可用上下文”，同时把易变的腾讯接口限制在可关闭的数据层中。
