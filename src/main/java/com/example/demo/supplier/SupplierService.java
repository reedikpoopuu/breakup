package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import com.example.demo.common.OutboundUrlValidator;
import com.example.demo.pricing.EnergyPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository repository;
    private final EnergyPackageRepository energyPackageRepository;
    private final OutboundUrlValidator outboundUrlValidator;

    public SupplierService(SupplierRepository repository, EnergyPackageRepository energyPackageRepository,
                            OutboundUrlValidator outboundUrlValidator) {
        this.repository = repository;
        this.energyPackageRepository = energyPackageRepository;
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Transactional(readOnly = true)
    public List<Supplier> listAll(CountryCode country) {
        return country == null ? repository.findAll() : repository.findByCountry(country);
    }

    @Transactional(readOnly = true)
    public List<Supplier> listActive(CountryCode country) {
        return country == null
                ? repository.findAll().stream().filter(Supplier::isActive).toList()
                : repository.findByCountryAndActiveTrue(country);
    }

    @Transactional
    public Supplier create(SupplierRequest request) {
        if (repository.existsByCountryAndName(request.country(), request.name())) {
            throw new DuplicateSupplierException(request.country(), request.name());
        }
        outboundUrlValidator.validate(request.priceUrl());
        Supplier supplier = new Supplier(request.country(), request.name(), request.rfqEmail(), request.priceUrl());
        return repository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = repository.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        boolean renaming = !supplier.getCountry().equals(request.country()) || !supplier.getName().equals(request.name());
        if (renaming && repository.existsByCountryAndName(request.country(), request.name())) {
            throw new DuplicateSupplierException(request.country(), request.name());
        }
        outboundUrlValidator.validate(request.priceUrl());
        supplier.update(request.name(), request.rfqEmail(), request.priceUrl());
        return supplier;
    }

    @Transactional
    public void softDelete(Long id) {
        Supplier supplier = repository.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.setActive(false);
        long removed = energyPackageRepository.deleteBySupplierNameAndCountry(supplier.getName(), supplier.getCountry());
        if (removed > 0) {
            log.info("Deleted {} energy package(s) for removed supplier {}/{}", removed, supplier.getCountry(), supplier.getName());
        }
    }
}
