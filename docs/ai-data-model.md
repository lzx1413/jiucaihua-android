# AI 数据模型与 Agent 读取规范

## 目的

阶段 8 的第一优先级不是聊天 UI，而是先定义 AI 可稳定读取的理财数据模型。
当前数据库已经足够支撑 agent 做两类核心能力：
- 全局投资组合分析
- 单个标的分析

但现有 Room entity 和部分 domain model 更偏向内部业务实现，不适合直接暴露给 agent。本文档用于固定阶段 8 的数据口径，避免工具层、编排层、UI 层各自解释一套字段语义。

## 当前数据库概览

### 1. holdings
文件：`app/src/main/java/com/jiucaihua/app/data/local/entity/HoldingEntity.kt`

用途：保存当前持仓快照。

字段：
- `id`: 主键
- `code`: 证券代码
- `name`: 证券名称
- `marketType`: 市场类型字符串
- `currency`: 币种
- `costPrice`: 成本单价
- `holdingAmount`: 持仓总成本金额
- `holdingShares`: 持仓份额/股数
- `isSoldOut`: 是否已清仓
- `createdAt`: 创建时间
- `updatedAt`: 更新时间

说明：
- 这是“当前持仓状态表”，不是交易流水表。
- 适合回答“我现在持有什么、成本多少、当前盈亏如何”。
- 不适合直接回答“我历史上怎么调仓、已实现盈亏多少、什么时候买卖过”。

### 2. stock_cache
文件：`app/src/main/java/com/jiucaihua/app/data/local/entity/StockCacheEntity.kt`

用途：保存股票最新行情缓存。

字段：
- `code`
- `name`
- `currency`
- `price`
- `yestClose`
- `open`
- `high`
- `low`
- `volume`
- `amount`
- `changePercent`
- `changeAmount`
- `time`
- `marketType`
- `updatedAt`

说明：
- 这是实时或最近一次拉取的行情快照。
- 适合支撑 agent 的实时分析、价格解释、波动概览。
- 不应直接作为最终工具返回结构，而应统一映射到 AI 读模型。

### 3. fund_cache
文件：`app/src/main/java/com/jiucaihua/app/data/local/entity/FundCacheEntity.kt`

用途：保存基金估值与净值缓存。

字段：
- `code`
- `name`
- `estimatedValue`
- `dailyChangePercent`
- `netAssetValue`
- `estimateTime`
- `navDate`
- `updatedAt`

说明：
- 基金当前更接近“估值快照”，不是股票式逐笔行情。
- 对 agent 来说，应该统一解释为“当前价格/估值 + 涨跌幅 + 更新时间”。

### 4. alerts
文件：`app/src/main/java/com/jiucaihua/app/data/local/entity/AlertEntity.kt`

用途：保存价格预警规则。

字段：
- `id`
- `code`
- `name`
- `alertType`
- `threshold`
- `isEnabled`
- `lastTriggeredAt`
- `createdAt`

说明：
- 可供 agent 输出风险提示、提醒密度、重点关注标的。
- 不应只返回原始规则，还应整理为“当前有哪些重要预警、哪些最近触发过”。

## 当前可复用的数据能力

### 组合聚合
文件：`app/src/main/java/com/jiucaihua/app/domain/usecase/GetPortfolioUseCase.kt`

已提供：
- 总市值
- 总成本
- 总收益
- 总收益率
- 当日盈亏
- 持仓列表
- 最新更新时间
- 港股汇率折算
- 股票/基金行情整合

这是全局分析工具的首选数据入口。

### 单标的 K 线
文件：`app/src/main/java/com/jiucaihua/app/domain/usecase/GetKLineDataUseCase.kt`

已提供：
- 日/周/月 K 线
- 统一 `KLineData` 结构

这是单条目分析中技术面部分的首选入口。

### 资讯
文件：`app/src/main/java/com/jiucaihua/app/domain/repository/NewsRepository.kt`

已提供：
- 市场资讯
- 个股资讯

可直接用于 agent 的新闻关联分析。

### 预警
文件：`app/src/main/java/com/jiucaihua/app/domain/repository/AlertRepository.kt`

已提供：
- 全部预警
- 指定标的预警
- 启用中的预警

可直接用于全局风险提示和单标的风险摘要。

### 市场状态
文件：`app/src/main/java/com/jiucaihua/app/domain/usecase/IsMarketOpenUseCase.kt`

已提供：
- 各市场交易状态

可直接作为 agent 分析时的上下文信息。

## 当前模型的语义限制

### 1. holdings 不是交易流水
数据库当前没有交易明细表。
因此当前 agent 支持：
- 当前持仓分析
- 当前浮盈浮亏分析
- 当前仓位分布分析
- 基于当前持仓的场景推演

当前不支持：
- 历史买卖轨迹分析
- 已实现盈亏分析
- 资金流入流出复盘
- 成本分批摊薄过程回放

### 2. `holdingAmount` 与 `holdingShares` 不能直接暴露给 agent
相关文件：
- `app/src/main/java/com/jiucaihua/app/domain/model/Holding.kt`
- `app/src/main/java/com/jiucaihua/app/presentation/holdings/AddEditHoldingViewModel.kt`

现状：
- `holdingAmount` 在录入层由 `costPrice * holdingShares` 推导
- 在基金场景中又承担成本基数含义
- `holdingShares` 对股票表示股数，对基金更接近份额

