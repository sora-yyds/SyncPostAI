# 智稿同步（SyncPostAI）

智稿同步（SyncPostAI）是一个 Halo 2.x 插件，用于接收外部 AI、自动化脚本或第三方系统推送的文章，并自动发布到 Halo。

适用场景：

- AI 写作工具生成文章后，直接发布到 Halo。
- 自动化脚本定时把 Markdown 文件发布到博客。
- 第三方内容系统把文章同步到 Halo。
- 其他 Halo 插件通过 HTTP 接口创建文章。

## 功能特性

- 通过公开接口推送文章。
- 使用 `X-SyncPost-Token` 请求头进行 Token 鉴权。
- 支持 `markdown`、`html`、`text` 三种内容类型。
- Markdown 自动转换为 HTML。
- 支持读取 Markdown 顶部的 Front Matter。
- 支持从 Front Matter 读取标题、作者、封面、摘要、分类和标签。
- 未传标题时自动兜底：Front Matter 标题、正文一级标题、正文片段、`未命名文章`。
- 支持随机封面图集。
- 发布成功后返回文章完整访问地址 `articleUrl`。

## 安装

1. 下载或构建插件 jar。
2. 进入 Halo 后台。
3. 打开「插件」页面。
4. 上传并启用智稿同步插件。
5. 进入插件设置，完成推送配置。

本地构建命令：

```powershell
.\gradlew.bat build
```

构建后的 jar 位于：

```text
build/libs/
```

## 插件设置

| 配置项 | 说明 |
| --- | --- |
| 启用外部推送 | 关闭后，所有外部推送请求都会被拒绝。 |
| 推送 Token | 外部系统调用接口时必须携带的密钥。 |
| 默认作者 | 请求和 Front Matter 都未指定作者时使用。 |
| 默认分类 | 请求和 Front Matter 都未指定分类时使用。 |
| 默认标签 | 请求和 Front Matter 都未指定标签时使用。 |
| 默认直接发布 | 请求未传 `publish` 时使用。 |
| 启用随机封面图集 | 当文章未指定封面时，从封面图集中随机选择封面。 |
| 封面图集 | 每一项填写一个可访问的图片 URL。 |

请妥善保管推送 Token，不要公开到网页、仓库或客户端代码中。

## 数据与隐私

智稿同步会根据你的配置处理以下数据：

- 接收外部请求中的文章标题、正文、摘要、作者、封面、分类、标签和发布状态。
- 将文章、分类和标签写入当前 Halo 站点。
- 在插件设置中保存推送 Token、默认作者、默认分类、默认标签和封面图集配置。
- 发布成功后向请求方返回文章资源名、快照名、发布状态和文章访问地址。

插件不会主动向第三方服务发送站点内容、用户数据、访问日志或推送 Token，也不包含遥测、统计、广告或远程配置功能。封面图集中的图片 URL 由站点管理员自行配置，文章页面访问这些图片时可能会由浏览器请求对应的图片服务。

## 接口地址

```http
POST /apis/api.syncpostai.sora.run/v1alpha1/articles
```

完整地址示例：

```text
https://你的站点域名/apis/api.syncpostai.sora.run/v1alpha1/articles
```

请求头：

```http
Content-Type: application/json; charset=utf-8
X-SyncPost-Token: 你的推送 Token
```

## 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | string | 是 | 文章正文。 |
| `contentType` | string | 否 | `markdown`、`html` 或 `text`，默认 `text`。 |
| `title` | string | 否 | 文章标题。优先级高于 Front Matter。 |
| `author` | string | 否 | Halo 用户名。优先级高于 Front Matter 中的 `author` / `auther`。 |
| `cover` | string | 否 | 文章封面图 URL。 |
| `excerpt` | string | 否 | 文章摘要。不传时使用 Halo 自动摘要。 |
| `slug` | string | 否 | 文章别名。建议传入稳定且唯一的值。 |
| `categories` | string[] | 否 | 分类名称列表。不存在时插件会自动创建。 |
| `tags` | string[] | 否 | 标签名称列表。不存在时插件会自动创建。 |
| `publish` | boolean | 否 | 是否直接发布。不传时使用插件设置。 |

请求体字段优先级高于 Markdown Front Matter。

## Markdown Front Matter

当 `contentType` 为 `markdown` 时，可以在 Markdown 文件顶部添加 Front Matter：

```markdown
---
title: 从企业记忆到世界模型：AI Agent 正进入“长期上下文”竞争
author: admin
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

兼容字段：

- `author`：推荐使用。
- `auther`：兼容旧写法。

标题解析顺序：

1. 请求体 `title`
2. Front Matter `title`
3. Markdown 正文第一个 `# 一级标题`
4. 正文前 30 个字符
5. `未命名文章`

封面解析顺序：

1. 请求体 `cover`
2. Front Matter `cover`
3. 插件设置中的随机封面图集
4. 空封面

## 调用示例

<details open>
<summary>REST JSON 示例</summary>

