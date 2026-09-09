<script lang="ts" setup>
import { reactive } from 'vue'
import { userRegister } from '@/api/userController.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { RuleObject } from 'ant-design-vue/es/form'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const router = useRouter()

// 自定义验证函数
async function validateCheckPassword(rule: RuleObject, value: string) {
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userRegister(formState)
  // 注册成功
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>
<template>
  <div id="userRegisterPage">
    <h2 class="title">鱼皮 AI 应用生成 - 用户注册</h2>
    <div class="desc">不写一行代码，生成完整应用</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
          { validator: validateCheckPassword, trigger: 'blur' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<style scoped>
#userRegisterPage {
  max-width: 400px;
  margin: 0 auto;
  padding: var(--spacing-xxl) var(--spacing-lg);
}

.title {
  text-align: center;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-sm);
  letter-spacing: -0.5px;
}

.desc {
  text-align: center;
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xxl);
  font-size: var(--font-size-base);
}

.tips {
  text-align: right;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-lg);
}

.tips a {
  color: var(--color-text-primary);
  text-decoration: underline;
  font-weight: var(--font-weight-medium);
}

.ant-form-item {
  margin-bottom: var(--spacing-lg);
}

.ant-btn-primary {
  height: 44px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
}
</style>
