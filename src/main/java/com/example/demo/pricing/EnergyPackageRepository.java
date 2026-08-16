package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnergyPackageRepository extends JpaRepository<EnergyPackage, Long> {

    List<EnergyPackage> findByCountry(CountryCode country);

    List<EnergyPackage> findByVisibleTrue();

    List<EnergyPackage> findByCountryAndVisibleTrue(CountryCode country);

    Optional<EnergyPackage> findBySupplierNameAndPackageName(String supplierName, String packageName);

    boolean existsBySupplierNameAndPackageName(String supplierName, String packageName);
}
