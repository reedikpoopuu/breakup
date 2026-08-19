package com.example.demo.switching;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LithuanianPublicHolidaysTest {

    @Test
    void recognizesEveryFixedDateHoliday() {
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 2, 16))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 3, 11))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 5, 1))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 6, 24))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 7, 6))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 12, 24))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 12, 26))).isTrue();
    }

    @Test
    void recognizesEasterSundayAndMondayAcrossSeveralYears() {
        // Verified against publicholidays.lt / independent cross-check before relying on
        // the algorithm - these are the real dates, not values back-computed from it.
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 4, 5))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 4, 6))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2027, 3, 28))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2027, 3, 29))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2029, 4, 1))).isTrue();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2029, 4, 2))).isTrue();
    }

    @Test
    void doesNotFlagAnOrdinaryWeekdayOrTheDayAfterEasterMonday() {
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 4, 7))).isFalse();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 9, 15))).isFalse();
        assertThat(LithuanianPublicHolidays.isPublicHoliday(LocalDate.of(2026, 12, 27))).isFalse();
    }
}
