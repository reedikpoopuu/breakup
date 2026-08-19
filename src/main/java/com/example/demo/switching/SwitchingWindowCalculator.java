package com.example.demo.switching;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The earliest date a new electricity contract can actually start, per each country's
 * supplier-switching rules - a deterministic function of (country, reference date), not
 * something derived from the old contract's expiry date (which is unrelated - see the
 * bug this replaces in {@code index.html}'s confirm-screen "earliest switch date"
 * field). Each country works the same way: switching by a cutoff date within the
 * current month gets you the 1st of next month; missing it pushes you one month
 * further out.
 * <ul>
 *   <li><b>EE</b>: 1st of next month, as long as you're on or before 7 days before
 *   that date; later than that and it's the 1st of the month after.</li>
 *   <li><b>LV</b>: 1st of next month if signed by the 9th of the current month;
 *   later than that and it's the 1st of the month after.</li>
 *   <li><b>LT</b>: 1st of next month if signed by 3 working days before the end of
 *   the current month; later than that and it's the 1st of the month after.</li>
 * </ul>
 * "Working days" for LT excludes both weekends and Lithuanian public holidays (see
 * {@link LithuanianPublicHolidays}) - added after finding a concrete, recurring case
 * this missed: in years where 31 December falls on a Monday, Tuesday, or Wednesday
 * (roughly 3 years in 7), the naive weekends-only count landed exactly on 26 December,
 * a real Lithuanian public holiday, silently telling a customer they'd made the
 * January-switch cutoff when the true legal answer was February. EE's and LV's rules
 * don't reference working days at all, so neither needed this.
 */
@Component
public class SwitchingWindowCalculator {

    private static final int LT_WORKING_DAYS_BEFORE_MONTH_END = 3;
    private static final int EE_DAYS_BEFORE_NEXT_MONTH = 7;
    private static final int LV_CUTOFF_DAY_OF_MONTH = 9;

    public LocalDate earliestSwitchDate(CountryCode country, LocalDate referenceDate) {
        LocalDate nextMonthStart = referenceDate.withDayOfMonth(1).plusMonths(1);
        boolean missedThisMonthsWindow = switch (country) {
            case EE -> referenceDate.isAfter(nextMonthStart.minusDays(EE_DAYS_BEFORE_NEXT_MONTH));
            case LV -> referenceDate.getDayOfMonth() > LV_CUTOFF_DAY_OF_MONTH;
            case LT -> referenceDate.isAfter(
                    workingDaysBefore(YearMonth.from(referenceDate).atEndOfMonth(), LT_WORKING_DAYS_BEFORE_MONTH_END));
        };
        return missedThisMonthsWindow ? nextMonthStart.plusMonths(1) : nextMonthStart;
    }

    /** Steps back from {@code date}, counting only Mon-Fri, until {@code workingDays} of them have been passed. */
    private static LocalDate workingDaysBefore(LocalDate date, int workingDays) {
        LocalDate cursor = date;
        int remaining = workingDays;
        while (remaining > 0) {
            cursor = cursor.minusDays(1);
            if (isWorkingDay(cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    private static boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                && !LithuanianPublicHolidays.isPublicHoliday(date);
    }
}
