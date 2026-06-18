package com.iduenduen.mtsservice.domain.order.repository;

import com.iduenduen.mtsservice.domain.order.entity.TradeOrder;
import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByAccountIdAndStatus(Long accountId, OrderStatus status);
    List<TradeOrder> findByAccountIdAndSideAndStatus(Long accountId, OrderSide side, OrderStatus status);

    List<TradeOrder> findByAccountIdAndStatusIn(Long accountId, List<OrderStatus> statuses);
    List<TradeOrder> findByAccountIdAndSideAndStatusIn(Long accountId, OrderSide side, List<OrderStatus> statuses);
}
