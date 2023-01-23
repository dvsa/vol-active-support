package activesupport.dates;

import org.joda.time.LocalDate;

public class LocalDateCalendar implements CalendarInterface {

    @Override
    public LocalDate now(){
        return LocalDate.now();
    }
}
