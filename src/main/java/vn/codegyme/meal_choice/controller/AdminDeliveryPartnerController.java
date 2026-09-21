package vn.codegyme.meal_choice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.codegyme.meal_choice.entity.DeliveryPartner;
import vn.codegyme.meal_choice.entity.DeliveryPartnerStatus;
import vn.codegyme.meal_choice.service.AdminService;

import java.util.UUID;

@Controller
@RequestMapping("/admin/delivery-partners")
@RequiredArgsConstructor
public class AdminDeliveryPartnerController {

    private final AdminService adminService;


    // Danh sách
    @GetMapping
    public String list(
            @RequestParam(required = false)
            DeliveryPartnerStatus status,
            Model model
    ) {

        model.addAttribute(
                "partners",
                status == null
                        ? adminService.getAllDeliveryPartners()
                        : adminService.getDeliveryPartnersByStatus(status)
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        return "admin-delivery-partner/list";
    }


    // Form tạo mới
    @GetMapping("/create")
    public String createForm(
            Model model
    ) {

        model.addAttribute(
                "partner",
                new DeliveryPartner()
        );

        model.addAttribute(
                "isEdit",
                false
        );

        return "admin-delivery-partner/form";
    }


    // Tạo mới
    @PostMapping("/create")
    public String create(
            @ModelAttribute DeliveryPartner partner,
            RedirectAttributes redirectAttributes
    ) {

        adminService.createDeliveryPartner(
                partner
        );

        redirectAttributes.addFlashAttribute(
                "message",
                "Tạo đối tác vận chuyển thành công."
        );

        return "redirect:/admin/delivery-partners";
    }


    // Xem chi tiết
    @GetMapping("/{id}")
    public String detail(
            @PathVariable UUID id,
            Model model
    ) {

        model.addAttribute(
                "partner",
                adminService.getDeliveryPartnerById(id)
        );

        return "admin-delivery-partner/detail";
    }


    // Form cập nhật
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable UUID id,
            Model model
    ) {

        model.addAttribute(
                "partner",
                adminService.getDeliveryPartnerById(id)
        );

        model.addAttribute(
                "isEdit",
                true
        );

        return "admin-delivery-partner/form";
    }


    // Cập nhật
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable UUID id,
            @ModelAttribute DeliveryPartner partner,
            RedirectAttributes redirectAttributes
    ) {

        adminService.updateDeliveryPartner(
                id,
                partner
        );

        redirectAttributes.addFlashAttribute(
                "message",
                "Cập nhật đối tác vận chuyển thành công."
        );

        return "redirect:/admin/delivery-partners";
    }


    // Khóa / mở khóa
    @PostMapping("/{id}/toggle-lock")
    public String toggleLock(
            @PathVariable UUID id,
            @RequestParam(required = false)
            String lockReason,
            RedirectAttributes redirectAttributes
    ) {

        try {

            adminService.toggleDeliveryPartnerLock(
                    id,
                    lockReason
            );

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Đã cập nhật trạng thái đối tác."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/delivery-partners";
    }
}