这意味着：
- 这些字段适合内部计算
- 不适合作为 agent 的统一语义字段直接使用

### 3. 汇率与行情新鲜度应在 AI 层标准化
港股需要汇率折算，行情也有缓存/实时之分。
agent 不应该自己理解这些底层差异，而应该拿到统一字段：
- 人民币口径成本
- 人民币口径市值
- 数据更新时间
- 数据是否过旧

## AI 专用标准化读模型

阶段 8 先不改 Room schema，先新增 AI 读模型。
这些模型只服务 agent/tool use，不替代现有 UI 领域模型。

### 1. PortfolioAnalysisSnapshot
用途：给 agent 做全局分析。

建议字段：
- `generatedAt`: 快照生成时间
- `baseCurrency`: 基准币种，当前固定为 `CNY`
- `totalMarketValueCny`: 组合总市值
- `totalCostCny`: 组合总成本
- `totalUnrealizedPnlCny`: 组合总浮盈亏
- `totalUnrealizedPnlPercent`: 组合总收益率
- `todayPnlCny`: 当日盈亏
- `marketSessions`: 当前市场状态
- `holdings`: 持仓分析列表
- `alertsSummary`: 预警摘要
- `dataFreshness`: 数据新鲜度摘要

### 2. HoldingAnalysisSnapshot
用途：给 agent 做单个标的分析。

建议字段：
- `code`
- `name`
- `marketType`
- `currency`
- `positionUnits`: 持仓数量，统一语义字段
- `unitLabel`: `股` / `份`
- `costBasisCny`: 成本总额（人民币口径）
- `avgCostPerUnit`: 平均成本单价
- `currentPrice`: 当前价格/估值
- `marketValueCny`: 当前市值（人民币口径）
- `unrealizedPnlCny`: 当前浮盈亏
- `unrealizedPnlPercent`: 当前收益率
- `latestQuoteTime`: 最新行情时间
- `activeAlerts`: 当前相关预警
- `relatedNews`: 相关资讯摘要
- `dataFreshness`: 当前标的数据新鲜度

### 3. DataFreshness
用途：让 agent 能判断“当前分析是不是基于过旧数据”。

建议字段：
- `quoteUpdatedAt`
- `quoteDisplayTime`
- `isQuoteStale`
- `source`: `LIVE` / `CACHE`

### 4. AlertsSummary
用途：全局风险提示。

建议字段：
- `enabledCount`
- `recentTriggeredCount`
- `affectedCodes`

## 字段统一原则

### 统一后的 agent 字段
无论股票还是基金，优先统一到以下语义：
- `positionUnits`
- `avgCostPerUnit`
- `currentPrice`
- `costBasisCny`
- `marketValueCny`
- `unrealizedPnlCny`
- `unrealizedPnlPercent`

### 不直接暴露给 agent 的内部字段
除非调试，不建议工具直接返回：
- `holdingAmount`
- `holdingShares`
- 原始 `StockCacheEntity`
- 原始 `FundCacheEntity`
- 原始 `HoldingEntity`

原因：
- 容易让模型自己推断业务语义
- 股票/基金口径不完全一致
- 后续修改内部实现会影响工具稳定性

## 工具层建议读取方式

### 全局分析工具
建议名称：`get_portfolio_analysis`

优先依赖：
- `GetPortfolioUseCase`
- `AlertRepository`
- `IsMarketOpenUseCase`

输出重点：
- 组合总览
- 市场分布
- 收益贡献排序
- 集中度提示
- 风险提示
- 数据新鲜度提示

### 单条目分析工具
建议名称：`get_holding_analysis`

优先依赖：
- `HoldingRepository`
- `StockRepository` / `FundRepository`
- `GetKLineDataUseCase`
- `NewsRepository`
- `AlertRepository`

输出重点：
- 当前持仓摘要
- 实时行情摘要
- K 线摘要
- 相关新闻摘要
- 预警摘要
- 数据新鲜度提示

## 当前支持与延后支持的能力边界

### 当前支持
- 当前组合分析
- 单标的分析
- 当前盈亏解释
- 当前仓位分布分析
- 场景推演
- 新闻关联分析
- 预警分析
- K 线技术面辅助分析

### 暂不支持
- 历史交易流水分析
- 已实现盈亏分析
- 多笔建仓拆分复盘
- 现金仓位分析
- 交易日志时间轴

## 后续数据库扩展建议

如果后续明确需要“历史归因分析”，再新增交易流水表。

建议新增：`PositionTransactionEntity`

建议字段：
- `id`
- `holdingCode`
- `side`
- `quantity`
- `price`
- `fee`
- `occurredAt`
- `note`

只有引入这类表之后，agent 才适合做：
- 已实现盈亏
- 调仓复盘
- 买入节奏分析
- 成本摊薄路径分析

## 实施约束

1. AI 工具必须优先复用现有 Repository / UseCase。
2. AI 层只新增薄封装，不反向污染业务层接口。
3. 工具返回结构要稳定、简洁、语义化。
4. 先做标准化读模型，再做工具，再做编排与聊天 UI。

## 推荐开发顺序

1. 新增本文档并在 `CLAUDE.md` 中引用
2. 新增 AI 标准化读模型
3. 新增全局分析与单条目分析 use case
4. 新增两个核心工具
5. 再继续做 orchestrator / chat UI / provider 接入
