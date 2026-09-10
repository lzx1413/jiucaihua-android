# 项目性能审查（2026-09-10）

初次审查基于提交 `c723e0e`（仓库版本 1.3.1），覆盖持仓、自选、资讯、大盘、详情、AI、备份、数据库、网络与后台任务的主要执行路径。用户反馈集中在持仓页上下滑动和分类展开。以下审查记录保留优化前状态；用户授权后的实施及验证结果见文末。

## 结论与证据边界

持仓分类内的持仓行没有独立懒加载，是与反馈最吻合的首要优化点；整组高度动画、主线程收益计算和重复刷新可能放大卡顿。代码足以确认这些执行结构，但各项对实际掉帧的贡献仍需同版本 Release + Perfetto 对照验证。

真机为 23127PN0CC，安装的是 **1.3.0 / versionCode 3 / DEBUGGABLE**，与仓库版本不同。测试时 A/H 股已收盘，收益图表折叠，A 股分类显示 4 条持仓。两个 ADB transport 指向同一部手机，使用 transport 48。

每组操作前执行 `dumpsys gfxinfo com.jiucaihua.app reset`，操作后读取统计：

| 场景 | 帧数 | P50 | P95 | P99 | Janky frames（非 legacy） | Legacy 指标 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 六次上下滑动，各 350ms | 496 | 6ms | 12ms | 20ms | 3 / 0.60% | 11.29% |
| A 股分类三轮收起/展开，点击间隔 1 秒 | 1036 | 7ms | 14ms | 61ms | 1 / 0.10% | 34.75% |

这是应用窗口在操作时段内的统计，包含时段内其他绘制，并非精确切分的动画帧。两种 jank 口径不能混用。观察到了长尾帧耗时，但这轮未证明持续严重掉帧，也不能据此给当前 1.3.1 定量结论。GPU P95 分别为 5ms、3ms，没有充分依据优先归因于 GPU；仍需线程时间线定位 CPU、布局及调度开销。

原始数据暂存在 `/tmp/jiucaihua-perf-scroll.txt` 和 `/tmp/jiucaihua-perf-expand.txt`。启动和唤屏阶段的混合统计没有纳入上表。未采集堆转储、Compose 重组计数或 Perfetto，因此不宣称已证明内存泄漏或某函数的实际耗时。

## 优先处理：持仓与刷新路径

### 1. P1：分类内所有持仓一次性创建、测量

- 位置：`PortfolioScreen.kt:487,512`；`CategoryHoldingSection.kt:117,128`。
- 外层 `LazyColumn` 的 item 是整个分类，内部 `Column + forEachIndexed` 创建该分类全部持仓行。分类刚进入视口，即使仅显示几行，其他行也参与组合和测量。
- `expandVertically/shrinkVertically` 对整个持仓组做高度动画，动画期间持续产生布局工作。持仓越多，分类进入视口、展开和重建时的开销越大。
- 建议：同一个 `LazyColumn` 中分别放分类标题、列标题、持仓行；持仓行使用稳定的 `holding.id` key 和合适的 `contentType`。展开状态按市场类型保存，移出 item 内的普通 `remember`，避免 item 离开组合后状态丢失。用箭头旋转、淡入或有限的可见行动画替代整个长列表的高度动画。
- 不建议在分类中另嵌一个无高度限制的纵向 `LazyColumn`。

### 2. P1：行情刷新反复执行交易统计，CPU 工作留在主线程

- 位置：`PortfolioViewModel.kt:245,359,410`；`GetPortfolioUseCase.kt:259`；`GetTransactionSummaryUseCase.kt:16`；`TransactionFifoCalculator.kt:15`；`GetPortfolioPeriodReturnsUseCase.kt:21`。
- `viewModelScope.launch` 未切换 dispatcher。Room 查询和 Retrofit 网络等待本身可挂起，但返回后的映射、排序、FIFO、收益统计继续在调用线程执行。
- 有历史快照时，一次持仓刷新会为累计收益调用交易汇总，再为当前图表快照调用同一汇总；周期收益另外读取全部交易。交易汇总最多取 5000 条，先排序，再在 FIFO 内排序，并创建交易与批次匹配结果；调用者在上述两处实际只需要现金流入减流出。
- `observeSnapshots`、缓存加载、行情刷新还会分别发起周期收益计算，缺少合并/取消过时任务。
- 建议：现金流使用专用 SQL 聚合或由交易变更驱动的缓存，不在每次报价变化时做完整 FIFO；历史基线只查询需要的一条。CPU 密集计算放到可注入的 Default dispatcher，派生收益状态统一计算，过时结果不得覆盖新状态。
- 优化时必须保留现金流调整、基准日期、汇率及收益口径，不能为了速度删除计算。

