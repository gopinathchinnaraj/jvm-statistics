package com.gopinath_statslab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order — JPA entity that maps to the `orders` table in PostgreSQL.
 *
 * LEARNING NOTES
 * ──────────────
 * @Entity
 *   Tells Hibernate: "This class = one row in a DB table."
 *   Hibernate generates the SQL SELECT/INSERT/UPDATE/DELETE for you.
 *
 * @Table(name = "orders")
 *   Maps to the "orders" table. Without this, Hibernate defaults to
 *   the class name ("Order") as the table name, which would fail here
 *   because "order" is a reserved SQL keyword.
 *
 * @Id + @GeneratedValue
 *   Marks the primary key. IDENTITY strategy tells Hibernate to let
 *   PostgreSQL's BIGSERIAL (auto-increment) assign the id on INSERT.
 *
 * @Column(name = "product_category")
 *   The Java field is "productCategory" (camelCase) but the DB column
 *   is "product_category" (snake_case). @Column bridges the naming gap.
 *
 * @Data (Lombok)
 *   Auto-generates: getters, setters, equals(), hashCode(), toString().
 *   Without Lombok you'd write ~80 lines of boilerplate manually.
 *
 * @Builder (Lombok)
 *   Lets you construct objects like:
 *     Order.builder().country("Iceland").amount(new BigDecimal("999")).build()
 *
 * @NoArgsConstructor / @AllArgsConstructor (Lombok)
 *   JPA requires a no-arg constructor. @AllArgsConstructor is needed
 *   by @Builder to work alongside @NoArgsConstructor.
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * customer_id — which customer placed this order.
     * ~100,000 distinct values, power-law skewed (a few customers
     * account for a disproportionate number of orders).
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /**
     * country — the core column for our demo.
     * India ~70%, Iceland ~0.012% (~120 rows).
     * When statistics are missing, the planner massively overestimates
     * how many Iceland rows exist → chooses FULL SCAN instead of INDEX.
     */
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    /**
     * city — logically tied to country (no impossible combos).
     * Useful for compound-predicate estimation experiments later.
     */
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /**
     * product_category — 10 categories, skewed distribution.
     * Electronics 20%, Healthcare 4%.
     */
    @Column(name = "product_category", nullable = false, length = 100)
    private String productCategory;

    /**
     * status — low cardinality (only 5 distinct values).
     * Ideal column for Most Common Values (MCV) statistics demo.
     * COMPLETED 65%, PENDING 15%, CANCELLED 10%, FAILED 6%, REFUNDED 4%.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * amount — NUMERIC(12,2) in the DB; BigDecimal in Java.
     * WHY BigDecimal and NOT double?
     * double uses binary floating point → 0.1 + 0.2 = 0.30000000000000004
     * BigDecimal is exact decimal arithmetic → safe for money.
     * 95% of amounts fall in 10..5,000 (squared distribution = concentrated
     * near low end). 5% fall in 5,000..100,000 (cubed = very few huge orders).
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * created_at — order timestamp.
     * Spread over 24 months with seasonal sine-wave weighting
     * (some months busier than others). Useful for date-range
     * histogram experiments in Phase 3.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
