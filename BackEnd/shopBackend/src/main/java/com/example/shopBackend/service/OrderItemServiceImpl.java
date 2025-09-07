package com.example.shopBackend.service;
import jakarta.validation.ValidationException;
import com.example.shopBackend.dto.OrderItemDto;
import com.example.shopBackend.dto.OrderRequest;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.entity.Order;
import com.example.shopBackend.entity.OrderItem;
import com.example.shopBackend.entity.Product;
import com.example.shopBackend.entity.User;
import com.example.shopBackend.enums.OrderStatus;
import com.example.shopBackend.enums.UserRole;
import com.example.shopBackend.exceptions.NotFoundException;
import com.example.shopBackend.mapper.EntityDtoMapper;
import com.example.shopBackend.repository.OrderItemRepo;
import com.example.shopBackend.repository.OrderRepo;
import com.example.shopBackend.repository.ProductRepo;
import com.example.shopBackend.specification.OrderItemSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;


import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {
    @Autowired
    private final OrderRepo orderRepo;
    @Autowired
    private final OrderItemRepo orderItemRepo;
    @Autowired
    private final ProductRepo productRepo;
    @Autowired
    private final UserService userService;
    @Autowired
    private final EntityDtoMapper entityDtoMapper;

    private static final int MAX_QUANTITY = 1000; // business rule limit
    private static final int MAX_ITEMS_PER_ORDER = 100;

    @Override
    public Response placeOrder(OrderRequest orderRequest) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new NotFoundException("Authenticated user not found");
        }
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new ValidationException("Order must contain at least one item");
        }
        if (orderRequest.getItems().size() > MAX_ITEMS_PER_ORDER) {
            throw new ValidationException("Too many items in order");
        }
        if (orderRequest.getDeliveryDate() == null) {
            throw new ValidationException("Delivery date is required");
        }
        LocalDate deliveryDate = orderRequest.getDeliveryDate();
        if (!deliveryDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Delivery date must be in the future");
        }
        if (deliveryDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ValidationException("No deliveries allowed on Sunday");
        }

        List<OrderItem> orderItems = orderRequest.getItems().stream().map(orderItemRequest -> {
            if (orderItemRequest.getQuantity() <= 0 || orderItemRequest.getQuantity() > MAX_QUANTITY) {
                throw new ValidationException("Invalid quantity for product id: " + orderItemRequest.getProductId());
            }

            Product product = productRepo.findById(orderItemRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product Not Found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(orderItemRequest.getQuantity());
            orderItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(orderItemRequest.getQuantity()))); //set price according to the quantity
            orderItem.setStatus(OrderStatus.PENDING);
            orderItem.setUser(user);
            return orderItem;

        }).collect(Collectors.toList());

        // calculate the total price — trust client only if > 0; otherwise compute server-side
        BigDecimal totalPrice = orderRequest.getTotalPrice() != null && orderRequest.getTotalPrice().compareTo(BigDecimal.ZERO) > 0
                ? orderRequest.getTotalPrice()
                : orderItems.stream().map(OrderItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total price must be greater than zero");
        }

        // create order entity
        Order order = new Order();
        order.setOrderItemList(orderItems);
        order.setTotalPrice(totalPrice);

        // set the order reference in each orderitem
        orderItems.forEach(orderItem -> orderItem.setOrder(order));

        orderRepo.save(order);

        return Response.builder()
                .status(200)
                .message("Order was successfully placed")
                .build();
    }

    @Override
    public Response updateOrderItemStatus(Long orderItemId, String status) {
        // Only admin should update order item statuses in many systems — enforce RBAC
        User user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized: only admins may update order statuses");
        }

        OrderItem orderItem = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order Item not found"));

        // validate and map status
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid order status: " + status);
        }

        orderItem.setStatus(newStatus);
        orderItemRepo.save(orderItem);
        return Response.builder()
                .status(200)
                .message("Order status updated successfully")
                .build();
    }

    @Override
    public Response filterOrderItems(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Long itemId, Pageable pageable) {
        // authorization: allow admin to view all; users only their own results
        User user = userService.getLoginUser();
        if (user == null) {
            throw new NotFoundException("Authenticated user not found");
        }

        Specification<OrderItem> spec = Specification.where(OrderItemSpecification.hasStatus(status))
                .and(OrderItemSpecification.createdBetween(startDate, endDate))
                .and(OrderItemSpecification.hasItemId(itemId));

        // If user is not admin, restrict to their own items
        if (user.getRole() != UserRole.ADMIN) {
            spec = spec.and(OrderItemSpecification.hasUserId(user.getId()));
        }

        Page<OrderItem> orderItemPage = orderItemRepo.findAll(spec, pageable);

        if (orderItemPage.isEmpty()) {
            throw new NotFoundException("No Order Found");
        }
        List<OrderItemDto> orderItemDtos = orderItemPage.getContent().stream()
                .map(entityDtoMapper::mapOrderItemToDtoPlusProductAndUser)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .orderItemList(orderItemDtos)
                .totalPage(orderItemPage.getTotalPages())
                .totalElement(orderItemPage.getTotalElements())
                .build();
    }
}
