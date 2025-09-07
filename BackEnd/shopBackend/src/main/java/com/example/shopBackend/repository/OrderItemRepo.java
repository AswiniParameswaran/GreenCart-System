package com.example.shopBackend.repository;

import com.example.shopBackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepo extends JpaRepository <OrderItem,Long>, JpaSpecificationExecutor <OrderItem> {
}
