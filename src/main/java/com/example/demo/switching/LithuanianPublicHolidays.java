package com.example.demo.switching;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Lithuania's statutory public holidays - fixed dates plus the two Easter-linked ones,
 * verified against publicholidays.lt for 2026 (which also confirms Christmas Eve, 24
 * December, is an official holiday there, not just the 25th/26th). Used only by {@link
 * SwitchingWindowCalculator}'s "3 working days before month-end" rule for Lithuania -
 * EE's and LV's rules don't reference working days at all, so neither needs this.
 * <p>
 * Deliberately self-contained rather than a third-party library or a live API: the
 * holiday <em>set</em> barely changes year to year, so a small, directly-testable rule
 * (fixed dates + a standard Easter algorithm) is more trustworthy for a
 * legally-significant date calculation than depending on someone else's data staying
 * current, and it avoids adding network I/O to what's otherwise an instant,
 * zero-dependency calculation. Revisit if Lithuania's parliament changes the list.
 */
final class LithuanianPublicHolidays {

    private static final Set<MonthDay> FIXED_DATE_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),    // New Year's Day
            MonthDay.of(2, 16),   // Day of Restoration of the State of Lithuania
            MonthDay.of(3, 11),   // Day of Restoration of Independence of Lithuania
            MonthDay.of(5, 1),    // International Workers' Day
            MonthDay.of(6, 24),   // St John's Day / Midsummer (Joninės/Rasos)
            MonthDay.of(7, 6),    // Statehood Day (King Mindaugas' Coronation Day)
            MonthDay.of(8, 15),   // Assumption Day
            MonthDay.of(11, 1),   // All Saints' Day
            MonthDay.of(11, 2),   // All Souls' Day
            MonthDay.of(12, 24),  // Christmas Eve
            MonthDay.of(12, 25),  // Christmas Day
            MonthDay.of(12, 26)   // 2nd Day of Christmas
    );

    private LithuanianPublicHolidays() {
    }

    static boolean isPublicHoliday(LocalDate date) {
        if (FIXED_DATE_HOLIDAYS.contains(MonthDay.from(date))) {
            return true;
        }
        LocalDate easterSunday = easterSunday(date.getYear());
        return date.equals(easterSunday) || date.equals(easterSunday.plusDays(1));
    }

    /**
     * Anonymous Gregorian algorithm (a.k.a. Meeus/Jones/Butcher) for the date of Easter
     * Sunday in the Gregorian calendar - cross-checked against the known real dates for
     * 2026 (5 April) through 2030 (21 April) before relying on it here.
     */
    private static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