### 3. P1：定时刷新与实际请求脱离，可重叠且页面不可见仍运行

- 位置：`PortfolioViewModel.kt:223,245`；`WatchlistViewModel.kt:82,102`；`PortfolioScreen.kt:132`。
- 定时循环调用普通 `refreshQuotes()`；该函数又启动独立 `viewModelScope.launch`。循环等待固定 10 秒，不等待本轮请求结束。初始化时的数据库首次 emission 与交易时段自动刷新也可能重复启动。
- 持仓和自选仅在 `onCleared` 时停止；导航去详情或应用切后台不必然销毁它们。`collectAsStateWithLifecycle` 只控制 UI 订阅，不会自动停止这些独立生产任务。
- 首次打开持仓页就创建自选 ViewModel，并启动资讯观察和资讯刷新，即使用户尚未查看这些标签。
- 建议：刷新主体改为可等待的 suspend 操作；同类行情只保留一个在途任务，重复触发合并，并处理过时返回。绑定前台生命周期与当前标签可见性，资讯按需首次加载，后台使用既有 WorkManager 策略。
- 详情页 `DetailScreen.kt:86`、独立大盘页 `MarketScreen.kt:44` 已有 `LifecycleResumeEffect`，可以作为现有模式参考。

### 4. P1：满足收盘快照条件时，一次刷新会再次请求完整持仓行情

- 位置：`PortfolioViewModel.kt:249,259`；`RecordSnapshotUseCase.kt:37,43`；`DailySnapshotSchedule.kt:9`。
- 已经拿到 summary 后，`recordSnapshot()` 又调用 `getPortfolioWithQuotes()`，并继续请求基准指数、计算交易汇总、保存持仓与资产快照；快照观察者再触发收益派生计算。
- 条件是工作日 16:10 后且市场状态满足要求，不能描述为全天每 10 秒都双倍请求。当前市场会话只返回 A/H 股，此时自动行情循环一般已暂停，但手动刷新和后台快照仍会遇到重复请求。
- 建议：快照接口接受刚取得的 summary；独立后台调用可自行获取一次。明确同日快照更新策略并避免无意义重复写入，保留收盘后补记能力。

## 其他模块

