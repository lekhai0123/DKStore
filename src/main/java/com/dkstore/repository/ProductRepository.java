package com.dkstore.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dkstore.models.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findTop6ByOrderByIdAsc();

    List<Product> findTop4ByOrderByIdDesc();

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> search(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.brand.name IN :brandNames")
    List<Product> findByBrand_Names(@Param("brandNames") List<String> brandNames);

    @Query("""
        SELECT p
        FROM Product p
        JOIN p.brand b
        WHERE (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    Page<Product> findByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
        SELECT p
        FROM Product p
        JOIN p.brand b
        WHERE (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND b.name IN :brands
    """)
    Page<Product> findByKeywordAndBrands(
            @Param("keyword") String keyword,
            @Param("brands") List<String> brands,
            Pageable pageable);
}