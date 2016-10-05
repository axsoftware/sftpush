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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;


/**
 * * SFTP Client validation
 */
public class SFTPushClientTest {

    /**
     * File test name
     */
    private static final String FILE_NAME = "source_sftp_client_file_test";

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
    private static SshServer SSHSERVER;

    /**
     * Create SSH server on native file system, to receive a test file
     *
     * @throws Exception
     */
    @BeforeClass
    public static void suiteSetUp() throws IOException {
        SSHSERVER = SshServer.setUpDefaultServer();
        SSHSERVER.setPort(PORT);
        SSHSERVER.setHost(HOST);

        SSHSERVER.setFileSystemFactory(new NativeFileSystemFactory());
        SSHSERVER.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        SSHSERVER.setCommandFactory(new ScpCommandFactory());
        SSHSERVER.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        final List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
        userAuthFactories.add(new UserAuthPasswordFactory());
        SSHSERVER.setUserAuthFactories(userAuthFactories);
        SSHSERVER.setPasswordAuthenticator((username, password, session) -> USERNAME.equals(username) && PASSWORD.equals(password));

        SSHSERVER.start();
    }

    /**
     * Remove temporary files
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        final Path outputFile = Paths.get(HOME_DIR.toString(), FILE_NAME);
        if (Files.exists(outputFile)) {
            Files.delete(outputFile);
        }
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
        final Path path = Paths.get(HOME_DIR.toString(), FILE_NAME);
        Files.write(path, contents.getBytes());

        final int port = this.SSHSERVER.getPort();
        final PushConfig pushConfig = new PushConfig(HOST, USERNAME, PASSWORD, port);
        final SFTPushClient sftPushClient = new SFTPushClient(pushConfig);

        sftPushClient.sendFile(path.getParent().toString(), path.getFileName().toString(), path.getParent().toString(), path.getFileName().toString());

        assertTrue(Files.exists(path));
    }

    @Test
    public void commitStream() throws SftpException, JSchException {
        final String contents = "foobar";
        final InputStream stream = new ByteArrayInputStream(contents.getBytes());
        final Path path = Paths.get(HOME_DIR.toString(), FILE_NAME);
        final int port = this.SSHSERVER.getPort();
        final PushConfig pushConfig = new PushConfig(HOST, USERNAME, PASSWORD, port);
        final SFTPushClient sftPushClient = new SFTPushClient(pushConfig);

        sftPushClient.sendFile(stream, path);

        assertTrue(Files.exists(path));
    }

    /**
     * Finish SSH Server
     *
     * @throws IOException
     */
    @AfterClass
    public static void suiteTearDown() throws IOException {
        SSHSERVER.stop();
    }
}