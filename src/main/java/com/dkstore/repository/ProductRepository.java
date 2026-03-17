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

    @Query(value = """
        select p.*
        from products p
        join brand b on b.id = p.brand_id
        where (
            cast(:keyword as text) is null
            or lower(p.name) like '%' || cast(:keyword as text) || '%'
            or lower(b.name) like '%' || cast(:keyword as text) || '%'
        )
        """,
        countQuery = """
        select count(*)
        from products p
        join brand b on b.id = p.brand_id
        where (
            cast(:keyword as text) is null
            or lower(p.name) like '%' || cast(:keyword as text) || '%'
            or lower(b.name) like '%' || cast(:keyword as text) || '%'
        )
        """,
        nativeQuery = true)
    Page<Product> findByKeywordNative(String keyword, Pageable pageable);

    @Query(value = """
        select p.*
        from products p
        join brand b on b.id = p.brand_id
        where (
            cast(:keyword as text) is null
            or lower(p.name) like '%' || cast(:keyword as text) || '%'
            or lower(b.name) like '%' || cast(:keyword as text) || '%'
        )
        and b.name in (:brands)
        """,
        countQuery = """
        select count(*)
        from products p
        join brand b on b.id = p.brand_id
        where (
            cast(:keyword as text) is null
            or lower(p.name) like '%' || cast(:keyword as text) || '%'
            or lower(b.name) like '%' || cast(:keyword as text) || '%'
        )
        and b.name in (:brands)
        """,
        nativeQuery = true)
    Page<Product> findByKeywordAndBrandsNative(String keyword, List<String> brands, Pageable pageable);
}