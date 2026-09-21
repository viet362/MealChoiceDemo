/**
 * JavaScript xử lý nghiệp vụ Đối soát doanh thu cho Merchant Hub
 * Quản lý dòng tiền, Xác nhận (Confirm) và Khiếu nại (Claim)
 */

let currentSettlementData = null;
let currentPeriodType = 'MONTH'; // 'MONTH' hoặc 'WEEK'
let confirmModalInstance = null;
let claimModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    confirmModalInstance = new bootstrap.Modal(document.getElementById('confirmModal'));
    claimModalInstance = new bootstrap.Modal(document.getElementById('claimModal'));

    loadClaimReasons();
    loadPeriods();

    // Lắng nghe sự kiện thay đổi kỳ đối soát
    const periodSelect = document.getElementById('periodSelect');
    if (periodSelect) {
        periodSelect.addEventListener('change', () => {
            loadSettlementData();
        });
    }
});

/**
 * Chuyển đổi giữa chế độ xem Theo Tháng và Theo Tuần
 */
function switchPeriodType(type) {
    if (currentPeriodType === type) return;
    currentPeriodType = type;

    const btnMonth = document.getElementById('btnPeriodTypeMonth');
    const btnWeek = document.getElementById('btnPeriodTypeWeek');

    if (type === 'MONTH') {
        btnMonth.className = 'btn btn-sm rounded-pill fw-semibold px-3 active btn-danger text-white';
        btnWeek.className = 'btn btn-sm rounded-pill fw-semibold px-3 text-muted';
    } else {
        btnWeek.className = 'btn btn-sm rounded-pill fw-semibold px-3 active btn-danger text-white';
        btnMonth.className = 'btn btn-sm rounded-pill fw-semibold px-3 text-muted';
    }

    loadPeriods();
}

/**
 * Format tiền tệ VNĐ (ví dụ: 150.000 ₫)
 */
function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

/**
 * Format ngày giờ (dd/MM/yyyy HH:mm)
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    if (isNaN(date.getTime())) return dateTimeStr;
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Tải danh sách các kỳ đối soát khả dụng (Tháng hoặc Tuần)
 */
async function loadPeriods() {
    const periodSelect = document.getElementById('periodSelect');
    try {
        const response = await fetch(`/api/merchant/settlements/periods?periodType=${encodeURIComponent(currentPeriodType)}`);
        if (!response.ok) {
            const errData = await response.json().catch(() => null);
            throw new Error(errData?.message || `HTTP ${response.status}: Không thể tải danh sách kỳ đối soát`);
        }
        const periods = await response.json();

        periodSelect.innerHTML = '';
        if (periods.length === 0) {
            periodSelect.innerHTML = '<option value="">Không có kỳ đối soát</option>';
            return;
        }

        periods.forEach((p, idx) => {
            const opt = document.createElement('option');
            opt.value = p.periodKey;
            opt.textContent = `${p.label} (${p.statusDisplayName})`;
            if (idx === 0) opt.selected = true;
            periodSelect.appendChild(opt);
        });

        // Tải dữ liệu kỳ đầu tiên
        loadSettlementData();
    } catch (error) {
        console.error('Error loadPeriods:', error);
        periodSelect.innerHTML = '<option value="">Lỗi tải dữ liệu kỳ</option>';
    }
}

/**
 * Tải danh sách lý do khiếu nại
 */
async function loadClaimReasons() {
    const claimReasonSelect = document.getElementById('claimReasonSelect');
    try {
        const response = await fetch('/api/merchant/settlements/claim-reasons');
        if (!response.ok) return;
        const reasons = await response.json();

        claimReasonSelect.innerHTML = '<option value="">-- Chọn lý do khiếu nại --</option>';
        reasons.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.key;
            opt.textContent = r.label;
            claimReasonSelect.appendChild(opt);
        });
    } catch (error) {
        console.error('Error loadClaimReasons:', error);
    }
}

