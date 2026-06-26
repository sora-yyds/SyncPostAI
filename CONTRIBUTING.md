# 贡献指南

感谢你愿意参与智稿同步（SyncPostAI）的改进。

## 参与方式

你可以通过以下方式参与：

- 提交问题反馈。
- 提出功能建议。
- 改进文档。
- 修复缺陷。
- 提交新功能。

## 开发环境

建议环境：

- JDK 21
- Node.js 20 或以上
- pnpm 9
- Docker，可选，用于本地运行 Halo 调试

安装前端依赖：

```powershell
.\gradlew.bat pnpmInstall
```

构建插件：

```powershell
.\gradlew.bat build
```

仅编译 Java：

```powershell
.\gradlew.bat compileJava
```

本地启动 Halo 调试环境：

```powershell
.\gradlew.bat haloServer
```

## 项目结构

常用目录：

```text
src/main/java/run/halo/starter/
├── endpoint/   # 外部 API
├── service/    # 发布文章等业务逻辑
├── model/      # 请求和响应模型
├── config/     # 插件设置模型
└── security/   # Token 校验

src/main/resources/
├── plugin.yaml
├── syncpostai-logo.png
└── extensions/
    ├── settings.yaml
    └── role-template-anonymous.yaml

ui/src/
├── index.ts
└── views/
    └── HomeView.vue
```

## 提交前检查

提交代码前请至少执行：

```powershell
.\gradlew.bat build
```

如果只改 Java 后端，可以先执行：

```powershell
.\gradlew.bat compileJava
```

## 代码约定

- 后端业务逻辑优先放在 `service` 包。
- 外部 API 入口放在 `endpoint` 包。
- 不要把业务逻辑写进 `SyncPostAiPlugin`。
- 插件设置字段变更时，同步更新 `settings.yaml`、README 和控制台示例。
- 对外接口字段变更时，同步更新 README。
- 不要提交本地构建产物、依赖目录和私人配置。

## 文档约定

- 面向普通用户的说明放在 `README.md`。
- 版本变更记录放在 `CHANGELOG.md`。
- 开发和贡献说明放在 `CONTRIBUTING.md`。
- 所有公开文档默认使用简体中文。

## 提交信息规范

本项目采用轻量级 Git commit 规范，不强制安装 Commitizen，也不配置提交模板。提交时请自觉遵守以下格式：

```text
type: description

- 详细说明 1
- 详细说明 2
```

### type 类型

`type` 表示本次提交的类别，只允许使用以下几种：

| type | 说明 |
| --- | --- |
| `fix` | 修复 bug。 |
| `add` | 新功能。 |
| `update` | 更新已有功能、配置或文档内容。 |
| `style` | 代码格式改变，不影响运行逻辑。 |
| `test` | 增加或调整测试代码。 |
| `revert` | 撤销上一次或某一次提交。 |
| `build` | 构建工具或构建过程变动。 |

### description 描述

`description` 是本次提交的简短描述，作为提交信息第一行：

- 尽量不超过 50 个字符。
- 使用清晰、直接的中文。
- 描述本次提交做了什么，而不是描述过程。

### 详细描述

如果本次提交包含多处改动，建议在第一行之后空一行，并使用 Markdown 分点说明：

- 每一项使用 `-` 开头。
- 说明具体改动内容、影响范围或兼容性变化。
- 不要写入 Token、账号、服务器地址等敏感信息。
- 如果提交已验证，可以补充构建或测试结果。

### 示例

```text
add: 支持随机封面图集

- 新增封面图集配置项
- 当文章没有传入封面时，随机选择一张图片作为封面
- 已通过 .\gradlew.bat build 验证
```

```text
fix: 修复 Markdown Front Matter 分类解析

- 兼容 YAML 列表形式的 categories 和 tags
- 修复分类为空时使用默认分类的逻辑
```

```text
update: 更新 README 调用示例

- 补充 PowerShell、Node.js、Python 调用示例
- 将示例接口路径更新为当前公开推送接口
- 增加 Windows PowerShell 下 UTF-8 JSON 文件发送说明
```

简短提交也可以只写一行：

```text
style: 格式化控制台页面样式
test: 增加文章推送服务测试
build: 升级 Halo 插件构建配置
revert: 撤销随机封面默认启用
```

不推荐：

```text
改了一点东西
fix
update code
临时提交
```

## 安全说明

请不要在 Issue、Pull Request 或提交记录中公开：

- 推送 Token
- Halo 后台账号密码
- 服务器私钥
- 私有 API 地址
- 其他敏感配置

如果发现安全问题，请联系项目维护者。
