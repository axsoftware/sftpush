package com.axsoftware.sftpush.client.sftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import com.axsoftware.sftpush.config.PushConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPushClient {

	private final Logger logger = Logger.getLogger(SFTPushClient.class.getName());
	
	private static final String EXCEPTION_NO_SUCH_FILE = "NO SUCH FILE: %s";

	private static final String EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP = "Error execute command SFTP";

	private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private PushConfig connection;

	private enum CHANNEL_TYPE {
		exec, sftp, shell
	};

	public SFTPushClient(PushConfig connection) {
		this.connection = connection;
	}

	private Session getSession() throws JSchException {

		final JSch jsch = new JSch();

		if (connection.getPpk() != null) {
			jsch.addIdentity(connection.getPpk());
		}

		final Session session = jsch.getSession(connection.getUsername(), connection.getHost(), connection.getPort());

		if (connection.getPassword() != null) {
			session.setPassword(connection.getPassword());
		}

		final Properties config = new Properties();
		config.put(STRICT_HOST_KEY_CHECKING, "no");
		session.setConfig(config);

		return session;
	}

	private Channel getChannel(final Session session, final CHANNEL_TYPE channelType) throws JSchException {
		return session.openChannel(channelType.toString());
	}

	private String formatPath(String path) {
		if (path == null) {
			throw new IllegalArgumentException("Invalid Path: " + path);
		}
		if (path.endsWith("/") || path.endsWith("\\")) {
			return path;
		}
		return path + "/";
	}

	/**
	 * Pull all remote files to local folder
	 * 
	 * @param remoteDir Path remote folder.
	 * @param localDir Path local folder.
	 * @throws JSchException Error connect SFTP.
	 * @throws SftpException Error send command SFTP.
	 */
	public void sendAllFiles(final String remoteDir, final String localDir) throws JSchException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote path: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid local folder: " + localDir);
		}

		final String formatDir = formatPath(remoteDir);
		final String formatLocalDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		try {

			final Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(formatDir);
			for (ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					sftpChannel.get(formatDir + listEntry.getFilename(), formatLocalDir + listEntry.getFilename());
				}
			}

		} catch (SftpException e) {
//			TODO SFP - Refactor 
			logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}
	}

	public void sendFile(String remoteDir, String remoteFileName, String localDir) throws JSchException, SftpException {
		sendFile(remoteDir, remoteFileName, localDir, remoteFileName);
	}

	/**
	 * Get file from remote directory to local folder
	 * 
	 * @param remoteDir - Path remote dir
	 * @param remoteFileName - Remote filename
	 * @param localDir - Path local folder
	 * @param localFileName - Local filename
	 * @throws JSchException - Error connect FTP
	 * @throws SftpException - Error connect SFTP
	 */
	public void sendFile(String remoteDir, String remoteFileName, String localDir, String localFileName) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote path: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid local folder: " + localDir);
		}

		if (remoteFileName == null || remoteFileName.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote filename: " + remoteFileName);
		}

		if (localFileName == null || localFileName.isEmpty()) {
			throw new IllegalArgumentException("Invalid local filename: " + localFileName);
		}

		remoteDir = formatPath(remoteDir);
		localDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;
		try {
			sftpChannel.get(remoteDir + remoteFileName, localDir + localFileName);
		} catch (SftpException e) {
//			TODO SFP - Refactor 
			logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);			
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}
	}

	/**
	 * List all remote files .
	 * 
	 * @param remotePath - Path remote dir.
	 * @return Return list filenames.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public List<String> listFiles(String remotePath) throws JSchException, SftpException {

		if (remotePath == null || remotePath.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote path: " + remotePath);
		}

		remotePath = formatPath(remotePath);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		List<String> filesNames = new ArrayList<>();

		try {

			Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(remotePath);

			for (ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					filesNames.add(listEntry.getFilename());
				}

			}

		} catch (SftpException e) {
//			TODO SFP - Refactor 
			logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);		
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}
		return filesNames;
	}

	/**
	 * Transfer remote files to local folder
	 * 
	 * Use this method when needs increase performance   
	 * 
	 * @param remotePath Path remote directory.
	 * @param localDir Path local dir.
	 * @param remoteFileNames Remote filenames.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public void doTransferFileList(String remoteDir, String localDir, String... remoteFileNames) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote folder: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid local folder: " + localDir);
		}

		if (remoteFileNames == null || remoteFileNames.length == 0) {
			throw new IllegalArgumentException("Invalid name of remote files: " + remoteFileNames);
		}

		remoteDir = formatPath(remoteDir);
		localDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		try {

			for (String remoteFileName : remoteFileNames) {

				try {

					sftpChannel.get(remoteDir + remoteFileName, localDir + remoteFileName);

				} catch (SftpException e) {

					if (e.id == 2) { 
//						TODO SFP - Refactor 
						logger.severe(String.format(EXCEPTION_NO_SUCH_FILE, remoteFileName));								
						continue;
					}

					throw e;
				}
			}

		} catch (SftpException e) {
			// 2: No such file
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}

	}

	/**
	 * Send local files to remote folder
	 * 
	 * @param remotePath Path remote folder.
	 * @param localDir Path local folder.
	 * @param localFileNames List name remote files.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public void putFileList(String localDir, String remoteDir, String... localFileNames) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid remote folder: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Invalid local folder: " + localDir);
		}

		if (localFileNames == null || localFileNames.length == 0) {
			throw new IllegalArgumentException("Invalid local name files: " + localFileNames);
		}

		remoteDir = formatPath(remoteDir);
		localDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		final ChannelSftp sftpChannel = (ChannelSftp) channel;
		try {

			for (String localFileName : localFileNames) {

				try {

					sftpChannel.put(localDir + localFileName, remoteDir + localFileName);

				} catch (SftpException e) {
					if (e.id == 2) {
//						TODO SFP - Refactor 
						logger.severe(String.format(EXCEPTION_NO_SUCH_FILE, localFileName));		
						continue;
					}
					throw e;
				}
			}

		} catch (SftpException e) {
//			TODO SFP - Refactor 
			logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);		
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}

	}

	public void setConnection(PushConfig connection) {
		this.connection = connection;
	}
}