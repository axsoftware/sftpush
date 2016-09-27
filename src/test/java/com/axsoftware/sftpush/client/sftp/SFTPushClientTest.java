package com.axsoftware.sftpush.client.sftp;

import com.axsoftware.sftpush.config.PushConfig;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * * SFTP Client validation
 */
public class SFTPushClientTest {

    /**
     * Server mount point
     */
    private static final Path HOME_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

    /**
     * Server address
     */
    private static final String HOST = "localhost";

    /**
     * User name
     */
    private static final String USERNAME = "user";

    /**
     * User password
     */
    private static final String PASSWORD = "password";

    /**
     * SSH Server port
     */
    private static final int PORT = 0;

    /**
     * SSH Server handle
     */
    private SshServer sshServer;

    /**
     * Create SSH server on native file system, to receive a test file
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort(PORT);
        this.sshServer.setHost(HOST);

        this.sshServer.setFileSystemFactory(new NativeFileSystemFactory());
        this.sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        this.sshServer.setCommandFactory(new ScpCommandFactory());
        this.sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        final List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
        userAuthFactories.add(new UserAuthPasswordFactory());
        this.sshServer.setUserAuthFactories(userAuthFactories);
        this.sshServer.setPasswordAuthenticator((username, password, session) -> USERNAME.equals(username) && PASSWORD.equals(password));

        this.sshServer.start();
    }

    /**
     * Send a file by SFTPushClient to SSH Server
     *
     * @throws JSchException
     * @throws SftpException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void commitFile() throws JSchException, SftpException, InterruptedException, IOException {
        final String contents = "foobar";
        final Path path = Paths.get(HOME_DIR.toString(), "source_sftp_client_test");
        Files.write(path, contents.getBytes());

        final int port = this.sshServer.getPort();
        final PushConfig pushConfig = new PushConfig(HOST, USERNAME, PASSWORD, port);
        final SFTPushClient sftPushClient = new SFTPushClient(pushConfig);

        sftPushClient.sendFile(path.getParent().toString(), path.getFileName().toString(), path.getParent().toString(), path.getFileName().toString());
    }

    /**
     * Finish SSH Server
     *
     * @throws IOException
     */
    @After
    public void tearDown() throws IOException {
        this.sshServer.stop();
    }
}