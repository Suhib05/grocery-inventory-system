'use strict';

(async () => {
  if (!await Layout.init()) return;
  const daysSelect = document.getElementById('daysSelect');

  async function load() {
    try {
      const a = await Api.get(`api/alerts?days=${daysSelect.value}`);
      document.getElementById('expiredCount').textContent = a.expired.length;
      document.getElementById('soonCount').textContent = a.expiringSoon.length;
      Layout.setAlertBadge(a.expired.length + a.expiringSoon.filter(p => p.daysUntilExpiry <= 7).length);

      const lost = a.expired.reduce((sum, p) => sum + p.quantity * Number(p.price), 0);
      const banner = document.getElementById('expiredBanner');
      banner.classList.toggle('hidden', a.expired.length === 0);
      document.getElementById('expiredBannerText').textContent =
        `${a.expired.length} expired product(s) are still in stock, worth ${Fmt.currency(lost)}. ` +
        'Remove them from the shelf and set their quantity to 0.';

      document.getElementById('expiredTable').innerHTML = a.expired.length
        ? a.expired.map(row).join('') : emptyRow(7, 'No expired products in stock. 🎉');
      document.getElementById('soonTable').innerHTML = a.expiringSoon.length
        ? a.expiringSoon.map(row).join('') : emptyRow(7, `Nothing expires in the next ${a.days} days.`);
    } catch (e) {
      Layout.flash(e.message, true);
    }
  }

  function row(p) {
    return `<tr>
      <td><strong>${esc(p.name)}</strong></td>
      <td>${esc(p.categoryName)}</td>
      <td class="text-right">${Fmt.int(p.quantity)}</td>
      <td class="text-right">${Fmt.currency(p.quantity * Number(p.price))}</td>
      <td class="nowrap">${Fmt.date(p.expiryDate)}</td>
      <td>${expiryCell(p.daysUntilExpiry)}</td>
      <td><div class="row-actions">
        ${p.daysUntilExpiry >= 0 ? `<a class="btn btn-ghost btn-sm" href="sales.html?productId=${p.id}">Sell</a>` : ''}
        <a class="btn btn-secondary btn-sm" href="product-form.html?id=${p.id}">Update</a>
      </div></td>
    </tr>`;
  }

  daysSelect.addEventListener('change', load);
  load();
})();
