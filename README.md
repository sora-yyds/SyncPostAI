# SyncPostAI

同步文章 AI 发布是一个 Halo 2.x 插件，用于接收外部 AI、自动化脚本或其他系统推送的文章内容，并通过安全 Token 校验后自动发布到 Halo。

典型场景：

- AI 写作工具生成文章后，直接推送到 Halo。
- 第三方内容系统将文章同步到 Halo。
- 本地 Markdown 文件通过脚本发布到 Halo。
- 其他 Halo 插件或外部服务调用本插件接口创建文章。

## 功能

- 提供外部文章推送 API。
- 支持 Token 鉴权，请求头为 `X-SyncPost-Token`。
- 支持 `text`、`html`、`markdown` 三种内容类型。
- Markdown 会自动转换为 HTML 后发布。
- 支持读取 Markdown 顶部的 Front Matter。
- `title` 可选；优先读取请求字段，其次读取 Front Matter，最后读取第一个一级标题 `# 标题`。
- 标题有兜底策略；即使没有传标题、Front Matter 和一级标题，也会生成安全标题，避免发布流程崩溃。
- `excerpt` 可选；优先读取请求字段，其次读取 Front Matter，不传时使用 Halo 自动摘要。
- 支持文章封面 `cover`；当封面为空且启用随机封面图集时，会从图集中随机选择一张。
- 自动创建请求中传入的分类和标签。
- 支持默认作者、默认分类、默认标签、默认发布状态等插件设置。
- 发布成功后响应中返回文章完整访问地址 `articleUrl`，方便请求方继续处理。

## 安装与配置

1. 执行构建：

```powershell
.\gradlew.bat build
```

2. 将生成的插件 jar 安装到 Halo：

```text
build/libs/
```

3. 在 Halo 控制台启用插件。

4. 进入插件设置，配置：

- 启用外部推送
- 推送 Token
- 默认作者
- 默认分类
- 默认标签
- 默认直接发布
- 随机封面图集

`推送 Token` 是外部系统调用接口时必须携带的密钥，请不要公开。

## 接口地址

```http
POST /apis/api.starter.halo.run/v1alpha1/articles
```

完整示例：

```text
https://你的站点域名/apis/api.starter.halo.run/v1alpha1/articles
```

请求头：

```http
Content-Type: application/json; charset=utf-8
X-SyncPost-Token: 你的推送 Token
```

## 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `title` | string | 否 | 文章标题。Markdown 模式下不传时，会读取第一个 `# 一级标题`。 |
| `content` | string | 是 | 文章正文内容。 |
| `contentType` | string | 否 | 内容类型：`markdown`、`html`、`text`。默认是 `text`。 |
| `excerpt` | string | 否 | 自定义摘要。不传时使用 Halo 自动摘要。 |
| `author` | string | 否 | 文章作者用户名。优先级高于 Front Matter 中的 `author` / `auther`。 |
| `cover` | string | 否 | 文章封面图 URL。为空时可由随机封面图集兜底。 |
| `slug` | string | 否 | 文章别名。建议外部系统传入稳定且唯一的值。 |
| `tags` | string[] | 否 | 标签显示名称。不存在时插件会自动创建。 |
| `categories` | string[] | 否 | 分类显示名称。不存在时插件会自动创建。 |
| `publish` | boolean | 否 | 是否直接发布。不传时使用插件设置中的默认发布状态。 |

请求字段优先级高于 Markdown Front Matter。也就是说，如果请求体里传了 `title`，即使 Markdown 顶部也写了 `title`，最终仍然使用请求体里的 `title`。

## Markdown Front Matter

当 `contentType` 为 `markdown` 时，可以在 Markdown 文件顶部添加 Front Matter：

```markdown
---
title: 从企业记忆到世界模型：AI Agent 正进入“长期上下文”竞争
auther: admin
cover:
excerpt: 今日 AI 动态显示，产业竞争正从单点模型能力转向长期上下文、组织知识、世界模拟与数据质量。
categories:
  - AI推送
tags:
  - AI Agent
  - 大模型
  - 企业AI
  - 世界模型
  - AI评测
---

这里开始写正文。
```

支持字段：

