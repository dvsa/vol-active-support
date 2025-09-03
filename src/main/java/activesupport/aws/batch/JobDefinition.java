package activesupport.aws.batch;

public enum JobDefinition {

    LAST_TM_LETTER("vol-app-int-last-tm-letter"),
    TRANSXCHANGE_CONSUMER("vol-app-int-transxchange-consumer"),

    CONTINUATIONS_REMINDER("vol-app-int-continuations-reminder"),
    DUPLICATE_VEHICLE_WARNING("vol-app-int-duplicate-vehicle-warning"),
    EXPIRE_BUS_REGISTRATION("vol-app-int-expire-bus-registration"),
    PROCESS_QUEUE("vol-app-int-process-queue"),

    BATCH_CLI("vol-app-int-batch-cli"),
    BATCH_PROCESS_QUEUE("vol-app-int-batch-process-queue"),
    EBSR_QUEUE("vol-app-int-ebsr-queue");

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
