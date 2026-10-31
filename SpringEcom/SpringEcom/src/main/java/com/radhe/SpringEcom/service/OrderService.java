package com.radhe.SpringEcom.service;


import com.radhe.SpringEcom.model.Order;
import com.radhe.SpringEcom.model.OrderItem;
import com.radhe.SpringEcom.model.Products;
import com.radhe.SpringEcom.model.dto.OrderItemRequest;
import com.radhe.SpringEcom.model.dto.OrderItemResponse;
import com.radhe.SpringEcom.model.dto.OrderRequest;
import com.radhe.SpringEcom.model.dto.OrderResponse;
import com.radhe.SpringEcom.repo.OrderRepo;
import com.radhe.SpringEcom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepo orderRepo;

    @Transactional
    public List<OrderResponse> getAllOrderResponses() {
        List<Order> orders = orderRepo.findByStatusNotOrderByIdDesc("PAYMENT_PENDING");
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(toResponse(order));
        }
        return responses;
    }

    // Order (entity) -> OrderResponse (frontend ko jaane wala). Transaction ke andar hi call karna.
    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            itemResponses.add(new OrderItemResponse(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotalPrice()
            ));
        }
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerName(),
                order.getEmail(),
                order.getStatus(),
                order.getOrderDate(),
                order.getRazorpayPaymentId(),
                itemResponses
        );
    }
}
