import {
  AlarmClock,
  CollectionTag,
  DataAnalysis,
  Document,
  HomeFilled,
  Management,
  Memo,
  Notebook,
  Star,
  User
} from '@element-plus/icons-vue'

export const SIDEBAR_MENU_ITEMS = [
  {
    path: '/index',
    label: '首页',
    icon: HomeFilled,
    ariaLabel: '首页',
    activePrefixes: ['/index', '/']
  },
  {
    path: '/error-question/list',
    label: '错题管理',
    icon: Memo,
    ariaLabel: '错题管理',
    activePrefixes: ['/error-question']
  },
  {
    path: '/study-material/list',
    label: '资料管理',
    icon: Document,
    ariaLabel: '资料管理',
    activePrefixes: ['/study-material']
  },
  {
    path: '/note/list',
    label: '笔记',
    icon: Notebook,
    ariaLabel: '笔记',
    activePrefixes: ['/note']
  },
  {
    path: '/review/center',
    label: '我的复习',
    icon: AlarmClock,
    ariaLabel: '我的复习',
    activePrefixes: ['/review']
  },
  {
    path: '/tag/list',
    label: '标签库',
    icon: CollectionTag,
    ariaLabel: '标签库',
    activePrefixes: ['/tag']
  },
  {
    path: '/statistics/overview',
    label: '统计分析',
    icon: DataAnalysis,
    ariaLabel: '统计分析',
    activePrefixes: ['/statistics']
  },
  {
    path: '/study-material/favorite',
    label: '我的收藏',
    icon: Star,
    ariaLabel: '我的收藏',
    activePrefixes: ['/study-material/favorite']
  },
  {
    path: '/user/center',
    label: '个人中心',
    icon: User,
    ariaLabel: '个人中心',
    activePrefixes: ['/user']
  }
]

export function resolveSidebarActivePath(path) {
  if (!path) {
    return '/index'
  }

  let matchedPath = '/index'
  let matchedLength = -1

  for (const item of SIDEBAR_MENU_ITEMS) {
    for (const prefix of item.activePrefixes || []) {
      if (prefix === '/' && (path === '/' || path === '/index')) {
        if (1 > matchedLength) {
          matchedPath = item.path
          matchedLength = 1
        }
        continue
      }

      if (prefix !== '/' && path.startsWith(prefix) && prefix.length > matchedLength) {
        matchedPath = item.path
        matchedLength = prefix.length
      }
    }
  }

  return matchedPath
}
