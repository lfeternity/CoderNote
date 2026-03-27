export const FLOAT_BUTTON_GROUPS = [
  { key: 'create', label: '新建类' },
  { key: 'ai', label: 'AI 类' },
  { key: 'tools', label: '工具类' },
  { key: 'settings', label: '设置类' }
]

export const FLOAT_BUTTON_ITEMS = [
  {
    key: 'new-question',
    group: 'create',
    label: '新建错题',
    icon: 'CirclePlus',
    action: 'route',
    payload: { path: '/error-question/add' }
  },
  {
    key: 'new-note',
    group: 'create',
    label: '新建笔记',
    icon: 'EditPen',
    action: 'route',
    payload: { path: '/note/add' }
  },
  {
    key: 'new-material',
    group: 'create',
    label: '新建资料',
    icon: 'Collection',
    action: 'route',
    payload: { path: '/study-material/add' }
  },
  {
    key: 'ai-solve',
    group: 'ai',
    label: 'AI 解题',
    icon: 'Cpu',
    action: 'ai-solve'
  },
  {
    key: 'ai-summary',
    group: 'ai',
    label: 'AI 总结',
    icon: 'Document',
    action: 'ai-summary'
  },
  {
    key: 'ai-chat',
    group: 'ai',
    label: 'AI 助手',
    icon: 'ChatDotRound',
    action: 'ai-chat'
  },
  {
    key: 'my-review',
    group: 'tools',
    label: '今日复习',
    icon: 'RefreshRight',
    action: 'route',
    payload: { path: '/review/center', query: { category: 'TODAY' } }
  },
  {
    key: 'batch-export',
    group: 'tools',
    label: '批量导出',
    icon: 'Download',
    action: 'batch-export'
  },
  {
    key: 'focus-mode',
    group: 'tools',
    label: '专注模式',
    icon: 'Aim',
    action: 'focus-mode'
  },
  {
    key: 'toggle-theme',
    group: 'settings',
    label: '切换主题',
    icon: 'MoonNight',
    action: 'toggle-theme'
  },
  {
    key: 'toggle-sidebar',
    group: 'settings',
    label: '折叠侧栏',
    icon: 'Fold',
    action: 'toggle-sidebar'
  }
]
