package vn.codegyme.meal_choice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/merchant/settlement")
public class MerchantSettlementViewController {

    @GetMapping
    public String settlementPage(Model model) {
        model.addAttribute("activeMenu", "settlement");
        return "merchant/settlement/settlement";
    }
}
