<template>
  <a-layout class="app-chat-page">
    <a-layout-content class="content">
      <div class="top-bar">
        <div class="app-name">{{ app?.appName || '应用对话' }}</div>
        <div class="top-bar-actions">
          <a-button v-if="app?.id" @click="showAppDetail = true">
            应用详情
          </a-button>
          <a-button
            v-if="app?.id && isOwner"
            :loading="downloadLoading"
            @click="handleDownloadCode"
          >
            下载代码
          </a-button>
          <a-button
            v-if="app?.id && isOwner"
            type="primary"
            :loading="deployLoading"
            @click="handleDeploy"
          >
            部署应用
          </a-button>
        </div>
      </div>

      <div class="main-content">
        <div class="chat-container">
          <div v-if="historyLoading || hasMoreHistory" class="history-toolbar">
            <a-button
              v-if="hasMoreHistory"
              type="link"
              :loading="loadMoreLoading"
              @click="loadMoreHistory"
            >
              加载更多
            </a-button>
            <span v-else-if="historyLoading" class="history-loading-text">正在加载历史消息...</span>
          </div>

          <div class="messages-container" ref="messagesContainer">
            <div v-if="historyLoading && messages.length === 0" class="history-placeholder">
              正在加载历史消息...
            </div>
            <div v-else-if="messages.length === 0" class="history-placeholder">
              暂无对话消息
            </div>
            <div
              v-for="(messageItem, index) in messages"
              :key="messageItem.id ?? `${messageItem.role}-${index}-${messageItem.createTime ?? ''}`"
              class="message-item"
              :class="{
                'user-message': messageItem.role === 'user',
                'ai-message': messageItem.role === 'assistant',
                'error-message': messageItem.messageType === 'error'
              }"
            >
              <div
                v-if="messageItem.role === 'user'"
                class="message-content"
              >
                {{ messageItem.content }}
              </div>
              <div
                v-else
                class="message-content markdown-body"
                v-html="renderMarkdown(messageItem.content)"
              ></div>
            </div>
            <div v-if="loading" class="message-item ai-message">
              <div class="message-content markdown-body">
                AI 正在生成代码...
              </div>
            </div>
          </div>

          <div class="input-container">
            <div v-if="selectedElements.length > 0" class="selected-elements-alert">
              <a-alert
                v-for="(element, index) in selectedElements"
                :key="index"
                type="info"
                show-icon
                closable
                @close="(e: Event) => { e.preventDefault(); visualEditor.removeElement(index) }"
                class="selected-element-item"
              >
                <template #message>
                  <span class="element-tag-name">&lt;{{ element.tagName }}{{ element.id ? ` #${element.id}` : '' }}{{ element.className ? ` .${element.className.split(' ').join(' .')}` : '' }}&gt;</span>
                  {{ element.textContent && element.textContent.length > 20 ? `${element.textContent.substring(0, 20)}...` : element.textContent }}
                </template>
              </a-alert>
            </div>
            <a-tooltip :title="isReadonly ? '无法在别人的作品下对话哦~' : null">
              <div>
                <a-textarea
                  v-model:value="userInput"
                  :placeholder="isReadonly ? '无法在别人的作品下对话哦~' : '请输入您的需求...'"
                  :rows="3"
                  @pressEnter="handleSend"
                  :disabled="loading || isReadonly"
                />
              </div>
            </a-tooltip>
            <div class="input-actions">
              <a-button
                v-if="!isEditMode"
                :disabled="isReadonly"
                @click="handleEnterEditMode"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                v-else
                danger
                @click="handleExitEditMode"
              >
                <template #icon><EditOutlined /></template>
                退出编辑
              </a-button>
              <a-button
                type="primary"
                :loading="loading"
                :disabled="isReadonly"
                @click="handleSend"
              >
                发送
              </a-button>
            </div>
          </div>
        </div>

        <div class="preview-container">
          <div v-if="!previewUrl" class="preview-placeholder">
            <div>等待代码生成完成...</div>
          </div>
          <iframe
            v-else
            ref="previewIframe"
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
          ></iframe>
        </div>
      </div>
    </a-layout-content>
  </a-layout>

  <a-modal
    v-model:open="showAppDetail"
    title="应用详情"
    :footer="null"
    width="440px"
  >
    <div v-if="app" class="app-detail-modal">
      <div class="detail-section">
        <div class="detail-section-title">应用基础信息</div>
        <div class="detail-row">
          <span class="detail-label">创建者</span>
          <div class="creator-info">
            <a-avatar :src="app.user?.userAvatar">
              {{ getUserInitial(app.user?.userName) }}
            </a-avatar>
            <span class="creator-name">{{ app.user?.userName || '未知用户' }}</span>
          </div>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ formatTime(app.createTime) }}</span>
        </div>
      </div>

      <div v-if="canManageApp" class="detail-section">
        <div class="detail-section-title">操作栏</div>
        <div class="detail-actions">
          <a-button type="primary" ghost @click="handleEditApp">
            修改
          </a-button>
          <a-button danger @click="handleDeleteApp">
            删除
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useVisualEditor } from '@/composables/useVisualEditor'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  adminDeleteApp,
  deleteApp,
  deployApp,
  downloadAppCode,
  getAppVoById
} from '@/api/appController'
import { listAppChatHistoryByPage } from '@/api/chatHistoryController'
import { API_BASE_URL, APP_PREVIEW_BASE_URL, APP_DEPLOY_BASE_URL } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { renderMarkdown } from '@/utils/markdown'
import 'highlight.js/styles/github-dark.css'

