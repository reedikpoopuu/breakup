package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrator-only read/write model for default energy offers - Control Room Table 2. */
@RestController
@RequestMapping("/api/admin/packages")
public class AdminEnergyPackageController {

    public record VisibilityRequest(boolean visible) {
    }

    private final EnergyPackageService energyPackageService;

    public AdminEnergyPackageController(EnergyPackageService energyPackageService) {
        this.energyPackageService = energyPackageService;
    }

    @GetMapping
    public List<EnergyPackageResponse> list(@RequestParam(required = false) CountryCode country) {
        return energyPackageService.listAll(country).stream().map(EnergyPackageResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnergyPackageResponse create(@Valid @RequestBody EnergyPackageRequest request) {
        return EnergyPackageResponse.from(energyPackageService.create(request));
    }

    @PutMapping("/{id}")
    public EnergyPackageResponse update(@PathVariable Long id, @Valid @RequestBody EnergyPackageRequest request) {
        return EnergyPackageResponse.from(energyPackageService.update(id, request));
    }

    @PatchMapping("/{id}/visibility")
    public EnergyPackageResponse setVisibility(@PathVariable Long id, @RequestBody VisibilityRequest request) {
        return EnergyPackageResponse.from(energyPackageService.setVisible(id, request.visible()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        energyPackageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scrape")
    public List<EnergyPackageResponse> scrape() {
        return energyPackageService.runScraper().stream().map(EnergyPackageResponse::from).toList();
    }
}
