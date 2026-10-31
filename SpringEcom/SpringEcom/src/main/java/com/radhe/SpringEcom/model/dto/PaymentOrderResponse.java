package com.radhe.SpringEcom.model.dto;

public record PaymentOrderResponse(
        String keyId,            // public key (rzp_test_...)
        String razorpayOrderId,  // order_xxxxx
        long amount,             // paise me (Rs 10 = 1000)
        String currency,
        String orderId           // hamara apna order id (ORDxxxx)
) {}
