# 更新日志

本文档记录智稿同步（SyncPostAI）的版本变更。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [未发布]

## [1.1.0] - 2026-06-26

### 新增

- 新增待审核队列，关闭默认直接发布或请求传入 `publish: false` 时，外部推送内容会先保存为待处理稿件。
- 新增 Halo 控制台审核页面，支持预览稿件、通过发布和拒绝稿件。
- 新增发布审计信息，记录推送来源、接收时间、审核状态、审核人、失败原因和发布结果。
- 新增 `source` 请求字段，用于标识 Astrbot、n8n、GitHub Actions 等推送来源。
- 新增 n8n、Dify、Coze、GitHub Actions 来源管理预设，支持独立启用开关、专属密钥和默认发布策略。
- 新增 Astrbot Pulse 来源预设，并移除全局推送 Token，改为按来源选择 Halo Secret。
- 新增审核记录筛选、单条删除和按保留天数清理已处理记录功能。
- 新增审核工作台后台角色模板和 UI 权限控制。

### 变更

- 默认作者设置改为从 Halo 用户列表中选择，避免填写不存在的用户。
- 来源 Token 改为通过 Halo Secret 资源保存，插件设置仅保存密钥引用。
- 插件安装后默认不自动启用，降低未配置前暴露公开推送接口的风险。
- 插件标识调整为合法的小写 `syncpostai`。

### 文档

- 更新 README，补充来源管理、审核队列、记录清理、Secret 配置和升级说明。
- 更新贡献指南，提交信息示例改为中文多行分点格式。

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