interface MessageItem {
  id?: number | string
  role: 'user' | 'assistant'
  content: string
  createTime?: string
  messageType?: string
}

const HISTORY_PAGE_SIZE = 10

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const routeAppId = route.params.id
const appId = ref<string | null>(
  typeof routeAppId === 'string' && routeAppId ? routeAppId : null
)

const app = ref<API.AppVO | null>(null)
const messages = ref<MessageItem[]>([])
const userInput = ref('')
const loading = ref(false)
const historyLoading = ref(false)
const loadMoreLoading = ref(false)
const deployLoading = ref(false)
const downloadLoading = ref(false)
const previewUrl = ref('')
const hasMoreHistory = ref(false)
const nextCursorCreateTime = ref<string>()
const nextCursorId = ref<number>()
const loadedHistoryCount = ref(0)
const showAppDetail = ref(false)
const messagesContainer = ref<HTMLDivElement | null>(null)
const previewIframe = ref<HTMLIFrameElement | null>(null)
const visualEditor = useVisualEditor({
  iframeRef: previewIframe,
  onElementSelected: (element) => {
    console.log('Element selected:', element)
  },
  onExitEditMode: () => {
    console.log('Exit edit mode')
  }
})
const isEditMode = visualEditor.isEditMode
const selectedElements = visualEditor.selectedElements

const isOwner = computed(() => !!app.value?.userId && app.value.userId === loginUserStore.loginUser.id)
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const isReadonly = computed(() => !!app.value && !isOwner.value)
const canManageApp = computed(() => isOwner.value || isAdmin.value)
const canLoadHistory = computed(() => isOwner.value || isAdmin.value)

const getUserInitial = (userName?: string) => {
  return userName?.trim()?.charAt(0)?.toUpperCase() || 'U'
}

const formatTime = (time?: string) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const mapChatHistoryToMessage = (chatHistory: API.ChatHistoryVO): MessageItem => {
  return {
    id: chatHistory.id,
    role: chatHistory.messageType === 'user' ? 'user' : 'assistant',
    content: chatHistory.message || '',
    createTime: chatHistory.createTime,
    messageType: chatHistory.messageType
  }
}

const updatePreviewUrl = () => {
  if (!app.value?.codeGenType || !appId.value) {
    previewUrl.value = ''
    return
  }
  // Vue 项目需要添加 /dist 后缀
  const distSuffix = app.value.codeGenType === 'vue_project' ? '/dist' : ''
  // 所有项目类型：从 static 目录读取源代码预览
  previewUrl.value = `/api/static/${app.value.codeGenType}_${appId.value}${distSuffix}/`
}

const refreshPreviewByMessageCount = (messageCount: number) => {
  if (messageCount >= 2) {
    updatePreviewUrl()
  }
}

