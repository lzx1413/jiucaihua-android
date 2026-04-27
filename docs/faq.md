# Jiucaihua FAQ 常见问题

## 用户常见问题

### Q1: 行情数据不刷新怎么办？

**可能原因**：
1. 当前非交易时段（收盘或午休）
2. 网络连接异常
3. 数据源接口暂时失效

**解决方法**：
- 检查顶部市场状态标签（显示"交易中"才刷新）
- 手动下拉刷新
- 检查网络连接
- 冷启动后会显示缓存数据，属于正常现象

---

### Q2: 为什么港股显示的是港币价格？

港股原始报价为港币，在组合汇总时会自动按汇率折算为人民币。单条持仓显示原币价格，方便对比原始成本。

---

### Q3: 基金估值和净值有什么区别？

| 类型 | 说明 | 更新时间 |
|------|------|----------|
| 估算净值 (GSZ) | 当日实时估算 | 交易时段每分钟 |
| 单位净值 (DWJZ) | 上一交易日官方净值 | 每日收盘后 |

基金盈利计算基于估算净值，实际净值需等待基金公司发布。

---

### Q4: 价格预警为什么没有收到通知？

**可能原因**：
1. 预警未启用（检查预警页启用状态）
2. 系统通知权限未开启
3. APP 在后台被系统杀死（Android 后台限制）
4. 价格未达到阈值

**解决方法**：
- 在预警管理页检查预警启用状态
- 检查系统设置中的通知权限
- 确保价格已触发阈值

---

### Q5: 深色模式切换后界面不变化？

APP 支持跟随系统主题。请检查：
1. 系统设置中已开启深色模式
2. APP 未在设置页手动指定主题
3. 重启 APP 后生效（部分场景）

---

### Q6: 如何修改已清仓的持仓？

已清仓持仓可在持仓列表中长按删除，或重新编辑后取消清仓标记。历史交易明细暂不支持（当前为持仓状态表，非交易流水表）。

---

## 开发者常见问题

### Q7: 编译报错 "Cannot resolve symbol"？

**可能原因**：
1. Gradle 依赖未同步
2. Kotlin 版本不兼容
3. IDE 缓存异常

**解决方法**：
```bash
# 清理并重新构建
./gradlew clean
./gradlew assembleDebug

# Android Studio: File > Invalidate Caches and Restart
```

---

### Q8: 新浪 A 股接口返回空数据？

**必须携带 Referer 头**：
```kotlin
// SinaRefererInterceptor.kt
request.header("Referer", "https://finance.sina.com.cn")
```

未携带 Referer 时，新浪接口返回空字符串。

---

### Q9: 港股行情显示乱码？

港股接口返回 GBK 编码，需通过拦截器解码：
```kotlin
// GBKResponseInterceptor.kt
val body = response.body()?.bytes()
val decoded = String(body, Charset.forName("GBK"))
```

---

### Q10: 基金 JSONP 解析失败？

基金接口返回 JSONP 格式（`jsonpgz({...});`），需正则提取 JSON：
```kotlin
val regex = Regex("jsonpgz\\((.*)\\);")
val json = regex.find(response)?.groupValues?.get(1)
val dto = Gson().fromJson(json, FundQuoteDto::class.java)
```

---

### Q11: Room 数据库迁移失败？

当前项目版本较低，数据库 Schema 变化时直接删除重建。生产环境需配置 Migration：

```kotlin
// 如需正式迁移
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE holdings ADD COLUMN new_field TEXT")
    }
}
```

---

### Q12: WorkManager 后台任务不执行？

**可能原因**：
1. 设备省电策略限制
2. APP 被强制停止
3. 不满足执行条件（非交易时段跳过）

**调试方法**：
```bash
# 查看任务状态
adb shell dumpsys jobscheduler

# 强制执行（调试用）
adb shell cmd jobscheduler run -f com.jiucaihua.app <job_id>
```

---

### Q13: AI 助手提示 "API Key 无效"？

**检查项**：
1. API Key 格式是否正确
2. 模型名称是否正确（DeepSeek: `deepseek-chat`）
3. 端点地址是否正确
4. API Key 是否已过期或余额不足

**测试连通性**：
在设置页点击"测试连接"按钮验证。

---

### Q14: K 线图不显示？

**可能原因**：
1. 东方财富接口返回空（非交易日）
2. MPAndroidChart 渲染异常
3. 数据格式解析失败

**调试方法**：
```kotlin
// GetKLineDataUseCase.kt
// 检查返回的 KLineData.points 是否为空
Log.d("KLine", "points: ${data.points.size}")
```

---

### Q15: 如何添加新的数据源？

1. 在 `data/remote/api/` 新增 Retrofit 接口
2. 在 `data/remote/dto/` 新增 DTO（如需）
3. 在 `di/NetworkModule.kt` 注册 Retrofit 实例
4. 在对应 Repository 中集成解析逻辑
5. 复用或新增 UseCase

---

## 性能相关

### Q16: APP 启动较慢？

冷启动时需加载数据库和网络数据。优化建议：
- Room 数据库预加载（已实现）
- 网络请求延迟加载（已实现）
- 压缩启动时协程数量

---

### Q17: 内存占用较高？

K 线数据较大时可能占用内存。优化建议：
- K 线数据限制条数（默认 100 条）
- 不保留历史 K 线数据（每次重新请求）
- 图片资源使用矢量图

---

## 功能边界

### Q18: 当前不支持哪些功能？

| 功能 | 原因 |
|------|------|
| 历史交易流水 | 当前为持仓状态表，非交易流水表 |
| 已实现盈亏 | 无交易流水，无法计算 |
| 成本分批摊薄 | 无多笔买入记录 |
| 现金仓位 | 仅管理持仓，无现金账户 |
| 行业对比 | 无行业分类数据源 |

如需支持，需新增 `PositionTransactionEntity` 表记录交易明细。

---

### Q19: AI 助手能做什么？

当前支持（基于 Tool Use）：
- 投资组合分析
- 单标的详情查询
- K 线技术分析
- 资讯关联分析
- 假设盈亏推演
- 预警状态查询

暂不支持：
- 自动交易（无交易接口）
- 预测未来价格（AI 不具备）
- 行业资金流向（无数据源）

---

## 更新历史

| 日期 | 更新内容 |
|------|----------|
| 2025-04-26 | 初始版本，覆盖用户与开发者常见问题 |