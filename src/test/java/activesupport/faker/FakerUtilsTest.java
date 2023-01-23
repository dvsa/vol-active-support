package activesupport.faker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

public class FakerUtilsTest {

    private FakerUtils sut = new FakerUtils();

    private String[] addressKeys = new String[] {"addressLine1", "addressLine2", "addressLine3", "addressLine4", "town"};

    private LinkedHashMap<String, String> address;
    private String firstName;
    private String lastName;
    private String companyName;
    private int idSize = 24;
    private int underMinimumIdSize = 12;
    private int mininumIdSize = 16;
    private String uniqueId;
    private String uniqueIdMinimum;
    private String natureOfBusiness;
    private String realPostcode;

    @Before
    public void generateFaker() {
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
        Assert.assertEquals(firstName.getClass(), String.class);
    }

    @Test
    public void firstNameIsNotEmptyString() {
        Assert.assertTrue(firstName.length() > 0);
    }

    @Test
    public void firstNameHasNoInvalidCharacters() {
        String invalidCharacters = ".\'ìí-";
        for (int i = 0; i < invalidCharacters.length(); i++) {
            Assert.assertFalse(firstName.contains(String.valueOf(invalidCharacters.charAt(i))));
        }
    }

    @Test
    public void lastNameIsTypeString() {
        Assert.assertEquals(lastName.getClass(), String.class);
    }

    @Test
    public void lastNameIsNotEmptyString() {
            Assert.assertTrue(lastName.length() > 0);
    }

    @Test
    public void lastNameHasNoInvalidCharacters() {
        String invalidCharacters = ".\'ìí-";
        for (int i = 0; i < invalidCharacters.length(); i++) {
            Assert.assertFalse(lastName.contains(String.valueOf(invalidCharacters.charAt(i))));
        }
    }

    @Test
    public void addressIsTypeString() {
        for (int i = 0; i < addressKeys.length; i++) {
            Assert.assertEquals(address.get(addressKeys[i]).getClass(), String.class);
        }
    }

    @Test
    public void AddressIsNotEmptyString() {
        for (int i = 0; i < addressKeys.length; i++) {
            Assert.assertTrue(address.get(addressKeys[i]).length() > 0);
        }
    }

    @Test
    public void AddressTownIsMaximum30InLength() {
        Assert.assertTrue(address.get("town").length() < 30);
    }

    @Test
    public void CompanyNameIsTypeString() {
        Assert.assertEquals(companyName.getClass(), String.class);
    }

    @Test
    public void CompanyNameIsNotEmptyString() {
        Assert.assertTrue(companyName.length() > 0);
    }

    @Test
    public void UniqueIdIsTypeString() {
        Assert.assertEquals(uniqueId.getClass(), String.class);
    }

    @Test
    public void UniqueIdIsNotEmptyString() {
        Assert.assertTrue(uniqueId.length() > 0);
    }

    @Test
    public void UniqueIdIsCorrectSize() {
        Assert.assertEquals(idSize, uniqueId.length());
    }

    @Test
    public void UniqueIdIsMinimum16Size() {
        Assert.assertEquals(mininumIdSize, uniqueIdMinimum.length());
    }

    @Test
    public void NatureOfBusinessIsTypeString() {
        Assert.assertEquals(natureOfBusiness.getClass(), String.class);
    }

    @Test
    public void NatureOfBusinessIsNotEmptyString() {
        Assert.assertTrue(natureOfBusiness.length() > 0);
    }

    @Test
    public void RealPostcodeIsTypeString() {
        Assert.assertEquals(realPostcode.getClass(), String.class);
    }

    @Test
    public  void RealPostcodeIsNotEmptyString() {
        Assert.assertTrue(realPostcode.length() > 0);
    }
}
