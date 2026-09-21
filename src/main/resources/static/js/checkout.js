/**
 * Trang Thanh toán Đơn hàng (Checkout)
 * File: static/js/checkout.js
 *
 * Giỏ hàng lấy từ /api/cart. Giá hiển thị ở đây luôn là giá mới nhất trong
 * database, nên số tiền trên màn hình khớp chính xác với số tiền server tính
 * lúc tạo đơn.
 */

let selectedPayment = 'COD';
let appliedVoucher = '';
let discountAmount = 0;
let currentShippingFee = 15000;
let selectedDeliveryPartnerId = null;
let serviceFee = 0;
let subtotalPrice = 0;
let currentMerchantId = null;
let currentCartItems = [];
let currentApplicableCoupons = [];
let currentSelectedAddress = null;
let cartHasUnavailableItems = false;

// Các mã giảm giá server chấp nhận (khớp với UserOrderServiceImpl)
const VOUCHERS = {
    'GIAM10K': { type: 'amount', value: 10000, label: 'Giảm 10.000đ' },
    'GIAM20K': { type: 'amount', value: 20000, label: 'Giảm 20.000đ' },
    'GIAM50K': { type: 'amount', value: 50000, label: 'Giảm 50.000đ' },
    'GIAM10%': { type: 'percent', value: 10, label: 'Giảm 10% tiền món' },
    'GIAM10PT': { type: 'percent', value: 10, label: 'Giảm 10% tiền món' },
    'GIAM20%': { type: 'percent', value: 20, label: 'Giảm 20% tiền món' },
    'GIAM20PT': { type: 'percent', value: 20, label: 'Giảm 20% tiền món' },
    'GIAM50%': { type: 'percent', value: 50, label: 'Giảm 50% tiền món' },
    'GIAM50PT': { type: 'percent', value: 50, label: 'Giảm 50% tiền món' },
    'FREESHIP': { type: 'freeship', value: 0, label: 'Miễn phí vận chuyển' }
};

function formatCurrency(val) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(Number(val) || 0)) + ' đ';
}

// ==================== 1. CHỌN ĐỊA CHỈ ====================

function selectModalAddress(cardEl) {
    document.querySelectorAll('.address-modal-card').forEach(c => {
        c.classList.remove('selected', 'border-danger', 'bg-danger-subtle');
        c.classList.add('border-light-subtle', 'bg-white');
        const radio = c.querySelector('input[type="radio"]');
        if (radio) radio.checked = false;
    });

    cardEl.classList.add('selected', 'border-danger', 'bg-danger-subtle');
    cardEl.classList.remove('border-light-subtle', 'bg-white');

    const r = cardEl.querySelector('input[type="radio"]');
    if (r) r.checked = true;
}

function confirmAddressSelection() {
    const selectedModalCard = document.querySelector('.address-modal-card.selected');

    if (!selectedModalCard) {
        alert('Vui lòng chọn một địa chỉ nhận hàng.');
        return;
    }

    const id = selectedModalCard.getAttribute('data-id');
    const name = selectedModalCard.getAttribute('data-name') || '';
    const phone = selectedModalCard.getAttribute('data-phone') || '';
    const address = selectedModalCard.getAttribute('data-address') || '';
    const note = selectedModalCard.getAttribute('data-note') || '';
    const isDefault = selectedModalCard.getAttribute('data-default') === 'true';

    currentSelectedAddress = { id, name, phone, address, note, isDefault };

    const nameEl = document.getElementById('selectedContactName');
    if (nameEl) nameEl.innerText = name;

    const phoneEl = document.getElementById('selectedContactPhone');
    if (phoneEl) phoneEl.innerText = phone;

    const addrEl = document.getElementById('selectedFullAddress');
    if (addrEl) addrEl.innerText = address;

    const defaultBadge = document.getElementById('selectedDefaultBadge');
    if (defaultBadge) {
        defaultBadge.style.display = isDefault ? 'inline-block' : 'none';
    }

    const noteBox = document.getElementById('selectedNoteContainer');
    const noteText = document.getElementById('selectedNoteText');
    if (noteBox && noteText) {
        if (note && note.trim().length > 0) {
            noteText.innerText = note;
            noteBox.style.display = 'block';
        } else {
            noteBox.style.display = 'none';
        }
    }

    const modalEl = document.getElementById('chooseAddressModal');
    if (modalEl && typeof bootstrap !== 'undefined') {
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
    }

    if (currentMerchantId && id) {
        loadShippingQuotes(currentMerchantId, id);
    }
}

// ==================== 2. BÁO GIÁ VẬN CHUYỂN ====================