| Front Matter 字段 | 说明 |
| --- | --- |
| `title` | 文章标题。 |
| `author` / `auther` | 文章作者用户名。`auther` 是兼容字段，建议后续使用 `author`。 |
| `cover` | 文章封面图 URL。为空时可使用随机封面图集兜底。 |
| `excerpt` | 文章摘要。 |
| `categories` | 文章分类列表。 |
| `tags` | 文章标签列表。 |

标题解析顺序：

1. 请求体里的 `title`
2. Front Matter 里的 `title`
3. Markdown 正文里的第一个 `# 一级标题`
4. 正文前 30 个字符
5. `未命名文章`

封面解析顺序：

1. 请求体里的 `cover`
2. Front Matter 里的 `cover`
3. 插件设置中的随机封面图集
4. 空封面

## Markdown 推送示例

`title` 可以省略，插件会读取 Markdown 中第一个一级标题：

```json
{
  "content": "---\ntitle: Markdown 自动发布测试文章\nauther: admin\ncover:\nexcerpt: 这是一段来自 Markdown Front Matter 的摘要。\ncategories:\n  - 默认分类\ntags:\n  - AI\n  - Markdown\n  - 测试\n---\n\n这是一篇由外部 AI 系统推送到 SyncPostAI 插件的测试文章。\n\n## 二级标题\n\n这里包含 **加粗文字** 和普通段落。",
  "contentType": "markdown",
  "slug": "markdown-auto-post",
  "publish": true
}
```

成功响应示例：

```json
{
  "success": true,
  "message": "Article published to Halo.",
  "articleName": "markdown-auto-post",
  "snapshotName": "markdown-auto-post-base-xxxx",
  "status": "published",
  "articleUrl": "https://你的站点域名/archives/markdown-auto-post"
}
```

请求方会收到这段 JSON 响应，可以直接读取 `articleUrl` 获取发布后的文章地址。

## PowerShell 调用示例

发送一篇 Markdown 文章：

```powershell
$siteUrl = "https://你的站点域名"
$token = "你的推送 Token"
$jsonPath = "$env:TEMP\syncpostai-test.json"

$json = @'
{
  "content": "# Markdown 自动发布测试文章\n\n这是一段 **加粗文字**。\n\n## 二级标题\n\n- 列表 1\n- 列表 2",
  "contentType": "markdown",
  "slug": "markdown-simple-test",
  "tags": ["AI", "Markdown", "测试"],
  "categories": ["默认分类"],
  "publish": true
}
'@

[System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "$siteUrl/apis/api.starter.halo.run/v1alpha1/articles" `
  -H "Content-Type: application/json; charset=utf-8" `
  -H "X-SyncPost-Token: $token" `
  --data-binary "@$jsonPath"
```

## 推送本地 Markdown 文件

Windows PowerShell 5.1 对中文和大段 Markdown 转 JSON 容易出问题，推荐先把 JSON 写入临时文件，再用 `curl.exe --data-binary` 发送。

```powershell
$siteUrl = "https://你的站点域名"
$token = "你的推送 Token"
$mdPath = "E:\Users\--sora--\Desktop\test.md"
$jsonPath = "$env:TEMP\syncpostai-markdown-file-test.json"

$content = Get-Content -Raw -Encoding UTF8 $mdPath

Add-Type -AssemblyName System.Web.Extensions
$serializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer
$serializer.MaxJsonLength = 2147483647
$escapedContent = $serializer.Serialize($content)

$json = @"
{
  "content": $escapedContent,
  "contentType": "markdown",
  "slug": "markdown-file-test-$(Get-Date -Format 'yyyyMMddHHmmss')",
  "tags": ["AI", "Markdown", "测试"],
  "categories": ["默认分类"],
  "publish": true
}
"@

[System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.UTF8Encoding]::new($false))

Get-Content -Raw -Encoding UTF8 $jsonPath | ConvertFrom-Json

curl.exe -X POST "$siteUrl/apis/api.starter.halo.run/v1alpha1/articles" `
  -H "Content-Type: application/json; charset=utf-8" `
  -H "X-SyncPost-Token: $token" `
  --data-binary "@$jsonPath"
```

