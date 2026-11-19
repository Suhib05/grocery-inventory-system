'use strict';

(async () => {
  if (!await Layout.init()) return;
  try {
    const d = await Api.get('api/dashboard');
    render(d);
  } catch (e) {
    Layout.flash(e.message, true);
  }

  function render(d) {
    const c = d.statusCounts;
    set('statTotal', Fmt.int(d.totalProducts));
    set('statLow', Fmt.int(c.LOW_STOCK + c.OUT_OF_STOCK));
    set('statExpiringSoon', Fmt.int(c.EXPIRING_SOON));
    set('statExpired', Fmt.int(c.EXPIRED));
    set('statStockValue', Fmt.currency(d.stockValue));
    set('statToday', Fmt.currency(d.revenueToday));
    set('rev7', Fmt.currency(d.revenueLast7Days));
    set('rev30', Fmt.currency(d.revenueLast30Days));
    Layout.setAlertBadge(c.EXPIRED + c.EXPIRING_SOON);

    document.getElementById('lowStockTable').innerHTML = d.lowStock.length
      ? d.lowStock.map(p => `<tr>
          <td><a href="product-form.html?id=${p.id}"><strong>${esc(p.name)}</strong></a></td>
          <td>${esc(p.categoryName)}</td>
          <td class="text-right">${p.quantity === 0 ? '<span class="badge badge-muted">Out</span>' : Fmt.int(p.quantity)}</td>
          <td class="text-right">${Fmt.int(p.minStock)}</td></tr>`).join('')
      : emptyRow(4, 'Stock levels look good. 👍');

    document.getElementById('expiringSoonTable').innerHTML = d.expiringSoon.length
      ? d.expiringSoon.map(p => `<tr>
          <td><strong>${esc(p.name)}</strong></td>
          <td>${esc(p.categoryName)}</td>
          <td class="nowrap">${Fmt.date(p.expiryDate)}</td>
          <td>${expiryCell(p.daysUntilExpiry)}</td></tr>`).join('')
      : emptyRow(4, 'Nothing expires in the next 7 days.');

    document.getElementById('expiredTable').innerHTML = d.expired.length
      ? d.expired.map(p => `<tr>
          <td><strong>${esc(p.name)}</strong></td>
          <td>${esc(p.categoryName)}</td>
          <td class="text-right">${Fmt.int(p.quantity)}</td>
          <td class="nowrap">${Fmt.date(p.expiryDate)}</td>
          <td>${expiryCell(p.daysUntilExpiry)}</td>
          <td class="text-right"><a class="btn btn-ghost btn-sm" href="product-form.html?id=${p.id}">Update</a></td></tr>`).join('')
      : emptyRow(6, 'No expired products on the shelf. 🎉');

    const chartEl = document.getElementById('revenueChart');
    RevenueChart.render(chartEl, d.revenueByDay);
    let resizeTimer;
    window.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => RevenueChart.render(chartEl, d.revenueByDay), 150);
    });
  }

  function set(id, text) { document.getElementById(id).textContent = text; }
})();

/**
 * Small dependency-free SVG bar chart for one series (daily revenue).
 * One hue, rounded data-end, 2px gaps, recessive grid, hover tooltip,
 * and a hidden data table for screen readers.
 */
const RevenueChart = {
  render(container, days) {
    const W = Math.max(320, container.clientWidth - 40), H = 220, padL = 52, padR = 8, padT = 12, padB = 28;
    const values = days.map(d => Number(d.total));
    const niceMax = this.niceCeil(Math.max(...values, 1));
    const plotW = W - padL - padR, plotH = H - padT - padB;
    const slot = plotW / days.length;
    const barW = Math.min(56, slot - 2);
    const y = v => padT + plotH - (v / niceMax) * plotH;

    const ticks = [0, 0.25, 0.5, 0.75, 1].map(f => f * niceMax);
    const grid = ticks.map(t => `<line x1="${padL}" x2="${W - padR}" y1="${y(t)}" y2="${y(t)}"/>`).join('');
    const yLabels = ticks.map(t =>
      `<text x="${padL - 8}" y="${y(t) + 4}" text-anchor="end">${this.shortMoney(t)}</text>`).join('');

    const bars = days.map((d, i) => {
      const v = values[i];
      const x = padL + i * slot + (slot - barW) / 2;
      const top = y(v), h = Math.max(0, padT + plotH - top);
      const r = Math.min(4, h, barW / 2);
      // Rounded top corners only; the bar stays square on the baseline.
      const path = h <= 0 ? '' :
        `M${x},${top + h} V${top + r} Q${x},${top} ${x + r},${top} H${x + barW - r} ` +
        `Q${x + barW},${top} ${x + barW},${top + r} V${top + h} Z`;
      return `<g data-i="${i}">
        <rect class="chart-hit" x="${padL + i * slot}" y="${padT}" width="${slot}" height="${plotH}"/>
        <path class="chart-bar" d="${path}"/>
        <text x="${x + barW / 2}" y="${H - 8}" text-anchor="middle">${Fmt.shortDay(d.date)}</text>
      </g>`;
    }).join('');

    container.innerHTML = `
      <svg class="chart-svg" viewBox="0 0 ${W} ${H}" role="img"
           aria-label="Bar chart of daily revenue for the last 7 days">
        <g class="chart-grid">${grid}</g>
        <g class="chart-axis">${yLabels}${bars}</g>
      </svg>
      <div class="chart-tooltip hidden"></div>
      <table class="sr-only"><caption>Daily revenue</caption>
        <tr><th>Date</th><th>Revenue</th></tr>
        ${days.map(d => `<tr><td>${Fmt.date(d.date)}</td><td>${Fmt.currency(d.total)}</td></tr>`).join('')}
      </table>`;

    const svg = container.querySelector('svg');
    const tip = container.querySelector('.chart-tooltip');
    svg.querySelectorAll('g[data-i]').forEach(g => {
      const i = Number(g.dataset.i);
      const bar = g.querySelector('.chart-bar');
      g.addEventListener('mouseenter', () => {
        bar.classList.add('is-active');
        const box = bar.getBoundingClientRect(), wrap = container.getBoundingClientRect();
        const anchorTop = box.height > 0 ? box.top : g.querySelector('.chart-hit').getBoundingClientRect().bottom - 30;
        tip.innerHTML = `<strong>${Fmt.currency(days[i].total)}</strong>${Fmt.date(days[i].date)}`;
        tip.style.left = `${box.left - wrap.left + box.width / 2}px`;
        tip.style.top = `${anchorTop - wrap.top - 4}px`;
        tip.classList.remove('hidden');
      });
      g.addEventListener('mouseleave', () => { bar.classList.remove('is-active'); tip.classList.add('hidden'); });
    });
  },

  niceCeil(v) {
    const mag = Math.pow(10, Math.floor(Math.log10(v)));
    const step = [1, 2, 2.5, 5, 10].find(s => s * mag >= v);
    return step * mag;
  },

  shortMoney(v) {
    return v >= 1000 ? `$${(v / 1000).toFixed(v % 1000 ? 1 : 0)}k` : `$${Math.round(v)}`;
  }
};
