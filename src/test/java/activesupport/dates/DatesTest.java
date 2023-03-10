package activesupport.dates;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DatesTest {

    private static Dates sut;
    private static CalendarInterface calendar;

    @BeforeAll
    public static void setup() {
        calendar = mock(CalendarInterface.class);
        sut = new Dates(calendar);
    }

    @Test
    public void getRelativeDateTest() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(0,0,0);
        assertEquals("22", date.get("day"));
        assertEquals("11", date.get("month"));
        assertEquals("2019", date.get("year"));
    }

    @Test
    public void getPastDay() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(-23,0,0);
        assertEquals("30", date.get("day"));
        assertEquals("10", date.get("month"));
        assertEquals("2019", date.get("year"));
    }

    @Test
    public void getFutureDay() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(9,0,0);
        assertEquals("1", date.get("day"));
        assertEquals("12", date.get("month"));
        assertEquals("2019", date.get("year"));
    }

    @Test
    public void getPastMonth() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(0,-11,0);
        assertEquals("22", date.get("day"));
        assertEquals("12", date.get("month"));
        assertEquals("2018", date.get("year"));
    }

    @Test
    public void getFutureMonth() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(0,-10,0);
        assertEquals("22", date.get("day"));
        assertEquals("1", date.get("month"));
        assertEquals("2019", date.get("year"));
    }

    @Test
    public void getPastYear() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(0,0,-5);
        assertEquals("22", date.get("day"));
        assertEquals("11", date.get("month"));
        assertEquals("2014", date.get("year"));
    }


    @Test
    public void getFutureYear() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        HashMap<String, String> date = sut.getDateHashMap(0,0,6);
        assertEquals("22", date.get("day"));
        assertEquals("11", date.get("month"));
        assertEquals("2025", date.get("year"));
    }

    @Test
    public void getFormatterDate() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(20,-5,10, "yyyy-MM-dd");
        assertEquals("2029-07-12", date);
    }

    @Test
    public void getFormatterPastDay() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(-20,0,0, "yyyy-MM-dd");
        assertEquals("2019-11-02", date);
    }

    @Test
    public void getFormatterFutureDay() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(10,0,0, "yyyy-MM-dd");
        assertEquals("2019-12-02", date);
    }


    @Test
    public void getFormatterPastMonth() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(0,5,0, "yyyy-MM-dd");
        assertEquals("2020-04-22", date);
    }


    @Test
    public void getFormatterFutureMonth() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(0,-5,0, "yyyy-MM-dd");
        assertEquals("2019-06-22", date);
    }


    @Test
    public void getFormatterPastYear() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(0,0,-24, "yyyy-MM-dd");
        assertEquals("1995-11-22", date);
    }

    @Test
    public void getFormatterFutureYear() {
        when(calendar.now()).thenReturn(LocalDate.parse("2019-11-22"));

        String date = sut.getFormattedDate(0,0,35, "yyyy-MM-dd");
        assertEquals("2054-11-22", date);
    }
}