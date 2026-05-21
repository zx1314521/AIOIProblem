import type { RouteRecordRaw } from 'vue-router'
import LoginView from './views/LoginView.vue'
import AnalysisView from './views/AnalysisView.vue'
import ProblemsView from './views/ProblemsView.vue'
import ProblemSetsView from './views/ProblemSetsView.vue'
import RecommendationsView from './views/RecommendationsView.vue'
import PassedView from './views/PassedView.vue'
import { authState } from './services/auth'

export const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/analysis' },
  { path: '/login', component: LoginView },
  { path: '/analysis', component: AnalysisView },
  { path: '/problems', component: ProblemsView },
  { path: '/sets', component: ProblemSetsView },
  { path: '/recommendations', component: RecommendationsView },
  { path: '/passed', component: PassedView }
]

export function requireAuth(path: string) {
  if (!authState.token && path !== '/login') {
    return '/login'
  }
  return path
}

