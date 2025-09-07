package com.example.shopBackend.specification;

import com.example.shopBackend.entity.OrderItem;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class OrderItemSpecification {

    public static Specification<OrderItem> hasStatus(Enum<?> status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<OrderItem> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else if (endDate != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            return null;
        };
    }

    public static Specification<OrderItem> hasItemId(Long itemId) {
        return (root, query, cb) -> itemId == null ? null : cb.equal(root.get("id"), itemId);
    }

    // ✅ Add this missing method
    public static Specification<OrderItem> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }
}
