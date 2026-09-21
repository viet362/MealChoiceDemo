document.addEventListener('DOMContentLoaded', () => {

    // Xử lý khi token hết hạn
    function handleAuthError() {
        window.showMerchantNotice?.({
            variant: 'danger',
            title: 'Phiên đăng nhập',
            message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
        });

        window.location.href = '/login';
    }

    window.handleAuthError = handleAuthError;

    // Tìm kiếm và làm mới danh sách món ăn
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const refreshBtn = document.getElementById('refreshBtn');

    if (searchInput && searchBtn) {

        // Gọi API tìm kiếm món ăn
        async function searchFoods() {
            const keyword = searchInput.value.trim();
            const token = localStorage.getItem('accessToken');

            try {
                const params = new URLSearchParams({
                    name: keyword,
                    page: '0',
                    size: '10'
                });

                const response = await fetch(
                    `/api/merchant/foods/search?${params.toString()}`,
                    {
                        method: 'GET',
                        credentials: 'include',
                        headers: token
                            ? {
                                'Authorization': `Bearer ${token}`
                            }
                            : {}
                    }
                );

                if (response.status === 401 || response.status === 403) {
                    handleAuthError();
                    return;
                }

                if (!response.ok) {
                    throw new Error('Không thể tìm kiếm món ăn');
                }

                const data = await response.json();

                console.log('Kết quả tìm kiếm:', data);

                renderSearchResult(data.content || []);

            } catch (error) {
                console.error('Search food error:', error);

                window.showMerchantNotice?.({
                    variant: 'danger',
                    title: 'Tìm kiếm thất bại',
                    message: 'Không thể tìm kiếm món ăn.'
                });
            }
        }

        // Hiển thị kết quả tìm kiếm
        function renderSearchResult(foods) {
            const tbody = document.querySelector('table tbody');

            if (!tbody) {
                return;
            }

            const rows = tbody.querySelectorAll('.food-row');
            const noResultRow = document.getElementById('noSearchResultRow');

            rows.forEach(row => {
                row.style.display = 'none';
            });

            if (!foods || foods.length === 0) {
                if (noResultRow) {
                    noResultRow.classList.remove('d-none');
                }
                return;
            }

            if (noResultRow) {
                noResultRow.classList.add('d-none');
            }

            foods.forEach(food => {
                const row = document.getElementById(`food-row-${food.id}`);

                if (row) {
                    row.style.display = '';
                }
            });
        }

        // Tìm kiếm khi bấm nút kính lúp
        searchBtn.addEventListener('click', searchFoods);

        // Tìm kiếm khi nhấn Enter
        searchInput.addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                searchFoods();
            }
        });
    }

    // Làm mới danh sách sau khi tìm kiếm
    refreshBtn?.addEventListener('click', () => {
        if (searchInput) {
            searchInput.value = '';
        }

        document.querySelectorAll('.food-row').forEach(row => {
            row.style.display = '';
        });

        document.getElementById('noSearchResultRow')?.classList.add('d-none');
    });

    // Xử lý đề cử món ăn
    document.querySelectorAll('.recommend-food-btn').forEach(button => {

        button.addEventListener('click', async function () {
            const foodId = this.dataset.foodId;
            const isRecommended = this.dataset.recommended === 'true';
            const token = localStorage.getItem('accessToken');

            this.disabled = true;

            try {
                const response = await fetch(
                    `/api/merchant/foods/${foodId}/recommend`,
                    {
                        method: 'PATCH',
                        credentials: 'include',
                        headers: token
                            ? {
                                'Authorization': `Bearer ${token}`
                            }
                            : {}
                    }
                );

                if (response.status === 401 || response.status === 403) {
                    handleAuthError();
                    return;
                }

                if (!response.ok) {
                    let message = 'Không thể cập nhật trạng thái đề cử.';

                    try {
                        const errorData = await response.json();

                        if (errorData.message) {
                            message = errorData.message;
                        }
                    } catch (error) {
                        console.error(error);
                    }

                    window.showMerchantNotice?.({
                        variant: 'danger',
                        title: 'Cập nhật thất bại',
                        message: message
                    });

                    return;
                }

                const nextRecommended = !isRecommended;

                this.dataset.recommended = nextRecommended.toString();

                if (nextRecommended) {
                    this.classList.remove('btn-outline-secondary');
                    this.classList.add('btn-warning');

                    this.innerHTML =
                        '<i class="bi bi-star-fill me-1"></i>Đã đề cử';

                    window.showMerchantNotice?.({
                        variant: 'success',
                        title: 'Đề cử món ăn thành công',
                        message: 'Món ăn đã được đánh dấu là món đề cử.'
                    });
                } else {
                    this.classList.remove('btn-warning');
                    this.classList.add('btn-outline-secondary');

                    this.innerHTML =
                        '<i class="bi bi-star me-1"></i>Đề cử';

                    window.showMerchantNotice?.({
                        variant: 'success',
                        title: 'Đã bỏ đề cử',
                        message: 'Món ăn đã được bỏ khỏi danh sách đề cử.'
                    });
                }

            } catch (error) {
                console.error('Recommend food error:', error);

                window.showMerchantNotice?.({
                    variant: 'danger',
                    title: 'Có lỗi xảy ra',
                    message: 'Không thể kết nối đến máy chủ.'
                });

            } finally {
                this.disabled = false;
            }
        });
    });

    // Lưu món ăn đang được chọn để xóa
    let foodIdToDelete = null;
    let buttonElementToDelete = null;

    const deleteModalEl =
        document.getElementById('deleteFoodModal');

    const deleteModal =
        deleteModalEl && typeof bootstrap !== 'undefined'
            ? new bootstrap.Modal(deleteModalEl)
            : null;

    const deleteFoodNameEl =
        document.getElementById('deleteFoodName');

    const confirmDeleteFoodBtn =
        document.getElementById('confirmDeleteFoodBtn');

    // Mở modal xác nhận xóa
    document.querySelectorAll('.delete-food-btn').forEach(button => {

        button.addEventListener('click', function () {

            foodIdToDelete = this.dataset.foodId;
            buttonElementToDelete = this;

            const foodName =
                this.dataset.foodName || 'món ăn này';

            if (deleteFoodNameEl) {
                deleteFoodNameEl.textContent = `"${foodName}"`;
            }

            if (deleteModal) {
                deleteModal.show();
            } else {
                if (confirm(
                    `Bạn có chắc chắn muốn xóa món "${foodName}" không?`
                )) {
                    executeDeleteFood(
                        foodIdToDelete,
                        buttonElementToDelete
                    );
                }
            }
        });
    });

    // Xác nhận xóa món ăn
    confirmDeleteFoodBtn?.addEventListener(
        'click',
        async function () {

            if (!foodIdToDelete) {
                return;
            }

            const originalBtnHtml =
                confirmDeleteFoodBtn.innerHTML;

            confirmDeleteFoodBtn.disabled = true;

            confirmDeleteFoodBtn.innerHTML =
                '<span class="spinner-border spinner-border-sm me-1"></span> Đang xóa...';

            await executeDeleteFood(
                foodIdToDelete,
                buttonElementToDelete
            );

            confirmDeleteFoodBtn.disabled = false;
            confirmDeleteFoodBtn.innerHTML = originalBtnHtml;

            if (deleteModal) {
                deleteModal.hide();
            }
        }
    );

    // Gọi API xóa món ăn
    async function executeDeleteFood(foodId, buttonEl) {
        const token = localStorage.getItem('accessToken');

        if (buttonEl) {
            buttonEl.disabled = true;
        }

        try {
            const response = await fetch(
                `/api/merchant/foods/${foodId}`,
                {
                    method: 'DELETE',
                    credentials: 'include',
                    headers: token
                        ? {
                            'Authorization': `Bearer ${token}`
                        }
                        : {}
                }
            );

            if (response.status === 401 || response.status === 403) {
                handleAuthError();
                return;
            }

            if (!response.ok) {
                let message = 'Không thể xóa món ăn.';

                try {
                    const errorData = await response.json();

                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch (error) {
                    console.error(error);
                }

                window.showMerchantNotice?.({
                    variant: 'danger',
                    title: 'Xóa thất bại',
                    message: message
                });

                return;
            }

            const targetRow =
                document.getElementById(`food-row-${foodId}`);

            if (targetRow) {
                targetRow.style.transition = 'all 0.3s ease';
                targetRow.style.opacity = '0';
                targetRow.style.transform = 'translateX(20px)';

                setTimeout(() => {
                    targetRow.remove();

                    const remainingRows =
                        document.querySelectorAll('.food-row');

                    if (remainingRows.length === 0) {
                        window.location.reload();
                    }
                }, 300);
            }

            window.showMerchantNotice?.({
                variant: 'success',
                title: 'Xóa món ăn thành công',
                message: 'Món ăn đã được xóa khỏi thực đơn của quán.'
            });

        } catch (error) {
            console.error('Delete food error:', error);

            window.showMerchantNotice?.({
                variant: 'danger',
                title: 'Có lỗi xảy ra',
                message: 'Không thể xóa món ăn do lỗi kết nối.'
            });

        } finally {
            if (buttonEl) {
                buttonEl.disabled = false;
            }
        }
    }
});