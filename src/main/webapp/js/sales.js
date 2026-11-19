'use strict';

(async () => {
  const user = await Layout.init();
  if (!user) return;

  const el = id => document.getElementById(id);
  const form = el('saleForm'), productSel = el('saleProduct'), qty = el('saleQty'),
        hint = el('saleHint'), submit = el('saleSubmit');
  const isAdmin = user.role === 'ADMIN';
  let products = [];

  const today = Fmt.todayIso();
  el('saleDate').value = today;
  el('saleDate').max = today;
  el('toDate').value = today;
  el('fromDate').value = isoDaysAgo(29);

  await loadProducts(new URLSearchParams(location.search).get('productId'));
  loadReport();

  async function loadProducts(selectId) {
    try {
      const all = await Api.get('api/products');
      products = all.filter(p => p.quantity > 0 && p.status !== 'EXPIRED');
      productSel.innerHTML = '<option value="">-- Select a product --</option>' + products.map(p =>
        `<option value="${p.id}">${esc(p.name)} (${Fmt.currency(p.price)}, ${p.quantity} left)</option>`).join('');
      if (selectId && products.some(p => String(p.id) === selectId)) productSel.value = selectId;
      updateHint();
    } catch (e) {
      Layout.flash(e.message, true);
    }
  }

  function selected() { return products.find(p => String(p.id) === productSel.value); }

  function updateHint() {
    const p = selected();
    const n = Number(qty.value);
    if (!p) { hint.textContent = ''; return; }
    qty.max = p.quantity;
    const total = Number.isInteger(n) && n > 0 ? ` · Total: ${Fmt.currency(n * Number(p.price))}` : '';
    hint.textContent = `${p.quantity} in stock at ${Fmt.currency(p.price)} each${total}`;
  }
  productSel.addEventListener('change', updateHint);
  qty.addEventListener('input', updateHint);

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const p = selected();
    const n = Number(qty.value);
    const errors = {};
    if (!p) errors.productId = 'Please choose a product.';
    if (!Number.isInteger(n) || n < 1) errors.quantity = 'Quantity must be at least 1.';
    else if (p && n > p.quantity) errors.quantity = `Only ${p.quantity} left in stock.`;
    showFieldErrors(form, errors);
    if (Object.keys(errors).length) return;

    submit.disabled = true;
    try {
      const sale = await Api.post('api/sales', { productId: p.id, quantity: n, saleDate: el('saleDate').value || null });
      Layout.flash(`Sale recorded: ${sale.quantitySold} × ${sale.productName} = ${Fmt.currency(sale.totalAmount)}.`);
      qty.value = 1;
      await loadProducts(String(p.id));
      loadReport();
    } catch (err) {
      showFieldErrors(form, err.fieldErrors);
      Layout.flash(err.message, true);
    } finally {
      submit.disabled = false;
    }
  });

  el('rangeForm').addEventListener('submit', e => { e.preventDefault(); loadReport(); });

  async function loadReport() {
    const body = el('salesBody');
    try {
      const r = await Api.get(`api/sales?from=${el('fromDate').value}&to=${el('toDate').value}`);
      el('sumRevenue').textContent = Fmt.currency(r.revenue);
      el('sumTransactions').textContent = Fmt.int(r.transactions);
      el('sumUnits').textContent = Fmt.int(r.unitsSold);
      body.innerHTML = r.sales.length ? r.sales.map(row).join('') : emptyRow(7, 'No sales in this period.');
    } catch (e) {
      body.innerHTML = emptyRow(7, e.message);
    }
  }

  function row(s) {
    const action = s.voidedAt
      ? '<span class="badge badge-muted">Voided</span>'
      : (isAdmin ? `<button type="button" class="btn btn-ghost btn-sm" data-void="${s.id}">Void</button>` : '');
    return `<tr class="${s.voidedAt ? 'voided' : ''}">
      <td class="nowrap">${Fmt.date(s.saleDate)}</td>
      <td>${esc(s.productName)}</td>
      <td class="text-right">${Fmt.int(s.quantitySold)}</td>
      <td class="text-right">${Fmt.currency(s.unitPrice)}</td>
      <td class="text-right"><strong>${Fmt.currency(s.totalAmount)}</strong></td>
      <td>${esc(s.recordedByName || '—')}</td>
      <td class="text-right">${action}</td>
    </tr>`;
  }

  el('salesBody').addEventListener('click', async e => {
    const btn = e.target.closest('[data-void]');
    if (!btn) return;
    const ok = await confirmDialog('Void sale',
      'Void this sale? It will no longer count toward revenue and the units go back into stock.', 'Void sale');
    if (!ok) return;
    try {
      await Api.post(`api/sales/${btn.dataset.void}/void`);
      Layout.flash('Sale voided and stock restored.');
      await loadProducts(productSel.value);
      loadReport();
    } catch (err) {
      Layout.flash(err.message, true);
    }
  });

  function isoDaysAgo(n) {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
})();
