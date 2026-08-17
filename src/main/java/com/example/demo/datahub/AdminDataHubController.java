package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrator-only, read-only status of the three national DataHub adapters. */
@RestController
@RequestMapping("/api/admin/datahub")
public class AdminDataHubController {

    private final EstfeedProperties estfeed;
    private final StepProperties step;
    private final EsoProperties eso;

    public AdminDataHubController(EstfeedProperties estfeed, StepProperties step, EsoProperties eso) {
        this.estfeed = estfeed;
        this.step = step;
        this.eso = eso;
    }

    @GetMapping("/status")
    public List<DataHubStatusResponse> status() {
        return List.of(
                new DataHubStatusResponse(
                        DataHubSource.ESTFEED, CountryCode.EE, estfeed.isConfigured(),
                        estfeed.getBaseUrl(), estfeed.getAuthBaseUrl(), estfeed.getAuthRealm(),
                        estfeed.getMarketParticipantEic(), estfeed.getMarketParticipantRole(),
                        StringUtils.hasText(estfeed.getClientId()) && StringUtils.hasText(estfeed.getClientSecret())),
                new DataHubStatusResponse(
                        DataHubSource.STEP, CountryCode.LV, step.isConfigured(),
                        step.getBaseUrl(), step.getAuthBaseUrl(), null, null, null,
                        StringUtils.hasText(step.getUsername()) && StringUtils.hasText(step.getPassword())),
                new DataHubStatusResponse(
                        DataHubSource.ESO, CountryCode.LT, eso.isConfigured(),
                        eso.getBaseUrl(), null, null, null, null,
                        StringUtils.hasText(eso.getClientId()) && StringUtils.hasText(eso.getClientSecret()))
        );
    }
}
