import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

import AppLayout from '../components/layout/AppLayout.vue'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import RegisterPage from '../pages/RegisterPage.vue'
import OauthBindPage from '../pages/OauthBindPage.vue'
import UserCenterPage from '../pages/UserCenterPage.vue'
import ChangePasswordPage from '../pages/ChangePasswordPage.vue'
import QuestionListPage from '../pages/QuestionListPage.vue'
import QuestionAddPage from '../pages/QuestionAddPage.vue'
import QuestionEditPage from '../pages/QuestionEditPage.vue'
import QuestionDetailPage from '../pages/QuestionDetailPage.vue'
import MaterialListPage from '../pages/MaterialListPage.vue'
import MaterialFavoritePage from '../pages/MaterialFavoritePage.vue'
import MaterialAddPage from '../pages/MaterialAddPage.vue'
import MaterialEditPage from '../pages/MaterialEditPage.vue'
import MaterialDetailPage from '../pages/MaterialDetailPage.vue'
import NoteListPage from '../pages/NoteListPage.vue'
import NoteAddPage from '../pages/NoteAddPage.vue'
import NoteAddFullscreenPage from '../pages/NoteAddFullscreenPage.vue'
import NoteEditPage from '../pages/NoteEditPage.vue'
import NoteEditFullscreenPage from '../pages/NoteEditFullscreenPage.vue'
import NoteDetailPage from '../pages/NoteDetailPage.vue'
import TagListPage from '../pages/TagListPage.vue'
import TagRelationPage from '../pages/TagRelationPage.vue'
import SearchResultPage from '../pages/SearchResultPage.vue'
import StatisticsPage from '../pages/StatisticsPage.vue'
import ReviewCenterPage from '../pages/ReviewCenterPage.vue'
import ReviewModePage from '../pages/ReviewModePage.vue'
import NotFoundPage from '../pages/NotFoundPage.vue'

const routes = [
  { path: '/', redirect: '/index' },
  { path: '/index', component: HomePage, meta: { publicPage: true } },
  { path: '/login', component: LoginPage, meta: { guestOnly: true } },
  { path: '/register', component: RegisterPage, meta: { guestOnly: true } },
  { path: '/oauth/bind', component: OauthBindPage, meta: { guestOnly: true } },
  { path: '/note/add/fullscreen', component: NoteAddFullscreenPage, meta: { requiresAuth: true } },
  { path: '/note/update/:noteId/fullscreen', component: NoteEditFullscreenPage, meta: { requiresAuth: true } },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: 'user/center', component: UserCenterPage, meta: { requiresAuth: true } },
      { path: 'user/change-password', component: ChangePasswordPage, meta: { requiresAuth: true } },

      {
        path: 'error-question/list',
        component: QuestionListPage,
        meta: { requiresAuth: true, disableViewFadeIn: true, disableViewScroll: true }
      },
      { path: 'error-question/add', component: QuestionAddPage, meta: { requiresAuth: true } },
      { path: 'error-question/update/:questionId', component: QuestionEditPage, meta: { requiresAuth: true } },
      { path: 'error-question/detail/:questionId', component: QuestionDetailPage, meta: { requiresAuth: true } },

      { path: 'study-material/list', component: MaterialListPage, meta: { requiresAuth: true, disableViewScroll: true } },
      { path: 'study-material/favorite', component: MaterialFavoritePage, meta: { requiresAuth: true, disableViewScroll: true } },
      { path: 'study-material/add', component: MaterialAddPage, meta: { requiresAuth: true } },
      { path: 'study-material/update/:materialId', component: MaterialEditPage, meta: { requiresAuth: true } },
      { path: 'study-material/detail/:materialId', component: MaterialDetailPage, meta: { requiresAuth: true } },

      { path: 'note/list', component: NoteListPage, meta: { requiresAuth: true, disableViewScroll: true } },
      { path: 'note/add', component: NoteAddPage, meta: { requiresAuth: true, disableViewScroll: true } },
      { path: 'note/update/:noteId', component: NoteEditPage, meta: { requiresAuth: true } },
      { path: 'note/detail/:noteId', component: NoteDetailPage, meta: { requiresAuth: true } },

      { path: 'tag/list', component: TagListPage, meta: { requiresAuth: true, disableViewScroll: true } },
      { path: 'statistics/overview', component: StatisticsPage, meta: { requiresAuth: true } },
      { path: 'review/center', component: ReviewCenterPage, meta: { requiresAuth: true } },
      { path: 'review/mode', component: ReviewModePage, meta: { requiresAuth: true } },
      { path: 'tag/relation/:tagId', component: TagRelationPage, meta: { requiresAuth: true } },
      { path: 'search/result', component: SearchResultPage, meta: { requiresAuth: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', component: NotFoundPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.bootstrap()

  if (to.query.oauthLoggedIn === '1' && !authStore.isLoggedIn) {
    try {
      await authStore.refreshProfile()
    } catch (error) {
      authStore.clearProfile()
    }
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return '/index'
  }

  return true
})

export default router
