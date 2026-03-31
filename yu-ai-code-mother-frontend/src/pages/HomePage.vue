<template>
  <div class="home-page">
    <!-- 网站标题 -->
    <div class="hero-section" style="text-align: center; padding: 60px 20px">
      <h1 style="font-size: 2.5rem; margin-bottom: 20px">AI代码生成平台</h1>
      <p style="font-size: 1.2rem; color: #666; margin-bottom: 30px">
        通过自然语言描述，自动生成前端代码
      </p>

      <!-- 用户提示词输入框 -->
      <a-input-search
        v-model:value="promptInput"
        placeholder="请输入您想要生成的网站描述，例如：创建一个电商网站首页..."
        enter-button="创建应用"
        size="large"
        @search="handleCreateApp"
        style="max-width: 800px; width: 100%"
      />
    </div>

    <!-- 我的应用分页列表 -->
    <div class="my-apps-section">
      <h2>我的应用</h2>
      <div v-if="myAppList.length === 0" class="empty-state">
        暂无应用，快去创建一个吧！
      </div>
      <div v-else>
        <a-row :gutter="16">
          <a-col
            v-for="app in myAppList"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            style="margin-bottom: 16px"
          >
            <a-card
              class="app-card"
              @click="goToAppChat(app.id)"
            >
              <template #cover>
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  alt="封面"
                />
                <div
                  v-else
                  class="app-cover-placeholder"
                >
                  <span>无封面</span>
                </div>
              </template>
              <a-card-meta :title="app.appName" />
              <template #actions>
                <a-space>
                  <EditOutlined @click.stop="goToAppEdit(app.id)" />
                  <DeleteOutlined @click.stop="handleDeleteMyApp(app)" />
                </a-space>
              </template>
            </a-card>
          </a-col>
        </a-row>

        <!-- 分页器 -->
        <a-pagination
          v-if="myPagination.total > myPagination.pageSize"
          class="pagination-wrapper"
          v-model:current="myPagination.current"
          v-model:pageSize="myPagination.pageSize"
          :total="myPagination.total"
          show-size-changer
          @change="handleMyAppPageChange"
        />
      </div>
    </div>

    <!-- 精选应用分页列表 -->
    <div class="featured-apps-section">
      <h2>精选应用</h2>
      <div v-if="featuredAppList.length === 0" class="empty-state">
        暂无精选应用
      </div>
      <div v-else>
        <a-row :gutter="16">
          <a-col
            v-for="app in featuredAppList"
            :key="app.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            style="margin-bottom: 16px"
          >
            <a-card
              class="app-card"
              @click="goToAppChat(app.id)"
            >
              <template #cover>
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  alt="封面"
                />
                <div
                  v-else
                  class="app-cover-placeholder"
                >
                  <span>无封面</span>
                </div>
              </template>
              <a-card-meta :title="app.appName" />
            </a-card>
          </a-col>
        </a-row>

        <!-- 分页器 -->
        <a-pagination
          v-if="featuredPagination.total > featuredPagination.pageSize"
          class="pagination-wrapper"
          v-model:current="featuredPagination.current"
          v-model:pageSize="featuredPagination.pageSize"
          :total="featuredPagination.total"
          show-size-changer
          @change="handleFeaturedAppPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import * as appController from '@/api/appController'
import type { API } from '@/api/typings'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'

const router = useRouter()

// 提示词输入
const promptInput = ref('')

// 我的应用列表
const myAppList = ref<API.AppVO[]>([])
const myPagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
})

// 精选应用列表
const featuredAppList = ref<API.AppVO[]>([])
const featuredPagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
})

// 创建应用
const handleCreateApp = async () => {
  if (!promptInput.value.trim()) {
    message.warning('请输入提示词')
    return
  }

  try {
    const response = await appController.addApp({
      initPrompt: promptInput.value.trim()
    })

    if (response.data?.data) {
      message.success('应用创建成功')
      // 跳转到对话页面并自动开始生成
      router.push(`/app/chat/${response.data.data}`)
    }
  } catch (error) {
    message.error('创建应用失败')
    console.error(error)
  }
}

