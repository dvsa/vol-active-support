package activesupport.aws.batch;

public enum JobDefinition {

    LAST_TM_LETTER("vol-app-int-last-tm-letter"),

    CONTINUATIONS_REMINDER("vol-app-int-digital-continuation-reminders"),
    DUPLICATE_VEHICLE_WARNING("vol-app-int-duplicate-vehicle-warning"),
    EXPIRE_BUS_REGISTRATION("vol-app-int-expire-bus-registration"),
    PROCESS_QUEUE("vol-app-int-process-queue-general"),

    PERMITS_RESET_TEST_DATA("vol-app-int-permits-reset-test-data ");

    private final String definitionName;

    JobDefinition(String definitionName) {
        this.definitionName = definitionName;
    }

    @Override
    public String toString() {
        return this.definitionName;
    }

    public String withRevision(int revision) {
        return this.definitionName + ":" + revision;
    }


    public String latest() {
        return this.definitionName;
    }


    public String getDefinitionName() {
        return this.definitionName;
    }
}
