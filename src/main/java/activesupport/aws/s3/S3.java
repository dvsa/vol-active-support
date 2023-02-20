package activesupport.aws.s3;

import activesupport.MissingRequiredArgument;
import activesupport.aws.s3.util.OurBuckets;
import activesupport.aws.s3.util.Util;
import activesupport.string.Str;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3 {

    private static AmazonS3 client = null;
    private static String s3BucketName = "devapp-olcs-pri-olcs-autotest-s3";

    private static String sesBucketName = "gov-uk-testing-ses-emails";

    private static String sesBucketPath = "gov_uk_testing_dev-dvsacloud_uk";


    public static String getLatestNIGVExportContents() throws IllegalAccessException, MissingRequiredArgument {
        String latestObjectName = getLatestNIExportName();
        return S3.getNIGVExport(latestObjectName).replaceAll(",,", ",null,");
    }

    public static String getLatestNIExportName() throws MissingRequiredArgument {
        return getLatestNIExportName(getObjectListing(Util.s3Path(FolderType.NI_EXPORT)));
    }

    public static String getLatestNIExportName(ObjectListing objectListing) {
        String objectMetadata = getLastObjectMetadata(objectListing);
        return Str.find("NiGvLicences-\\d{14}\\.csv", objectMetadata).get();
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

    public static String getSecrets() {
        S3Object s3Object = S3.getS3Object("devappci-shd-pri-qarepo", "secrets.json");
        return objectContents(s3Object);
    }

    public static String objectContents(@NotNull S3Object s3Object) {
        return Str.inputStreamContents(s3Object.getObjectContent());
    }

    /**
     * This extracts the temporary password out the emails stored in the S3 bucket.
     * The specific object that the password will be extracted out of if inferred from the emailAddress.
     *
     * @param emailAddress This is the email address used to create an account on external(self-serve).
     * @param S3BucketName This is the name of the S3 bucket.
     */
    public static String getTempPassword(@NotNull String emailAddress, @NotNull String S3BucketName) throws MissingRequiredArgument {
        String S3ObjectName = Util.s3RetrieveObject(emailAddress, "__Your_temporary_password");
        String S3Path = Util.s3Path(S3ObjectName);
        S3Object s3Object = S3.getS3Object(S3BucketName, S3Path);
        return extractTempPasswordFromS3Object(s3Object);
    }


    public static String getGovSignInCode(String sesBucketName, String sesBucketPath) throws MissingRequiredArgument {
        String lastModified = listObjectsByLastModified(sesBucketName, sesBucketPath);
        if (client().doesObjectExist(sesBucketName, lastModified)) {
            S3Object s3Object = client().getObject(sesBucketName, lastModified);
            return extractEmailCodeFromS3Object(s3Object);
        } else {
            return null;
        }
    }

    public static String getSignInCode() {
        return getGovSignInCode(sesBucketName, sesBucketPath);
    }


    /**
     * This extracts the temporary password out the emails stored in the S3 bucket.
     * The specific object that the password will be extracted out of if inferred from the emailAddress.
     *
     * @param emailAddress This is the email address used to create an account on external(self-serve).
     */
    public static String getTempPassword(@NotNull String emailAddress) throws MissingRequiredArgument {
        return getTempPassword("Reset_Your_Password", s3BucketName);
    }

    private static S3Object getTMLastLetterEmail(@NotNull String emailAddress) throws MissingRequiredArgument {
        String S3ObjectName = Util.s3RetrieveObject(emailAddress, "__Urgent_Removal_of_last_Transport_Manager");
        String stringCap = S3ObjectName.substring(0, Math.min(S3ObjectName.length(), 100));
        String S3Path = Util.s3Path(stringCap);
        return S3.getS3Object(s3BucketName, S3Path);
    }

    public static String getPasswordResetLink(@NotNull String emailAddress) throws MissingRequiredArgument {
        try {
            TimeUnit.SECONDS.sleep(1);
            String S3ObjectName = Util.s3RetrieveObject(emailAddress, "__Reset_your_password");
            String stringCap = S3ObjectName.substring(0, Math.min(S3ObjectName.length(), 100));
            String S3Path = Util.s3Path(stringCap);
            S3Object s3Object = S3.getS3Object(s3BucketName, S3Path);
            String s3ObjContents = (new Scanner(s3Object.getObjectContent())).useDelimiter("\\A").next();
            return s3ObjContents;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return emailAddress;
    }

    public static String getUsernameInfoLink(@NotNull String emailAddress) throws MissingRequiredArgument {
        String S3ObjectName = Util.s3RetrieveObject(emailAddress, "__Your_account_information");
        String stringCap = S3ObjectName.substring(0, Math.min(S3ObjectName.length(), 100));
        String s3Path = Util.s3Path(stringCap);
        S3Object s3Object = S3.getS3Object(s3BucketName, s3Path);
        return extractUsernameFromS3Object(s3Object);
    }

    public static boolean checkLastTMLetterAttachment(@NotNull String emailAddress, String licenceNo) throws MissingRequiredArgument {
        S3Object emailObject = getTMLastLetterEmail(emailAddress);
        String s3ObjContents = new Scanner(emailObject.getObjectContent()).useDelimiter("\\A").next();
        return s3ObjContents.contains(String.format("%s_Last_TM_letter_Licence_%s", licenceNo, licenceNo));
    }

    private static String extractTempPasswordFromS3Object(S3Object s3Object) {
        String s3ObjContents = new Scanner(s3Object.getObjectContent()).useDelimiter("\\A").next();
        Pattern pattern = Pattern.compile("[.\\w\\S]{0,30}(?==0ASign)");
        Matcher matcher = pattern.matcher(s3ObjContents);
        matcher.find();
        return matcher.group();
    }


    private static String extractEmailCodeFromS3Object(S3Object s3Object) {
        String s3ObjContents = new Scanner(s3Object.getObjectContent()).useDelimiter("\\A").next();
        Pattern pattern = Pattern.compile("[\\d]{6}(?= The code)");
        Matcher matcher = pattern.matcher(s3ObjContents);
        matcher.find();
        return matcher.group();
    }

    private static String extractUsernameFromS3Object(S3Object s3Object) {
        String s3ObjContents = new Scanner(s3Object.getObjectContent()).useDelimiter("\\A").next();
        Pattern pattern = Pattern.compile("[\\w\\S]{0,30}(?==0ASign)");
        Matcher matcher = pattern.matcher(s3ObjContents);
        matcher.find();
        String username = matcher.group();
        return username;
    }

    public static S3Object getS3Object(String s3BucketName, String s3Path) {
        return createS3Client().getObject(new GetObjectRequest(s3BucketName, s3Path));
    }

    public static String getEcmtCorrespondences(String email, String licenceNumber, String permitApplicationNumber) {
        return getEcmtCorrespondences(email, permitApplicationNumber);
    }

    public static String getEcmtCorrespondences(String email, String referenceNumber) {
        String sanitisedEmail = email.replaceAll("[@\\.]", "");
        String licenceNumber = Str.find("\\w{2}\\d{7}", referenceNumber).get();
        String permitNumber = Str.find("(?<=\\w{2}\\d{7} / )\\d+", referenceNumber).get();

        String objectKey = Util.s3Path(
                String.format(
                        "%s__ECMT_permit_application_response_reference_%s__%s",
                        sanitisedEmail,
                        licenceNumber,
                        permitNumber
                ),
                FolderType.EMAIL
        );

        return client().getObjectAsString(
                s3BucketName,
                objectKey
        );
    }

    public static AmazonS3 client() {
        return createS3Client();
    }

    public static AmazonS3 client(Regions region) {
        return createS3Client(region);
    }

    public static AmazonS3 createS3Client() {
        return createS3Client(Regions.EU_WEST_1);
    }

    public static AmazonS3 createS3Client(Regions region) {
        if (client == null) {
            client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .withRegion(region)
                    .build();
        }

        return client;
    }

    public static void deleteObject(String key) {
        deleteObject(OurBuckets.QA, key);
    }

    public static void deleteObject(String bucket, String key) {
        ObjectListing objectListing = client().listObjects(bucket, key);
        boolean hasNextList;

        do {
            for (S3ObjectSummary file : objectListing.getObjectSummaries()) {
                client().deleteObject(bucket, file.getKey());
            }

            hasNextList = hasNextObjectsList(objectListing);

            if (hasNextList)
                objectListing = client().listNextBatchOfObjects(objectListing);
        } while (hasNextList);
    }

    public static boolean any(String bucket, String key) {
        ObjectListing objectListing = client().listObjects(bucket, key);
        boolean hasNextList;
        boolean found = false;

        do {
            found = objectListing.getObjectSummaries().stream()
                    .anyMatch((object) -> object.getKey().toLowerCase().contains(key.toLowerCase()));

            if (found)
                return true;

            hasNextList = hasNextObjectsList(objectListing);

            if (hasNextList)
                objectListing = client().listNextBatchOfObjects(objectListing);
        } while (hasNextList);

        return false;
    }

    public static boolean hasNextObjectsList(ObjectListing objectListing) {
        return objectListing.isTruncated();
    }

    public static boolean objectExists(String objectPath) {
        return client().doesObjectExist(OurBuckets.QA, objectPath);
    }

    public static void uploadObject(String bucketName, String path, String fileName) {
        client().putObject(
                bucketName,
                path,
                new File(fileName)
        );
    }

    public static String listObjectsByLastModified(String bucketName, String path) {
        long kickOut = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < kickOut) {
        }
        ObjectListing objectListing = client().listObjects(bucketName, path);
        S3ObjectSummary latestObject = null;
        for (S3ObjectSummary os : objectListing.getObjectSummaries()) {
            if (latestObject == null || os.getLastModified().after(latestObject.getLastModified())) {
                latestObject = os;
            }
        }
        if (latestObject != null) {
            return latestObject.getKey();
        } else {
            return null;
        }
    }

    public static void downloadObject(String bucketName, String path, String fileName) {
        S3Object s3object = client().getObject(bucketName, path);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        try {
            FileUtils.copyInputStreamToFile(inputStream, new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