// 获取我的应用列表
const fetchMyAppList = async () => {
  try {
    const response = await appController.listMyAppByPage({
      pageNum: myPagination.value.current,
      pageSize: myPagination.value.pageSize
    })

    if (response.data?.data?.records) {
      myAppList.value = response.data.data.records
      myPagination.value.total = response.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('获取我的应用列表失败')
    console.error(error)
  }
}

// 获取精选应用列表
const fetchFeaturedAppList = async () => {
  try {
    const response = await appController.listFeaturedAppByPage({
      pageNum: featuredPagination.value.current,
      pageSize: featuredPagination.value.pageSize
    })

    if (response.data?.data?.records) {
      featuredAppList.value = response.data.data.records
      featuredPagination.value.total = response.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('获取精选应用列表失败')
    console.error(error)
  }
}

// 删除我的应用
const handleDeleteMyApp = async (app: API.AppVO) => {
  try {
    await appController.deleteApp({ id: app.id })
    message.success('删除成功')
    fetchMyAppList()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

// 页面跳转
const goToAppChat = (appId: string | number) => {
  router.push(`/app/chat/${appId}`)
}

const goToAppEdit = (appId: string | number | undefined) => {
  if (appId === undefined || appId === null) {
    message.error('应用 id 无效')
    return
  }
  router.push(`/app/edit/${appId}`)
}

// 分页变化处理
const handleMyAppPageChange = (page: number, pageSize: number) => {
  myPagination.value.current = page
  myPagination.value.pageSize = pageSize
  fetchMyAppList()
}

const handleFeaturedAppPageChange = (page: number, pageSize: number) => {
  featuredPagination.value.current = page
  featuredPagination.value.pageSize = pageSize
  fetchFeaturedAppList()
}

// 初始化
onMounted(() => {
  fetchMyAppList()
  fetchFeaturedAppList()
})
</script>

<style scoped>
.home-page {
  min-height: 100%;
}

/* Hero Section */
.hero-section {
  text-align: center;
  padding: var(--spacing-xxxl) var(--spacing-lg);
  background: var(--color-bg-primary);
}

.hero-section h1 {
  font-size: var(--font-size-xxxl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-md);
  letter-spacing: -1px;
}

.hero-section p {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xl);
}

.hero-section .ant-input-search {
  max-width: 800px;
  width: 100%;
}

.hero-section .ant-input-search .ant-input-search-button {
  border-radius: 0 !important;
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.hero-section .ant-input-search .ant-input-search-button:hover {
  background: var(--color-accent-hover);
  border-color: var(--color-accent-hover);
}

/* Section titles */
.my-apps-section,
.featured-apps-section {
  padding: var(--spacing-xl) 0;
}

.my-apps-section h2,
.featured-apps-section h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
}

/* Empty state */
.empty-state {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: var(--spacing-xxl) 0;
  font-size: var(--font-size-base);
}

/* App Cards */
.app-card {
  cursor: pointer;
  transition: border-color var(--transition-duration) var(--transition-timing);
  background: var(--color-bg-primary);
}

.app-card:hover {
  border-color: var(--color-text-secondary);
}

.app-card .ant-card-cover {
  height: 150px;
  overflow: hidden;
}

.app-card .ant-card-cover img {
  object-fit: cover;
}

.app-card .ant-card-meta-title {
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-base);
}

.app-card .ant-card-actions {
  background: var(--color-bg-primary);
}

.app-card .ant-card-actions li {
  margin: var(--spacing-sm) 0;
}

/* Featured section background */
.featured-apps-section {
  background: var(--color-bg-secondary);
}

/* Pagination */
.pagination-wrapper {
  text-align: center;
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
}

/* Card cover placeholder */
.app-cover-placeholder {
  height: 150px;
  background: var(--color-bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
}
</style>
