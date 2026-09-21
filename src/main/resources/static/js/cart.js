// ==================== MEALCHOICE CART (GIỎ HÀNG LƯU TRÊN SERVER) ====================
//
// Giỏ hàng được lưu trong database qua /api/cart. File này giữ một bản sao trong
// bộ nhớ để các hàm cũ (getGlobalCart, addToCartGlobal, changeQuantityGlobal,
// saveGlobalCart) vẫn gọi đồng bộ được như trước, nên các trang cũ không cần sửa.
//
// Giá món KHÔNG lưu ở client. Mỗi lần đồng bộ, server trả về giá mới nhất từ
// bảng foods, nên người dùng không bao giờ thấy giá cũ.

(function () {
    if (window.__MEALCHOICE_CART_INITIALIZED__) {
        return;
    }
    window.__MEALCHOICE_CART_INITIALIZED__ = true;

    const CART_API = "/api/cart";

    // Bản sao giỏ hàng trong bộ nhớ, luôn được cập nhật từ phản hồi của server
    let cartSummary = emptySummary();
    let cartItems = [];
    let lastSyncFailed = false;

    function emptySummary() {
        return {
            id: null,
            merchantId: null,
            merchantName: null,
            merchantBankName: null,
            merchantBankAccountNumber: null,
            items: [],
            totalItems: 0,
            originalSubtotal: 0,
            subtotalPrice: 0,
            savings: 0,
            serviceFee: 0,
            estimatedTotal: 0,
            hasUnavailableItems: false,
            empty: true
        };
    }

    // ==================== TIỆN ÍCH ====================

    function isLoggedIn() {
        if (localStorage.getItem("accessToken") || localStorage.getItem("displayName")) {
            return true;
        }

        // Đăng nhập ở tab khác, hoặc localStorage bị xóa nhưng cookie còn hạn
        return /(^|;\s*)accessToken=[^;]+/.test(document.cookie);
    }

    function formatCurrencyGlobal(value) {
        return new Intl.NumberFormat("vi-VN").format(Math.round(Number(value) || 0)) + " đ";
    }

    function redirectToLogin() {
        sessionStorage.setItem("redirectAfterLogin", window.location.href);
        window.location.href = "/login";
    }

    function requireLogin(actionLabel) {
        if (isLoggedIn()) {
            return true;
        }

        alert("Vui lòng đăng nhập để " + actionLabel + ".");
        redirectToLogin();
        return false;
    }

    async function callCartApi(path, options = {}) {
        const fetchFn = (typeof window.fetchWithAuth === "function")
            ? window.fetchWithAuth
            : fetch;

        const headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        };

        const token = localStorage.getItem("accessToken");
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        const response = await fetchFn(CART_API + path, {
            ...options,
            headers: { ...headers, ...(options.headers || {}) }
        });

        let payload = null;
        try {
            payload = await response.json();
        } catch (e) {
            payload = null;
        }

        return { response, payload };
    }

    /**
     * Chuyển dữ liệu server sang cấu trúc mà các trang cũ đang dùng.
     * Giữ nguyên `id` = foodId để những chỗ gọi changeQuantityGlobal(foodId, delta) vẫn chạy.
     */
    function toLegacyItems(summary) {
        if (!summary || !Array.isArray(summary.items)) {
            return [];
        }

        return summary.items.map(item => ({
            id: item.foodId,
            cartItemId: item.id,
            name: item.foodName,
            price: Number(item.price || 0),
            discountPrice: Number(item.effectivePrice || item.price || 0),
            serviceFee: Number(item.serviceFee || 0),
            merchantId: summary.merchantId,
            merchantName: summary.merchantName,
            image: item.foodImage,
            quantity: Number(item.quantity || 0),
            note: item.note || "",
            available: item.available !== false,
            unavailableReason: item.unavailableReason || null
        }));
    }

    function applySummary(summary) {
        cartSummary = summary || emptySummary();
        cartItems = toLegacyItems(cartSummary);
        lastSyncFailed = false;

        window.dispatchEvent(new CustomEvent("cartUpdated", { detail: cartSummary }));
    }

    // ==================== ĐỒNG BỘ VỚI SERVER ====================

    async function refreshCart() {
        if (!isLoggedIn()) {
            applySummary(emptySummary());
            return cartSummary;
        }

        try {
            const { response, payload } = await callCartApi("", { method: "GET" });

            if (response.status === 401) {
                console.warn("[cart] /api/cart trả về 401, phiên đăng nhập không hợp lệ.");
                applySummary(emptySummary());
                return cartSummary;
            }

            if (response.ok && payload && payload.success) {
                applySummary(payload.data);
                return cartSummary;
            }

            lastSyncFailed = true;
            console.warn(
                "[cart] GET /api/cart thất bại:",
                response.status,
                payload && payload.message ? payload.message : ""
            );
        } catch (error) {
            console.error("[cart] Không gọi được /api/cart:", error);
            lastSyncFailed = true;
        }

        return cartSummary;
    }

    // ==================== THÊM MÓN ====================

    /**
     * Hỗ trợ cả hai cách gọi cũ:
     *   addToCartGlobal(foodObject)
     *   addToCartGlobal(id, name, price, discountPrice, serviceFee, merchantId, merchantName, image)
     */
    async function addToCartGlobal(idOrItem, name, price, discountPrice,
        serviceFee = 0, merchantId = null,
        merchantName = null, image = null) {

        if (!requireLogin("thêm món vào giỏ hàng")) {
            return null;
        }

        let foodId;
        let quantity = 1;
        let note = "";

        if (typeof idOrItem === "object" && idOrItem !== null) {
            foodId = idOrItem.id;
            quantity = Number(idOrItem.quantity || 1);
            note = idOrItem.note || "";
        } else {
            foodId = idOrItem;
        }

        if (!foodId) {
            console.warn("addToCartGlobal: thiếu foodId");
            return null;
        }

        return await postAddItem(foodId, quantity, note, false);
    }

    async function postAddItem(foodId, quantity, note, replaceCart) {
        try {
            const { response, payload } = await callCartApi("/items", {
                method: "POST",
                body: JSON.stringify({
                    foodId: foodId,
                    quantity: quantity,
                    note: note,
                    replaceCart: replaceCart
                })
            });

            if (response.status === 401) {
                redirectToLogin();
                return null;
            }

            // [ĐA QUÁN] Ràng buộc 1 quán đã được tắt ở backend nên 409 không còn xảy ra.
            // Giữ lại comment để dễ rollback nếu cần.
            //
            // if (response.status === 409 && payload && payload.error === "MERCHANT_CONFLICT") {
            //     const confirmed = confirm(
            //         `Giỏ hàng của bạn đang có món từ "${payload.currentMerchantName}".\n` +
            //         `Mỗi đơn hàng chỉ đặt được từ một quán.\n\n` +
            //         `Xóa giỏ hàng cũ để thêm món từ "${payload.newMerchantName}"?`
            //     );
            //     if (!confirmed) { return null; }
            //     return await postAddItem(foodId, quantity, note, true);
            // }

            if (response.ok && payload && payload.success) {
                applySummary(payload.data);
                return cartSummary;
            }

            alert((payload && payload.message) || "Không thêm được món vào giỏ hàng.");
            return null;

        } catch (error) {
            console.error("Lỗi thêm món vào giỏ:", error);
            alert("Không kết nối được tới máy chủ. Vui lòng thử lại.");
            return null;
        }
    }

    // ==================== SỬA SỐ LƯỢNG ====================

    function findCartItemIdByFoodId(foodId) {
        const item = cartItems.find(i => String(i.id) === String(foodId));
        return item ? item.cartItemId : null;
    }

    async function patchItem(cartItemId, body) {
        if (!cartItemId) {
            return null;
        }

        try {
            const { response, payload } = await callCartApi(`/items/${cartItemId}`, {
                method: "PATCH",
                body: JSON.stringify(body)
            });

            if (response.status === 401) {
                redirectToLogin();
                return null;
            }

            if (response.ok && payload && payload.success) {
                applySummary(payload.data);
                return cartSummary;
            }

            alert((payload && payload.message) || "Không cập nhật được giỏ hàng.");
            return null;

        } catch (error) {
            console.error("Lỗi cập nhật giỏ hàng:", error);
            alert("Không kết nối được tới máy chủ. Vui lòng thử lại.");
            return null;
        }
    }

    /**
     * Cộng/trừ số lượng theo foodId. Về 0 thì món tự bị xóa khỏi giỏ.
     */
    async function changeQuantityGlobal(foodId, delta) {
        const cartItemId = findCartItemIdByFoodId(foodId);

        if (!cartItemId) {
            return null;
        }

        return await patchItem(cartItemId, { quantityDelta: delta });
    }

    /**
     * Đặt số lượng cụ thể theo foodId.
     */
    async function setQuantityGlobal(foodId, quantity) {
        const cartItemId = findCartItemIdByFoodId(foodId);

        if (!cartItemId) {
            return null;
        }

        return await patchItem(cartItemId, { quantity: quantity });
    }

    /**
     * Sửa ghi chú của một món trong giỏ.
     */
    async function updateItemNoteGlobal(foodId, note) {
        const cartItemId = findCartItemIdByFoodId(foodId);

        if (!cartItemId) {
            return null;
        }

        return await patchItem(cartItemId, { note: note });
    }

    // ==================== XÓA MÓN / XÓA GIỎ ====================

    async function removeFromCartGlobal(foodId) {
        const cartItemId = findCartItemIdByFoodId(foodId);

        if (!cartItemId) {
            return null;
        }

        try {
            const { response, payload } = await callCartApi(`/items/${cartItemId}`, {
                method: "DELETE"
            });

            if (response.status === 401) {
                redirectToLogin();
                return null;
            }

            if (response.ok && payload && payload.success) {
                applySummary(payload.data);
                return cartSummary;
            }

            alert((payload && payload.message) || "Không xóa được món khỏi giỏ hàng.");
            return null;

        } catch (error) {
            console.error("Lỗi xóa món khỏi giỏ:", error);
            return null;
        }
    }

    async function clearGlobalCart() {
        if (!isLoggedIn()) {
            applySummary(emptySummary());
            return cartSummary;
        }

        try {
            const { response, payload } = await callCartApi("", { method: "DELETE" });

            if (response.ok && payload && payload.success) {
                applySummary(payload.data);
                return cartSummary;
            }
        } catch (error) {
            console.error("Lỗi xóa giỏ hàng:", error);
        }

        return cartSummary;
    }

    /**
     * Tương thích ngược với saveGlobalCart(danh_sách) của bản localStorage.
     * Đồng bộ giỏ hàng trên server cho khớp với danh sách truyền vào.
     */
    async function saveGlobalCart(list) {
        if (!Array.isArray(list) || list.length === 0) {
            return await clearGlobalCart();
        }

        if (!requireLogin("lưu giỏ hàng")) {
            return null;
        }

        await refreshCart();

        // Thêm hoặc chỉnh các món có trong danh sách
        for (const wanted of list) {
            if (!wanted || !wanted.id) {
                continue;
            }

            const wantedQty = Number(wanted.quantity || 1);
            const existing = cartItems.find(i => String(i.id) === String(wanted.id));

            if (!existing) {
                await postAddItem(wanted.id, wantedQty, wanted.note || "", true);
            } else if (existing.quantity !== wantedQty) {
                await setQuantityGlobal(wanted.id, wantedQty);
            }
        }

        // Bỏ những món không còn trong danh sách
        const wantedIds = list.map(i => String(i.id));

        for (const current of [...cartItems]) {
            if (!wantedIds.includes(String(current.id))) {
                await removeFromCartGlobal(current.id);
            }
        }

        return cartSummary;
    }

    // ==================== ĐỌC GIỎ HÀNG (ĐỒNG BỘ) ====================

    function getGlobalCart() {
        return cartItems.map(item => ({ ...item }));
    }

    function getCartSummary() {
        return { ...cartSummary };
    }

    function isCartReady() {
        return !lastSyncFailed;
    }

    // ==================== GIAO DIỆN GIỎ HÀNG TRÊN NAVBAR ====================

    function updateNavbarCartUI() {
        const countEl = document.getElementById("navbarCartCount");
        const itemsEl = document.getElementById("navbarCartItems");
        const totalEl = document.getElementById("navbarCartTotal");
        const checkoutBtn = document.getElementById("btnNavbarCheckout");

        if (!countEl) {
            return;
        }

        const items = cartItems;

        if (itemsEl) {
            itemsEl.innerHTML = "";
        }

        if (!items.length) {
            countEl.innerText = "0";

            if (itemsEl) {
                itemsEl.innerHTML = isLoggedIn()
                    ? `<div class="text-center py-4 text-muted fs-8">
                           <i class="bi bi-basket fs-2 d-block opacity-50 mb-1"></i>
                           <span>Chưa có món nào. Chọn món để bắt đầu.</span>
                       </div>`
                    : `<div class="text-center py-4 text-muted fs-8">
                           <i class="bi bi-person-lock fs-2 d-block opacity-50 mb-1"></i>
                           <span>Đăng nhập để dùng giỏ hàng</span>
                       </div>`;
            }

            checkoutBtn?.setAttribute("disabled", "true");
            checkoutBtn?.classList.add("opacity-50");

            if (totalEl) {
                totalEl.innerText = formatCurrencyGlobal(0);
            }

            return;
        }

        checkoutBtn?.removeAttribute("disabled");
        checkoutBtn?.classList.remove("opacity-50");

        items.forEach(item => {
            if (!itemsEl) {
                return;
            }

            const div = document.createElement("div");
            div.className =
                "d-flex align-items-center justify-content-between mb-2 pb-2 border-bottom border-light-subtle fs-8";

            const unavailableBadge = item.available
                ? ""
                : `<span class="badge bg-danger-subtle text-danger border border-danger-subtle d-block mt-1">Hết phục vụ</span>`;

            div.innerHTML = `
                <div class="pe-2 overflow-hidden flex-grow-1" style="max-width: 180px;">
                    <span class="d-block fw-semibold text-dark text-truncate" title="${item.name}">
                        ${item.name}
                    </span>
                    <span class="text-danger fw-bold">${formatCurrencyGlobal(item.discountPrice)}</span>
                    ${unavailableBadge}
                </div>

                <div class="d-flex align-items-center gap-1.5 flex-shrink-0">
                    <button type="button"
                            class="btn btn-xs btn-outline-secondary rounded-circle d-flex align-items-center justify-content-center"
                            style="width:18px;height:18px;padding:0;"
                            data-cart-decrease="${item.id}">-</button>

                    <span class="fw-bold" style="min-width:12px;text-align:center;">${item.quantity}</span>

                    <button type="button"
                            class="btn btn-xs btn-outline-danger rounded-circle d-flex align-items-center justify-content-center"
                            style="width:18px;height:18px;padding:0;"
                            data-cart-increase="${item.id}">+</button>
                </div>
            `;

            itemsEl.appendChild(div);
        });

        countEl.innerText = cartSummary.totalItems || 0;

        if (totalEl) {
            totalEl.innerText = formatCurrencyGlobal(cartSummary.estimatedTotal || 0);
        }

        if (cartSummary.hasUnavailableItems && checkoutBtn) {
            checkoutBtn.setAttribute("disabled", "true");
            checkoutBtn.classList.add("opacity-50");
        }
    }

    // Bấm +/- trong dropdown giỏ hàng của navbar
    document.addEventListener("click", async e => {
        const decreaseBtn = e.target.closest("[data-cart-decrease]");
        const increaseBtn = e.target.closest("[data-cart-increase]");

        if (!decreaseBtn && !increaseBtn) {
            return;
        }

        e.preventDefault();
        e.stopPropagation();

        const btn = decreaseBtn || increaseBtn;
        btn.disabled = true;

        const foodId = decreaseBtn
            ? decreaseBtn.getAttribute("data-cart-decrease")
            : increaseBtn.getAttribute("data-cart-increase");

        await changeQuantityGlobal(Number(foodId), decreaseBtn ? -1 : 1);

        btn.disabled = false;
    });

    // ==================== KHỞI TẠO ====================

    document.addEventListener("DOMContentLoaded", () => {

        refreshCart();

        document.getElementById("btnNavbarCheckout")
            ?.addEventListener("click", e => {
                e.preventDefault();
                e.stopPropagation();

                if (cartItems.length) {
                    window.location.href = "/checkout";
                }
            });
    });

    window.addEventListener("cartUpdated", updateNavbarCartUI);

    // ==================== XUẤT RA GLOBAL ====================

    window.getGlobalCart = getGlobalCart;
    window.getCartSummary = getCartSummary;
    window.isCartReady = isCartReady;
    window.refreshCart = refreshCart;

    window.addToCartGlobal = addToCartGlobal;
    window.addToGlobalCart = addToCartGlobal;

    window.changeQuantityGlobal = changeQuantityGlobal;
    window.setQuantityGlobal = setQuantityGlobal;
    window.updateItemNoteGlobal = updateItemNoteGlobal;

    window.removeFromCartGlobal = removeFromCartGlobal;
    window.clearGlobalCart = clearGlobalCart;
    window.saveGlobalCart = saveGlobalCart;

    window.updateNavbarCartUI = updateNavbarCartUI;
    window.formatCurrencyGlobal = formatCurrencyGlobal;

})();