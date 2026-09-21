package vn.codegyme.meal_choice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.entity.MerchantStatus;
import vn.codegyme.meal_choice.service.AdminService;

import java.util.UUID;

@Controller
@RequestMapping("/admin/merchants")
public class AdminMerchantController {

    private final AdminService adminService;

    public AdminMerchantController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Danh sách merchant
    @GetMapping
    public String showMerchantList(
            @RequestParam(name = "status", required = false) MerchantStatus status,
            Model model) {

        model.addAttribute(
                "merchants",
                status == null
                        ? adminService.getAllMerchants()
                        : adminService.getMerchantsByStatus(status)
        );

        model.addAttribute("selectedStatus", status);

        model.addAttribute(
                "pendingTrustedPartnerIds",
                adminService.getPendingTrustedPartnerMerchantIds()
        );

        return "admin/merchant/list";
    }

    // Xem chi tiết merchant
    @GetMapping("/{id}")
    public String showMerchantDetail(
            @PathVariable(name = "id") UUID id,
            Model model) {

        model.addAttribute("merchant", adminService.getMerchantById(id));

        return "admin/merchant/detail";
    }
    //    merchant block không xem được
    @GetMapping("/merchant-blocked")
    public String merchantBlocked() {
        return "redirect:/home?merchantBlocked=true";
    }

    // Duyệt
    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable(name = "id") UUID id,
            RedirectAttributes redirectAttributes) {

        adminService.approveMerchant(id);
        redirectAttributes.addFlashAttribute("message", "Đã duyệt đăng ký merchant.");

        return "redirect:/admin/merchants";
    }

    // Từ chối
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "reason", required = false) String rejectReason,
            RedirectAttributes redirectAttributes) {

        adminService.rejectMerchant(id, rejectReason);
        redirectAttributes.addFlashAttribute("message", "Đã từ chối đăng ký merchant.");

        return "redirect:/admin/merchants";
    }

    // Khóa /lý do khóa/ mở khóa/
    @PostMapping("/{id}/toggle-lock")
    public String toggleLock(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "lockReason", required = false) String lockReason,
            RedirectAttributes redirectAttributes) {

        try {

            adminService.toggleMerchantLockStatus(
                    id,
                    lockReason
            );

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Đã cập nhật trạng thái merchant."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/merchants";
    }

    // Duyệt đối tác thân thiết
    @PostMapping("/{id}/trusted-partner")
    public String approveTrustedPartner(
            @PathVariable(name = "id") UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.approveTrustedPartner(id);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Đã duyệt đối tác thân thiết."
            );
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/merchants";
    }

    // Bỏ đối tác thân thiết
    @PostMapping("/{id}/trusted-partner/remove")
    public String removeTrustedPartner(
            @PathVariable(name = "id") UUID id,
            RedirectAttributes redirectAttributes) {

        adminService.removeTrustedPartner(id);
        redirectAttributes.addFlashAttribute(
                "message",
                "Đã bỏ trạng thái đối tác thân thiết."
        );

        return "redirect:/admin/merchants";
    }

    @PostMapping("/{id}/trusted-partner/reject")
    public String rejectTrustedPartner(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.rejectTrustedPartner(id, reason);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Đã từ chối đăng ký đối tác thân thiết."
            );
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/merchants";
    }
}