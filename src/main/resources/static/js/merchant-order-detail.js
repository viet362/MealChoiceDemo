/**
 * Quản lý Chi tiết Đơn hàng của Merchant (Realtime countdown & auto status transition)
 * File: static/js/merchant-order-detail.js
 */

let detailCancelOrderId = null;
let detailCancelModalInstance = null;
let detailRejectOrderId = null;
let detailRejectModalInstance = null;
let detailActiveInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('cancelOrderDetailModal');
    if (modalEl && typeof bootstrap !== 'undefined') {
        detailCancelModalInstance = new bootstrap.Modal(modalEl);
    }

    const rejectModalEl = document.getElementById('rejectDeliveryDetailModal');
    if (rejectModalEl && typeof bootstrap !== 'undefined') {
        detailRejectModalInstance = new bootstrap.Modal(rejectModalEl);
    }

    document.getElementById('btnConfirmCancelDetail')?.addEventListener('click', () => {
        if (!detailCancelOrderId) return;
        const reason = document.getElementById('cancelDetailReasonInput')?.value;
        executeCancelOrderDetail(detailCancelOrderId, reason);
    });

    document.getElementById('btnConfirmRejectDetail')?.addEventListener('click', () => {
        if (!detailRejectOrderId) return;
        const reason = document.getElementById('rejectDetailReasonInput')?.value;
        executeRejectDeliveryDetail(detailRejectOrderId, reason);
    });

    // Khởi tạo countdown widget trên trang chi tiết nếu có
    initDetailTimer();
});

function formatTime(seconds) {
    if (seconds <= 0) return '00:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function initDetailTimer() {
    if (detailActiveInterval) {
        clearInterval(detailActiveInterval);
    }

    const prepBox = document.getElementById('detailPrepTimerBox');
    if (prepBox) {
        const orderId = prepBox.dataset.orderId;
        let remaining = parseInt(prepBox.dataset.remaining, 10);
        const display = document.getElementById('detailPrepCountdown');

        if (display && !isNaN(remaining) && remaining > 0) {
            display.innerText = formatTime(remaining);
            detailActiveInterval = setInterval(() => {
                remaining--;
                if (remaining <= 0) {
                    clearInterval(detailActiveInterval);
                    display.innerText = '00:00';
                    transitionDetailToDelivering(orderId, 15 * 60);
                } else {
                    display.innerText = formatTime(remaining);
                }
            }, 1000);
        }
        return;
    }

    const deliveryBox = document.getElementById('detailDeliveryTimerBox');
    if (deliveryBox) {
        const orderId = deliveryBox.dataset.orderId;
        let remaining = parseInt(deliveryBox.dataset.remaining, 10);
        const display = document.getElementById('detailDeliveryCountdown');
        const completeBtn = document.getElementById('detailBtnComplete');

        if (!isNaN(remaining)) {
            if (remaining > 0) {
                if (display) display.innerText = formatTime(remaining);
                if (completeBtn) {
                    completeBtn.disabled = true;
                    const btnText = document.getElementById('detailCompleteBtnText');
                    if (btnText) btnText.innerText = `Đang giao (${formatTime(remaining)})`;
                }

                detailActiveInterval = setInterval(() => {
                    remaining--;
                    if (remaining <= 0) {
                        clearInterval(detailActiveInterval);
                        if (display) display.innerText = '00:00';
                        if (completeBtn) {
                            completeBtn.disabled = false;
                            const btnText = document.getElementById('detailCompleteBtnText');
                            if (btnText) btnText.innerText = 'Xác nhận đã nhận tiền & Hoàn thành';
                        }
                    } else {
                        if (display) display.innerText = formatTime(remaining);
                        if (completeBtn) {
                            const btnText = document.getElementById('detailCompleteBtnText');
                            if (btnText) btnText.innerText = `Đang giao (${formatTime(remaining)})`;
                        }
                    }
                }, 1000);
            } else {
                if (display) display.innerText = '00:00';
                if (completeBtn) {
                    completeBtn.disabled = false;
                    const btnText = document.getElementById('detailCompleteBtnText');
                    if (btnText) btnText.innerText = 'Xác nhận đã nhận tiền & Hoàn thành';
                }
            }
        }
    }
}

