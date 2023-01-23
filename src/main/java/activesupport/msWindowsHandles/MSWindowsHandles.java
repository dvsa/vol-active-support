package activesupport.msWindowsHandles;

import java.io.NotActiveException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;

public class MSWindowsHandles {

    public interface User32 extends W32APIOptions {

        static int SW_SHOW_MAXIMIZE = 3;
        User32 instance = Native.load("user32", User32.class, DEFAULT_OPTIONS);

        boolean ShowWindow(WinDef.HWND hWnd, int nCmdShow);
        boolean SetForegroundWindow(WinDef.HWND hWnd);
        WinDef.HWND FindWindow(String winClass,String title);

    }

    public static void focusWindows(String applicationClassName, String title) throws NotActiveException {
        User32 user32 = User32.instance;
        WinDef.HWND hWnd = user32.FindWindow(applicationClassName, title);
        if(hWnd != null){
            user32.ShowWindow(hWnd, User32.SW_SHOW_MAXIMIZE);
            user32.SetForegroundWindow(hWnd);
        } else {
            throw new NotActiveException("Window not found.");
        }
    }
}