| 优先级 | 模块与位置 | 已观察到的问题、触发条件 | 建议 |
| --- | --- | --- | --- |
| P2 | 自选 `WatchlistViewModel.kt:109,129` | `forEach` 串行逐只请求，股票也每次只传一个 code；长列表和慢接口延长整轮刷新，又与 10 秒定时重叠 | 按市场批量获取股票报价，基金限制并发；与持仓共用同代码的在途请求 |
| P2 | 基金 `FundRepositoryImpl.kt:30,42`；持仓 `GetPortfolioUseCase.kt:51` | 每只基金启动 async，无应用层并发上限；整页等所有市场和基金结果到齐才发布。多基金或慢接口拖长加载，不等同于网络等待阻塞 UI | 限制并发，增加明确的整轮时间预算，先显示缓存；分市场更新要明确数据时间和汇总一致性 |
| P2 | 资讯 `NewsRepositoryImpl.kt:566`；`NewsFlashDao.kt:14`；`PortfolioViewModel.kt:110` | 观察查询取完整表/内容，之后才在主线程 Flow map 中筛选 24h、转模型；持仓首页也订阅并处理 | 将筛选、投影、数量限制下推 SQL，长列表分页；映射切到后台，按需订阅；拆分状态但不要假定所有子组件必定重组 |
| P2 | 持仓图表 `EarningsChartView.kt:66,212` | 图表组合时重新计算收益序列，绘制时重新创建点列表、Path 和日期文本；全部历史或频繁更新时增大分配与计算 | 数据派生用 remember/后台计算，绘制几何用 drawWithCache，缓存标签并按像素密度降采样。图表默认折叠，本轮测量中不是活跃图表热点 |
| P2 | 详情 `DetailViewModel.kt:103`；`DetailScreen.kt:117` | 持仓、交易历史、报价、K 线顺序加载，K 线返回前全页仍是 loading；慢 K 线会让已到达的报价不可见 | 优先展示缓存和报价，K 线及附加信息独立加载；保留各自 loading/error |
| P2 | 详情列表/图表 `DetailScreen.kt:124,382`；`KLineChartView.kt:124,253`；`FundNavChartView.kt:86` | 页面使用滚动 Column，交易记录等内部 forEach；AndroidView update 执行时重新构造图表数据并刷新 | 长记录懒加载/分页，图表只在相关数据或样式变化时更新；需重组追踪判断当前 update 实际频率，不宣称每个 tick 都重建 |
| P2 | 备份 `BackupViewModel.kt:69,124` | 主线程打开 URI、读取整个备份字节并反序列化；导出序列化也在主线程。大备份或慢文档提供器可明显冻结页面 | 打开/读取/关闭流放 IO，JSON 编解码移出主线程，评估流式处理；恢复数据库本身已有 IO 调度 |
| P2 | AI `OpenAiCompatibleLlmAgentClient.kt:49`；`AiAgentOrchestrator.kt:22,35` | 每次 nextStep 新建 Retrofit，未注入复用 OkHttp；每轮发送累积完整对话。4 次迭代上限不限制跨轮历史 | 按 endpoint 复用 Retrofit/OkHttp，缓存工具定义，设置对话与工具结果体积预算，避免长期会话不断增加请求体 |
| P3 | 后台 `QuoteRefreshWorker.kt:24`；`CheckAlertsUseCase.kt`；`SecurityEventSyncWorker.kt:44` | 行情刷新、告警、证券事件任务分别取数据，没有统一的跨调用在途合并；证券事件按 code 串行同步 | 优先在 repository 复用短期报价和在途请求，限制事件同步并发；不是已证明的主线程卡顿根因 |
| P3 | CETP `CetpToolProvider.kt:81` | 同步 Binder 调用用 runBlocking(IO) 等完整工具链，没有工具级总时间预算；外部并发调用可能占用 Binder 线程 | 增加工具总时限和并发限制；跨进程调用通常占用 Binder 线程，不能据此断言阻塞 UI 主线程 |
| P2（验证基础） | `app/build.gradle.kts:96` 附近 | Release 的 `isMinifyEnabled = false`；真机当前还是 DEBUGGABLE 包。没有项目级 Macrobenchmark/Baseline Profile 配置 | 建立同代码、同签名测试条件下的 Release 基准；启用 R8 前核对 Moshi/反射/图表规则及回归，不直接把构建开关当成根因修复 |

## 已有合理设计与不应误判的地方

- 持仓按市场并发请求，A/H/US 股接口支持批量；Room suspend 查询没有发现 `allowMainThreadQueries`。
- 大部分资讯抓取、HTML 解析和汇率获取已有 IO 调度；网络等待慢不代表主线程同步阻塞。
- UI 使用 `collectAsStateWithLifecycle`，详情及独立大盘页有前台刷新控制；WorkManager 使用唯一周期任务。
- 持仓图表范围过滤已经使用 remember；图表默认折叠。无需把所有滚动问题都归因于图表。
- 未发现足够证据证明图片内存泄漏、GPU 饱和或持续 ANR。仅靠 List 参数不稳定，也不能断言整个 Compose 页面每次全部重组。

## 建议实施与验收顺序

1. 先改持仓分类为逐行懒加载，保存展开状态，简化整组高度动画。这直接对应用户反馈，避免同时修改收益算法干扰验证。
2. 再合并刷新任务并绑定可见性，复用已有 summary 记录快照；把收益派生移出主线程、减少重复交易查询。
3. 依次处理自选批量请求、资讯查询、备份主线程 IO、详情渐进显示和图表缓存。
4. 使用同一份代码的 Release 包，固定设备、刷新率、持仓数量、图表状态和手势；至少覆盖现有数据、单分类 50/200 行、较长交易历史，分别测滑动、展开、行情刷新同时滚动。规模测试应使用测试数据环境，避免修改用户真实账户。
5. Perfetto 检查 FrameTimeline、主线程 Compose layout/measure、GC、数据库/网络回调；对照每轮请求数和收益计算次数。验证离开页面后前台轮询停止，返回恢复；慢接口超过刷新间隔时不叠加同类请求。
6. 修改后运行已有收益/FIFO/快照回归测试，构建并安装真机。优化前后保持收益数值、排序、长按操作和展开状态正确，按设备实际帧预算评估改善。

