/* ========================================
   DeepSeek Harness Java 实战教程 - 主控制器
   13章 · 翻页导航 · 主题切换 · 学习进度
   ======================================== */

const App = {
  currentChapter: null,
  chapters: [
    // 序章
    { id: 'ch00', num: 0, title: '教程导读：这本书怎么读', section: '🎬 序章', file: 'chapters/ch00-intro.html' },

    // 第一篇：快速上手
    { id: 'ch01', num: 1, title: '十分钟跑起来：构建、启动与第一次对话', section: '🚀 第一篇：快速上手', file: 'chapters/ch01-quickstart.html' },

    // 第二篇：架构设计
    { id: 'ch02', num: 2, title: '六边形架构：模块划分与依赖方向', section: '🏗️ 第二篇：架构设计', file: 'chapters/ch02-architecture.html' },
    { id: 'ch03', num: 3, title: '领域设计：事件风暴与 27 个限界上下文', section: '🏗️ 第二篇：架构设计', file: 'chapters/ch03-domain.html' },

    // 第三篇：核心机制
    { id: 'ch04', num: 4, title: 'ReactLoopAgent：ReAct 循环引擎', section: '🧠 第三篇：核心机制', file: 'chapters/ch04-react-loop.html' },
    { id: 'ch05', num: 5, title: '会话与事件溯源：SessionLog 的秘密', section: '🧠 第三篇：核心机制', file: 'chapters/ch05-session-event.html' },
    { id: 'ch06', num: 6, title: '意图识别与策略树：消息如何流转', section: '🧠 第三篇：核心机制', file: 'chapters/ch06-intent-strategy.html' },
    { id: 'ch07', num: 7, title: 'SSE 流式协议：meta/chunk/step_break/done', section: '🧠 第三篇：核心机制', file: 'chapters/ch07-sse-protocol.html' },

    // 第四篇：工具与扩展
    { id: 'ch08', num: 8, title: '工具系统：ToolCallExecutor 与 Hook 链', section: '🛠️ 第四篇：工具与扩展', file: 'chapters/ch08-tools.html' },
    { id: 'ch09', num: 9, title: '插件开发实战：Java 与 Node 双模式', section: '🛠️ 第四篇：工具与扩展', file: 'chapters/ch09-plugin.html' },
    { id: 'ch10', num: 10, title: 'MCP 集成：连接外部工具生态', section: '🛠️ 第四篇：工具与扩展', file: 'chapters/ch10-mcp.html' },

    // 第五篇：治理
    { id: 'ch11', num: 11, title: '任务、审批与治理：安全执行的边界', section: '🛡️ 第五篇：治理', file: 'chapters/ch11-governance.html' },

    // 第六篇：部署与生产化
    { id: 'ch12', num: 12, title: '部署与生产化：从本地到上线', section: '⚙️ 第六篇：部署与生产化', file: 'chapters/ch12-deployment.html' },

    // 终章
    { id: 'ch13', num: 13, title: '终章：已知边界与进阶路线', section: '🌟 终章', file: 'chapters/ch13-epilogue.html' }
  ],

  async init() {
    this.renderSidebar();
    this.bindEvents();
    this.loadTheme();
    Progress.updateUI();
    this.renderHome();

    const hash = location.hash.replace('#', '');
    if (hash && this.chapters.find(c => c.id === hash)) {
      this.loadChapter(hash);
    }
  },

  renderSidebar() {
    const nav = document.getElementById('sidebarNav');
    let html = '';
    let lastSection = null;
    this.chapters.forEach(ch => {
      if (ch.section !== lastSection) {
        html += `<div class="nav-section">${ch.section}</div>`;
        lastSection = ch.section;
      }
      html += `<a class="nav-item" data-chapter="${ch.id}" href="#${ch.id}">
        <span class="nav-num">${String(ch.num).padStart(2, '0')}</span>
        <span class="nav-title">${ch.title}</span>
      </a>`;
    });
    nav.innerHTML = html;
    nav.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', e => {
        e.preventDefault();
        this.loadChapter(item.dataset.chapter);
      });
    });
  },

  bindEvents() {
    document.getElementById('themeToggle').addEventListener('click', () => this.toggleTheme());
    document.getElementById('prevChapter').addEventListener('click', () => this.goPrev());
    document.getElementById('nextChapter').addEventListener('click', () => this.goNext());

    const backToTop = document.getElementById('backToTop');
    const contentBody = document.getElementById('contentBody');
    window.addEventListener('scroll', () => {
      backToTop.classList.toggle('visible', window.scrollY > 400);
    });
    backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

    window.addEventListener('hashchange', () => {
      const hash = location.hash.replace('#', '');
      if (hash && hash !== this.currentChapter) this.loadChapter(hash);
      if (!hash) this.renderHome();
    });
  },

  loadTheme() {
    const saved = localStorage.getItem('dsh-java-guide-theme');
    if (saved === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      document.getElementById('themeToggle').textContent = '☀️';
    }
  },

  toggleTheme() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) document.documentElement.removeAttribute('data-theme');
    else document.documentElement.setAttribute('data-theme', 'dark');
    localStorage.setItem('dsh-java-guide-theme', isDark ? 'light' : 'dark');
    document.getElementById('themeToggle').textContent = isDark ? '🌙' : '☀️';
  },

  goHome() {
    location.hash = '';
    this.renderHome();
  },

  renderHome() {
    this.currentChapter = null;
    document.getElementById('breadcrumb').textContent = '首页';
    document.getElementById('prevChapter').disabled = true;
    document.getElementById('nextChapter').disabled = false;
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    document.getElementById('tocNav').innerHTML = '<div style="padding:12px;color:var(--color-text-secondary);font-size:13px;">选择左侧章节开始学习</div>';

    const parts = {};
    this.chapters.forEach(ch => { (parts[ch.section] = parts[ch.section] || []).push(ch); });

    let cards = '';
    Object.entries(parts).forEach(([section, chs]) => {
      cards += `<div class="home-part"><h3 class="home-part-title">${section}</h3><div class="home-chapter-list">`;
      chs.forEach(ch => {
        const status = Progress.getStatus(ch.id);
        const badge = status === 'completed' ? '<span class="home-badge done">✓ 已学完</span>' :
                      status === 'learning' ? '<span class="home-badge learning">学习中</span>' : '';
        cards += `<a class="home-chapter-card" href="#${ch.id}">
          <div class="home-chapter-num">${String(ch.num).padStart(2, '0')}</div>
          <div class="home-chapter-info"><div class="home-chapter-title">${ch.title}</div></div>
          ${badge}
        </a>`;
      });
      cards += '</div></div>';
    });

    document.getElementById('contentBody').innerHTML = `
      <div class="home-hero">
        <div class="home-hero-tag">📖 实战教程</div>
        <h1 class="home-hero-title">DeepSeek Harness Java<br>从入门到精通</h1>
        <p class="home-hero-desc">13 章渐进式教程，带你从零跑起一个基于 DDD 六边形架构的 Agent Harness，<br>深入 ReAct 循环、事件溯源、策略树、插件体系与审批治理的每一行设计。</p>
        <div class="home-hero-actions">
          <a class="home-btn primary" href="#ch00">开始学习 →</a>
          <a class="home-btn" href="https://github.com/fuzhengwei/deepseek-harness-java" target="_blank">GitHub 仓库</a>
        </div>
        <div class="home-hero-stats">
          <div class="home-stat"><div class="home-stat-num">13</div><div class="home-stat-label">章节</div></div>
          <div class="home-stat"><div class="home-stat-num">6</div><div class="home-stat-label">篇幅</div></div>
          <div class="home-stat"><div class="home-stat-num">27</div><div class="home-stat-label">限界上下文</div></div>
          <div class="home-stat"><div class="home-stat-num">7</div><div class="home-stat-label">Maven 模块</div></div>
        </div>
      </div>
      ${cards}
      <div class="card" style="margin-top:24px;border-left:3px solid var(--color-accent);">
        <div class="card-title">💡 阅读建议</div>
        <p style="font-size:13px;color:var(--color-text-secondary);margin:0;">
          本教程循序渐进、承上启下：<strong>第一篇先跑起来</strong>获得直觉，<strong>第二篇俯瞰架构</strong>建立全局，
          <strong>第三篇深入核心机制</strong>理解原理，<strong>第四篇动手扩展</strong>插件与工具，<strong>第五六篇走向生产</strong>。
          每章开头有「本章导读」，结尾有「小结与下一章预告」，像看书一样按顺序读体验最佳。
        </p>
      </div>`;
  },

  async loadChapter(chapterId) {
    const ch = this.chapters.find(c => c.id === chapterId);
    if (!ch) return;
    this.currentChapter = chapterId;
    location.hash = chapterId;
    Progress.markLearning(chapterId);

    document.querySelectorAll('.nav-item').forEach(i => {
      i.classList.toggle('active', i.dataset.chapter === chapterId);
    });

    const contentBody = document.getElementById('contentBody');
    contentBody.innerHTML = '<div class="loading">📖 加载章节中...</div>';
    window.scrollTo({ top: 0 });

    try {
      const resp = await fetch(ch.file);
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const html = await resp.text();
      contentBody.innerHTML = html;
      this.afterRender(ch);
    } catch (e) {
      contentBody.innerHTML = `<div class="card"><div class="card-title">⚠️ 加载失败</div><p>${ch.file}: ${e.message}</p></div>`;
    }
  },

  afterRender(ch) {
    // 面包屑与翻页按钮
    document.getElementById('breadcrumb').textContent = `${ch.section} · 第${ch.num}章 ${ch.title}`;
    const idx = this.chapters.findIndex(c => c.id === ch.id);
    document.getElementById('prevChapter').disabled = idx <= 0;
    document.getElementById('nextChapter').disabled = idx >= this.chapters.length - 1;

    // 代码高亮
    if (window.Prism) Prism.highlightAll();

    // 流程图
    if (window.FlowChart) FlowChart.init();

    // 生成本章目录 TOC
    this.renderTOC();

    // 章节底部「标记完成」按钮
    const contentBody = document.getElementById('contentBody');
    const footer = document.createElement('div');
    footer.className = 'chapter-footer';
    footer.innerHTML = `<button class="home-btn primary" id="markDoneBtn">✓ 标记本章已学完</button>`;
    contentBody.appendChild(footer);
    footer.querySelector('#markDoneBtn').addEventListener('click', () => {
      Progress.markCompleted(ch.id);
      footer.innerHTML = '<div style="color:var(--color-success);font-size:14px;">✓ 本章已完成，点击右上角「下一章」继续</div>';
    });
  },

  renderTOC() {
    const tocNav = document.getElementById('tocNav');
    const headings = document.querySelectorAll('#contentBody h2.section-heading, #contentBody h1.chapter-title');
    if (!headings.length) {
      tocNav.innerHTML = '<div style="padding:12px;color:var(--color-text-secondary);font-size:13px;">本章无小节</div>';
      return;
    }
    let html = '';
    headings.forEach((h, i) => {
      if (!h.id) h.id = 'toc-h-' + i;
      const isTitle = h.classList.contains('chapter-title');
      html += `<a class="toc-item${isTitle ? ' toc-title' : ''}" href="javascript:void(0)" data-target="${h.id}">${h.textContent}</a>`;
    });
    tocNav.innerHTML = html;
    tocNav.querySelectorAll('.toc-item').forEach(item => {
      item.addEventListener('click', () => {
        const el = document.getElementById(item.dataset.target);
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    });
  },

  goPrev() {
    const idx = this.chapters.findIndex(c => c.id === this.currentChapter);
    if (idx > 0) this.loadChapter(this.chapters[idx - 1].id);
    else this.goHome();
  },

  goNext() {
    if (!this.currentChapter) { this.loadChapter('ch00'); return; }
    const idx = this.chapters.findIndex(c => c.id === this.currentChapter);
    if (idx < this.chapters.length - 1) this.loadChapter(this.chapters[idx + 1].id);
  }
};

window.App = App;
document.addEventListener('DOMContentLoaded', () => App.init());
