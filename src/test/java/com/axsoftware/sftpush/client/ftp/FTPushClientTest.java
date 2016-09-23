package com.axsoftware.sftpush.client.ftp;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import com.axsoftware.sftpush.config.PushConfig;

public class FTPushClientTest {

    private static final String HOME_DIR 	= "/";
    private static final String FILE 		= "/test.txt";
    private static final String CONTENTS 	= "FTPush Test";
    private static final String HOST 		= "localhost";
    private static final String USERNAME 	= "user";
    private static final String PASSWORD 	= "password";
	
	private FakeFtpServer fakeFtpServer;

	@Before
	public void setUp() throws Exception {
		fakeFtpServer = new FakeFtpServer();
		fakeFtpServer.setServerControlPort(0); // use any free port

		FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new FileEntry(FILE, CONTENTS));
		fakeFtpServer.setFileSystem(fileSystem);

		UserAccount userAccount = new UserAccount(USERNAME, PASSWORD, HOME_DIR);
		fakeFtpServer.addUserAccount(userAccount);

		fakeFtpServer.start();
	}

	@Test
	public void downloadFile() {

		int port = fakeFtpServer.getServerControlPort();
		final PushConfig config = new PushConfig(HOST, USERNAME, PASSWORD, port);
		final FTPushClient ftp = new FTPushClient(config);
		ftp.connect();

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ftp.download(FILE, outputStream);
		
		Assert.assertEquals("contents", CONTENTS, outputStream.toString());
		
	}
	
	@After
	public void stop() {
		fakeFtpServer.stop();
	}
	
}