说明：

- `.md` 文件不是以文件对象上传，而是读取文件内容后放入 JSON 的 `content` 字段。
- 如果 Markdown 文件内有 `# 一级标题`，并且请求中没有传 `title`，插件会使用这个一级标题作为文章标题。
- 如果没有传 `excerpt`，Halo 会自动生成摘要。

## 其他插件或 AI 系统如何调用

其他系统只需要向本插件接口发送一个 HTTP POST 请求。

调用步骤：

1. 在 Halo 插件设置中启用外部推送。
2. 设置一个足够安全的推送 Token。
3. 外部系统生成文章内容。
4. 将文章内容组装成 JSON。
5. 请求头中加入 `X-SyncPost-Token`。
6. POST 到 `/apis/api.starter.halo.run/v1alpha1/articles`。

### 通用 HTTP 请求

```http
POST https://你的站点域名/apis/api.starter.halo.run/v1alpha1/articles
Content-Type: application/json; charset=utf-8
X-SyncPost-Token: 你的推送 Token

{
  "content": "# AI 生成的文章标题\n\n这里是 AI 生成的正文。",
  "contentType": "markdown",
  "slug": "ai-generated-post-20260624",
  "tags": ["AI"],
  "categories": ["默认分类"],
  "publish": true
}
```

### JavaScript / Node.js 示例

```js
const siteUrl = 'https://你的站点域名'
const token = '你的推送 Token'

const response = await fetch(`${siteUrl}/apis/api.starter.halo.run/v1alpha1/articles`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
    'X-SyncPost-Token': token,
  },
  body: JSON.stringify({
    content: '# AI 生成的文章标题\n\n这里是 AI 生成的正文，支持 **Markdown**。',
    contentType: 'markdown',
    excerpt: '这是一段可选摘要。',
    slug: `ai-post-${Date.now()}`,
    tags: ['AI'],
    categories: ['默认分类'],
    publish: true,
  }),
})

const result = await response.json()
console.log(result)
```

### Python 示例

```python
import requests
import time

site_url = "https://你的站点域名"
token = "你的推送 Token"

payload = {
    "content": "# AI 生成的文章标题\n\n这里是 AI 生成的正文，支持 **Markdown**。",
    "contentType": "markdown",
    "excerpt": "这是一段可选摘要。",
    "slug": f"ai-post-{int(time.time())}",
    "tags": ["AI"],
    "categories": ["默认分类"],
    "publish": True,
}

response = requests.post(
    f"{site_url}/apis/api.starter.halo.run/v1alpha1/articles",
    headers={
        "Content-Type": "application/json; charset=utf-8",
        "X-SyncPost-Token": token,
    },
    json=payload,
    timeout=30,
)

print(response.status_code)
print(response.json())
```

## 常见问题

### 为什么返回登录页？

请确认接口路径是：

```text
/apis/api.starter.halo.run/v1alpha1/articles
```

不要使用旧路径：

```text
/apis/api.starter.halo.run/v1alpha1/ai/articles
```

### 为什么返回 `Failed to read HTTP message`？

通常是请求体不是合法 JSON，或者编码不正确。建议：

- 使用 `Content-Type: application/json; charset=utf-8`。
- Windows PowerShell 下先写入 UTF-8 JSON 文件，再用 `curl.exe --data-binary "@文件路径"` 发送。
- 发送前用 `ConvertFrom-Json` 检查 JSON 是否合法。

### 为什么提示文章已存在？

插件会使用 `slug` 生成文章资源名。相同 `slug` 重复推送时会返回已存在。测试时建议在 `slug` 后加时间戳。

### Markdown 是否支持标题、加粗和列表？

支持。请求中设置：

```json
{
  "contentType": "markdown"
}
```

插件会将 Markdown 转换为 HTML 后发布。

## 开发

环境要求：

- JDK 21
- Node 20
- pnpm 9
- Docker，可选，用于本地 Halo 调试

构建：

```powershell
.\gradlew.bat build
```

仅编译 Java：

```powershell
.\gradlew.bat compileJava
```

本地 Halo 调试：

```powershell
.\gradlew.bat haloServer
```

## 许可证

GPL-3.0
