package activesupport.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.jetbrains.annotations.NotNull;

public class SSH {

    public static Session openTunnel(@NotNull String sshUser, @NotNull String remoteHost, @NotNull String sshPrivateKeyPath) throws Exception {
        // Create SSH session.  Port 22 is your SSH port which
        // is open in your firewall setup.
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
        jsch.addIdentity(sshPrivateKeyPath);

        Session session = jsch.getSession(sshUser, remoteHost, 22);
        // Additional SSH options.  See your ssh_config manual for
        // more options.  Set options according to your requirements.
        session.setConfig(config);
        session.connect();
        session.openChannel("direct-tcpip");
        return session;
    }

    public static int portForwarding(@NotNull int listeningPort, @NotNull String destinationHost, @NotNull int destinationPort, @NotNull Session session) throws Exception {
        int assignedPort = session.setPortForwardingL(listeningPort, destinationHost, destinationPort);
        if (assignedPort == 0) {
            System.err.println("Port forwarding failed !");
        }
        return assignedPort;
    }
}


