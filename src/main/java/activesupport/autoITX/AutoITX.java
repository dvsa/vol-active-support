package activesupport.autoITX;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;

import autoitx4java.AutoItX;

import com.jacob.com.LibraryLoader;

public class AutoITX {

    public static void autoItJacobVersionControl(String jacobVersion, String libraryPath) throws MalformedURLException {
        String jacobDllVersionToUse;
        if (System.getProperty("sun.arch.data.model").contains("32")) {
            jacobDllVersionToUse = String.format("%s-x86.dll",jacobVersion);
        } else {
            jacobDllVersionToUse = String.format("%s-x64.dll",jacobVersion);
        }
        File file = new File(libraryPath, jacobDllVersionToUse);
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, file.getAbsolutePath());
    }

    public static AutoItX initiateAutoItX(String jacobVersion, String libraryPath) throws MalformedURLException, InterruptedException {
        autoItJacobVersionControl(jacobVersion, libraryPath);
        return new AutoItX();
    }
}
