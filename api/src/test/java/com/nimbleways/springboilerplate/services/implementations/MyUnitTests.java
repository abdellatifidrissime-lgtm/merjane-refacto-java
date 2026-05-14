package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.nimbleways.springboilerplate.entities.ProductType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private ProductService productService;

    // ─── NORMAL ────────────────────────────────────────────────────────────────

    @Test
    void should_notify_delay_when_normal_product_has_lead_time() {
        // GIVEN
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(15, product.getLeadTime());
        verify(productRepository, times(1)).save(product);
        verify(notificationService, times(1)).sendDelayNotification(15, "RJ45 Cable");
    }

    // ─── SEASONAL ──────────────────────────────────────────────────────────────

    @Test
    void should_set_available_to_zero_and_notify_when_lead_time_exceeds_season_end() {
        // GIVEN — leadTime=10j, saison finit dans 5j → réappro impossible
        Product product = new Product(null, 10, 5, ProductType.SEASONAL, "Watermelon",
                null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.handleSeasonalProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification("Watermelon");
    }

    @Test
    void should_notify_out_of_stock_when_season_has_not_started_yet() {
        // GIVEN — saison commence dans 30j
        Product product = new Product(null, 5, 0, ProductType.SEASONAL, "Grapes",
                null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(120));
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.handleSeasonalProduct(product);

        // THEN
        verify(notificationService).sendOutOfStockNotification("Grapes");
    }

    // ─── EXPIRABLE ─────────────────────────────────────────────────────────────

    @Test
    void should_decrease_stock_when_expirable_product_is_valid_and_in_stock() {
        // GIVEN — expire demain, stock > 0
        Product product = new Product(null, 5, 10, ProductType.EXPIRABLE, "Butter",
                LocalDate.now().plusDays(1), null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.handleExpiredProduct(product);

        // THEN
        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
    }

    @Test
    void should_notify_and_zero_stock_when_expirable_product_is_expired() {
        // GIVEN — expiré hier
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Product product = new Product(null, 5, 3, ProductType.EXPIRABLE, "Milk",
                yesterday, null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.handleExpiredProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Milk", yesterday);
    }

    @Test
    void should_notify_and_zero_stock_when_expirable_product_has_no_stock() {
        // GIVEN — pas expiré mais stock = 0
        Product product = new Product(null, 5, 0, ProductType.EXPIRABLE, "Yogurt",
                LocalDate.now().plusDays(5), null, null);
        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.handleExpiredProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Yogurt", product.getExpiryDate());
    }
}