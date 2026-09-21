package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.codegyme.meal_choice.dto.settlement.AdminSettlementItemDTO;
import vn.codegyme.meal_choice.dto.settlement.AdminSettlementStatsDTO;
import vn.codegyme.meal_choice.service.MerchantSettlementService;

import java.util.List;

@Controller
@RequestMapping("/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final MerchantSettlementService settlementService;

    @GetMapping
    public String showAdminSettlements(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        List<AdminSettlementItemDTO> settlements = settlementService.getAdminSettlements(status, keyword);
        AdminSettlementStatsDTO stats = settlementService.getAdminSettlementStats();

        model.addAttribute("settlements", settlements);
        model.addAttribute("stats", stats);
        model.addAttribute("selectedStatus", status != null ? status : "ALL");
        model.addAttribute("keyword", keyword != null ? keyword : "");

        return "admin/settlement/settlement";
    }
}
