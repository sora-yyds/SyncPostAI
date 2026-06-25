<script setup lang="ts">
import { computed, ref } from 'vue'
import RiArticleLine from '~icons/ri/article-line'
import RiCheckboxCircleLine from '~icons/ri/checkbox-circle-line'
import RiFileCopyLine from '~icons/ri/file-copy-line'
import RiShieldKeyholeLine from '~icons/ri/shield-keyhole-line'
import RiUploadCloud2Line from '~icons/ri/upload-cloud-2-line'

const endpointPath = '/apis/api.syncpostai.sora.run/v1alpha1/articles'
const tokenHeader = 'X-SyncPost-Token'

const samplePayload = {
  content:
    '---\ntitle: AI 生成文章示例\nauthor: admin\ncover:\nexcerpt: 这是一段来自 Markdown Front Matter 的摘要。\ncategories:\n  - AI推送\ntags:\n  - AI Agent\n  - 大模型\n---\n\n这是一篇由外部 AI 系统推送到 Halo 的 Markdown 文章。\n\n## 二级标题\n\n这里包含 **加粗文字**、列表和普通段落。',
  contentType: 'markdown',
  slug: 'ai-generated-post',
  publish: true,
}

const payloadText = JSON.stringify(samplePayload, null, 2)

const requestText = computed(
  () => `$body = @'
${payloadText}
'@

Invoke-RestMethod \`
  -Method POST \`
  -Uri "\${siteUrl}${endpointPath}" \`
  -Headers @{ "${tokenHeader}" = "<推送 Token>" } \`
  -ContentType "application/json; charset=utf-8" \`
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))`,
)

const copiedKey = ref<string>()

async function copyText(key: string, value: string) {
  await navigator.clipboard.writeText(value)
  copiedKey.value = key
  window.setTimeout(() => {
    if (copiedKey.value === key) {
      copiedKey.value = undefined
    }
  }, 1500)
}
</script>

<template>
  <main class="syncpost">
    <header class="syncpost__header">
      <div>
        <p class="syncpost__eyebrow">外部文章推送</p>
        <h1>智稿同步（SyncPostAI）</h1>
      </div>
      <div class="syncpost__status">
        <RiCheckboxCircleLine />
        已就绪
      </div>
    </header>

    <section class="syncpost__grid">
      <article class="panel panel--primary">
        <div class="panel__title">
          <RiUploadCloud2Line />
          推送接口
        </div>
        <div class="field">
          <span>请求方法</span>
          <code>POST</code>
        </div>
        <div class="field">
          <span>接口路径</span>
          <code>{{ endpointPath }}</code>
          <button type="button" class="icon-button" title="复制路径" @click="copyText('path', endpointPath)">
            <RiFileCopyLine />
          </button>
        </div>
        <div class="field">
          <span>请求头</span>
          <code>{{ tokenHeader }}: &lt;推送 Token&gt;</code>
          <button
            type="button"
            class="icon-button"
            title="复制请求头名称"
            @click="copyText('header', tokenHeader)"
          >
            <RiFileCopyLine />
          </button>
        </div>
      </article>

      <article class="panel">
        <div class="panel__title">
          <RiShieldKeyholeLine />
          使用前检查
        </div>
        <ul class="checklist">
          <li>已在插件设置中启用外部推送</li>
          <li>已填写推送 Token</li>
          <li>默认作者是已存在的 Halo 用户名</li>
          <li>Markdown Front Matter 可填写标题、作者、封面、摘要、分类和标签</li>
          <li>未传标题时，插件会自动读取 Front Matter 标题或正文一级标题</li>
          <li>启用随机封面图集后，空封面会自动从图集中选择</li>
        </ul>
      </article>
    </section>

    <section class="syncpost__samples">
      <article class="panel">
        <div class="panel__title">
          <RiArticleLine />
          JSON 请求示例
        </div>
        <pre>{{ payloadText }}</pre>
        <button type="button" class="copy-button" @click="copyText('payload', payloadText)">
          <RiFileCopyLine />
          {{ copiedKey === 'payload' ? '已复制' : '复制 JSON' }}
        </button>
      </article>

      <article class="panel">
        <div class="panel__title">
          <RiArticleLine />
          PowerShell 请求示例
        </div>
        <pre>{{ requestText }}</pre>
        <button type="button" class="copy-button" @click="copyText('request', requestText)">
          <RiFileCopyLine />
          {{ copiedKey === 'request' ? '已复制' : '复制请求' }}
        </button>
      </article>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.syncpost {
  min-height: 100%;
  background: #f6f8fb;
  color: #172033;
  padding: 24px;
}

.syncpost__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  max-width: 1160px;
  margin: 0 auto 20px;

  h1 {
    margin: 2px 0 0;
    font-size: 26px;
    line-height: 1.2;
    font-weight: 700;
  }
}

.syncpost__eyebrow {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.4;
}

.syncpost__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 600;

  svg {
    width: 18px;
    height: 18px;
  }
}

.syncpost__grid,
.syncpost__samples {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  max-width: 1160px;
  margin: 0 auto 16px;
}

.syncpost__samples {
  align-items: start;
}

.panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.panel--primary {
  border-color: #bfdbfe;
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 700;

  svg {
    width: 18px;
    height: 18px;
    color: #2563eb;
  }
}

.field {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  border-top: 1px solid #f1f5f9;
  font-size: 13px;

  &:first-of-type {
    border-top: 0;
  }

  span {
    color: #64748b;
  }

  code {
    overflow-wrap: anywhere;
    color: #0f172a;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
  }
}

.icon-button,
.copy-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    background: #f8fafc;
  }
}

.icon-button {
  width: 32px;
  height: 32px;

  svg {
    width: 16px;
    height: 16px;
  }
}

.copy-button {
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  margin-top: 12px;
  font-size: 13px;

  svg {
    width: 16px;
    height: 16px;
  }
}

.checklist {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
  color: #334155;
  font-size: 13px;

  li {
    position: relative;
    padding-left: 18px;

    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 7px;
      width: 6px;
      height: 6px;
      border-radius: 999px;
      background: #2563eb;
    }
  }
}

pre {
  overflow: auto;
  max-height: 360px;
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 6px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 860px) {
  .syncpost {
    padding: 16px;
  }

  .syncpost__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .syncpost__grid,
  .syncpost__samples {
    grid-template-columns: 1fr;
  }

  .field {
    grid-template-columns: 1fr 34px;

    span {
      grid-column: 1 / -1;
    }
  }
}
</style>
