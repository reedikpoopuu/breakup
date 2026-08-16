package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Customer-facing read path - visible packages only; hidden ones never leave the admin API. */
@RestController
@RequestMapping("/api/packages")
public class PublicEnergyPackageController {

    private final EnergyPackageService energyPackageService;

    public PublicEnergyPackageController(EnergyPackageService energyPackageService) {
        this.energyPackageService = energyPackageService;
    }

    @GetMapping
    public List<EnergyPackageResponse> list(@RequestParam(required = false) CountryCode country) {
        return energyPackageService.listVisible(country).stream().map(EnergyPackageResponse::from).toList();
    }
}
