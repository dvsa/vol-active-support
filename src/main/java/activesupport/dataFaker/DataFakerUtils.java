package activesupport.dataFaker;

import activesupport.http.RestUtils;
import net.datafaker.Faker;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.IntStream;

public class DataFakerUtils {

    private final Faker faker = new Faker(new Locale("en-GB"));

    private record FullName(String firstName, String lastName) {}
    private record Address(String addressLine1, String addressLine2, String addressLine3, String addressLine4, String town) {}

    private FullName generateFullName() {
        return switch ((int) (Math.random() * 8)) {
            case 0 -> new FullName(
                    faker.elderScrolls().firstName().toLowerCase(),
                    faker.elderScrolls().lastName().toLowerCase()
            );
            case 1 -> extractFullName(faker.friends().character());
            case 2 -> extractFullName(faker.funnyName().name());
            case 3 -> extractFullName(faker.harryPotter().character());
            case 4 -> extractFullName(faker.howIMetYourMother().character());
            case 5 -> extractFullName(faker.lordOfTheRings().character());
            case 6 -> extractFullName(faker.rickAndMorty().character());
            case 7 -> extractFullName(faker.witcher().character());
            default -> throw new IllegalStateException("Unexpected value");
        };
    }

    private FullName extractFullName(String fullName) {
        var parts = fullName.toLowerCase().split(" ");
        if (parts.length != 2 || fullName.replaceAll("[.'\\-]", "").matches(".*[^a-z ].*")) {
            return generateFullName();
        }
        return new FullName(parts[0], parts[1]);
    }

    public LinkedHashMap<String, String> getFullNameMap() {
        var fullName = generateFullName();
        var hashName = new LinkedHashMap<String, String>();
        hashName.put("firstName", fullName.firstName());
        hashName.put("lastName", fullName.lastName());
        return hashName;
    }

    public String generateFirstName() {
        return generateFullName().firstName();
    }

    public String generateLastName() {
        return generateFullName().lastName();
    }

    /**
     * Generate address generates the following address lines: Address Line 1, Address Line 2, Address Line 3, Address Line 4, Town.
     * Function then uses a hashmap to call the values using the following keys: addressLine1, addressLine2, addressLine3, addressLine4 and town.
     * @return hash map of addresses.
     */

    public LinkedHashMap<String, String> generateAddress() {
        var address = new Address(
                faker.address().streetAddressNumber(),
                faker.address().streetName(),
                faker.address().cityName(),
                faker.address().cityPrefix() + " " + faker.address().city(),
                faker.address().country()
        );

        var town = address.town().length() > 30 ? address.town().substring(0, 30) : address.town();

        var addressMap = new LinkedHashMap<String, String>();
        addressMap.put("addressLine1", address.addressLine1());
        addressMap.put("addressLine2", address.addressLine2());
        addressMap.put("addressLine3", address.addressLine3());
        addressMap.put("addressLine4", address.addressLine4());
        addressMap.put("town", town);

        return addressMap;
    }

    public String generateCompanyName() {
        return faker.funnyName().name() + ", " + faker.company().name() + " " + faker.company().suffix();
    }

    /**
     * generateUniqueId returns an id containing a random mix of lowercase and uppercase letters, numbers and special characters
     * with a minimum id size of 16 characters for security.
     * @param sizeMin16
     * @return uniqueId
     */

    public String generateUniqueId(int sizeMin16) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*";
        return IntStream.range(0, Math.max(16, sizeMin16))
                .mapToObj(i -> String.valueOf(characters.charAt(faker.random().nextInt(characters.length()))))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public String generateNatureOfBusiness() {
        return faker.company().bs() + " in " + faker.job().field();
    }

    public String letterify(String regex) {
        return faker.letterify(regex);
    }

    public String numerify(String regex) {
        return faker.numerify(regex);
    }

    public String bothify(String regex) {
        return faker.bothify(regex);
    }
    public String getRandomRealUKPostcode() {
        var headers = new HashMap<String, String>();
        return RestUtils.get("http://api.postcodes.io/random/postcodes", headers)
                .extract()
                .jsonPath()
                .get("result.postcode");
    }
}