async function loadShippingQuotes(merchantId, addressId) {
    const container = document.getElementById('deliveryPartnersContainer');

    if (!container) return;

    container.innerHTML = `
        <div class="text-center py-3 text-muted font-size-13">
            <span class="spinner-border spinner-border-sm text-danger me-2"></span>
            Đang tính phí vận chuyển theo khoảng cách...
        </div>
    `;

    try {
        const fetchFn = (typeof fetchWithAuth === 'function') ? fetchWithAuth : fetch;
        const response = await fetchFn(
            `/api/delivery/quotes?merchantId=${merchantId}&addressId=${addressId}`
        );

        if (response.ok) {
            renderShippingQuotes(await response.json());
        } else {
            renderFallbackDeliveryPartner();
        }
    } catch (e) {
        console.warn('Không tải được báo giá vận chuyển:', e);
        renderFallbackDeliveryPartner();
    }
}

function renderShippingQuotes(quotes) {
    const container = document.getElementById('deliveryPartnersContainer');
    const distanceBadge = document.getElementById('deliveryDistanceText');

    if (!container) return;

    if (!quotes || quotes.length === 0) {
        renderFallbackDeliveryPartner();
        return;
    }

    const dist = quotes[0].distanceKm || 3.0;
    if (distanceBadge) {
        distanceBadge.innerText = `Khoảng cách: ~${dist} km`;
    }

    container.innerHTML = '';

    quotes.forEach((q, index) => {
        const isSelected = index === 0;

        if (isSelected) {
            selectedDeliveryPartnerId = q.partnerId;
            currentShippingFee = q.shippingFee;
        }

        const card = document.createElement('div');
        card.className = `delivery-partner-card d-flex justify-content-between align-items-center ${isSelected ? 'selected' : ''}`;
        card.setAttribute('data-partner-id', q.partnerId);
        card.setAttribute('data-fee', q.shippingFee);
        card.onclick = () => selectDeliveryPartnerCard(card, q.partnerId, q.shippingFee);

        const peakBadge = q.peakHour
            ? `<span class="badge bg-warning-subtle text-warning border border-warning-subtle font-size-11 ms-1.5"><i class="bi bi-lightning-charge-fill me-0.5"></i>Giờ cao điểm</span>`
            : `<span class="badge bg-success-subtle text-success border border-success-subtle font-size-11 ms-1.5"><i class="bi bi-check-circle me-0.5"></i>Tiêu chuẩn</span>`;

        card.innerHTML = `
            <div class="d-flex align-items-center gap-3">
                <div class="bg-light p-2 rounded-3 text-danger fs-4 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                    <i class="bi bi-truck"></i>
                </div>
                <div>
                    <div class="d-flex align-items-center flex-wrap">
                        <strong class="text-dark fs-6">${q.partnerName}</strong>
                        ${peakBadge}
                    </div>
                    <small class="text-muted d-block mt-0.5 font-size-12">
                        <i class="bi bi-clock me-1"></i>Giao trong khoảng ${Math.max(0.5, Math.round(q.distanceKm * 0.5))} phút &bull; ~${q.distanceKm} km
                    </small>
                </div>
            </div>
            <div class="d-flex align-items-center gap-3">
                <strong class="text-danger fs-6">${formatCurrency(q.shippingFee)}</strong>
                <input class="form-check-input" type="radio" name="deliveryPartnerRadio" value="${q.partnerId}" ${isSelected ? 'checked' : ''}>
            </div>
        `;

        container.appendChild(card);
    });

    recalculateVoucher();
    updatePriceSummary();
}

function renderFallbackDeliveryPartner() {
    const container = document.getElementById('deliveryPartnersContainer');
    const distanceBadge = document.getElementById('deliveryDistanceText');

    if (distanceBadge) distanceBadge.innerText = 'Khoảng cách: ~3.0 km';
    if (!container) return;

    currentShippingFee = 15000;
    selectedDeliveryPartnerId = null;

    container.innerHTML = `
        <div class="delivery-partner-card selected d-flex justify-content-between align-items-center">
            <div class="d-flex align-items-center gap-3">
                <div class="bg-light p-2 rounded-3 text-danger fs-4 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                    <i class="bi bi-truck"></i>
                </div>
                <div>
                    <strong class="text-dark fs-6">Giao hàng tiêu chuẩn MealChoice</strong>
                    <small class="text-muted d-block mt-0.5 font-size-12">
                        <i class="bi bi-clock me-1"></i>Giao trong khoảng 12 phút
                    </small>
                </div>
            </div>
            <div class="d-flex align-items-center gap-3">
                <strong class="text-danger fs-6">15.000 đ</strong>
                <input class="form-check-input" type="radio" name="deliveryPartnerRadio" checked>
            </div>
        </div>
    `;

    recalculateVoucher();
    updatePriceSummary();
}

