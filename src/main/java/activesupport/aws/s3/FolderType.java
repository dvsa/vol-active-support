package activesupport.aws.s3;

public enum FolderType {
    NI_EXPORT("data-gov-ni-export/"),
    EMAIL("email"),
    ROOT("");

    private String name;

    FolderType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
