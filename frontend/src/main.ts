import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from '@/App.vue'
import { installPermissionDirective } from '@/directives/permission'
import router from '@/router'
import { useSettingsStore } from '@/stores/settings'
import '@/styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
installPermissionDirective(app)

const settings = useSettingsStore()
settings.applyTheme()

app.use(router)
app.mount('#app')
