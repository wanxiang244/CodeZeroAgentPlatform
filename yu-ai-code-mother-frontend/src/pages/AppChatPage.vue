<template>
  <a-layout class="app-chat-page">
    <a-layout-content class="content">
      <!-- 顶部栏 -->
      <div class="top-bar">
        <div class="app-name">{{ app?.appName || '应用对话' }}</div>
        <a-button
          type="primary"
          @click="handleDeploy"
          :loading="deployLoading"
          v-if="app?.id"
        >
          部署应用
        </a-button>
      </div>

      <!-- 核心内容区域 -->
      <div class="main-content">
        <!-- 左侧对话区域 -->
        <div class="chat-container">
          <div class="messages-container" ref="messagesContainer">
            <div
              v-for="(message, index) in messages"
              :key="index"
              class="message-item"
              :class="{ 'user-message': message.role === 'user', 'ai-message': message.role === 'assistant' }"
            >
              <div
                v-if="message.role === 'user'"
                class="message-content"
              >
                {{ message.content }}
              </div>
              <div
                v-else
                class="message-content markdown-body"
                v-html="renderMarkdown(message.content)"
              ></div>
            </div>
            <div v-if="loading" class="message-item ai-message">
              <div class="message-content markdown-body">
                AI 正在生成代码...
              </div>
            </div>
          </div>

          <!-- 用户输入框 -->
          <div class="input-container">
            <a-textarea
              v-model:value="userInput"
              placeholder="请输入您的需求..."
              :rows="3"
              @pressEnter="handleSend"
              :disabled="loading"
            />
            <a-button
              type="primary"
              @click="handleSend"
              :loading="loading"
              style="margin-top: 8px;"
            >
              发送
            </a-button>
          </div>
        </div>

        <!-- 右侧网页展示区域 -->
        <div class="preview-container">
          <div v-if="!previewUrl" class="preview-placeholder">
            <div>等待代码生成完成...</div>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
          ></iframe>
        </div>
      </div>
    </a-layout-content>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppVoById, deployApp } from '@/api/appController'
import { API_BASE_URL, APP_PREVIEW_BASE_URL } from '@/config/env'
import { renderMarkdown } from '@/utils/markdown'
import 'highlight.js/styles/github-dark.css'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const routeAppId = route.params.id
const appId = ref<string | null>(
  typeof routeAppId === 'string' && routeAppId ? routeAppId : null
)
const app = ref<API.AppVO | null>(null)
const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const deployLoading = ref(false)
const previewUrl = ref('')
const messagesContainer = ref<HTMLDivElement | null>(null)

