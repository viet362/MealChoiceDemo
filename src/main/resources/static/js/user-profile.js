// API Base URL
const API_BASE = '/api/user';
const PROVINCES_API = "https://provinces.open-api.vn/api/";

// Global state
let currentUser = null;
let isEditing = false;
let initialProfileData = {};
let initialAddressData = {};
let cachedProvinces = null;

// Tải danh sách tỉnh thành (có cache)
async function getProvinces() {
    if (cachedProvinces && cachedProvinces.length > 0) {
        return cachedProvinces;
    }
    try {
        const res = await fetch(`${PROVINCES_API}?depth=1`);
        if (res.ok) {
            cachedProvinces = await res.json();
            return cachedProvinces;
        }
    } catch (e) {
        console.warn("Lỗi tải tỉnh thành từ API:", e);
    }
    return [];
}

// Tải quận huyện theo mã tỉnh
async function getDistrictsByProvinceCode(provCode) {
    if (!provCode) return [];
    try {
        const res = await fetch(`${PROVINCES_API}p/${provCode}?depth=2`);
        if (res.ok) {
            const data = await res.json();
            return data.districts || [];
        }
    } catch (e) {
        console.warn("Lỗi tải quận huyện từ API:", e);
    }
    return [];
}

// Tải phường xã theo mã quận
async function getWardsByDistrictCode(distCode) {
    if (!distCode) return [];
    try {
        const res = await fetch(`${PROVINCES_API}d/${distCode}?depth=2`);
        if (res.ok) {
            const data = await res.json();
            return data.wards || [];
        }
    } catch (e) {
        console.warn("Lỗi tải phường xã từ API:", e);
    }
    return [];
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    checkAuthentication();
    setupEventListeners();
    setupAddressSelectListeners();
    getProvinces(); // Tiền nạp danh sách tỉnh trong nền
});

// Helper to get Auth Headers with JWT Bearer Token
function getAuthHeaders(extraHeaders = {}) {
    const token = localStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        ...extraHeaders
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

// Check if user is logged in
function checkAuthentication() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        window.location.href = '/login';
        return;
    }
    loadProfile();
}

async function silentRefresh() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return;

    try {
        const response = await fetch('/api/auth/refresh-token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            if (data.accessToken) {
                localStorage.setItem('accessToken', data.accessToken);
                document.cookie = `accessToken=${data.accessToken}; path=/; max-age=86400; SameSite=Lax`;
            }
        } else if (response.status === 401 || response.status === 403) {
            // Refresh token hết hạn → logout
            console.warn('Refresh token hết hạn, đăng xuất...');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
        }
    } catch (e) {
        console.warn('Silent refresh tạm thời thất bại:', e);
    }
}

// Logout function
async function logout() {
    if (!confirm('Bạn có chắc muốn đăng xuất?')) {
        return;
    }

    try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ refreshToken })
            });
        }
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
    }
}

// Setup Event Listeners
function setupEventListeners() {
    const profileForm = document.getElementById('profileForm');
    if (profileForm) {
        profileForm.addEventListener('submit', handleProfileSubmit);
    }
    const addressForm = document.getElementById('addressForm');
    if (addressForm) {
        addressForm.addEventListener('submit', (e) => {
            e.preventDefault();
            submitAddressForm();
        });
    }
}

// ==================== PROFILE FUNCTIONS ====================

