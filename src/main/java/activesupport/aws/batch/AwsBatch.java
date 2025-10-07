package activesupport.aws.batch;

import activesupport.system.out.Output;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;
import software.amazon.awssdk.services.batch.model.*;

import java.util.HashMap;
import java.util.Map;

import static activesupport.system.out.Output.printColoredLog;

public class AwsBatch {

    private static final String REGION = "eu-west-1";

    public enum JobQueue {
        DEFAULT("vol-app-%s-default"),
        LIQUIBASE("vol-app-%s-liquibase");

        private final String queueNameTemplate;

        JobQueue(String queueNameTemplate) {
            this.queueNameTemplate = queueNameTemplate;
        }

        public String resolve(String env) {
            if ("qa".equalsIgnoreCase(env)) {
                env = "int";
            }
            return String.format(this.queueNameTemplate, env);
        }

        public static JobQueue fromResolvedName(String resolvedName, String env) {
            for (JobQueue queue : JobQueue.values()) {
                if (queue.resolve(env).equalsIgnoreCase(resolvedName)) {
                    return queue;
                }
            }
            throw new IllegalArgumentException("No JobQueue found for resolved name: " + resolvedName);
        }
    }

    public boolean triggerAwsBatchJob(String jobDefinition) throws Exception {
        String jobId = triggerAwsBatchJobWithId(jobDefinition);
        return jobId != null && !jobId.isEmpty();
    }

    public String triggerAwsBatchJobWithId(String jobDefinition) throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        String env = System.getProperty("env", "default").toLowerCase();
        parameters.put("ENVIRONMENT_NAME", env);

        try {
            String jobName = jobDefinition + "-" + System.currentTimeMillis();
            String jobId = submitJob(AwsBatch.JobQueue.DEFAULT, JobDefinition.valueOf(jobDefinition), parameters, jobName);
            Output.printColoredLog("[INFO] AWS Batch job triggered successfully. Job ID: " + jobId);
            long timeout = System.currentTimeMillis() + 100000L;

            String jobStatus;
            do {
                jobStatus = String.valueOf(getJobStatus(jobId));
                Output.printColoredLog("[INFO] Job " + jobId + " status: " + jobStatus);
                if ("FAILED".equalsIgnoreCase(jobStatus)) {
                    throw new RuntimeException("Job failed. Status: " + jobStatus);
                }

                Thread.sleep(5000L);
            } while (!"SUCCEEDED".equalsIgnoreCase(jobStatus) && System.currentTimeMillis() < timeout);

            if (!"SUCCEEDED".equalsIgnoreCase(jobStatus)) {
                throw new RuntimeException("Job did not complete successfully within the timeout period.");
            }

            return jobId; // Return the job ID
        } catch (Exception e) {
            Output.printColoredLog("[ERROR] Failed to trigger AWS Batch job: " + e.getMessage());
            throw e;
        }
    }
    public static String submitJob(JobQueue jobQueue, JobDefinition jobDef, Map<String, String> params) throws Exception {
        String env = System.getProperty("env", "default").toLowerCase();
        String actualQueueName = jobQueue.resolve(env);

        try (BatchClient batchClient = BatchClient.builder()
                .region(Region.of(REGION))
                .build()) {

            SubmitJobRequest.Builder requestBuilder = SubmitJobRequest.builder()
                    .jobQueue(actualQueueName)
                    .jobDefinition(jobDef.toString())
                    .parameters(params);

            SubmitJobResponse response = batchClient.submitJob(requestBuilder.build());
            return response.jobId();
        }
    }

    public static String submitJob(JobQueue queue, JobDefinition definition, Map<String, String> parameters, String jobName) throws Exception {
        String env = System.getProperty("env", "default").toLowerCase();
        String actualQueueName = queue.resolve(env);

        try (BatchClient batchClient = BatchClient.builder()
                .region(Region.of(REGION))
                .build()) {


            SubmitJobRequest.Builder requestBuilder = SubmitJobRequest.builder()
                    .jobQueue(actualQueueName)
                    .jobDefinition(definition.toString())
                    .parameters(parameters)
                    .schedulingPriorityOverride(1)
                    .shareIdentifier("default");

            if (jobName != null && !jobName.isEmpty()) {
                requestBuilder.jobName(jobName);
            } else {
                requestBuilder.jobName(generateJobName(definition.toString()));
            }


            SubmitJobResponse response = batchClient.submitJob(requestBuilder.build());
            String jobId = response.jobId();

            System.out.println(printColoredLog("[INFO] Job submitted successfully. Job ID: " + jobId));


            monitorJob(batchClient, jobId);

            return jobId;
        }
    }

    private static String generateJobName(String jobDefinitionName) {
        return jobDefinitionName + "-" + System.currentTimeMillis();
    }

    public static JobStatus getJobStatus(String jobId) throws Exception {
        try (BatchClient batchClient = BatchClient.builder()
                .region(Region.of(REGION))
                .build()) {

            DescribeJobsRequest request = DescribeJobsRequest.builder()
                    .jobs(jobId)
                    .build();

            DescribeJobsResponse response = batchClient.describeJobs(request);

            if (!response.jobs().isEmpty()) {
                return response.jobs().get(0).status();
            } else {
                throw new Exception("Job not found: " + jobId);
            }
        }
    }

    private static void monitorJob(BatchClient batchClient, String jobId) {
        try {
            JobStatus currentStatus = null;
            int attempts = 0;
            int maxAttempts = 30;

            while (attempts < maxAttempts) {
                DescribeJobsRequest describeRequest = DescribeJobsRequest.builder()
                        .jobs(jobId)
                        .build();

                DescribeJobsResponse describeResponse = batchClient.describeJobs(describeRequest);

                if (!describeResponse.jobs().isEmpty()) {
                    JobDetail job = describeResponse.jobs().get(0);
                    JobStatus newStatus = job.status();

                    if (newStatus != currentStatus) {
                        currentStatus = newStatus;
                        System.out.println("[INFO] Job " + jobId + " status: " + currentStatus);

                        if (currentStatus == JobStatus.SUCCEEDED) {
                            System.out.println("[INFO] Job completed successfully!");
                            break;
                        } else if (currentStatus == JobStatus.FAILED) {
                            System.out.println("[ERROR] Job failed. Reason: " + job.statusReason());
                            break;
                        }
                    }
                }

                Thread.sleep(5000);
                attempts++;
            }

            if (attempts >= maxAttempts) {
                System.out.println("[WARN] Job monitoring timed out. Job may still be running.");
            }

        } catch (Exception e) {
            System.out.println("[WARN] Error monitoring job: " + e.getMessage());
        }
    }
}