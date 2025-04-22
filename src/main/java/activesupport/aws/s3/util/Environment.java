package activesupport.aws.s3.util;

import activesupport.system.out.Output;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

public class Environment {

    public static EnvironmentType enumType(@NotNull String env){
        EnvironmentType envEnum = null;
        switch(env){
            case "qa":
                envEnum = EnvironmentType.QUALITY_ASSURANCE;
                break;
            case "dev":
                envEnum = EnvironmentType.DEVELOP;
                break;
            case "da":
                envEnum = EnvironmentType.DAILY_ASSURANCE;
                break;
            case "reg":
                envEnum = EnvironmentType.REGRESSION;
                break;
            case "prep":
            case "pp":
            case "pre":
                envEnum = EnvironmentType.PREPRODUCTION;
                break;
            case "prod":
                envEnum = EnvironmentType.PRODUCTION;
                break;
            default:
                throw new IllegalArgumentException(Output.printColoredLog(String.format("[ERROR] %s does not match up to any environment")));
        }
        return envEnum;
    }

    public static String name(@NotNull EnvironmentType env){
        String name;
        switch(env){
            case QUALITY_ASSURANCE:
                name = "qa";
                break;
            case DEVELOP:
                name = "dev";
                break;
            case DAILY_ASSURANCE:
                name = "da";
                break;
            case REGRESSION:
                name = "reg";
                break;
            case PREPRODUCTION:
                name = "prep";
                break;
            case PRODUCTION:
                name = "prod";
                break;
            default:
                String errorMessage = String.format(
                        "[ERROR] %s is not a supported %s%s",
                        env,
                        Output.colorText("ENVIRONMENT", Ansi.Color.RED),
                        Output.colorText("!!!", Ansi.Color.YELLOW)
                );

                throw new IllegalArgumentException(Output.printColoredLog(errorMessage));
        }
        return name;
    }

}
