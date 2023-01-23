package activesupport.system;

import activesupport.MissingRequiredArgument;
import activesupport.file.Files;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertiesTest {

    @Test
    public void savesAndReadsPropertiesFromFile() throws IOException {
        String pathToProps = "properties/test.properties";
        String expectedProperty = "TestKey";
        String expectedValue = "TestValue";

        // Arrange
        Properties.writeProperty(expectedProperty, expectedValue, pathToProps);

        // Act
        Properties.loadProperties(pathToProps);

        // Assert
        String actualPropertyValue = System.getProperty(expectedProperty);
        assertEquals(expectedValue, actualPropertyValue);

        // Cleanup
        Files.deleteFolderAndItsContent(Paths.get(pathToProps).getParent());
    }

    @Test
    public void addsPropertiesToSystemProperties(){
        // Arrange
        String expectedProperty = "testProperty";
        String expectedPropertyValue = "testPropertyValue";

        // Act
        Properties.set(expectedProperty, expectedPropertyValue);

        // Assert
        assertEquals(expectedPropertyValue, System.getProperty(expectedProperty));
    }

    @Test
    public void retrievesPropertyValuesFromSystemProperties() throws MissingRequiredArgument {
        // Arrange
        String expectedProperty = "testProperty";
        String expectedPropertyValue = "testPropertyValue";
        Properties.set(expectedProperty, expectedPropertyValue);

        // Act
        String actualPropertyValue = Properties.get(expectedProperty);

        // Assert
        assertEquals(expectedPropertyValue, actualPropertyValue);
    }

}
