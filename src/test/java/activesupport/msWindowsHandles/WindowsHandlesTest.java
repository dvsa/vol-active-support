package activesupport.msWindowsHandles;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.NotActiveException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class WindowsHandlesTest {

    @Test
    public void msWindowsHandlesTest() throws IOException {
        if (System.getProperty("os.name").contains("Windows")) {
            Runtime.getRuntime().exec("Notepad.exe");
            try {
                MSWindowsHandles.focusWindows("Notepad", null);
            }catch (NotActiveException e){
                assertNotEquals("Window not found.", e.getMessage());
            }

        }
    }
}