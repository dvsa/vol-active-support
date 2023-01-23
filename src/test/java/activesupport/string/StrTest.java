package activesupport.string;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StrTest {

    @Test
    public void returnsTheMatchingSubstring(){
        String subject = "Today's date is 06-01-2018.";
        String regex = "[\\d\\-\\/]+";

        String expectedSubstring = "06-01-2018";
        String actualSubstring = Str.find(regex, subject).get();

        assertEquals(expectedSubstring, actualSubstring);
    }
}
