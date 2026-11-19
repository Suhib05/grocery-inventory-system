/* Builds the sidebar, loads the current user, wires logout, and shows flash messages. */
'use strict';

const NAV = [
  { section: 'Main' },
  { id: 'dashboard', href: 'dashboard.html', icon: '📊', label: 'Dashboard' },
  { id: 'products', href: 'products.html', icon: '📦', label: 'Products' },
  { id: 'product-form', href: 'product-form.html', icon: '➕', label: 'Add Product' },
  { section: 'Alerts' },
  { id: 'expiry-alerts', href: 'expiry-alerts.html', icon: '⏰', label: 'Expiry Alerts', badge: 'navAlertBadge' },
  { section: 'Sales' },
  { id: 'sales', href: 'sales.html', icon: '💰', label: 'Sales' }
];

const Layout = {
  async init() {
    const page = document.body.dataset.page;
    let me;
    try {
      me = await Api.get('api/auth/me');
    } catch (e) {
      return null; // a 401 already redirected to the login page
    }
    Api.csrfToken = me.csrfToken;
    Api.user = me.user;

    this.renderSidebar(page, me.user);
    document.querySelectorAll('[data-admin-only]').forEach(el => {
      if (me.user.role !== 'ADMIN') el.remove();
    });
    const dateEl = document.getElementById('currentDate');
    if (dateEl) dateEl.textContent = new Date().toLocaleDateString('en-CA',
      { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });

    this.showPendingFlash();
    return me.user;
  },

  renderSidebar(page, user) {
    const sidebar = document.getElementById('sidebar');
    const links = NAV.map(item => item.section
      ? `<div class="nav-section-label">${item.section}</div>`
      : `<a href="${item.href}" class="nav-item${item.id === page ? ' active' : ''}"` +
        `${item.id === page ? ' aria-current="page"' : ''}>` +
        `<span class="nav-icon">${item.icon}</span> ${item.label}` +
        (item.badge ? ` <span class="nav-badge hidden" id="${item.badge}"></span>` : '') + `</a>`
    ).join('');

    sidebar.innerHTML = `
      <div class="sidebar-header">
        <div class="sidebar-logo"><div class="logo-icon">🛒</div>
          <h1>GroceryIQ <span>Inventory System</span></h1></div>
      </div>
      <nav class="sidebar-nav" aria-label="Main">
        ${links}
        <div class="nav-section-label">Account</div>
        <button type="button" class="nav-item" id="logoutBtn"><span class="nav-icon">🚪</span> Log out</button>
      </nav>
      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-avatar">${esc((user.fullName || user.username).charAt(0).toUpperCase())}</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">${esc(user.fullName)}</div>
            <div class="sidebar-user-role">${esc(user.role.toLowerCase())}</div>
          </div>
        </div>
      </div>`;

    document.getElementById('logoutBtn').addEventListener('click', async () => {
      try { await Api.post('api/auth/logout'); } finally { window.location.href = 'login.html'; }
    });
    const menuBtn = document.getElementById('menuBtn');
    if (menuBtn) menuBtn.addEventListener('click', () => sidebar.classList.toggle('open'));
  },

  /** Number of products that need attention, shown next to "Expiry Alerts". */
  setAlertBadge(count) {
    const b = document.getElementById('navAlertBadge');
    if (!b) return;
    b.textContent = count;
    b.classList.toggle('hidden', !count);
  },

  flash(message, isError) {
    const box = document.getElementById('flashMsg');
    if (!box) return;
    box.querySelector('.flash-text').textContent = message;
    box.classList.toggle('flash-error', !!isError);
    box.classList.remove('hidden');
    box.scrollIntoView({ block: 'nearest' });
  },

  /** Store a message to show on the next page (e.g. after saving and redirecting). */
  flashNext(message) {
    try { sessionStorage.setItem('flash', message); } catch (e) { /* storage unavailable */ }
  },

  showPendingFlash() {
    const box = document.getElementById('flashMsg');
    if (box) box.querySelector('.flash-close').addEventListener('click', () => box.classList.add('hidden'));
    let msg = null;
    try { msg = sessionStorage.getItem('flash'); sessionStorage.removeItem('flash'); } catch (e) { /* ignore */ }
    if (msg) this.flash(msg);
  }
};

/** Simple confirm dialog using the .modal styles. Resolves true if confirmed. */
function confirmDialog(title, text, confirmLabel) {
  return new Promise(resolve => {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop open';
    backdrop.innerHTML = `
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="dlgTitle">
        <h3 id="dlgTitle">${esc(title)}</h3>
        <p>${esc(text)}</p>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" data-act="no">Cancel</button>
          <button type="button" class="btn btn-danger" data-act="yes">${esc(confirmLabel)}</button>
        </div>
      </div>`;
    const close = ok => { backdrop.remove(); document.removeEventListener('keydown', onKey); resolve(ok); };
    const onKey = e => { if (e.key === 'Escape') close(false); };
    backdrop.addEventListener('click', e => {
      if (e.target === backdrop) close(false);
      const act = e.target.closest('[data-act]');
      if (act) close(act.dataset.act === 'yes');
    });
    document.addEventListener('keydown', onKey);
    document.body.appendChild(backdrop);
    backdrop.querySelector('[data-act="no"]').focus();
  });
}
