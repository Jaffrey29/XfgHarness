/* ========================================
   轻量语法高亮引擎
   支持 java / bash / json / yaml / http / xml
   自研 tokenizer，无外部依赖，暗色主题友好
   ======================================== */
const Syntax = {

  /* HTML 转义（高亮前必须先转义，防止 < > & 被解析） */
  esc(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  },

  /* 各语言 token 规则，按优先级从上往下匹配 */
  rules: {
    java: [
      { t: 'comment',  r: /\/\/[^\n]*|\/\*[\s\S]*?\*\// },
      { t: 'string',   r: /"(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*'/ },
      { t: 'annotation', r: /@[A-Za-z_]\w*/ },
      { t: 'keyword',  r: /\b(?:package|import|public|private|protected|class|interface|enum|extends|implements|static|final|void|return|new|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|this|super|instanceof|abstract|synchronized|volatile|transient|native|default|var|record|sealed|permits|non-sealed|yield|goto|const|boolean|byte|short|int|long|float|double|char|null|true|false)\b/ },
      { t: 'type',     r: /\b[A-Z][A-Za-z0-9_]*\b/ },
      { t: 'number',   r: /\b\d[\d_]*(?:\.\d+)?[lLfFdD]?\b|0x[\da-fA-F_]+\b/ },
      { t: 'function', r: /[a-z_]\w*(?=\s*\()/ },
      { t: 'operator', r: /[+\-*/%=!<>&|^~?:]+|->|::/ },
    ],
    bash: [
      { t: 'comment',  r: /#[^\n]*/ },
      { t: 'string',   r: /"(?:[^"\\]|\\.)*"|'[^']*'/ },
      { t: 'keyword',  r: /\b(?:if|then|else|elif|fi|for|while|do|done|case|esac|function|in|export|local|return|echo|cd|ls|cat|grep|sed|awk|curl|git|mvn|java|docker|chmod|mkdir|rm|cp|mv|tar|source|sudo|apt|brew)\b/ },
      { t: 'variable', r: /\$\{?[A-Za-z_]\w*\}?|\$[0-9@#?*!]/ },
      { t: 'number',   r: /\b\d+\b/ },
      { t: 'option',   r: /(?<=\s)-{1,2}[a-zA-Z][\w-]*/ },
    ],
    json: [
      { t: 'key',      r: /"(?:[^"\\]|\\.)*"(?=\s*:)/ },
      { t: 'string',   r: /"(?:[^"\\]|\\.)*"/ },
      { t: 'number',   r: /\b-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b/ },
      { t: 'keyword',  r: /\b(?:true|false|null)\b/ },
    ],
    yaml: [
      { t: 'comment',  r: /#[^\n]*/ },
      { t: 'key',      r: /[\w."'-]+(?=\s*:)/ },
      { t: 'string',   r: /"(?:[^"\\]|\\.)*"|'[^']*'/ },
      { t: 'number',   r: /\b\d+(?:\.\d+)?\b/ },
      { t: 'keyword',  r: /\b(?:true|false|null|yes|no|on|off)\b/ },
    ],
    http: [
      { t: 'comment',  r: /#[^\n]*/ },
      { t: 'keyword',  r: /\b(?:GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\b/ },
      { t: 'header',   r: /\b(?:Content-Type|Authorization|Accept|Host|User-Agent|Content-Length|Cache-Control|Connection|Cookie|Set-Cookie|Location|Server|Date|X-[\w-]+)(?=\s*:)/ },
      { t: 'string',   r: /"(?:[^"\\]|\\.)*"/ },
      { t: 'number',   r: /\b\d{3}\b/ },
    ],
    xml: [
      { t: 'comment',  r: /<!--[\s\S]*?-->/ },
      { t: 'tag',      r: /<\/?[\w:.-]+|\/?>/ },
      { t: 'attr',     r: /[\w:.-]+(?==)/ },
      { t: 'string',   r: /"[^"]*"|'[^']*'/ },
    ],
    properties: [
      { t: 'comment',  r: /#[^\n]*/ },
      { t: 'key',      r: /[\w.-]+(?==)/ },
      { t: 'string',   r: /=.*/ },
    ],
    sql: [
      { t: 'comment',  r: /--[^\n]*/ },
      { t: 'string',   r: /'[^']*'|"[^"]*"/ },
      { t: 'keyword',  r: /\b(?:SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|PRIMARY|KEY|NOT|NULL|DEFAULT|AUTO_INCREMENT|ENGINE|CHARSET|ORDER|BY|GROUP|LIMIT|JOIN|LEFT|RIGHT|INNER|ON|AS|AND|OR|IN|IS|LIKE|BETWEEN|EXISTS|IF|BIGINT|VARCHAR|INT|TEXT|TIMESTAMP|DATETIME|TINYINT)\b/i },
      { t: 'number',   r: /\b\d+\b/ },
    ],
  },

  /* 逐字符扫描，按规则优先级匹配 */
  tokenize(code, lang) {
    const rules = this.rules[lang];
    if (!rules) return this.esc(code);
    let out = '', i = 0, n = code.length;
    while (i < n) {
      let matched = false;
      for (const { t, r } of rules) {
        // 从当前位置开始匹配（sticky 语义：把正则锚定到 i）
        const re = new RegExp(r.source, r.flags.replace('g', '') + (r.flags.includes('y') ? '' : ''));
        const sub = code.slice(i);
        const m = re.exec(sub);
        if (m && m.index === 0) {
          out += `<span class="tk-${t}">${this.esc(m[0])}</span>`;
          i += m[0].length;
          matched = true;
          break;
        }
      }
      if (!matched) {
        // 处理多字符行首匹配（如 yaml key 用了 ^...m）
        out += this.esc(code[i]);
        i++;
      }
    }
    return out;
  },

  /* 检测语言：优先 data-lang，否则启发式嗅探 */
  detectLang(pre) {
    const dl = (pre.dataset.lang || '').toLowerCase();
    if (dl && this.rules[dl]) return dl;
    const t = pre.textContent;
    if (/^\s*(GET|POST|PUT|DELETE|PATCH)\s+\//m.test(t) || /HTTP\/1\.1|Content-Type:/i.test(t)) return 'http';
    if (/^\s*[\w.-]+\s*[:=]\s*\S/m.test(t) && !/[{};]/.test(t) && /:\s/.test(t)) return 'yaml';
    if (/^\s*[\w.-]+=\S/m.test(t) && !/[{}]/.test(t)) return 'properties';
    if (/[{}]/.test(t) && /"[\w-]+"\s*:/.test(t)) return 'json';
    if (/\b(SELECT|INSERT|CREATE TABLE|UPDATE)\b/i.test(t)) return 'sql';
    if (/\b(public|private|class|interface|void|import java|package )\b/.test(t)) return 'java';
    if (/<\w+[^>]*>/.test(t) && /<\/\w+>/.test(t)) return 'xml';
    if (/\b(mvn|git|docker|curl|java -jar|chmod|export|cd |ls |cat |grep )\b/.test(t) || /^#!/.test(t) || /\$\{?\w/.test(t)) return 'bash';
    return null;
  },

  /* 给单个 pre 做高亮 + 包一层 wrapper（语言标签 + 复制按钮） */
  enhance(pre) {
    if (pre.dataset.enhanced) return;
    pre.dataset.enhanced = '1';
    const lang = this.detectLang(pre);
    if (lang) {
      pre.innerHTML = this.tokenize(pre.textContent, lang);
    }
    // wrapper
    const block = pre.closest('.code-block');
    if (!block) return;
    const wrap = document.createElement('div');
    wrap.className = 'code-wrap';
    const bar = document.createElement('div');
    bar.className = 'code-bar';
    bar.innerHTML = `<span class="code-bar-lang">${lang || 'text'}</span>`;
    const btn = document.createElement('button');
    btn.className = 'code-copy';
    btn.textContent = '复制';
    btn.addEventListener('click', () => {
      navigator.clipboard.writeText(pre.textContent).then(() => {
        btn.textContent = '✓ 已复制';
        setTimeout(() => btn.textContent = '复制', 1500);
      });
    });
    bar.appendChild(btn);
    block.insertBefore(wrap, pre);
    wrap.appendChild(bar);
    wrap.appendChild(pre);
  },

  highlightAll(root) {
    (root || document).querySelectorAll('#contentBody .code-block pre, #contentBody pre').forEach(p => this.enhance(p));
  }
};
window.Syntax = Syntax;
