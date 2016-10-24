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
import org.junit.*;

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
	 * Test file
	 */
	private static final Path FILE_PATH = Paths.get(HOME_DIR.toString(), FILE_NAME);

	/**
	 * SFTP Push client handle
	 */
	private SFTPushClient sftPushClient;

	/**
	 * Create SSH server on native file system, to receive a test file
	 *
	 * @throws Exception
	 */
	@BeforeClass
	public static void suiteSetUp() throws IOException {
		initServer();
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
		initClient();
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
	public void uploadFile() throws JSchException, SftpException, InterruptedException, IOException {

		Assume.assumeTrue(isUnix());

		Files.write(FILE_PATH, "foobar".getBytes());

		this.sftPushClient.sendFile(FILE_PATH.getParent().toString(), FILE_PATH.getFileName().toString(), FILE_PATH.getParent().toString(), FILE_PATH.getFileName().toString());

		assertTrue(Files.exists(FILE_PATH));
	}

	@Test
	public void uploadStream() throws SftpException, JSchException {

		Assume.assumeTrue(isUnix());

		final InputStream stream = new ByteArrayInputStream("foobar".getBytes());

		this.sftPushClient.sendFile(stream, FILE_PATH);

		assertTrue(Files.exists(FILE_PATH));
	}

	@Test
	public void downloadFile() {
		Assume.assumeTrue(isUnix());


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

	private static boolean isUnix() {
		final String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0);
	}

	private static void initServer() throws IOException {
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

	private void initClient() {
		final int port = SSHSERVER.getPort();
		final PushConfig pushConfig = new PushConfig(HOST, USERNAME, PASSWORD, port);
		this.sftPushClient = new SFTPushClient(pushConfig);
	}
}