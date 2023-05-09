package activesupport.faker;

import activesupport.http.RestUtils;
import activesupport.number.Int;
import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;

public class FakerUtils {

    private final Faker faker = new Faker();
    private final FakeValuesService fakeValuesService = new FakeValuesService(
            new Locale("en-GB"), new RandomService());

    private LinkedHashMap<String, String> generateFullName() {
        var fakerLibraries = (int)(Math.floor(Math.random() * 7) + 1);
        String fullName = null;

        do {
            switch (fakerLibraries){
                case 1:
                    fullName = String.format("%s %s", faker.elderScrolls().firstName().toLowerCase(), faker.elderScrolls().lastName().toLowerCase());
                    break;
                case 2:
                    fullName = faker.friends().character().toLowerCase();
                    break;
                case 3:
                    fullName = faker.funnyName().name().toLowerCase();
                    break;
                case 4:
                    fullName = faker.harryPotter().character().toLowerCase();
                    break;
                case 5:
                    fullName = faker.howIMetYourMother().character().toLowerCase();
                    break;
                case 6:
                    fullName = faker.lordOfTheRings().character().toLowerCase();
                    break;
                case 7:
                    fullName = faker.rickAndMorty().character().toLowerCase();
                    break;
                case 8:
                    fullName = faker.witcher().character().toLowerCase();
                    break;
            }
        } while ( !Objects.requireNonNull(fullName).contains(" ") || fullName.split(" ").length > 2 || fullName.replaceAll("[.\'ìí-]", ".").contains("."));

        var splitName = fullName.split(" ");

        LinkedHashMap<String, String> hashName = new LinkedHashMap<>();
        hashName.put("firstName", splitName[0]);
        hashName.put("lastName", splitName[1]);

        return hashName;
    }

    public String generateFirstName() {
        return generateFullName().get("firstName");
    }

    public String generateLastName() {
        return generateFullName().get("lastName");
    }

    /**
     * Generate address generates the following address lines: Address Line 1, Address Line 2, Address Line 3, Address Line 4, Town.
     * Function then uses a hashmap to call the values using the following keys: addressLine1, addressLine2, addressLine3, addressLine4 and town.
     * @return hash map of addresses.
     */

    public LinkedHashMap<String, String> generateAddress() {
        LinkedHashMap<String, String> address = new LinkedHashMap<>();

        address.put("addressLine1", faker.address().streetAddressNumber());
        address.put("addressLine2", faker.address().streetName());
        address.put("addressLine3", faker.address().cityName());
        address.put("addressLine4", String.format("%s %s", faker.address().cityPrefix(), faker.address().city()));
        do {
            address.put("town", faker.address().country());
        } while (address.get("town").length() > 30);
        return address;
    }

    public String generateCompanyName() {
        return String.format("%s, %s %s", faker.funnyName().name(), faker.company().name(), faker.company().suffix());
    }

    /**
     * generateUniqueId returns an id containing a random mix of lowercase and uppercase letters, numbers and special characters
     * with a minimum id size of 16 characters for security.
     * @param sizeMin16
     * @return uniqueId
     */

    public String generateUniqueId(int sizeMin16) {
        var lowercaseLetters = "abcdefghijklmnopqrstuvwxyz";
        var uppercaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        var numbers = "0123456789";
        var specials = "!@£#$%^&*";
        var id = "";

        for (int i = 0; i < (i < 16 ? 16 : sizeMin16); i++) {
            int choice = Int.random(1, 62);
            id = id.concat(String.valueOf((lowercaseLetters + uppercaseLetters + numbers + specials).charAt(choice)));
        }
        return id;
    }

    public String generateNatureOfBusiness() {
        return String.format("%s in %s", faker.company().bs(), faker.job().field());
    }

    public String letterify(String regex){
        return fakeValuesService.letterify(regex);
    }

    public String numerify(String regex){
        return fakeValuesService.numerify(regex);
    }

    public String bothify(String regex){
        return fakeValuesService.bothify(regex);
    }

    public String getRandomRealUKPostcode() {
        HashMap<String, String> headers = new HashMap<>();
        return RestUtils.get("http://api.postcodes.io/random/postcodes", headers).extract().jsonPath().get("result.postcode");
    }
}