'use strict';

(async () => {
  const user = await Layout.init();
  if (!user) return;

  const el = id => document.getElementById(id);
  const search = el('searchInput'), category = el('categoryFilter'), status = el('statusFilter'),
        sort = el('sortSelect'), order = el('orderSelect'), body = el('productsBody');
  const isAdmin = user.role === 'ADMIN';

  // Restore filters from the URL so links like products.html?status=LOW_STOCK work.
  const params = new URLSearchParams(location.search);
  search.value = params.get('q') || '';
  status.value = params.get('status') || '';
  sort.value = params.get('sort') || 'name';
  order.value = params.get('order') || 'asc';

  try {
    const cats = await Api.get('api/categories');
    category.insertAdjacentHTML('beforeend',
      cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join(''));
    category.value = params.get('categoryId') || '';
  } catch (e) {
    Layout.flash(e.message, true);
  }

  function currentQuery() {
    const q = new URLSearchParams();
    if (search.value.trim()) q.set('q', search.value.trim());
    if (category.value) q.set('categoryId', category.value);
    if (status.value) q.set('status', status.value);
    if (sort.value !== 'name') q.set('sort', sort.value);
    if (order.value !== 'asc') q.set('order', order.value);
    return q.toString();
  }

  let requestNo = 0;
  async function load() {
    const qs = currentQuery();
    history.replaceState(null, '', qs ? `?${qs}` : location.pathname);
    el('exportBtn').href = `api/products/export${qs ? '?' + qs : ''}`;
    const mine = ++requestNo; // ignore responses that arrive out of order
    try {
      const products = await Api.get(`api/products${qs ? '?' + qs : ''}`);
      if (mine !== requestNo) return;
      el('productCount').textContent = products.length;
      body.innerHTML = products.length ? products.map(row).join('') : emptyRow(8, 'No products match these filters.');
    } catch (e) {
      if (mine === requestNo) body.innerHTML = emptyRow(8, e.message);
    }
  }

  function row(p) {
    return `<tr>
      <td><strong>${esc(p.name)}</strong></td>
      <td>${esc(p.categoryName)}</td>
      <td class="text-right">${Fmt.int(p.quantity)}</td>
      <td class="text-right">${Fmt.currency(p.price)}</td>
      <td class="text-right">${Fmt.int(p.minStock)}</td>
      <td class="nowrap">${p.expiryDate ? Fmt.date(p.expiryDate) + '<br>' + expiryCell(p.daysUntilExpiry) : expiryCell(null)}</td>
      <td>${statusBadge(p)}</td>
      <td><div class="row-actions">
        <a class="btn btn-ghost btn-sm" href="sales.html?productId=${p.id}" title="Record a sale">Sell</a>
        <a class="btn btn-secondary btn-sm" href="product-form.html?id=${p.id}">Edit</a>
        ${isAdmin ? `<button type="button" class="btn btn-danger btn-sm" data-delete="${p.id}" data-name="${esc(p.name)}">Delete</button>` : ''}
      </div></td>
    </tr>`;
  }

  body.addEventListener('click', async e => {
    const btn = e.target.closest('[data-delete]');
    if (!btn) return;
    const ok = await confirmDialog('Delete product',
      `Delete "${btn.dataset.name}"? It will be removed from inventory; its sales history is kept.`, 'Delete');
    if (!ok) return;
    try {
      await Api.del(`api/products/${btn.dataset.delete}`);
      Layout.flash(`"${btn.dataset.name}" was deleted.`);
      load();
    } catch (err) {
      Layout.flash(err.message, true);
    }
  });

  let debounce;
  search.addEventListener('input', () => { clearTimeout(debounce); debounce = setTimeout(load, 250); });
  [category, status, sort, order].forEach(s => s.addEventListener('change', load));
  el('resetBtn').addEventListener('click', () => {
    search.value = ''; category.value = ''; status.value = ''; sort.value = 'name'; order.value = 'asc';
    load();
  });

  load();
})();