// 获取应用详情
const fetchApp = async () => {
  const currentAppId = appId.value
  if (!currentAppId) return

  try {
    const response = await getAppVoById({ id: currentAppId })
    if (response.data && response.data.code === 0) {
      app.value = response.data.data ?? null
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    message.error('获取应用详情失败')
  }
}

// 初始化对话 - 发送初始提示词
const initConversation = async () => {
  if (!app.value?.initPrompt) return

  // 添加用户消息（初始提示词）
  messages.value.push({
    role: 'user',
    content: app.value.initPrompt
  })

  // 开始流式生成
  await startStreamGeneration(app.value.initPrompt)
}

// 开始流式生成
const startStreamGeneration = async (prompt: string) => {
  const currentAppId = appId.value
  if (!currentAppId) return

  loading.value = true
  let accumulatedContent = ''

  try {
    // 使用 EventSource，构造函数中传入 withCredentials
    const url = `${API_BASE_URL}/app/chat-to-gen-code?appId=${currentAppId}`
    const eventSource = new EventSource(url, { withCredentials: true })

    eventSource.onopen = () => {
      console.log('SSE 连接成功')
    }

    eventSource.onmessage = (event) => {
      console.log('SSE 收到消息:', event.data)
      const data = event.data
      if (data === '[DONE]') {
        eventSource.close()
        loading.value = false

        console.log('代码生成完成，设置预览URL:', app.value)
        // 生成完成后显示预览
        if (app.value && app.value.codeGenType) {
          previewUrl.value = `${APP_PREVIEW_BASE_URL}/static/${app.value.codeGenType}_${currentAppId}/`
          console.log('预览URL:', previewUrl.value)
        }

        // 添加 AI 完成消息
        messages.value.push({
          role: 'assistant',
          content: accumulatedContent
        })
        scrollToBottom()
        return
      }

      accumulatedContent += data
      // 实时更新最后一条消息
      if (messages.value.length > 0 && messages.value[messages.value.length - 1].role === 'assistant') {
        messages.value[messages.value.length - 1].content = accumulatedContent
      } else {
        messages.value.push({
          role: 'assistant',
          content: accumulatedContent
        })
      }
      scrollToBottom()
    }

    eventSource.onerror = (error) => {
      console.error('SSE 连接错误:', error)
      eventSource.close()
      loading.value = false
      message.error('代码生成失败')
    }
  } catch (error) {
    console.error('启动流式生成失败:', error)
    loading.value = false
    message.error('启动代码生成失败')
  }
}

// 发送消息
const handleSend = async () => {
  if (!userInput.value.trim() || loading.value) return

  const userMessage = userInput.value.trim()
  messages.value.push({
    role: 'user',
    content: userMessage
  })

  userInput.value = ''

  // TODO: 实现后续对话逻辑
  message.info('后续对话功能待实现')
}

// 部署应用
const handleDeploy = async () => {
  const currentAppId = appId.value
  if (!currentAppId) return

  deployLoading.value = true
  try {
    const response = await deployApp({ appId: currentAppId })
    if (response.data && response.data.code === 0) {
      const deployUrl = response.data.data
      message.success(`部署成功！访问地址: ${deployUrl}`)
      // 可以在这里打开新窗口或复制到剪贴板
    }
  } catch (error) {
    console.error('部署失败:', error)
    message.error('部署失败')
  } finally {
    deployLoading.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(async () => {
  await fetchApp()
  await initConversation()
})
</script>

<style scoped>
.app-chat-page {
  height: 100vh;
  background-color: var(--color-bg-secondary);
}

.content {
  height: 100vh;
  padding: 0;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 56px;
  padding: 0 var(--spacing-md);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border);
}

.app-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.main-content {
  display: grid;
  grid-template-columns: 2fr 3fr;
  gap: 0;
  height: calc(100vh - 56px);
}

.chat-container {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
  border-right: 1px solid var(--color-border);
  overflow: hidden;
  min-width: 0;
}

.messages-container {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0));
}

.message-item {
  display: flex;
  width: 100%;
}

.message-content {
  max-width: 80%;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: 16px;
  line-height: var(--line-height-relaxed);
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.08);
  word-break: break-word;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.user-message {
  align-self: flex-end;
  justify-content: flex-end;
}

.ai-message {
  align-self: flex-start;
  justify-content: flex-start;
}

.user-message .message-content {
  background: linear-gradient(135deg, var(--color-text-primary), #6366f1);
  color: var(--color-bg-primary);
}

.ai-message .message-content {
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.3);
  color: var(--color-text-primary);
  backdrop-filter: blur(6px);
}

.markdown-body {
  font-size: var(--font-size-sm);
  line-height: 1.7;
  color: inherit;
}

.markdown-body p {
  margin: 0 0 var(--spacing-sm);
}

.markdown-body ul,
.markdown-body ol {
  margin: 0 0 var(--spacing-sm) 1.2em;
  padding-left: 1.2em;
}

.markdown-body li + li {
  margin-top: 4px;
}

.markdown-body blockquote {
  margin: var(--spacing-sm) 0;
  padding: var(--spacing-sm) var(--spacing-md);
  border-left: 3px solid var(--color-border);
  background: rgba(59, 130, 246, 0.05);
}

.markdown-body code {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, Monaco, monospace;
  font-size: 13px;
}

.markdown-body :not(pre) > code {
  padding: 0.15em 0.35em;
  border-radius: 6px;
  background: rgba(99, 102, 241, 0.12);
  color: #e0e7ff;
}

.markdown-body pre {
  margin: var(--spacing-md) 0;
  padding: var(--spacing-md);
  background: #0f172a;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 14px;
  overflow-x: auto;
}

.markdown-body pre code {
  display: block;
  background: transparent;
  color: inherit;
  padding: 0;
}

.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: var(--spacing-md) 0;
  font-size: 13px;
}

.markdown-body th,
.markdown-body td {
  border: 1px solid rgba(148, 163, 184, 0.4);
  padding: 8px 12px;
  text-align: left;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.4);
  border-radius: 999px;
}

.input-container {
  padding: var(--spacing-sm) var(--spacing-md) var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-primary);
}

.preview-container {
  background: var(--color-bg-primary);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-md);
}

.preview-iframe {
  flex: 1;
  width: 100%;
  border: none;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
    height: auto;
  }

  .preview-container {
    width: 100%;
    height: 400px;
  }

  .chat-container {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
