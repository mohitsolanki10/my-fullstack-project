package com.radhe.SpringEcom.model.dto;

public record PaymentVerifyRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}