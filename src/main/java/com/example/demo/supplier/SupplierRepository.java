package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByCountry(CountryCode country);

    List<Supplier> findByCountryAndActiveTrue(CountryCode country);

    Optional<Supplier> findByCountryAndName(CountryCode country, String name);

    boolean existsByCountryAndName(CountryCode country, String name);
}
