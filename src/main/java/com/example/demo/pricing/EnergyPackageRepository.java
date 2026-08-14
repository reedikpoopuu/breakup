package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnergyPackageRepository extends JpaRepository<EnergyPackage, Long> {

    List<EnergyPackage> findByCountry(CountryCode country);

    boolean existsBySupplierNameAndPackageName(String supplierName, String packageName);
}
