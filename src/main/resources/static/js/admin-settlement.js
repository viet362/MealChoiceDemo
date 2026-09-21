/**
 * Admin Settlement & Claim Management Script
 */
let settlementDetailModalInstance = null;
let claimHandleModalInstance = null;
let imageLightboxModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    settlementDetailModalInstance = new bootstrap.Modal(document.getElementById('settlementDetailModal'));
    claimHandleModalInstance = new bootstrap.Modal(document.getElementById('claimHandleModal'));
    imageLightboxModalInstance = new bootstrap.Modal(document.getElementById('imageLightboxModal'));
});

/**
 * Format currency VND
 */
function formatCurrency(amount) {
    if (amount === undefined || amount === null) return '0 đ';
    return Number(amount).toLocaleString('vi-VN') + ' đ';
}

/**
 * Format Date Time
 */
function formatDateTime(dtStr) {
    if (!dtStr) return '-';
    const date = new Date(dtStr);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Mở Modal Tra Soát đơn hàng của Kỳ Đối Soát
 */
async function openSettlementOverviewModal(settlementId) {
    const tbody = document.getElementById('modalOrdersTbody');
    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-danger me-2"></div>Đang tải dữ liệu tra soát...</td></tr>';
    
    settlementDetailModalInstance.show();

    try {
        const res = await fetch(`/api/admin/settlements/${settlementId}/overview`);
        if (!res.ok) throw new Error('Không thể tải chi tiết kỳ đối soát');
        const data = await res.json();

        document.getElementById('modalMerchantSubTitle').textContent = `Cửa hàng: ${data.merchantRestaurantName || '-'} | Kỳ: ${data.periodLabel || '-'}`;
        document.getElementById('modalGross').textContent = formatCurrency(data.totalGrossRevenue);
        document.getElementById('modalDiscount').textContent = formatCurrency(data.totalDiscount);
        document.getElementById('modalFee').textContent = formatCurrency(data.totalCommissionFee);
        document.getElementById('modalNet').textContent = formatCurrency(data.netRevenue);

        if (!data.orders || data.orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted">Không có đơn hàng hoàn thành nào trong kỳ này.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        data.orders.forEach((order, index) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center text-muted">${index + 1}</td>
                <td><span class="fw-bold text-dark font-monospace">${order.orderCode}</span></td>
                <td><small class="text-muted">${formatDateTime(order.createdAt)}</small></td>
                <td>
                    <div class="fw-medium text-dark">${order.contactName || '-'}</div>
                    <small class="text-muted">${order.contactPhone || ''}</small>
                </td>
                <td class="text-end fw-semibold text-dark">${formatCurrency(order.subtotalPrice)}</td>
                <td class="text-end text-warning-emphasis">${formatCurrency(order.discountAmount)}</td>
                <td class="text-end" style="color: #7e22ce;">${formatCurrency(order.commissionFee)}</td>
                <td class="text-end fw-bold text-success">${formatCurrency(order.netAmount)}</td>
                <td class="text-center">
                    <span class="badge ${order.statusBadgeClass} rounded-pill px-2 py-1 font-size-11">${order.statusDisplayName}</span>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error('Error openSettlementOverviewModal:', e);
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger">Lỗi tải dữ liệu: ${e.message}</td></tr>`;
    }
}

/**
 * Mở Modal Xử lý Khiếu nại từ Button Data Attributes
 */
function openClaimHandleModalFromBtn(btn) {
    const settlementId = btn.getAttribute('data-settlement-id');
    const claimId = btn.getAttribute('data-claim-id');
    const reasonName = btn.getAttribute('data-claim-reason');
    const description = btn.getAttribute('data-claim-desc');
    const evidenceUrl = btn.getAttribute('data-claim-img');
    const claimStatus = btn.getAttribute('data-claim-status');
    const merchantName = btn.getAttribute('data-merchant-name');
    const periodLabel = btn.getAttribute('data-period-label');
    const adminNote = btn.getAttribute('data-admin-note');

    openClaimHandleModal(settlementId, claimId, reasonName, description, evidenceUrl, claimStatus, merchantName, periodLabel, adminNote);
}

/**
 * Mở Modal Xử lý Khiếu nại (Approve / Reject)
 */
function openClaimHandleModal(settlementId, claimId, reasonName, description, evidenceUrl, claimStatus, merchantName, periodLabel, adminNote) {
    document.getElementById('handleClaimId').value = claimId;
    document.getElementById('claimModalMerchantInfo').textContent = `Merchant: ${merchantName} | Kỳ: ${periodLabel}`;
    document.getElementById('claimReasonText').textContent = reasonName || '-';
    document.getElementById('claimDescriptionText').textContent = description || '-';

    const adminNoteInput = document.getElementById('adminNoteInput');
    adminNoteInput.value = adminNote || '';

    const evidenceRow = document.getElementById('claimEvidenceRow');
    const evidenceImg = document.getElementById('claimEvidenceImg');
    if (evidenceUrl && evidenceUrl.trim()) {
        evidenceImg.src = evidenceUrl;
        evidenceRow.classList.remove('d-none');
    } else {
        evidenceRow.classList.add('d-none');
    }

    const badgeContainer = document.getElementById('claimStatusBadgeContainer');
    const processedNotice = document.getElementById('claimProcessedNotice');
    const processedNoticeText = document.getElementById('claimProcessedNoticeText');
    const actionButtonsContainer = document.getElementById('claimActionButtonsContainer');
    const adminNoteRequiredMark = document.getElementById('adminNoteRequiredMark');
    const adminNoteHelpText = document.getElementById('adminNoteHelpText');
    const adjustmentContainer = document.getElementById('adjustmentAmountContainer');
    const adjustmentAmountInput = document.getElementById('adjustmentAmountInput');

    if (claimStatus === 'RESOLVED') {
        badgeContainer.innerHTML = '<span class="badge bg-success">Đã chấp thuận</span>';
        if (processedNotice) {
            processedNotice.className = 'alert alert-success py-2 px-3 rounded-3 small mb-3';
            processedNotice.classList.remove('d-none');
            processedNoticeText.textContent = 'Khiếu nại này đã được Admin CHẤP THUẬN và kỳ đối soát đã chuyển sang trạng thái ĐÃ XÁC NHẬN để thực hiện lệnh chi trả tiền (Payout).';
        }
        if (actionButtonsContainer) actionButtonsContainer.classList.add('d-none');
        if (adminNoteRequiredMark) adminNoteRequiredMark.classList.add('d-none');
        if (adminNoteHelpText) adminNoteHelpText.textContent = 'Ghi chú giải trình khi Admin duyệt khiếu nại (Chế độ xem).';
        if (adjustmentContainer) adjustmentContainer.classList.add('d-none');
        adminNoteInput.setAttribute('readonly', 'true');
    } else if (claimStatus === 'REJECTED') {
        badgeContainer.innerHTML = '<span class="badge bg-danger">Đã từ chối</span>';
        if (processedNotice) {
            processedNotice.className = 'alert alert-warning py-2 px-3 rounded-3 small mb-3';
            processedNotice.classList.remove('d-none');
            processedNoticeText.textContent = 'Khiếu nại này đã bị TỪ CHỐI và kỳ đối soát đã chuyển về trạng thái CHỜ XÁC NHẬN để Merchant xem xét lại.';
        }
        if (actionButtonsContainer) actionButtonsContainer.classList.add('d-none');
        if (adminNoteRequiredMark) adminNoteRequiredMark.classList.add('d-none');
        if (adminNoteHelpText) adminNoteHelpText.textContent = 'Lý do từ chối gửi cho Merchant (Chế độ xem).';
        if (adjustmentContainer) adjustmentContainer.classList.add('d-none');
        adminNoteInput.setAttribute('readonly', 'true');
    } else {
        badgeContainer.innerHTML = '<span class="badge bg-warning text-dark">Chờ xử lý</span>';
        if (processedNotice) processedNotice.classList.add('d-none');
        if (actionButtonsContainer) actionButtonsContainer.classList.remove('d-none');
        if (adminNoteRequiredMark) adminNoteRequiredMark.classList.remove('d-none');
        if (adminNoteHelpText) adminNoteHelpText.textContent = 'Nội dung này sẽ hiển thị trực tiếp trên giao diện của Merchant.';
        if (adjustmentContainer) adjustmentContainer.classList.remove('d-none');
        if (adjustmentAmountInput) adjustmentAmountInput.value = '0';
        adminNoteInput.removeAttribute('readonly');
    }

    claimHandleModalInstance.show();
}

/**
 * Phóng to ảnh chứng từ
 */
function showLightbox(src) {
    if (!src) return;
    document.getElementById('lightboxImage').src = src;
    imageLightboxModalInstance.show();
}

/**
 * Thực hiện hành động Duyệt (resolve) hoặc Từ chối (reject) khiếu nại
 */
async function executeClaimAction(action) {
    const claimId = document.getElementById('handleClaimId').value;
    const adminNote = document.getElementById('adminNoteInput').value;
    const adjustmentAmountInput = document.getElementById('adjustmentAmountInput');
    const adjustmentAmount = adjustmentAmountInput ? parseFloat(adjustmentAmountInput.value || 0) : 0;

    if (action === 'reject' && !adminNote.trim()) {
        alert('Vui lòng nhập ghi chú giải trình lý do từ chối để Merchant nắm thông tin!');
        document.getElementById('adminNoteInput').focus();
        return;
    }

    const confirmMsg = action === 'resolve'
        ? `Bạn có chắc chắn muốn CHẤP THUẬN khiếu nại này với số tiền điều chỉnh là ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(adjustmentAmount)} và chuyển kỳ đối soát sang trạng thái ĐÃ XÁC NHẬN?`
        : 'Bạn có chắc chắn muốn TỪ CHỐI khiếu nại này?';

    const isConfirm = confirm(confirmMsg);
    if (!isConfirm) return;

    const btnResolve = document.getElementById('btnResolveClaim');
    const btnReject = document.getElementById('btnRejectClaim');
    btnResolve.setAttribute('disabled', 'true');
    btnReject.setAttribute('disabled', 'true');

    try {
        const url = `/api/admin/settlements/claims/${claimId}/${action}`;
        const payload = action === 'resolve'
            ? { adjustmentAmount: adjustmentAmount, adminNote: adminNote.trim() }
            : { adminNote: adminNote.trim() };

        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const result = await res.json();
        if (!res.ok || !result.success) {
            throw new Error(result.message || 'Xử lý khiếu nại thất bại');
        }

        alert(result.message || 'Thao tác thành công!');
        claimHandleModalInstance.hide();
        window.location.reload();
    } catch (e) {
        console.error('Error executeClaimAction:', e);
        alert(`Lỗi: ${e.message}`);
    } finally {
        btnResolve.removeAttribute('disabled');
        btnReject.removeAttribute('disabled');
    }
}
