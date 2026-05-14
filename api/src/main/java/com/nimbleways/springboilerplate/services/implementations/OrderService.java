package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public OrderService(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    public void processOrder(Order order) {
        for (Product product : order.getItems()) {
            processProduct(product);
        }
    }

    private void processProduct(Product product) {
        switch (product.getType()) {
            case NORMAL    -> handleNormalProduct(product);
            case SEASONAL  -> handleSeasonalProduct(product);
            case EXPIRABLE -> handleExpirableProduct(product);
        }
    }

    private void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else if (product.getLeadTime() > 0) {
            productService.notifyDelay(product.getLeadTime(), product);
        }
    }

    private void handleSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        boolean isInSeason = !today.isBefore(product.getSeasonStartDate())
                && today.isBefore(product.getSeasonEndDate());

        if (isInSeason && product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            productService.handleSeasonalProduct(product);
        }
    }

    private void handleExpirableProduct(Product product) {
        productService.handleExpiredProduct(product);
    }
}