package com.example.demo.switching;

import com.example.demo.common.CountryCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SwitchingWindowCalculatorTest {

    private final SwitchingWindowCalculator calculator = new SwitchingWindowCalculator();

    // ---- Estonia: 1st of next month, unless within the last 7 days before it ----

    @Test
    void eeWellWithinTheWindowGetsNextMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.EE, LocalDate.of(2026, 10, 1));
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void eeOnTheSevenDayCutoffStillGetsNextMonth() {
        // Nov 1 2026 minus 7 days = Oct 25 2026 - the cutoff itself still counts ("until").
        LocalDate result = calculator.earliestSwitchDate(CountryCode.EE, LocalDate.of(2026, 10, 25));
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void eeOneDayPastTheCutoffIsPushedAnotherMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.EE, LocalDate.of(2026, 10, 26));
        assertThat(result).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    // ---- Latvia: 1st of next month if signed by the 9th of the current month ----

    @Test
    void lvWellWithinTheWindowGetsNextMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LV, LocalDate.of(2026, 10, 1));
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void lvOnTheNinthStillGetsNextMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LV, LocalDate.of(2026, 10, 9));
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void lvOnTheTenthIsPushedAnotherMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LV, LocalDate.of(2026, 10, 10));
        assertThat(result).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    // ---- Lithuania: 1st of next month if signed by 3 working days before month-end ----
    // September 2026 ends on Wednesday the 30th; counting back 3 working days (Tue 29,
    // Mon 28, skip the weekend, Fri 25) lands the cutoff on Friday 25 September.

    @Test
    void ltWellWithinTheWindowGetsNextMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LT, LocalDate.of(2026, 9, 1));
        assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void ltOnTheWorkingDayCutoffStillGetsNextMonth() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LT, LocalDate.of(2026, 9, 25));
        assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void ltOnePastTheCutoffIsPushedAnotherMonth() {
        // 26 September 2026 is a Saturday - still counts as "past" the Friday cutoff
        // even though it isn't itself a working day.
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LT, LocalDate.of(2026, 9, 26));
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void ltCountsOnlyWeekdaysAsWorkingDaysWhenSteppingBackThroughAWeekend() {
        // Confirms the weekend (26-27 Sept) is skipped rather than counted: the Monday
        // 28th and Tuesday 29th are the two working days closest to month-end, and the
        // Friday before the weekend is the third.
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LT, LocalDate.of(2026, 9, 24));
        assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    // ---- Month-length edge case: computing "next month" must not be thrown off by
    // reference dates near the end of a long month rolling into a short one ----

    @Test
    void handlesReferenceDatesAcrossMonthLengthChanges() {
        LocalDate result = calculator.earliestSwitchDate(CountryCode.LV, LocalDate.of(2026, 1, 31));
        assertThat(result).isEqualTo(LocalDate.of(2026, 3, 1));
    }
}
