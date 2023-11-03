package activesupport.jenkins;

import activesupport.jenkins.exceptions.JenkinsBuildFailed;
import activesupport.system.Properties;
import activesupport.system.out.Output;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.helper.Range;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jenkins {

    public static final String Jenkins_Url = "https://jenkins.olcs.dev-dvsacloud.uk/";

    public enum Job {
        NI_EXPORT("Batch/Batch_data-gov-NI-export"),
        BATCH_PROCESS_QUEUE("Batch/Batch_Process_Queue"),
        BATCH_RUN_CLI("Batch/job/Batch_Run_Cli"),
        PROCESS_QUEUE("Batch/Process_Queue");

        private final String name;

        Job(@NotNull String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static void trigger(@NotNull Job job, @NotNull HashMap<String, String> params) throws Exception {
        String username = Properties.get("JENKINS_USERNAME", true);
        String password = Properties.get("JENKINS_PASSWORD", true);


        JenkinsServer batchProcessJobs = new JenkinsServer(new URI(Jenkins_Url), username, password);

        JobWithDetails details = batchProcessJobs.getJob(job.toString());
//        JobWithDetails details = batchProcessJobs.getJob(job.toString());
        int lastSuccessfulBuild = details.getNextBuildNumber();
        details.build(params, true);
        List<Build> builds = details.getAllBuilds(Range.build().from(lastSuccessfulBuild).build());

        for (Build build : builds) {
            BuildWithDetails buildInfo = build.details();

            while (buildInfo.isBuilding()) {
                if (buildInfo.getResult() == BuildResult.SUCCESS) {
                    break;
                } else if (buildInfo.getResult() == BuildResult.FAILURE) {
                    throw new JenkinsBuildFailed();
                } else {
                    throw new Exception(Output.printColoredLog("[ERROR] Jenkins job was not successfully completed"));
                }
            }
        }
    }

    public static void triggerBuild(@NotNull Job job, @NotNull HashMap<String, String> params) throws Exception {
        String username = Properties.get("JENKINS_USERNAME", true);
        String password = Properties.get("JENKINS_PASSWORD", true);

        JenkinsServer batchProcessJobs = new JenkinsServer(new URI(Jenkins_Url), username, password);
        JobWithDetails details = batchProcessJobs.getJob(job.toString());
        int lastBuildNo = details.getLastBuild().getNumber();
        details.build(params, true);
        while (lastBuildNo != details.getLastBuild().getNumber()) {
//          Do nothing
        }

    }
}
