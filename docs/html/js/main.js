/* ========================================
   DeepSeek Harness Java 实战教程 - 主控制器
   13章 · 翻页导航 · 主题切换 · 学习进度
   ======================================== */

const App = {
  currentChapter: null,
  staticMode: false,
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
    this.initHomeEnhancements();

    // 静态章节页模式（build.py 生成的独立 URL 页）：
    // 正文已内联，SPA 只做增强——高亮/流程图/进度，不再接管路由
    const staticChapter = document.body.dataset.chapter;
    if (staticChapter && this.chapters.find(c => c.id === staticChapter)) {
      this.currentChapter = staticChapter;
      this.enhanceStaticPage(staticChapter);
      return;
    }

    // 静态首页（build.py 生成，含整体框架结构/运行时旅程版块）：
    // 不接管渲染（SPA renderHome 会覆盖静态内容），仅做增强；hash 则跳转真实章节页
    if (document.querySelector('.home-hero')) {
      const h = location.hash.replace('#', '');
      const target = h && document.querySelector(`.nav-item[data-chapter="${h}"]`);
      const href = target && target.getAttribute('href');
      if (href && !href.startsWith('#')) location.href = href;
      return;
    }

    this.renderHome();

    const hash = location.hash.replace('#', '');
    if (hash && this.chapters.find(c => c.id === hash)) {
      this.loadChapter(hash);
    }
  },

  // 首页增强：滚动渐入动效 + 整体框架结构交互（悬停/点击联动详情面板）
  initHomeEnhancements() {
    const body = document.getElementById('contentBody');
    if (!body) return;

    // 1) 滚动渐入：JS 动态加类，禁用 JS 时内容照常可见（SEO 友好）
    const revealEls = [];
    body.querySelectorAll('.arch-section, .journey-section, .home-part, .home-tip').forEach(el => {
      el.classList.add('reveal');
      revealEls.push(el);
    });
    body.querySelectorAll('.home-chapter-list').forEach(el => {
      el.classList.add('reveal-stagger');
      revealEls.push(el);
    });
    if ('IntersectionObserver' in window) {
      const io = new IntersectionObserver(entries => {
        entries.forEach(en => {
          if (en.isIntersecting) {
            en.target.classList.add('in');
            io.unobserve(en.target);
          }
        });
      }, { threshold: 0.06, rootMargin: '0px 0px -4% 0px' });
      revealEls.forEach(el => io.observe(el));
    } else {
      revealEls.forEach(el => el.classList.add('in'));
    }

    // 2) 架构图交互：悬停/点击左侧模块 → 右侧详情面板切换；详情内按钮跳章节
    const map = body.querySelector('.arch-map');
    const detail = document.getElementById('archDetail');
    if (!map || !detail) return;
    const layers = Array.from(map.querySelectorAll('.arch-layer'));
    const findTemplate = mid =>
      document.querySelector(`template[data-arch-detail="${mid}"]`);

    const showDetail = layer => {
      layers.forEach(l => l.classList.toggle('selected', l === layer));
      const tpl = findTemplate(layer.dataset.module);
      if (!tpl) return;
      detail.innerHTML = '';
      detail.appendChild(tpl.content.cloneNode(true));
      detail.classList.add('active');
      const color = getComputedStyle(layer).getPropertyValue('--layer-color').trim();
      if (color) detail.style.setProperty('--detail-color', color);
    };

    layers.forEach(layer => {
      layer.addEventListener('mouseenter', () => showDetail(layer));
      layer.addEventListener('click', () => showDetail(layer));
      layer.addEventListener('keydown', e => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          showDetail(layer);
          const link = detail.querySelector('.arch-detail-link');
          if (link) link.focus();
        }
      });
    });
    // 默认展示 Domain 核心模块，首屏即有内容
    const domainLayer = layers.find(l => l.classList.contains('core')) || layers[0];
    if (domainLayer) showDetail(domainLayer);
  },

  // 静态章节页增强：代码高亮 + 流程图 + 进度标记 + 已学完按钮
  enhanceStaticPage(chapterId) {
    this.staticMode = true;
    const ch = this.chapters.find(c => c.id === chapterId);
    const body = document.getElementById('contentBody');
    if (!ch || !body) return;

    Progress.markLearning(chapterId);
    if (window.Syntax) Syntax.highlightAll();
    if (window.FlowChart) FlowChart.init();
    Progress.updateUI();

    // 标题锚点 id（TOC 跳转用）
    const headings = body.querySelectorAll('h2.section-heading, h1.chapter-title');
    headings.forEach((h, i) => { if (!h.id) h.id = 'sec-' + (i + 1); });

    // 已学完按钮（追加到正文末）
    const footer = document.createElement('div');
    footer.className = 'chapter-footer';
    footer.innerHTML = `<button class="home-btn primary" id="markDoneBtn">✓ 标记本章已学完</button>`;
    body.appendChild(footer);
    footer.querySelector('#markDoneBtn').addEventListener('click', () => {
      Progress.markCompleted(chapterId);
      footer.innerHTML = '<div style="color:var(--color-success);font-size:14px;">✓ 本章已完成，右上角「下一章」继续</div>';
    });

    // 首页徽章占位回填（静态首页的 data-badge 占位符）
    document.querySelectorAll('.home-badge-slot').forEach(slot => {
      const status = Progress.getStatus(slot.dataset.badge);
      if (status === 'completed') slot.innerHTML = '<span class="home-badge done">✓ 已学完</span>';
      else if (status === 'learning') slot.innerHTML = '<span class="home-badge learning">学习中</span>';
    });
  },

  renderSidebar() {
    const nav = document.getElementById('sidebarNav');
    // 静态页（build.py 已渲染真实 .html 链接）：直接保留服务端侧栏，
    // 不再接管为 SPA hash 路由——点击即整页跳转，file:// 打开也可用
    if (nav.querySelector('.nav-item[href$=".html"]')) return;
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
    document.getElementById('themeToggle')?.addEventListener('click', () => this.toggleTheme());
    // SPA 翻页按钮仅存在于 index.html 壳；静态章节页用真实链接，元素可能不存在
    document.getElementById('prevChapter')?.addEventListener('click', () => this.goPrev());
    document.getElementById('nextChapter')?.addEventListener('click', () => this.goNext());

    const backToTop = document.getElementById('backToTop');
    backToTop?.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
    const contentBody = document.getElementById('contentBody');
    if (contentBody) {
      window.addEventListener('scroll', () => {
        backToTop?.classList.toggle('visible', window.scrollY > 400);
      });
    }

    window.addEventListener('hashchange', () => {
      if (this.staticMode) return; // 静态页不接管路由
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
        <div class="home-hero-glow glow-a"></div>
        <div class="home-hero-glow glow-b"></div>
        <div class="home-hero-inner">
          <div class="home-hero-eyebrow"><span class="tag-dot"></span>实战教程 · 基于真实开源项目</div>
          <h1 class="home-hero-title">
            把 Agent Harness<br>读得<span class="title-em">明明白白</span>
          </h1>
          <p class="home-hero-desc">13 章渐进式教程，基于开源项目 <strong>deepseek-harness-java</strong>，<br>从跑起来到读懂 ReAct 循环、事件溯源、策略树、插件体系与审批治理。</p>
          <div class="home-hero-actions">
            <a class="home-btn primary" href="#ch00">开始学习<span class="btn-arrow">→</span></a>
            <a class="home-btn ghost" href="https://github.com/fuzhengwei/deepseek-harness-java" target="_blank">GitHub 仓库</a>
          </div>
          <div class="home-hero-stats">
            <div class="home-stat"><div class="home-stat-num">13</div><div class="home-stat-label">章节</div></div>
            <div class="home-stat-divider"></div>
            <div class="home-stat"><div class="home-stat-num">6</div><div class="home-stat-label">篇幅</div></div>
            <div class="home-stat-divider"></div>
            <div class="home-stat"><div class="home-stat-num">27</div><div class="home-stat-label">限界上下文</div></div>
            <div class="home-stat-divider"></div>
            <div class="home-stat"><div class="home-stat-num">7</div><div class="home-stat-label">Maven 模块</div></div>
          </div>
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

    // 代码高亮（自研引擎）
    if (window.Syntax) Syntax.highlightAll();

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
