package com.radhe.SpringEcom.Controller;


import com.radhe.SpringEcom.model.dto.OrderRequest;
import com.radhe.SpringEcom.model.dto.OrderResponse;
import com.radhe.SpringEcom.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders(){
        List<OrderResponse> responses = orderService.getAllOrderResponses();
        return new ResponseEntity<>(responses,HttpStatus.OK);
    }
}