const fetchApp = async () => {
  const currentAppId = appId.value
  if (!currentAppId) return

  try {
    const response = await getAppVoById({ id: currentAppId as never })
    if (response.data?.code === 0) {
      app.value = response.data.data ?? null
      // 应用已部署时，根据项目类型更新预览 URL
      if (app.value?.deployKey) {
        const distSuffix = app.value.codeGenType === 'vue_project' ? '/dist' : ''
        previewUrl.value = `/api/static/${app.value.codeGenType}_${currentAppId}${distSuffix}/`
      }
    }
  } catch (error) {
    console.error('获取应用详情失败:', error)
    message.error('获取应用详情失败')
  }
}

const fetchChatHistory = async (loadMore = false) => {
  const currentAppId = appId.value
  if (!currentAppId || !canLoadHistory.value) {
    return
  }

  const container = messagesContainer.value
  const previousScrollHeight = container?.scrollHeight ?? 0
  const previousScrollTop = container?.scrollTop ?? 0

  if (loadMore) {
    loadMoreLoading.value = true
  } else {
    historyLoading.value = true
  }

  try {
    const requestBody: API.ChatHistoryQueryRequest = {
      appId: currentAppId as never,
      pageSize: HISTORY_PAGE_SIZE
    }
    if (loadMore) {
      requestBody.cursorCreateTime = nextCursorCreateTime.value
      requestBody.cursorId = nextCursorId.value
    }

    const response = await listAppChatHistoryByPage(requestBody)
    const pageData = response.data?.data
    const historyRecords = pageData?.records ?? []
    const mappedMessages = historyRecords.map(mapChatHistoryToMessage)

    if (loadMore) {
      messages.value = [...mappedMessages, ...messages.value]
      loadedHistoryCount.value += mappedMessages.length
      await nextTick()
      if (container) {
        const currentScrollHeight = container.scrollHeight
        container.scrollTop = currentScrollHeight - previousScrollHeight + previousScrollTop
      }
    } else {
      messages.value = mappedMessages
      loadedHistoryCount.value = mappedMessages.length
      refreshPreviewByMessageCount(mappedMessages.length)
    }

    hasMoreHistory.value = pageData?.hasMore ?? false
    nextCursorCreateTime.value = pageData?.nextCursorCreateTime
    nextCursorId.value = pageData?.nextCursorId
  } catch (error) {
    console.error('加载对话历史失败:', error)
    if (!loadMore) {
      message.error('加载对话历史失败')
    } else {
      message.error('加载更多失败')
    }
  } finally {
    historyLoading.value = false
    loadMoreLoading.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const startStreamGeneration = async (prompt: string) => {
  const currentAppId = appId.value
  if (!currentAppId) return

  const assistantMessageId = `assistant-${Date.now()}`
  const userMessageItem: MessageItem = {
    id: `user-${Date.now()}`,
    role: 'user',
    content: prompt,
    messageType: 'user'
  }
  const assistantMessageItem: MessageItem = {
    id: assistantMessageId,
    role: 'assistant',
    content: '',
    messageType: 'ai'
  }

  messages.value.push(userMessageItem, assistantMessageItem)
  loadedHistoryCount.value += 2
  scrollToBottom()

  loading.value = true

  try {
    const searchParams = new URLSearchParams({
      appId: currentAppId,
      userMessage: prompt
    })
    const url = `${API_BASE_URL}/app/chat-to-gen-code?${searchParams.toString()}`
    const eventSource = new EventSource(url, { withCredentials: true })

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data === '[DONE]') {
        eventSource.close()
        loading.value = false
        refreshPreviewByMessageCount(messages.value.length)
        scrollToBottom()
        return
      }

      const targetMessage = messages.value.find(message => message.id === assistantMessageId)
      if (targetMessage) {
        targetMessage.content += data
      }
      scrollToBottom()
    }

    eventSource.onerror = (error) => {
      console.error('SSE 连接错误:', error)
      eventSource.close()
      loading.value = false
      const targetMessage = messages.value.find(message => message.id === assistantMessageId)
      if (targetMessage && !targetMessage.content) {
        targetMessage.content = '代码生成失败，请稍后重试'
        targetMessage.messageType = 'error'
      }
      message.error('代码生成失败')
    }
  } catch (error) {
    console.error('启动流式生成失败:', error)
    loading.value = false
    message.error('启动代码生成失败')
  }
}

const maybeAutoStartConversation = async () => {
  if (!isOwner.value || loadedHistoryCount.value > 0 || !app.value?.initPrompt) {
    return
  }
  await startStreamGeneration(app.value.initPrompt)
}

