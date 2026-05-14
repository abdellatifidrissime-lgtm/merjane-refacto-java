package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MyControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    // ─── NORMAL ────────────────────────────────────────────────────────────────

    @Test
    void should_decrease_stock_when_normal_product_is_available() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.NORMAL, "USB Cable",
                null, null, null);
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        Product result = productRepository.findById(product.getId()).get();
        assertEquals(9, (int) result.getAvailable());
        verifyNoInteractions(notificationService);
    }

    @Test
    void should_notify_delay_when_normal_product_is_out_of_stock() throws Exception {
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "USB Dongle",
                null, null, null);
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        verify(notificationService).sendDelayNotification(15, "USB Dongle");
    }

    // ─── EXPIRABLE ─────────────────────────────────────────────────────────────

    @Test
    void should_decrease_stock_when_expirable_product_is_valid() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.EXPIRABLE, "Butter",
                LocalDate.now().plusDays(26), null, null);
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        Product result = productRepository.findById(product.getId()).get();
        assertEquals(9, (int) result.getAvailable());
    }

    @Test
    void should_notify_expiration_when_expirable_product_is_expired() throws Exception {
        LocalDate expiryDate = LocalDate.now().minusDays(2);
        Product product = new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk",
                expiryDate, null, null);
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        verify(notificationService).sendExpirationNotification("Milk", expiryDate);
    }

    // ─── SEASONAL ──────────────────────────────────────────────────────────────

    @Test
    void should_decrease_stock_when_seasonal_product_is_in_season() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.SEASONAL, "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        Product result = productRepository.findById(product.getId()).get();
        assertEquals(9, (int) result.getAvailable());
    }

    @Test
    void should_notify_out_of_stock_when_seasonal_product_is_out_of_season() throws Exception {
        Product product = new Product(null, 15, 10, ProductType.SEASONAL, "Grapes",
                null, LocalDate.now().plusDays(180), LocalDate.now().plusDays(240));
        Order order = createAndSaveOrder(product);

        performProcessOrder(order.getId());

        verify(notificationService).sendOutOfStockNotification("Grapes");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Order createAndSaveOrder(Product product) {
        productRepository.save(product);
        Order order = new Order();
        order.setItems(Set.of(product));
        return orderRepository.save(order);
    }

    private void performProcessOrder(Long orderId) throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", orderId)
                        .contentType("application/json"))
                .andExpect(status().isOk());
    }
}