/**
 * Quản lý danh sách đơn hàng của Merchant
 * File: static/js/merchant-order-list.js
 */

let currentCancelOrderId = null;
let cancelModalInstance = null;
let currentRejectOrderId = null;
let rejectModalInstance = null;

const activeTimers = new Map();

document.addEventListener('DOMContentLoaded', () => {
    const cancelModalEl = document.getElementById('cancelOrderModal');
    if (cancelModalEl && typeof bootstrap !== 'undefined') {
        cancelModalInstance = new bootstrap.Modal(cancelModalEl);
    }

    const rejectModalEl = document.getElementById('rejectDeliveryModal');
    if (rejectModalEl && typeof bootstrap !== 'undefined') {
        rejectModalInstance = new bootstrap.Modal(rejectModalEl);
    }

    const refreshBtn = document.querySelector('button[onclick="location.reload()"]');

    if (refreshBtn) {
        refreshBtn.removeAttribute('onclick');

        refreshBtn.addEventListener('click', () => {
            window.location.href = '/merchant/orders?page=0';
        });
    }

    document.getElementById('btnConfirmCancelOrder')?.addEventListener('click', () => {
        if (!currentCancelOrderId) return;
        const reason = document.getElementById('cancelReasonInput')?.value || '';
        executeCancelOrder(currentCancelOrderId, reason);
    });

    document.getElementById('btnConfirmRejectDelivery')?.addEventListener('click', () => {
        if (!currentRejectOrderId) return;
        const reason = document.getElementById('rejectReasonInput')?.value || '';
        executeRejectDelivery(currentRejectOrderId, reason);
    });

    initAllTimers();

    // Đồng bộ tìm kiếm và bộ lọc với URL
    const searchForm = document.querySelector('form[action="/merchant/orders"]');

    searchForm?.addEventListener('submit', function (e) {
        e.preventDefault();

        const keyword = this.querySelector('input[name="keyword"]')?.value.trim() || '';
        const status = this.querySelector('input[name="status"]')?.value || '';
        const params = new URLSearchParams();

        if (keyword) params.set('keyword', keyword);
        if (status) params.set('status', status);
        params.set('page', '0');

        window.location.href = `/merchant/orders?${params.toString()}`;
    });

    // Khi đổi trạng thái thì giữ lại từ khóa tìm kiếm
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            const url = new URL(this.href, window.location.origin);
            const keyword = searchForm?.querySelector('input[name="keyword"]')?.value.trim() || '';

            if (keyword) {
                url.searchParams.set('keyword', keyword);
            } else {
                url.searchParams.delete('keyword');
            }

            url.searchParams.set('page', '0');
            window.location.href = url.toString();
        });
    });
});

// Làm mới danh sách và xóa tìm kiếm, bộ lọc
function refreshOrderList() {
    window.location.href = '/merchant/orders?page=0';
}

// Quản lý timer
function initAllTimers() {
    document.querySelectorAll('.prep-timer').forEach(el => {
        const orderId = el.dataset.orderId;
        const remaining = parseInt(el.dataset.remaining, 10);

        if (orderId && !isNaN(remaining) && remaining > 0) {
            startPrepCountdown(orderId, remaining);
        }
    });

    document.querySelectorAll('.delivery-timer').forEach(el => {
        const orderId = el.dataset.orderId;
        const remaining = parseInt(el.dataset.remaining, 10);

        if (orderId && !isNaN(remaining)) {
            startDeliveryCountdown(orderId, remaining);
        }
    });
}

