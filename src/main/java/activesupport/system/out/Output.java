package activesupport.system.out;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

public class Output {

    // TODO: Split into two methods as this method violates command query separation principle
    public static String printColoredLog(@NotNull String subject){

        String formattedSubject = colorText("[", DEFAULT)
                + colorText(extractStatementInformationType(subject), subjectColor(subject))
                + colorText("]", DEFAULT)
                + " "
                + colorText(extractStatementWithoutInformationType(subject), DEFAULT);

        System.out.println(formattedSubject);

        return formattedSubject;
    }

    public static String colorText(@NotNull String text, @NotNull Ansi.Color color){
        return ansi().fg(color).a(text).reset().toString();
    }

    private static String extractStatementInformationType(@NotNull String subject){
        Pattern pattern = Pattern.compile("(?<=\\[)\\w+", Pattern.CASE_INSENSITIVE);

        return extractMatch(subject, pattern);
    }

    private static String extractStatementWithoutInformationType(@NotNull String subject){
        Pattern pattern = Pattern.compile("(?<=\\] )[^\\[\\]]+", Pattern.CASE_INSENSITIVE);

        return extractMatch(subject, pattern);
    }

    private static String extractMatch(@NotNull String subject, @NotNull Pattern pattern){
        String result = subject;

        Matcher matcher = pattern.matcher(subject);
        if(matcher.find()){
            result = matcher.group();
        }

        return result;
    }

    private static Ansi.Color subjectColor(@NotNull String subject){
        subject = subject.toLowerCase();
        Ansi.Color color = Ansi.Color.DEFAULT;

        if(subject.startsWith("[error]")){
            color = RED;
        } else if(subject.startsWith("[info]")){
            color = Ansi.Color.BLUE;
        } else if(subject.startsWith("[pass]")){
            color = GREEN;
        } else if(subject.startsWith("[warning]")){
            color = DEFAULT;
        }

        return color;
    }

}
