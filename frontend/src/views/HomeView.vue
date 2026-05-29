<script setup lang="ts">
import { ArrowRight, Download, FileSearch, Layers3, PlugZap, SquareCheckBig } from 'lucide-vue-next'

const extensionUrl = '/downloads/aioi-oj-import-extension.zip'

const features = [
  {
    icon: FileSearch,
    title: '题面分析',
    text: '粘贴题面或批量上传文件，由 AI 识别难度、标签和训练提示。'
  },
  {
    icon: Layers3,
    title: '题库与题单',
    text: '集中管理题目、题单和通过记录，适合教学选题和阶段训练。'
  },
  {
    icon: PlugZap,
    title: 'OJ 同步',
    text: '浏览器插件可从 Codeforces、AtCoder、洛谷、牛客导入题目和提交状态。'
  }
]
</script>

<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="home-hero__content">
        <p class="home-kicker">Teaching Problem Workspace</p>
        <h1>AIOIProblem</h1>
        <p class="home-subtitle">面向信息学竞赛教学的选题与训练工作台</p>
        <p class="home-copy">
          AIOIProblem 用题面分析、题库整理、题单编排和通过记录，把日常训练中的零散题目沉淀成可复用的教学资产。
        </p>
        <div class="home-actions">
          <a class="home-button primary" href="#/login">
            进入工作台
            <ArrowRight :size="18" />
          </a>
          <a class="home-button" :href="extensionUrl" download>
            下载浏览器插件
            <Download :size="18" />
          </a>
        </div>
      </div>

      <div class="home-board" aria-label="项目能力概览">
        <div class="board-topline">
          <span>OJ Import</span>
          <strong>AI Queue</strong>
        </div>
        <div class="board-flow">
          <span>提交记录</span>
          <ArrowRight :size="18" />
          <span>题面抓取</span>
          <ArrowRight :size="18" />
          <span>题库沉淀</span>
        </div>
        <div class="board-result">
          <SquareCheckBig :size="22" />
          <div>
            <strong>Accepted 自动标记通过</strong>
            <small>同源题号去重，空题与不可访问题跳过</small>
          </div>
        </div>
      </div>
    </section>

    <section class="home-features" aria-label="项目介绍">
      <article v-for="feature in features" :key="feature.title">
        <component :is="feature.icon" :size="24" />
        <h2>{{ feature.title }}</h2>
        <p>{{ feature.text }}</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(23, 104, 79, 0.12), transparent 36%),
    linear-gradient(315deg, rgba(55, 101, 143, 0.13), transparent 34%),
    var(--bg);
}

.home-hero {
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.75fr);
  gap: 28px;
  align-items: center;
  max-width: 1160px;
  margin: 0 auto;
}

.home-hero__content {
  display: grid;
  gap: 18px;
}

.home-kicker {
  width: fit-content;
  margin: 0;
  padding: 7px 10px;
  border: 1px solid rgba(23, 104, 79, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.home-hero h1 {
  margin: 0;
  font-size: 76px;
  line-height: 0.95;
  letter-spacing: 0;
}

.home-subtitle {
  margin: 0;
  color: #234338;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.25;
}

.home-copy {
  max-width: 720px;
  margin: 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.8;
}

.home-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
}

.home-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: var(--surface);
  color: var(--ink);
  font-weight: 800;
  text-decoration: none;
  box-shadow: var(--shadow-sm);
}

.home-button.primary {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

.home-board {
  display: grid;
  gap: 18px;
  padding: 22px;
  border: 1px solid rgba(23, 104, 79, 0.18);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.72)),
    var(--surface);
  box-shadow: var(--shadow-lg);
}

.board-topline,
.board-flow,
.board-result {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(249, 250, 247, 0.9);
}

.board-topline {
  display: flex;
  justify-content: space-between;
  padding: 14px;
  color: var(--muted);
}

.board-flow {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr;
  gap: 8px;
  align-items: center;
  padding: 14px;
  color: #284138;
  font-weight: 800;
  text-align: center;
}

.board-result {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 15px;
  background: #f4f7fb;
  color: var(--blue);
}

.board-result strong,
.board-result small {
  display: block;
}

.board-result small {
  margin-top: 3px;
  color: var(--muted);
}

.home-features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  max-width: 1160px;
  margin: -42px auto 0;
}

.home-features article {
  min-height: 170px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.home-features svg {
  color: var(--primary);
}

.home-features h2 {
  margin: 18px 0 8px;
  font-size: 20px;
}

.home-features p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
}

@media (max-width: 900px) {
  .home-hero {
    min-height: auto;
    grid-template-columns: 1fr;
    padding: 34px 0 90px;
  }

  .home-hero h1 {
    font-size: 48px;
  }

  .home-subtitle {
    font-size: 22px;
  }

  .home-features {
    grid-template-columns: 1fr;
    margin-top: -54px;
  }
}
</style>
