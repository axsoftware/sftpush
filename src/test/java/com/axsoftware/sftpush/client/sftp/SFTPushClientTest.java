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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * * SFTP Client validation
 */
public class SFTPushClientTest {

	/**
	 * File test name
	 */
	private static final String SRC_FILE_NAME = "source_sftp_client_file_test";

	/**
	 * File target name
	 */
	private static final String TARGET_FILE_NAME = "target_sftp_client_file_test";

	/**
	 * Server mount point
	 */
	private static final Path HOME_DIR = Paths.get("target", "sftp-test");

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
	private static final Path SRC_FILE_PATH = Paths.get(HOME_DIR.toString(), SRC_FILE_NAME);

	/**
	 * Target test file
	 */
	private static final Path TARGET_FILE_PATH = Paths.get(HOME_DIR.toString(), TARGET_FILE_NAME);

	/**
	 * Target test directory
	 */
	private static final Path TARGET_DIR_PATH = Paths.get(HOME_DIR.toString(), "couse");

	/**
	 * File contents
	 */
	private static final String FILE_CONTENTS = "foobar";

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
		if (!Files.exists(HOME_DIR)) {
			Files.createDirectory(HOME_DIR);
		}
		Files.write(SRC_FILE_PATH, FILE_CONTENTS.getBytes());
	}

	/**
	 * Remove temporary files
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		if (Files.exists(TARGET_FILE_PATH)) {
			Files.delete(TARGET_FILE_PATH);
		}
		if (Files.exists(TARGET_DIR_PATH)) {
			final List<Path> files = Files.list(TARGET_DIR_PATH).collect(toList());
			for (final Path file : files) {
				Files.delete(file);
			}
			Files.delete(TARGET_DIR_PATH);
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
		assertFalse(Files.exists(TARGET_FILE_PATH));

		this.sftPushClient.uploadFile(SRC_FILE_PATH.toFile(), TARGET_FILE_PATH);

		assertTrue(Files.exists(TARGET_FILE_PATH));
	}

	@Test
	public void uploadStream() throws SftpException, JSchException, FileNotFoundException {

		Assume.assumeTrue(isUnix());

		final InputStream stream = new FileInputStream(SRC_FILE_PATH.toFile());
		assertFalse(Files.exists(TARGET_FILE_PATH));

		this.sftPushClient.uploadFile(stream, TARGET_FILE_PATH);

		assertTrue(Files.exists(TARGET_FILE_PATH));
	}

	@Test
	public void downloadFile() throws IOException, SftpException, JSchException {

		Assume.assumeTrue(isUnix());
		assertFalse(Files.exists(TARGET_FILE_PATH));

		this.sftPushClient.downloadFile(SRC_FILE_PATH, TARGET_FILE_PATH);

		assertTrue(Files.exists(TARGET_FILE_PATH));
	}

	@Test
	public void makeDirectory() throws FileNotFoundException, JSchException, SftpException {
		Assume.assumeTrue(isUnix());

		final Path expectedTargetFile = Paths.get(TARGET_DIR_PATH.toString(), TARGET_FILE_NAME);

		assertFalse(Files.exists(TARGET_DIR_PATH));
		assertFalse(Files.exists(expectedTargetFile));
		this.sftPushClient.createRemoteDirectory(TARGET_DIR_PATH.toString());
		assertTrue(Files.exists(TARGET_DIR_PATH));
		this.sftPushClient.uploadFile(SRC_FILE_PATH.toFile(), expectedTargetFile);
		assertTrue(Files.exists(expectedTargetFile));
	}

	@Test(expected = SftpException.class)
	public void makeDirectoryFail() throws FileNotFoundException, JSchException, SftpException {
		Assume.assumeTrue(isUnix());

		final Path expectedTargetFile = Paths.get(TARGET_DIR_PATH.toString(), TARGET_FILE_NAME);
		this.sftPushClient.uploadFile(SRC_FILE_PATH.toFile(), expectedTargetFile);
	}

	@Test
	public void moveRemoteFile() throws FileNotFoundException, JSchException, SftpException {
		Assume.assumeTrue(isUnix());

		final Path expectedTargetFile = Paths.get(TARGET_DIR_PATH.toString(), TARGET_FILE_NAME);

		assertFalse(Files.exists(TARGET_FILE_PATH));
		this.sftPushClient.uploadFile(SRC_FILE_PATH.toFile(), TARGET_FILE_PATH);
		assertTrue(Files.exists(TARGET_FILE_PATH));
		this.sftPushClient.createRemoteDirectory(TARGET_DIR_PATH.toString());
		assertTrue(Files.exists(TARGET_DIR_PATH));
		this.sftPushClient.moveRemoteFile(TARGET_FILE_PATH, expectedTargetFile);
		assertTrue(Files.exists(expectedTargetFile));
		assertFalse(Files.exists(TARGET_FILE_PATH));
	}

	/**
	 * Finish SSH Server
	 *
	 * @throws IOException
	 */
	@AfterClass
	public static void suiteTearDown() throws IOException {
		SSHSERVER.stop();
		Files.delete(SRC_FILE_PATH);
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