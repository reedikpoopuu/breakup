package com.example.demo.switching;

import com.example.demo.common.CountryCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** No auth header anywhere here on purpose - see SecurityConfig, this endpoint is public. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EarliestSwitchDateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void isReachableWithoutAuthenticationAndMatchesTheCalculatorDirectly() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate expected = new SwitchingWindowCalculator().earliestSwitchDate(CountryCode.EE, today);

        mockMvc.perform(get("/api/switching/earliest-date").param("country", "EE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("EE"))
                .andExpect(jsonPath("$.referenceDate").value(today.toString()))
                .andExpect(jsonPath("$.earliestSwitchDate").value(expected.toString()));
    }

    @Test
    void rejectsAnUnrecognizedCountry() throws Exception {
        mockMvc.perform(get("/api/switching/earliest-date").param("country", "not-a-country"))
                .andExpect(status().isBadRequest());
    }
}