function formatTime(seconds) {
    if (seconds <= 0) return '00:00';

    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;

    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// Đếm ngược thời gian chuẩn bị
function startPrepCountdown(orderId, initialSeconds) {
    const timerKey = `prep_${orderId}`;

    if (activeTimers.has(timerKey)) {
        clearInterval(activeTimers.get(timerKey));
    }

    let remaining = initialSeconds;
    const timerEl = document.querySelector(
        `.prep-timer[data-order-id="${orderId}"]`
    );

    const updateDisplay = () => {
        if (!timerEl) return;

        const display = timerEl.querySelector('.timer-display');
        if (display) {
            display.textContent = formatTime(remaining);
        }
    };

    updateDisplay();

    const intervalId = setInterval(() => {
        remaining--;

        if (remaining <= 0) {
            clearInterval(intervalId);
            activeTimers.delete(timerKey);
            transitionToDelivering(orderId, 15 * 60);
            return;
        }

        updateDisplay();
    }, 1000);

    activeTimers.set(timerKey, intervalId);
}

// Đếm ngược thời gian giao hàng
function startDeliveryCountdown(orderId, initialSeconds) {
    const timerKey = `delivery_${orderId}`;

    if (activeTimers.has(timerKey)) {
        clearInterval(activeTimers.get(timerKey));
    }

    let remaining = initialSeconds;
    const timerEl = document.querySelector(
        `.delivery-timer[data-order-id="${orderId}"]`
    );
    const completeBtn = document.getElementById(`btn-complete-${orderId}`);

    const updateDisplay = () => {
        if (!timerEl) return;

        const display = timerEl.querySelector('.timer-display');
        if (display) {
            display.textContent = formatTime(remaining);
        }
    };

    if (remaining > 0) {
        updateDisplay();

        const intervalId = setInterval(() => {
            remaining--;

            if (remaining <= 0) {
                clearInterval(intervalId);
                activeTimers.delete(timerKey);
                enableCompleteButton(orderId);
                return;
            }

            updateDisplay();
        }, 1000);

        activeTimers.set(timerKey, intervalId);
    } else {
        enableCompleteButton(orderId);
    }
}

// Kích hoạt nút hoàn thành
function enableCompleteButton(orderId) {
    const timerEl = document.querySelector(
        `.delivery-timer[data-order-id="${orderId}"]`
    );

    if (timerEl) {
        timerEl.innerHTML = `
            <i class="bi bi-geo-alt-fill text-success me-1"></i>
            <span class="text-success fw-bold">Đã đến nơi</span>
        `;
    }

    const completeBtn = document.getElementById(`btn-complete-${orderId}`);

    if (completeBtn) {
        completeBtn.disabled = false;
        completeBtn.title = 'Xác nhận đã nhận tiền và giao xong';
    }

    const rejectBtn = document.getElementById(`btn-reject-${orderId}`);

    if (rejectBtn) {
        rejectBtn.classList.remove('d-none');
    }
}

// Chuyển đơn sang trạng thái đang giao
function transitionToDelivering(orderId, deliverySeconds = 900) {
    const statusCol = document.getElementById(`status-container-${orderId}`);

    if (statusCol) {
        statusCol.innerHTML = `
            <span class="badge badge-status bg-info text-white">
                Đang giao
            </span>
            <small class="text-info font-monospace fw-bold font-size-11 delivery-timer"
                   data-order-id="${orderId}"
                   data-remaining="${deliverySeconds}">
                <i class="bi bi-truck me-1"></i>
                <span class="timer-display">--:--</span>
            </small>
        `;
    }

    const actionCol = document.getElementById(`action-container-${orderId}`);

    if (actionCol) {
        actionCol.innerHTML = `
            <button type="button"
                    class="btn btn-sm btn-success rounded-pill px-3 fw-semibold font-size-12 shadow-sm btn-complete-order"
                    id="btn-complete-${orderId}"
                    disabled
                    onclick="handleCompleteOrder(${orderId})">
                <i class="bi bi-check2-circle me-1"></i>Đã giao
            </button>
            <button type="button"
                    class="btn btn-sm btn-outline-danger rounded-pill px-2.5 font-size-12"
                    id="btn-reject-${orderId}"
                    onclick="openRejectDeliveryModal(${orderId}, '')">
                <i class="bi bi-person-x"></i>
            </button>
            <a href="/merchant/orders/${orderId}"
               class="btn btn-sm btn-light border rounded-pill px-2.5 font-size-12 text-secondary"
               title="Chi tiết đơn hàng">
                <i class="bi bi-chevron-right"></i>
            </a>
        `;
    }

    startDeliveryCountdown(orderId, deliverySeconds);
}

// Nhận đơn
async function handleAcceptOrder(orderId) {
    if (!confirm('Bạn có chắc chắn muốn nhận đơn hàng này và bắt đầu chuẩn bị món?')) {
        return;
    }

    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/accept`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || 'Không thể nhận đơn hàng');
            return;
        }

        const data = result.data;
        const prepSeconds = data?.remainingPrepSeconds || 600;

        const statusCol = document.getElementById(`status-container-${orderId}`);

        if (statusCol) {
            statusCol.innerHTML = `
                <div class="d-flex flex-column gap-1">
                    <span class="badge badge-status bg-primary text-white">
                        Đang chuẩn bị
                    </span>
                    <small class="text-primary font-monospace fw-bold font-size-11 prep-timer"
                           data-order-id="${orderId}"
                           data-remaining="${prepSeconds}">
                        <i class="bi bi-clock me-1"></i>
                        <span class="timer-display">--:--</span>
                    </small>
                </div>
            `;
        }

        const actionCol = document.getElementById(`action-container-${orderId}`);

        if (actionCol) {
            actionCol.innerHTML = `
                <button type="button"
                        class="btn btn-sm btn-primary rounded-pill px-3 fw-semibold font-size-12 shadow-sm"
                        onclick="handleStartDelivery(${orderId})">
                    <i class="bi bi-send me-1"></i>Giao ngay
                </button>
                <a href="/merchant/orders/${orderId}"
                   class="btn btn-sm btn-light border rounded-pill px-2.5 font-size-12 text-secondary"
                   title="Chi tiết đơn hàng">
                    <i class="bi bi-chevron-right"></i>
                </a>
            `;
        }

        startPrepCountdown(orderId, prepSeconds);

        showToast(
            'Thành công',
            'Đã nhận đơn hàng! Đơn đã chuyển sang trạng thái Đang chuẩn bị.',
            'success'
        );
    } catch (error) {
        console.error('Lỗi khi nhận đơn:', error);
        alert('Đã xảy ra lỗi kết nối');
    }
}

// Bắt đầu giao hàng
async function handleStartDelivery(orderId) {
    if (!confirm('Xác nhận món ăn đã làm xong và bắt đầu giao cho khách?')) {
        return;
    }

    try {
        const response = await fetch(
            `/api/merchant/orders/${orderId}/start-delivery`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );

        const result = await response.json();

        if (!result.success) {
            alert(result.message || 'Không thể cập nhật trạng thái giao hàng');
            return;
        }

        const data = result.data;
        const deliverySeconds = data?.remainingDeliverySeconds || 900;
        const timerKey = `prep_${orderId}`;

        if (activeTimers.has(timerKey)) {
            clearInterval(activeTimers.get(timerKey));
            activeTimers.delete(timerKey);
        }

        transitionToDelivering(orderId, deliverySeconds);

        showToast(
            'Bắt đầu giao',
            'Đơn hàng đã chuyển sang trạng thái Đang giao!',
            'info'
        );
    } catch (error) {
        console.error('Lỗi khi bắt đầu giao hàng:', error);
        alert('Đã xảy ra lỗi kết nối');
    }
}

// Mở modal hủy đơn
function openCancelModal(orderId, orderCode) {
    currentCancelOrderId = orderId;

    const codeEl = document.getElementById('modalOrderCode');
    if (codeEl) codeEl.innerText = orderCode;

    const reasonInput = document.getElementById('cancelReasonInput');
    if (reasonInput) reasonInput.value = '';

    cancelModalInstance?.show();
}

// Hủy đơn
async function executeCancelOrder(orderId, reason) {
    try {
        const response = await fetch(
            `/api/merchant/orders/${orderId}/cancel`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    cancelReason: reason
                })
            }
        );

        const result = await response.json();

        if (!result.success) {
            alert(result.message || 'Không thể hủy đơn hàng');
            return;
        }

        cancelModalInstance?.hide();

        const statusCol = document.getElementById(`status-container-${orderId}`);

        if (statusCol) {
            statusCol.innerHTML = `
                <span class="badge badge-status bg-danger bg-opacity-10 text-danger border border-danger">
                    Đã hủy
                </span>
            `;
        }

        const actionCol = document.getElementById(`action-container-${orderId}`);

        if (actionCol) {
            actionCol.innerHTML = `
                <a href="/merchant/orders/${orderId}"
                   class="btn btn-sm btn-light border rounded-pill px-2.5 font-size-12 text-secondary"
                   title="Chi tiết đơn hàng">
                    <i class="bi bi-chevron-right"></i>
                </a>
            `;
        }

        showToast(
            'Đã hủy',
            'Đơn hàng đã được hủy thành công.',
            'warning'
        );
    } catch (error) {
        console.error('Lỗi khi hủy đơn:', error);
        alert('Đã xảy ra lỗi kết nối');
    }
}

// Hoàn thành đơn
async function handleCompleteOrder(orderId) {
    if (!confirm('Xác nhận bạn đã giao món và nhận tiền đầy đủ cho đơn hàng này?')) {
        return;
    }

    try {
        const response = await fetch(
            `/api/merchant/orders/${orderId}/complete`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );

        const result = await response.json();

        if (!result.success) {
            alert(result.message || 'Không thể hoàn thành đơn hàng');
            return;
        }

        const timerKey = `delivery_${orderId}`;

        if (activeTimers.has(timerKey)) {
            clearInterval(activeTimers.get(timerKey));
            activeTimers.delete(timerKey);
        }

        const statusCol = document.getElementById(`status-container-${orderId}`);

        if (statusCol) {
            statusCol.innerHTML = `
                <span class="badge badge-status bg-success text-white">
                    Hoàn thành
                </span>
            `;
        }

        const actionCol = document.getElementById(`action-container-${orderId}`);

        if (actionCol) {
            actionCol.innerHTML = `
                <a href="/merchant/orders/${orderId}"
                   class="btn btn-sm btn-light border rounded-pill px-2.5 font-size-12 text-secondary"
                   title="Chi tiết đơn hàng">
                    <i class="bi bi-chevron-right"></i>
                </a>
            `;
        }

        showToast(
            'Thành công',
            'Đơn hàng đã được hoàn thành thành công!',
            'success'
        );
    } catch (error) {
        console.error('Lỗi khi hoàn thành đơn:', error);
        alert('Đã xảy ra lỗi kết nối');
    }
}

// Mở modal khách không nhận
function openRejectDeliveryModal(orderId, orderCode) {
    currentRejectOrderId = orderId;

    const codeEl = document.getElementById('rejectModalOrderCode');
    if (codeEl) codeEl.innerText = orderCode;

    const reasonInput = document.getElementById('rejectReasonInput');
    if (reasonInput) {
        reasonInput.value = 'Khách không nhận hàng';
    }

    rejectModalInstance?.show();
}

// Ghi nhận khách không nhận hàng
async function executeRejectDelivery(orderId, reason) {
    try {
        const response = await fetch(
            `/api/merchant/orders/${orderId}/failed-delivery`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    cancelReason: reason
                })
            }
        );

        const result = await response.json();

        if (!result.success) {
            alert(result.message || 'Không thể ghi nhận hủy đơn hàng');
            return;
        }

        rejectModalInstance?.hide();

        const timerKey = `delivery_${orderId}`;

        if (activeTimers.has(timerKey)) {
            clearInterval(activeTimers.get(timerKey));
            activeTimers.delete(timerKey);
        }

        const statusCol = document.getElementById(`status-container-${orderId}`);

        if (statusCol) {
            statusCol.innerHTML = `
                <span class="badge badge-status bg-danger bg-opacity-10 text-danger border border-danger">
                    Đã hủy
                </span>
            `;
        }

        const actionCol = document.getElementById(`action-container-${orderId}`);

        if (actionCol) {
            actionCol.innerHTML = `
                <a href="/merchant/orders/${orderId}"
                   class="btn btn-sm btn-light border rounded-pill px-2.5 font-size-12 text-secondary"
                   title="Chi tiết đơn hàng">
                    <i class="bi bi-chevron-right"></i>
                </a>
            `;
        }

        showToast(
            'Đã hủy',
            'Đã ghi nhận đơn hàng bị hủy do khách không nhận hàng.',
            'warning'
        );
    } catch (error) {
        console.error('Lỗi khi ghi nhận khách không nhận:', error);
        alert('Đã xảy ra lỗi kết nối');
    }
}

function showToast(title, message, type = 'info') {
    if (typeof window.showMerchantToast === 'function') {
        window.showMerchantToast(title, message, type);
    } else {
        alert(message);
    }
}