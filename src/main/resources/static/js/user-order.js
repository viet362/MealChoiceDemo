/**
 * Trang "Đơn hàng của tôi"
 * File: static/js/user-orders.js
 *
 * Xử lý xem chi tiết một đơn hàng và hủy đơn.
 * Chỉ đơn ở trạng thái PENDING (quán chưa nhận đơn) mới hủy được.
 */

(function () {

    const ORDERS_API = "/api/user/orders";

    let detailModal = null;
    let cancelModal = null;

    // Đơn đang thao tác trong modal hủy
    let pendingCancel = { id: null, code: null };

    function formatCurrency(value) {
        return new Intl.NumberFormat("vi-VN").format(Math.round(Number(value) || 0)) + " đ";
    }

    function apiFetch(url, options = {}) {
        const fetchFn = (typeof fetchWithAuth === "function") ? fetchWithAuth : fetch;

        const headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        };

        const token = localStorage.getItem("accessToken");
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        return fetchFn(url, {
            ...options,
            headers: { ...headers, ...(options.headers || {}) }
        });
    }

    // ==================== CHI TIẾT ĐƠN HÀNG ====================

    async function openOrderDetail(orderId) {
        const loadingEl = document.getElementById("detailLoading");
        const errorEl = document.getElementById("detailError");
        const contentEl = document.getElementById("detailContent");
        const cancelBtn = document.getElementById("detailCancelBtn");

        loadingEl?.classList.remove("d-none");
        errorEl?.classList.add("d-none");
        contentEl?.classList.add("d-none");
        cancelBtn?.classList.add("d-none");

        document.getElementById("detailOrderCode").innerText = "—";

        detailModal?.show();

        try {
            const response = await apiFetch(`${ORDERS_API}/${orderId}`);
            const payload = await response.json().catch(() => ({}));

            if (response.status === 401) {
                showDetailError("Phiên đăng nhập đã hết hạn. Đăng nhập lại để xem đơn hàng.");
                return;
            }

            if (!response.ok || !payload.success) {
                showDetailError(payload.message || "Không tải được chi tiết đơn hàng.");
                return;
            }

            renderOrderDetail(payload.data);

        } catch (error) {
            console.error("Lỗi tải chi tiết đơn hàng:", error);
            showDetailError("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.");
        }
    }

    function showDetailError(message) {
        document.getElementById("detailLoading")?.classList.add("d-none");
        document.getElementById("detailContent")?.classList.add("d-none");

        const errorEl = document.getElementById("detailError");
        if (errorEl) {
            errorEl.innerText = message;
            errorEl.classList.remove("d-none");
        }
    }

    function renderOrderDetail(order) {
        document.getElementById("detailLoading")?.classList.add("d-none");
        document.getElementById("detailError")?.classList.add("d-none");
        document.getElementById("detailContent")?.classList.remove("d-none");

        document.getElementById("detailOrderCode").innerText = order.orderCode || "—";

        // Trạng thái
        const statusBadge = document.getElementById("detailStatusBadge");
        statusBadge.className = "badge rounded-pill px-3 py-2 fw-semibold " + (order.statusBadgeClass || "bg-secondary");
        statusBadge.innerText = order.statusDisplayName || "";

        document.getElementById("detailCreatedAt").innerText =
            order.formattedCreatedAt ? "Đặt lúc " + order.formattedCreatedAt : "";

        // Lý do hủy
        const cancelBox = document.getElementById("detailCancelBox");
        if (order.cancelReason) {
            document.getElementById("detailCancelReason").innerText = order.cancelReason;
            cancelBox.classList.remove("d-none");
        } else {
            cancelBox.classList.add("d-none");
        }

        // Cửa hàng
        document.getElementById("detailMerchantName").innerText = order.merchantName || "Cửa hàng";
        document.getElementById("detailMerchantAddress").innerText = order.merchantAddress || "";
        document.getElementById("detailMerchantPhone").innerText =
            order.merchantPhone ? "Điện thoại: " + order.merchantPhone : "";

        // Giao hàng
        document.getElementById("detailContact").innerText =
            `${order.customerName || ""} · ${order.customerPhone || ""}`;
        document.getElementById("detailAddress").innerText = order.deliveryAddress || "";
        document.getElementById("detailDeliveryPartner").innerText =
            order.deliveryPartnerName ? "Đơn vị giao: " + order.deliveryPartnerName : "";
        document.getElementById("detailEta").innerText =
            order.formattedEstimatedDeliveryTime
                ? "Dự kiến giao: " + order.formattedEstimatedDeliveryTime
                : "";

        const noteRow = document.getElementById("detailNoteRow");
        if (order.note) {
            document.getElementById("detailNote").innerText = order.note;
            noteRow.classList.remove("d-none");
        } else {
            noteRow.classList.add("d-none");
        }

        // Món ăn
        const itemsEl = document.getElementById("detailItems");
        itemsEl.innerHTML = "";

        (order.items || []).forEach(item => {
            const row = document.createElement("div");
            row.className = "d-flex align-items-center justify-content-between";

            const image = item.foodImage
                ? `<img src="${item.foodImage}" class="order-detail-thumb border" alt="${item.foodName}">`
                : `<div class="order-detail-thumb bg-light border d-flex align-items-center justify-content-center text-muted">
                       <i class="bi bi-image"></i>
                   </div>`;

            const noteLine = item.note
                ? `<div class="text-muted font-size-12 fst-italic">${item.note}</div>`
                : "";

            row.innerHTML = `
                <div class="d-flex align-items-center gap-3 overflow-hidden">
                    ${image}
                    <div class="overflow-hidden">
                        <div class="fw-semibold text-dark text-truncate">${item.foodName}</div>
                        <small class="text-muted">${formatCurrency(item.price)} × ${item.quantity}</small>
                        ${noteLine}
                    </div>
                </div>
                <strong class="text-dark text-nowrap">${formatCurrency(item.subtotal)}</strong>
            `;

            itemsEl.appendChild(row);
        });

        // Chi phí
        document.getElementById("detailSubtotal").innerText = formatCurrency(order.subtotalPrice);
        document.getElementById("detailShippingFee").innerText = formatCurrency(order.shippingFee);
        document.getElementById("detailServiceFee").innerText = formatCurrency(order.serviceFee);
        document.getElementById("detailPaymentMethod").innerText = order.paymentMethodDisplayName || "";
        document.getElementById("detailTotal").innerText = formatCurrency(order.totalAmount);

        const discountRow = document.getElementById("detailDiscountRow");
        if (Number(order.discountAmount) > 0) {
            document.getElementById("detailDiscount").innerText = "- " + formatCurrency(order.discountAmount);
            discountRow.classList.remove("d-none");
            discountRow.classList.add("d-flex");
        } else {
            discountRow.classList.add("d-none");
            discountRow.classList.remove("d-flex");
        }

        // Nút hủy trong modal chi tiết
        const cancelBtn = document.getElementById("detailCancelBtn");
        if (order.canCancelByUser) {
            cancelBtn.classList.remove("d-none");
            cancelBtn.onclick = () => {
                detailModal?.hide();
                openCancelModal(order.id, order.orderCode);
            };
        } else {
            cancelBtn.classList.add("d-none");
            cancelBtn.onclick = null;
        }
    }

    // ==================== HỦY ĐƠN HÀNG ====================

    function openCancelModal(orderId, orderCode) {
        pendingCancel = { id: orderId, code: orderCode };

        document.getElementById("cancelOrderCode").innerText = orderCode || "—";

        const input = document.getElementById("cancelReasonInput");
        input.value = "";
        input.classList.remove("is-invalid");

        document.getElementById("cancelReasonCount").innerText = "0";
        document.getElementById("cancelReasonError").classList.add("d-none");

        const confirmBtn = document.getElementById("confirmCancelBtn");
        confirmBtn.disabled = false;
        confirmBtn.innerHTML = "Xác nhận hủy";

        cancelModal?.show();
    }

    async function submitCancel() {
        const input = document.getElementById("cancelReasonInput");
        const errorEl = document.getElementById("cancelReasonError");
        const confirmBtn = document.getElementById("confirmCancelBtn");

        const reason = input.value.trim();

        // Lý do hủy là bắt buộc
        if (!reason) {
            input.classList.add("is-invalid");
            errorEl.classList.remove("d-none");
            input.focus();
            return;
        }

        input.classList.remove("is-invalid");
        errorEl.classList.add("d-none");

        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang hủy...';

        try {
            const response = await apiFetch(`${ORDERS_API}/${pendingCancel.id}/cancel`, {
                method: "POST",
                body: JSON.stringify({ cancelReason: reason })
            });

            const payload = await response.json().catch(() => ({}));

            if (response.ok && payload.success) {
                cancelModal?.hide();
                alert(payload.message || "Đã hủy đơn hàng.");
                window.location.reload();
                return;
            }

            if (response.status === 409) {
                // Quán đã nhận đơn trong lúc người dùng đang mở modal
                cancelModal?.hide();
                alert(payload.message || "Quán đã tiếp nhận đơn hàng nên không thể hủy.");
                window.location.reload();
                return;
            }

            if (response.status === 401) {
                alert("Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục.");
                window.location.href = "/login";
                return;
            }

            errorEl.innerText = payload.message || "Không hủy được đơn hàng.";
            errorEl.classList.remove("d-none");

        } catch (error) {
            console.error("Lỗi hủy đơn hàng:", error);
            errorEl.innerText = "Không kết nối được tới máy chủ. Thử lại sau.";
            errorEl.classList.remove("d-none");

        } finally {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = "Xác nhận hủy";
        }
    }

    // ==================== KHỞI TẠO ====================

    document.addEventListener("DOMContentLoaded", () => {

        if (typeof bootstrap !== "undefined") {
            const detailEl = document.getElementById("orderDetailModal");
            const cancelEl = document.getElementById("cancelOrderModal");

            if (detailEl) detailModal = new bootstrap.Modal(detailEl);
            if (cancelEl) cancelModal = new bootstrap.Modal(cancelEl);
        }

        // Mở chi tiết / mở modal hủy
        document.addEventListener("click", e => {
            const viewBtn = e.target.closest("[data-view-order]");
            if (viewBtn) {
                e.preventDefault();
                openOrderDetail(viewBtn.getAttribute("data-view-order"));
                return;
            }

            const cancelBtn = e.target.closest("[data-cancel-order]");
            if (cancelBtn) {
                e.preventDefault();
                openCancelModal(
                    cancelBtn.getAttribute("data-cancel-order"),
                    cancelBtn.getAttribute("data-order-code")
                );
            }
        });

        // Gợi ý lý do hủy
        document.querySelectorAll(".cancel-reason-chip").forEach(chip => {
            chip.addEventListener("click", () => {
                const input = document.getElementById("cancelReasonInput");
                input.value = chip.getAttribute("data-reason") || "";
                input.dispatchEvent(new Event("input"));
                input.focus();
            });
        });

        // Đếm ký tự
        document.getElementById("cancelReasonInput")?.addEventListener("input", e => {
            document.getElementById("cancelReasonCount").innerText = e.target.value.length;

            if (e.target.value.trim()) {
                e.target.classList.remove("is-invalid");
                document.getElementById("cancelReasonError").classList.add("d-none");
            }
        });

        document.getElementById("confirmCancelBtn")?.addEventListener("click", submitCancel);
    });

})();