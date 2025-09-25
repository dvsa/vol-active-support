package activesupport.faker;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FakerUtilsTest {

    @Test
    void testGenerateFirstName() {
        FakerUtils fakerUtils = new FakerUtils();
        String firstName = fakerUtils.generateFirstName();
        assertNotNull(firstName, "First name should not be null");
        assertFalse(firstName.isEmpty(), "First name should not be empty");
    }

    @Test
    void testGenerateLastName() {
        FakerUtils fakerUtils = new FakerUtils();
        String lastName = fakerUtils.generateLastName();
        assertNotNull(lastName, "Last name should not be null");
        assertFalse(lastName.isEmpty(), "Last name should not be empty");
    }

    @Test
    void testGenerateAddress() {
        FakerUtils fakerUtils = new FakerUtils();
        LinkedHashMap<String, String> address = fakerUtils.generateAddress();
        assertNotNull(address, "Address should not be null");
        assertEquals(5, address.size(), "Address should have 5 fields");
        assertTrue(address.containsKey("addressLine1"), "Address should contain addressLine1");
        assertTrue(address.containsKey("addressLine2"), "Address should contain addressLine2");
        assertTrue(address.containsKey("addressLine3"), "Address should contain addressLine3");
        assertTrue(address.containsKey("addressLine4"), "Address should contain addressLine4");
        assertTrue(address.containsKey("town"), "Address should contain town");
    }

    @Test
    void testGenerateCompanyName() {
        FakerUtils fakerUtils = new FakerUtils();
        String companyName = fakerUtils.generateCompanyName();
        assertNotNull(companyName, "Company name should not be null");
        assertFalse(companyName.isEmpty(), "Company name should not be empty");
    }

    @Test
    void testGenerateUniqueId() {
        FakerUtils fakerUtils = new FakerUtils();
        String uniqueId = fakerUtils.generateUniqueId(16);
        assertNotNull(uniqueId, "Unique ID should not be null");
        assertEquals(16, uniqueId.length(), "Unique ID should have the specified length");
    }

    @Test
    void testGenerateNatureOfBusiness() {
        FakerUtils fakerUtils = new FakerUtils();
        String natureOfBusiness = fakerUtils.generateNatureOfBusiness();
        assertNotNull(natureOfBusiness, "Nature of business should not be null");
        assertFalse(natureOfBusiness.isEmpty(), "Nature of business should not be empty");
    }

    @Test
    void testLetterify() {
        FakerUtils fakerUtils = new FakerUtils();
        String result = fakerUtils.letterify("????");
        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.length(), "Result should have the same length as the input");
        assertTrue(result.matches("[a-z]{4}"), "Result should contain only lowercase letters");
    }

    @Test
    void testNumerify() {
        FakerUtils fakerUtils = new FakerUtils();
        String result = fakerUtils.numerify("####");
        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.length(), "Result should have the same length as the input");
        assertTrue(result.matches("\\d{4}"), "Result should contain only digits");
    }

    @Test
    void testBothify() {
        FakerUtils fakerUtils = new FakerUtils();
        String result = fakerUtils.bothify("##??");
        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.length(), "Result should have the same length as the input");
        assertTrue(result.matches("\\d{2}[a-z]{2}"), "Result should contain digits followed by letters");
    }

    @Test
    void testGetRandomRealUKPostcode() {
        FakerUtils fakerUtils = new FakerUtils();
        String postcode = fakerUtils.getRandomRealUKPostcode();
        assertNotNull(postcode, "Postcode should not be null");
        assertFalse(postcode.isEmpty(), "Postcode should not be empty");
    }

    @Test
    void testGenerateNorthEastEnglandAddress() {
        FakerUtils fakerUtils = new FakerUtils();
        LinkedHashMap<String, String> address = fakerUtils.generateNorthEastEnglandAddress();
        Map<String, String[]> cityToPostcodeMap = Map.of(
                "Newcastle", new String[]{"NE1", "NE2", "NE3"},
                "Sunderland", new String[]{"SR1", "SR2"},
                "Durham", new String[]{"DH1", "DH2"},
                "Gateshead", new String[]{"NE8", "NE9"},
                "Middlesbrough", new String[]{"TS1", "TS2"}
        );
        String city = address.get("addressLine3");
        String postcode = address.get("postcode");

        assertTrue(cityToPostcodeMap.containsKey(city), "City is not valid for North East England");
        assertTrue(
                cityToPostcodeMap.get(city) != null &&
                        cityToPostcodeMap.get(city).length > 0 &&
                        postcode.startsWith(cityToPostcodeMap.get(city)[0].substring(0, 2)),
                "Postcode does not match the city"
        );
    }
}