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
  │  19 个 ToolExecutor
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
| `jiucaihua__get_kline_data` | `get_kline_data` | K线图表数据 | `code` (必须), `period`, `limit` |
| `jiucaihua__get_indicator_snapshot` | `get_indicator_snapshot` | 技术指标快照 | `code` (必须), `cost_price`, `hold_days` |
| `jiucaihua__get_market_news` | `get_market_news` | 市场资讯摘要 | `topic`, `query`, `limit` |
| `jiucaihua__get_stock_news` | `get_stock_news` | 个股相关资讯；`code` 存在时精确关联腾讯事件，`name` 保持旧搜索兼容 | `code` 或 `name` 至少一个，`kinds`, `limit` |
| `jiucaihua__get_stock_context` | `get_stock_context` | 个股行情、技术摘要、事件和个股资金流的复合快照 | `code` (必须), `sections`, `event_limit` |
| `jiucaihua__get_stock_events` | `get_stock_events` | 新闻、公告、定期报告和研报的统一事件列表 | `code` (必须), `kinds`, `limit` |
| `jiucaihua__get_stock_fund_flow` | `get_stock_fund_flow` | 单只 A 股资金流，和沪深港通资金流分开 | `code` (必须), `period` |
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
| `jiucaihua__get_holding_transaction_history` | `get_holding_transaction_history` | 单标的交易历史和收益拆解 | `code` (必须), `market_type`, `limit` |
| `jiucaihua__get_portfolio_performance` | `get_portfolio_performance` | 组合真实收益概览，按总资产和现金流变化分析 | `from`, `to` |

## CETP v1 与副作用扩展

CETP v1 的标准互操作范围只允许读取数据，不允许写入、提交或删除。九财花当前对外工具中的
`jiucaihua__create_alert` 和 `jiucaihua__delete_alert` 会改变价格预警配置，因此不属于
只读 CETP v1 核心。Consumer 应将这两个工具视为有副作用操作，并在调用前应用自己的审批与
安全策略。

其余 17 个工具只读取或分析投资数据，不修改持仓、交易流水、自选列表或其他业务数据。

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
| `ai/tool/*.kt` | 19 个 ToolExecutor 实现 |
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

每个事件固定包含 `externalId`、`kind`、`title`、`summary`、`publisher`、`publishedAt`、`symbols`、`url`、`provider` 和 `isStale`；研报专属字段放在 `attributes`。个股资金流的数值字段为 JSON number 或 `null`。

Provider 会递归把 JSON 数组、对象和 `null` 转为 Kotlin `List`、`Map` 和 `null`，与 App 内工具入口一致。非法参数返回 `INVALID_ARGS`，不支持市场返回 `UNSUPPORTED_MARKET`，没有可用供应商时返回 `PROVIDER_UNAVAILABLE`；未知异常返回不含请求 URL 或响应内容的 `INTERNAL_ERROR`。部分 section 失败仍返回成功快照，并在 section 状态和顶层 `warnings` 中标记。