## 框架依据

- [Android：Lazy 列表中一个 item 放多个元素，会共同组合与测量；应使用 Release + R8 测试](https://developer.android.com/develop/ui/compose/lists)
- [Android：viewModelScope 默认在主线程，suspend 本身不转移工作线程](https://developer.android.com/kotlin/coroutines)
- [Android：协程 main-safe 与 dispatcher 最佳实践](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Android：Compose 性能最佳实践，缓存计算、稳定 key 与缩小状态读取范围](https://developer.android.com/develop/ui/compose/performance/bestpractices)

## 本轮优化实施与验证

本轮已落地持仓及刷新路径的第一批优化：

- 分类标题、列标题和每条持仓分别作为 LazyColumn item，提供稳定 key/contentType；折叠状态及持仓列表滚动位置提升到标签容器保存。取消整个分类的高度动画，保留箭头旋转；分类汇总改为按可用宽度分配，避免展开箭头溢出。
- 持仓和自选仅在各自标签处于前台时刷新。RefreshRunner 合并重复请求，业务变更最多补一轮，定时循环等待本轮完成；离开页面取消任务，取消信号不再被行情回退逻辑吞掉。旧缓存和已失效的行情结果不会覆盖新行情。
- 行情解析、持仓汇总和收益派生放到可注入的后台 dispatcher；合并并取消过时的收益/历史查询。资讯延迟到打开资讯标签时订阅和首次刷新，其 Flow 映射移出主线程。
- 现金流改用 SQL 聚合，保留原交易分析“最近 5000 条交易”的窗口和汇率口径；周期收益/收益历史只加载 CASH_IN/CASH_OUT，保持其原本不限条数的口径。最早收益基准仅查询首条资产快照及对应日期持仓快照，保留逐持仓合并规则。
- 收盘快照复用本轮 summary；自选按市场批量请求；基金报价请求增加 repository 级并发上限 4。

没有修改数据库 schema、收益公式、用户交易或现金记录。详情、备份、AI、R8 与图表绘制缓存等后续项目仍保留在上述审查清单中。

验证结果：

- `:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:assembleDebugAndroidTest` 成功，53 个单元测试通过。
- 真机内存 Room 数据库：6000 条混合交易下，空数据、时间截止、同时间戳排序、5000 条窗口和汇率换算的新旧现金流结果一致；不限条数的现金流列表与原全量结果过滤后相同。
- 真机 Compose：200 条持仓的屏幕外行未提前组合，滚动到底后的点击/长按正常；分类折叠状态恢复正常。两项 UI 测试通过。
- MIUI 起初拒绝测试框架的后台 Activity 启动，随后测试又受到锁屏/清理影响。测试规则现先通过 shell 正常打开目标应用，并仅为测试 Activity 临时保持亮屏；没有调整手机后台权限或常驻设置。修正测试前台启动方式后，两项 UI 测试完整通过。
- 优化后的 1.3.1 Debug 已以 `adb install -r -t` 覆盖安装，保留现有应用数据；检查了真实持仓页布局。

同设备、相同六次滑动和三轮分类开合操作的补测：

| 优化后场景 | 帧数 | P50 | P95 | P99 | Janky frames（非 legacy） |
| --- | ---: | ---: | ---: | ---: | ---: |
| 首轮滑动 | 478 | 6ms | 20ms | 42ms | 7 / 1.46% |
| 分类收起/展开 | 1110 | 6ms | 8ms | 89ms | 2 / 0.18% |
| 预热后再次滑动 | 536 | 6ms | 13ms | 19ms | 2 / 0.37% |

展开的大多数帧耗时降低，但尾部仍有波动；首轮滑动也没有稳定优于原采样。这些是不同版本的 Debug 实际数据，不能把单轮结果宣传为固定百分比改善。逐行懒加载、请求合并与结果正确性已有自动化验证；正式性能收益仍需相同代码基线的 Release/FrameTimeline 对照。

原始补测文件：`/tmp/jiucaihua-optimized-scroll.txt`、`/tmp/jiucaihua-optimized-expand.txt`、`/tmp/jiucaihua-optimized-scroll-warm.txt`。布局截图：`/tmp/jiucaihua-optimized.png`。
