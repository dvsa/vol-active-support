package activesupport.dates;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.LinkedHashMap;

public class Dates {

    private CalendarInterface calendar;

    public Dates(CalendarInterface calendar) {
        this.calendar = calendar;
    }

    public LinkedHashMap<String, String> getDateHashMap(int plusOrMinusDay, int plusOrMinusMonth, int plusOrMinusYear) {
        LocalDate localDate = calendar.now();

        localDate  = getLocalDate(plusOrMinusDay, plusOrMinusMonth, plusOrMinusYear, localDate);

        LinkedHashMap<String, String> getRelativeDate = new LinkedHashMap<>();
        {
            getRelativeDate.put("day", String.valueOf(localDate.getDayOfMonth()));
            getRelativeDate.put("month", String.valueOf(localDate.getMonthOfYear()));
            getRelativeDate.put("year", String.valueOf(localDate.getYear()));
        }
        return getRelativeDate;
    }

    @NotNull
    public String getFormattedDate(int plusOrMinusDay, int plusOrMinusMonth, int plusOrMinusYear, String pattern) {
        LocalDate date = calendar.now();
        DateTimeFormatter format = DateTimeFormat.forPattern(pattern);
        date = getLocalDate(plusOrMinusDay, plusOrMinusMonth, plusOrMinusYear, date);
        return date.toString(format);
    }

    @NotNull
    private LocalDate getLocalDate(int day, int month, int year, LocalDate localDate) {
        localDate = localDate.plusDays(day);
        localDate = localDate.plusMonths(month);
        localDate = localDate.plusYears(year);
        return localDate;
    }
}