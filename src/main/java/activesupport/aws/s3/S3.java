package activesupport.aws.s3;

import activesupport.MissingRequiredArgument;
import activesupport.aws.s3.util.Util;
import activesupport.string.Str;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3 {

    private static String s3BucketName = "devapp-olcs-pri-olcs-autotest-s3";

    public static String getLatestNIGVExportContents() throws IllegalAccessException, MissingRequiredArgument {
        String latestObjectName = getLatestNIExportName();
        return S3.getNIGVExport(latestObjectName).replaceAll(",,", ",null,");
    }

    public static String getLatestNIExportName() throws MissingRequiredArgument {
        return getLatestNIExportName(getObjectListing(Util.s3Path(FolderType.NI_EXPORT)));
    }

    public static String getLatestNIExportName(ObjectListing objectListing) {
        String objectMetadata = getLastObjectMetadata(objectListing);
        return Str.find("NiGvLicences-\\d{14}\\.csv", objectMetadata);
    }

    private static String getLastObjectMetadata(ObjectListing objectListing) {
        List<S3ObjectSummary> summaries = getS3ObjectSummaries(objectListing);
        return summaries.get(summaries.size() - 1).getKey();
    }

    private static List<S3ObjectSummary> getS3ObjectSummaries(ObjectListing objectListing) {
        ObjectListing objectList = objectListing;
        return objectList.getObjectSummaries();
    }

    private static ObjectListing getObjectListing(@NotNull String prefix) {
        ListObjectsRequest listObjectRequest = new ListObjectsRequest()
                .withBucketName(s3BucketName)
                .withPrefix(prefix);

        return S3.createS3Client().listObjects(listObjectRequest);
    }

    public static String getNIGVExport(@NotNull String S3ObjectName) throws MissingRequiredArgument {
        String S3Path = Util.s3Path(S3ObjectName, FolderType.NI_EXPORT);
        S3Object s3Object = S3.getS3Object(s3BucketName, S3Path);
        return objectContents(s3Object);
    }

    public static String objectContents(@NotNull S3Object s3Object){
        return Str.inputStreamContents(s3Object.getObjectContent());
    }

    /**
     * This extracts the temporary password out out the emails stored in the S3 bucket.
     * The specific object that the password will be extracted out of if inferred from the emailAddress.
     * @param emailAddress This is the email address used to create an account on external(self-serve).
     * @param S3BucketName This is the name of the S3 bucket.
     * */
    public static String getTempPassword(@NotNull String emailAddress, @NotNull String S3BucketName) throws MissingRequiredArgument {
        String S3ObjectName = Util.s3TempPasswordObjectName(emailAddress);
        String S3Path = Util.s3Path(S3ObjectName);
        S3Object s3Object = S3.getS3Object(S3BucketName, S3Path);
        return extractTempPasswordFromS3Object(s3Object);
    }

    /**
     * This extracts the temporary password out out the emails stored in the S3 bucket.
     * The specific object that the password will be extracted out of if inferred from the emailAddress.
     * @param emailAddress This is the email address used to create an account on external(self-serve).
     * */
    public static String getTempPassword(@NotNull String emailAddress) throws MissingRequiredArgument {
        return getTempPassword(emailAddress, s3BucketName);
    }

    private static String extractTempPasswordFromS3Object(S3Object s3Object) {
        String s3ObjContents = new Scanner(s3Object.getObjectContent()).useDelimiter("\\A").next();
        Pattern pattern = Pattern.compile("[\\w]{6,20}(?==0ASign in at)");
        Matcher matcher = pattern.matcher(s3ObjContents);
        matcher.find();
        String tempPassword = matcher.group();
        return tempPassword;

    }

    private static S3Object getS3Object(String s3BucketName, String s3Path) {
        return createS3Client().getObject(new GetObjectRequest(s3BucketName, s3Path));
    }

    private static AmazonS3 createS3Client(){
        String region = "eu-west-1";
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(region)
                .build();

        return s3;
    }
}
