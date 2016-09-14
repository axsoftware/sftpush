package com.axsoftware.sftpush.client.sftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.axsoftware.sftpush.config.SFTPushConfig;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPushClient {

	private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private final Logger logger = Logger.getLogger(SFTPushClient.class.getName());

	private enum CHANNEL_TYPE {
		exec, sftp, shell
	};

	private SFTPushConfig connection;

	public SFTPushClient(SFTPushConfig connection) {
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
	 * Transfere todos os arquivos do diretorio remoto para o diretorio local.
	 * 
	 * @param remoteDir Caminho do diretorio remoto.
	 * @param localDir Caminho do diretorio local.
	 * @throws JSchException Erro ao estabelecer sessao FTP.
	 * @throws SftpException Erro na conexao segura FTP (SFTP).
	 */
	public void doTransferAllFiles(String remoteDir, String localDir) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho remoto invalido: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho local invalido: " + localDir);
		}

		remoteDir = formatPath(remoteDir);
		localDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		Vector<ChannelSftp.LsEntry> list;

		try {

			list = sftpChannel.ls(remoteDir);

			for (ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					sftpChannel.get(remoteDir + listEntry.getFilename(), localDir + listEntry.getFilename());
				}
			}

		} catch (SftpException e) {

			logger.throwing(sourceClass, sourceMethod, e);

		} finally {

			sftpChannel.exit();
			session.disconnect();

		}

	}

	public void doTransferFile(String remoteDir, String remoteFileName, String localDir) throws JSchException, SftpException {
		doTransferFile(remoteDir, remoteFileName, localDir, remoteFileName);
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
	public void doTransferFile(String remoteDir, String remoteFileName, String localDir, String localFileName) throws JSchException, SftpException {

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

			logger.error("Erro ao realizar SFTP.", e);

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
	public List<String> doListFiles(String remotePath) throws JSchException, SftpException {

		if (remotePath == null || remotePath.isEmpty()) {
			throw new IllegalArgumentException("Caminho remoto invalido: " + remotePath);
		}

		remotePath = formatPath(remotePath);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		Vector<ChannelSftp.LsEntry> list;

		List<String> filesNames = new ArrayList<>();

		try {

			list = sftpChannel.ls(remotePath);

			for (ChannelSftp.LsEntry listEntry : list) {

				if (!listEntry.getAttrs().isDir()) {
					filesNames.add(listEntry.getFilename());
				}

			}

		} catch (SftpException e) {
			logger.error("Erro ao realizar SFTP.", e);
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

					if (e.id == 2) { // 2: No such file
						logger.error("Arquivo nao existe: " + remoteFileName);
						continue;
					}

					throw e;
				}
			}

		} catch (SftpException e) {
			logger.error("Erro ao realizar SFTP.", e);
			throw e;
		} finally {
			sftpChannel.exit();
			session.disconnect();
		}

	}

	/**
	 * Send local files to remote folder
	 * 
	 * 
	 * @param remotePath Path remote folder.
	 * @param localDir Path local folder.
	 * @param localFileNames List name remote files.
	 * @throws JSchException Error connect session SFTP.
	 * @throws SftpException Error execute command SFTP.
	 */
	public void doPutFileList(String localDir, String remoteDir, String... localFileNames) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho remoto invalido: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho local invalido: " + localDir);
		}

		if (localFileNames == null || localFileNames.length == 0) {
			throw new IllegalArgumentException("Nome de arquivos locais invalido: " + localFileNames);
		}

		remoteDir = formatPath(remoteDir);
		localDir = formatPath(localDir);

		final Session session = getSession();
		session.connect();

		final Channel channel = getChannel(session, CHANNEL_TYPE.sftp);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;

		try {

			for (String localFileName : localFileNames) {

				try {

					sftpChannel.put(localDir + localFileName, remoteDir + localFileName);

				} catch (SftpException e) {

					if (e.id == 2) { // 2: No such file
						logger.error("Arquivo nao existe: " + localFileName);
						continue;
					}

					throw e;

				}

			}

		} catch (SftpException e) {

			logger.error("Erro ao realizar SFTP.", e);

			throw e;

		} finally {

			sftpChannel.exit();
			session.disconnect();

		}

	}

	public void setConnection(SFTPushConfig connection) {
		this.connection = connection;
	}
}