<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import RiArticleLine from '~icons/ri/article-line'
import RiCheckboxCircleLine from '~icons/ri/checkbox-circle-line'
import RiCloseCircleLine from '~icons/ri/close-circle-line'
import RiFileCopyLine from '~icons/ri/file-copy-line'
import RiRefreshLine from '~icons/ri/refresh-line'
import RiShieldKeyholeLine from '~icons/ri/shield-keyhole-line'
import RiUploadCloud2Line from '~icons/ri/upload-cloud-2-line'

type ReviewStatus = 'pending' | 'approved' | 'rejected' | 'failed'

interface ArticleReview {
  metadata: {
    name: string
    creationTimestamp?: string
  }
  spec: {
    title: string
    content: string
    contentType: string
    slug?: string
    source?: string
    status: ReviewStatus
    articleName?: string
    articleUrl?: string
    failureReason?: string
    rejectReason?: string
    receivedAt?: string
    reviewedAt?: string
    reviewedBy?: string
    attempts?: number
  }
}

const endpointPath = '/apis/api.syncpostai.sora.run/v1alpha1/articles'
const reviewEndpoint = '/apis/console.api.syncpostai.sora.run/v1alpha1/article-reviews'
const tokenHeader = 'X-SyncPost-Token'
const managedSources = [
  {
    key: 'astrbot_plugin_pulse',
    name: 'Astrbot Pulse',
    note: 'QQ 机器人 AI 资讯推送插件',
    url: 'https://github.com/sora-yyds/astrbot_plugin_pulse',
  },
  { key: 'n8n', name: 'n8n', note: 'HTTP Request 节点', url: 'https://n8n.io/' },
  { key: 'dify', name: 'Dify', note: 'HTTP Request 节点', url: 'https://dify.ai/' },
  { key: 'coze', name: 'Coze', note: 'HTTP Request 节点', url: 'https://www.coze.com/' },
  { key: 'github-actions', name: 'GitHub Actions', note: 'curl / shell step', url: 'https://github.com/features/actions' },
]

const samplePayload = {
  source: 'astrbot_plugin_pulse',
  content:
    '---\ntitle: AI 生成文章示例\nauthor: admin\ncover:\nexcerpt: 这是一段来自 Markdown Front Matter 的摘要。\ncategories:\n  - AI推送\ntags:\n  - AI Agent\n  - 大模型\n---\n\n这是一篇由外部 AI 系统推送到 Halo 的 Markdown 文章。\n\n## 二级标题\n\n这里包含 **加粗文字**、列表和普通段落。',
  contentType: 'markdown',
  slug: 'ai-generated-post',
  publish: false,
}

const payloadText = JSON.stringify(samplePayload, null, 2)

const examples = computed(() => [
  {
    key: 'powershell',
    label: 'PowerShell',
    code: `$body = @'
${payloadText}
'@

Invoke-RestMethod \`
  -Method POST \`
  -Uri "\${siteUrl}${endpointPath}" \`
  -Headers @{ "${tokenHeader}" = "<来源 Token>" } \`
  -ContentType "application/json; charset=utf-8" \`
  -Body ([System.Text.Encoding]::UTF8.GetBytes($body))`,
  },
  {
    key: 'javascript',
    label: 'JavaScript',
    code: `const siteUrl = 'https://你的站点域名'
const token = '你的来源 Token'

const response = await fetch(\`\${siteUrl}${endpointPath}\`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
    '${tokenHeader}': token,
  },
  body: JSON.stringify(${payloadText}),
})

console.log(await response.json())`,
  },
  {
    key: 'python',
    label: 'Python',
    code: `import requests

site_url = "https://你的站点域名"
token = "你的来源 Token"

payload = ${payloadText}

response = requests.post(
    f"{site_url}${endpointPath}",
    headers={
        "Content-Type": "application/json; charset=utf-8",
        "${tokenHeader}": token,
    },
    json=payload,
    timeout=30,
)

print(response.json())`,
  },
  {
    key: 'rest',
    label: 'REST',
    code: `POST ${endpointPath}
Content-Type: application/json; charset=utf-8
${tokenHeader}: <来源 Token>

${payloadText}`,
  },
])

const activeExample = ref('powershell')
const activeExampleCode = computed(() => examples.value.find((example) => example.key === activeExample.value)?.code || '')

const reviews = ref<ArticleReview[]>([])
const selectedName = ref<string>()
const copiedKey = ref<string>()
const loading = ref(false)
const operatingName = ref<string>()
const notice = ref('')
const error = ref('')
const activeStatus = ref<ReviewStatus | 'all'>('pending')
const retentionDays = ref(90)