function selectDeliveryPartnerCard(cardEl, partnerId, fee) {
    document.querySelectorAll('.delivery-partner-card').forEach(c => {
        c.classList.remove('selected');
        const radio = c.querySelector('input[type="radio"]');
        if (radio) radio.checked = false;
    });

    cardEl.classList.add('selected');
    const r = cardEl.querySelector('input[type="radio"]');
    if (r) r.checked = true;

    selectedDeliveryPartnerId = partnerId;
    currentShippingFee = fee;

    recalculateVoucher();
    updatePriceSummary();
}

// ==================== 3. PHƯƠNG THỨC THANH TOÁN ====================

function selectPaymentMethod(cardEl, method) {
    if (cardEl.classList.contains('disabled-payment-method')) {
        return;
    }

    selectedPayment = method;

    document.querySelectorAll('.payment-card').forEach(c => {
        c.classList.remove('selected');
        const radio = c.querySelector('input[type="radio"]');
        if (radio) radio.checked = false;
    });

    cardEl.classList.add('selected');
    const r = cardEl.querySelector('input[type="radio"]');
    if (r) r.checked = true;

    const bankBox = document.getElementById('bankTransferInfoBox');
    if (bankBox) {
        bankBox.style.display = (method === 'CARD') ? 'block' : 'none';
    }
}

function copyBankAccount(event) {
    if (event) event.stopPropagation();

    const acc = document.getElementById('checkoutMerchantBankAccount')?.innerText;
    if (!acc) return;

    navigator.clipboard.writeText(acc)
        .then(() => alert('Đã sao chép số tài khoản: ' + acc))
        .catch(() => alert('Số tài khoản: ' + acc));
}

function updateMerchantBankInfo(summary) {
    const bankName = (summary.merchantBankName || '').trim();
    const bankAccount = (summary.merchantBankAccountNumber || '').trim();
    const hasBank = bankName.length > 0 && bankAccount.length > 0;

    const cardEl = document.getElementById('paymentMethodCARD');
    const cardRadio = document.getElementById('paymentRadioCARD');
    const notSupportedNotice = document.getElementById('bankNotSupportedNotice');
    const bankBox = document.getElementById('bankTransferInfoBox');

    if (!hasBank) {
        // Quán chưa khai báo tài khoản ngân hàng: chỉ cho thanh toán COD
        cardEl?.classList.add('disabled-payment-method');
        if (cardRadio) cardRadio.disabled = true;
        if (notSupportedNotice) notSupportedNotice.style.display = 'block';
        if (bankBox) bankBox.style.display = 'none';

        if (selectedPayment === 'CARD') {
            const codCard = document.getElementById('paymentMethodCOD');
            if (codCard) selectPaymentMethod(codCard, 'COD');
        }
        return;
    }

    cardEl?.classList.remove('disabled-payment-method');
    if (cardRadio) cardRadio.disabled = false;
    if (notSupportedNotice) notSupportedNotice.style.display = 'none';

    const bankNameEl = document.getElementById('checkoutMerchantBankName');
    if (bankNameEl) bankNameEl.innerText = bankName;

    const bankAccEl = document.getElementById('checkoutMerchantBankAccount');
    if (bankAccEl) bankAccEl.innerText = bankAccount;

    const ownerEl = document.getElementById('checkoutMerchantOwnerName');
    if (ownerEl) ownerEl.innerText = summary.merchantName || 'Cửa hàng';

    if (selectedPayment === 'CARD' && bankBox) {
        bankBox.style.display = 'block';
    }
}

// ==================== 4. TẢI GIỎ HÀNG TỪ SERVER ====================