/**
 * Tải dữ liệu tổng quan đối soát và chi tiết đơn hàng
 */
async function loadSettlementData() {
    const periodSelect = document.getElementById('periodSelect');
    const periodKey = periodSelect.value;

    const tbody = document.getElementById('settlementOrdersTableBody');
    const emptyState = document.getElementById('emptyOrdersState');

    tbody.innerHTML = `
        <tr>
            <td colspan="9" class="text-center py-5 text-muted">
                <div class="spinner-border spinner-border-sm text-danger me-2" role="status"></div>
                Đang tổng hợp số liệu đối soát...
            </td>
        </tr>
    `;
    emptyState.classList.add('d-none');

    try {
        const url = `/api/merchant/settlements/overview?periodType=${encodeURIComponent(currentPeriodType)}${periodKey ? `&periodKey=${encodeURIComponent(periodKey)}` : ''}`;
        const response = await fetch(url);
        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || 'Lỗi lấy số liệu đối soát');
        }

        const data = await response.json();
        currentSettlementData = data;
        renderSettlementOverview(data);
    } catch (error) {
        console.error('Error loadSettlementData:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-4 text-danger">
                    <i class="bi bi-exclamation-circle me-1"></i> ${error.message}
                </td>
            </tr>
        `;
    }
}

/**
 * Render dữ liệu tổng quan và chi tiết ra UI
 */
function renderSettlementOverview(data) {
    // 1. Cập nhật 4 Cards tài chính
    document.getElementById('cardGrossRevenue').textContent = formatCurrency(data.totalGrossRevenue);
    document.getElementById('cardTotalOrders').textContent = `${data.totalOrders} đơn hoàn thành`;
    document.getElementById('cardTotalDiscount').textContent = formatCurrency(data.totalDiscount);
    document.getElementById('cardCommissionFee').textContent = formatCurrency(data.totalCommissionFee);
    document.getElementById('cardCommissionRateBadge').textContent = data.commissionRateDisplay || '0.001%';
    document.getElementById('cardNetRevenue').textContent = formatCurrency(data.netRevenue);

    // Cập nhật thông tin điều chỉnh nếu có
    const adjInfoBox = document.getElementById('cardAdjustmentInfo');
    if (adjInfoBox) {
        if (data.adjustmentAmount && data.adjustmentAmount !== 0) {
            adjInfoBox.classList.remove('d-none');
            document.getElementById('cardOriginalNetRevenue').textContent = formatCurrency(data.originalNetRevenue);
            const adjBadge = document.getElementById('cardAdjustmentBadge');
            adjBadge.textContent = `${data.adjustmentAmount > 0 ? '+' : ''}${formatCurrency(data.adjustmentAmount)} (Điều chỉnh)`;
            adjBadge.className = `badge ${data.adjustmentAmount >= 0 ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'} px-1.5 py-0.5 ms-1`;
        } else {
            adjInfoBox.classList.add('d-none');
        }
    }

    // 2. Cập nhật Badge trạng thái kỳ & In-Progress / Overlap alert
    const badge = document.getElementById('currentPeriodStatusBadge');
    badge.className = `badge badge-pill-custom ${data.statusBadgeClass || 'bg-warning text-dark'}`;
    badge.textContent = data.statusDisplayName;

    const inProgressBox = document.getElementById('inProgressNoticeBox');
    if (inProgressBox) {
        if (data.isInProgress) {
            inProgressBox.classList.remove('d-none');
            const endEl = document.getElementById('inProgressEndTimeText');
            if (endEl) endEl.textContent = formatDateTime(data.endDate);
        } else {
            inProgressBox.classList.add('d-none');
        }
    }

    // 2.1 Xử lý Overlap Alert (chống đối soát trùng lặp Tuần & Tháng)
    const overlapBox = document.getElementById('overlapNoticeBox');
    if (overlapBox) {
        if (data.hasOverlap) {
            overlapBox.classList.remove('d-none');
            const msgEl = document.getElementById('overlapMessageText');
            if (msgEl) msgEl.textContent = data.overlapMessage || 'Kỳ đối soát này có khoảng thời gian đã được xác nhận chốt tiền ở kỳ khác. Kỳ này chỉ dùng để xem báo cáo thống kê và không thể chốt số.';
        } else {
            overlapBox.classList.add('d-none');
        }
    }

    // 3. Xử lý Alert khiếu nại (PENDING / REJECTED / RESOLVED)
    const disputeBox = document.getElementById('disputeAlertBox');
    const claimRejectedBox = document.getElementById('claimRejectedAlertBox');
    const claimResolvedBox = document.getElementById('claimResolvedAlertBox');

    if (disputeBox) disputeBox.classList.add('d-none');
    if (claimRejectedBox) claimRejectedBox.classList.add('d-none');
    if (claimResolvedBox) claimResolvedBox.classList.add('d-none');

    if (data.claimStatus === 'PENDING' || data.status === 'DISPUTED') {
        if (disputeBox) {
            disputeBox.classList.remove('d-none');
            const periodEl = document.getElementById('disputePeriodLabel');
            if (periodEl) periodEl.textContent = `(${data.periodLabel || ''})`;
            document.getElementById('claimReasonText').textContent = data.claimReason || 'Không xác định';
            document.getElementById('claimDescriptionText').textContent = data.claimDescription || 'Không có mô tả chi tiết';
            document.getElementById('claimCreatedAtText').textContent = data.claimCreatedAt ? `Thời gian gửi: ${formatDateTime(data.claimCreatedAt)}` : '';

            const evidenceContainer = document.getElementById('claimEvidenceLinkContainer');
            const evidenceLink = document.getElementById('claimEvidenceLink');
            if (data.claimEvidenceUrl) {
                evidenceContainer.classList.remove('d-none');
                evidenceLink.href = data.claimEvidenceUrl;
            } else {
                evidenceContainer.classList.add('d-none');
            }
        }
    } else if (data.claimStatus === 'REJECTED') {
        if (claimRejectedBox) {
            claimRejectedBox.classList.remove('d-none');
            const periodEl = document.getElementById('rejectedClaimPeriodLabel');
            if (periodEl) periodEl.textContent = `[${data.periodLabel || ''}]`;
            document.getElementById('rejectedClaimReason').textContent = data.claimReason || 'Đối soát doanh thu';
            document.getElementById('rejectedAdminNoteText').textContent = data.claimAdminNote || 'Admin đã xem xét và từ chối khiếu nại.';
        }
    } else if (data.claimStatus === 'RESOLVED') {
        if (claimResolvedBox) {
            claimResolvedBox.classList.remove('d-none');
            const periodEl = document.getElementById('resolvedClaimPeriodLabel');
            if (periodEl) periodEl.textContent = `[${data.periodLabel || ''}]`;
            document.getElementById('resolvedAdminNoteText').textContent = data.claimAdminNote || 'Admin đã xác minh và chấp thuận khiếu nại.';
        }
    }

    // 4. Cập nhật Footer Action Bar
    const footerStatusText = document.getElementById('footerStatusText');
    footerStatusText.textContent = data.hasOverlap ? `${data.statusDisplayName} (Đã chốt ở kỳ khác)` : data.statusDisplayName;
    footerStatusText.className = `fw-bold ${data.status === 'CONFIRMED' ? 'text-success' : (data.status === 'DISPUTED' ? 'text-danger' : (data.hasOverlap ? 'text-warning' : (data.isInProgress ? 'text-info' : 'text-warning')))}`;

    const confirmedTimeBox = document.getElementById('confirmedTimeBox');
    if (data.status === 'CONFIRMED' && data.confirmedAt) {
        confirmedTimeBox.classList.remove('d-none');
        document.getElementById('confirmedTimeText').textContent = formatDateTime(data.confirmedAt);
    } else {
        confirmedTimeBox.classList.add('d-none');
    }

    // Khóa hoặc mở các nút hành động
    const btnConfirm = document.getElementById('btnOpenConfirmModal');
    const btnClaim = document.getElementById('btnOpenClaimModal');

    if (data.hasOverlap) {
        btnConfirm.setAttribute('disabled', 'true');
        btnClaim.setAttribute('disabled', 'true');
        btnConfirm.classList.add('opacity-50');
        btnClaim.classList.add('opacity-50');
        btnConfirm.title = data.overlapMessage || 'Kỳ đối soát này có khoảng thời gian đã được chốt ở kỳ khác.';
        btnClaim.title = 'Kỳ đối soát này có khoảng thời gian đã được chốt ở kỳ khác, không thể gửi khiếu nại.';
    } else if (data.isInProgress) {
        btnConfirm.setAttribute('disabled', 'true');
        btnClaim.setAttribute('disabled', 'true');
        btnConfirm.classList.add('opacity-50');
        btnClaim.classList.add('opacity-50');
        btnConfirm.title = 'Kỳ đối soát đang diễn ra, chưa thể chốt số';
        btnClaim.title = 'Kỳ đối soát đang diễn ra, chưa thể gửi khiếu nại';
    } else if (data.actionable) {
        btnConfirm.removeAttribute('disabled');
        btnClaim.removeAttribute('disabled');
        btnConfirm.classList.remove('opacity-50');
        btnClaim.classList.remove('opacity-50');
        btnConfirm.title = '';
        btnClaim.title = '';
    } else {
        btnConfirm.setAttribute('disabled', 'true');
        btnClaim.setAttribute('disabled', 'true');
        btnConfirm.classList.add('opacity-50');
        btnClaim.classList.add('opacity-50');
        btnConfirm.title = 'Kỳ đối soát này đã được xác nhận hoặc đang trong quá trình khiếu nại';
        btnClaim.title = 'Kỳ đối soát này đã được xác nhận hoặc đang trong quá trình khiếu nại';
    }

    // 5. Render danh sách đơn hàng
    const tbody = document.getElementById('settlementOrdersTableBody');
    const emptyState = document.getElementById('emptyOrdersState');
    tbody.innerHTML = '';

    if (!data.orders || data.orders.length === 0) {
        emptyState.classList.remove('d-none');
        return;
    }

    emptyState.classList.add('d-none');

    data.orders.forEach((order, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="text-center fw-medium text-muted">${index + 1}</td>
            <td>
                <span class="fw-bold text-dark font-monospace">${order.orderCode}</span>
            </td>
            <td>
                <span class="text-muted small">${formatDateTime(order.createdAt)}</span>
            </td>
            <td>
                <div class="fw-medium text-dark">${order.contactName || '-'}</div>
                <small class="text-muted">${order.contactPhone || ''}</small>
            </td>
            <td class="text-end fw-semibold text-dark">${formatCurrency(order.subtotalPrice)}</td>
            <td class="text-end text-warning-emphasis">${formatCurrency(order.discountAmount)}</td>
            <td class="text-end text-purple" style="color: #7e22ce;">${formatCurrency(order.commissionFee)}</td>
            <td class="text-end fw-bold text-success">${formatCurrency(order.netAmount)}</td>
            <td class="text-center">
                <span class="badge ${order.statusBadgeClass} rounded-pill px-2.5 py-1 font-size-11">${order.statusDisplayName}</span>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

/**
 * Mở Modal Xác nhận đối soát
 */
function openConfirmModal() {
    if (!currentSettlementData || !currentSettlementData.actionable) {
        alert('Kỳ đối soát này đã được xác nhận hoặc đang trong quá trình khiếu nại.');
        return;
    }

    document.getElementById('confirmModalPeriodLabel').textContent = currentSettlementData.periodLabel;
    document.getElementById('confirmModalGross').textContent = formatCurrency(currentSettlementData.totalGrossRevenue);
    document.getElementById('confirmModalDiscount').textContent = formatCurrency(currentSettlementData.totalDiscount);
    document.getElementById('confirmModalFee').textContent = formatCurrency(currentSettlementData.totalCommissionFee);
    document.getElementById('confirmModalNet').textContent = formatCurrency(currentSettlementData.netRevenue);

    confirmModalInstance.show();
}

/**
 * Submit Xác nhận đối soát sang hệ thống
 */
async function submitConfirmSettlement() {
    if (!currentSettlementData) return;

    const btnSubmit = document.getElementById('btnSubmitConfirm');
    const spinner = document.getElementById('confirmSpinner');

    btnSubmit.setAttribute('disabled', 'true');
    spinner.classList.remove('d-none');

    try {
        const response = await fetch(`/api/merchant/settlements/${currentSettlementData.settlementId}/confirm`, {
            method: 'POST'
        });

        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Xác nhận đối soát thất bại');
        }

        confirmModalInstance.hide();
        alert(result.message || 'Xác nhận đối soát thành công!');

        // Cập nhật lại dữ liệu
        await loadPeriods();
    } catch (error) {
        console.error('Error submitConfirmSettlement:', error);
        alert(`Lỗi: ${error.message}`);
    } finally {
        btnSubmit.removeAttribute('disabled');
        spinner.classList.add('d-none');
    }
}

/**
 * Mở Modal Khiếu nại đối soát
 */
function openClaimModal() {
    if (!currentSettlementData || !currentSettlementData.actionable) {
        alert('Kỳ đối soát này không ở trạng thái chờ xác nhận.');
        return;
    }

    document.getElementById('claimModalPeriodLabel').textContent = currentSettlementData.periodLabel;
    document.getElementById('claimForm').reset();
    document.getElementById('imagePreviewBox').classList.add('d-none');
    document.getElementById('imagePreviewElement').src = '';

    claimModalInstance.show();
}

/**
 * Xem trước ảnh upload khiếu nại
 */
function previewClaimImage(event) {
    const file = event.target.files[0];
    const previewBox = document.getElementById('imagePreviewBox');
    const previewImg = document.getElementById('imagePreviewElement');

    if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            previewImg.src = e.target.result;
            previewBox.classList.remove('d-none');
        };
        reader.readAsDataURL(file);
    } else {
        previewBox.classList.add('d-none');
        previewImg.src = '';
    }
}

