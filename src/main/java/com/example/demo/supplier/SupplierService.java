package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
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
        supplier.update(request.name(), request.rfqEmail(), request.priceUrl());
        return supplier;
    }

    @Transactional
    public void softDelete(Long id) {
        Supplier supplier = repository.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
        supplier.setActive(false);
    }
}
