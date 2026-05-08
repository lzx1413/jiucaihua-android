# 数据源接口说明

本文档详细说明 Jiucaihua 使用的外部数据源接口，包括请求方式、参数、返回格式和编码处理。

## 概览

| 数据源 | 用途 | 编码 | 数据类型 |
|--------|------|------|----------|
| 新浪财经 | A 股行情 | GB18030 | JS 变量 |
| 腾讯财经 | 港股行情 | GBK | 文本字段 |
| 天天基金 | 基金估值 | UTF-8 | JSONP |
| 东方财富 | K 线数据 | UTF-8 | JSON |
| 中国银行 | 港股汇率 | UTF-8 | HTML |
| 节假日 API | 交易日判断 | UTF-8 | JSON |

---

## 一、新浪财经 - A 股行情

### 接口地址

```
GET https://hq.sinajs.cn/list={股票代码}
```

### 请求示例

```bash
curl -H "Referer: https://finance.sina.com.cn" \
     "https://hq.sinajs.cn/list=sh600519,sz000001"
```

**重要**：必须携带 `Referer: https://finance.sina.com.cn` 头，否则返回空数据。

### 代码格式

| 市场 | 前缀 | 示例 |
|------|------|------|
| 上交所 | sh | sh600519（贵州茅台） |
| 深交所 | sz | sz000001（平安银行） |
| 北交所 | bj | bj430047 |

### 返回格式

```javascript
var hq_str_sh600519="贵州茅台,1870.00,1869.99,1880.00,1885.00,1868.00,1869.99,1869.99,6900,13000000,1869.00,1868.99,1869.98,1869.99,1869.00,1869.99,1869.98,2025-04-26,15:00:00";
```

### 字段映射（逗号分隔，索引位置）

| 索引 | 字段 | 说明 |
|------|------|------|
| 0 | name | 证券名称 |
| 1 | open | 今开 |
| 2 | yestClose | 昨收 |
| 3 | price | 当前价 |
| 4 | high | 最高 |
| 5 | low | 最低 |
| 8 | volume | 成交量（股） |
| 9 | amount | 成交额（元） |
| 30 | date | 日期 |
| 31 | time | 时间 |

### 编码处理

响应为 GB18030 编码，需通过 OkHttp 拦截器解码：

```kotlin
// GBKResponseInterceptor.kt
class GBKResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body()?.bytes()
        val gbkString = body?.let { String(it, Charset.forName("GB18030")) }
        // ...
    }
}
```

### 项目实现文件

- Retrofit 接口：`data/remote/api/SinaStockApi.kt`
- 解析逻辑：`data/repository/StockRepositoryImpl.kt`
- 拦截器：`data/remote/interceptor/SinaRefererInterceptor.kt`

---

## 二、腾讯财经 - 港股行情

### 接口地址

```
GET https://qt.gtimg.cn/q={股票代码}
```

### 请求示例

```bash
curl "https://qt.gtimg.cn/q=r_hk00700"
```

### 代码格式

港股代码需加 `r_` 前缀，市场标识 `hk`：
- `r_hk00700` → 腾讯控股
- `r_hk00941` → 中国移动

### 返回格式

```text
v_r_hk00700="1~腾讯控股~00700~380.00~378.00~382.00~...~380.00~...";
```

### 字段映射（`~` 分隔，索引位置）

| 索引 | 字段 | 说明 |
|------|------|------|
| 1 | name | 证券名称 |
| 3 | price | 当前价 |
| 4 | yestClose | 昨收 |
| 5 | open | 今开 |
| 33 | high | 最高 |
| 34 | low | 最低 |
| 36 | volume | 成交量 |
| 37 | amount | 成交额 |

### 编码处理

响应为 GBK 编码，同样通过 `GBKResponseInterceptor` 处理。

### 项目实现文件

- Retrofit 接口：`data/remote/api/TencentHKStockApi.kt`
- 解析逻辑：`data/repository/StockRepositoryImpl.kt`

---

## 三、天天基金 - 基金估值

### 接口地址

```
GET https://fundgz.1234567.com.cn/js/{基金代码}.js
```

### 请求示例

```bash
curl "https://fundgz.1234567.com.cn/js/110011.js"
```

### 返回格式（JSONP）

```javascript
jsonpgz({
  "fundcode": "110011",
  "name": "易方达中小盘",
  "jzrq": "2025-04-25",
  "dwjz": "5.1234",
  "gsz": "5.1500",
  "gszzl": "0.52",
  "gztime": "2025-04-26 14:30"
});
```

### 字段说明

| 字段 | 说明 |
|------|------|
| fundcode | 基金代码 |
| name | 基金名称 |
| jzrq | 净值日期（上一交易日） |
| dwjz | 单位净值（上一日） |
| gsz | 估算净值（实时） |
| gszzl | 估算涨跌幅（%） |
| gztime | 估算时间 |

### 解析方式

JSONP 格式，需正则提取 JSON 内容：

```kotlin
// 正则匹配
val jsonRegex = Regex("jsonpgz\\((.*)\\);")
val json = jsonRegex.find(response)?.groupValues?.get(1)
// 然后用 Gson 解析
```

