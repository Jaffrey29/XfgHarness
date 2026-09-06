#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
教程站构建器
- 从 js/main.js 提取章节注册表（单一数据源）
- chapters/*.html 片段 → 根目录独立静态页（正文内联，搜索引擎可直读）
- 生成静态 index.html、sitemap.xml、robots.txt
- 幂等：可重复运行；不修改 chapters/ 片段源文件
用法：python3 build.py
"""
import html as htmllib
import re
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CHAPTERS_DIR = ROOT / 'chapters'
# 部署域名（百度收录需要绝对 URL；换域名只改这一行）
SITE_URL = 'https://fuzhengwei.github.io/deepseek-harness-java/docs/html'
SITE_TITLE = 'DeepSeek Harness Java 实战教程'

# 阅读/滚动进度条脚本（普通字符串：JS 花括号不能进 f-string）
SCROLL_PROGRESS_JS = """  <script>
    window.addEventListener('scroll', () => {
      const h = document.documentElement;
      const pct = h.scrollTop / (h.scrollHeight - h.clientHeight) * 100;
      const bar = document.getElementById('readingProgress');
      if (bar) bar.style.width = (pct || 0) + '%';
    });
  </script>
"""

def load_registry():
    """从 main.js 提取章节注册表（单一数据源）"""
    src = (ROOT / 'js' / 'main.js').read_text(encoding='utf-8')
    pat = re.compile(
        r"\{\s*id:\s*'([^']+)'\s*,\s*num:\s*(\d+)\s*,\s*title:\s*'([^']+)'\s*,"
        r"\s*section:\s*'([^']+)'\s*,\s*file:\s*'chapters/([^']+)'\s*,?\s*\}")
    out = []
    for m in pat.finditer(src):
        out.append(dict(id=m.group(1), num=int(m.group(2)), title=m.group(3),
                        section=m.group(4), frag=m.group(5)))
    if len(out) < 10:
        sys.exit(f'[build] 注册表提取异常：仅 {len(out)} 章，检查 main.js 格式')
    return out

def strip_tags(s):
    return re.sub(r'<[^>]+>', '', s)

def page_desc(frag_html):
    """取片段 subtitle 或第一个 <p> 的纯文本作 description（去内联标签，截 118 字）"""
    m = re.search(r'<p class="chapter-subtitle"[^>]*>([\s\S]*?)</p>', frag_html)
    if not m:
        m = re.search(r'<p[^>]*>([\s\S]*?)</p>', frag_html)
    if not m:
        return SITE_TITLE
    txt = re.sub(r'<[^>]+>', '', m.group(1))
    txt = htmllib.unescape(re.sub(r'\s+', ' ', txt)).strip()
    return (txt[:115] + '…') if len(txt) > 118 else txt

# 首页章节卡简介（≤42 字 · 一眼看清这章讲啥）
# 来源：每章 chapter-subtitle 已精炼；这里进一步浓缩为"卡片导览"
CH_DESC = {
    'ch00': '读前地图 · 全书脉络与学习路径',
    'ch01': '十分钟跑起来 · 构建启动第一次对话',
    'ch02': '六边形架构 · 模块划分与依赖方向',
    'ch03': '领域设计 · 事件风暴与 27 个上下文',
    'ch04': 'ReAct 循环 · Agent 核心引擎剖析',
    'ch05': '会话事件 · SessionLog 事件溯源',
    'ch06': '意图策略 · 消息路由与责任链',
    'ch07': 'SSE 协议 · meta chunk step_break',
    'ch08': '工具系统 · Executor 与 Hook 链',
    'ch09': '插件开发 · Java 与 Node 双模式',
    'ch10': 'MCP 集成 · 连接外部工具生态',
    'ch11': '治理审批 · 权限矩阵与人工决策',
    'ch12': '部署生产 · 配置监控与容错落地',
    'ch13': '尾声 · 总结复盘与扩展路线',
}

# 首页 Hero 右侧架构插画 SVG（六层架构 · 编辑排版 · 浅色背景）
HOME_HERO_ART_SVG = '''<svg viewBox="0 0 460 360" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Harness 六层架构示意">
  <defs>
    <linearGradient id="lgPurple" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#a5b4fc"/><stop offset="100%" stop-color="#6366f1"/>
    </linearGradient>
    <linearGradient id="lgBlue" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#7dd3fc"/><stop offset="100%" stop-color="#0ea5e9"/>
    </linearGradient>
    <linearGradient id="lgGreen" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#6ee7b7"/><stop offset="100%" stop-color="#10b981"/>
    </linearGradient>
    <linearGradient id="lgAmber" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#fcd34d"/><stop offset="100%" stop-color="#f59e0b"/>
    </linearGradient>
    <linearGradient id="lgPink" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#f9a8d4"/><stop offset="100%" stop-color="#ec4899"/>
    </linearGradient>
    <linearGradient id="lgSlate" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#cbd5e1"/><stop offset="100%" stop-color="#64748b"/>
    </linearGradient>
    <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="6" in="SourceAlpha"/>
      <feOffset dy="4"/>
      <feComponentTransfer><feFuncA type="linear" slope="0.18"/></feComponentTransfer>
      <feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
  </defs>

  <!-- 背景柔光圆 -->
  <circle cx="80" cy="60" r="60" fill="url(#lgPurple)" opacity="0.10"/>
  <circle cx="400" cy="300" r="70" fill="url(#lgBlue)" opacity="0.10"/>

  <!-- Trigger 层（API 入口） -->
  <g filter="url(#softShadow)">
    <rect x="150" y="20" width="160" height="44" rx="10" fill="white" stroke="#cbd5e1" stroke-width="1"/>
    <rect x="150" y="20" width="4" height="44" rx="2" fill="url(#lgPurple)"/>
    <text x="180" y="40" font-size="13" font-weight="700" fill="#0f172a">Trigger</text>
    <text x="180" y="56" font-size="10.5" fill="#64748b">REST + SSE 控制台</text>
  </g>
  <text x="80" y="48" font-size="10.5" font-weight="700" fill="#64748b">①</text>

  <!-- API/Case 层 -->
  <g filter="url(#softShadow)">
    <rect x="150" y="84" width="160" height="44" rx="10" fill="white" stroke="#cbd5e1" stroke-width="1"/>
    <rect x="150" y="84" width="4" height="44" rx="2" fill="url(#lgBlue)"/>
    <text x="180" y="104" font-size="13" font-weight="700" fill="#0f172a">API / Case</text>
    <text x="180" y="120" font-size="10.5" fill="#64748b">CQRS + 责任链策略</text>
  </g>
  <text x="80" y="112" font-size="10.5" font-weight="700" fill="#64748b">②</text>

  <!-- Domain 层（核心，高亮） -->
  <g filter="url(#softShadow)">
    <rect x="130" y="148" width="200" height="50" rx="12" fill="url(#lgPurple)" opacity="0.95"/>
    <text x="230" y="170" font-size="14" font-weight="800" fill="white" text-anchor="middle">Domain（核心）</text>
    <text x="230" y="187" font-size="11" fill="rgba(255,255,255,0.85)" text-anchor="middle">ReactLoopAgent · SessionLog</text>
  </g>
  <text x="78" y="178" font-size="10.5" font-weight="700" fill="#64748b">③</text>

  <!-- Types SPI -->
  <g filter="url(#softShadow)">
    <rect x="150" y="216" width="160" height="44" rx="10" fill="white" stroke="#cbd5e1" stroke-width="1"/>
    <rect x="150" y="216" width="4" height="44" rx="2" fill="url(#lgAmber)"/>
    <text x="180" y="236" font-size="13" font-weight="700" fill="#0f172a">Types (SPI)</text>
    <text x="180" y="252" font-size="10.5" fill="#64748b">端口与可插拔接口</text>
  </g>
  <text x="78" y="244" font-size="10.5" font-weight="700" fill="#64748b">④</text>

  <!-- Plugins + Infrastructure（横向并列） -->
  <g filter="url(#softShadow)">
    <rect x="60" y="288" width="160" height="44" rx="10" fill="white" stroke="#cbd5e1" stroke-width="1"/>
    <rect x="60" y="288" width="4" height="44" rx="2" fill="url(#lgPink)"/>
    <text x="90" y="308" font-size="13" font-weight="700" fill="#0f172a">Plugins</text>
    <text x="90" y="324" font-size="10.5" fill="#64748b">Java JAR · Node Sidecar · MCP</text>
  </g>
  <text x="40" y="316" font-size="10.5" font-weight="700" fill="#64748b">⑤</text>

  <g filter="url(#softShadow)">
    <rect x="240" y="288" width="160" height="44" rx="10" fill="white" stroke="#cbd5e1" stroke-width="1"/>
    <rect x="240" y="288" width="4" height="44" rx="2" fill="url(#lgSlate)"/>
    <text x="270" y="308" font-size="13" font-weight="700" fill="#0f172a">Infrastructure</text>
    <text x="270" y="324" font-size="10.5" fill="#64748b">MyBatis · LLM · Shell</text>
  </g>
  <text x="416" y="316" font-size="10.5" font-weight="700" fill="#64748b">⑥</text>

  <!-- 箭头连线 -->
  <defs>
    <marker id="ah" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto">
      <path d="M0,0 L10,5 L0,10 z" fill="#94a3b8"/>
    </marker>
  </defs>
  <line x1="230" y1="64"  x2="230" y2="80"  stroke="#94a3b8" stroke-width="1.4" marker-end="url(#ah)"/>
  <line x1="230" y1="128" x2="230" y2="144" stroke="#94a3b8" stroke-width="1.4" marker-end="url(#ah)"/>
  <line x1="230" y1="198" x2="230" y2="212" stroke="#94a3b8" stroke-width="1.4" marker-end="url(#ah)"/>
  <line x1="200" y1="260" x2="160" y2="284" stroke="#94a3b8" stroke-width="1.4" marker-end="url(#ah)"/>
  <line x1="260" y1="260" x2="300" y2="284" stroke="#94a3b8" stroke-width="1.4" marker-end="url(#ah)"/>
</svg>'''

# ============================================================
# 首页 · 整体框架结构（交互式架构图）
# 左：7 个 Maven 模块分层图（点击/悬停联动右侧详情，双击/回车跳章节）
# 右：模块详情面板（内容在 <template data-arch-detail> 中，由 main.js 切换）
# ============================================================
_ARCH_ARROW = ('<div class="arch-arrow">'
               '<svg width="12" height="14" viewBox="0 0 12 14" fill="none">'
               '<path d="M6 1v10M2 8l4 4 4-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>'
               '</svg><span class="arch-arrow-label">%s</span></div>')

def _arch_layer(mid, icon, color, name, pkg, desc, go, core=False):
    cls = 'arch-layer core' if core else 'arch-layer'
    return (f'<div class="{cls}" data-module="{mid}" style="--layer-color:{color}" tabindex="0" role="button">'
            f'<span class="arch-layer-icon">{icon}</span>'
            f'<span class="arch-layer-main"><span class="arch-layer-name">{name}</span>'
            f'<span class="arch-layer-pkg" title="{pkg}">{pkg}</span></span>'
            f'<span class="arch-layer-desc">{desc}</span>'
            f'<span class="arch-layer-go">{go} →</span></div>')

def _arch_detail(mid, color, tag, name, artifact, desc, classes, link_href, link_text):
    chips = ''.join(f'<span class="arch-class-chip">{c}</span>' for c in classes)
    return (f'<template data-arch-detail="{mid}">'
            f'<div style="--detail-color:{color}">'
            f'<span class="arch-detail-tag">{tag}</span>'
            f'<h3 class="arch-detail-name">{name}</h3>'
            f'<div class="arch-detail-artifact">{artifact}</div>'
            f'<p class="arch-detail-desc">{desc}</p>'
            f'<div class="arch-detail-h">关键类 / 组件</div>'
            f'<div class="arch-detail-classes">{chips}</div>'
            f'<a class="arch-detail-link" href="{link_href}">{link_text} →</a>'
            f'</div></template>')

HOME_ARCH_HTML = (
    '<section class="arch-section" id="archSection">'
    '<div class="arch-head">'
    '<div class="arch-eyebrow">整体框架结构</div>'
    '<h2 class="arch-title">一图看清：7 个 Maven 模块如何协同</h2>'
    '<p class="arch-subtitle">DDD 六边形架构 · 依赖方向永远指向 <code>Domain</code> 核心 · 点击模块查看详情并跳转对应章节</p>'
    '</div>'
    '<div class="arch-grid">'
    '<div class="arch-map">'
    + _arch_layer('app', '🚀', '#8b5cf6', 'App', 'deepseek-harness-java-app', '启动装配 · 端口 8090', '第1章')
    + (_ARCH_ARROW % '依赖')
    + _arch_layer('trigger', '🌐', '#6366f1', 'Trigger', '18 个 Controller（CQRS 拆分）', 'REST + SSE 入口', '第7章')
    + (_ARCH_ARROW % '调用')
    + _arch_layer('case', '🧭', '#0ea5e9', 'API / Case', '接口层 + 用例编排', '策略树 = 责任链', '第6章')
    + (_ARCH_ARROW % '驱动')
    + _arch_layer('domain', '🧠', '#4f46e5', 'Domain（核心）', 'ReactLoopAgent · SessionLog', '不依赖任何外层', '第4章', core=True)
    + (_ARCH_ARROW % '定义端口 ↓')
    + _arch_layer('types', '🔌', '#f59e0b', 'Types (SPI)', '端口与可插拔接口', '契约定义层', '第2章')
    + (_ARCH_ARROW % '实现端口 ↓')
    + '<div class="arch-split">'
    + _arch_layer('plugins', '🧩', '#ec4899', 'Plugins', 'Java JAR · Node sidecar · MCP', '进程内 / 跨进程', '第9章')
    + _arch_layer('infra', '🗄️', '#64748b', 'Infrastructure', 'MyBatis · LLM 网关 · Shell', '适配器实现', '第2章')
    + '</div>'
    '</div>'
    '<aside class="arch-detail" id="archDetail">'
    '<div class="arch-detail-hint" id="archDetailHint">'
    '<div class="arch-detail-hint-icon">👆</div>'
    '<div>悬停或点击左侧模块<br>查看它的职责、关键类与对应章节</div>'
    '</div>'
    '</aside>'
    '</div>'
    + _arch_detail('app', '#8b5cf6', '启动模块', 'App · 启动与装配', 'deepseek-harness-java-app',
        '整个系统的入口：Spring Boot 3.3 启动类、全局配置装配（AgentFactory 手工注入端口实现）、H2/MySQL 数据源切换，'
        '以及自带原生 JS 的 Web 控制台（static/index.html + app.js）。所有 Bean 在这里被"接线"成一棵可运行的树。',
        ['HarnessApplication', 'HarnessApplicationConfig', 'AgentFactory', 'static/app.js'],
        'ch01-quickstart.html', '第1章：十分钟跑起来')
    + _arch_detail('trigger', '#6366f1', '接口适配层', 'Trigger · REST + SSE 入口', 'deepseek-harness-java-trigger',
        '18 个 Controller 按 CQRS 拆成 command/query 两侧，统一前缀 /api/agent、/api/harness/*、/api/workflow。'
        '它只做协议转换：把 HTTP 请求翻译成用例调用，把用例的流式输出翻译成 SSE 事件（meta/chunk/step_break/done）。',
        ['AgentController', 'HarnessTaskController', 'ApprovalController', 'PluginController'],
        'ch07-sse-protocol.html', '第7章：SSE 流式协议')
    + _arch_detail('case', '#0ea5e9', '用例编排层', 'API / Case · 策略树与责任链', 'deepseek-harness-java-api + deepseek-harness-java-case',
        'API 层定义用例接口（IAgentUseCase），Case 层用"策略树 = 责任链"实现消息流转：'
        'AbstractStrategyRouter 统一 doApply + getNext，Agent 链 Resolve→Intent→Dispatch→Collect，任务提交链 SubmissionRoot→PermissionCheck→Enqueue。',
        ['AgentUseCase', 'AbstractStrategyRouter', 'AgentIntentNode', 'AgentDispatchNode', 'AgentCollectNode'],
        'ch06-intent-strategy.html', '第6章：意图识别与策略树')
    + _arch_detail('domain', '#4f46e5', '核心域', 'Domain · 系统的心脏', 'deepseek-harness-java-domain',
        '唯一不依赖任何外层的模块。ReactLoopAgent 用 turn/step 两级循环驱动"思考-行动-观察"，单 turn 上限 50 步、'
        'max_tokens 受控续写最多 4 次；SessionLog 以内存 WAL + SurfaceOp 投影实现事件溯源；ToolCallExecutor 串起 Hook 链与审批闸口。',
        ['ReactLoopAgent', 'SessionLog', 'BasicCompactionEngine', 'ToolCallExecutor', 'PermissionPolicyService'],
        'ch04-react-loop.html', '第4章：ReAct 循环引擎')
    + _arch_detail('types', '#f59e0b', '契约层', 'Types (SPI) · 端口定义', 'deepseek-harness-java-types',
        '六边形架构的"端口"：只定义接口与值对象，不包含任何实现。Domain 依赖它表达对外需求（如 LLM 流式调用），'
        'Plugins 与 Infrastructure 实现它完成"适配"。sealed 接口 ContentBlock/StreamChunk 让协议演进可编译期检查。',
        ['ILlmRuntimePort', 'ContentBlock (sealed)', 'StreamChunk (sealed)', 'Flow.Publisher'],
        'ch02-architecture.html', '第2章：六边形架构')
    + _arch_detail('plugins', '#ec4899', '扩展生态', 'Plugins · 双模式插件体系', 'plugins/ 目录',
        '三段式生命周期：Registry（安装/启停）→ Bridge（识别 DSH_PACKAGE / JAVA_NATIVE 等包型）→ Runtime'
        '（Java = 进程内 URLClassLoader 隔离加载；Node = sidecar 子进程 JSON-RPC）。MCP 通过 StdioMcpClient 接入外部工具生态。',
        ['PluginRegistry', 'PluginBridge', 'NodeSidecarRuntime', 'StdioMcpClient', 'McpToolAdapter'],
        'ch09-plugin.html', '第9章：插件开发实战')
    + _arch_detail('infra', '#64748b', '适配器层', 'Infrastructure · 技术实现', 'deepseek-harness-java-infrastructure',
        'Domain 端口的具体实现：MyBatis DAO 持久化会话/任务/审批等 12 张表；DeepSeekAdapter 手解 SSE 对接 LLM 网关'
        '（HTTP 总超时 120s，重试在 InMemoryLlmRuntimePort）；LocalShellExecutor 提供本地命令执行能力。',
        ['DeepSeekAdapter', 'InMemoryLlmRuntimePort', 'LocalShellExecutor', 'MyBatis DAO × 12 表'],
        'ch02-architecture.html', '第2章：六边形架构')
    + '</section>'
)

# ============================================================
# 首页 · 一次对话的运行时旅程（6 步流动图）
# ============================================================
def _journey_step(icon, name, sub, href, link):
    return (f'<a class="journey-step" href="{href}">'
            f'<span class="journey-step-icon">{icon}</span>'
            f'<span class="journey-step-name">{name}</span>'
            f'<span class="journey-step-sub">{sub}</span>'
            f'<span class="journey-step-link">{link} →</span></a>')

HOME_JOURNEY_HTML = (
    '<section class="journey-section">'
    '<div class="journey-head">'
    '<div class="journey-eyebrow">运行时视角</div>'
    '<h2 class="journey-title">一次对话的旅程：从敲下回车到看到流式回答</h2>'
    '<p class="journey-subtitle">跟着一条消息走完整个 Harness——每个节点都对应书中一章</p>'
    '</div>'
    '<div class="journey-flow">'
    + _journey_step('🧑', '用户提问', 'Web 控制台<br>发起 SSE 连接', 'ch01-quickstart.html', '第1章')
    + _journey_step('🌐', 'Controller 层', 'AgentController<br>POST /api/agent', 'ch07-sse-protocol.html', '第7章')
    + _journey_step('🧭', '策略责任链', 'Resolve→Intent<br>→Dispatch', 'ch06-intent-strategy.html', '第6章')
    + _journey_step('🧠', 'ReAct 循环', 'ReactLoopAgent<br>思考-行动-观察', 'ch04-react-loop.html', '第4章')
    + _journey_step('🔧', '工具执行器', 'ToolCallExecutor<br>Hook 链 + 审批', 'ch08-tools.html', '第8章')
    + _journey_step('📡', 'SSE 事件流', 'meta/chunk<br>step_break/done', 'ch07-sse-protocol.html', '第7章')
    + '</div>'
    '<div class="journey-note">💡 关键认知：LLM 只出现在第 4 步的一次 <code>stream()</code> 调用里——其余全是 Harness 的工程：'
    '协议、路由、状态、工具、治理。这正是"模型之外的外壳"的含义。</div>'
    '</section>'
)

def page_slug(ch):
    return ch['frag'].replace('.html', '')

def page_file(ch):
    return ROOT / page_slug(ch) + '.html' if False else ROOT / (page_slug(ch) + '.html')

def with_h2_ids(frag_html):
    """给 h2.section-heading 加锚点 id（若缺失），返回 (html, [(id, 文本)]) 供 TOC 用"""
    toc = []
    counter = [0]
    def repl(m):
        counter[0] += 1
        hid = f'sec-{counter[0]}'
        toc.append((hid, re.sub(r'<[^>]+>', '', m.group(2)).strip()))
        return f'<h2 class="section-heading" id="{hid}">{m.group(2)}</h2>'
    html = re.sub(r'<h2 class="section-heading"(?:\s+id="[^"]*")?>([\s\S]*?)</h2>',
                  lambda m: repl(re.match(r'', '') ) if False else '', frag_html)
    return html, toc

def with_h2_ids_v2(frag_html):
    """给 h2.section-heading 加锚点 id（保留片段内已有 id），返回 (html, toc)"""
    toc = []
    out = []
    pos = 0
    for m in re.finditer(r'<h2 class="section-heading"[^>]*>([\s\S]*?)</h2>', frag_html):
        inner = m.group(1)
        txt = re.sub(r'<[^>]+>', '', inner).strip()
        hid = 'sec-' + str(len(toc) + 1)
        out.append(frag_html[pos:m.start()])
        out.append(f'<h2 class="section-heading" id="{hid}">{inner}</h2>')
        pos = m.end()
        toc.append((hid, txt))
    out.append(frag_html[pos:])
    return ''.join(out), toc

def render_sidebar(chapters, current_id):
    """静态侧边栏：href 指真实页 URL，当前章 active"""
    html, last = [], None
    for ch in chapters:
        if ch['section'] != last:
            html.append(f'<div class="nav-section">{ch["section"]}</div>')
            last = ch['section']
        active = ' active' if ch['id'] == current_id else ''
        href = page_slug(ch) + '.html'
        html.append(
            f'<a class="nav-item{active}" data-chapter="{ch["id"]}" href="{href}">'
            f'<span class="nav-num">{ch["num"]:02d}</span>'
            f'<span class="nav-title">{htmllib.escape(ch["title"])}</span></a>')
    return ''.join(html)

def render_toc(toc):
    if not toc:
        return '<div style="padding:12px;color:var(--color-text-secondary);font-size:13px;">本章无小节</div>'
    items = ''.join(
        f'<a class="toc-item" href="#{hid}">{htmllib.escape(txt)}</a>'
        for hid, txt in toc)
    return items

def wrap_body(frag_html, ch):
    """片段 → 章节正文：包一层编辑式页头（eyebrow 篇名）"""
    eyebrow = f'<div class="chapter-eyebrow">{htmllib.escape(ch["section"])}</div>'
    return f'<div class="chapter-head">{eyebrow}{frag_html}</div>'

def render_page(chapters, ch, prev_ch, next_ch):
    frag_raw = (CHAPTERS_DIR / ch['frag']).read_text(encoding='utf-8')
    frag, toc = with_h2_ids_v2(frag_raw)
    desc = page_desc(frag_raw)
    slug = page_slug(ch)
    canon = f'{SITE_URL}/{slug}.html'
    prev_next = []
    for label, pc, arrow in (('prev', prev_ch, '←'), ('next', next_ch, '→')):
        if pc:
            prev_next.append(
                f'<a class="page-nav-card {label}" href="{page_slug(pc)}.html">'
                f'<span class="page-nav-label">{arrow} {"上一章" if label == "prev" else "下一章"}</span>'
                f'<span class="page-nav-title">{htmllib.escape(pc["title"])}</span></a>')
        else:
            prev_next.append(
                f'<a class="page-nav-card {label} disabled" href="index.html">'
                f'<span class="page-nav-label">{"←" if label == "prev" else "→"} {"上一章" if label == "prev" else "下一章"}</span>'
                f'<span class="page-nav-title">回首页</span></a>')
    page_nav = ('<nav class="page-nav">' + ''.join(prev_next) + '</nav>')
    title = f'第{ch["num"]}章 {ch["title"]} | {SITE_TITLE}'
    return f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{htmllib.escape(title)}</title>
  <meta name="description" content="{htmllib.escape(desc)}">
  <link rel="canonical" href="{canon}">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📚</text></svg>">
  <link rel="stylesheet" href="css/book.css">
  <link rel="stylesheet" href="css/site.css">
  <link rel="stylesheet" href="css/flowchart.css">
</head>
<body data-chapter="{ch["id"]}">
  <div class="reading-progress" id="readingProgress"></div>
  <div class="app-layout chat-collapsed">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="sidebar-logo" onclick="location.href='index.html'" title="回到首页">📚 Harness 教程</h1>
        <button class="theme-toggle" id="themeToggle" title="切换主题">🌙</button>
      </div>
      <div class="sidebar-progress">
        <div class="progress-bar"><div class="progress-bar-fill" style="width: 0%"></div></div>
        <div class="progress-text"><span>学习进度</span><span id="progressText">0/14 章</span></div>
      </div>
      <nav class="sidebar-nav" id="sidebarNav">{render_sidebar(chapters, ch["id"])}</nav>
    </aside>
    <main class="content-area">
      <div class="content-header">
        <div class="breadcrumb"><a href="index.html" style="text-decoration:none;">首页</a><span class="crumb-sep">/</span>{htmllib.escape(ch["section"])}</div>
        <div class="nav-buttons">
          <a class="nav-btn" href="{page_slug(prev_ch) + ".html" if prev_ch else "index.html"}">← 上一章</a>
          <a class="nav-btn" href="{page_slug(next_ch) + ".html" if next_ch else "index.html"}">下一章 →</a>
        </div>
      </div>
      <div class="content-body" id="contentBody">{wrap_body(frag, ch)}</div>
      {page_nav}
      <button class="back-to-top" id="backToTop" title="回到顶部" aria-label="回到顶部">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="M12 3.5L4 12h5v8h6v-8h5z"/></svg>
      </button>
    </main>
    <aside class="toc-panel" id="tocPanel">
      <div class="toc-header"><h3>📋 本章目录</h3></div>
      <nav class="toc-nav" id="tocNav">{render_toc(toc)}</nav>
    </aside>
    <aside class="right-toolbar">
      <a class="toolbar-btn toolbar-btn-github" href="https://github.com/fuzhengwei/deepseek-harness-java" target="_blank" title="GitHub 源码" style="text-decoration:none;color:inherit;">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
      </a>
    </aside>
  </div>
  <script src="js/highlight.js"></script>
  <script src="js/progress.js"></script>
  <script src="js/flowchart.js"></script>
  <script src="js/main.js"></script>
{SCROLL_PROGRESS_JS}</body>
</html>'''

def render_index(chapters):
    cards = ''
    for section, chs in parts_grouped(chapters):
        # 移除 section 字符串里的 emoji（用 .home-part-title-bar 替代装饰），保留纯文字篇名
        section_text = re.sub(r'^[^\w\u4e00-\u9fff]+', '', section).strip()
        cards += f'<div class="home-part"><div class="home-part-title"><span class="home-part-title-bar"></span><span class="home-part-title-text">{section_text}</span><span class="home-part-title-meta">{len(chs)} 章</span></div><div class="home-chapter-list">'
        for ch in chs:
            slug = page_slug(ch)
            desc = CH_DESC.get(ch['id'], '')
            cards += (f'<a class="home-chapter-card" data-section="{htmllib.escape(ch["section"])}" href="{slug}.html">'
                      f'<div class="home-chapter-num">{ch["num"]:02d}</div>'
                      f'<div class="home-chapter-info"><div class="home-chapter-title">{htmllib.escape(ch["title"])}</div>'
                      f'<div class="home-chapter-desc">{htmllib.escape(desc)}</div></div>'
                      f'<span class="home-badge-slot" data-badge="{ch["id"]}"></span></a>')
        cards += '</div></div>'
    return f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{SITE_TITLE} — 从零精通 Agent Harness 的设计与实现</title>
  <meta name="description" content="13 章渐进式实战教程，基于真实开源项目 deepseek-harness-java，深入 DDD 六边形架构、ReAct 循环、事件溯源、策略树、插件体系与审批治理。">
  <link rel="canonical" href="{SITE_URL}/">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📚</text></svg>">
  <link rel="stylesheet" href="css/book.css">
  <link rel="stylesheet" href="css/site.css">
  <link rel="stylesheet" href="css/flowchart.css">
</head>
<body>
  <div class="reading-progress" id="readingProgress"></div>
  <div class="app-layout chat-collapsed">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="sidebar-logo" onclick="location.href='index.html'" title="回到首页">📚 Harness 教程</h1>
        <button class="theme-toggle" id="themeToggle" title="切换主题">🌙</button>
      </div>
      <div class="sidebar-progress">
        <div class="progress-bar"><div class="progress-bar-fill" style="width: 0%"></div></div>
        <div class="progress-text"><span>学习进度</span><span id="progressText">0/14 章</span></div>
      </div>
      <nav class="sidebar-nav" id="sidebarNav">{render_sidebar(chapters, None)}</nav>
    </aside>
    <main class="content-area">
      <div class="content-header">
        <div class="breadcrumb">首页</div>
        <div class="nav-buttons">
          <a class="nav-btn" href="{page_slug(chapters[0])}.html">第一章 →</a>
        </div>
      </div>
      <div class="content-body" id="contentBody">
        <div class="home-hero">
          <div class="home-hero-glow glow-a"></div>
          <div class="home-hero-glow glow-b"></div>
          <div class="home-hero-inner">
            <div class="home-hero-eyebrow"><span class="tag-dot"></span>实战教程 · 基于真实开源项目</div>
            <h1 class="home-hero-title">把 Agent Harness<br>读得<span class="title-em">明明白白</span></h1>
            <p class="home-hero-desc">13 章渐进式教程，基于开源项目 <strong>deepseek-harness-java</strong>，<br>从跑起来到读懂 ReAct 循环、事件溯源、策略树、插件体系与审批治理。</p>
            <div class="home-hero-actions">
              <a class="home-btn primary" href="{page_slug(chapters[1])}.html">开始学习<span class="btn-arrow">→</span></a>
              <a class="home-btn ghost" href="https://github.com/fuzhengwei/deepseek-harness-java" target="_blank">GitHub 仓库</a>
            </div>
            <div class="home-hero-stats">
              <div class="home-stat"><div class="home-stat-num">13</div><div class="home-stat-label">章节</div></div>
              <div class="home-stat"><div class="home-stat-num">6</div><div class="home-stat-label">篇幅</div></div>
              <div class="home-stat"><div class="home-stat-num">27</div><div class="home-stat-label">限界上下文</div></div>
              <div class="home-stat"><div class="home-stat-num">7</div><div class="home-stat-label">Maven 模块</div></div>
            </div>
          </div>
          <div class="home-hero-art">
            <div class="home-hero-art-frame">
              <div class="home-hero-art-caption">Harness 六层架构 · DDD 六边形</div>
              {HOME_HERO_ART_SVG}
            </div>
          </div>
        </div>
        {HOME_ARCH_HTML}
        {HOME_JOURNEY_HTML}
        {cards}
        <div class="home-tip">
          <div class="home-tip-icon">💡</div>
          <div class="home-tip-body">
            <div class="home-tip-title">阅读建议</div>
            <div class="home-tip-text">
              本教程循序渐进、承上启下：<strong>第一篇先跑起来</strong>获得直觉，<strong>第二篇俯瞰架构</strong>建立全局，
              <strong>第三篇深入核心机制</strong>理解原理，<strong>第四篇动手扩展</strong>插件与工具，<strong>第五六篇走向生产</strong>。
              每章开头有「本章导读」，结尾有「小结与下一章预告」，像看书一样按顺序读体验最佳。
            </div>
          </div>
        </div>
      </div>
      <button class="back-to-top" id="backToTop" title="回到顶部" aria-label="回到顶部">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="M12 3.5L4 12h5v8h6v-8h5z"/></svg>
      </button>
    </main>
    <aside class="toc-panel" id="tocPanel">
      <div class="toc-header"><h3>📋 站点导航</h3></div>
      <nav class="toc-nav" id="tocNav">{render_sidebar(chapters, None)}</nav>
    </aside>
    <aside class="right-toolbar">
      <a class="toolbar-btn toolbar-btn-github" href="https://github.com/fuzhengwei/deepseek-harness-java" target="_blank" title="GitHub 源码" style="text-decoration:none;color:inherit;">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
      </a>
    </aside>
  </div>
  <script src="js/highlight.js"></script>
  <script src="js/progress.js"></script>
  <script src="js/flowchart.js"></script>
  <script src="js/main.js"></script>
{SCROLL_PROGRESS_JS}</body>
</html>'''

def parts_grouped(chapters):
    out, seen = [], {}
    for ch in chapters:
        seen.setdefault(ch['section'], []).append(ch)
    return list(seen.items())

def main():
    chapters = load_registry()
    print(f'[build] 注册表 {len(chapters)} 章')
    # 独立章节页
    for i, ch in enumerate(chapters):
        prev_ch = chapters[i - 1] if i > 0 else None
        next_ch = chapters[i + 1] if i < len(chapters) - 1 else None
        page = render_page(chapters, ch, prev_ch, next_ch)
        path = ROOT / (page_slug(ch) + '.html')
        path.write_text(page, encoding='utf-8')
        print(f'[build] {path.name}  ({len(page)//1024}KB)')
    # 静态首页
    (ROOT / 'index.html').write_text(render_index(chapters), encoding='utf-8')
    print('[build] index.html')
    # sitemap.xml
    urls = [f'{SITE_URL}/'] + [f'{SITE_URL}/{page_slug(ch)}.html' for ch in chapters]
    today = time.strftime('%Y-%m-%d')
    sitemap = ['<?xml version="1.0" encoding="UTF-8"?>',
               '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">']
    for u in urls:
        sitemap.append(f'  <url><loc>{u}</loc><lastmod>{today}</lastmod><changefreq>weekly</changefreq><priority>{"1.0" if u.endswith("/") else "0.8"}</priority></url>')
    sitemap.append('</urlset>')
    (ROOT / 'sitemap.xml').write_text('\n'.join(sitemap), encoding='utf-8')
    print(f'[build] sitemap.xml ({len(urls)} urls)')
    (ROOT / 'robots.txt').write_text(
        'User-agent: *\nAllow: /\n\nSitemap: ' + SITE_URL + '/sitemap.xml\n', encoding='utf-8')
    print('[build] robots.txt')

if __name__ == '__main__':
    main()
