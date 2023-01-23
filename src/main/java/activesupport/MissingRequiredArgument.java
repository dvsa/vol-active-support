package activesupport;

import activesupport.system.out.Output;

public class MissingRequiredArgument extends RuntimeException {
    public MissingRequiredArgument() {
        super("[ERROR] You are missing an argument that needs to be specified at run time or in your system variables");
    }

    public MissingRequiredArgument(String message) {
        super(Output.printColoredLog(message));
    }

    public MissingRequiredArgument(String message, Throwable cause) {
        super(Output.printColoredLog(message), cause);
    }

    public MissingRequiredArgument(Throwable cause) {
        super(cause);
    }
}