async function loadCheckoutCart() {
    const itemsList = document.getElementById('checkoutItemsList');
    const itemCount = document.getElementById('checkoutItemCount');
    const merchantTitle = document.getElementById('checkoutMerchantName');

    if (!itemsList) return;

    itemsList.innerHTML = `
        <div class="text-center py-4 text-muted font-size-13">
            <span class="spinner-border spinner-border-sm text-danger me-2"></span>
            Đang tải giỏ hàng...
        </div>
    `;

    const summary = (typeof refreshCart === 'function')
        ? await refreshCart()
        : null;

    const urlParams = new URLSearchParams(window.location.search);
    const targetMerchantId = urlParams.get('merchantId');

    if (summary && summary.items && targetMerchantId) {
        summary.items = summary.items.filter(item => String(item.merchantId) === targetMerchantId);

        // Recalculate summary totals
        summary.subtotalPrice = summary.items.reduce((sum, item) => sum + Number(item.subtotal || 0), 0);
        summary.serviceFee = summary.items.reduce((max, item) => Math.max(max, Number(item.serviceFee || 0)), 0);
        summary.totalItems = summary.items.reduce((sum, item) => sum + Number(item.quantity || 1), 0);
        summary.hasUnavailableItems = summary.items.some(item => !item.available);
        summary.empty = summary.items.length === 0;

        if (summary.items.length > 0) {
            summary.merchantName = summary.items[0].merchantName;
            summary.merchantId = targetMerchantId;
        }
    }

    if (!summary || summary.empty) {
        itemsList.innerHTML = `
            <div class="text-center py-4 text-muted">
                <i class="bi bi-cart-x fs-2 d-block text-secondary opacity-50 mb-2"></i>
                <p class="mb-2 fw-medium">Không có món ăn nào của quán này trong giỏ hàng</p>
                <a href="/cart" class="btn btn-outline-danger btn-sm rounded-pill px-3">Quay lại giỏ hàng</a>
            </div>
        `;

        if (itemCount) itemCount.innerText = '0 món';

        subtotalPrice = 0;
        serviceFee = 0;
        cartHasUnavailableItems = false;

        updatePriceSummary();
        return;
    }

    if (!summary.merchantId && summary.items && summary.items.length > 0) {
        summary.merchantId = summary.items[0].merchantId;
        if (!summary.merchantName) {
            summary.merchantName = summary.items[0].merchantName;
        }
    }

    currentMerchantId = summary.merchantId;
    currentCartItems = summary.items || [];
    cartHasUnavailableItems = !!summary.hasUnavailableItems;

    if (merchantTitle) {
        merchantTitle.innerText = summary.merchantName || 'Cửa hàng';
    }

    updateMerchantBankInfo(summary);

    // Render danh sách món (nhóm theo quán)
    itemsList.innerHTML = '';

    const groups = new Map();
    summary.items.forEach(item => {
        const key = item.merchantId || 'unknown';
        if (!groups.has(key)) {
            groups.set(key, { merchantName: item.merchantName || 'Cửa hàng', items: [] });
        }
        groups.get(key).items.push(item);
    });

    groups.forEach((group, merchantId) => {
        if (groups.size > 1) {
            const header = document.createElement('div');
            header.className = 'fw-bold text-danger font-size-13 py-2 mt-2 border-bottom';
            header.innerHTML = `<i class="bi bi-shop me-1"></i>${group.merchantName}`;
            itemsList.appendChild(header);
        }

        group.items.forEach(item => {
            const div = document.createElement('div');
            div.className = 'd-flex justify-content-between align-items-center py-2.5 border-bottom border-light';

            const warning = item.available
                ? ''
                : `<div class="text-danger font-size-11 mt-0.5">
                       <i class="bi bi-exclamation-triangle me-1"></i>${item.unavailableReason || 'Không còn phục vụ'}
                   </div>`;

            const noteLine = item.note
                ? `<div class="text-muted font-size-11 fst-italic mt-0.5">${item.note}</div>`
                : '';

            div.innerHTML = `
                <div class="d-flex align-items-center gap-2.5 overflow-hidden">
                    <img src="${item.foodImage || '/images/default-food.svg'}"
                         class="food-item-img border"
                         alt="${item.foodName}"
                         data-fallback="/images/default-food.svg">
                    <div class="fw-bold text-dark fs-7">${item.quantity}x</div>
                    <div class="overflow-hidden">
                        <div class="fw-semibold text-dark fs-7 text-truncate" title="${item.foodName}">${item.foodName}</div>
                        <small class="text-muted font-size-12">${formatCurrency(item.effectivePrice)}</small>
                        ${noteLine}
                        ${warning}
                    </div>
                </div>
                <div class="fw-bold text-dark font-size-14 text-nowrap">${formatCurrency(item.subtotal)}</div>
            `;

            itemsList.appendChild(div);
        });
    });

    if (itemCount) {
        itemCount.innerText = `${summary.totalItems} món`;
    }

    subtotalPrice = Number(summary.subtotalPrice || 0);
    serviceFee = Number(summary.serviceFee || 0);

    // Có món hết phục vụ thì chặn đặt hàng
    const placeOrderBtn = document.getElementById('btnPlaceOrder');
    if (placeOrderBtn) {
        if (cartHasUnavailableItems) {
            placeOrderBtn.disabled = true;
            placeOrderBtn.innerHTML =
                '<i class="bi bi-exclamation-triangle me-2"></i>Bỏ món hết phục vụ để tiếp tục';
        } else {
            placeOrderBtn.disabled = false;
            placeOrderBtn.innerHTML = '<i class="bi bi-bag-check-fill me-2"></i>Đặt hàng';
        }
    }

    if (currentSelectedAddress && currentSelectedAddress.id && currentMerchantId) {
        loadShippingQuotes(currentMerchantId, currentSelectedAddress.id);
    }

    await loadApplicableCoupons(currentMerchantId, currentCartItems);
    recalculateVoucher();
    updatePriceSummary();
}

// ==================== 5. VOUCHER & TỔNG TIỀN ====================