### 项目实现文件

- Retrofit 接口：`data/remote/api/FundApi.kt`
- DTO：`data/remote/dto/FundQuoteDto.kt`
- Repository：`data/repository/FundRepositoryImpl.kt`

---

## 四、东方财富 - K 线数据

### 接口地址

```
GET https://push2his.eastmoney.com/api/qt/stock/kline/get
```

### 请求参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| secid | 证券 ID（市场.代码） | 1.600519 |
| klt | K 线周期 | 101（日K）、102（周K）、103（月K） |
| fqt | 复权类型 | 1（前复权） |
| lmt | 数据条数 | 100 |
| fields1 | 字段组 1 | f1,f2,f3,f4,f5,f6 |
| fields2 | 字段组 2 | f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63 |

### secid 格式

| 市场 | secid 前缀 | 示例 |
|------|------------|------|
| 上交所 | 1 | 1.600519 |
| 深交所 | 0 | 0.000001 |

### 返回格式（JSON）

```json
{
  "data": {
    "klines": [
      "2025-04-01,1870.00,1880.00,1865.00,1878.00,690000,1300000000",
      "2025-04-02,1878.00,1885.00,1870.00,1882.00,720000,1350000000"
    ]
  }
}
```

### klines 字段映射（逗号分隔）

| 索引 | 字段 | 说明 |
|------|------|------|
| 0 | date | 日期 |
| 1 | open | 开盘价 |
| 2 | close | 收盘价 |
| 3 | low | 最低价 |
| 4 | high | 最高价 |
| 5 | volume | 成交量 |
| 6 | amount | 成交额 |

### 项目实现文件

- Retrofit 接口：`data/remote/api/EastMoneyKLineApi.kt`
- DTO：`data/remote/dto/KLineDto.kt`
- UseCase：`domain/usecase/GetKLineDataUseCase.kt`

---

## 五、中国银行 - 港股汇率

### 接口地址

```
GET https://www.boc.cn/sourcedb/whpj/index.html
```

### 解析方式

HTML 页面，使用 Jsoup 解析表格：

```kotlin
// Jsoup 解析
val doc = Jsoup.parse(html)
val table = doc.select("table").first()
val rows = table.select("tr")
// 查找港币行，提取现汇买入价
```

### 关键字段

| 字段 | 说明 |
|------|------|
| 现汇买入价 | 银行买入港币的价格 |
| 现汇卖出价 | 银行卖出港币的价格 |

### 使用场景

港股持仓折算人民币时使用：`市值(CNY) = 港币市值 * HKD/CNY汇率`

### 项目实现文件

- Repository：`data/repository/ExchangeRateRepositoryImpl.kt`

---

## 六、节假日 API - 交易日判断

### 接口地址

```
GET https://timor.tech/api/holiday/info/{日期}
```

### 请求示例

```bash
curl "https://timor.tech/api/holiday/info/2025-05-01"
```

### 返回格式（JSON）

```json
{
  "date": "2025-05-01",
  "holiday": true,
  "name": "劳动节"
}
```

### 使用场景

判断是否为交易日：
- `holiday: true` → 非交易日，停止行情刷新
- `holiday: false` → 交易日（需结合周末判断）

### 项目实现文件

- Retrofit 接口：`data/remote/api/HolidayApi.kt`
- Repository：`data/repository/MarketCalendarRepositoryImpl.kt`

---

## 七、资讯数据源

### 7.1 韭研公社 - 个股资讯搜索

**接口地址**：
```
POST https://app.jiuyangongshe.com/jystock-app/api/v2/article/search
```

**请求参数**：
| 参数 | 说明 | 示例 |
|------|------|------|
| keyword | 股票名称 | 贵州茅台 |
| limit | 返回条数 | 10 |
| order | 排序 | 1 |
| type | 类型 | 1 |

**认证方式**：Token 从百度统计 ETag 获取，每次请求需携带 `timestamp` 和 `token` 头。

**项目实现**：`data/remote/api/JiuYanApi.kt`

### 7.2 证券时报（人民财讯）- A股快讯

**接口地址**：
```
GET https://www.stcn.com/article/list.html?type=kx
```

**解析方式**：Jsoup 网页抓取，返回 JSON 格式快讯列表，每条快讯需额外请求详情页获取完整内容。

**返回字段**：`id`, `title`, `content`, `source`, `time`（epoch毫秒）, `isRed`（利好标记）

**项目实现**：`data/repository/NewsRepositoryImpl.kt`（`fetchStcnNews`）

### 7.3 选股宝 - A股快讯

**接口地址**：
```
GET https://baoer-api.xuangubao.com.cn/api/v6/message/newsflash
```

**请求参数**：
| 参数 | 说明 | 示例 |
|------|------|------|
| limit | 返回条数 | 20 |
| subj_ids | 主题分类ID | 9,10,723,35,469 |
| platform | 平台 | pcweb |

**返回格式**：
```json
{
  "code": 20000,
  "data": {
    "messages": [
      { "id": 1, "title": "...", "summary": "...", "impact": 1, "created_at": 1715000000 }
    ]
  }
}
```