function transitionDetailToDelivering(orderId, deliverySeconds) {
    const badge = document.getElementById('detailStatusBadge');
    if (badge) {
        badge.className = 'badge badge-status-lg bg-info text-white';
        badge.innerText = 'Đang giao hàng';
    }

    const actionHeader = document.getElementById('detailActionHeader');
    if (actionHeader) {
        actionHeader.innerHTML = `
            <div class="d-flex gap-2" id="deliveringButtonGroup">
                <button type="button" class="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm d-inline-flex align-items-center gap-2"
                        id="detailBtnComplete"
                        onclick="handleCompleteOrderDetail(${orderId})">
                    <i class="bi bi-check-circle-fill fs-5"></i>
                    <span id="detailCompleteBtnText">Đang giao (${formatTime(deliverySeconds)})</span>
                </button>
                <button type="button" class="btn btn-outline-danger rounded-pill px-4 py-2 fw-bold d-inline-flex align-items-center gap-2"
                        onclick="openRejectDeliveryModalDetail(${orderId}, '')">
                    <i class="bi bi-person-x fs-5"></i>
                    <span>Khách không nhận</span>
                </button>
            </div>
            <a href="/merchant/orders" class="btn btn-light border rounded-pill px-4 py-2 text-secondary fw-semibold">
                <i class="bi bi-list-ul me-1"></i>Về danh sách
            </a>
        `;
    }

    initDetailTimer();
}

// 1. NHẬN ĐƠN
async function handleAcceptOrderDetail(orderId) {
    if (!confirm('Bạn có chắc chắn muốn nhận đơn hàng này và bắt đầu chuẩn bị món?')) return;

    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/accept`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const result = await response.json();
        if (result.success) {
            const badge = document.getElementById('detailStatusBadge');
            if (badge) {
                badge.className = 'badge badge-status-lg bg-primary text-white';
                badge.innerText = 'Đang chuẩn bị';
            }

            // Cập nhật header sang nút "Bắt đầu giao ngay"
            const actionHeader = document.getElementById('detailActionHeader');
            if (actionHeader) {
                actionHeader.innerHTML = `
                    <div class="d-flex gap-2 align-items-center" id="preparingButtonGroup">
                        <button type="button" class="btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm d-inline-flex align-items-center gap-2"
                                onclick="handleStartDeliveryDetail(${orderId})">
                            <i class="bi bi-send-fill fs-5"></i>
                            <span>Bắt đầu giao ngay</span>
                        </button>
                    </div>
                    <a href="/merchant/orders" class="btn btn-light border rounded-pill px-4 py-2 text-secondary fw-semibold">
                        <i class="bi bi-list-ul me-1"></i>Về danh sách
                    </a>
                `;
            }

            alert('Đã nhận đơn hàng thành công! Đang chuyển sang giai đoạn chuẩn bị.');
            location.reload();
        } else {
            alert(result.message || 'Không thể nhận đơn');
        }
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
    }
}

// 2. BẮT ĐẦU GIAO HÀNG
async function handleStartDeliveryDetail(orderId) {
    if (!confirm('Xác nhận món ăn đã làm xong và bắt đầu giao cho khách?')) return;

    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/start-delivery`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const result = await response.json();
        if (result.success) {
            const data = result.data;
            const deliverySeconds = data?.remainingDeliverySeconds || 900;
            transitionDetailToDelivering(orderId, deliverySeconds);
            alert('Đã bắt đầu giao đơn hàng!');
            location.reload();
        } else {
            alert(result.message || 'Không thể bắt đầu giao hàng');
        }
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
    }
}

