package activesupport.faker;

import activesupport.http.RestUtils;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;

public class FakerUtils {
    private final Random random = new Random();


    private static final String[] FIRST_NAMES = {
            "james", "john", "robert", "michael", "william", "david", "richard", "joseph", "thomas", "charles",
            "mary", "patricia", "jennifer", "linda", "elizabeth", "barbara", "susan", "jessica", "sarah", "karen"
    };

    private static final String[] LAST_NAMES = {
            "smith", "johnson", "williams", "brown", "jones", "garcia", "miller", "davis", "rodriguez", "martinez",
            "hernandez", "lopez", "gonzalez", "wilson", "anderson", "thomas", "taylor", "moore", "jackson", "martin"
    };

    private static final String[] STREET_NAMES = {
            "maple", "oak", "cedar", "pine", "elm", "washington", "lake", "hill", "forest", "river",
            "highland", "madison", "park", "main", "church", "north", "south", "east", "west"
    };

    private static final String[] CITIES = {
            "london", "manchester", "birmingham", "leeds", "glasgow", "liverpool", "bristol", "oxford", "cambridge", "cardiff",
            "edinburgh", "belfast", "nottingham", "sheffield", "leicester", "coventry", "bath", "york", "newcastle"
    };

    private static final String[] COUNTRIES = {
            "england", "scotland", "wales", "northern ireland", "ireland", "france", "germany", "spain", "italy", "netherlands"
    };

    private static final String[] COMPANY_TYPES = {
            "ltd", "limited", "llc", "incorporated", "corp", "holdings", "group", "solutions", "enterprises", "international"
    };

    private static final String[] BUSINESS_ACTIVITIES = {
            "software development", "consulting", "retail", "manufacturing", "healthcare", "education", "finance",
            "marketing", "logistics", "real estate"
    };

    private static final String[] BUSINESS_SECTORS = {
            "technology", "healthcare", "finance", "education", "retail", "manufacturing", "services",
            "construction", "agriculture", "energy"
    };

    private String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }

    private LinkedHashMap<String, String> generateFullName() {
        String firstName = getRandomElement(FIRST_NAMES);
        String lastName = getRandomElement(LAST_NAMES);

        LinkedHashMap<String, String> hashName = new LinkedHashMap<>();
        hashName.put("firstName", firstName);
        hashName.put("lastName", lastName);
        return hashName;
    }

    public String generateFirstName() {
        return generateFullName().get("firstName");
    }

    public String generateLastName() {
        return generateFullName().get("lastName");
    }

    public LinkedHashMap<String, String> generateAddress() {
        LinkedHashMap<String, String> address = new LinkedHashMap<>();

        address.put("addressLine1", String.valueOf(random.nextInt(300) + 1));
        address.put("addressLine2", getRandomElement(STREET_NAMES) + " " +
                (random.nextBoolean() ? "street" : "road"));
        address.put("addressLine3", getRandomElement(CITIES));
        address.put("addressLine4", getRandomElement(CITIES) + " district");
        address.put("town", getRandomElement(COUNTRIES));

        return address;
    }

    public LinkedHashMap<String, String> generateNorthEastEnglandAddress() {
        LinkedHashMap<String, String> address = new LinkedHashMap<>();

        HashMap<String, String[]> cityToPostcodeMap = new HashMap<>();
        cityToPostcodeMap.put("Newcastle", new String[]{"NE1", "NE2", "NE3"});
        cityToPostcodeMap.put("Sunderland", new String[]{"SR1", "SR2"});
        cityToPostcodeMap.put("Durham", new String[]{"DH1", "DH2"});
        cityToPostcodeMap.put("Gateshead", new String[]{"NE8", "NE9"});
        cityToPostcodeMap.put("Middlesbrough", new String[]{"TS1", "TS2"});

        String city = this.getRandomElement(cityToPostcodeMap.keySet().toArray(new String[0]));
        String postcodePrefix = this.getRandomElement(cityToPostcodeMap.get(city));

        address.put("addressLine1", String.valueOf(this.random.nextInt(300) + 1));
        String streetName = this.getRandomElement(STREET_NAMES);
        address.put("addressLine2", streetName + " " + (this.random.nextBoolean() ? "Street" : "Road"));
        address.put("addressLine3", city);
        address.put("addressLine4", city + " District");
        address.put("town", "England");
        address.put("postcode", String.format("%s %d%s",
                postcodePrefix,
                this.random.nextInt(10),
                this.letterify("?U")));

        return address;
    }

    public String generateCompanyName() {
        return String.format("%s %s %s",
                getRandomElement(LAST_NAMES),
                getRandomElement(BUSINESS_ACTIVITIES),
                getRandomElement(COMPANY_TYPES));
    }

    public String generateUniqueId(int sizeMin16) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@£#$%^&*";
        StringBuilder id = new StringBuilder();
        int size = Math.max(16, sizeMin16);

        for (int i = 0; i < size; i++) {
            id.append(chars.charAt(random.nextInt(chars.length())));
        }

        return id.toString();
    }

    public String generateNatureOfBusiness() {
        return String.format("%s in %s",
                getRandomElement(BUSINESS_ACTIVITIES),
                getRandomElement(BUSINESS_SECTORS));
    }

    public String letterify(String regex) {
        return regex.chars()
                .mapToObj(ch -> ch == '?' ?
                        String.valueOf((char)('a' + random.nextInt(26))) :
                        String.valueOf((char)ch))
                .reduce("", String::concat);
    }

    public String numerify(String regex) {
        return regex.chars()
                .mapToObj(ch -> ch == '#' ?
                        String.valueOf(random.nextInt(10)) :
                        String.valueOf((char)ch))
                .reduce("", String::concat);
    }

    public String bothify(String regex) {
        return numerify(letterify(regex));
    }

    public String getRandomRealUKPostcode() {
        HashMap<String, String> headers = new HashMap<>();
        return RestUtils.get("http://api.postcodes.io/random/postcodes", headers)
                .extract()
                .jsonPath()
                .get("result.postcode");
    }
}