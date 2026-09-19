# CDN 接入说明

业务读取链接默认走存储后端原生 URL。开启 `storage.cdn.enabled` 后，由 `ResourceService` 统一签发腾讯云 EdgeOne URL 鉴权 **Type D** 路径令牌，避免把 COS 预签名 GET 丢进 CDN。

实现：`CdnUrlSigner`。官方格式：[鉴权方法 D](https://edgeone.ai/zh/document/62478)。

## 为什么不能直接用 COS 预签名

COS GET 预签名每次带不同的 `q-sign-*` 参数，CDN 无法复用缓存。把主机名换成 EdgeOne 域名也不行：预签名绑定 COS 主机和签名串，节点验签会失败。

正确拆分：

| 场景 | 路径 |
|------|------|
| 上传 | 继续走 COS/OSS PUT 预签名，不经过 CDN |
| 读取（CDN 关闭） | `StorageService.getResourceReadUrl`：COS/OSS GET 预签名，LOCAL 为 `/common/file/local` HMAC |
| 读取（CDN 开启） | `CdnUrlSigner` 签发 EdgeOne Type D URL；COS 源站用私有桶回源鉴权 |

## 配置

```yaml
storage:
  cdn:
    enabled: false
    url-prefix: ""
    auth-key: ""
```

| 项 | 含义 |
|---|---|
| `enabled` | `false`：原生读取链接。`true`：签发 CDN 路径令牌 |
| `url-prefix` | EdgeOne 对外访问前缀，必须是无查询参数的绝对 URL，不要尾斜杠。可带路径前缀，例如 `https://static.example.com/private` |
| `auth-key` | 与 EdgeOne 控制台 Type D 鉴权密钥一致 |

在 `application.yml` / `application-prod.yml` 中直接填写上述项。

开启 CDN 但未配置 `url-prefix` / `auth-key` 时，`CdnUrlSigner` 抛出 `IllegalStateException`，不会回退到 COS 预签名。

## EdgeOne 控制台

1. 源站选 COS 私有桶，打开源站鉴权，让节点用服务角色回源，客户端不能直连 COS。
2. URL 鉴权选 **方法 D**，密钥与 `storage.cdn.auth-key` 相同，有效期与 `storage.presigned-url-ttl-seconds` 对齐（默认 300 秒）。
3. 缓存键忽略 `token`、`t`，否则每个签名都是新对象，CDN 没有命中。
4. 不要把 COS 预签名 URL 的查询参数带进 CDN。
5. LOCAL 存储走 CDN 时，回源路径是应用的 `GET /common/file/local`，不是对象 key。

## 签名算法（Type D）

签发 URL：

```text
https://{host}{requestPath}?{originQuery&}token={md5}&t={unixSeconds}
```

令牌：

```text
token = MD5( authKey + requestPath + timestamp )
```

| 字段 | 规则 |
|------|------|
| `authKey` | `storage.cdn.auth-key`，UTF-8 原文，不加分隔符 |
| `requestPath` | 实际请求路径，含前导 `/`，**不含**查询串和 fragment |
| `timestamp` | Unix 秒，十进制；查询参数名是 `t` |
| `MD5` | UTF-8 字节上计算，输出小写十六进制 |

查询参数不参与 `token`。可篡改的业务参数必须另签。

对应实现：

```java
String token = md5(authKey + requestPath + timestamp);
```

`requestPath` 由 CDN 域名前缀路径（去掉尾 `/`）与资源路径拼接。`sign(storageKey)` 会先把 key 做路径编码：

```text
requestPath = stripTrailingSlash(urlPrefix.path) + "/" + encodePath(storageKey)
```

`storageKey` 不能为空、不能以 `/` 开头、不能带 `?` / `#`。

### 计算示例

固定时刻 `t=1721029907`，密钥 `test-secret`。

对象 `avatar/1.png`：

```text
MD5("test-secret/avatar/1.png1721029907")
  = 81a97b30d25b4d66f2978240008a4430

https://static.example.com/avatar/1.png?token=81a97b30d25b4d66f2978240008a4430&t=1721029907
```

域名前缀含路径、key 含空格时，编码后的路径进入 MD5：

```text
url-prefix = https://static.example.com/private/
storageKey = avatar/a b.png
requestPath = /private/avatar/a%20b.png

MD5("test-secret/private/avatar/a%20b.png1721029907")
  = fbcf558f94425fa0904705b197bf42a6
```

LOCAL 回源只签路径 `/common/file/local`，`key` / `expires` / `signature` 不进入 MD5：

```text
MD5("test-secret/common/file/local1721029907")
  = 96f266ee47fcf9965117d2e095830f71

https://static.example.com/common/file/local
  ?key=avatar/1.png&expires=...&signature=...
  &token=96f266ee47fcf9965117d2e095830f71&t=1721029907
```

节点校验：请求未过期，再按同样公式算 MD5，与 URL 中 `token` 比较。

## 读取路由

`ResourceService.getCdnReadUrl`：

1. `storage.cdn.enabled=false`：调用对应 `StorageService.getResourceReadUrl`。
2. OSS/COS 且 CDN 开启：`cdnUrlSigner.sign(storageKey)`，CDN 回源对象 key。
3. LOCAL 且 CDN 开启：先用 `LocalFileUrlSigner` 生成 `key`、`expires`、`signature`，再 `cdnUrlSigner.signPath("/common/file/local", query)`。CDN 只保护回源路径，文件权限仍由本地 HMAC 约束。

上传始终走存储后端预签名，与 CDN 开关无关。

## 缓存

- 缓存键必须忽略 `token`、`t`。
- 对象 key 使用 UUID/内容版本，覆盖写时路径会变，避免旧缓存。
- 删除或覆盖后应主动刷新对应 EdgeOne 路径，否则 TTL 内节点可能继续返回旧对象。
