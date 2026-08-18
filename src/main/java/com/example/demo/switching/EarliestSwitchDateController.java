package com.example.demo.switching;

import com.example.demo.common.CountryCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Public (see SecurityConfig - no company/session context needed, nothing sensitive):
 * "if I switched today, what's the earliest my new contract could start in this
 * country?" Pure calendar arithmetic, not user- or contract-specific - see
 * {@link SwitchingWindowCalculator}.
 */
@RestController
@RequestMapping("/api/switching")
public class EarliestSwitchDateController {

    public record EarliestSwitchDateResponse(CountryCode country, LocalDate referenceDate, LocalDate earliestSwitchDate) {
    }

    private final SwitchingWindowCalculator calculator;

    public EarliestSwitchDateController(SwitchingWindowCalculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/earliest-date")
    public EarliestSwitchDateResponse earliestSwitchDate(@RequestParam CountryCode country) {
        LocalDate today = LocalDate.now();
        return new EarliestSwitchDateResponse(country, today, calculator.earliestSwitchDate(country, today));
    }
}