// 3. MỞ MODAL HỦY ĐƠN (PENDING)
function openCancelModalDetail(orderId, orderCode) {
    detailCancelOrderId = orderId;
    const codeEl = document.getElementById('modalDetailOrderCode');
    if (codeEl) codeEl.innerText = orderCode;
    const reasonInput = document.getElementById('cancelDetailReasonInput');
    if (reasonInput) reasonInput.value = '';
    if (detailCancelModalInstance) {
        detailCancelModalInstance.show();
    }
}

// 4. THỰC HIỆN HỦY ĐƠN
async function executeCancelOrderDetail(orderId, reason) {
    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/cancel`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cancelReason: reason })
        });

        const result = await response.json();
        if (result.success) {
            if (detailCancelModalInstance) {
                detailCancelModalInstance.hide();
            }

            if (detailActiveInterval) {
                clearInterval(detailActiveInterval);
                detailActiveInterval = null;
            }

            const badge = document.getElementById('detailStatusBadge');

            if (badge) {
                badge.className = 'badge badge-status-lg bg-danger text-white';
                badge.innerText = 'Đã hủy';
            }

            const group = document.getElementById('pendingButtonGroup');

            if (group) {
                group.remove();
            }

            alert('Đã hủy đơn hàng thành công.');
            location.reload();
        } else {
            alert(result.message || 'Không thể hủy đơn');
        }
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
    }
}

// 5. HOÀN THÀNH ĐƠN (ĐÃ NHẬN TIỀN)
async function handleCompleteOrderDetail(orderId) {
    if (!confirm('Xác nhận bạn đã giao món và nhận tiền đầy đủ cho đơn hàng này?')) return;

    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/complete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            if (detailActiveInterval) {
                clearInterval(detailActiveInterval);
            }

            const badge = document.getElementById('detailStatusBadge');
            if (badge) {
                badge.className = 'badge badge-status-lg bg-success text-white';
                badge.innerText = 'Hoàn thành';
            }

            const group = document.getElementById('deliveringButtonGroup') || document.getElementById('preparingButtonGroup');
            if (group) group.remove();

            alert('Đơn hàng đã được ghi nhận hoàn thành thành công!');
            location.reload();
        } else {
            alert(result.message || 'Không thể hoàn thành đơn hàng');
        }
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
    }
}

// 5. MỞ MODAL KHÁCH KHÔNG NHẬN HÀNG
function openRejectDeliveryModalDetail(orderId, orderCode) {
    detailRejectOrderId = orderId;
    const codeEl = document.getElementById('rejectDetailModalOrderCode');
    if (codeEl) codeEl.innerText = orderCode;
    const reasonInput = document.getElementById('rejectDetailReasonInput');
    if (reasonInput) reasonInput.value = 'Khách không nhận hàng';
    if (detailRejectModalInstance) {
        detailRejectModalInstance.show();
    }
}

// 7. THỰC HIỆN GHI NHẬN KHÁCH KHÔNG NHẬN HÀNG
async function executeRejectDeliveryDetail(orderId, reason) {
    try {
        const response = await fetch(`/api/merchant/orders/${orderId}/failed-delivery`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cancelReason: reason })
        });

        const result = await response.json();
        if (result.success) {
            if (detailRejectModalInstance) {
                detailRejectModalInstance.hide();
            }

            if (detailActiveInterval) {
                clearInterval(detailActiveInterval);
            }

            const badge = document.getElementById('detailStatusBadge');
            if (badge) {
                badge.className = 'badge badge-status-lg bg-danger text-white';
                badge.innerText = 'Đã hủy';
            }

            const group = document.getElementById('deliveringButtonGroup') || document.getElementById('preparingButtonGroup');
            if (group) group.remove();

            alert('Đã ghi nhận đơn hàng bị hủy do khách không nhận hàng.');
            location.reload();
        } else {
            alert(result.message || 'Không thể ghi nhận hủy đơn');
        }
    } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
    }
}
