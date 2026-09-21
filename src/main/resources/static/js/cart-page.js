/**
 * cart-page.js — Logic trang giỏ hàng full-page (/cart)
 *
 * Phụ thuộc: cart.js (đã được load qua navbar.html, cung cấp các hàm global:
 *   refreshCart, getCartSummary, changeQuantityGlobal, removeFromCartGlobal, clearGlobalCart)
 *
 * Luồng:
 *  1. DOMContentLoaded → chờ cart.js khởi tạo → gọi refreshCart() → render
 *  2. Lắng nghe event "cartUpdated" để tự re-render khi giỏ thay đổi
 *  3. Các nút +/- và xóa gọi thẳng API qua cart.js global functions
 */

(function () {
    'use strict';

    /* ==================== HELPERS ==================== */

    function fmt(value) {
        return new Intl.NumberFormat('vi-VN').format(Math.round(Number(value) || 0)) + ' đ';
    }

    function escHtml(str) {
        const d = document.createElement('div');
        d.textContent = str || '';
        return d.innerHTML;
    }

    /* ==================== RENDER ==================== */

    /**
     * Nhóm các items theo merchantId.
     * Mặc dù backend hiện chỉ hỗ trợ 1 shop/giỏ, hàm này sẵn sàng cho nhiều shop.
     */
    function groupByMerchant(summary) {
        const groups = new Map();

        (summary.items || []).forEach(item => {
            const key = item.merchantId || 'unknown';
            if (!groups.has(key)) {
                groups.set(key, {
                    merchantId: item.merchantId,
                    merchantName: item.merchantName || 'Cửa hàng',
                    items: []
                });
            }
            groups.get(key).items.push(item);
        });

        return [...groups.values()];
    }

    function renderCartItem(item) {
        const hasDiscount = item.discountPrice != null
            && Number(item.discountPrice) > 0
            && Number(item.discountPrice) < Number(item.price);

        const effectivePrice = Number(item.effectivePrice || item.discountPrice || item.price || 0);
        const subtotal = effectivePrice * Number(item.quantity || 1);

        const thumbHtml = (item.foodImage && item.foodImage.trim())
            ? `<img src="${escHtml(item.foodImage)}" class="item-thumb" alt="${escHtml(item.foodName)}" loading="lazy">`
            : `<div class="item-thumb-placeholder"><i class="bi bi-image"></i></div>`;

        const discountHtml = hasDiscount
            ? `<span class="item-price-original">${fmt(item.price)}</span>`
            : '';

        const noteHtml = item.note
            ? `<div class="item-note"><i class="bi bi-chat-left-text me-1"></i>${escHtml(item.note)}</div>`
            : '';

        const unavailableHtml = item.available === false
            ? `<span class="unavailable-badge"><i class="bi bi-x-circle me-1"></i>Hết phục vụ</span>`
            : '';

        return `
            <div class="cart-item" data-cart-item-id="${item.id}" data-food-id="${item.foodId}">
                ${thumbHtml}
                <div class="item-info">
                    <div class="item-name" title="${escHtml(item.foodName)}">${escHtml(item.foodName)}</div>
                    <div class="d-flex align-items-center gap-2 flex-wrap mt-1">
                        <span class="item-price-effective">${fmt(effectivePrice)}</span>
                        ${discountHtml}
                    </div>
                    ${noteHtml}
                    ${unavailableHtml}
                </div>

                <div class="qty-control">
                    <button class="qty-btn qty-minus"
                            data-qty-action="decrease"
                            data-food-id="${item.foodId}"
                            aria-label="Giảm số lượng"
                            title="${Number(item.quantity) === 1 ? 'Xóa món' : 'Giảm số lượng'}">
                        ${Number(item.quantity) === 1 ? '<i class="bi bi-trash3" style="font-size:.8rem;"></i>' : '−'}
                    </button>
                    <span class="qty-num">${item.quantity}</span>
                    <button class="qty-btn qty-plus"
                            data-qty-action="increase"
                            data-food-id="${item.foodId}"
                            aria-label="Tăng số lượng">+</button>
                </div>

                <div class="item-right">
                    <span class="item-subtotal">${fmt(subtotal)}</span>
                    <button class="btn-remove"
                            data-remove-food-id="${item.foodId}"
                            aria-label="Xóa món ${escHtml(item.foodName)}"
                            title="Xóa món này">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>
            </div>`;
    }

    function renderShopGroup(group) {
        const itemsHtml = group.items.map(renderCartItem).join('');
        const groupTotal = group.items.reduce((sum, it) => {
            const ep = Number(it.effectivePrice || it.discountPrice || it.price || 0);
            return sum + ep * Number(it.quantity || 1);
        }, 0);

        return `
            <div class="shop-group" data-merchant-id="${escHtml(String(group.merchantId || ''))}">
                <div class="shop-group-header d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <div class="d-flex align-items-center gap-2">
                        <div class="shop-icon"><i class="bi bi-shop"></i></div>
                        <div>
                            <div class="shop-name">${escHtml(group.merchantName)}</div>
                            <div class="shop-meta">${group.items.length} món · ${fmt(groupTotal)}</div>
                        </div>
                    </div>
                    <a href="/checkout?merchantId=${escHtml(String(group.merchantId || ''))}" class="btn btn-danger btn-sm rounded-pill fw-semibold px-3 py-1.5 shadow-sm">
                        Thanh toán quán này
                    </a>
                </div>
                <div class="cart-items-wrapper">
                    ${itemsHtml}
                </div>
            </div>`;
    }

    function renderSummary(summary) {
        const subtotal = Number(summary.subtotalPrice || 0);
        const serviceFee = Number(summary.serviceFee || 0);
        const estimatedTotal = Number(summary.estimatedTotal || 0);
        const savings = Number(summary.savings || 0);
        const hasUnavailable = summary.hasUnavailableItems === true;

        document.getElementById('summSubtotal').textContent = fmt(subtotal);
        document.getElementById('summServiceFee').textContent = fmt(serviceFee);
        document.getElementById('summTotal').textContent = fmt(estimatedTotal);

        const savingsRow = document.getElementById('summSavingsRow');
        if (savings > 0) {
            document.getElementById('summSavings').textContent = '− ' + fmt(savings);
            savingsRow.classList.remove('d-none');
        } else {
            savingsRow.classList.add('d-none');
        }

        const btnCheckout = document.getElementById('btnCheckout');
        if (btnCheckout) {
            btnCheckout.classList.add('d-none'); // [ĐA QUÁN] Ẩn nút thanh toán tổng
        }
    }

    function updateCartInPlace(summary) {
        const listEl = document.getElementById('shopGroupList');
        if (!listEl) return false;

        // Lấy danh sách ID hiện tại trên DOM
        const currentItemEls = Array.from(listEl.querySelectorAll('.cart-item'));
        const currentFoodIds = currentItemEls.map(el => String(el.getAttribute('data-food-id'))).sort();
        
        // Lấy danh sách ID mới
        const newFoodIds = (summary.items || []).map(item => String(item.foodId)).sort();

        // Nếu số lượng món thay đổi, hoặc danh sách id khác nhau -> render lại toàn bộ
        if (currentFoodIds.length !== newFoodIds.length) return false;
        for (let i = 0; i < currentFoodIds.length; i++) {
            if (currentFoodIds[i] !== newFoodIds[i]) return false;
        }

        // Cập nhật DOM trực tiếp (chỉ gán nếu có thay đổi để tránh repaint/flicker)
        (summary.items || []).forEach(item => {
            const itemEl = listEl.querySelector(`.cart-item[data-food-id="${item.foodId}"]`);
            if (itemEl) {
                // Update số lượng
                const qtyNum = itemEl.querySelector('.qty-num');
                if (qtyNum && qtyNum.textContent != item.quantity) {
                    qtyNum.textContent = item.quantity;
                }
                
                // Update tổng tiền món
                const effectivePrice = Number(item.effectivePrice || item.discountPrice || item.price || 0);
                const subtotal = effectivePrice * Number(item.quantity || 1);
                const subtotalEl = itemEl.querySelector('.item-subtotal');
                const subtotalText = fmt(subtotal);
                if (subtotalEl && subtotalEl.textContent !== subtotalText) {
                    subtotalEl.textContent = subtotalText;
                }
                
                // Update nút trừ (đổi thành icon thùng rác nếu sl = 1)
                const minusBtn = itemEl.querySelector('.qty-minus');
                if (minusBtn) {
                    const newIcon = Number(item.quantity) === 1 
                        ? '<i class="bi bi-trash3" style="font-size:.8rem;"></i>' 
                        : '−';
                    if (minusBtn.innerHTML !== newIcon) {
                        minusBtn.innerHTML = newIcon;
                        minusBtn.setAttribute('title', Number(item.quantity) === 1 ? 'Xóa món' : 'Giảm số lượng');
                    }
                }
            }
        });

        // Cập nhật tổng tiền của từng quán
        const groups = groupByMerchant(summary);
        groups.forEach(group => {
            const groupEl = listEl.querySelector(`.shop-group[data-merchant-id="${escHtml(String(group.merchantId || ''))}"]`);
            if (groupEl) {
                const groupTotal = group.items.reduce((sum, it) => {
                    const ep = Number(it.effectivePrice || it.discountPrice || it.price || 0);
                    return sum + ep * Number(it.quantity || 1);
                }, 0);
                const metaEl = groupEl.querySelector('.shop-meta');
                const newMetaText = `${group.items.length} món · ${fmt(groupTotal)}`;
                if (metaEl && metaEl.textContent !== newMetaText) {
                    metaEl.textContent = newMetaText;
                }
            }
        });

        return true;
    }

    function renderCart(summary) {
        const skeleton = document.getElementById('cartSkeleton');
        const content = document.getElementById('cartContent');
        const empty = document.getElementById('cartEmpty');
        const summSkeleton = document.getElementById('summarySkeleton');
        const summContent = document.getElementById('summaryContent');
        const summEmpty = document.getElementById('summaryEmpty');
        const subtitleEl = document.getElementById('cartSubtitle');

        skeleton?.classList.add('d-none');
        summSkeleton?.classList.add('d-none');

        if (!summary || summary.empty || !summary.items || summary.items.length === 0) {
            // Empty
            content?.classList.add('d-none');
            empty?.classList.remove('d-none');
            summContent?.classList.add('d-none');
            summEmpty?.classList.remove('d-none');
            if (subtitleEl) subtitleEl.textContent = 'Giỏ hàng của bạn đang trống.';
            return;
        }

        // Có món
        empty?.classList.add('d-none');
        content?.classList.remove('d-none');
        summEmpty?.classList.add('d-none');
        summContent?.classList.remove('d-none');

        const totalItems = Number(summary.totalItems || 0);
        if (subtitleEl) {
            subtitleEl.textContent = `${totalItems} món đang chờ bạn đặt hàng.`;
        }

        // Cảnh báo món hết phục vụ
        const alertEl = document.getElementById('unavailableAlert');
        if (summary.hasUnavailableItems) {
            alertEl?.classList.remove('d-none');
        } else {
            alertEl?.classList.add('d-none');
        }

        // Render shop groups: Cố gắng update in-place trước để tránh flicker
        if (!updateCartInPlace(summary)) {
            const groups = groupByMerchant(summary);
            const listEl = document.getElementById('shopGroupList');
            if (listEl) {
                listEl.innerHTML = groups.map(renderShopGroup).join('');
            }
        }

        // Render summary
        renderSummary(summary);
    }

    /* ==================== ACTIONS ==================== */

    let actionInProgress = false;

    async function handleQtyAction(foodId, action) {
        if (actionInProgress) return;
        actionInProgress = true;

        const summary = window.getCartSummary ? window.getCartSummary() : null;
        const currentItem = summary?.items?.find(it => String(it.foodId) === String(foodId));

        if (action === 'decrease' && currentItem && Number(currentItem.quantity) === 1) {
            // Số lượng về 0 → xóa món
            await handleRemove(foodId);
        } else {
            const delta = action === 'increase' ? 1 : -1;
            await window.changeQuantityGlobal(Number(foodId), delta);
        }

        actionInProgress = false;
    }

    async function handleRemove(foodId) {
        // Animation trước khi xóa
        const row = document.querySelector(`[data-food-id="${foodId}"].cart-item`);
        if (row) {
            row.classList.add('removing');
            await new Promise(r => setTimeout(r, 280));
        }
        await window.removeFromCartGlobal(Number(foodId));
    }

    async function handleClearCart() {
        const confirmed = confirm('Bạn có chắc muốn xóa toàn bộ giỏ hàng không?');
        if (!confirmed) return;
        await window.clearGlobalCart();
    }

    /* ==================== EVENT DELEGATION ==================== */

    document.addEventListener('click', async e => {
        // Nút +/-
        const qtyBtn = e.target.closest('[data-qty-action]');
        if (qtyBtn) {
            e.preventDefault();
            qtyBtn.disabled = true;
            const foodId = qtyBtn.getAttribute('data-food-id');
            const action = qtyBtn.getAttribute('data-qty-action');
            await handleQtyAction(foodId, action);
            qtyBtn.disabled = false;
            return;
        }

        // Nút xóa (×)
        const removeBtn = e.target.closest('[data-remove-food-id]');
        if (removeBtn) {
            e.preventDefault();
            removeBtn.disabled = true;
            const foodId = removeBtn.getAttribute('data-remove-food-id');
            await handleRemove(foodId);
            return;
        }

        // Nút xóa toàn bộ
        const clearBtn = e.target.closest('#btnClearCart');
        if (clearBtn) {
            e.preventDefault();
            await handleClearCart();
        }
    });

    /* ==================== INIT ==================== */

    document.addEventListener('DOMContentLoaded', async () => {
        // Chờ cart.js sẵn sàng (nó được load qua navbar fragment)
        // Thử poll tối đa 3 giây
        let retries = 0;
        while (!window.refreshCart && retries < 30) {
            await new Promise(r => setTimeout(r, 100));
            retries++;
        }

        if (window.refreshCart) {
            await window.refreshCart();
        } else {
            // Fallback: gọi API trực tiếp
            console.warn('[cart-page] cart.js chưa load, fallback sang fetch trực tiếp.');
            try {
                const token = localStorage.getItem('accessToken');
                const headers = { 'Accept': 'application/json' };
                if (token) headers['Authorization'] = 'Bearer ' + token;
                const res = await fetch('/api/cart', { headers });
                const data = await res.json();
                if (res.ok && data.success) {
                    renderCart(data.data);
                } else {
                    renderCart(null);
                }
            } catch {
                renderCart(null);
            }
        }
    });

    // Lắng nghe cartUpdated từ cart.js
    window.addEventListener('cartUpdated', e => {
        renderCart(e.detail);
    });

})();
