package com.radhe.SpringEcom.Controller;

import com.radhe.SpringEcom.model.dto.OrderRequest;
import com.radhe.SpringEcom.model.dto.OrderResponse;
import com.radhe.SpringEcom.model.dto.PaymentOrderResponse;
import com.radhe.SpringEcom.model.dto.PaymentVerifyRequest;
import com.radhe.SpringEcom.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Frontend: "Pay" dabate hi ye call hota hai
    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(@RequestBody OrderRequest request) {
        try {
            return ResponseEntity.ok(paymentService.createPaymentOrder(request));
        } catch (RazorpayException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Razorpay error: " + e.getMessage());
        }
    }

    // Frontend: payment successful hone ke baad ye call hota hai
    @PostMapping("/verify")
    public ResponseEntity<OrderResponse> verify(@RequestBody PaymentVerifyRequest request) {
        return new ResponseEntity<>(paymentService.verifyAndPlaceOrder(request), HttpStatus.CREATED);
    }
}
