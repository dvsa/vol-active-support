package activesupport.string;

import activesupport.autoITX.bool.Bool;
import activesupport.number.Int;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Str {

    public static String inputStreamContents(@NotNull InputStream inputStream ){
        return new Scanner(inputStream).useDelimiter("\\A").next();
    }

    public static String randomNumbers(int length) {
        StringBuilder str = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            if (i == 0) {
                str.append(Int.random(1, 9));
            } else {
                str.append(Int.random(0, 9));
            }
        }

        return str.toString();
    }

    public static String randomWord(int minCharacters, int maxCharacters){
        int wordLength = Int.random(minCharacters, maxCharacters);

        return randomWord(wordLength);
    }

    public static String randomWord(int numberOfCharacters){
        StringBuilder word = new StringBuilder(numberOfCharacters);

        for(int i = 0; i < numberOfCharacters; i++){
            word.append(randomLetter());
        }

        return word.toString();
    }

    public static char randomLetter(){
        int lowercaseASCIICode = Int.random(97, 122);
        int uppercaseASCIICode = Int.random(65, 90);

        return Bool.random() ? (char) uppercaseASCIICode : (char) lowercaseASCIICode;
    }

    public static Optional<String> find(@NotNull String regex, @NotNull String subject){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(subject);

        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }
}