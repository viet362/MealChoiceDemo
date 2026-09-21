document.addEventListener("DOMContentLoaded", function () {
    // Chờ HTML tải xong rồi xác định form đang sử dụng
    const form = document.getElementById("foodCreateForm") || document.getElementById("foodEditForm");
    if (!form) return;

    // Xác định trang hiện tại là thêm món hay sửa món
    const mode = document.body.dataset.foodMode || (form.id === "foodEditForm" ? "edit" : "create");

    // Lấy các phần tử dùng chung cho cả hai form
    const submitButton = document.getElementById("submitButton");
    const priceInput = document.getElementById("price");
    const discountType = document.getElementById("discountType");
    const discountValue = document.getElementById("discountValue");
    const discountValueGroup = document.getElementById("discountValueGroup");
    const discountValueLabel = document.getElementById("discountValueLabel");
    const discountUnit = document.getElementById("discountUnit");
    const discountPrice = document.getElementById("discountPrice");
    const serviceFeeInput = document.getElementById("serviceFee");
    const imageInput = document.getElementById("images");
    const imagePreview = document.getElementById("imagePreview");
    const imageDropZone = document.getElementById("imageDropZone");
    const foodId = form.dataset.foodId;

    let selectedFiles = [];
    const deletedImages = new Set();

    // Xử lý dùng chung cho cả thêm và sửa món
    function buildAuthHeaders() {
        const token = localStorage.getItem("accessToken");
        return token ? {Authorization: `Bearer ${token}`} : {};
    }

    function showError(title, message) {
        window.showMerchantNotice?.({
            variant: "danger",
            title,
            message
        });
    }

    function showSuccess(title, message) {
        window.showMerchantNotice?.({
            variant: "success",
            title,
            message
        });
    }

    // Cập nhật nội dung và số lượng đã chọn của dropdown
    function updateMultiSelectText(wrapper) {
        const selectedText = wrapper.querySelector(".multi-select-selected");
        const selectedCount = wrapper.querySelector(".selected-count");
        const checkboxes = wrapper.querySelectorAll(".multi-select-checkbox:checked");
        const count = checkboxes.length;

        if (selectedCount) {
            selectedCount.textContent = count;
        }

        if (!selectedText) return;

        if (wrapper.id === "categorySelector") {
            selectedText.textContent = count > 0
                ? `Đã chọn ${count} danh mục`
                : "Chọn danh mục món ăn";
        } else if (wrapper.id === "tagSelector") {
            selectedText.textContent = count > 0
                ? `Đã chọn ${count} tag`
                : "Chọn tag";
        } else if (wrapper.id === "couponSelector") {
            selectedText.textContent = count > 0
                ? `Đã chọn ${count} coupon`
                : "Chọn coupon áp dụng";
        }
    }

    // Khởi tạo dropdown chọn nhiều danh mục, tag và coupon
    function initMultiSelect(wrapperId) {
        const wrapper = document.getElementById(wrapperId);
        if (!wrapper) return;

        const toggle = wrapper.querySelector(".multi-select-toggle");
        const search = wrapper.querySelector(".multi-select-search");
        const list = wrapper.querySelector(".multi-select-list");

        if (!toggle || !list) return;

        // Bấm vào ô chọn để mở hoặc đóng danh sách
        toggle.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            document.querySelectorAll(".multi-select.open").forEach(item => {
                if (item !== wrapper) {
                    item.classList.remove("open");
                    item.querySelector(".multi-select-toggle")
                        ?.setAttribute("aria-expanded", "false");
                }
            });

            const isOpen = wrapper.classList.toggle("open");
            toggle.setAttribute("aria-expanded", String(isOpen));
        });

        // Chọn hoặc bỏ chọn danh mục, tag, coupon
        list.addEventListener("click", function (event) {
            const item = event.target.closest(".multi-select-item");
            if (!item) return;

            const checkbox = item.querySelector(".multi-select-checkbox");
            if (!checkbox) return;

            if (event.target !== checkbox) {
                checkbox.checked = !checkbox.checked;
            }

            updateMultiSelectText(wrapper);
        });

        // Tìm kiếm trong danh sách
        if (search) {
            search.addEventListener("input", function () {
                const keyword = this.value.trim().toLowerCase();
                let visibleCount = 0;

                list.querySelectorAll(".multi-select-item").forEach(item => {
                    const label = item.dataset.label?.toLowerCase() || "";

                    if (label.includes(keyword)) {
                        item.style.display = "";
                        visibleCount++;
                    } else {
                        item.style.display = "none";
                    }
                });

                const empty = list.querySelector(".multi-select-empty");

                if (empty) {
                    empty.classList.toggle("d-none", visibleCount > 0);
                }
            });
        }

        // Hiển thị đúng trạng thái khi mở trang sửa món
        updateMultiSelectText(wrapper);
    }

    // Đóng dropdown khi click ra bên ngoài
    function closeMultiSelects(event) {
        if (event.target.closest(".multi-select")) return;

        document.querySelectorAll(".multi-select.open").forEach(wrapper => {
            wrapper.classList.remove("open");
            wrapper.querySelector(".multi-select-toggle")
                ?.setAttribute("aria-expanded", "false");
        });
    }

    // Lấy danh sách ID đang được chọn
    function getSelectedValues(selectorId) {
        return Array.from(
            document.querySelectorAll(`#${selectorId} .multi-select-checkbox:checked`)
        ).map(checkbox => checkbox.value);
    }

    // Hiển thị hoặc ẩn phần nhập mức giảm
    function updateDiscountUI() {
        if (!discountType || !discountValueGroup) return;

        const type = discountType.value;

        if (type === "NONE") {
            discountValueGroup.classList.add("d-none");
            discountValue.value = "";
            discountValue.removeAttribute("required");
            discountPrice.value = priceInput.value || "";
            discountPrice.readOnly = true;
            return;
        }

        if (type === "FIXED_PRICE") {
            discountValueGroup.classList.add("d-none");
            discountValue.value = "";
            discountValue.removeAttribute("required");
            discountPrice.readOnly = false;
            return;
        }

        if (type === "PERCENT") {
            discountValueGroup.classList.remove("d-none");
            discountValue.setAttribute("required", "required");
            discountValueLabel.textContent = "Mức giảm";
            discountUnit.textContent = "%";
            discountValue.placeholder = "20";
            discountValue.min = "0";
            discountValue.max = "99.99";
            discountValue.step = "0.01";
            discountPrice.readOnly = true;
            calculateDiscountPrice();
        }
    }

    // Tính giá sau giảm khi giảm theo phần trăm
    function calculateDiscountPrice() {
        if (!priceInput || !discountType || !discountValue || !discountPrice) return;

        const price = Number(priceInput.value);
        const type = discountType.value;
        const value = Number(discountValue.value);

        if (!Number.isFinite(price) || price <= 0) {
            if (type === "PERCENT") {
                discountPrice.value = "";
            }
            return;
        }

        if (type === "NONE") {
            discountPrice.value = price;
            return;
        }

        if (type === "FIXED_PRICE") {
            return;
        }

        if (type === "PERCENT") {
            if (!Number.isFinite(value) || value <= 0 || value >= 100) {
                discountPrice.value = "";
                return;
            }

            discountPrice.value = (price * (100 - value) / 100).toFixed(2);
        }
    }

    // Kiểm tra dữ liệu trước khi gửi form
    function validateForm() {
        const foodName = document.getElementById("foodName")?.value.trim();
        const merchantAddressId = document.getElementById("merchantAddressId")?.value;
        const preparationTime = Number(document.getElementById("preparationTime")?.value);
        const price = Number(priceInput?.value);
        const serviceFee = Number(serviceFeeInput?.value || 0);
        const type = discountType?.value;
        const value = Number(discountValue?.value);
        const finalPrice = Number(discountPrice?.value);
        const categoryIds = getSelectedValues("categorySelector");

        if (!foodName) {
            showError("Thiếu thông tin", "Vui lòng nhập tên món ăn.");
            return false;
        }

        if (!merchantAddressId) {
            showError("Thiếu thông tin", "Vui lòng chọn địa chỉ cửa hàng.");
            return false;
        }

        if (categoryIds.length === 0) {
            showError("Thiếu thông tin", "Vui lòng chọn ít nhất một danh mục món ăn.");
            return false;
        }

        if (!Number.isFinite(preparationTime) || preparationTime < 0) {
            showError(
                "Thời gian không hợp lệ",
                "Thời gian chuẩn bị phải lớn hơn hoặc bằng 0 phút."
            );
            return false;
        }

        if (!Number.isFinite(price) || price <= 0) {
            showError("Giá tiền không hợp lệ", "Giá bán phải lớn hơn 0.");
            priceInput.focus();
            return false;
        }

        if (type === "FIXED_PRICE") {
            if (!Number.isFinite(finalPrice) || finalPrice <= 0) {
                showError(
                    "Giá sau khuyến mãi không hợp lệ",
                    "Giá sau khuyến mãi phải lớn hơn 0."
                );
                discountPrice.focus();
                return false;
            }

            if (finalPrice >= price) {
                showError(
                    "Giá sau khuyến mãi không hợp lệ",
                    "Giá sau khuyến mãi phải nhỏ hơn giá bán gốc."
                );
                discountPrice.focus();
                return false;
            }
        }

        if (type === "PERCENT") {
            if (!Number.isFinite(value) || value <= 0 || value >= 100) {
                showError(
                    "Phần trăm giảm không hợp lệ",
                    "Phần trăm giảm phải lớn hơn 0% và nhỏ hơn 100%."
                );
                discountValue.focus();
                return false;
            }

            if (!Number.isFinite(finalPrice) || finalPrice <= 0 || finalPrice >= price) {
                showError(
                    "Giá sau khuyến mãi không hợp lệ",
                    "Giá sau khuyến mãi phải lớn hơn 0 và nhỏ hơn giá bán."
                );
                return false;
            }
        }

        if (type === "NONE") {
            discountValue.value = "";
            discountPrice.value = price;
        }

        if (!Number.isFinite(serviceFee) || serviceFee < 0) {
            showError(
                "Phí dịch vụ không hợp lệ",
                "Phí dịch vụ không được nhỏ hơn 0."
            );
            serviceFeeInput.focus();
            return false;
        }

        return true;
    }

    // Hiển thị ảnh mới được chọn
    function renderImagePreview() {
        if (!imagePreview) return;

        imagePreview.innerHTML = "";

        selectedFiles.forEach((file, index) => {
            const reader = new FileReader();

            reader.onload = function (event) {
                const wrapper = document.createElement("div");

                wrapper.className =
                    "position-relative border rounded-3 overflow-hidden bg-light shadow-sm";
                wrapper.style.width = "90px";
                wrapper.style.height = "90px";

                wrapper.innerHTML = `
                    <img src="${event.target.result}"
                         alt="Ảnh xem trước"
                         class="w-100 h-100 object-fit-cover">
                    <button type="button"
                            class="remove-preview-image btn btn-danger btn-sm position-absolute top-0 end-0 rounded-circle d-flex align-items-center justify-content-center"
                            data-index="${index}"
                            style="width:24px;height:24px;padding:0;transform:translate(25%,-25%);">
                        <i class="bi bi-x"></i>
                    </button>
                `;

                imagePreview.appendChild(wrapper);
            };

            reader.readAsDataURL(file);
        });
    }

    // Thêm ảnh hợp lệ vào danh sách
    function addFiles(files) {
        const validTypes = [
            "image/jpeg",
            "image/png",
            "image/webp"
        ];

        Array.from(files).forEach(file => {
            if (!validTypes.includes(file.type)) {
                showError(
                    "Ảnh không hợp lệ",
                    `${file.name} không phải định dạng JPG, JPEG, PNG hoặc WEBP.`
                );
                return;
            }

            const exists = selectedFiles.some(oldFile =>
                oldFile.name === file.name &&
                oldFile.size === file.size &&
                oldFile.lastModified === file.lastModified
            );

            if (!exists) {
                selectedFiles.push(file);
            }
        });

        renderImagePreview();
        updateFileInput();
    }

    // Đồng bộ ảnh đã chọn với input file
    function updateFileInput() {
        if (!imageInput) return;

        const dataTransfer = new DataTransfer();

        selectedFiles.forEach(file => {
            dataTransfer.items.add(file);
        });

        imageInput.files = dataTransfer.files;
    }

    // Xử lý chọn ảnh, kéo thả ảnh và xóa ảnh mới
    function initImageUpload() {
        imageInput?.addEventListener("change", function () {
            addFiles(this.files);
        });

        if (imageDropZone) {
            ["dragenter", "dragover"].forEach(eventName => {
                imageDropZone.addEventListener(eventName, function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    imageDropZone.classList.add("border-danger");
                });
            });

            ["dragleave", "drop"].forEach(eventName => {
                imageDropZone.addEventListener(eventName, function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    imageDropZone.classList.remove("border-danger");
                });
            });

            imageDropZone.addEventListener("drop", function (event) {
                addFiles(event.dataTransfer.files);
            });
        }

        imagePreview?.addEventListener("click", function (event) {
            const button = event.target.closest(".remove-preview-image");
            if (!button) return;

            const index = Number(button.dataset.index);
            if (!Number.isInteger(index)) return;

            selectedFiles.splice(index, 1);
            renderImagePreview();
            updateFileInput();
        });
    }

    // Xử lý phần giảm giá
    function initDiscount() {
        discountType?.addEventListener("change", function () {
            discountValue.value = "";
            discountPrice.value = "";

            if (discountType.value === "NONE") {
                discountPrice.value = priceInput.value || "";
            }

            updateDiscountUI();
        });

        priceInput?.addEventListener("input", function () {
            if (discountType.value === "NONE") {
                discountPrice.value = priceInput.value;
            } else if (discountType.value === "PERCENT") {
                calculateDiscountPrice();
            }
        });

        discountValue?.addEventListener("input", calculateDiscountPrice);

        updateDiscountUI();
    }

    // Tạo FormData dùng chung cho thêm và sửa món
    function buildFoodFormData() {
        const formData = new FormData(form);

        formData.set("discountType", discountType.value);

        if (discountType.value === "NONE") {
            formData.set("discountValue", "");
            formData.set("discountPrice", priceInput.value);
        }

        if (discountType.value === "FIXED_PRICE") {
            formData.set("discountValue", "");
            formData.set("discountPrice", discountPrice.value);
        }

        if (discountType.value === "PERCENT") {
            formData.set("discountValue", discountValue.value);
            formData.set("discountPrice", discountPrice.value);
        }

        return formData;
    }

    // Khóa nút submit trong lúc gửi request
    function setSubmitLoading(loading, text) {
        if (!submitButton) return;

        submitButton.disabled = loading;
        submitButton.innerHTML = loading
            ? `<span class="spinner-border spinner-border-sm me-1"></span>${text}`
            : submitButton.dataset.defaultText;
    }

    // Xử lý lỗi trả về từ API
    async function handleResponseError(response, defaultMessage, title) {
        if (response.status === 401 || response.status === 403) {
            showError(
                "Phiên đăng nhập",
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
            );
            window.location.href = "/login";
            return true;
        }

        if (response.ok) return false;

        let message = defaultMessage;

        try {
            const errorData = await response.json();

            if (errorData.message) {
                message = errorData.message;
            }
        } catch (error) {
            console.error("Không thể đọc thông tin lỗi:", error);
        }

        showError(title, message);
        return true;
    }

    // Khởi tạo các chức năng dùng chung cho cả hai trang
    initMultiSelect("categorySelector");
    initMultiSelect("tagSelector");
    initMultiSelect("couponSelector");
    initImageUpload();
    initDiscount();
    document.addEventListener("click", closeMultiSelects);

    // Xử lý riêng cho trang thêm món
    function initCreate() {
        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            if (!validateForm()) return;

            if (selectedFiles.length < 2) {
                showError(
                    "Thiếu hình ảnh",
                    "Vui lòng chọn ít nhất 2 hình ảnh cho món ăn."
                );
                return;
            }

            submitButton.dataset.defaultText =
                '<i class="bi bi-plus-lg me-1"></i>Thêm món ăn';

            setSubmitLoading(true, "Đang thêm...");

            try {
                const response = await fetch("/api/merchant/foods", {
                    method: "POST",
                    credentials: "include",
                    headers: buildAuthHeaders(),
                    body: buildFoodFormData()
                });

                const hasError = await handleResponseError(
                    response,
                    "Không thể thêm món ăn.",
                    "Không thể thêm món ăn"
                );

                if (hasError) return;

                showSuccess(
                    "Thêm món ăn thành công",
                    "Món ăn đã được thêm vào danh sách."
                );

                setTimeout(() => {
                    window.location.href = "/merchant/foods";
                }, 900);
            } catch (error) {
                console.error("Lỗi thêm món ăn:", error);

                showError(
                    "Có lỗi xảy ra",
                    "Không thể thêm món ăn. Vui lòng thử lại."
                );
            } finally {
                setSubmitLoading(false);
            }
        });
    }

    // Xử lý riêng cho trang sửa món
    function initEdit() {
        // Nạp lại dữ liệu giảm giá hiện tại
        if (window.foodEditData) {
            if (discountType.value === "FIXED_PRICE") {
                discountPrice.value = window.foodEditData.discountPrice ?? "";
            }

            if (discountType.value === "PERCENT") {
                discountValue.value = window.foodEditData.discountValue ?? "";
                calculateDiscountPrice();
            }
        }

        // Xử lý xóa ảnh cũ
        document.querySelectorAll(".remove-image-btn").forEach(button => {
            button.addEventListener("click", function () {
                const imageUrl = this.dataset.imageUrl;
                if (!imageUrl) return;

                deletedImages.add(imageUrl);
                this.closest(".existing-image")?.remove();

                const existingImages = document.querySelectorAll(
                    "#existingImages .existing-image"
                );

                if (existingImages.length === 0) {
                    const container = document.getElementById("existingImages");

                    if (container && !document.getElementById("noExistingImages")) {
                        const message = document.createElement("div");

                        message.id = "noExistingImages";
                        message.className =
                            "text-muted small p-3 bg-light w-100 text-center rounded-3";
                        message.textContent = "Chưa có hình ảnh nào cho món này.";

                        container.appendChild(message);
                    }
                }
            });
        });

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            if (!validateForm()) return;

            submitButton.dataset.defaultText =
                '<i class="bi bi-check-lg me-1"></i>Lưu thay đổi';

            setSubmitLoading(true, "Đang lưu...");

            try {
                const formData = buildFoodFormData();

                // Xóa ảnh cũ khỏi FormData rồi thêm lại ảnh mới nếu có
                formData.delete("images");

                if (imageInput?.files) {
                    Array.from(imageInput.files).forEach(file => {
                        formData.append("images", file);
                    });
                }

                // Gửi danh sách ảnh cũ đã bị xóa
                deletedImages.forEach(imageUrl => {
                    formData.append("deletedImages", imageUrl);
                });

                const response = await fetch(`/api/merchant/foods/${foodId}`, {
                    method: "PUT",
                    credentials: "include",
                    headers: buildAuthHeaders(),
                    body: formData
                });

                const hasError = await handleResponseError(
                    response,
                    "Không thể lưu thay đổi.",
                    "Không thể lưu thay đổi"
                );

                if (hasError) return;

                showSuccess(
                    "Cập nhật món ăn thành công",
                    "Thông tin món ăn đã được cập nhật."
                );

                setTimeout(() => {
                    window.location.href = `/merchant/foods/${foodId}`;
                }, 900);
            } catch (error) {
                console.error("Lỗi cập nhật món ăn:", error);

                showError(
                    "Có lỗi xảy ra",
                    "Không thể cập nhật món ăn. Vui lòng thử lại."
                );
            } finally {
                setSubmitLoading(false);
            }
        });
    }

    // Chạy đúng phần xử lý theo trang hiện tại
    if (mode === "create") {
        initCreate();
    } else {
        initEdit();
    }
});