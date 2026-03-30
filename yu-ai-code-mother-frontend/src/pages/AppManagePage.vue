<template>
  <div class="app-manage-page">
    <a-card title="应用管理" style="margin: 20px">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="应用名称">
          <a-input v-model:value="queryParams.appName" placeholder="请输入应用名称" />
        </a-form-item>
        <a-form-item label="代码生成类型">
          <a-select v-model:value="queryParams.codeGenType" placeholder="请选择类型" allow-clear>
            <a-select-option value="html">HTML单文件</a-select-option>
            <a-select-option value="multi_file">多文件</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input-number v-model:value="queryParams.userId" placeholder="请输入用户ID" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="appList"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" danger @click="handleDelete(record)">删除</a-button>
              <a-button
                type="link"
                :disabled="record.priority === 99"
                @click="handleFeature(record)"
              >
                {{ record.priority === 99 ? '已精选' : '精选' }}
              </a-button>
            </a-space>
          </template>
          <template v-else-if="column.key === 'cover'">
            <a-image
              v-if="record.cover"
              :src="record.cover"
              width="50"
              height="50"
              style="object-fit: cover"
            />
            <span v-else>无封面</span>
          </template>
          <template v-else-if="column.key === 'deployedTime'">
            {{ record.deployedTime ? dayjs(record.deployedTime).format('YYYY-MM-DD HH:mm:ss') : '未部署' }}
          </template>
          <template v-else-if="column.key === 'createTime' || column.key === 'updateTime'">
            {{ dayjs(record[column.key]).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import * as appController from '@/api/appController'
import type { API } from '@/api/typings'

const router = useRouter()

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  appName: undefined as string | undefined,
  codeGenType: undefined as string | undefined,
  userId: undefined as number | undefined
})

// 表格数据
const appList = ref<API.AppVO[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 表格列配置
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '应用名称', dataIndex: 'appName', key: 'appName' },
  { title: '封面', key: 'cover', width: 80 },
  { title: '代码生成类型', dataIndex: 'codeGenType', key: 'codeGenType' },
  { title: '部署Key', dataIndex: 'deployKey', key: 'deployKey' },
  { title: '部署时间', key: 'deployedTime' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '创建时间', key: 'createTime' },
  { title: '更新时间', key: 'updateTime' },
  { title: '操作', key: 'action', width: 150 }
]

// 获取应用列表
const fetchAppList = async () => {
  loading.value = true
  try {
    const response = await appController.listAppVoByPage({
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })

    if (response.data?.data?.records) {
      appList.value = response.data.data.records
      pagination.total = response.data.data.totalRow || 0
    }
  } catch (error: any) {
    console.error('获取应用列表失败:', error)
    if (error.response) {
      console.error('响应数据:', error.response.data)
      message.error(`获取应用列表失败: ${error.response.data?.message || error.message}`)
    } else {
      message.error('获取应用列表失败')
    }
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchAppList()
}

// 重置
const handleReset = () => {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 20,
    sortField: '',
    sortOrder: '',
    appName: undefined,
    codeGenType: undefined,
    userId: undefined
  })
  pagination.current = 1
  fetchAppList()
}

// 表格变化处理
const handleTableChange = (paginationConfig: any) => {
  pagination.current = paginationConfig.current
  pagination.pageSize = paginationConfig.pageSize
  fetchAppList()
}

// 编辑应用
const handleEdit = (record: API.AppVO) => {
  router.push(`/app/edit/${record.id}`)
}

// 删除应用
const handleDelete = async (record: API.AppVO) => {
  try {
    await appController.adminDeleteApp({ id: record.id })
    message.success('删除成功')
    fetchAppList()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

// 精选应用
const handleFeature = async (record: API.AppVO) => {
  try {
    await appController.adminUpdateApp({
      id: record.id,
      appName: record.appName,
      cover: record.cover,
      priority: 99
    })
    message.success('设置精选成功')
    fetchAppList()
  } catch (error) {
    message.error('设置精选失败')
    console.error(error)
  }
}

// 初始化
onMounted(() => {
  fetchAppList()
})
</script>

<style scoped>
.app-manage-page {
  min-height: 100%;
}

.app-manage-page :deep(.ant-card) {
  border: none;
}

.app-manage-page :deep(.ant-card-head) {
  border-bottom: 1px solid var(--color-border-light);
  padding: var(--spacing-md) 0;
}

.app-manage-page :deep(.ant-card-head-title) {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
}

.app-manage-page :deep(.ant-form-item) {
  margin-bottom: var(--spacing-md);
}

.app-manage-page :deep(.ant-form-item-label > label) {
  font-size: var(--font-size-sm);
}

.app-manage-page :deep(.ant-table) {
  margin-top: var(--spacing-lg);
}

.app-manage-page :deep(.ant-btn-link) {
  padding: 0 var(--spacing-sm);
}
</style>
