# 更新日志

本文档记录智稿同步（SyncPostAI）的版本变更。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-06-25

### 首次发布

- 提供外部文章推送接口，支持 AI 系统、自动化脚本和第三方服务向 Halo 发布文章。
- 支持 `X-SyncPost-Token` 请求头鉴权。
- 支持 `markdown`、`html`、`text` 三种内容类型。
- 支持 Markdown Front Matter，字段包括 `title`、`author`、`auther`、`cover`、`excerpt`、`categories`、`tags`。
- 支持标题兜底策略，未传标题时可从 Front Matter、Markdown 一级标题或正文片段生成标题。
- 支持自动创建文章分类和标签。
- 支持配置默认作者、默认分类、默认标签、默认发布状态和随机封面图集。
- 支持发布成功后返回文章完整访问地址 `articleUrl`。
- 提供 Halo 控制台页面，展示接口路径、请求头和调用示例。
