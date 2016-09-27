package com.axsoftware.sftpush.client.sftp;

import com.axsoftware.sftpush.config.PushConfig;
import com.jcraft.jsch.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

public class SFTPushClient {

	private final Logger logger = Logger.getLogger(SFTPushClient.class.getName());
	
	private static final String EXCEPTION_NO_SUCH_FILE = "NO SUCH FILE: %s";

	private static final String EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP = "Error execute command SFTP";

	private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private static final String HOST_KEY_ALGORITHMS = "HostKeyAlgorithms";

	private PushConfig connection;

	private enum CHANNEL_TYPE {
		exec, sftp, shell
	};

	public static void main(final String[] args) {
		System.out.println(File.separator);
	}

	public SFTPushClient(final PushConfig connection) {
		this.connection = connection;
	}

	private Session getSession() throws JSchException {

		final JSch jsch = new JSch();

		if (this.connection.getPpk() != null) {
			jsch.addIdentity(this.connection.getPpk());
		}

		final Session session = jsch.getSession(this.connection.getUsername(), this.connection.getHost(), this.connection.getPort());

		if (this.connection.getPassword() != null) {
			session.setPassword(this.connection.getPassword());
		}

		final Properties config = new Properties();
		config.put(STRICT_HOST_KEY_CHECKING, "no");
		config.put(HOST_KEY_ALGORITHMS, "+ssh-dss");
		session.setConfig(config);

		return session;
	}

	private Channel getChannel(final Session session, final CHANNEL_TYPE channelType) throws JSchException {
		return session.openChannel(channelType.toString());
	}

	private String formatPath(final String path) {
		if (path == null) {
			throw new IllegalArgumentException("Invalid Path: " + path);
		}
		return path.endsWith(File.separator) ? path : path + File.separator; 
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

		final ChannelSftp sftpChannel = (ChannelSftp) channel;

		try {

			final Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(formatDir);
			for (final ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					sftpChannel.get(formatDir + listEntry.getFilename(), formatLocalDir + listEntry.getFilename());
				}
			}

		} catch (final SftpException e) {
//			TODO SFP - Refactor 
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}
	}

	public void sendFile(final String remoteDir, final String remoteFileName, final String localDir) throws JSchException, SftpException {
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
	public void sendFile(String remoteDir, final String remoteFileName, String localDir, final String localFileName) throws JSchException, SftpException {

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

		final ChannelSftp sftpChannel = (ChannelSftp) channel;
		try {
			sftpChannel.get(remoteDir + remoteFileName, localDir + localFileName);
		} catch (final SftpException e) {
//			TODO SFP - Refactor 
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
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

		final ChannelSftp sftpChannel = (ChannelSftp) channel;

		final List<String> filesNames = new ArrayList<>();

		try {

			final Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(remotePath);

			for (final ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					filesNames.add(listEntry.getFilename());
				}

			}

		} catch (final SftpException e) {
//			TODO SFP - Refactor 
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
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
	 * @param remoteDir Path remote directory.
	 * @param localDir Path local dir.
	 * @param remoteFileNames Remote filenames.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public void doTransferFileList(String remoteDir, String localDir, final String... remoteFileNames) throws JSchException, SftpException {

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

		final ChannelSftp sftpChannel = (ChannelSftp) channel;

		try {

			for (final String remoteFileName : remoteFileNames) {

				try {

					sftpChannel.get(remoteDir + remoteFileName, localDir + remoteFileName);

				} catch (final SftpException e) {

					if (e.id == 2) { 
//						TODO SFP - Refactor 
						this.logger.severe(String.format(EXCEPTION_NO_SUCH_FILE, remoteFileName));
						continue;
					}

					throw e;
				}
			}

		} catch (final SftpException e) {
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
	 * @param localDir Path remote folder.
	 * @param localDir Path local folder.
	 * @param localFileNames List name remote files.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public void putFileList(String localDir, String remoteDir, final String... localFileNames) throws JSchException, SftpException {

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

			for (final String localFileName : localFileNames) {

				try {

					sftpChannel.put(localDir + localFileName, remoteDir + localFileName);

				} catch (final SftpException e) {
					if (e.id == 2) {
//						TODO SFP - Refactor 
						this.logger.severe(String.format(EXCEPTION_NO_SUCH_FILE, localFileName));
						continue;
					}
					throw e;
				}
			}

		} catch (final SftpException e) {
//			TODO SFP - Refactor 
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}

	}

	public void setConnection(final PushConfig connection) {
		this.connection = connection;
	}
}