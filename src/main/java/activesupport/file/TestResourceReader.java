package activesupport.file;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class TestResourceReader {

    private Config testFile;

    public TestResourceReader() {
        this.testFile = ConfigFactory.defaultApplication();
    }

    public TestResourceReader(String filePath) throws FileNotFoundException {
        File tempFile = new File(String.format("%s/src/test/resources/%s", System.getProperty("user.dir"), filePath));
        if (tempFile.exists()){
            this.testFile = ConfigFactory.load(filePath);
        } else {
            throw new FileNotFoundException();
        }
    }

    public void setFile(String filePath) {
        this.testFile = ConfigFactory.load(filePath);
    }

    public Config getFile() {
        return testFile;
    }
}



