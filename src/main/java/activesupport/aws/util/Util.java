package activesupport.aws.util;

import activesupport.MissingRequiredArgument;
import activesupport.aws.FolderType;
import activesupport.system.Properties;
import activesupport.system.out.Output;
import org.jetbrains.annotations.NotNull;

public class Util {

   public static String s3RetrieveObject(@NotNull String object, @NotNull String emailSubject) {
       String S3SanitiseObjectName = object.replaceAll("[@\\._-]", "");
       return S3SanitiseObjectName + emailSubject;
   }
    public static String s3ResetPassword(@NotNull String emailSubject) {
        String S3SanitiseObjectName = emailSubject.replaceAll("[@\\._-]", "");
        return S3SanitiseObjectName + emailSubject;
    }

    public static String s3Directory(@NotNull FolderType folderType) throws MissingRequiredArgument {
        return s3Path(folderType);
    }

    public static String s3Path(@NotNull FolderType folderType) throws MissingRequiredArgument {
        return s3Path("", folderType);
    }

    public static String s3Path(@NotNull String S3ObjectName) throws MissingRequiredArgument {
        FolderType folderType = FolderType.EMAIL;
        return s3Path(S3ObjectName, folderType);
    }

    public static String s3Path(@NotNull String S3ObjectName, @NotNull FolderType folderType) throws MissingRequiredArgument {
        String env = Properties.get("env", true);

        if(env.isEmpty()) {
            throw new IllegalArgumentException(
                    Output.printColoredLog("[ERROR] SYSTEM PROPERTY: env is not set")
            );
        }
        return String.format("%s.olcs.dev-dvsacloud.uk/%s/%s", env, folderType.toString(), S3ObjectName);
    }

    }


