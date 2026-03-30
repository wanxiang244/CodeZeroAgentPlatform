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
              <div class="message-content">
                {{ message.content }}
              </div>
            </div>
            <div v-if="loading" class="message-item ai-message">
              <div class="message-content">AI 正在生成代码...</div>
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
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppById, chatToGenCode } from '@/api/appController'
import { deployApp } from '@/api/appController'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const router = useRouter()
const appId = ref<string>(String(route.params.id) || '')
const app = ref<API.AppVO | null>(null)
const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const deployLoading = ref(false)
const previewUrl = ref('')
const messagesContainer = ref<HTMLDivElement | null>(null)

// 获取应用详情
const fetchApp = async () => {
  if (!appId.value) return

  try {
    const response = await getAppById({ id: appId.value })
    if (response.data && response.data.code === 0) {
      app.value = response.data.data
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
  if (!appId.value) return

  loading.value = true
  let accumulatedContent = ''

  try {
    // 使用 EventSource，构造函数中传入 withCredentials
    const url = `http://localhost:8123/api/app/chat-to-gen-code?appId=${appId.value}`
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
          previewUrl.value = `http://localhost:8123/api/static/${app.value.codeGenType}_${appId.value}/`
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
  if (!appId.value) return

  deployLoading.value = true
  try {
    const response = await deployApp({ appId: appId.value })
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
  padding: var(--spacing-lg);
  max-width: 1600px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  margin-bottom: var(--spacing-lg);
}

.app-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.main-content {
  display: flex;
  gap: var(--spacing-lg);
  height: calc(100vh - 200px);
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: var(--spacing-md);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.message-item {
  max-width: 80%;
  padding: var(--spacing-md) var(--spacing-lg);
  word-wrap: break-word;
  line-height: var(--line-height-relaxed);
  border-radius: 0;
}

.user-message {
  align-self: flex-end;
  background-color: var(--color-text-primary);
  color: var(--color-bg-primary);
}

.ai-message {
  align-self: flex-start;
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.input-container {
  padding: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-primary);
}

.preview-container {
  width: 500px;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
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
    flex-direction: column;
  }

  .preview-container {
    width: 100%;
    height: 400px;
  }
}
</style>