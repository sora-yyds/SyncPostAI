import { definePlugin } from '@halo-dev/console-shared'
import HomeView from './views/HomeView.vue'
import { markRaw } from 'vue'
import RiRobot2Line from '~icons/ri/robot-2-line'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/syncpostai',
        name: 'SyncPostAI',
        component: HomeView,
        meta: {
          title: '智稿同步（SyncPostAI）',
          searchable: true,
          menu: {
            name: '智稿同步',
            group: 'tool',
            icon: markRaw(RiRobot2Line),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