async function loadApplicableCoupons(merchantId, items) {
    const selectEl = document.getElementById('voucherSelect');
    if (!selectEl) return;

    if (!merchantId && items && items.length > 0) {
        merchantId = items.find(i => i.merchantId)?.merchantId;
    }

    if (!merchantId || !items || items.length === 0) {
        selectEl.innerHTML = '<option value="">-- Không có mã ưu đãi nào --</option>';
        currentApplicableCoupons = [];
        return;
    }

    const foodIds = items.map(it => (it.foodId != null ? it.foodId : it.id)).filter(Boolean);
    const url = `/api/checkout/applicable-coupons?merchantId=${encodeURIComponent(merchantId)}&foodIds=${foodIds.join(',')}`;

    try {
        const res = (typeof fetchWithCheckoutAuth === 'function')
            ? await fetchWithCheckoutAuth(url)
            : await fetch(url, { headers: { 'Accept': 'application/json' } });

        if (!res.ok) {
            selectEl.innerHTML = '<option value="">-- Không có mã ưu đãi khả dụng --</option>';
            currentApplicableCoupons = [];
            return;
        }

        const coupons = await res.json();
        currentApplicableCoupons = Array.isArray(coupons) ? coupons : [];

        selectEl.innerHTML = '';

        if (currentApplicableCoupons.length === 0) {
            selectEl.innerHTML = '<option value="">-- Quán không có mã giảm giá cho các món này --</option>';
            appliedVoucher = '';
            discountAmount = 0;
            const msgEl = document.getElementById('voucherMessage');
            if (msgEl) msgEl.innerText = '';
            return;
        }

        const defaultOpt = document.createElement('option');
        defaultOpt.value = '';
        defaultOpt.textContent = '-- Chọn 1 mã ưu đãi của quán --';
        selectEl.appendChild(defaultOpt);

        currentApplicableCoupons.forEach(cp => {
            const opt = document.createElement('option');
            opt.value = cp.couponCode;
            opt.textContent = cp.displayText || `${cp.couponCode} - ${cp.label}`;
            if (appliedVoucher && appliedVoucher === cp.couponCode) {
                opt.selected = true;
            }
            selectEl.appendChild(opt);
        });

        if (appliedVoucher && !currentApplicableCoupons.some(c => c.couponCode === appliedVoucher)) {
            appliedVoucher = '';
            discountAmount = 0;
            const msgEl = document.getElementById('voucherMessage');
            if (msgEl) msgEl.innerText = '';
        }
    } catch (err) {
        console.error('Lỗi khi tải mã giảm giá của quán:', err);
        selectEl.innerHTML = '<option value="">-- Không thể tải mã ưu đãi --</option>';
    }
}

function recalculateVoucher() {
    if (!appliedVoucher) {
        discountAmount = 0;
        return;
    }

    const coupon = currentApplicableCoupons.find(c => c.couponCode === appliedVoucher);
    if (!coupon) {
        if (appliedVoucher === 'FREESHIP') {
            discountAmount = currentShippingFee;
        } else {
            discountAmount = 0;
        }
        return;
    }

    // Tính tổng tiền các món trong giỏ hàng được áp dụng coupon này
    let eligibleSubtotal = 0;
    const applicableIds = Array.isArray(coupon.applicableFoodIds) ? coupon.applicableFoodIds : [];

    if (applicableIds.length === 0) {
        // Áp dụng cho toàn quán
        eligibleSubtotal = subtotalPrice;
    } else {
        const idSet = new Set(applicableIds.map(Number));
        currentCartItems.forEach(it => {
            const fId = Number(it.foodId != null ? it.foodId : it.id);
            if (idSet.has(fId)) {
                eligibleSubtotal += Number(it.subtotal || 0);
            }
        });
    }

    if (eligibleSubtotal <= 0) {
        discountAmount = 0;
        return;
    }

    if (coupon.discountType === 'PERCENT') {
        discountAmount = Math.round(eligibleSubtotal * Number(coupon.discountValue) / 100);
    } else if (coupon.discountType === 'FIXED') {
        discountAmount = Math.min(eligibleSubtotal, Number(coupon.discountValue));
    }

    const ceiling = subtotalPrice + currentShippingFee;
    if (discountAmount > ceiling) {
        discountAmount = ceiling;
    }
}

function removeCheckoutItem(foodId) {
    if (typeof window.changeQuantityGlobal === 'function') {
        const cart = (typeof getGlobalCart === 'function') ? getGlobalCart() : [];
        const it = cart.find(i => i.id === foodId);
        if (it) {
            window.changeQuantityGlobal(foodId, -it.quantity);
        }
    } else {
        let cart = [];
        try {
            cart = JSON.parse(localStorage.getItem('mealchoice_cart') || '[]');
        } catch (_) { }
        cart = cart.filter(i => i.id !== foodId);
        localStorage.setItem('mealchoice_cart', JSON.stringify(cart));
        window.dispatchEvent(new Event('cartUpdated'));
    }
    loadCheckoutCart();
}