```json
{
  "content": "---\ntitle: AI 生成文章示例\nauthor: admin\ncover:\nexcerpt: 这是一段摘要。\ncategories:\n  - AI推送\ntags:\n  - AI Agent\n  - 大模型\n---\n\n这是一篇由外部 AI 系统推送到 Halo 的 Markdown 文章。\n\n## 二级标题\n\n这里包含 **加粗文字** 和普通段落。",
  "contentType": "markdown",
  "slug": "ai-generated-post",
  "publish": true
}
```

成功响应：

```json
{
  "success": true,
  "message": "Article published to Halo.",
  "articleName": "ai-generated-post",
  "snapshotName": "ai-generated-post-base-xxxx",
  "status": "published",
  "articleUrl": "https://你的站点域名/archives/ai-generated-post"
}
```

</details>

<details>
<summary>PowerShell 示例</summary>

```powershell
$siteUrl = "https://你的站点域名"
$token = "你的推送 Token"
$jsonPath = "$env:TEMP\syncpostai-test.json"

$json = @'
{
  "content": "# AI 生成文章示例\n\n这是一段 **Markdown** 正文。",
  "contentType": "markdown",
  "slug": "ai-generated-post",
  "tags": ["AI", "Markdown"],
  "categories": ["AI推送"],
  "publish": true
}
'@

[System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "$siteUrl/apis/api.syncpostai.sora.run/v1alpha1/articles" `
  -H "Content-Type: application/json; charset=utf-8" `
  -H "X-SyncPost-Token: $token" `
  --data-binary "@$jsonPath"
```

</details>

<details>
<summary>推送本地 Markdown 文件</summary>

```powershell
$siteUrl = "https://你的站点域名"
$token = "你的推送 Token"
$mdPath = "D:\Articles\demo.md"
$jsonPath = "$env:TEMP\syncpostai-markdown-file.json"

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
  "publish": true
}
"@

[System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.UTF8Encoding]::new($false))

curl.exe -X POST "$siteUrl/apis/api.syncpostai.sora.run/v1alpha1/articles" `
  -H "Content-Type: application/json; charset=utf-8" `
  -H "X-SyncPost-Token: $token" `
  --data-binary "@$jsonPath"
```

插件当前不是上传 `.md` 文件对象，而是读取 Markdown 文件内容后放入 JSON 的 `content` 字段。

</details>

<details>
<summary>Node.js 示例</summary>

```js
const siteUrl = 'https://你的站点域名'
const token = '你的推送 Token'

const response = await fetch(`${siteUrl}/apis/api.syncpostai.sora.run/v1alpha1/articles`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
    'X-SyncPost-Token': token,
  },
  body: JSON.stringify({
    content: '# AI 生成文章示例\n\n这里是正文，支持 **Markdown**。',
    contentType: 'markdown',
    slug: `ai-post-${Date.now()}`,
    publish: true,
  }),
})

const result = await response.json()
console.log(result.articleUrl)
```

</details>

<details>
<summary>Python 示例</summary>

```python
import time
import requests

site_url = "https://你的站点域名"
token = "你的推送 Token"

payload = {
    "content": "# AI 生成文章示例\n\n这里是正文，支持 **Markdown**。",
    "contentType": "markdown",
    "slug": f"ai-post-{int(time.time())}",
    "publish": True,
}

response = requests.post(
    f"{site_url}/apis/api.syncpostai.sora.run/v1alpha1/articles",
    headers={
        "Content-Type": "application/json; charset=utf-8",
        "X-SyncPost-Token": token,
    },
    json=payload,
    timeout=30,
)

print(response.json())
```

</details>

## 常见问题

### 为什么返回登录页？

请确认接口路径是：

```text
/apis/api.syncpostai.sora.run/v1alpha1/articles
```

不要使用旧路径：

```text
/apis/api.syncpostai.sora.run/v1alpha1/ai/articles
/apis/api.starter.halo.run/v1alpha1/articles
```

### 为什么返回 `Failed to read HTTP message`？

通常是请求体不是合法 JSON，或者编码不正确。建议：

- 使用 `Content-Type: application/json; charset=utf-8`。
- Windows PowerShell 下先写入 UTF-8 JSON 文件，再用 `curl.exe --data-binary "@文件路径"` 发送。
- 发送前用 `ConvertFrom-Json` 检查 JSON 是否合法。

### 为什么提示文章已存在？

插件会使用 `slug` 生成文章资源名。相同 `slug` 重复推送会返回已存在。测试时建议在 `slug` 后加时间戳。

### 为什么没有封面？

请按顺序检查：

1. 请求体是否传了 `cover`。
2. Markdown Front Matter 是否填写了 `cover`。
3. 插件设置中是否启用了随机封面图集。
4. 封面图集里的 URL 是否可以公网访问。

## 相关链接

- 仓库地址：<https://github.com/sora-yyds/SyncPostAI/>
- 问题反馈：<https://github.com/sora-yyds/SyncPostAI/issues>

## 许可证

本项目使用 GPL-3.0 开源协议。
