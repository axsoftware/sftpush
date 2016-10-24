package com.axsoftware.sftpush.client.ftp;

import com.axsoftware.sftpush.config.PushConfig;
import org.apache.commons.net.ftp.FTP;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class FTPushClientTest {

	private static final String HOME_DIR = "/";
	private static final Path FILE = Paths.get("/", "test.txt");
	private static final String CONTENTS = "FTPush Test";
	private static final String HOST = "localhost";
	private static final String USERNAME = "user";
	private static final String PASSWORD = "password";

	private FakeFtpServer fakeFtpServer;
	private FTPushClient ftpClient;

	@Before
	public void setUp() throws Exception {
		this.initServer();
		this.initClient();
	}

	@Test
	public void downloadFile() {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		this.ftpClient.download(FILE.getFileName().toString(), FILE.getParent().toString(), outputStream);

		Assert.assertEquals("contents", CONTENTS, outputStream.toString());
	}

	@Test
	public void uploadFile() throws IOException {
		final Path path = Paths.get(System.getProperty("java.io.tmpdir"), FTPushClientTest.class.getName());
		Files.write(path, "foobar".getBytes());

		final InputStream fileStream = new FileInputStream(path.toFile());
		this.ftpClient.upload(fileStream, "test.tmp", null, FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE, false);
	}

	@After
	public void stop() {
		this.fakeFtpServer.stop();
	}

	/**
	 * Start FTP server
	 */
	private void initServer() {
		this.fakeFtpServer = new FakeFtpServer();
		this.fakeFtpServer.setServerControlPort(0);

		final FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new FileEntry(FILE.toString(), CONTENTS));
		assertTrue(fileSystem.isFile(FILE.toString()));
		this.fakeFtpServer.setFileSystem(fileSystem);

		final UserAccount userAccount = new UserAccount(USERNAME, PASSWORD, HOME_DIR);
		this.fakeFtpServer.addUserAccount(userAccount);

		this.fakeFtpServer.start();
	}

	/**
	 * Start FTP client
	 */
	private void initClient() {
		final int port = this.fakeFtpServer.getServerControlPort();
		final PushConfig config = new PushConfig(HOST, USERNAME, PASSWORD, port);

		this.ftpClient = new FTPushClient(config);
		this.ftpClient.connect();
	}

}
