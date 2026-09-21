package vn.codegyme.meal_choice.service.impl;

import org.springframework.stereotype.Service;
import vn.codegyme.meal_choice.dto.stat.CouponStatDTO;
import vn.codegyme.meal_choice.dto.stat.CustomerStatDTO;
import vn.codegyme.meal_choice.dto.stat.FoodStatDTO;
import vn.codegyme.meal_choice.dto.stat.RevenueStatDTO;
import vn.codegyme.meal_choice.entity.OrderStatus;
import vn.codegyme.meal_choice.repository.OrderItemRepository;
import vn.codegyme.meal_choice.repository.OrderRepository;
import vn.codegyme.meal_choice.service.MerchantStatService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantStatServiceImpl
        implements MerchantStatService {

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;


    public MerchantStatServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository
    ) {

        this.orderRepository =
                orderRepository;

        this.orderItemRepository =
                orderItemRepository;
    }


    // =====================================================
    // TỔNG DOANH THU TÍCH LŨY
    // Dùng cho Rút tiền / Thanh lý
    // Tính với tất cả các đơn
    // =====================================================

    @Override
    public BigDecimal getTotalRevenue(
            UUID merchantId
    ) {

        BigDecimal totalRevenue =
                orderRepository
                        .calculateTotalRevenue(
                                merchantId
                        );


        return totalRevenue != null
                ? totalRevenue
                : BigDecimal.ZERO;
    }


    // =====================================================
    // THỐNG KÊ DOANH THU
    // =====================================================

    @Override
    public List<RevenueStatDTO> getRevenueStats(
            UUID merchantId,
            String type
    ) {

        String merchantIdStr =
                merchantId.toString();


        List<Object[]> rawData =
                switch (
                        type != null
                                ? type.toUpperCase()
                                : "MONTH"
                        ) {

                    case "WEEK" ->
                            orderRepository
                                    .findRevenueStatsByWeek(
                                            merchantIdStr
                                    );

                    case "QUARTER" ->
                            orderRepository
                                    .findRevenueStatsByQuarter(
                                            merchantIdStr
                                    );

                    default ->
                            orderRepository
                                    .findRevenueStatsByMonth(
                                            merchantIdStr
                                    );
                };


        return rawData.stream()
                .map(row ->
                        new RevenueStatDTO(
                                (String) row[0],

                                ((Number) row[1])
                                        .longValue(),

                                BigDecimal.valueOf(
                                        ((Number) row[2])
                                                .doubleValue()
                                )
                        )
                )
                .toList();
    }


    // =====================================================
    // THỐNG KÊ MÓN ĂN
    // =====================================================

    @Override
    public List<FoodStatDTO> getFoodStats(
            UUID merchantId
    ) {

        return orderItemRepository
                .findFoodStatsByMerchant(
                        merchantId
                );
    }


    // =====================================================
    // THỐNG KÊ KHÁCH HÀNG
    // =====================================================

    @Override
    public List<CustomerStatDTO> getCustomerStats(
            UUID merchantId
    ) {

        List<Object[]> rawData =
                orderRepository
                        .findCustomerStatsByMerchant(
                                merchantId.toString()
                        );


        return rawData.stream()
                .map(row ->
                        new CustomerStatDTO(
                                (String) row[0],

                                (String) row[1],

                                ((Number) row[2])
                                        .longValue(),

                                BigDecimal.valueOf(
                                        ((Number) row[3])
                                                .doubleValue()
                                )
                        )
                )
                .toList();
    }


    // =====================================================
    // THỐNG KÊ COUPON
    // =====================================================

    @Override
    public CouponStatDTO getCouponStats(
            UUID merchantId
    ) {

        List<Object[]> rawData =
                orderRepository
                        .findCouponStatsByMerchant(
                                merchantId.toString()
                        );


        if (rawData.isEmpty()
                || rawData.get(0) == null) {

            return new CouponStatDTO(
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }


        Object[] row =
                rawData.get(0);


        long totalOrdersWithDiscount =
                row[0] != null
                        ? ((Number) row[0])
                        .longValue()
                        : 0L;


        BigDecimal totalDiscountAmount =
                row[1] != null
                        ? BigDecimal.valueOf(
                        ((Number) row[1])
                                .doubleValue()
                )
                        : BigDecimal.ZERO;


        BigDecimal totalRevenue =
                row[2] != null
                        ? BigDecimal.valueOf(
                        ((Number) row[2])
                                .doubleValue()
                )
                        : BigDecimal.ZERO;


        return new CouponStatDTO(
                totalOrdersWithDiscount,
                totalDiscountAmount,
                totalRevenue
        );
    }
}