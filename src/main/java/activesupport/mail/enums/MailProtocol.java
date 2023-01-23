package activesupport.mail.enums;

public enum MailProtocol {

    Imap("imaps"),
    Pop3("pop3s");

    private String type;

    MailProtocol(String type){
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

}
