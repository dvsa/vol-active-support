package activesupport.faker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class FakerUtilsTest {

    private static final FakerUtils sut = new FakerUtils();

    private final String[] addressKeys = new String[] {"addressLine1", "addressLine2", "addressLine3", "addressLine4", "town"};

    private static LinkedHashMap<String, String> address;
    private static String firstName;
    private static String lastName;
    private static String companyName;
    private static final int idSize = 24;
    private static final int underMinimumIdSize = 12;
    private static int minimumIdSize = 16;
    private static String uniqueId;
    private static String uniqueIdMinimum;
    private static String natureOfBusiness;
    private static String realPostcode;


    @BeforeAll
    public static void setUp() {
        firstName = sut.generateFirstName();
        lastName = sut.generateLastName();
        address = sut.generateAddress();
        companyName = sut.generateCompanyName();
        uniqueId = sut.generateUniqueId(idSize);
        uniqueIdMinimum = sut.generateUniqueId(underMinimumIdSize);
        natureOfBusiness = sut.generateNatureOfBusiness();
        realPostcode = sut.getRandomRealUKPostcode();
    }

    @Test
    public void firstNameIsTypeString() {
        assertEquals(firstName.getClass(), String.class);
    }

    @Test
    public void firstNameIsNotEmptyString() {
        assertTrue(firstName.length() > 0);
    }

    @Test
    public void firstNameHasNoInvalidCharacters() {
        String invalidCharacters = ".'ìí-";
        for (int i = 0; i < invalidCharacters.length(); i++) {
            assertFalse(firstName.contains(String.valueOf(invalidCharacters.charAt(i))));
        }
    }

    @Test
    public void lastNameIsTypeString() {
        assertEquals(lastName.getClass(), String.class);
    }

    @Test
    public void lastNameIsNotEmptyString() {
            assertTrue(lastName.length() > 0);
    }

    @Test
    public void lastNameHasNoInvalidCharacters() {
        String invalidCharacters = ".'ìí-";
        for (int i = 0; i < invalidCharacters.length(); i++) {
            assertFalse(lastName.contains(String.valueOf(invalidCharacters.charAt(i))));
        }
    }

    @Test
    public void addressIsTypeString() {
        for (int i = 0; i < addressKeys.length; i++) {
            assertEquals(address.get(addressKeys[i]).getClass(), String.class);
        }
    }

    @Test
    public void AddressIsNotEmptyString() {
        for (int i = 0; i < addressKeys.length; i++) {
            assertTrue(address.get(addressKeys[i]).length() > 0);
        }
    }

    @Test
    public void AddressTownIsMaximum30InLength() {
        assertTrue(address.get("town").length() < 30);
    }

    @Test
    public void CompanyNameIsTypeString() {
        assertEquals(companyName.getClass(), String.class);
    }

    @Test
    public void CompanyNameIsNotEmptyString() {
        assertTrue(companyName.length() > 0);
    }

    @Test
    public void UniqueIdIsTypeString() {
        assertEquals(uniqueId.getClass(), String.class);
    }

    @Test
    public void UniqueIdIsNotEmptyString() {
        assertTrue(uniqueId.length() > 0);
    }

    @Test
    public void UniqueIdIsCorrectSize() {
        assertEquals(idSize, uniqueId.length());
    }

    @Test
    public void UniqueIdIsMinimum16Size() {
        assertEquals(minimumIdSize, uniqueIdMinimum.length());
    }

    @Test
    public void NatureOfBusinessIsTypeString() {
        assertEquals(natureOfBusiness.getClass(), String.class);
    }

    @Test
    public void NatureOfBusinessIsNotEmptyString() {
        assertTrue(natureOfBusiness.length() > 0);
    }

    @Test
    public void RealPostcodeIsTypeString() {
        assertEquals(realPostcode.getClass(), String.class);
    }

    @Test
    public  void RealPostcodeIsNotEmptyString() {
        assertTrue(realPostcode.length() > 0);
    }
}