<template>
  <div class="app-edit-page">
    <a-card :title="isEdit ? '编辑应用' : '创建应用'" style="margin: 20px">
      <a-form
        :model="form"
        :rules="rules"
        ref="formRef"
        @finish="handleSubmit"
        label-col="{ span: 4 }"
        wrapper-col="{ span: 16 }"
      >
        <a-form-item label="应用名称" name="appName">
          <a-input v-model:value="form.appName" placeholder="请输入应用名称" />
        </a-form-item>

        <a-form-item label="封面URL" name="cover">
          <a-input v-model:value="form.cover" placeholder="请输入封面图片URL" />
        </a-form-item>

        <a-form-item v-if="isAdmin && isEdit" label="优先级" name="priority">
          <a-input-number v-model:value="form.priority" :min="0" :max="999" />
        </a-form-item>

        <a-form-item v-if="!isEdit" label="初始提示词" name="initPrompt">
          <a-textarea
            v-model:value="form.initPrompt"
            placeholder="请输入初始提示词，用于生成代码"
            :rows="4"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" html-type="submit" :loading="submitting">
            {{ isEdit ? '更新' : '创建' }}
          </a-button>
          <a-button style="margin-left: 8px" @click="handleCancel">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import * as appController from '@/api/appController'
import * as userController from '@/api/userController'
import type { API } from '@/api/typings'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 表单引用
const formRef = ref()

// 判断是否为编辑模式
const isEdit = computed(() => !!route.params.id)

// 判断是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

// 表单数据
const form = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: ''
})

// 表单规则
const rules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  initPrompt: [{ required: true, message: '请输入初始提示词', trigger: 'blur' }]
}

const submitting = ref(false)

// 获取应用详情（编辑模式）
const fetchAppDetail = async () => {
  if (!isEdit.value) return

  const appId = route.params.id as string
  try {
    const response = await appController.getAppVOById({ id: appId })
    if (response.data) {
      Object.assign(form, {
        appName: response.data.appName || '',
        cover: response.data.cover || '',
        priority: response.data.priority || 0
      })
    }
  } catch (error) {
    message.error('获取应用详情失败')
    console.error(error)
    router.back()
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validateFields()
    submitting.value = true

    if (isEdit.value) {
      // 编辑应用
      const appId = route.params.id as string

      // 管理员可以编辑所有字段，普通用户只能编辑应用名称和封面
      const updateData: any = {
        id: appId,
        appName: form.appName,
        cover: form.cover
      }

      if (isAdmin.value) {
        updateData.priority = form.priority
      }

      const response = isAdmin.value
        ? await appController.adminUpdateApp(updateData)
        : await appController.updateApp(updateData)

      if (response.data) {
        message.success('更新成功')
        router.push(`/app/chat/${appId}`)
      }
    } else {
      // 创建应用
      const response = await appController.addApp({
        initPrompt: form.initPrompt
      })

      if (response.data?.data) {
        message.success('创建成功')
        // 跳转到对话页面并自动开始生成
        router.push(`/app/chat/${response.data.data}`)
      }
    }
  } catch (error) {
    message.error(isEdit.value ? '更新失败' : '创建失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// 取消操作
const handleCancel = () => {
  router.back()
}

// 初始化
onMounted(() => {
  if (isEdit.value) {
    fetchAppDetail()
  }
})
</script>

<style scoped>
.app-edit-page {
  min-height: 100vh;
}
</style>