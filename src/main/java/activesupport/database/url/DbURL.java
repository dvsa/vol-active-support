package activesupport.database.url;

import activesupport.database.credential.DatabaseCredentialType;

import static activesupport.database.DBUnit.loadDBCredential;

public class DbURL {


    private int portNumber;


    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getDBUrl(String env) {
        String dbURL;
        switch (env.toLowerCase().trim()) {
            case "int":
            case "pp":
                dbURL = String.format("jdbc:mysql://localhost:%s/OLCS_RDS_OLCSDB?user=%s&password=%s&useSSL=false",getPortNumber(),loadDBCredential(DatabaseCredentialType.USERNAME),
                        loadDBCredential(DatabaseCredentialType.PASSWORD));
                break;
            default:
                dbURL = String.format("jdbc:mysql://olcsdb-rds.olcs.%s.nonprod.dvsa.aws:3306/OLCS_RDS_OLCSDB?user=%s&password=%s&useSSL=false",env,loadDBCredential(DatabaseCredentialType.USERNAME),
                        loadDBCredential(DatabaseCredentialType.PASSWORD));
                break;
        }
        return dbURL;
    }
}