function updateCheckoutItemQty(foodId, delta) {
    if (typeof window.changeQuantityGlobal === 'function') {
        window.changeQuantityGlobal(foodId, delta);
    } else {
        let cart = [];
        try {
            cart = JSON.parse(localStorage.getItem('mealchoice_cart') || '[]');
        } catch (_) { }
        const it = cart.find(i => i.id === foodId);
        if (it) {
            it.quantity += delta;
            if (it.quantity <= 0) {
                cart = cart.filter(i => i.id !== foodId);
            }
        }
        localStorage.setItem('mealchoice_cart', JSON.stringify(cart));
        window.dispatchEvent(new Event('cartUpdated'));
    }
    loadCheckoutCart();
}

window.removeCheckoutItem = removeCheckoutItem;
window.updateCheckoutItemQty = updateCheckoutItemQty;

function updatePriceSummary() {
    const total = subtotalPrice + currentShippingFee + serviceFee - discountAmount;
    const subEl = document.getElementById('summarySubtotal');
    if (subEl) subEl.innerText = formatCurrency(subtotalPrice);
    const shipEl = document.getElementById('summaryShippingFee');
    if (shipEl) shipEl.innerText = formatCurrency(currentShippingFee);
    const srvEl = document.getElementById('summaryServiceFee');
    if (srvEl) srvEl.innerText = formatCurrency(serviceFee);
    const totalEl = document.getElementById('summaryTotalAmount');
    if (totalEl) totalEl.innerText = formatCurrency(Math.max(0, total));

    const discRow = document.getElementById('summaryDiscountRow');
    if (discRow) {
        if (discountAmount > 0) {
            discRow.style.setProperty('display', 'flex', 'important');
            const discEl = document.getElementById('summaryDiscount');
            if (discEl) discEl.innerText = `- ${formatCurrency(discountAmount)}`;
        } else {
            discRow.style.setProperty('display', 'none', 'important');
        }
    }
}

// ==================== 6. KHỞI TẠO TRANG ====================

// ==================== QUẢN LÝ ĐỊA CHỈ HÀNH CHÍNH (TỈNH/HUYỆN/XÃ) ====================
const PROVINCES_API = "https://provinces.open-api.vn/api/";
let cachedProvinces = null;

async function getProvinces() {
    if (cachedProvinces && cachedProvinces.length > 0) {
        return cachedProvinces;
    }
    try {
        const res = await fetch(`${PROVINCES_API}?depth=1`);
        if (res.ok) {
            cachedProvinces = await res.json();
            return cachedProvinces;
        }
    } catch (e) {
        console.warn("Lỗi tải tỉnh thành từ API:", e);
    }
    return [];
}

async function getDistrictsByProvinceCode(provCode) {
    if (!provCode) return [];
    try {
        const res = await fetch(`${PROVINCES_API}p/${provCode}?depth=2`);
        if (res.ok) {
            const data = await res.json();
            return data.districts || [];
        }
    } catch (e) {
        console.warn("Lỗi tải quận huyện từ API:", e);
    }
    return [];
}

async function getWardsByDistrictCode(distCode) {
    if (!distCode) return [];
    try {
        const res = await fetch(`${PROVINCES_API}d/${distCode}?depth=2`);
        if (res.ok) {
            const data = await res.json();
            return data.wards || [];
        }
    } catch (e) {
        console.warn("Lỗi tải phường xã từ API:", e);
    }
    return [];
}

