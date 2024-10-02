package activesupport.dataFaker;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataFakerUtilsTest {

    private static final DataFakerUtils sut = new DataFakerUtils();

    private record TestData(
            LinkedHashMap<String, String> fullName,
            LinkedHashMap<String, String> address,
            String companyName,
            String uniqueId,
            String uniqueIdMinimum,
            String natureOfBusiness,
            String realPostcode
    ) {}

    private static final TestData testData = new TestData(
            sut.getFullNameMap(),
            sut.generateAddress(),
            sut.generateCompanyName(),
            sut.generateUniqueId(24),
            sut.generateUniqueId(12),
            sut.generateNatureOfBusiness(),
            sut.getRandomRealUKPostcode()
    );

    private static final List<String> addressKeys = List.of("addressLine1", "addressLine2", "addressLine3", "addressLine4", "town");

    @Test
    public void testFullName() {
        var fullName = testData.fullName();
        assertTrue(fullName.containsKey("firstName") && fullName.containsKey("lastName"), "Full name should contain first and last name");
        assertAll("Full name parts should be valid",
                () -> assertTrue(fullName.get("firstName").length() > 0, "First name should not be empty"),
                () -> assertTrue(fullName.get("lastName").length() > 0, "Last name should not be empty"),
                () -> assertFalse(fullName.get("firstName").matches(".*[.'ìí-].*"), "First name should not contain invalid characters"),
                () -> assertFalse(fullName.get("lastName").matches(".*[.'ìí-].*"), "Last name should not contain invalid characters")
        );
    }

    @Test
    public void testAddress() {
        var address = testData.address();
        assertAll("Address parts should be valid",
                () -> assertTrue(addressKeys.stream().allMatch(key -> address.get(key) instanceof String && !address.get(key).isEmpty()),
                        "All address parts should be non-empty strings"),
                () -> assertTrue(address.get("town").length() <= 30, "Town name should not exceed 30 characters")
        );
    }

    @Test
    public void testCompanyName() {
        assertTrue(testData.companyName().length() > 0, "Company name should not be empty");
    }

    @Test
    public void testUniqueId() {
        var validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*";
        assertAll("Unique ID should be valid",
                () -> assertEquals(24, testData.uniqueId().length(), "Unique ID should be of correct size"),
                () -> assertEquals(16, testData.uniqueIdMinimum().length(), "Minimum unique ID should be at least 16 characters"),
                () -> assertTrue(testData.uniqueId().chars().allMatch(ch -> validChars.indexOf(ch) != -1), "Unique ID should only contain valid characters")
        );
    }

    @Test
    public void testNatureOfBusiness() {
        assertTrue(testData.natureOfBusiness().length() > 0, "Nature of business should not be empty");
    }

    @Test
    public void testRealPostcode() {
        assertTrue(testData.realPostcode().length() > 0, "Real postcode should not be empty");
    }

    @Test
    public void testLetterify() {
        assertTrue(sut.letterify("????").matches("[a-zA-Z]{4}"), "Letterify should produce only letters");
    }

    @Test
    public void testNumerify() {
        assertTrue(sut.numerify("####").matches("\\d{4}"), "Numerify should produce only numbers");
    }

    @Test
    public void testBothify() {
        assertTrue(sut.bothify("##??").matches("\\d{2}[a-zA-Z]{2}"), "Bothify should produce numbers and letters");
    }
}