/**
 * Submit Gửi khiếu nại đối soát
 */
async function submitClaimSettlement(event) {
    event.preventDefault();
    if (!currentSettlementData) return;

    const reason = document.getElementById('claimReasonSelect').value;
    const description = document.getElementById('claimDescriptionInput').value;
    const evidenceFile = document.getElementById('claimEvidenceInput').files[0];

    if (!reason) {
        alert('Vui lòng chọn lý do khiếu nại');
        return;
    }

    if (!description.trim()) {
        alert('Vui lòng nhập mô tả chi tiết');
        return;
    }

    const btnSubmit = document.getElementById('btnSubmitClaim');
    const spinner = document.getElementById('claimSpinner');

    btnSubmit.setAttribute('disabled', 'true');
    spinner.classList.remove('d-none');

    const formData = new FormData();
    formData.append('reason', reason);
    formData.append('description', description);
    if (evidenceFile) {
        formData.append('evidenceImage', evidenceFile);
    }

    try {
        const response = await fetch(`/api/merchant/settlements/${currentSettlementData.settlementId}/claim`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Gửi khiếu nại thất bại');
        }

        claimModalInstance.hide();
        alert(result.message || 'Gửi khiếu nại thành công!');

        // Cập nhật lại dữ liệu
        await loadPeriods();
    } catch (error) {
        console.error('Error submitClaimSettlement:', error);
        alert(`Lỗi: ${error.message}`);
    } finally {
        btnSubmit.removeAttribute('disabled');
        spinner.classList.add('d-none');
    }
}
