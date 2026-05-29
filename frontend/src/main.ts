import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import { routes } from './routes'
import { consumeExtensionAuthFromLocation } from './services/extensionAuth'
import './styles.css'

consumeExtensionAuthFromLocation()

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

createApp(App).use(router).mount('#app')