function setupCheckoutAddressSelectListeners() {
    const citySelect = document.getElementById('modalCity');
    const districtSelect = document.getElementById('modalDistrict');
    const wardSelect = document.getElementById('modalWard');
    const modalEl = document.getElementById('newAddressModal');

    // Nạp tỉnh thành khi mở modal
    if (modalEl) {
        modalEl.addEventListener('show.bs.modal', async () => {
            const contactNameInput = document.getElementById('modalContactName');
            const contactPhoneInput = document.getElementById('modalContactPhone');
            if (contactNameInput && !contactNameInput.value) {
                contactNameInput.value = localStorage.getItem('displayName') || '';
            }

            if (citySelect && citySelect.options.length <= 1) {
                citySelect.innerHTML = '<option value="" selected disabled>Đang tải Tỉnh/Thành phố...</option>';
                const provinces = await getProvinces();
                let cityHtml = '<option value="" selected disabled>Tỉnh/Thành phố</option>';
                provinces.forEach(p => {
                    cityHtml += `<option value="${p.name}" data-code="${p.code}">${p.name}</option>`;
                });
                citySelect.innerHTML = cityHtml;
            }
        });
    }

    if (citySelect) {
        citySelect.addEventListener('change', async function () {
            const selectedOpt = citySelect.options[citySelect.selectedIndex];
            const provCode = selectedOpt ? selectedOpt.getAttribute('data-code') : null;

            districtSelect.innerHTML = '<option value="" selected disabled>Đang tải Quận/Huyện...</option>';
            wardSelect.innerHTML = '<option value="" selected disabled>Phường/Xã</option>';
            districtSelect.disabled = true;
            wardSelect.disabled = true;

            if (!provCode) {
                districtSelect.innerHTML = '<option value="" selected disabled>Quận/Huyện</option>';
                return;
            }

            const districts = await getDistrictsByProvinceCode(provCode);
            let distHtml = '<option value="" selected disabled>Quận/Huyện</option>';
            districts.forEach(d => {
                distHtml += `<option value="${d.name}" data-code="${d.code}">${d.name}</option>`;
            });
            districtSelect.innerHTML = distHtml;
            districtSelect.disabled = false;
        });
    }

    if (districtSelect) {
        districtSelect.addEventListener('change', async function () {
            const selectedOpt = districtSelect.options[districtSelect.selectedIndex];
            const distCode = selectedOpt ? selectedOpt.getAttribute('data-code') : null;

            wardSelect.innerHTML = '<option value="" selected disabled>Đang tải Phường/Xã...</option>';
            wardSelect.disabled = true;

            if (!distCode) {
                wardSelect.innerHTML = '<option value="" selected disabled>Phường/Xã</option>';
                return;
            }

            const wards = await getWardsByDistrictCode(distCode);
            let wardHtml = '<option value="" selected disabled>Phường/Xã</option>';
            wards.forEach(w => {
                wardHtml += `<option value="${w.name}" data-code="${w.code}">${w.name}</option>`;
            });
            wardSelect.innerHTML = wardHtml;
            wardSelect.disabled = false;
        });
    }
}

function getCheckoutAuthHeaders(extraHeaders = {}) {
    const token = localStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        ...extraHeaders
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

async function fetchWithCheckoutAuth(url, options = {}) {
    options.headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...getCheckoutAuthHeaders(),
        ...options.headers
    };
    if (typeof window.fetchWithAuth === 'function') {
        return window.fetchWithAuth(url, options);
    }
    return fetch(url, options);
}

