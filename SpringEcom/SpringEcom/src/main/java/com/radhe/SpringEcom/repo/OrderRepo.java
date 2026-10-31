package com.radhe.SpringEcom.repo;

import com.radhe.SpringEcom.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order,Integer> {


    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Jin orders ka payment nahi hua (PAYMENT_PENDING) unhe list me nahi dikhana
    List<Order> findByStatusNotOrderByIdDesc(String status);

}
