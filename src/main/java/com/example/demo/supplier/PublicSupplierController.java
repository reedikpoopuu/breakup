package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Authenticated-user read path feeding the RFQ matrix UI - active suppliers only. */
@RestController
@RequestMapping("/api/suppliers")
public class PublicSupplierController {

    private final SupplierService supplierService;

    public PublicSupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> list(@RequestParam(required = false) CountryCode country) {
        return supplierService.listActive(country).stream().map(SupplierResponse::from).toList();
    }
}
