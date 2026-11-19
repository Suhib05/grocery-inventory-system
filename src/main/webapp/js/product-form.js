'use strict';

(async () => {
  if (!await Layout.init()) return;

  const form = document.getElementById('productForm');
  const errorBox = document.getElementById('formError');
  const submitBtn = document.getElementById('submitBtn');
  const id = new URLSearchParams(location.search).get('id');

  try {
    const cats = await Api.get('api/categories');
    form.categoryId.insertAdjacentHTML('beforeend',
      cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join(''));

    if (id) {
      const p = await Api.get(`api/products/${encodeURIComponent(id)}`);
      document.title = `Edit ${p.name} – GroceryIQ`;
      document.getElementById('pageTitle').textContent = 'Edit Product';
      document.getElementById('crumbAction').textContent = 'Edit';
      document.getElementById('formTitle').textContent = p.name;
      form.name.value = p.name;
      form.categoryId.value = p.categoryId;
      form.price.value = Number(p.price).toFixed(2);
      form.quantity.value = p.quantity;
      form.minStock.value = p.minStock;
      form.expiryDate.value = p.expiryDate || '';
    }
  } catch (e) {
    showError(e.message);
    submitBtn.disabled = true;
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    errorBox.classList.add('hidden');

    const errors = validate();
    showFieldErrors(form, errors);
    if (Object.keys(errors).length) return;

    const payload = {
      name: form.name.value.trim(),
      categoryId: Number(form.categoryId.value),
      price: form.price.value,
      quantity: Number(form.quantity.value),
      minStock: form.minStock.value === '' ? null : Number(form.minStock.value),
      expiryDate: form.expiryDate.value || null
    };

    submitBtn.disabled = true;
    try {
      const saved = id
        ? await Api.put(`api/products/${encodeURIComponent(id)}`, payload)
        : await Api.post('api/products', payload);
      Layout.flashNext(`"${saved.name}" was ${id ? 'updated' : 'added'}.`);
      window.location.href = 'products.html';
    } catch (err) {
      showFieldErrors(form, err.fieldErrors);
      showError(err.message);
    } finally {
      submitBtn.disabled = false;
    }
  });

  /** Quick checks in the browser; the server re-checks everything. */
  function validate() {
    const errors = {};
    if (!form.name.value.trim()) errors.name = 'Product name is required.';
    if (!form.categoryId.value) errors.categoryId = 'Please choose a category.';
    if (form.price.value === '') errors.price = 'Price is required.';
    else if (Number(form.price.value) < 0) errors.price = "Price can't be negative.";
    if (form.quantity.value === '') errors.quantity = 'Quantity is required.';
    else if (!Number.isInteger(Number(form.quantity.value)) || Number(form.quantity.value) < 0)
      errors.quantity = 'Quantity must be a whole number, 0 or more.';
    if (form.minStock.value !== '' && (!Number.isInteger(Number(form.minStock.value)) || Number(form.minStock.value) < 0))
      errors.minStock = 'Minimum stock must be a whole number, 0 or more.';
    return errors;
  }

  function showError(msg) {
    document.getElementById('formErrorText').textContent = msg;
    errorBox.classList.remove('hidden');
  }
})();
