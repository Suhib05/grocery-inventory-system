/* Shared helpers: API calls with CSRF + error handling, and formatting. */
'use strict';

class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

const Api = {
  csrfToken: null,
  user: null,

  async request(method, url, body) {
    const headers = { 'Accept': 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET' && this.csrfToken) headers['X-CSRF-Token'] = this.csrfToken;

    let res;
    try {
      res = await fetch(url, {
        method, headers, credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body)
      });
    } catch (e) {
      throw new ApiError('Could not reach the server. Check your connection.', 0);
    }

    if (res.status === 401 && !url.endsWith('auth/login')) {
      window.location.href = 'login.html';
      throw new ApiError('Session expired', 401);
    }
    if (res.status === 204) return null;

    const isJson = (res.headers.get('Content-Type') || '').includes('application/json');
    const data = isJson ? await res.json() : null;
    if (!res.ok) {
      throw new ApiError((data && data.error) || `Request failed (${res.status})`, res.status,
                         data && data.fieldErrors);
    }
    return data;
  },

  get(url)        { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body === undefined ? {} : body); },
  put(url, body)  { return this.request('PUT', url, body); },
  del(url)        { return this.request('DELETE', url); }
};

const Fmt = {
  money: new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD' }),
  number: new Intl.NumberFormat('en-CA'),

  currency(v) { return this.money.format(Number(v || 0)); },
  int(v) { return this.number.format(Number(v || 0)); },

  /** "2026-11-18" -> "Nov 18, 2026" (parsed as a local date, not UTC). */
  date(iso) {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('en-CA', { month: 'short', day: 'numeric', year: 'numeric' });
  },
  shortDay(iso) {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('en-CA', { weekday: 'short' });
  },
  todayIso() {
    const t = new Date();
    return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`;
  }
};

/** Escape text before putting it into HTML. Every server value goes through this. */
function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

const STATUS_BADGE = {
  IN_STOCK: 'badge-success',
  LOW_STOCK: 'badge-warning',
  OUT_OF_STOCK: 'badge-muted',
  EXPIRING_SOON: 'badge-warning',
  EXPIRED: 'badge-danger'
};
const STATUS_ICON = { IN_STOCK: '✓', LOW_STOCK: '▼', OUT_OF_STOCK: '○', EXPIRING_SOON: '⏰', EXPIRED: '✕' };

function statusBadge(p) {
  return `<span class="badge ${STATUS_BADGE[p.status] || 'badge-muted'}">` +
         `${STATUS_ICON[p.status] || ''} ${esc(p.statusLabel)}</span>`;
}

/** "in 3 days" / "today" / "2 days ago", coloured by urgency. */
function expiryCell(days) {
  if (days === null || days === undefined) return '<span class="muted">No expiry</span>';
  let cls = 'expiry-ok';
  if (days < 0) cls = 'expiry-expired';
  else if (days <= 3) cls = 'expiry-critical';
  else if (days <= 7) cls = 'expiry-warning';
  let text;
  if (days === 0) text = 'Today';
  else if (days === 1) text = 'Tomorrow';
  else if (days > 0) text = `In ${days} days`;
  else text = `${-days} day${days === -1 ? '' : 's'} ago`;
  return `<span class="expiry-days ${cls}">${text}</span>`;
}

function emptyRow(cols, text) {
  return `<tr><td colspan="${cols}" class="table-empty">${esc(text)}</td></tr>`;
}

/** Show server field errors next to inputs that have data-field="name". */
function showFieldErrors(form, fieldErrors) {
  form.querySelectorAll('[data-error-for]').forEach(el => { el.textContent = ''; el.classList.add('hidden'); });
  form.querySelectorAll('.error').forEach(el => el.classList.remove('error'));
  Object.entries(fieldErrors || {}).forEach(([field, msg]) => {
    const input = form.querySelector(`[name="${field}"]`);
    const err = form.querySelector(`[data-error-for="${field}"]`);
    if (input) input.classList.add('error');
    if (err) { err.textContent = msg; err.classList.remove('hidden'); }
  });
}
