// ==================== MEALCHOICE NAVBAR & GLOBAL UTILITIES ====================
//
// Phần giỏ hàng đã chuyển sang /js/cart.js (giỏ hàng lưu trên server).
// File này chỉ còn xử lý: phiên đăng nhập, refresh token, menu navbar,
// yêu thích món và theo dõi quán.

(function () {
    if (window.__MEALCHOICE_NAVBAR_INITIALIZED__) return;
    window.__MEALCHOICE_NAVBAR_INITIALIZED__ = true;

    // ==================== AUTH & SESSION ====================

    function clearAuthSession() {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("displayName");
        localStorage.removeItem("userRoles");
        localStorage.removeItem("mealchoice_cart"); // dọn giỏ hàng cũ của bản localStorage
        ["accessToken", "refreshToken", "displayName", "userRoles"]
            .forEach(key => localStorage.removeItem(key));

        document.cookie = "accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
    }

    async function handleLogout(e) {
        e?.preventDefault();

        if (!confirm("Bạn có chắc chắn muốn đăng xuất không?")) return;

        try {
            const refreshToken = localStorage.getItem("refreshToken");

            await fetch("/api/auth/logout", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken: refreshToken || "" })
            });
        } catch (error) {
            console.error("Lỗi đăng xuất:", error);
        } finally {
            clearAuthSession();
            sessionStorage.clear();
            window.location.href = "/login";
        }
    }

    // ==================== AUTH FETCH WRAPPER ====================

    function getAuthHeaders() {
        const token = localStorage.getItem("accessToken");
        return token ? { Authorization: `Bearer ${token}` } : {};
    }

    let refreshTokenPromise = null;

    async function doRefreshToken() {
        if (refreshTokenPromise) return refreshTokenPromise;

        refreshTokenPromise = (async () => {
            const refreshToken = localStorage.getItem("refreshToken");

            if (!refreshToken) {
                clearAuthSession();
                window.location.href = "/login";
                return null;
            }

            try {
                const response = await fetch("/api/auth/refresh-token", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify({ refreshToken })
                });

                if (response.ok) {
                    const data = await response.json();

                    if (data.accessToken) {
                        localStorage.setItem("accessToken", data.accessToken);

                        document.cookie =
                            `accessToken=${data.accessToken}; path=/; max-age=86400; SameSite=Lax`;

                        if (data.refreshToken) {
                            localStorage.setItem("refreshToken", data.refreshToken);
                        }

                        return data.accessToken;
                    }
                }

                console.warn("Refresh Token đã hết hạn hoặc không hợp lệ. Đăng xuất...");
                clearAuthSession();
                window.location.href = "/login";
                return null;
            } catch (error) {
                console.error("Lỗi kết nối khi làm mới token:", error);
                return null;
            } finally {
                refreshTokenPromise = null;
            }
        })();

        return refreshTokenPromise;
    }

    // Tự động refresh token và gọi lại request khi dính HTTP 401 hoặc bị chuyển hướng
    async function fetchWithAuth(url, options = {}) {
        const defaultHeaders = {
            "Accept": "application/json"
        };
        if (options.body && typeof options.body === 'string' && (!options.headers || !options.headers['Content-Type'])) {
            defaultHeaders["Content-Type"] = "application/json";
        }

        options.headers = {
            ...defaultHeaders,
            ...options.headers,
            ...getAuthHeaders()
        };

        let response = await fetch(url, options);

        const isUnauthorized =
            response.status === 401 ||
            (response.redirected && response.url.includes("/login"));

        if (isUnauthorized) {
            console.log("Token hết hạn, đang refresh...");

            const newToken = await doRefreshToken();

            if (newToken) {
                options.headers = {
                    Accept: "application/json",
                    ...options.headers,
                    ...getAuthHeaders()
                };

                response = await fetch(url, options);
            }
        }

        return response;
    }

    window.getAuthHeaders = getAuthHeaders;
    window.fetchWithAuth = fetchWithAuth;

    function getUserRoles() {
        try {
            return JSON.parse(localStorage.getItem("userRoles") || "[]");
        } catch (error) {
            console.error("Không đọc được userRoles:", error);
            return [];
        }
    }

    // ==================== NAVBAR ====================

    function hideMerchantNavigation() {
        document.getElementById("registerMerchantNav")?.classList.add("d-none");
        document.getElementById("merchantPendingNav")?.classList.add("d-none");
        document.getElementById("merchantRoleNav")?.classList.add("d-none");
    }

    function showMerchantRegister() {
        document.getElementById("registerMerchantNav")?.classList.remove("d-none");
        document.getElementById("merchantPendingNav")?.classList.add("d-none");
        document.getElementById("merchantRoleNav")?.classList.add("d-none");
    }

    function showMerchantPending() {
        document.getElementById("registerMerchantNav")?.classList.add("d-none");
        document.getElementById("merchantPendingNav")?.classList.remove("d-none");
        document.getElementById("merchantRoleNav")?.classList.add("d-none");
    }

    function showMerchantChannel() {
        document.getElementById("registerMerchantNav")?.classList.add("d-none");
        document.getElementById("merchantPendingNav")?.classList.add("d-none");
        document.getElementById("merchantRoleNav")?.classList.remove("d-none");
    }

    function initNavbar() {
        const displayName = localStorage.getItem("displayName");
        const accessToken = localStorage.getItem("accessToken");
        const roles = getUserRoles();

        const guestAuthNav = document.getElementById("guestAuthNav");
        const userDropdownNav = document.getElementById("userDropdownNav");
        const adminRoleNav = document.getElementById("adminRoleNav");

        const isLoggedIn = !!(displayName || accessToken);

        const isAdmin =
            displayName === "Admin" ||
            roles.includes("ROLE_ADMIN") ||
            roles.includes("ADMIN");

        if (!isLoggedIn) {
            guestAuthNav?.classList.remove("d-none");
            userDropdownNav?.classList.add("d-none");
            adminRoleNav?.classList.add("d-none");
            hideMerchantNavigation();
            return;
        }

        guestAuthNav?.classList.add("d-none");
        userDropdownNav?.classList.remove("d-none");

        const userName = document.getElementById("navbarUserName");

        if (userName && displayName) {
            userName.replaceChildren(document.createTextNode(displayName));
        }

        if (isAdmin) {
            adminRoleNav?.classList.remove("d-none");
            hideMerchantNavigation();
        } else {
            adminRoleNav?.classList.add("d-none");
            checkMerchantStatus();
        }

        const redirectUrl = sessionStorage.getItem("redirectAfterLogin");

        if (redirectUrl) {
            sessionStorage.removeItem("redirectAfterLogin");
            window.location.href = redirectUrl;
        }
    }

    async function checkMerchantStatus() {
        const token = localStorage.getItem("accessToken");

        if (!token) {
            console.log("[Merchant] Không có accessToken");
            return;
        }

        try {
            console.log("[Merchant] Đang kiểm tra /api/merchant/my-status...");

            const response = await fetchWithAuth("/api/merchant/my-status");

            console.log("[Merchant] Response status:", response.status);
            console.log("[Merchant] Response URL:", response.url);

            if (!response.ok) {
                console.error("[Merchant] API lỗi:", response.status);
                return;
            }

            const contentType = response.headers.get("content-type");
            console.log("[Merchant] Content-Type:", contentType);

            if (!contentType?.includes("application/json")) {
                console.error("[Merchant] Response không phải JSON");
                return;
            }

            const data = await response.json();

            const registerNav = document.getElementById("registerMerchantNav");
            const pendingNav = document.getElementById("merchantPendingNav");
            const merchantNav = document.getElementById("merchantRoleNav");
            console.log("[Merchant] API my-status trả về:", data);
            console.log("[Merchant] registered:", data.registered);
            console.log("[Merchant] status:", data.status);

            hideMerchantNavigation();

            if (!data.registered) {
                console.log("[Merchant] Chưa đăng ký → hiện Đăng ký Merchant");
                showMerchantRegister();
                return;
            }

            if (data.status === "PENDING") {
                console.log("[Merchant] PENDING → hiện Chờ duyệt");
                showMerchantPending();
                return;
            }

            if (data.status === "REJECTED") {
                console.log("[Merchant] REJECTED → hiện Đăng ký Merchant");
                showMerchantRegister();
                return;
            }

            if (data.status === "APPROVED") {
                console.log("[Merchant] APPROVED → hiện Kênh Merchant");
                showMerchantChannel();
                return;
            }

            console.warn("[Merchant] Status không xác định:", data.status);
        } catch (error) {
            console.error("[Merchant] Lỗi kiểm tra trạng thái:", error);
        }
    }

    function initMerchantBlockedModal() {
        const button = document.getElementById("merchantChannelBtn");

        if (!button) return;

        button.addEventListener("click", function (e) {
            if (this.dataset.blocked !== "true") return;

            e.preventDefault();

            const modalElement = document.getElementById("merchantBlockedModal");

            if (modalElement && typeof bootstrap !== "undefined") {
                bootstrap.Modal
                    .getOrCreateInstance(modalElement)
                    .show();
            }
        });
    }

    // ==================== YÊU THÍCH MÓN ====================

    document.addEventListener("click", async e => {
        const button = e.target.closest(".btn-wishlist");
        if (!button) return;

        e.preventDefault();
        e.stopPropagation();

        const isLoggedIn =
            localStorage.getItem("displayName") || localStorage.getItem("accessToken");

        if (!isLoggedIn) {
            alert("Vui lòng đăng nhập để thực hiện chức năng này!");

            sessionStorage.setItem(
                "redirectAfterLogin",
                window.location.href
            );

            window.location.href = "/login";
            return;
        }

        const foodId = button.dataset.foodId;
        const icon = button.querySelector("i");

        if (!foodId || !icon) return;

        const isLiked = icon.classList.contains("bi-heart-fill");
        const url = isLiked
            ? `/api/foods/${foodId}/unlike`
            : `/api/foods/${foodId}/like`;

        try {
            const response = await fetchWithAuth(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                throw new Error("Wishlist request failed");
            }

            icon.classList.toggle("bi-heart-fill", !isLiked);
            icon.classList.toggle("bi-heart", isLiked);
            button.title = isLiked ? "Yêu thích" : "Bỏ yêu thích";
        } catch (error) {
            console.error("Lỗi Wishlist:", error);
            alert("Có lỗi xảy ra, vui lòng thử lại sau!");
        }
    });

    // ==================== THEO DÕI QUÁN ====================

    document.addEventListener("click", async e => {
        const button = e.target.closest(".btn-follow-merchant");
        if (!button) return;

        e.preventDefault();
        e.stopPropagation();

        const isLoggedIn =
            localStorage.getItem("displayName") || localStorage.getItem("accessToken");

        if (!isLoggedIn) {
            alert("Vui lòng đăng nhập để thực hiện chức năng này!");
            sessionStorage.setItem("redirectAfterLogin", window.location.href);
            window.location.href = "/login";
            return;
        }

        const merchantId = button.dataset.merchantId;
        if (!merchantId) return;

        const isFollowed = button.classList.contains("btn-danger");

        const url = isFollowed
            ? `/api/merchants/${merchantId}/unfollow`
            : `/api/merchants/${merchantId}/follow`;

        try {
            const response = await fetchWithAuth(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                throw new Error("Follow request failed: " + response.status);
            }

            let data = {};
            const contentType = response.headers.get("content-type");

            if (contentType?.includes("application/json")) {
                data = await response.json();
                console.log("=== FOLLOW DEBUG === response data:", JSON.stringify(data));
            }

            const newFollowed =
                typeof data.followed !== "undefined"
                    ? data.followed
                    : !isFollowed;

            document
                .querySelectorAll(
                    `.btn-follow-merchant[data-merchant-id="${merchantId}"]`
                )
                .forEach(btn => {
                    btn.classList.toggle("btn-danger", newFollowed);
                    btn.classList.toggle("btn-outline-danger", !newFollowed);

                    const icon = btn.querySelector("i");
                    const text = btn.querySelector("span");

                    if (icon) {
                        icon.className = newFollowed
                            ? "bi bi-check-lg me-1"
                            : "bi bi-plus-lg me-1";
                    }

                    if (text) {
                        text.innerText = newFollowed
                            ? "Đang theo dõi"
                            : "Theo dõi";
                    }
                });

            const followerCountEl =
                document.getElementById("storeFollowerCount");

            if (
                followerCountEl &&
                typeof data.followerCount !== "undefined"
            ) {
                followerCountEl.innerText = data.followerCount;
            }

        } catch (error) {
            console.error("Lỗi Follow Merchant:", error);
            alert("Có lỗi xảy ra, vui lòng thử lại sau!");
        }
    });


    // ==================== CHỐNG ẢNH LỖI LẶP VÔ HẠN ====================
    //
    // Trước đây avatar dùng onerror="this.src='/images/default-avatar.png'".
    // File đó không tồn tại, nên mỗi lần lỗi lại gán đúng URL vừa lỗi ->
    // trình duyệt bắn request 404 liên tục cho tới khi rời trang.
    //
    // Bộ lọc dưới đây bắt sự kiện error ở pha capture (sự kiện error của <img>
    // không nổi bọt nên bắt buộc dùng capture), thay ảnh bằng ảnh dự phòng ĐÚNG
    // MỘT LẦN rồi đánh dấu; lần lỗi sau không đổi src nữa. Áp dụng cho mọi <img>
    // trên toàn site, kể cả ảnh do JS render sau này.

    const DEFAULT_IMAGE_FALLBACK = "/images/default-food.svg";

    document.addEventListener("error", event => {
        const img = event.target;

        if (!img || img.tagName !== "IMG") {
            return;
        }

        // Đã thay ảnh dự phòng mà vẫn lỗi: dừng hẳn, không thử lại nữa
        if (img.dataset.fallbackApplied === "1") {
            img.onerror = null;
            return;
        }

        img.dataset.fallbackApplied = "1";
        img.onerror = null;

        const fallback = img.dataset.fallback || DEFAULT_IMAGE_FALLBACK;

        // Ảnh dự phòng trùng với ảnh vừa lỗi thì đừng tải lại
        if (img.getAttribute("src") === fallback) {
            return;
        }

        img.src = fallback;
    }, true);

    // ==================== XUẤT RA GLOBAL ====================

    window.clearAuthSession = clearAuthSession;
    window.handleLogout = handleLogout;
    window.getAuthHeaders = getAuthHeaders;
    window.fetchWithAuth = fetchWithAuth;

})();