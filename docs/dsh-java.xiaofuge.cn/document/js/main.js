/* ========================================
   DeepSeek Harness Java 实战教程 - 主控制器
   17章 · 翻页导航 · 主题切换 · 学习进度
   ======================================== */

const App = {
  currentChapter: null,
  staticMode: false,
  chapters: [
    // 序章
    { id: 'ch00', num: 0, title: '教程导读：这本书怎么读', section: '🎬 序章', file: 'ch00-intro.html' },

    // 第一篇：快速上手
    { id: 'ch01', num: 1, title: '十分钟跑起来：构建、启动与第一次对话', section: '🚀 第一篇：快速上手', file: 'ch01-quickstart.html' },

    // 第二篇：架构设计
    { id: 'ch02', num: 2, title: '六边形架构：模块划分与依赖方向', section: '🏗️ 第二篇：架构设计', file: 'ch02-architecture.html' },
    { id: 'ch03', num: 3, title: '领域设计：事件风暴与 27 个限界上下文', section: '🏗️ 第二篇：架构设计', file: 'ch03-domain.html' },

    // 第三篇：核心机制
    { id: 'ch04', num: 4, title: 'ReactLoopAgent：ReAct 循环引擎', section: '🧠 第三篇：核心机制', file: 'ch04-react-loop.html' },
    { id: 'ch05', num: 5, title: '会话与事件溯源：SessionLog 的秘密', section: '🧠 第三篇：核心机制', file: 'ch05-session-event.html' },
    { id: 'ch06', num: 6, title: '意图识别与策略树：消息如何流转', section: '🧠 第三篇：核心机制', file: 'ch06-intent-strategy.html' },
    { id: 'ch07', num: 7, title: 'SSE 流式协议：推理、工具卡片与会话终态', section: '🧠 第三篇：核心机制', file: 'ch07-sse-protocol.html' },

    // 第四篇：工具与扩展
    { id: 'ch08', num: 8, title: '工具系统：ToolCallExecutor 与 Hook 链', section: '🛠️ 第四篇：工具与扩展', file: 'ch08-tools.html' },
    { id: 'ch09', num: 9, title: '插件系统设计：从 ClassLoader 到对话执行', section: '🛠️ 第四篇：工具与扩展', file: 'ch09-plugin.html' },
    { id: 'ch10', num: 10, title: 'MCP 集成：连接外部工具生态', section: '🛠️ 第四篇：工具与扩展', file: 'ch10-mcp.html' },

    // 第五篇：治理
    { id: 'ch11', num: 11, title: '任务、审批与治理：安全执行的边界', section: '🛡️ 第五篇：治理', file: 'ch11-governance.html' },

    // 第六篇：部署与生产化
    { id: 'ch12', num: 12, title: '部署与生产化：从本地到上线', section: '⚙️ 第六篇：部署与生产化', file: 'ch12-deployment.html' },

    { id: 'ch13', num: 13, title: '边界与进阶路线：从 Demo 到产品', section: '⚙️ 第六篇：部署与生产化', file: 'ch13-epilogue.html' },

    // 工程全景
    { id: 'ch14', num: 14, title: '工程全景：API、控制台、数据与测试', section: '🧰 工程全景', file: 'ch14-engineering.html' },
    { id: 'ch15', num: 15, title: '工作流与子代理：从单 Agent 到协同执行', section: '🧰 工程全景', file: 'ch15-workflow-subagent.html' },
    { id: 'ch16', num: 16, title: '目标、计划、技能与定时任务：长程上下文治理', section: '🧰 工程全景', file: 'ch16-platform-capabilities.html' },

    // 版本演进
    { id: 'ch17', num: 17, title: '版本演进：v0.1.3 到 v0.1.5 交付实录', section: '📦 版本演进', file: 'ch17-release-notes.html' }
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

  // 静态章节页增强：代码高亮 + 流程图 + 进度标记 + 已学完按钮 + 八股/面试标签
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

    // 八股 / 面试 标签系统：识别 summary-box-bagu 和 #quizArea 后插入标签条
    this.installChapterTabs(body);

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

    // 右侧「本章目录」滚动高亮 + 平滑跳转
    this.initTocScrollspy();
  },

  /**
   * 章节标签页（独立可点击的 Tab）
   * - 三个标签：📖 文章 · 📋 八股 · 📝 面试
   * - 每个标签相互独立，点哪个显示哪个 section
   * - 默认显示「文章」；点击切换显示/隐藏
   * - 八股区：通过 .summary-box-bagu 识别（div 含 class summary-box-bagu）
   * - 面试区：通过 #quizArea 容器识别（其后紧跟 <script> 提供题库）
   */
  installChapterTabs(body) {
    // 1) 找到八股区（含 summary-box-bagu 类），并把整块打上 data-tab-section="bagu"
    const baguBox = body.querySelector('.summary-box-bagu');
    if (baguBox) baguBox.setAttribute('data-tab-section', 'bagu');

    // 2) 找到面试区（#quizArea 容器），并把它和其后直系兄弟 <script> 都打 bagu/quiz
    const quizArea = body.querySelector('#quizArea');
    if (quizArea) {
      quizArea.setAttribute('data-tab-section', 'quiz');
      // 紧随其后的 <script> 标签属于面试区（提供题库），也打 quiz
      let n = quizArea.nextElementSibling;
      while (n && n.tagName === 'SCRIPT') {
        n.setAttribute('data-tab-section', 'quiz');
        n = n.nextElementSibling;
      }
    }

    // 3) 找到文章区：直接子元素中除上述以外的所有顶级 section 都视为 article
    //    兼容：通过包装容器（如果存在）标整体，否则逐个打标
    const topChildren = Array.from(body.children);
    let baguCount = baguBox ? baguBox.querySelectorAll('.qa-item').length : 0;
    const hasArticle = topChildren.some(c => !c.hasAttribute('data-tab-section'));
    const hasBagu = !!baguBox;
    const hasQuiz = !!quizArea;

    if (!hasBagu && !hasQuiz) return;  // 两块都没有就不出标签条

    // 给"文章"区域子元素打标（只给顶层直系子元素打）
    topChildren.forEach(child => {
      if (!child.hasAttribute('data-tab-section')) {
        child.setAttribute('data-tab-section', 'article');
      }
    });

    // 4) 构造标签条
    const nav = document.createElement('div');
    nav.className = 'chapter-tabs-nav';
    nav.setAttribute('role', 'tablist');
    nav.setAttribute('aria-label', '章节内容切换');

    const tabDefs = [];
    if (hasArticle) tabDefs.push({ id: 'article', icon: '📖', label: '文章', active: true });
    if (hasBagu)    tabDefs.push({ id: 'bagu',    icon: '📋', label: '八股', badge: baguCount });
    if (hasQuiz)    tabDefs.push({ id: 'quiz',    icon: '📝', label: '面试' });

    // 标签切换函数
    const switchTab = (tabId) => {
      nav.querySelectorAll('.chapter-tab-btn').forEach(b =>
        b.classList.toggle('active', b.dataset.tab === tabId)
      );
      // 切换显示：以 chapter-tabs-nav（自身）外的顶级子元素为准
      Array.from(body.children).forEach(child => {
        if (child === nav) return;
        const sec = child.getAttribute('data-tab-section') || 'article';
        child.classList.toggle('tab-hidden', sec !== tabId);
      });
      // 切到 quiz 时如果还没渲染，主动跑一下 setTimeout 里的 render 调用
      if (tabId === 'quiz' && quizArea && !quizArea.querySelector('.quiz-section')) {
        // 重新触发：克隆并重新执行紧跟其后的 script 即可
        const scripts = [];
        let n = quizArea.nextElementSibling;
        while (n && n.tagName === 'SCRIPT') { scripts.push(n); n = n.nextElementSibling; }
        if (scripts.length) {
          scripts.forEach(old => {
            const fresh = document.createElement('script');
            Array.from(old.attributes).forEach(attr => fresh.setAttribute(attr.name, attr.value));
            fresh.textContent = old.textContent;
            old.parentNode.replaceChild(fresh, old);
          });
        }
      }
      // 吸顶样式刷新
      requestAnimationFrame(() => {
        const rect = nav.getBoundingClientRect();
        const headerRect = document.querySelector('.content-header')?.getBoundingClientRect();
        const bottom = headerRect ? headerRect.bottom : 60;
        nav.classList.toggle('is-stuck', Math.abs(rect.top - bottom) < 2);
      });
    };

    tabDefs.forEach(def => {
      const btn = document.createElement('button');
      btn.className = 'chapter-tab-btn' + (def.active ? ' active' : '');
      btn.setAttribute('role', 'tab');
      btn.setAttribute('data-tab', def.id);
      btn.type = 'button';
      btn.innerHTML = `<span class="tab-icon">${def.icon}</span>` +
                      `<span>${def.label}</span>` +
                      (def.badge != null ? `<span class="tab-badge">${def.badge}</span>` : '');
      btn.addEventListener('click', e => {
        e.preventDefault();
        switchTab(def.id);
      });
      nav.appendChild(btn);
    });

    // 5) 插入标签条到 body 开头（在第一个 article 元素之前）
    body.insertBefore(nav, body.firstChild);

    // 6) 初始化：激活默认标签，隐藏其它内容
    const activeTab = (tabDefs.find(t => t.active) || tabDefs[0]).id;
    Array.from(body.children).forEach(child => {
      if (child === nav) return;
      const sec = child.getAttribute('data-tab-section') || 'article';
      child.classList.toggle('tab-hidden', sec !== activeTab);
    });

    // 7) 滚动时切换吸顶样式（取自 sticky top，可视需求再加）
    const onScroll = () => {
      const rect = nav.getBoundingClientRect();
      const headerRect = document.querySelector('.content-header')?.getBoundingClientRect();
      const bottom = headerRect ? headerRect.bottom : 60;
      nav.classList.toggle('is-stuck', Math.abs(rect.top - bottom) < 2);
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  },

  renderSidebar() {
    const nav = document.getElementById('sidebarNav');
    // 静态页（build.py 已渲染真实 .html 链接）：直接保留服务端侧栏，
    // 不再接管为 SPA hash 路由——点击即整页跳转，file:// 打开也可用
    if (nav.querySelector('.nav-item[href$=".html"]')) return;
    let html = '';
    let lastSection = null;
    const splitSection = s => {
      const m = s.match(/^([\u{1F000}-\u{1FAFF}\u2600-\u27BF\uFE0F\u200D]+)\s*/u);
      return m ? [m[1], s.slice(m[0].length)] : ['', s];
    };
    const isStaticPage = !!document.body.dataset.chapter;
    this.chapters.forEach(ch => {
      if (ch.section !== lastSection) {
        if (lastSection !== null) html += '</div>';
        const [emoji, text] = splitSection(ch.section);
        html += `<div class="nav-group" style="--part:var(--color-accent)">`
              + `<div class="nav-section"><span class="nav-section-dot"></span>`
              + `<span class="nav-section-emoji">${emoji}</span>`
              + `<span class="nav-section-title">${text}</span>`
              + `<span class="nav-section-count">${String(ch.num).padStart(2, '0')}</span></div>`;
        lastSection = ch.section;
      }
      const href = isStaticPage ? ch.file : `#${ch.id}`;
      html += `<a class="nav-item" data-chapter="${ch.id}" href="${href}">
        <span class="nav-num">${String(ch.num).padStart(2, '0')}</span>
        <span class="nav-title">${ch.title}</span>
      </a>`;
    });
    nav.innerHTML = html + (lastSection !== null ? '</div>' : '');
    if (isStaticPage) {
      nav.querySelector(`.nav-item[data-chapter="${this.currentChapter || document.body.dataset.chapter}"]`)?.classList.add('active');
      return;
    }
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
          <p class="home-hero-desc">17 章渐进式教程，基于开源项目 <strong>deepseek-harness-java</strong>，<br>从跑起来到读懂 ReAct 循环、事件溯源、策略树、插件体系、审批治理与平台能力。</p>
          <div class="home-hero-actions">
            <a class="home-btn primary" href="#ch00">开始学习<span class="btn-arrow">→</span></a>
            <a class="home-btn ghost" href="https://gitcode.net/KnowledgePlanet/deepseek-harness-java" target="_blank" rel="noopener">查看源码</a>
          </div>
          <div class="home-hero-stats">
            <div class="home-stat"><div class="home-stat-num">17</div><div class="home-stat-label">章节</div></div>
            <div class="home-stat-divider"></div>
            <div class="home-stat"><div class="home-stat-num">8</div><div class="home-stat-label">篇幅</div></div>
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

    // 八股 / 面试 标签
    const contentBody = document.getElementById('contentBody');
    this.installChapterTabs(contentBody);

    // 章节底部「标记完成」按钮
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
      tocNav.innerHTML = '<div class="toc-empty"><span class="toc-empty-icon">📭</span>本章无小节</div>';
      return;
    }
    let html = '';
    headings.forEach((h, i) => {
      if (!h.id) h.id = 'toc-h-' + i;
      const isTitle = h.classList.contains('chapter-title');
      const label = String(i + 1).padStart(2, '0');
      html += `<a class="toc-item${isTitle ? ' toc-title' : ''}" href="javascript:void(0)" data-target="${h.id}">`
            + `<span class="toc-num">${label}</span><span class="toc-text">${h.textContent}</span></a>`;
    });
    tocNav.innerHTML = html;
    this.initTocScrollspy();
  },

  // 右侧「本章目录」滚动高亮：父容器 .content-area 内滚动，用 IntersectionObserver 定位当前小节
  initTocScrollspy() {
    const tocNav = document.getElementById('tocNav');
    const contentArea = document.querySelector('.content-area');
    if (!tocNav || !contentArea) return;
    const items = Array.from(tocNav.querySelectorAll('.toc-item[data-target]'));
    const pairs = [];
    items.forEach(it => {
      const el = document.getElementById(it.dataset.target);
      if (el) pairs.push({ el, it });
    });
    if (!pairs.length) return;

    // 点击 → 平滑滚动到对应小节（父容器是滚动容器）
    items.forEach(it => {
      if (it.dataset.bound) return;
      it.dataset.bound = '1';
      it.addEventListener('click', e => {
        e.preventDefault();
        const el = document.getElementById(it.dataset.target);
        if (el) scrollToHeading(el);
      });
    });

    const setActive = id => items.forEach(it => it.classList.toggle('active', it.dataset.target === id));
    setActive(pairs[0].it.dataset.target);

    if (!('IntersectionObserver' in window)) return;
    const io = new IntersectionObserver(entries => {
      const visible = entries.filter(e => e.isIntersecting);
      if (visible.length) {
        visible.sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        setActive(visible[0].target.id);
      }
    }, { root: contentArea, rootMargin: '-12% 0px -68% 0px', threshold: 0 });
    pairs.forEach(p => io.observe(p.el));
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

// 在父滚动容器 .content-area 内把元素平滑滚到顶部（留出内容头部高度）
function scrollToHeading(el) {
  const contentArea = document.querySelector('.content-area');
  if (!contentArea) { el.scrollIntoView({ behavior: 'smooth', block: 'start' }); return; }
  const target = el.getBoundingClientRect().top - contentArea.getBoundingClientRect().top + contentArea.scrollTop - 78;
  contentArea.scrollTo({ top: target, behavior: 'smooth' });
}

window.App = App;
document.addEventListener('DOMContentLoaded', () => App.init());
