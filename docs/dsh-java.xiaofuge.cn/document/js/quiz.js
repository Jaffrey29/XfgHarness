/* ===========================================================
   DeepSeek Harness Java 教程 — 课后考试（Quiz）
   - 单题多题类型支持（'single'/'multiple'）
   - 选项点击 → 选中 → 提交 → 判分 → 显示解析
   - 章节内的 <script> 块通过 window.Quiz.render(...) 注入
   =========================================================== */

const Quiz = {
  currentQuestions: [],
  currentAnswers: {},   // { qIndex: [optIndex,...] }
  submitted: false,

  /**
   * 渲染考试区域
   * @param {string} containerId — 容器 <div id="...">
   * @param {Array}  questions   — 题库（含 type/question/options/answer/explanation）
   */
  render(containerId, questions) {
    const container = document.getElementById(containerId);
    if (!container) return;

    this.currentQuestions = questions;
    this.currentAnswers = {};
    this.submitted = false;

    let html = `
      <div class="quiz-section" id="quizContainer">
        <div class="quiz-header">
          <h3>📝 课后考试</h3>
          <span class="quiz-score" id="quizScore">共 ${questions.length} 题</span>
        </div>
        <div class="quiz-body" id="quizBody">
    `;

    questions.forEach((q, i) => {
      // 跳过没有选项的开放题
      if (!q.options || !Array.isArray(q.options) || q.options.length === 0) return;
      const isMulti = q.type === 'multiple' || q.type === 'multi';
      const typeLabel = isMulti ? '多选题' : '单选题';
      html += `
        <div class="quiz-question" data-q-index="${i}">
          <div class="q-type">${typeLabel}</div>
          <div class="q-text">${i + 1}. ${q.question}</div>
          <div class="quiz-options">
      `;

      q.options.forEach((opt, j) => {
        const letter = String.fromCharCode(65 + j);
        html += `
          <div class="quiz-option" data-q="${i}" data-opt="${j}">
            <span class="option-letter">${letter}</span>
            <span>${opt}</span>
          </div>
        `;
      });

      html += `
          </div>
          <div class="quiz-explanation" id="explanation-${i}">
            <strong>解析：</strong>${q.explanation}
          </div>
        </div>
      `;
    });

    html += `
        </div>
        <div class="quiz-actions">
          <button class="quiz-btn primary" data-action="submit">提交答案</button>
          <button class="quiz-btn" data-action="reset">重做</button>
        </div>
        <div id="quizResult"></div>
      </div>
    `;

    container.innerHTML = html;

    // 绑定事件（用 data-action 避免 inline onclick 被 CSP 拦）
    container.querySelector('[data-action="submit"]')?.addEventListener('click', () => this.submit());
    container.querySelector('[data-action="reset"]')?.addEventListener('click', () => this.reset());

    this.bindOptions();
  },

  /**
   * 绑定选项点击事件
   */
  bindOptions() {
    const container = document.getElementById('quizContainer');
    if (!container) return;
    container.querySelectorAll('.quiz-option').forEach(el => {
      if (el.dataset.bound === '1') return;
      el.dataset.bound = '1';
      el.addEventListener('click', () => {
        if (this.submitted) return;

        const qIndex = parseInt(el.dataset.q, 10);
        const optIndex = parseInt(el.dataset.opt, 10);
        const question = this.currentQuestions[qIndex];
        if (!question) return;

        if (question.type === 'single') {
          // 单选：清除同题其他选项
          container.querySelectorAll(`.quiz-option[data-q="${qIndex}"]`).forEach(o => {
            o.classList.remove('selected');
          });
          el.classList.add('selected');
          this.currentAnswers[qIndex] = [optIndex];
        } else {
          // 多选：切换
          el.classList.toggle('selected');
          if (!this.currentAnswers[qIndex]) {
            this.currentAnswers[qIndex] = [];
          }
          const arr = this.currentAnswers[qIndex];
          const idx = arr.indexOf(optIndex);
          if (idx > -1) arr.splice(idx, 1);
          else arr.push(optIndex);
        }
      });
    });
  },

  /**
   * 提交答案 → 判分 → 标记正误 → 显示解析
   */
  submit() {
    this.submitted = true;
    const container = document.getElementById('quizContainer');
    if (!container) return;

    let correct = 0;
    const wrongQuestions = [];

    this.currentQuestions.forEach((q, i) => {
      const userAnswer = this.currentAnswers[i] || [];
      // 规范化正确答案（兼容数字/字符串/A-D）
      let correctAnswer = q.answer;
      if (typeof correctAnswer === 'number') correctAnswer = [correctAnswer];
      if (typeof correctAnswer === 'string' && /^[A-D]$/.test(correctAnswer)) {
        correctAnswer = [correctAnswer.charCodeAt(0) - 65];
      }
      if (!Array.isArray(correctAnswer)) correctAnswer = [];
      correctAnswer = correctAnswer.map(a =>
        (typeof a === 'string' && /^[A-D]$/.test(a)) ? a.charCodeAt(0) - 65 : a
      );
      const normalizedUser = userAnswer.map(a =>
        (typeof a === 'string' && /^[A-D]$/.test(a)) ? a.charCodeAt(0) - 65 : a
      );

      const isCorrect = normalizedUser.length === correctAnswer.length &&
        correctAnswer.every(a => normalizedUser.includes(a));

      if (isCorrect) correct++;
      else wrongQuestions.push({
        question: q.question,
        userAnswer: normalizedUser.map(idx => String.fromCharCode(65 + idx)),
        correctAnswer: correctAnswer.map(idx => String.fromCharCode(65 + idx)),
        explanation: q.explanation
      });

      // 标记每个选项
      container.querySelectorAll(`.quiz-option[data-q="${i}"]`).forEach(el => {
        const optIndex = parseInt(el.dataset.opt, 10);
        el.classList.remove('selected');
        if (correctAnswer.includes(optIndex)) el.classList.add('correct');
        else if (normalizedUser.includes(optIndex)) el.classList.add('wrong');
      });

      // 显示解析
      const exp = container.querySelector(`#explanation-${i}`);
      if (exp) exp.classList.add('show');
    });

    // 显示结果
    const total = this.currentQuestions.length;
    const score = total > 0 ? Math.round((correct / total) * 100) : 0;
    const resultEl = container.querySelector('#quizResult');
    if (resultEl) {
      let grade = '加油，继续努力';
      if (score >= 90) grade = '优秀 🏆';
      else if (score >= 75) grade = '良好 👍';
      else if (score >= 60) grade = '及格，继续加油';

      resultEl.innerHTML = `
        <div class="quiz-result">
          <div class="score-circle">${score}</div>
          <div class="score-label">${grade} · 答对 ${correct}/${total} 题</div>
        </div>
      `;
    }

    // 保存进度（可选）
    try {
      const chapterId = document.body.dataset.chapter || 'unknown';
      const KEY = 'dsh-java-quiz-' + chapterId;
      localStorage.setItem(KEY, JSON.stringify({
        score, correct, total,
        wrongCount: wrongQuestions.length,
        timestamp: Date.now()
      }));
    } catch (e) { /* localStorage 可能不可用，吞掉 */ }

    // 禁用选项指针
    container.querySelectorAll('.quiz-option').forEach(el => {
      el.style.cursor = 'default';
    });

    // 隐藏提交按钮
    const submitBtn = container.querySelector('[data-action="submit"]');
    if (submitBtn) submitBtn.style.display = 'none';
  },

  /**
   * 重做：清空选中态、隐藏解析、清空结果
   */
  reset() {
    this.currentAnswers = {};
    this.submitted = false;
    const container = document.getElementById('quizContainer');
    if (!container) return;

    container.querySelectorAll('.quiz-option').forEach(el => {
      el.classList.remove('selected', 'correct', 'wrong');
      el.style.cursor = 'pointer';
    });
    container.querySelectorAll('.quiz-explanation').forEach(el => {
      el.classList.remove('show');
    });
    const resultEl = container.querySelector('#quizResult');
    if (resultEl) resultEl.innerHTML = '';

    const submitBtn = container.querySelector('[data-action="submit"]');
    if (submitBtn) submitBtn.style.display = '';

    this.bindOptions();
  }
};

window.Quiz = Quiz;
