package activesupport.faker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class FakerUtilsTest {

    private static final FakerUtils sut = new FakerUtils();

    private final String[] addressKeys = new String[] {"addressLine1", "addressLine2", "addressLine3", "addressLine4", "town"};

    private final LinkedHashMap<String, String> address;
    private final String firstName;
    private final String lastName;
    private final String companyName;
    private static final int idSize = 24;
    private static final int underMinimumIdSize = 12;
    private final int mininumIdSize = 16;
    private final String uniqueId;
    private final String uniqueIdMinimum;
    private final String natureOfBusiness;
    private final String realPostcode;


    public FakerUtilsTest() {
        this.firstName = sut.generateFirstName();
        this.lastName = sut.generateLastName();
        this.address = sut.generateAddress();
        this.companyName = sut.generateCompanyName();
        this.uniqueId = sut.generateUniqueId(idSize);
        this.uniqueIdMinimum = sut.generateUniqueId(underMinimumIdSize);
        this.natureOfBusiness = sut.generateNatureOfBusiness();
        this.realPostcode = sut.getRandomRealUKPostcode();
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
        assertEquals(mininumIdSize, uniqueIdMinimum.length());
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