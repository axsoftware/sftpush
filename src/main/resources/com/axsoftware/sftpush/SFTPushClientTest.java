package com.axsoftware.sftpush;

import org.junit.Test;

import com.axsoftware.sftpush.client.sftp.SFTPushClient;
import com.axsoftware.sftpush.config.PushConfig;

public class SFTPushClientTest {

	
	@Test
	public void upload(){
		final SFTPushClient client = new SFTPushClient(new PushConfig("user", "sftp.host.com", "path_file.ppk"));
	}
}