const loadMoreHistory = async () => {
  if (loadMoreLoading.value || !hasMoreHistory.value) {
    return
  }
  await fetchChatHistory(true)
}

const handleEnterEditMode = () => {
  visualEditor.enterEditMode()
}

const handleExitEditMode = () => {
  visualEditor.exitEditMode()
}

const handleSend = async () => {
  if (!userInput.value.trim() || loading.value || isReadonly.value) {
    return
  }

  let currentMessage = userInput.value.trim()
  userInput.value = ''

  // Add selected elements info to prompt
  if (selectedElements.value.length > 0) {
    const elementsInfo = selectedElements.value
      .map(el => `<${el.tagName}${el.id ? ` id="${el.id}"` : ''}${el.className ? ` class="${el.className}"` : ''}>${el.textContent || ''}</${el.tagName}>`)
      .join('\n')
    currentMessage = `请修改以下元素：\n${elementsInfo}\n\n用户需求：${currentMessage}`

    // Clear selection and exit edit mode after sending
    visualEditor.clearSelection()
    isEditMode.value = false
  }

  await startStreamGeneration(currentMessage)
}

const handleDeploy = async () => {
  const currentAppId = appId.value
  if (!currentAppId) return

  deployLoading.value = true
  try {
    const response = await deployApp({ appId: currentAppId as never })
    if (response.data?.code === 0) {
      const deployUrl = response.data.data
      // 更新应用中存储 deployKey 并刷新预览
      if (app.value && deployUrl) {
        const urlParts = deployUrl.split('/')
        const deployKey = urlParts[urlParts.length - 1]
        app.value.deployKey = deployKey
        // 刷新预览 URL
        updatePreviewUrl()
      }
      message.success(`部署成功！访问地址: ${deployUrl}`)
    }
  } catch (error) {
    console.error('部署失败:', error)
    message.error('部署失败')
  } finally {
    deployLoading.value = false
  }
}

const handleDownloadCode = async () => {
  const currentAppId = appId.value
  if (!currentAppId) return

  downloadLoading.value = true
  try {
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
    const response = await fetch(`${API_BASE_URL}/app/download/${currentAppId}`, {
      credentials: 'include'
    })
    if (!response.ok) {
      if (response.status === 401) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
        return
      }
      throw new Error('下载失败')
    }
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${currentAppId}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  } finally {
    downloadLoading.value = false
  }
}

const handleEditApp = () => {
  if (!appId.value) return
  showAppDetail.value = false
  router.push(`/app/edit/${appId.value}`)
}

const handleDeleteApp = () => {
  if (!app.value?.id || !canManageApp.value) return

  Modal.confirm({
    title: '确认删除该应用？',
    content: '删除后将无法恢复。',
    okText: '删除',
    okButtonProps: {
      danger: true
    },
    cancelText: '取消',
    async onOk() {
      try {
        if (!app.value?.id) return
        if (isAdmin.value && !isOwner.value) {
          await adminDeleteApp({ id: app.value.id })
        } else {
          await deleteApp({ id: app.value.id })
        }
        message.success('删除成功')
        showAppDetail.value = false
        router.push('/')
      } catch (error) {
        console.error('删除应用失败:', error)
        message.error('删除失败')
        throw error
      }
    }
  })
}

onMounted(async () => {
  try {
    await loginUserStore.fetchLoginUser()
  } catch (error) {
    console.error('获取登录用户失败:', error)
  }

  await fetchApp()
  await fetchChatHistory()
  await maybeAutoStartConversation()
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

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
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

.history-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 44px;
  padding: 0 var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
}

.history-loading-text {
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
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

.history-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
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

.error-message .message-content {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.08);
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

.app-detail-modal {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.detail-section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
}

.detail-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.detail-value {
  color: var(--color-text-primary);
}

.creator-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.creator-name {
  color: var(--color-text-primary);
}

.detail-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.detail-actions .ant-btn {
  flex: 1;
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

.selected-elements-alert {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.selected-element-item {
  padding: 4px 8px;
}

.selected-element-item :deep(.ant-alert-message) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.element-tag-name {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, Monaco, monospace;
  font-size: 12px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.input-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.input-actions .ant-btn {
  flex: 1;
}
</style>
