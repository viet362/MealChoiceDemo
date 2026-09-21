document.addEventListener("DOMContentLoaded", () => {

    const form = document.querySelector("[data-coupon-form]");
    const discountType = document.getElementById("discountType");
    const discountValue = document.getElementById("discountValue");
    const submitButton = document.getElementById("submitButton");

    // Cập nhật ô giá trị theo loại giảm
    if (form && discountType && discountValue) {

        function updateDiscountUI() {
            const isPercent = discountType.value === "PERCENT";

            discountValue.max = isPercent ? "100" : "";
            discountValue.step = isPercent ? "0.01" : "1";
            discountValue.placeholder = isPercent
                ? "Ví dụ: 20"
                : "Ví dụ: 20000";
        }

        discountType.addEventListener("change", updateDiscountUI);
        updateDiscountUI();

        // Kiểm tra trước khi gửi form
        form.addEventListener("submit", function (event) {

            if (!form.checkValidity()) {
                event.preventDefault();
                form.classList.add("was-validated");

                window.showMerchantNotice?.({
                    variant: "danger",
                    title: "Thông tin không hợp lệ",
                    message: "Vui lòng kiểm tra lại thông tin coupon."
                });

                return;
            }

            const type = discountType.value;
            const value = Number(discountValue.value);

            if (!Number.isFinite(value) || value <= 0) {
                event.preventDefault();

                window.showMerchantNotice?.({
                    variant: "danger",
                    title: "Giá trị giảm không hợp lệ",
                    message: "Giá trị giảm phải lớn hơn 0."
                });

                discountValue.focus();
                return;
            }

            if (type === "PERCENT" && value > 100) {
                event.preventDefault();

                window.showMerchantNotice?.({
                    variant: "danger",
                    title: "Phần trăm không hợp lệ",
                    message: "Giảm giá theo phần trăm không được vượt quá 100%."
                });

                discountValue.focus();
                return;
            }

            submitButton?.setAttribute("disabled", "disabled");

            if (submitButton) {
                submitButton.innerHTML =
                    '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
            }
        });
    }

    // Xác nhận trước khi xóa
    document.querySelectorAll("[data-delete-coupon]").forEach(form => {

        form.addEventListener("submit", function (event) {
            event.preventDefault();

            const couponCode = this.dataset.couponCode || "này";

            if (!confirm(`Bạn có chắc muốn xóa coupon "${couponCode}" không?`)) {
                return;
            }

            const button = this.querySelector("button");

            if (button) {
                button.disabled = true;
                button.innerHTML =
                    '<span class="spinner-border spinner-border-sm"></span>';
            }

            this.submit();
        });

    });

});