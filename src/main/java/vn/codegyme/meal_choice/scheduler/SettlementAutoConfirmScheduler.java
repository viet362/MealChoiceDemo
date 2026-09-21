package vn.codegyme.meal_choice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.codegyme.meal_choice.service.MerchantSettlementService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementAutoConfirmScheduler {

    private final MerchantSettlementService settlementService;

    /**
     * Tự động xác nhận các kỳ đối soát PENDING_CONFIRMATION đã kết thúc kỳ quá 5 ngày
     * Chạy định kỳ vào 02:00 sáng mỗi ngày
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runAutoConfirmSettlements() {
        log.info("Bắt đầu tiến trình kiểm tra và tự động xác nhận kỳ đối soát (Auto-confirm)...");
        try {
            int autoConfirmedCount = settlementService.autoConfirmPendingSettlements(5);
            log.info("Tiến trình Auto-confirm hoàn tất. Tổng số kỳ đối soát đã tự động chốt: {}", autoConfirmedCount);
        } catch (Exception e) {
            log.error("Lỗi trong tiến trình Auto-confirm kỳ đối soát: {}", e.getMessage(), e);
        }
    }
}
