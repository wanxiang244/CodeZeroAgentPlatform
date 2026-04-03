<template>
  <div class="chat-history-manage-page">
    <a-card title="对话管理" style="margin: 20px">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="应用ID">
          <a-input-number v-model:value="queryParams.appId" placeholder="请输入应用ID" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input-number v-model:value="queryParams.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select v-model:value="queryParams.messageType" placeholder="请选择消息类型" allow-clear>
            <a-select-option value="user">用户消息</a-select-option>
            <a-select-option value="ai">AI消息</a-select-option>
            <a-select-option value="error">错误消息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="消息内容">
          <a-input v-model:value="queryParams.message" placeholder="请输入消息内容" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="chatHistoryList"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        table-layout="fixed"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'messageType'">
            <a-tag :color="getMessageTypeColor(record.messageType)">
              {{ getMessageTypeText(record.messageType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'message'">
            <div class="message-cell" :title="record.message">
              {{ record.message || '-' }}
            </div>
          </template>
          <template v-else-if="column.key === 'user'">
            <div class="user-cell">
              <a-avatar :src="record.user?.userAvatar" size="small">
                {{ getUserInitial(record.user?.userName) }}
              </a-avatar>
              <span>{{ record.user?.userName || '未知用户' }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'createTime' || column.key === 'updateTime'">
            {{ record[column.key] ? dayjs(record[column.key]).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleViewApp(record)">查看应用</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { listChatHistoryByPageForAdmin } from '@/api/chatHistoryController'

const router = useRouter()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  appId: undefined as number | undefined,
  userId: undefined as number | undefined,
  messageType: undefined as string | undefined,
  message: undefined as string | undefined
})

const chatHistoryList = ref<API.ChatHistoryVO[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '应用ID', dataIndex: 'appId', key: 'appId', width: 120 },
  { title: '应用名称', dataIndex: 'appName', key: 'appName', width: 160 },
  { title: '发送用户', key: 'user', width: 180 },
  { title: '消息类型', key: 'messageType', width: 110 },
  { title: '消息内容', key: 'message' },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '更新时间', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 110 }
]

const getUserInitial = (userName?: string) => {
  return userName?.trim()?.charAt(0)?.toUpperCase() || 'U'
}

const getMessageTypeText = (messageType?: string) => {
  if (messageType === 'user') return '用户消息'
  if (messageType === 'ai') return 'AI消息'
  if (messageType === 'error') return '错误消息'
  return '未知类型'
}

const getMessageTypeColor = (messageType?: string) => {
  if (messageType === 'user') return 'blue'
  if (messageType === 'ai') return 'green'
  if (messageType === 'error') return 'red'
  return 'default'
}

const fetchChatHistoryList = async () => {
  loading.value = true
  try {
    const response = await listChatHistoryByPageForAdmin({
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })

    if (response.data?.data?.records) {
      chatHistoryList.value = response.data.data.records
      pagination.total = response.data.data.totalRow || 0
    }
  } catch (error: any) {
    console.error('获取对话列表失败:', error)
    if (error.response) {
      message.error(`获取对话列表失败: ${error.response.data?.message || error.message}`)
    } else {
      message.error('获取对话列表失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchChatHistoryList()
}

const handleReset = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 20,
    appId: undefined,
    userId: undefined,
    messageType: undefined,
    message: undefined
  })
  pagination.current = 1
  fetchChatHistoryList()
}

const handleTableChange = (paginationConfig: any) => {
  pagination.current = paginationConfig.current
  pagination.pageSize = paginationConfig.pageSize
  fetchChatHistoryList()
}

const handleViewApp = (record: API.ChatHistoryVO) => {
  if (!record.appId) {
    message.error('应用ID不存在')
    return
  }
  router.push(`/app/chat/${record.appId}`)
}

onMounted(() => {
  fetchChatHistoryList()
})
</script>

<style scoped>
.chat-history-manage-page {
  min-height: 100%;
}

.chat-history-manage-page :deep(.ant-card) {
  border: none;
}

.chat-history-manage-page :deep(.ant-card-head) {
  border-bottom: 1px solid var(--color-border-light);
  padding: var(--spacing-md) 0;
}

.chat-history-manage-page :deep(.ant-card-head-title) {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
}

.chat-history-manage-page :deep(.ant-form-item) {
  margin-bottom: var(--spacing-md);
}

.chat-history-manage-page :deep(.ant-form-item-label > label) {
  font-size: var(--font-size-sm);
}

.chat-history-manage-page :deep(.ant-table) {
  margin-top: var(--spacing-lg);
}

.message-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}
</style>
