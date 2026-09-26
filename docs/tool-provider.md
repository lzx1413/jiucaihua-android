# CETP Tool Provider

九财花实现了 [ClawSeed External Tool Protocol (CETP) v1](https://github.com/lzx1413/clawseed/blob/main/docs/zh/external-tool-protocol.md)，将应用内的投资数据工具通过 ContentProvider 暴露给兼容 CETP 的客户端（如 ClawSeed）。查询与分析工具遵循 CETP v1 的只读核心；创建和删除价格预警属于有副作用的显式扩展。

## 架构

```
CETP Consumer (ClawSeed 等)
  │
  │  ContentResolver.call()
  ▼
CetpToolProvider (ContentProvider)
  │  命名空间前缀: jiucaihua__
  │  外部工具白名单: EXTERNAL_TOOLS
  │
  │  EntryPointAccessors → ToolRegistry
  ▼
ToolRegistry (Hilt Singleton)
  │  22 个对外 ToolExecutor
  ▼
Use Cases → Repositories → API / Room
```

核心设计：Provider 是 IPC 薄层，不直接持有业务逻辑。命名空间前缀 `jiucaihua__` 和外部白名单 `EXTERNAL_TOOLS` 仅在 Provider 层生效，应用内 LLM 仍使用原始工具名。

## 暴露的工具

所有工具名在 CETP 层自动添加 `jiucaihua__` 前缀，Consumer 侧看到的是 `jiucaihua__get_portfolio_analysis` 等名称。

除 `jiucaihua__create_alert` 和 `jiucaihua__delete_alert` 外，其余工具均为只读查询或分析。

| CETP 工具名 | 内部名 | 说明 | 参数 |
|---|---|---|---|
| `jiucaihua__get_portfolio_analysis` | `get_portfolio_analysis` | 投资组合全局快照 | 无 |
| `jiucaihua__get_holding_analysis` | `get_holding_analysis` | 单标的持仓分析 | `code` (必须) |
| `jiucaihua__get_kline_data` | `get_kline_data` | K线图表数据 | `code` (必须), `period`, `limit`（默认60，最大120）, `include_indicators`, `include_latest` |
| `jiucaihua__get_indicator_snapshot` | `get_indicator_snapshot` | 技术指标快照 | `code` (必须), `cost_price`, `hold_days` |
| `jiucaihua__get_market_news` | `get_market_news` | 市场资讯摘要 | `topic`, `query`, `limit` |
| `jiucaihua__get_stock_news` | `get_stock_news` | 个股相关资讯；`code` 存在时精确关联腾讯事件，`name` 保持旧搜索兼容 | `code` 或 `name` 至少一个，`kinds`, `limit` |
| `jiucaihua__get_stock_context` | `get_stock_context` | 个股行情、技术摘要、事件和个股资金流的复合快照 | `code` (必须), `sections`, `event_limit` |
| `jiucaihua__get_stock_events` | `get_stock_events` | 新闻、公告、定期报告和研报的统一事件列表 | `code` (必须), `kinds`, `limit` |
| `jiucaihua__get_stock_fund_flow` | `get_stock_fund_flow` | 单只 A 股资金流，和沪深港通资金流分开 | `code` (必须), `period=summary/intraday/5d/both` |
| `jiucaihua__get_alerts` | `get_alerts` | 价格预警快照（含 id，可用于 delete_alert） | `code` |
| `jiucaihua__create_alert` | `create_alert` | 创建价格预警 | `code` (必须), `name` (必须), `alertType` (必须), `threshold` (必须) |
| `jiucaihua__delete_alert` | `delete_alert` | 删除价格预警 | `id` (必须) |
| `jiucaihua__calculate_what_if` | `calculate_what_if` | 目标价/涨跌幅假设推演 | `code` (必须), `targetPrice`/`changePercent` |
| `jiucaihua__get_market_indices` | `get_market_indices` | 各市场主要指数行情 | `market` (A_STOCK/HK_STOCK/US_STOCK/GOLD) |
| `jiucaihua__get_fund_flow` | `get_fund_flow` | 沪深港通资金流向 | 无 |
| `jiucaihua__search_securities` | `search_securities` | 按关键词搜索证券 | `keyword` (必须), `limit` |
| `jiucaihua__get_market_status` | `get_market_status` | 市场交易状态与汇率 | 无 |
| `jiucaihua__get_watchlist` | `get_watchlist` | 自选证券及最新行情 | 无 |
| `jiucaihua__get_transactions` | `get_transactions` | 交易流水明细 | `code`, `market_type`, `type`, `from`, `to`, `limit`, `offset` |
| `jiucaihua__get_transaction_summary` | `get_transaction_summary` | 交易聚合摘要，含 FIFO 已实现收益、分红、费用税费和现金流 | `code`, `market_type`, `from`, `to` |
| `jiucaihua__get_holding_transaction_history` | `get_holding_transaction_history` | 单标的交易历史和收益拆解 | `code` (必须), `market_type`, `limit`, `offset` |
| `jiucaihua__get_portfolio_performance` | `get_portfolio_performance` | 组合真实收益概览，按总资产和现金流变化分析 | `from`, `to` |

所有 CETP 成功结果返回紧凑 JSON 并省略 `null` 字段，保留有效的零值。`get_kline_data` 默认返回60根、最多120根K线；价格、成交量、涨跌幅及技术指标均在计算完成后四舍五入到最多三位小数，保持 JSON number 类型，不强制补尾零。`latestPoint` 默认省略，只有 `include_latest=true` 时返回，因为它与 `points` 最后一项重复。省略指标表示数据不足或不可用，不表示指标值为零；原始行情和内部指标计算仍使用完整精度。`get_stock_fund_flow` 默认只返回汇总，`period` 可选择分时序列、5日序列或两者。

## 返回格式与精简规则

下表省略命名空间前缀和部分业务字段；字段的完整类型以 `ai/model/` 和 `ai/tool/SecurityEventToolSnapshots.kt` 为准。所有成功数据都是 JSON 对象，不含格式化缩进。

| 工具 | 返回数据结构（主要字段） |
|---|---|
| `get_portfolio_analysis` | `{generatedAt, baseCurrency, totalMarketValueCny, ...盈亏汇总, holdings:[持仓快照], alertsSummary, dataFreshness}` |
| `get_holding_analysis` | `{code,name,marketType,currency,...持仓盈亏,activeAlerts:[],relatedNews:[],dataFreshness}` |
| `get_kline_data` | `{code,name,period,currency,source,asOf?,requestedLimit,pointsCount,volumeUnit,highestHigh?,lowestLow?,points:[{date,open,close,high,low,volume,changePercent,...可用指标}],latestPoint?}` |
| `get_indicator_snapshot` | `{code,name,status,price?,date?,currency,source?,ma5?,ma20?,ma60?,ma120?,...可用指标,holdingWindowComplete?}` |
| `get_market_news` | `{generatedAt,total,limit,possiblyTruncated,items:[{title,summary,source,time?,sourceType}]}` |
| `get_stock_news` | `{keyword,code?,count,limit,possiblyTruncated,articles:[{title,summary,source,time?,sourceType,url,kind?,isStale?}]}` |
| `get_stock_context` | `{code,name,generatedAt,quote?,technical?,events?,stockFundFlow?,profile?,warnings:[]}` |
| `get_stock_events` | `{code,limit,possiblyTruncated,items:[事件],warnings:[]}` |
| `get_stock_fund_flow` | `{code,period,currency,unit,sourceUpdatedAt?,...可用资金流汇总,intradayPoints?,fiveDaySummary?,provider,isStale,warnings:[]}` |
| `get_alerts` | `{total,enabledCount,recentTriggeredCount,alerts:[{id,code,name,alertType,threshold,actionHint?,isEnabled,lastTriggeredAt?}]}` |
| `create_alert` | `{success:true,id,message}` |
| `delete_alert` | `{success:true,message}` |
| `calculate_what_if` | `{code,name,currentPrice,targetPrice,targetChangePercent,...目标市值及盈亏差异}` |
| `get_market_indices` | `{generatedAt,groups:[{market,label,indices:[{code,name,price?,changePercent?,changeAmount?,sourceTime?,priceUnit,currency?,status}]}]}` |
| `get_fund_flow` | `{generatedAt,updateTime?,unit,currency,northFlow:{...},southFlow:{...}}` |
| `search_securities` | `{generatedAt,keyword,total,limit,possiblyTruncated,results:[{code,displayCode,name,marketType}]}` |
| `get_market_status` | `{generatedAt,isTodayHoliday,sessions:{市场:状态},hkdToCnyRate}` |
| `get_watchlist` | `{generatedAt,items:[{code,name,marketType,quoteStatus,currentPrice?,changePercent?,changeAmount?,sourceTime?,currency?}]}` |
| `get_transactions` | `{total,limit,offset,hasMore,transactions:[交易明细]}` |
| `get_transaction_summary` | `{buyAmountCny,sellAmountCny,...收益和现金流汇总,tradeCount,buyCount,sellCount,firstTradeDate?,lastTradeDate?}` |
| `get_holding_transaction_history` | `{code,name,marketType?,...持仓收益,totalTransactions,limit,offset,hasMore,transactions:[交易明细]}` |
| `get_portfolio_performance` | `{currency,startDate,endDate,startValue,endValue,...现金流及收益}` |

- **K 线**：默认 `limit=60`，范围 1–120；`include_indicators=false` 仅返回 OHLCV 和涨跌幅。输出裁剪前用额外历史数据计算指标，避免短输出导致 MA120 丢失；供应商历史不足时仍省略缺失指标。基金净值仅支持 DAILY。
- **组合上下文**：`get_stock_context` 默认仅请求 `quote`、`technical`。未请求的 section 完全省略；需要事件、资金流或公司资料时显式指定 `sections`。资金流 section 默认只含汇总。
- **资讯**：`get_stock_news` 只保留一份 `articles`，不再重复返回 `events`。新闻和证券搜索最多 50 条；`total/count` 是本次返回数，`possiblyTruncated=true` 仅表示达到上限，不能当成已知总量。
- **交易分页**：两种明细工具最多 200 条；使用 `offset + transactions.length` 获取下一页，`hasMore=false` 时结束。持仓历史的收益汇总始终基于全量交易，不受分页影响。
- **参数错误**：缺失必填参数、错误类型、越界、未知字段、非法枚举和 `from > to` 返回 `INVALID_ARGS`。不会把字符串数字或小数 ID 静默转换后执行。创建预警只允许四种价格/涨跌幅类型，阈值必须大于 0；删除不存在的预警返回 `NOT_FOUND`。
- **缺失行情**：自选和指标快照省略缺失价格并标记 `unavailable`；基金自选使用基金行情接口。有效零值（例如成交量 0 或资金流 0）仍然保留。
- **来源与时间**：`generatedAt` 为 ISO 8601 UTC 响应生成时间；资讯 `time` 在已知 epoch 时也是 ISO 8601。交易日期、事件 `publishedAt`、资金流 `sourceUpdatedAt` 和预警触发时间保留 epoch 毫秒。K 线 `date/asOf` 是交易日期。行情的 `sourceTime/quoteDisplayTime` 保留供应商原始显示时间，可能不含时区；不伪造时间戳。组合 `dataFreshness.source` 区分 NETWORK、CACHE、UNAVAILABLE、UNKNOWN 和 MIXED；NETWORK 不保证是此刻成交价，无法可靠判定过期时省略 `isQuoteStale`。
- **单位**：股票/基金价格给出币种，指数价格为 `index_points`；K 线成交量保留供应商单位 `provider_native`，黄金 K 线可能是期货代理序列，其币种也标记为 `provider_native`，不要直接按持仓现货价格解读。沪深港通资金流为供应商货币的万元（`10000_provider_currency`）；不能将所有市场的金额默认视为人民币。

兼容提示：这次减少了默认 K 线条数、默认上下文 section 和资金流序列；删除了重复的新闻事件数组，指数改为精简快照。依赖完整输出的客户端应显式传入参数，并重新获取工具定义。

## CETP v1 与副作用扩展

CETP v1 的标准互操作范围只允许读取数据，不允许写入、提交或删除。九财花当前对外工具中的
`jiucaihua__create_alert` 和 `jiucaihua__delete_alert` 会改变价格预警配置，因此不属于
只读 CETP v1 核心。Consumer 应将这两个工具视为有副作用操作，并在调用前应用自己的审批与
安全策略。

其余 20 个工具只读取或分析投资数据，不修改持仓、交易流水、自选列表或其他业务数据。

## 协议接口

Authority: `com.jiucaihua.app.clawseed.tools`

### list_tools

```
content call --uri content://com.jiucaihua.app.clawseed.tools --method list_tools
```

返回所有白名单内工具的定义（含 `jiucaihua__` 前缀）。

### execute_tool

```
content call --uri content://com.jiucaihua.app.clawseed.tools \
  --method execute_tool \
  --extra tool_name:s:jiucaihua__get_market_status
```

`tool_name` 必须使用带前缀的名称，裸名会返回 `TOOL_NOT_FOUND`。

### get_provider_info

```
content call --uri content://com.jiucaihua.app.clawseed.tools --method get_provider_info
```

## 安全

- Provider 声明自定义权限 `com.clawseed.permission.ACCESS_TOOLS`（protectionLevel=normal）
- Consumer 需在 Manifest 中 `<uses-permission>` 申请该权限
- 白名单机制确保只有显式声明的工具对外暴露，未来新增的内部工具不会自动泄露
- `normal` 权限不构成强身份认证；Provider 白名单和 Consumer 侧审批仍然必要

## 关键文件

| 文件 | 职责 |
|---|---|
| `cetp/CetpToolProvider.kt` | ContentProvider 实现，CETP 协议适配层 |
| `cetp/CetpToolEntryPoint.kt` | Hilt EntryPoint，从 ContentProvider 访问 ToolRegistry |
| `cetp/CetpDiscoveryService.kt` | 发现服务，供 Consumer 扫描 |
| `ai/tool/ToolRegistry.kt` | 工具注册表（Hilt Singleton） |
| `ai/tool/ToolExecutor.kt` | 工具执行接口 |
| `ai/tool/*.kt` | 22 个对外 ToolExecutor 实现 |
| `ai/model/CetpToolSnapshots.kt` | 新增 4 个工具的输出模型 |
| `ai/usecase/BuildCetpToolSnapshotsUseCase.kt` | 新增 4 个工具的 Use Case |

## 新增工具

新增工具时需修改以下位置：

1. 创建 `ToolExecutor` 实现类（`ai/tool/`）
2. 如需新模型，创建快照类（`ai/model/`）和 Use Case（`ai/usecase/`）
3. 在 `AiModule.kt` 中注册 `@Binds @IntoSet`
4. 如需对外暴露，在 `CetpToolProvider.EXTERNAL_TOOLS` 中添加内部工具名

仅第 4 步决定了工具是否对外暴露，未加入白名单的工具仅应用内 AI 可用。

## 证券事件工具契约

`code` 仅接受九财花规范代码，例如 `sh600519`、`hk00700`、`usr_AAPL`。`kinds` 是 `NEWS`、`ANNOUNCEMENT`、`PERIODIC_REPORT`、`RESEARCH` 的数组，`limit` 范围为 1 至 50。

事件包含 `externalId`、`kind`、`title`、`summary`、`publisher`、`publishedAt`、`symbols`、`url`、`provider` 和 `isStale`；研报专属字段放在 `attributes`。个股资金流的有效数值为 JSON number；缺失字段省略。

Provider 会递归把 JSON 数组、对象和 `null` 转为 Kotlin `List`、`Map` 和 `null`，与 App 内工具入口一致。成功结果使用紧凑 JSON 并省略 `null` 字段，保留有效的零值。非法参数返回 `INVALID_ARGS`，不存在的资源返回 `NOT_FOUND`，不支持市场返回 `UNSUPPORTED_MARKET`，没有可用供应商时返回 `PROVIDER_UNAVAILABLE`；未知异常返回不含请求 URL 或响应内容的 `INTERNAL_ERROR`。部分 section 失败仍返回成功快照，并在 section 状态和顶层 `warnings` 中标记。
