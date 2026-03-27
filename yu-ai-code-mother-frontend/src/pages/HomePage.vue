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
    <div class="my-apps-section" style="padding: 20px">
      <h2 style="margin-bottom: 20px">我的应用</h2>
      <div v-if="myAppList.length === 0" style="text-align: center; color: #999; padding: 40px 0">
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
              hoverable
              @click="goToAppChat(app.id)"
              style="cursor: pointer"
            >
              <template #cover>
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  alt="封面"
                  style="height: 150px; object-fit: cover"
                />
                <div
                  v-else
                  style="height: 150px; background: #f5f5f5; display: flex; align-items: center; justify-content: center"
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
          v-model:current="myPagination.current"
          v-model:pageSize="myPagination.pageSize"
          :total="myPagination.total"
          show-size-changer
          @change="handleMyAppPageChange"
          style="text-align: center; margin-top: 20px"
        />
      </div>
    </div>

    <!-- 精选应用分页列表 -->
    <div class="featured-apps-section" style="padding: 20px; background: #f9f9f9">
      <h2 style="margin-bottom: 20px">精选应用</h2>
      <div v-if="featuredAppList.length === 0" style="text-align: center; color: #999; padding: 40px 0">
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
              hoverable
              @click="goToAppChat(app.id)"
              style="cursor: pointer"
            >
              <template #cover>
                <img
                  v-if="app.cover"
                  :src="app.cover"
                  alt="封面"
                  style="height: 150px; object-fit: cover"
                />
                <div
                  v-else
                  style="height: 150px; background: #f5f5f5; display: flex; align-items: center; justify-content: center"
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
          v-model:current="featuredPagination.current"
          v-model:pageSize="featuredPagination.pageSize"
          :total="featuredPagination.total"
          show-size-changer
          @change="handleFeaturedAppPageChange"
          style="text-align: center; margin-top: 20px"
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

    if (response.data) {
      message.success('应用创建成功')
      // 跳转到对话页面并自动开始生成
      router.push(`/app/chat/${response.data}`)
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
const goToAppChat = (appId: number) => {
  router.push(`/app/chat/${appId}`)
}

const goToAppEdit = (appId: number) => {
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
  min-height: 100vh;
}
</style>