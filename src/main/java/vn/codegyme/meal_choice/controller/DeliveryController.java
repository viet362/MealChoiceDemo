package vn.codegyme.meal_choice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.codegyme.meal_choice.service.DeliveryQuoteService;
import vn.codegyme.meal_choice.service.ShippingQuote;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryQuoteService
            deliveryQuoteService;


    @GetMapping("/quotes")
    public List<ShippingQuote> getQuotes(

            @RequestParam UUID merchantId,

            @RequestParam Long addressId
    ) {

        return deliveryQuoteService
                .getQuotes(
                        merchantId,
                        addressId
                );
    }
}
