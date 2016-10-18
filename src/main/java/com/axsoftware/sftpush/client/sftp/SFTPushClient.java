package com.axsoftware.sftpush.client.sftp;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
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

	private static final String HOST_KEY_ALGORITHMS = "HostKeyAlgorithms";

	private PushConfig connection;

	private Session sftpSession;
	
	private enum CHANNEL_TYPE {
		exec, sftp, shell
	}

	public SFTPushClient(final PushConfig connection) {
		this.connection = connection;
	}

	/**
	 * Get current SFTP Session
	 * @return
	 * @throws JSchException
	 */
	public Session getSession() throws JSchException {
		if(sftpSession == null || !sftpSession.isConnected()){
			return getSFTPession();
		}
		return sftpSession;
	}
	
	private Session getSFTPession() throws JSchException {

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
	 * Get SFTP Channel
	 * 
	 * @return
	 * @throws JSchException
	 */
	private ChannelSftp getChannelSftp() throws JSchException{
		sftpSession = getSession();
		sftpSession.connect();
		final Channel channel = getChannel(sftpSession, CHANNEL_TYPE.sftp);
		channel.connect();
		return (ChannelSftp) channel;
	}
	
	/**
	 * Disconnect SFTP
	 */
	private void disconnectSession(){
		if(sftpSession != null){
			sftpSession.disconnect();
		}
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

		final ChannelSftp sftpChannel = getChannelSftp();
		try {
			@SuppressWarnings("unchecked")
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
			disconnectSession();
		}
	}

	public void sendFile(final String remoteDir, final String remoteFileName, final String localDir) throws JSchException, SftpException {
		sendFile(remoteDir, remoteFileName, localDir, remoteFileName);
	}

	/**
	 * Collects a input stream and create a remote file.
	 *
	 * @param fileStream Input file contents
	 * @param remotePath Ouput file on remote server
	 */
	public void sendFile(final InputStream fileStream, final Path remotePath) throws JSchException, SftpException {
		if (fileStream == null) {
			throw new IllegalArgumentException("Input stream must be valid");
		}

		if (remotePath == null || remotePath.toString().isEmpty()) {
			throw new IllegalArgumentException("Remote path must be valid");
		}

		final ChannelSftp sftpChannel = getChannelSftp();
		try {
			sftpChannel.put(fileStream, remotePath.toString());
		} catch (final SftpException e) {
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
			throw e;
		} finally {
			sftpChannel.exit();
			disconnectSession();
		}
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

		final ChannelSftp sftpChannel = getChannelSftp();
		try {
			sftpChannel.get(remoteDir + remoteFileName, localDir + localFileName);
		} catch (final SftpException e) {
//			TODO SFP - Refactor 
			this.logger.severe(EXCEPTION_ERROR_EXECUTE_COMMAND_SFTP);
			throw e;
		} finally {
			sftpChannel.exit();
			disconnectSession();
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

		final ChannelSftp sftpChannel = getChannelSftp();
		final List<String> filesNames = new ArrayList<>();
		try {
			@SuppressWarnings("unchecked")
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
			disconnectSession();
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

		final ChannelSftp sftpChannel = getChannelSftp();
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
			disconnectSession();
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

		final ChannelSftp sftpChannel = getChannelSftp();
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
			disconnectSession();
		}
	}

	public void setConnection(final PushConfig connection) {
		this.connection = connection;
	}
}