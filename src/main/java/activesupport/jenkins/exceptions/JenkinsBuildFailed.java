package activesupport.jenkins.exceptions;

import activesupport.system.out.Output;

public class JenkinsBuildFailed extends Exception {
    public JenkinsBuildFailed() {
        super("[ERROR] Jenkins build failed");
    }

    public JenkinsBuildFailed(String message) {
        super(Output.printColoredLog(message));
    }

    public JenkinsBuildFailed(String message, Throwable cause) {
        super(Output.printColoredLog(message), cause);
    }

    public JenkinsBuildFailed(Throwable cause) {
        super(cause);
    }
}
