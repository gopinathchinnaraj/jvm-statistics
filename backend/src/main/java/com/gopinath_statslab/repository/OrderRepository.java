package com.gopinath_statslab.repository;

import com.gopinath_statslab.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * OrderRepository — data access layer for the orders table.
 *
 * LEARNING NOTES
 * ──────────────
 * extends JpaRepository<Order, Long>
 *   - Order  = the entity type
 *   - Long   = the type of the primary key (id is Long)
 *
 *   By extending JpaRepository, Spring Data gives you these methods
 *   FOR FREE — you write ZERO SQL for them:
 *
 *     findAll()                  → SELECT * FROM orders
 *     findById(id)               → SELECT * FROM orders WHERE id = ?
 *     save(order)                → INSERT or UPDATE
 *     deleteById(id)             → DELETE FROM orders WHERE id = ?
 *     count()                    → SELECT COUNT(*) FROM orders
 *     existsById(id)             → SELECT 1 FROM orders WHERE id = ?
 *
 *   Spring Data implements these at runtime by generating a proxy class.
 *   You never write the implementation yourself.
 *
 * Custom queries
 *   When the free methods aren't enough, you can either:
 *   a) Use @Query with JPQL  (Java Persistence Query Language — like SQL
 *      but references entity/field names, not table/column names)
 *   b) Use @Query(nativeQuery=true) for raw PostgreSQL SQL
 *   c) Use method name derivation: findByCountry(String country)
 *      → Spring Data reads the method name and generates:
 *        SELECT * FROM orders WHERE country = ?
 *
 * @Repository
 *   Marks this as a Spring bean. Also enables Spring's exception
 *   translation — converts low-level SQLException into Spring's
 *   DataAccessException hierarchy (easier to catch and handle).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ──────────────────────────────────────────────────────────────
    // Method-name derived queries — Spring Data reads the name and
    // generates the SQL automatically.
    // ──────────────────────────────────────────────────────────────

    /**
     * Find all orders from a specific country.
     *
     * Generated SQL (approximately):
     *   SELECT * FROM orders WHERE country = ?
     *
     * This is the CORE query for our Phase 1 demo:
     *   findByCountry("Iceland") → 120 rows
     *   findByCountry("India")   → ~700,000 rows
     */
    List<Order> findByCountry(String country);

    /**
     * Count orders by country — used for actual row count in plan comparison.
     *
     * Generated SQL:
     *   SELECT COUNT(*) FROM orders WHERE country = ?
     */
    long countByCountry(String country);

    /**
     * Count orders by status.
     *
     * Generated SQL:
     *   SELECT COUNT(*) FROM orders WHERE status = ?
     */
    long countByStatus(String status);

    /**
     * Find orders above a certain amount.
     *
     * Generated SQL:
     *   SELECT * FROM orders WHERE amount > ?
     */
    List<Order> findByAmountGreaterThan(BigDecimal amount);

    /**
     * Count orders above a certain amount.
     *
     * Generated SQL:
     *   SELECT COUNT(*) FROM orders WHERE amount > ?
     */
    long countByAmountGreaterThan(BigDecimal amount);

    // ──────────────────────────────────────────────────────────────
    // @Query — custom JPQL for aggregations and analytics
    // Note: field names here are Java field names (camelCase),
    //       NOT column names. Hibernate translates them to SQL.
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns all distinct country values in the dataset.
     * Used by the statistics collector to discover categorical columns.
     */
    @Query("SELECT DISTINCT o.country FROM Order o ORDER BY o.country")
    List<String> findDistinctCountries();

    /**
     * Returns all distinct status values.
     */
    @Query("SELECT DISTINCT o.status FROM Order o ORDER BY o.status")
    List<String> findDistinctStatuses();

    /**
     * Returns [country, count] pairs — the Most Common Values query
     * for the country column. Used by Phase 2 statistics collection.
     *
     * Returns Object[] where:
     *   [0] = country (String)
     *   [1] = count   (Long)
     */
    @Query("SELECT o.country, COUNT(o) FROM Order o GROUP BY o.country ORDER BY COUNT(o) DESC")
    List<Object[]> countByCountryGrouped();

    /**
     * Returns [status, count] pairs — MCV query for status column.
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status ORDER BY COUNT(o) DESC")
    List<Object[]> countByStatusGrouped();

    /**
     * Returns [productCategory, count] pairs.
     */
    @Query("SELECT o.productCategory, COUNT(o) FROM Order o GROUP BY o.productCategory ORDER BY COUNT(o) DESC")
    List<Object[]> countByProductCategoryGrouped();

    /**
     * Compound predicate — country AND amount filter.
     * Used in Phase 3 to demonstrate multi-column selectivity estimation.
     *
     * Generated SQL:
     *   SELECT COUNT(*) FROM orders WHERE country = ? AND amount > ?
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.country = :country AND o.amount > :minAmount")
    long countByCountryAndAmountGreaterThan(
            @Param("country") String country,
            @Param("minAmount") BigDecimal minAmount
    );
}
