package com.monkcommerce.coupons.model;

import com.monkcommerce.coupons.utils.JsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Examples: "cart-wise", "product-wise", "bxgy"
    @Column(nullable = false)
    private String type;

    // Stores the specific rules (thresholds, product lists) as a JSON string
    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> details;

    private LocalDateTime expirationDate; // Bonus feature

    private boolean isActive = true;
}

