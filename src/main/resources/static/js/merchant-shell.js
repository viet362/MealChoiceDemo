(function () {
  const body = document.body;
  const sidebar = document.querySelector('.merchant-sidebar');
  const backdrop = document.querySelector('[data-merchant-sidebar-backdrop]');
  const toggleButtons = Array.from(
      document.querySelectorAll('[data-merchant-sidebar-toggle]')
  );
  const noticeRoot = document.querySelector('[data-merchant-notice]');
  const noticeBackdrop = document.querySelector('[data-merchant-notice-backdrop]');
  const noticeTitle = document.querySelector('[data-merchant-notice-title]');
  const noticeMessage = document.querySelector('[data-merchant-notice-message]');
  const noticeIcon = document.querySelector('[data-merchant-notice-icon]');
  const noticeClose = document.querySelector('[data-merchant-notice-close]');


  const storageKey = 'merchantSidebarCollapsed';
  const desktopQuery = window.matchMedia('(min-width: 992px)');

  function applyDesktopState(collapsed) {
    body.classList.toggle('merchant-sidebar-collapsed', collapsed);
  }

  function openMobileSidebar() {
    body.classList.add('merchant-sidebar-open');
  }

  function closeMobileSidebar() {
    body.classList.remove('merchant-sidebar-open');
  }

  function hideNotice() {
    if (!noticeRoot || !noticeBackdrop) {
      return;
    }

    noticeRoot.hidden = true;
    noticeBackdrop.hidden = true;
    noticeRoot.classList.remove('is-visible');
    noticeBackdrop.classList.remove('is-visible');
    noticeRoot.setAttribute('aria-hidden', 'true');
  }

  function showNotice(options = {}) {
    if (!noticeRoot || !noticeBackdrop || !noticeTitle || !noticeMessage || !noticeIcon) {
      return;
    }

    const variant = options.variant || 'info';
    const title = options.title || 'Thông báo';
    const message = options.message || '';

    noticeTitle.textContent = title;
    noticeMessage.textContent = message;
    noticeRoot.dataset.variant = variant;

    noticeIcon.innerHTML =
        variant === 'success'
            ? '<i class="bi bi-check-circle-fill"></i>'
            : variant === 'danger'
                ? '<i class="bi bi-exclamation-triangle-fill"></i>'
                : '<i class="bi bi-info-circle-fill"></i>';

    noticeRoot.hidden = false;
    noticeBackdrop.hidden = false;
    noticeRoot.classList.add('is-visible');
    noticeBackdrop.classList.add('is-visible');
    noticeRoot.setAttribute('aria-hidden', 'false');
  }

  function syncMode() {
    if (desktopQuery.matches) {
      closeMobileSidebar();
      applyDesktopState(localStorage.getItem(storageKey) === '1');
    } else {
      body.classList.remove('merchant-sidebar-collapsed');
    }
  }

  toggleButtons.forEach((button) => {
    button.addEventListener('click', () => {
      if (desktopQuery.matches) {
        const nextCollapsed = !body.classList.contains('merchant-sidebar-collapsed');
        applyDesktopState(nextCollapsed);
        localStorage.setItem(storageKey, nextCollapsed ? '1' : '0');
      } else if (body.classList.contains('merchant-sidebar-open')) {
        closeMobileSidebar();
      } else {
        openMobileSidebar();
      }
    });
  });

  backdrop?.addEventListener('click', closeMobileSidebar);
  noticeBackdrop?.addEventListener('click', hideNotice);
  noticeClose?.addEventListener('click', hideNotice);

  desktopQuery.addEventListener('change', syncMode);
  syncMode();

  window.MerchantUI = {
    showNotice,
    hideNotice
  };
  window.showMerchantNotice = showNotice;
  window.hideMerchantNotice = hideNotice;
})();
function showAlert(type, message) {
  const container = document.getElementById("alertContainer");

  if (!container) {
    return;
  }

  container.innerHTML = `
        <div class="alert alert-${type === "success" ? "success" : "danger"} alert-dismissible fade show rounded-3 shadow-sm">
            ${message}
            <button type="button"
                    class="btn-close"
                    data-bs-dismiss="alert">
            </button>
        </div>
    `;
}

window.showAlert = showAlert;