const selectedReview = computed(() => reviews.value.find((review) => review.metadata.name === selectedName.value))

const pendingCount = computed(() => reviews.value.filter((review) => review.spec.status === 'pending').length)

const statusFilters: Array<{ label: string; value: ReviewStatus | 'all' }> = [
  { label: '待审核', value: 'pending' },
  { label: '已发布', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '发布失败', value: 'failed' },
  { label: '全部', value: 'all' },
]

async function requestJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...(options.headers || {}),
    },
    ...options,
  })
  if (!response.ok) {
    throw new Error(await response.text())
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json()
}

async function loadReviews() {
  loading.value = true
  error.value = ''
  try {
    const query = activeStatus.value === 'all' ? '' : `?status=${activeStatus.value}`
    reviews.value = await requestJson<ArticleReview[]>(`${reviewEndpoint}${query}`)
    if (selectedName.value && !reviews.value.some((review) => review.metadata.name === selectedName.value)) {
      selectedName.value = undefined
    }
    if (!selectedName.value && reviews.value.length > 0) {
      selectedName.value = reviews.value[0].metadata.name
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载待审核队列失败'
  } finally {
    loading.value = false
  }
}

async function changeStatusFilter(status: ReviewStatus | 'all') {
  activeStatus.value = status
  await loadReviews()
}

async function approve(review: ArticleReview) {
  operatingName.value = review.metadata.name
  error.value = ''
  notice.value = ''
  try {
    await requestJson(`${reviewEndpoint}/${review.metadata.name}/approve`, { method: 'POST' })
    notice.value = '已发布文章'
    await loadReviews()
    selectedName.value = review.metadata.name
  } catch (e) {
    error.value = e instanceof Error ? e.message : '发布失败'
  } finally {
    operatingName.value = undefined
  }
}

async function reject(review: ArticleReview) {
  const reason = window.prompt('请输入拒绝原因，可留空。') || ''
  operatingName.value = review.metadata.name
  error.value = ''
  notice.value = ''
  try {
    await requestJson(`${reviewEndpoint}/${review.metadata.name}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
    notice.value = '已拒绝稿件'
    await loadReviews()
    selectedName.value = review.metadata.name
  } catch (e) {
    error.value = e instanceof Error ? e.message : '拒绝失败'
  } finally {
    operatingName.value = undefined
  }
}

async function deleteReview(review: ArticleReview) {
  if (!window.confirm(`确认删除审核记录“${review.spec.title}”吗？此操作不会删除已经发布的 Halo 文章。`)) {
    return
  }
  operatingName.value = review.metadata.name
  error.value = ''
  notice.value = ''
  try {
    await requestJson<void>(`${reviewEndpoint}/${review.metadata.name}`, { method: 'DELETE' })
    notice.value = '已删除审核记录'
    selectedName.value = undefined
    await loadReviews()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除失败'
  } finally {
    operatingName.value = undefined
  }
}

async function cleanupReviews() {
  const days = Math.max(1, Number(retentionDays.value) || 90)
  if (!window.confirm(`确认清理 ${days} 天前的已发布、已拒绝、发布失败记录吗？待审核记录不会被清理。`)) {
    return
  }
  error.value = ''
  notice.value = ''
  try {
    const result = await requestJson<{ deletedCount: number }>(`${reviewEndpoint}/cleanup`, {
      method: 'POST',
      body: JSON.stringify({ retentionDays: days }),
    })
    notice.value = `已清理 ${result.deletedCount} 条审核记录`
    selectedName.value = undefined
    await loadReviews()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '清理失败'
  }
}

async function copyText(key: string, value: string) {
  await navigator.clipboard.writeText(value)
  copiedKey.value = key
  window.setTimeout(() => {
    if (copiedKey.value === key) {
      copiedKey.value = undefined
    }
  }, 1500)
}

function statusText(status: ReviewStatus) {
  return {
    pending: '待审核',
    approved: '已发布',
    rejected: '已拒绝',
    failed: '发布失败',
  }[status]
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(loadReviews)
</script>

<template>
  <main class="syncpost">
    <header class="syncpost__header">
      <div>
        <p class="syncpost__eyebrow">AI 内容接收、审核与发布</p>
        <h1>智稿同步</h1>
      </div>
      <div class="syncpost__status">
        <RiCheckboxCircleLine />
        {{ pendingCount }} 篇待审核
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
          <code>{{ tokenHeader }}: &lt;来源 Token&gt;</code>
          <button type="button" class="icon-button" title="复制请求头名称" @click="copyText('header', tokenHeader)">
            <RiFileCopyLine />
          </button>
        </div>
      </article>

      <article class="panel">
        <div class="panel__title">
          <RiShieldKeyholeLine />
          审核模式
        </div>
        <ul class="checklist">
          <li>请求传入 <code>publish: false</code> 时进入待审核队列</li>
          <li>插件设置关闭“默认直接发布”后，未传 publish 的请求也会进入队列</li>
          <li>管理员可在下方预览、发布或拒绝稿件</li>
          <li>审核记录会保存来源、状态、审核人、失败原因和发布时间</li>
        </ul>
      </article>
    </section>

    <section class="review-layout">
      <article class="panel review-list">
        <div class="panel__title panel__title--row">
          <span>
            <RiArticleLine />
            审核记录
          </span>
          <button type="button" class="copy-button" :disabled="loading" @click="loadReviews">
            <RiRefreshLine />
            刷新
          </button>
        </div>
        <div class="status-tabs">
          <button
            v-for="filter in statusFilters"
            :key="filter.value"
            type="button"
            class="status-tab"
            :class="{ 'status-tab--active': activeStatus === filter.value }"
            @click="changeStatusFilter(filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>
        <div class="cleanup-row">
          <label>
            保留
            <input v-model.number="retentionDays" type="number" min="1" />
            天
          </label>
          <button type="button" class="copy-button" @click="cleanupReviews">清理已处理记录</button>
        </div>
        <p v-if="notice" class="notice">{{ notice }}</p>
        <p v-if="error" class="error">{{ error }}</p>
        <button
          v-for="review in reviews"
          :key="review.metadata.name"
          type="button"
          class="review-item"
          :class="{ 'review-item--active': selectedName === review.metadata.name }"
          @click="selectedName = review.metadata.name"
        >
          <span class="review-item__title">{{ review.spec.title }}</span>
          <span class="review-item__meta">
            {{ review.spec.source || 'external' }} · {{ formatTime(review.spec.receivedAt) }}
          </span>
          <span class="badge" :class="`badge--${review.spec.status}`">{{ statusText(review.spec.status) }}</span>
        </button>
        <p v-if="!loading && reviews.length === 0" class="empty">暂无待审核记录。</p>
      </article>

      <article class="panel review-detail">
        <template v-if="selectedReview">
          <div class="panel__title panel__title--row">
            <span>
              <RiArticleLine />
              稿件预览
            </span>
            <span class="badge" :class="`badge--${selectedReview.spec.status}`">
              {{ statusText(selectedReview.spec.status) }}
            </span>
          </div>
          <h2>{{ selectedReview.spec.title }}</h2>
          <div class="meta-grid">
            <span>来源</span>
            <strong>{{ selectedReview.spec.source || 'external' }}</strong>
            <span>Slug</span>
            <strong>{{ selectedReview.spec.slug || '-' }}</strong>
            <span>接收时间</span>
            <strong>{{ formatTime(selectedReview.spec.receivedAt) }}</strong>
            <span>审核人</span>
            <strong>{{ selectedReview.spec.reviewedBy || '-' }}</strong>
            <span>审核时间</span>
            <strong>{{ formatTime(selectedReview.spec.reviewedAt) }}</strong>
            <span>尝试次数</span>
            <strong>{{ selectedReview.spec.attempts || 0 }}</strong>
          </div>
          <p v-if="selectedReview.spec.failureReason" class="error">失败原因：{{ selectedReview.spec.failureReason }}</p>
          <p v-if="selectedReview.spec.rejectReason" class="notice">拒绝原因：{{ selectedReview.spec.rejectReason }}</p>
          <a v-if="selectedReview.spec.articleUrl" class="article-link" :href="selectedReview.spec.articleUrl" target="_blank">
            查看已发布文章
          </a>
          <pre class="content-preview">{{ selectedReview.spec.content }}</pre>
          <div class="actions">
            <button
              type="button"
              class="primary-button"
              :disabled="operatingName === selectedReview.metadata.name || selectedReview.spec.status === 'approved'"
              @click="approve(selectedReview)"
            >
              <RiCheckboxCircleLine />
              通过并发布
            </button>
            <button
              type="button"
              class="danger-button"
              :disabled="operatingName === selectedReview.metadata.name || selectedReview.spec.status === 'approved'"
              @click="reject(selectedReview)"
            >
              <RiCloseCircleLine />
              拒绝
            </button>
            <button
              type="button"
              class="copy-button"
              :disabled="operatingName === selectedReview.metadata.name"
              @click="deleteReview(selectedReview)"
            >
              删除记录
            </button>
          </div>
        </template>
        <p v-else class="empty">请选择一篇稿件查看详情。</p>
      </article>
    </section>

    <section class="source-layout">
      <article class="panel">
        <div class="panel__title">
          <RiShieldKeyholeLine />
          来源管理
        </div>
        <div class="source-grid">
          <a
            v-for="source in managedSources"
            :key="source.key"
            class="source-item"
            :href="source.url"
            target="_blank"
            rel="noreferrer"
          >
            <strong>{{ source.name }}</strong>
            <span>{{ source.note }}</span>
            <code>source: "{{ source.key }}"</code>
          </a>
        </div>
      </article>
    </section>

    <section class="example-layout">
      <article class="example-panel">
        <div class="example-tabs">
          <button
            v-for="example in examples"
            :key="example.key"
            type="button"
            class="example-tab"
            :class="{ 'example-tab--active': activeExample === example.key }"
            @click="activeExample = example.key"
          >
            {{ example.label }}
          </button>
        </div>
        <div class="example-code-wrap">
          <button type="button" class="example-copy" title="复制示例" @click="copyText('example', activeExampleCode)">
            <RiFileCopyLine />
          </button>
          <pre class="example-code">{{ activeExampleCode }}</pre>
        </div>
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
  max-width: 1180px;
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
}

.syncpost__grid,
.review-layout,
.source-layout,
.example-layout {
  display: grid;
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto 16px;
}

.syncpost__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.review-layout {
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  align-items: start;
}

.source-layout {
  grid-template-columns: 1fr;
}

.example-layout {
  grid-template-columns: 1fr;
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

.panel__title--row {
  justify-content: space-between;

  span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
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
.copy-button,
.primary-button,
.danger-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.icon-button,
.copy-button {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;

  &:hover:not(:disabled) {
    background: #f8fafc;
  }
}

.icon-button {
  width: 32px;
  height: 32px;
}

.copy-button,
.primary-button,
.danger-button {
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
}

.primary-button {
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #fff;
}

.danger-button {
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #be123c;
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

.review-list {
  display: grid;
  gap: 8px;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-tab {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.status-tab--active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.cleanup-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  font-size: 13px;

  label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #334155;
  }

  input {
    width: 72px;
    height: 30px;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    padding: 0 8px;
    background: #fff;
  }
}

.review-item {
  position: relative;
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.review-item--active {
  border-color: #2563eb;
  background: #eff6ff;
}

.review-item__title {
  padding-right: 70px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.review-item__meta {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
}

.review-item .badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

.badge--pending {
  background: #fef3c7;
  color: #92400e;
}

.badge--approved {
  background: #dcfce7;
  color: #166534;
}

.badge--rejected,
.badge--failed {
  background: #ffe4e6;
  color: #be123c;
}

.review-detail h2 {
  margin: 0 0 14px;
  font-size: 20px;
  line-height: 1.35;
}

.meta-grid {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 8px 12px;
  margin-bottom: 12px;
  font-size: 13px;

  span {
    color: #64748b;
  }

  strong {
    overflow-wrap: anywhere;
    color: #172033;
    font-weight: 600;
  }
}

.article-link {
  display: inline-flex;
  margin-bottom: 12px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.source-item {
  display: grid;
  gap: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
  color: inherit;
  text-decoration: none;

  &:hover {
    border-color: #93c5fd;
    background: #eff6ff;
  }

  strong {
    font-size: 14px;
  }

  span,
  code {
    color: #64748b;
    font-size: 12px;
    overflow-wrap: anywhere;
  }
}

.example-panel {
  overflow: hidden;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
}

.example-tabs {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 46px;
  padding: 0 18px;
  border-bottom: 1px solid #d1d5db;
  background: #fff;
}

.example-tab {
  position: relative;
  height: 46px;
  border: 0;
  background: transparent;
  color: #334155;
  padding: 0;
  font-size: 14px;
  cursor: pointer;
}

.example-tab--active {
  color: #2563eb;
  font-weight: 600;

  &::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: -1px;
    height: 3px;
    border-radius: 999px 999px 0 0;
    background: #2563eb;
  }
}

.example-code-wrap {
  position: relative;
  background: #f3f4f6;
}

.example-copy {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #334155;
  cursor: pointer;

  svg {
    width: 18px;
    height: 18px;
  }
}

.example-code {
  max-height: 420px;
  min-height: 220px;
  background: #f3f4f6;
  color: #111827;
  border-radius: 0;
  padding: 28px 56px 28px 24px;
}

.notice,
.error,
.empty {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.notice {
  color: #166534;
}

.error {
  color: #be123c;
  overflow-wrap: anywhere;
}

.empty {
  color: #64748b;
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

.content-preview {
  max-height: 460px;
}

@media (max-width: 900px) {
  .syncpost {
    padding: 16px;
  }

  .syncpost__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .syncpost__grid,
  .review-layout,
  .example-layout,
  .source-grid {
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
