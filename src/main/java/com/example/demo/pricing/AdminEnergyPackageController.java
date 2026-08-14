package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrator-only read model for default energy offers - Control Room Table 2. */
@RestController
@RequestMapping("/api/admin/packages")
public class AdminEnergyPackageController {

    private final EnergyPackageService energyPackageService;

    public AdminEnergyPackageController(EnergyPackageService energyPackageService) {
        this.energyPackageService = energyPackageService;
    }

    @GetMapping
    public List<EnergyPackageResponse> list(@RequestParam(required = false) CountryCode country) {
        return energyPackageService.listAll(country).stream().map(EnergyPackageResponse::from).toList();
    }

    @PostMapping("/scrape")
    public List<EnergyPackageResponse> scrape() {
        return energyPackageService.runScraper().stream().map(EnergyPackageResponse::from).toList();
    }
}