// Load user profile
async function loadProfile() {
    showLoading(true);
    try {
        const fetchFn = (typeof window.fetchWithAuth === 'function') ? window.fetchWithAuth : fetch;
        const response = await fetchFn(`${API_BASE}/profile`, {
            headers: getAuthHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return;
        }

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayProfile(currentUser);
            displayAddresses(currentUser.addresses);
        } else {
            showAlert('error', result.message || 'Không thể tải thông tin profile');
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        showAlert('error', 'Lỗi kết nối server');
    } finally {
        showLoading(false);
    }
}

// Display profile data
function displayProfile(user) {
    const displayNameEl = document.getElementById('displayName');
    if (displayNameEl) displayNameEl.value = user.displayName || user.name || '';

    const emailEl = document.getElementById('email');
    if (emailEl) emailEl.value = user.email || '';

    const phoneEl = document.getElementById('phoneNumber');
    if (phoneEl) phoneEl.value = user.phoneNumber || '';

    const dobEl = document.getElementById('dob');
    if (dobEl) {
        let dobValue = user.dob || user.dateOfBirth || '';

        if (dobValue && dobValue.includes('T')) {
            dobValue = dobValue.split('T')[0];
        }

        dobEl.value = dobValue;
    }

    // Gender
    document.getElementById('genderMale').checked = user.gender === 'MALE';
    document.getElementById('genderFemale').checked = user.gender === 'FEMALE';
    document.getElementById('genderOther').checked = user.gender === 'OTHER';
}

// Enable profile editing
function editProfile() {
    isEditing = true;
    const displayNameEl = document.getElementById('displayName');
    if (displayNameEl) displayNameEl.disabled = false;
    const dobEl = document.getElementById('dob');
    if (dobEl) dobEl.disabled = false;
    document.querySelectorAll('input[name="gender"]').forEach(input => {
    input.disabled = false;
   });
    document.getElementById('profileActions').style.display = 'block';

    let dobValue = currentUser.dob || currentUser.dateOfBirth || '';
    if (dobValue && dobValue.includes('T')) {
        dobValue = dobValue.split('T')[0];
    }

    // Lưu dữ liệu ban đầu để so sánh dirty fields
    initialProfileData = {
        displayName: displayNameEl ? displayNameEl.value : '',
        dob: dobValue,
        gender: document.querySelector('input[name="gender"]:checked')?.value || ''
    };
}

// Cancel profile editing
function cancelEdit() {
    isEditing = false;
    const displayNameEl = document.getElementById('displayName');
    if (displayNameEl) displayNameEl.disabled = true;
    const dobEl = document.getElementById('dob');
    if (dobEl) dobEl.disabled = true;
    document.querySelectorAll('input[name="gender"]').forEach(input => {
    input.disabled = true;
    });
    document.getElementById('profileActions').style.display = 'none';
    displayProfile(currentUser); // Reset to original data
}

// Handle profile form submit (Partial Update via PATCH)
async function handleProfileSubmit(e) {
    e.preventDefault();

    const currentDisplayName = document.getElementById('displayName').value;
    const currentDob = document.getElementById('dob').value;
    const currentGender = document.querySelector('input[name="gender"]:checked')?.value || '';

    // Chỉ đưa vào payload các trường có sự thay đổi (dirty fields)
    const payload = {};
    if (currentDisplayName !== initialProfileData.displayName) {
        payload.displayName = currentDisplayName;
    }
    if (currentDob !== initialProfileData.dob) {
    payload.dob = currentDob || null;
}
    if (currentGender !== initialProfileData.gender) {
        payload.gender = currentGender || null;
    }

    if (Object.keys(payload).length === 0) {
        showAlert('info', 'Không có thông tin nào thay đổi');
        cancelEdit();
        return;
    }

    showLoading(true);
    try {
        const fetchFn = (typeof window.fetchWithAuth === 'function') ? window.fetchWithAuth : fetch;
        const response = await fetchFn(`${API_BASE}/profile`, {
            method: 'PATCH',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayProfile(currentUser);
            cancelEdit();
            showAlert('success', 'Cập nhật thông tin cá nhân thành công!');
        } else {
            showAlert('error', result.message || 'Cập nhật thất bại');
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        showAlert('error', 'Lỗi kết nối server');
    } finally {
        showLoading(false);
    }
}

// ==================== ADDRESS FUNCTIONS ====================

// Display addresses
function displayAddresses(addresses) {
    const container = document.getElementById('addressList');
    if (!container) return;

    if (!addresses || addresses.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5 text-muted">
                <div class="display-6 mb-2">📭</div>
                <h6 class="fw-bold mb-1">Chưa có địa chỉ giao hàng nào</h6>
                <p class="small text-secondary mb-0">Hãy thêm địa chỉ để đặt hàng dễ dàng hơn!</p>
            </div>
        `;
        return;
    }

    container.innerHTML = addresses.map(addr => `
        <div class="col-md-6 col-12">
            <div class="card h-100 p-3 rounded-4 border ${addr.isDefault ? 'border-danger border-2 bg-light-subtle shadow-sm' : 'border-light-subtle shadow-sm'}">
                <div class="card-body p-2 d-flex flex-column justify-content-between">
                    <div>
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="fw-bold text-dark mb-0">
                                <i class="bi bi-person-fill text-danger me-1"></i>${addr.contactName}
                            </h6>
                            ${addr.isDefault ? '<span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1"><i class="bi bi-check-circle-fill me-1"></i>Mặc định</span>' : ''}
                        </div>
                        <p class="text-secondary small mb-2">
                            <i class="bi bi-telephone-fill me-1"></i><strong>${addr.contactPhone}</strong>
                        </p>
                        <p class="text-dark small mb-1">
                            <i class="bi bi-geo-alt-fill text-danger me-1"></i><strong>Địa chỉ:</strong> ${addr.street}
                        </p>
                        <p class="text-muted small mb-2 ms-4">
                            ${addr.ward}, ${addr.district}, ${addr.city}
                        </p>
                        ${addr.note ? `<p class="text-secondary small mb-2 bg-white p-2 rounded-3 border"><i class="bi bi-sticky me-1 text-warning"></i><strong>Ghi chú:</strong> ${addr.note}</p>` : ''}
                    </div>

                    <div class="d-flex justify-content-end align-items-center gap-2 pt-3 border-top mt-2">
                        ${!addr.isDefault ? `
                            <button class="btn btn-sm btn-outline-success rounded-pill px-3" onclick="setDefaultAddress(${addr.id})">
                                <i class="bi bi-star me-1"></i> Mặc định
                            </button>
                        ` : ''}
                        <button class="btn btn-sm btn-outline-primary rounded-pill px-3" onclick="editAddress(${addr.id})">
                            <i class="bi bi-pencil me-1"></i> Sửa
                        </button>
                        <button class="btn btn-sm btn-outline-danger rounded-pill px-3" onclick="deleteAddress(${addr.id})">
                            <i class="bi bi-trash me-1"></i> Xóa
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

// Gắn listener thay đổi dropdown địa chỉ
function setupAddressSelectListeners() {
    const citySelect = document.getElementById('city');
    const districtSelect = document.getElementById('district');
    const wardSelect = document.getElementById('ward');

    if (citySelect) {
        citySelect.addEventListener('change', async function() {
            const selectedOpt = citySelect.options[citySelect.selectedIndex];
            const provCode = selectedOpt ? selectedOpt.getAttribute('data-code') : null;

            districtSelect.innerHTML = '<option value="" selected disabled>Đang tải Quận/Huyện...</option>';
            wardSelect.innerHTML = '<option value="" selected disabled>Phường/Xã</option>';
            wardSelect.disabled = true;

            if (!provCode) {
                districtSelect.innerHTML = '<option value="" selected disabled>Quận/Huyện</option>';
                districtSelect.disabled = true;
                return;
            }

            const districts = await getDistrictsByProvinceCode(provCode);
            let distHtml = '<option value="" selected disabled>Quận/Huyện</option>';
            districts.forEach(d => {
                distHtml += `<option value="${d.name}" data-code="${d.code}">${d.name}</option>`;
            });
            districtSelect.innerHTML = distHtml;
            districtSelect.disabled = false;
        });
    }

    if (districtSelect) {
        districtSelect.addEventListener('change', async function() {
            const selectedOpt = districtSelect.options[districtSelect.selectedIndex];
            const distCode = selectedOpt ? selectedOpt.getAttribute('data-code') : null;

            wardSelect.innerHTML = '<option value="" selected disabled>Đang tải Phường/Xã...</option>';

            if (!distCode) {
                wardSelect.innerHTML = '<option value="" selected disabled>Phường/Xã</option>';
                wardSelect.disabled = true;
                return;
            }

            const wards = await getWardsByDistrictCode(distCode);
            let wardHtml = '<option value="" selected disabled>Phường/Xã</option>';
            wards.forEach(w => {
                wardHtml += `<option value="${w.name}" data-code="${w.code}">${w.name}</option>`;
            });
            wardSelect.innerHTML = wardHtml;
            wardSelect.disabled = false;
        });
    }
}

// Open address modal (Tỉnh/Huyện/Xã)
async function openAddressModal(addressId = null) {
    const modalEl = document.getElementById('addressModal');
    if (!modalEl) return;
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    const form = document.getElementById('addressForm');

    form.reset();
    document.getElementById('addressId').value = '';

    const contactNameInput = document.getElementById('contactName');
    const contactPhoneInput = document.getElementById('contactPhone');
    const contactNotice = document.getElementById('contactNotice');

    const citySelect = document.getElementById('city');
    const districtSelect = document.getElementById('district');
    const wardSelect = document.getElementById('ward');

    if (addressId) {
        const selectedAddressId = Number(addressId);
        const address = currentUser?.addresses?.find(a => Number(a.id) === selectedAddressId);

        if (!address) {
            showAlert('error', 'Địa chỉ không hợp lệ hoặc đã bị xóa');
            return;
        }

        document.getElementById('modalTitle').innerHTML = '<i class="bi bi-pencil me-2 text-danger"></i>Chỉnh Sửa Địa Chỉ';
        document.getElementById('addressId').value = address.id;
        contactNameInput.value = address.contactName || '';
        contactPhoneInput.value = address.contactPhone || '';

        contactNameInput.disabled = false;
        contactPhoneInput.disabled = true;
        if (contactNotice) contactNotice.classList.remove('d-none');

        document.getElementById('street').value = address.street || '';
        document.getElementById('note').value = address.note || '';
        document.getElementById('isDefault').checked = Boolean(address.isDefault);

        const oldCity = (address.city || '').trim();
        const oldDistrict = (address.district || '').trim();
        const oldWard = (address.ward || '').trim();

        // 1. NGAY LẬP TỨC điền giá trị cũ vào 3 dropdown để người dùng nhìn thấy ngay
        citySelect.innerHTML = `<option value="${oldCity}" selected>${oldCity || 'Tỉnh/Thành phố'}</option>`;
        districtSelect.innerHTML = `<option value="${oldDistrict}" selected>${oldDistrict || 'Quận/Huyện'}</option>`;
        districtSelect.disabled = false;
        wardSelect.innerHTML = `<option value="${oldWard}" selected>${oldWard || 'Phường/Xã'}</option>`;
        wardSelect.disabled = false;

        modal.show();

        // 2. Nạp toàn bộ danh mục từ API và chọn đúng mục đang có
        try {
            const provinces = await getProvinces();
            if (provinces && provinces.length > 0) {
                let cityHtml = '<option value="" disabled>Tỉnh/Thành phố</option>';
                let matchedProvCode = null;
                let isCityMatched = false;

                const targetCity = oldCity.toLowerCase();
                provinces.forEach(p => {
                    const pName = p.name.trim();
                    const pLower = pName.toLowerCase();
                    const isSelected = targetCity && (pLower === targetCity || pLower.includes(targetCity) || targetCity.includes(pLower));
                    if (isSelected && !isCityMatched) {
                        cityHtml += `<option value="${pName}" data-code="${p.code}" selected>${pName}</option>`;
                        matchedProvCode = p.code;
                        isCityMatched = true;
                    } else {
                        cityHtml += `<option value="${pName}" data-code="${p.code}">${pName}</option>`;
                    }
                });

                if (!isCityMatched && oldCity) {
                    cityHtml = `<option value="${oldCity}" selected>${oldCity}</option>` + cityHtml;
                }
                citySelect.innerHTML = cityHtml;

                // Nếu khớp được mã Tỉnh -> nạp Quận/Huyện
                if (matchedProvCode) {
                    const districts = await getDistrictsByProvinceCode(matchedProvCode);
                    let distHtml = '<option value="" disabled>Quận/Huyện</option>';
                    let matchedDistCode = null;
                    let isDistMatched = false;

                    const targetDist = oldDistrict.toLowerCase();
                    districts.forEach(d => {
                        const dName = d.name.trim();
                        const dLower = dName.toLowerCase();
                        const isSelected = targetDist && (dLower === targetDist || dLower.includes(targetDist) || targetDist.includes(dLower));
                        if (isSelected && !isDistMatched) {
                            distHtml += `<option value="${dName}" data-code="${d.code}" selected>${dName}</option>`;
                            matchedDistCode = d.code;
                            isDistMatched = true;
                        } else {
                            distHtml += `<option value="${dName}" data-code="${d.code}">${dName}</option>`;
                        }
                    });

                    if (!isDistMatched && oldDistrict) {
                        distHtml = `<option value="${oldDistrict}" selected>${oldDistrict}</option>` + distHtml;
                    }
                    districtSelect.innerHTML = distHtml;
                    districtSelect.disabled = false;

                    // Nếu khớp được mã Quận -> nạp Phường/Xã
                    if (matchedDistCode) {
                        const wards = await getWardsByDistrictCode(matchedDistCode);
                        let wardHtml = '<option value="" disabled>Phường/Xã</option>';
                        let isWardMatched = false;

                        const targetWard = oldWard.toLowerCase();
                        wards.forEach(w => {
                            const wName = w.name.trim();
                            const wLower = wName.toLowerCase();
                            const isSelected = targetWard && (wLower === targetWard || wLower.includes(targetWard) || targetWard.includes(wLower));
                            if (isSelected && !isWardMatched) {
                                wardHtml += `<option value="${wName}" data-code="${w.code}" selected>${wName}</option>`;
                                isWardMatched = true;
                            } else {
                                wardHtml += `<option value="${wName}" data-code="${w.code}">${wName}</option>`;
                            }
                        });

                        if (!isWardMatched && oldWard) {
                            wardHtml = `<option value="${oldWard}" selected>${oldWard}</option>` + wardHtml;
                        }
                        wardSelect.innerHTML = wardHtml;
                        wardSelect.disabled = false;
                    }
                }
            }
        } catch (err) {
            console.warn('Lỗi nạp danh mục hành chính đầy đủ:', err);
        }

        initialAddressData = {
            city: oldCity,
            district: oldDistrict,
            ward: oldWard,
            street: address.street || '',
            note: address.note || '',
            isDefault: Boolean(address.isDefault)
        };
    } else {
        document.getElementById('modalTitle').innerHTML = '<i class="bi bi-house-add me-2 text-danger"></i>Thêm Địa Chỉ Mới';
        contactNameInput.disabled = false;
        contactPhoneInput.disabled = false;
        if (contactNotice) contactNotice.classList.add('d-none');

        citySelect.innerHTML = '<option value="" selected disabled>Đang tải Tỉnh/Thành phố...</option>';
        districtSelect.innerHTML = '<option value="" selected disabled>Quận/Huyện</option>';
        wardSelect.innerHTML = '<option value="" selected disabled>Phường/Xã</option>';
        districtSelect.disabled = true;
        wardSelect.disabled = true;

        initialAddressData = {};
        modal.show();

        const provinces = await getProvinces();
        let cityHtml = '<option value="" selected disabled>Tỉnh/Thành phố</option>';
        provinces.forEach(p => {
            cityHtml += `<option value="${p.name}" data-code="${p.code}">${p.name}</option>`;
        });
        citySelect.innerHTML = cityHtml;
    }
}

// Close address modal
function closeAddressModal() {
    const modalEl = document.getElementById('addressModal');
    if (!modalEl) return;
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    if (modal) {
        modal.hide();
    }
}

// Edit address
function editAddress(addressId) {
    const target = currentUser?.addresses?.find(a => Number(a.id) === Number(addressId));

    if (!target) {
        showAlert('error', 'Không tìm thấy địa chỉ để sửa');
        return;
    }

    openAddressModal(Number(addressId));
}

// lưu địa chỉ
async function submitAddressForm() {
    const addressId = document.getElementById('addressId').value;
    const isEdit = Boolean(addressId);

    const contactName = document.getElementById('contactName').value.trim();
    const contactPhone = document.getElementById('contactPhone').value.trim();
    const city = document.getElementById('city').value;
    const district = document.getElementById('district').value;
    const ward = document.getElementById('ward').value;
    const street = document.getElementById('street').value.trim();
    const note = document.getElementById('note').value.trim() || null;
    const isDefault = document.getElementById('isDefault').checked;

    if (!isEdit && (!contactName || !contactPhone)) {
        showAlert('error', 'Vui lòng nhập tên người nhận và số điện thoại');
        return;
    }

    if (!city || !district || !ward || !street) {
        showAlert('error', 'Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện, Phường/Xã và nhập địa chỉ chi tiết');
        return;
    }

    let url;
    let method;
    let formData;

    if (isEdit) {
        url = `${API_BASE}/address/${addressId}`;
        method = 'PATCH';
        formData = {
            contactName: contactName,
            contactPhone: contactPhone,
            city: city,
            district: district,
            ward: ward,
            street: street,
            note: note,
            isDefault: isDefault
        };
    } else {
        url = `${API_BASE}/address`;
        method = 'POST';
        formData = {
            contactName: contactName,
            contactPhone: contactPhone,
            city: city,
            district: district,
            ward: ward,
            street: street,
            note: note,
            isDefault: isDefault
        };
    }

    showLoading(true);
    try {
        const fetchFn = (typeof window.fetchWithAuth === 'function') ? window.fetchWithAuth : fetch;
        const response = await fetchFn(url, {
            method: method,
            headers: getAuthHeaders(),
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayAddresses(currentUser.addresses);
            closeAddressModal();

            if (isEdit) {
                showAlert('success', 'Sửa địa chỉ thành công!');
            } else {
                showAlert('success', 'Thêm địa chỉ thành công!');
            }
        } else {
            showAlert('error', result.message || 'Lưu địa chỉ thất bại');
        }
    } catch (error) {
        console.error('Error saving address:', error);
        showAlert('error', 'Lỗi kết nối server');
    } finally {
        showLoading(false);
    }
}

// Delete address
async function deleteAddress(addressId) {
    if (!confirm('⚠️ Bạn có chắc muốn xóa địa chỉ này?')) {
        return;
    }

    showLoading(true);
    try {
        const fetchFn = (typeof window.fetchWithAuth === 'function') ? window.fetchWithAuth : fetch;
        const response = await fetchFn(`${API_BASE}/address/${addressId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayAddresses(currentUser.addresses);
            showAlert('success', 'Xóa địa chỉ thành công!');
        } else {
            showAlert('error', result.message || 'Xóa địa chỉ thất bại');
        }
    } catch (error) {
        console.error('Error deleting address:', error);
        showAlert('error', 'Lỗi kết nối server');
    } finally {
        showLoading(false);
    }
}

// Set default address
async function setDefaultAddress(addressId) {
    showLoading(true);
    try {
        const fetchFn = (typeof window.fetchWithAuth === 'function') ? window.fetchWithAuth : fetch;
        const response = await fetchFn(`${API_BASE}/address/${addressId}/set-default`, {
            method: 'PATCH',
            headers: getAuthHeaders()
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result.data;
            displayAddresses(currentUser.addresses);
            showAlert('success', 'Đã đặt địa chỉ mặc định!');
        } else {
            showAlert('error', result.message || 'Thất bại');
        }
    } catch (error) {
        console.error('Error setting default address:', error);
        showAlert('error', 'Lỗi kết nối server');
    } finally {
        showLoading(false);
    }
}

// ==================== UTILITY FUNCTIONS ====================

// Show alert
function showAlert(type, message) {
    const container = document.getElementById('alertContainer');
    if (!container) return;
    let alertClass = 'alert-danger';
    let icon = 'bi-exclamation-triangle-fill';

    if (type === 'success') {
        alertClass = 'alert-success';
        icon = 'bi-check-circle-fill';
    } else if (type === 'info') {
        alertClass = 'alert-info';
        icon = 'bi-info-circle-fill';
    }

    container.innerHTML = `
        <div class="alert ${alertClass} alert-dismissible fade show rounded-3 shadow-sm d-flex align-items-center mb-4" role="alert">
            <i class="bi ${icon} fs-5 me-2"></i>
            <div>${message}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;

    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

// Show/hide loading
function showLoading(show) {
    const loading = document.getElementById('loading');
    if (loading) {
        if (show) {
            loading.classList.remove('d-none');
            loading.classList.add('d-flex');
        } else {
            loading.classList.remove('d-flex');
            loading.classList.add('d-none');
        }
    }
}

// Export functions to global scope
window.submitAddressForm = submitAddressForm;
window.openAddressModal = openAddressModal;
window.editAddress = editAddress;
window.closeAddressModal = closeAddressModal;
window.deleteAddress = deleteAddress;
window.setDefaultAddress = setDefaultAddress;