document.addEventListener('DOMContentLoaded', () => {

    // Địa chỉ đang chọn – đọc từ card địa chỉ mặc định đã được render sẵn bởi Thymeleaf
    const initialSelectedCard =
        document.querySelector('.address-modal-card.selected') ||
        document.querySelector('.address-modal-card');

    // Khởi tạo currentSelectedAddress từ card địa chỉ mặc định ngay khi trang load
    // để loadCheckoutCart() có thể tự động gọi loadShippingQuotes() mà không cần
    // user phải bấm chọn lại địa chỉ một lần nữa.
    if (initialSelectedCard) {
        const id      = initialSelectedCard.getAttribute('data-id');
        const name    = initialSelectedCard.getAttribute('data-name')    || '';
        const phone   = initialSelectedCard.getAttribute('data-phone')   || '';
        const address = initialSelectedCard.getAttribute('data-address') || '';
        const note    = initialSelectedCard.getAttribute('data-note')    || '';
        const isDefault = initialSelectedCard.getAttribute('data-default') === 'true';

        if (id && address) {
            currentSelectedAddress = { id, name, phone, address, note, isDefault };
        }
    }

    // Khởi tạo listeners cho dropdown địa chỉ hành chính
    setupCheckoutAddressSelectListeners();

    loadCheckoutCart();

    // ---------- Áp dụng voucher (Select Option - chỉ chọn 1) ----------
    const voucherSelect = document.getElementById('voucherSelect');
    voucherSelect?.addEventListener('change', (e) => {
        const code = e.target.value ? e.target.value.trim().toUpperCase() : '';
        const msgEl = document.getElementById('voucherMessage');

        if (!code) {
            appliedVoucher = '';
            discountAmount = 0;
            if (msgEl) {
                msgEl.className = 'font-size-12 mt-1';
                msgEl.innerHTML = '';
            }
            updatePriceSummary();
            return;
        }

        const coupon = currentApplicableCoupons.find(c => c.couponCode === code);
        if (!coupon && code !== 'FREESHIP') {
            appliedVoucher = '';
            discountAmount = 0;
            if (msgEl) {
                msgEl.className = 'font-size-12 mt-1 text-danger';
                msgEl.innerText = 'Mã này không khả dụng cho các món trong giỏ hàng.';
            }
            updatePriceSummary();
            return;
        }

        appliedVoucher = code;
        recalculateVoucher();

        if (msgEl) {
            msgEl.className = 'font-size-12 mt-1 text-success fw-medium';
            const foodText = (coupon && coupon.applicableFoodNames && coupon.applicableFoodNames.length > 0)
                ? ` (Áp dụng cho: <strong>${coupon.applicableFoodNames.join(', ')}</strong>)`
                : '';
            msgEl.innerHTML = `<i class="bi bi-check-circle-fill me-1"></i>Đã áp dụng <strong>${code}</strong>: ${coupon ? coupon.label : ''}${foodText}`;
        }

        updatePriceSummary();
    });

    // ---------- Lưu địa chỉ mới ----------
    let isSavingAddress = false;
    document.getElementById('btnSaveAddress')?.addEventListener('click', async () => {
        if (isSavingAddress) return;

        const contactName = document.getElementById('modalContactName')?.value.trim();
        const contactPhone = document.getElementById('modalContactPhone')?.value.trim();
        const city = document.getElementById('modalCity')?.value;
        const district = document.getElementById('modalDistrict')?.value;
        const ward = document.getElementById('modalWard')?.value;
        const street = document.getElementById('modalStreet')?.value.trim();
        const note = document.getElementById('modalNote')?.value.trim();
        const isDefault = document.getElementById('modalIsDefault')?.checked ?? true;

        if (!contactName || !contactPhone) {
            alert('Vui lòng nhập tên người nhận và số điện thoại');
            return;
        }

        if (!city || !district || !ward) {
            alert('Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã');
            return;
        }

        if (!street) {
            alert('Vui lòng nhập địa chỉ chi tiết (số nhà, ngõ/đường)');
            return;
        }

        if (!/^(0|\+84)[0-9]{9,10}$/.test(contactPhone)) {
            alert('Số điện thoại chưa đúng định dạng. Ví dụ: 0987654321');
            return;
        }

        const saveBtn = document.getElementById('btnSaveAddress');
        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
        }

        isSavingAddress = true;

        try {
            const payload = {
                contactName,
                contactPhone,
                city,
                district,
                ward,
                street,
                note: note || null,
                isDefault: Boolean(isDefault)
            };

            const response = await fetchWithCheckoutAuth('/api/user/address', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                location.reload();
            } else {
                const err = await response.json().catch(() => ({}));
                alert(err.message || 'Không lưu được địa chỉ.');
                if (saveBtn) {
                    saveBtn.disabled = false;
                    saveBtn.innerHTML = 'Lưu địa chỉ';
                }
                isSavingAddress = false;
            }
        } catch (e) {
            console.error('Lỗi lưu địa chỉ:', e);
            alert('Không kết nối được tới máy chủ.');
            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.innerHTML = 'Lưu địa chỉ';
            }
            isSavingAddress = false;
        }
    });

    // ---------- Đặt hàng ----------
    document.getElementById('btnPlaceOrder')?.addEventListener('click', async () => {

        if (!currentSelectedAddress || !currentSelectedAddress.address) {
            alert('Chọn địa chỉ giao hàng trước khi đặt món.');
            return;
        }

        const summary = (typeof getCartSummary === 'function') ? getCartSummary() : null;

        if (!summary || summary.empty) {
            alert('Giỏ hàng đang trống.');
            return;
        }

        if (summary.hasUnavailableItems) {
            alert('Giỏ hàng có món không còn phục vụ. Bỏ món đó ra trước khi đặt.');
            return;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const targetMerchantId = urlParams.get('merchantId') || currentMerchantId;

        if (!targetMerchantId) {
            alert('Không thể xác định thông tin Cửa hàng đối tác cho giỏ hàng này. Vui lòng tải lại trang và thử lại!');
            return;
        }

        const payload = {
            merchantId: targetMerchantId,
            deliveryPartnerId: selectedDeliveryPartnerId,
            addressId: currentSelectedAddress.id ? Number(currentSelectedAddress.id) : null,
            contactName: currentSelectedAddress.name,
            contactPhone: currentSelectedAddress.phone,
            deliveryAddress: currentSelectedAddress.address,
            note: document.getElementById('orderNoteInput')?.value.trim() || '',
            paymentMethod: selectedPayment,
            voucherCode: appliedVoucher
        };

        const btn = document.getElementById('btnPlaceOrder');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';
        }

        try {
            const response = await fetchWithCheckoutAuth('/api/checkout/place-order', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            const result = await response.json().catch(() => ({}));

            if (result.success) {
                if (typeof refreshCart === 'function') {
                    await refreshCart();
                }

                window.location.href = `/checkout/success?orderCode=${result.orderCode}`;
            } else {
                alert(result.message || 'Đặt hàng chưa thành công. Vui lòng thử lại.');
                if (btn) {
                    btn.disabled = false;
                    btn.innerHTML = '<i class="bi bi-bag-check-fill me-2"></i>Đặt hàng';
                }
            }
        } catch (e) {
            console.error('Lỗi đặt hàng:', e);
            alert('Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-bag-check-fill me-2"></i>Đặt hàng';
            }
        }
    });
});