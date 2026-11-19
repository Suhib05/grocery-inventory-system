'use strict';

(() => {
  const form = document.getElementById('loginForm');
  const errorBox = document.getElementById('loginError');
  const btn = document.getElementById('loginBtn');

  document.getElementById('showPwd').addEventListener('change', e => {
    document.getElementById('password').type = e.target.checked ? 'text' : 'password';
  });

  form.addEventListener('submit', async e => {
    e.preventDefault();
    errorBox.classList.add('hidden');
    const username = form.username.value.trim();
    const password = form.password.value;

    const errors = {};
    if (!username) errors.username = 'Username is required.';
    if (!password) errors.password = 'Password is required.';
    showFieldErrors(form, errors);
    if (Object.keys(errors).length) return;

    btn.disabled = true;
    btn.textContent = 'Signing in…';
    try {
      await Api.post('api/auth/login', { username, password });
      window.location.href = './';
    } catch (err) {
      document.getElementById('errorText').textContent = err.message;
      errorBox.classList.remove('hidden');
      form.password.value = '';
      form.password.focus();
    } finally {
      btn.disabled = false;
      btn.textContent = 'Sign in →';
    }
  });
})();
