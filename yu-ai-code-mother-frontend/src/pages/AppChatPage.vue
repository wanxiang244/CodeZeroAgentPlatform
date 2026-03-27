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
const appId = ref<number>(Number(route.params.id) || 0)
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
    const eventSource = new EventSource(
      `http://localhost:8123/api/app/chat-to-gen-code?appId=${appId.value}`
    )

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data === '[DONE]') {
        eventSource.close()
        loading.value = false

        // 生成完成后显示预览
        if (app.value) {
          previewUrl.value = `http://localhost:8123/api/static/${app.value.codeGenType}_${appId.value}/`
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
  background-color: #f5f5f5;
}

.content {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.app-name {
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
}

.main-content {
  display: flex;
  gap: 20px;
  height: calc(100vh - 200px);
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.5;
}

.user-message {
  align-self: flex-end;
  background-color: #1890ff;
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message {
  align-self: flex-start;
  background-color: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.input-container {
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background: white;
}

.preview-container {
  width: 500px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.preview-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 16px;
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