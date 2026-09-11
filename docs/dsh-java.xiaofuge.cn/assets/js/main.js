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

document.querySelectorAll('a[href^="#"]').forEach((link) => {
  link.addEventListener('click', (event) => {
    const target = document.querySelector(link.getAttribute('href'));
    if (!target) return;
    event.preventDefault();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

document.querySelectorAll('.copy-button').forEach((button) => {
  button.addEventListener('click', async () => {
    const card = button.closest('.prompt-card');
    const code = card?.querySelector('pre code');
    if (!code) return;
    await navigator.clipboard.writeText(code.innerText);
    const original = button.textContent;
    button.textContent = '已复制';
    setTimeout(() => {
      button.textContent = original;
    }, 1400);
  });
});
