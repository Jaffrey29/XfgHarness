/* dsh-java 官网 · 交互脚本 v2 */

/* ---- 移动端菜单 ---- */
const menuButton = document.querySelector('.menu-button');
const mobileNav = document.querySelector('.mobile-nav');

if (menuButton && mobileNav) {
  menuButton.addEventListener('click', () => {
    const expanded = menuButton.getAttribute('aria-expanded') === 'true';
    menuButton.setAttribute('aria-expanded', String(!expanded));
    mobileNav.classList.toggle('active');
  });

  mobileNav.addEventListener('click', (event) => {
    if (event.target.tagName === 'A') {
      menuButton.setAttribute('aria-expanded', 'false');
      mobileNav.classList.remove('active');
    }
  });
}

/* ---- 锚点平滑滚动 ---- */
document.querySelectorAll('a[href^="#"]').forEach((link) => {
  link.addEventListener('click', (event) => {
    const target = document.querySelector(link.getAttribute('href'));
    if (!target) return;
    event.preventDefault();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

/* ---- 导航滚动收缩 ---- */
const siteHeader = document.querySelector('.site-header');
if (siteHeader) {
  const onScroll = () => {
    siteHeader.classList.toggle('is-scrolled', window.scrollY > 24);
  };
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });
}

/* ---- 鼠标跟随光斑（轻量、降级安全） ---- */
const spotlight = document.querySelector('.spotlight');
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

if (spotlight && !reduceMotion && window.matchMedia('(pointer: fine)').matches) {
  let rafId = null;
  window.addEventListener('pointermove', (event) => {
    if (rafId) return;
    rafId = requestAnimationFrame(() => {
      spotlight.style.setProperty('--mx', event.clientX + 'px');
      spotlight.style.setProperty('--my', event.clientY + 'px');
      rafId = null;
    });
  }, { passive: true });
}

/* ---- 滚动渐入动画（带轻错落延迟） ---- */
const revealTargets = document.querySelectorAll('.reveal');

if ('IntersectionObserver' in window && revealTargets.length && !reduceMotion) {
  // 同一容器内的 reveal 元素按顺序加一点延迟，形成错落感
  const groups = new Map();
  revealTargets.forEach((el) => {
    const parent = el.parentElement;
    if (!groups.has(parent)) groups.set(parent, 0);
    const index = groups.get(parent);
    el.style.transitionDelay = Math.min(index * 70, 350) + 'ms';
    groups.set(parent, index + 1);
  });

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.1, rootMargin: '0px 0px -5% 0px' }
  );
  revealTargets.forEach((el) => observer.observe(el));
} else {
  revealTargets.forEach((el) => el.classList.add('is-visible'));
}
