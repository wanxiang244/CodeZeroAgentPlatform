<template>
  <!-- 可访问性：跳过导航链接 -->
  <a href="#main-content" class="skip-link">跳过导航</a>

  <a-layout-header class="header" :class="{ 'header-scrolled': isScrolled }">
    <div class="header-inner">
      <!-- 左侧：Logo和标题 -->
      <RouterLink to="/" class="header-logo-link">
        <div class="header-left">
          <img class="logo" src="@/assets/logo.png" alt="鱼皮应用生成 Logo" />
          <h1 class="site-title">鱼皮应用生成</h1>
        </div>
      </RouterLink>

      <!-- 中间：桌面端导航菜单 -->
      <div class="header-menu desktop-menu">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </div>

      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space class="user-info">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                <span class="username">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>

        <!-- 移动端汉堡菜单按钮 -->
        <button
          class="mobile-menu-btn"
          :aria-expanded="mobileMenuVisible"
          aria-label="打开导航菜单"
          @click="mobileMenuVisible = !mobileMenuVisible"
        >
          <span class="hamburger" :class="{ 'hamburger-open': mobileMenuVisible }">
            <span></span>
            <span></span>
            <span></span>
          </span>
        </button>
      </div>
    </div>

    <!-- 移动端抽屉菜单 -->
    <a-drawer
      v-model:open="mobileMenuVisible"
      placement="right"
      :width="260"
      :closable="true"
      title="导航"
    >
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="vertical"
        :items="menuItems"
        @click="handleMobileMenuClick"
      />
    </a-drawer>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { userLogout } from '@/api/userController.ts'

// 获取登录用户状态
const loginUserStore = useLoginUserStore()

const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/']
)
// 移动端菜单显示状态
const mobileMenuVisible = ref(false)
// 滚动状态
const isScrolled = ref(false)

// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 监听滚动，添加阴影
const handleScroll = () => {
  isScrolled.value = window.scrollY > 4
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    label: '首页',
    title: '首页',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/chatHistoryManage',
    label: '对话管理',
    title: '对话管理',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理桌面端菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 处理移动端菜单点击
const handleMobileMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  mobileMenuVisible.value = false
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 跳过导航链接（可访问性） */
.skip-link {
  position: absolute;
  top: -40px;
  left: 16px;
  padding: 8px 16px;
  background: var(--color-primary, #1677ff);
  color: #fff;
  border-radius: 0 0 4px 4px;
  z-index: 9999;
  font-size: 14px;
  text-decoration: none;
  transition: top 0.2s ease;
}

.skip-link:focus {
  top: 0;
}

/* 导航栏主体 */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg-primary) !important;
  padding: 0;
  height: var(--header-height);
  line-height: var(--header-height);
  border-bottom: 1px solid var(--color-border-light);
  transition: box-shadow 0.25s ease;
}

.header-scrolled {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 var(--spacing-lg);
  gap: var(--spacing-md);
}

/* Logo 区域 */
.header-logo-link {
  text-decoration: none;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  transition: opacity 0.2s ease;
  cursor: pointer;
  min-width: 160px;
}

.header-left:hover {
  opacity: 0.75;
}

.logo {
  height: 36px;
  width: 36px;
  display: block;
}

.site-title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: 1px;
  text-transform: uppercase;
  white-space: nowrap;
}

/* 中间菜单 */
.header-menu {
  flex: 1;
  min-width: 0;
}

.header-menu :deep(.ant-menu-horizontal) {
  border-bottom: none !important;
  line-height: var(--header-height);
  background: transparent;
}

/* 右侧区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}

.user-login-status {
  display: flex;
  align-items: center;
}

/* 用户信息 hover */
.user-info {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s ease;
}

.user-info:hover {
  background: var(--color-bg-secondary, rgba(0, 0, 0, 0.04));
}

.username {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

/* 登录按钮 hover */
.header :deep(.ant-btn-primary) {
  transition: all 0.2s ease;
}

.header :deep(.ant-btn-primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}

/* 移动端汉堡按钮 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s ease;
  min-width: 44px;
  min-height: 44px;
  align-items: center;
  justify-content: center;
}

.mobile-menu-btn:hover {
  background: var(--color-bg-secondary, rgba(0, 0, 0, 0.04));
}

.mobile-menu-btn:focus-visible {
  outline: 2px solid var(--color-primary, #1677ff);
  outline-offset: 2px;
}

/* 汉堡图标 */
.hamburger {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 20px;
}

.hamburger span {
  display: block;
  height: 2px;
  width: 100%;
  background: var(--color-text-primary);
  border-radius: 2px;
  transition: transform 0.25s ease, opacity 0.25s ease;
  transform-origin: center;
}

.hamburger-open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.hamburger-open span:nth-child(2) {
  opacity: 0;
  transform: scaleX(0);
}

.hamburger-open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* 响应式：移动端 */
@media (max-width: 767px) {
  .desktop-menu {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .site-title {
    font-size: var(--font-size-md, 14px);
    letter-spacing: 0;
  }

  .header-left {
    min-width: auto;
  }
}

/* 平板端 */
@media (min-width: 768px) and (max-width: 1023px) {
  .site-title {
    display: none;
  }
}
</style>