**impact 取值**：1=利好，-1=利空，0=中性

**项目实现**：`data/remote/api/XuanGuBaoNewsApi.kt`

### 7.4 财联社 - A股实时快讯

**接口地址**：
```
GET https://www.cls.cn/nodeapi/telegraphList
```

**请求参数**：
| 参数 | 说明 | 示例 |
|------|------|------|
| app | 应用标识 | CailianpressWeb |
| os | 平台 | web |
| sv | 版本 | 7.7.5 |
| rn | 返回条数 | 20 |
| sign | 签名 | 计算得出 |

**签名算法**：参数按 key 排序 → URL 编码 → SHA-1 → MD5，实现见 `data/remote/util/ClsSignHelper.kt`

**返回格式**：
```json
{
  "data": {
    "roll_data": [
      { "id": 1, "ctime": 1715000000, "title": "...", "brief": "...", "content": "...", "is_ad": false }
    ]
  }
}
```

**项目实现**：`data/remote/api/ClsNewsApi.kt`

### 7.5 华尔街见闻 - 国际财经

**接口地址**：
```
GET https://api-one.wallstcn.com/apiv1/content/lives
```

**请求参数**：
| 参数 | 说明 | 可选值 |
|------|------|--------|
| channel | 频道 | global-channel, a-stock-channel, us-stock-channel, forex-channel, commodity-channel |
| client | 客户端 | pc |
| limit | 返回条数 | 20 |
| cursor | 分页游标 | 0 |

**无需认证**。返回 `data.items` 数组，含 `content_text`, `content_short`, `display_time`, `resource_type` 等字段。

**项目实现**：`data/remote/api/WallstreetCnApi.kt`

### 7.6 金十数据 - 宏观/期货

**接口地址**：
```
GET https://flash-api.jin10.com/get_flash_list
```

**请求参数**：
| 参数 | 说明 | 示例 |
|------|------|------|
| channel | 频道 | -8200 |
| vip | VIP | 1 |

**请求头**：需携带 `x-app-id: bVBF4FyRTn5NJF5n` 和 `x-version: 1.0.0`

**返回格式**：
```json
{
  "data": [
    { "type": 0, "id": 1, "time": "2025-05-08 14:30:00", "data": { "content": "..." }, "important": false }
  ]
}
```

`type=0` 为快讯，`type=1` 为经济数据。广告内容通过正则过滤。

**项目实现**：`data/remote/api/Jin10Api.kt`

### 7.7 东方财富 - 综合快讯

**接口地址**：
```
GET https://newsapi.eastmoney.com/kuaixun
```

**请求参数**：
| 参数 | 说明 | 可选值 |
|------|------|--------|
| type | 快讯分类 | 101=焦点, 102=7x24全球, 103=上市公司, 105=全球股市, 106=商品, 107=外汇 |
| pagesize | 返回条数 | 20 |
| pageindex | 页码 | 1 |

**无需认证**。不传 `callback` 参数返回纯 JSON；传入则返回 JSONP。

**返回字段**：`id`, `title`, `digest`, `content`, `showtime`, `source`, `url`

**项目实现**：`data/remote/api/EastMoneyNewsApi.kt`

### 7.8 新闻聚合策略

`NewsRepositoryImpl` 通过 `coroutineScope + async` 并行抓取 6 个市场新闻源，每个源独立 try-catch 容错，合并后按时间降序排列。支持通过 `NewsTopic` 枚举按主题筛选相关源：

| NewsTopic | 对应源 |
|-----------|--------|
| A_STOCK | 财联社、选股宝、东方财富、证券时报 |
| GLOBAL | 华尔街见闻、金十 |
| FUTURES | 金十、东方财富 |
| US_STOCK | 华尔街见闻 |
| FOREX | 华尔街见闻、金十 |

---

## 八、错误处理

### 常见错误码

| 场景 | 处理方式 |
|------|----------|
| 网络超时 | 显示缓存数据，提示刷新 |
| 接口返回空 | 检查 Referer 头（新浪） |
| 编码异常 | 使用 GBK 拦截器解码 |
| 数据格式异常 | 正则匹配失败时返回 null |

### 缓存策略

所有行情数据优先缓存到 Room：
- `StockCacheEntity`：股票行情
- `FundCacheEntity`：基金估值

断网时优先显示缓存数据，界面不阻塞。

---

## 九、参考文件索引

| leek-fund 文件 | 参考内容 |
|----------------|----------|
| `src/explorer/stockService.ts` | 新浪 A 股解析 |
| `src/shared/tencentStock.ts` | 腾讯港股字段映射 |
| `src/explorer/fundService.ts` | 基金 JSONP 解析 |
| `src/shared/utils.ts` | 盈亏计算、时段检测 |
| `src/webview/stockTrend.ts` | K 线图展示 |

---

## 十、更新历史

| 日期 | 更新内容 |
|------|----------|
| 2025-05-08 | 新增5个新闻源文档（选股宝/财联社/华尔街见闻/金十/东方财富），补充聚合策略说明 |
| 2025-04-26 | 初始版本，完整记录各数据源接口 |