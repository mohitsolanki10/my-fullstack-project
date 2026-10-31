package com.radhe.SpringEcom.service;

import com.radhe.SpringEcom.model.Order;
import com.radhe.SpringEcom.model.OrderItem;
import com.radhe.SpringEcom.model.Products;
import com.radhe.SpringEcom.model.dto.*;
import com.radhe.SpringEcom.repo.OrderRepo;
import com.radhe.SpringEcom.repo.ProductRepo;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderService orderService;


    @Transactional
    public PaymentOrderResponse createPaymentOrder(OrderRequest request) throws RazorpayException {

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = new Order();
        String orderId = "ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderId(orderId);
        order.setCustomerName(request.customerName());
        order.setEmail(request.email());
        order.setStatus("PAYMENT_PENDING");
        order.setOrderDate(LocalDate.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.items()) {
            if (itemReq.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
            }

            Products product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

            if (!product.isProductAvailable() || product.getStockQuantity() < itemReq.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName());
            }

            BigDecimal linePrice = product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(linePrice);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .totalPrice(linePrice)
                    .order(order)
                    .build());
        }

        // Razorpay amount PAISE me leta hai (Rs 1 = 100 paise)
        long amountInPaise = total.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (amountInPaise < 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum order amount is Rs 1");
        }

        JSONObject options = new JSONObject();
        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put("receipt", orderId);

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        com.razorpay.Order razorpayOrder = client.orders.create(options);
        String razorpayOrderId = razorpayOrder.get("id");

        order.setRazorpayOrderId(razorpayOrderId);
        order.setTotalAmount(total);
        order.setOrderItems(orderItems);
        orderRepo.save(order);

        return new PaymentOrderResponse(keyId, razorpayOrderId, amountInPaise, "INR", orderId);
    }


    @Transactional
    public OrderResponse verifyAndPlaceOrder(PaymentVerifyRequest req) {

        Order order = orderRepo.findByRazorpayOrderId(req.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Agar pehle hi verify ho chuka hai (double click / retry) to wahi order wapas de do
        if ("PLACED".equals(order.getStatus())) {
            return orderService.toResponse(order);
        }

        if (!isSignatureValid(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        }

        for (OrderItem item : order.getOrderItems()) {
            Products product = item.getProduct();
            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0) {
                // Payment ho chuka hai par stock khatam -> is order ko dashboard se manually refund karna hoga
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock finished for " + product.getName() + ". Please contact support for refund.");
            }
            product.setStockQuantity(newStock);
            productRepo.save(product);
        }

        order.setStatus("PLACED");
        order.setRazorpayPaymentId(req.razorpayPaymentId());
        orderRepo.save(order);

        return orderService.toResponse(order);
    }

    // Razorpay ka rule: HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret) == signature
    private boolean isSignatureValid(String razorpayOrderId, String paymentId, String signature) {
        if (razorpayOrderId == null || paymentId == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((razorpayOrderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}

