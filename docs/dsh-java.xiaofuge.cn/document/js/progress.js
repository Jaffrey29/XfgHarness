/* ========================================
   AI Agent Guide - 学习进度追踪
   localStorage 存储
   ======================================== */

const Progress = {
  STORAGE_KEY: 'dsh-java-guide-progress',

  /**
   * 获取全部进度
   */
  getAll() {
    try {
      return JSON.parse(localStorage.getItem(this.STORAGE_KEY)) || {
        chapters: {},
        quizResults: {},
        startTime: Date.now(),
        totalTime: 0
      };
    } catch {
      return { chapters: {}, quizResults: {}, startTime: Date.now(), totalTime: 0 };
    }
  },

  /**
   * 保存全部进度
   */
  save(data) {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data));
  },

  /**
   * 标记章节完成
   */
  markCompleted(chapterId) {
    const data = this.getAll();
    data.chapters[chapterId] = {
      status: 'completed',
      completedAt: Date.now()
    };
    this.save(data);
    this.updateUI();
  },

  /**
   * 标记章节学习中
   */
  markLearning(chapterId) {
    const data = this.getAll();
    if (!data.chapters[chapterId]) {
      data.chapters[chapterId] = {
        status: 'learning',
        startedAt: Date.now()
      };
    }
    this.save(data);
    this.updateUI();
  },

  /**
   * 获取章节状态
   */
  getStatus(chapterId) {
    const data = this.getAll();
    return data.chapters[chapterId]?.status || 'none';
  },

  /**
   * 保存章测试成绩
   */
  saveQuizResult(chapterId, result) {
    const data = this.getAll();
    data.quizResults[chapterId] = {
      ...result,
      takenAt: Date.now()
    };
    this.save(data);
    this.updateUI();
  },

  /**
   * 保存综合考试成绩
   */
  saveExamScore(score, correct, total) {
    const data = this.getAll();
    if (!data.examScores) data.examScores = [];
    data.examScores.push({
      score,
      correct,
      total,
      takenAt: Date.now()
    });
    // 只保留最近 20 次考试成绩
    if (data.examScores.length > 20) {
      data.examScores = data.examScores.slice(-20);
    }
    this.save(data);
    this.updateUI();
  },

  /**
   * 获取综合考试成绩列表
   */
  getExamScores() {
    return this.getAll().examScores || [];
  },

  /**
   * 获取考试成绩
   */
  getQuizResults() {
    return this.getAll().quizResults;
  },

  /**
   * 计算总体进度
   */
  getOverallProgress() {
    const data = this.getAll();
    const total = App.chapters ? App.chapters.length : 17;
    const completed = Object.values(data.chapters).filter(c => c.status === 'completed').length;
    return Math.round((completed / total) * 100);
  },

  /**
   * 更新UI显示
   */
  updateUI() {
    // 更新进度条
    const progress = this.getOverallProgress();
    const data = this.getAll();
    const completed = Object.values(data.chapters).filter(c => c.status === 'completed').length;
    const total = App.chapters ? App.chapters.length : 17;
    const fillEl = document.querySelector('.sidebar-progress .progress-bar-fill');
    const numEl = document.getElementById('progressText');
    const capEl = document.getElementById('progressCaption');

    if (fillEl) fillEl.style.width = Math.round((completed / total) * 100) + '%';
    if (numEl) numEl.innerHTML = `${completed}/${total} 章`;

    // 进度徽章文案
    if (capEl) {
      if (completed === 0) capEl.textContent = 'Lv.1 · 尚未开始，从第 1 章出发';
      else if (completed === total) capEl.textContent = '🎉 Lv.MAX · 全部学完，太棒了';
      else {
        const pct = Math.round((completed / total) * 100);
        const lv = Math.min(9, Math.floor(pct / 12) + 1);
        capEl.textContent = `Lv.${lv} · 已完成 ${pct}%，继续前进`;
      }
    }
    
    // 更新导航项状态
    document.querySelectorAll('.nav-item').forEach(item => {
      const chapterId = item.dataset.chapter;
      if (!chapterId) return;
      
      const status = this.getStatus(chapterId);
      item.classList.remove('completed', 'learning');
      if (status === 'completed') {
        item.classList.add('completed');
      } else if (status === 'learning') {
        item.classList.add('learning');
      }
    });
  },

  /**
   * 获取所有错题
   */
  getAllWrongQuestions() {
    const results = this.getQuizResults();
    const all = [];
    Object.keys(results).forEach(chapterId => {
      const result = results[chapterId];
      if (result.wrongQuestions) {
        result.wrongQuestions.forEach(wq => {
          all.push({ ...wq, chapter: chapterId });
        });
      }
    });
    return all;
  }
};

window.Progress = Progress;
