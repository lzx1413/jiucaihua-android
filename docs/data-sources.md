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
GET https://app.jiuyangongshe.com/jystock-app/api/v2/article/search
```

**请求参数**：
| 参数 | 说明 | 示例 |
|------|------|------|
| keyword | 股票名称 | 贵州茅台 |
| pageSize | 返回条数 | 10 |

**返回格式**：
```json
{
  "data": [
    {
      "title": "茅台一季度业绩分析",
      "content": "...",
      "create_time": "2025-04-26"
    }
  ]
}
```

**项目实现**：`data/remote/api/JiuYanApi.kt`

### 7.2 市场资讯

项目使用内部接口获取市场快讯，具体实现见：
- `data/repository/NewsRepositoryImpl.kt`

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
| 2025-04-26 | 初始版本，完整记录各数据源接口 |