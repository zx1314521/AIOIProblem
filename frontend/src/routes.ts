import type { RouteRecordRaw } from 'vue-router'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import AnalysisView from './views/AnalysisView.vue'
import AnalysisHistoryView from './views/AnalysisHistoryView.vue'
import ProblemsView from './views/ProblemsView.vue'
import ProblemSetsView from './views/ProblemSetsView.vue'
import RecommendationsView from './views/RecommendationsView.vue'
import PassedView from './views/PassedView.vue'
import SettingsView from './views/SettingsView.vue'
import OjImportHistoryView from './views/OjImportHistoryView.vue'
import { authState } from './services/auth'

export const routes: RouteRecordRaw[] = [
  { path: '/', component: HomeView },
  { path: '/login', component: LoginView },
  { path: '/analysis', component: AnalysisView },
  { path: '/history', component: AnalysisHistoryView },
  { path: '/oj-imports', component: OjImportHistoryView },
  { path: '/problems', component: ProblemsView },
  { path: '/sets', component: ProblemSetsView },
  { path: '/batch', redirect: '/analysis' },
  { path: '/recommendations', component: RecommendationsView },
  { path: '/passed', component: PassedView },
  { path: '/settings', component: SettingsView },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export function requireAuth(path: string) {
  if (!authState.token && path !== '/login' && path !== '/') {
    return '/login'
  }
  return path
}
