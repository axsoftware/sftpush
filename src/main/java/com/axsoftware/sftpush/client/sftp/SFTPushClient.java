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
		config.put(STRICT_HOST_KEY_CHECKING, IntegradorParametroService.FTP_STRICT_HOST_KEY);
		session.setConfig(config);

		return session;

	}

	private Channel getChannel(final Session session, final CHANNEL_TYPE channelType) throws JSchException {

		Channel channel = session.openChannel(channelType.toString());

		return channel;

	}

	private String formatPath(String path) {

		if (path == null) {
			throw new IllegalArgumentException("Path invalido: " + path);
		}

		if (path.endsWith("/") || path.endsWith("\\")) {
			return path;
		}

		return path + "/";

	}

	/**
	 * Transfere todos os arquivos do diretorio remoto para o diretorio local.
	 * 
	 * @param remoteDir
	 *            Caminho do diretorio remoto.
	 * @param localDir
	 *            Caminho do diretorio local.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
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

			logger.

		} finally {

			sftpChannel.exit();
			session.disconnect();

		}

	}

	/**
	 * Transfere arquivo do diretorio remoto para o diretorio local.
	 * 
	 * @param remoteDir
	 *            Caminho do diretorio remoto.
	 * @param remoteFileName
	 *            Nome do arquivo remoto.
	 * @param localDir
	 *            Caminho do diretorio local.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
	 */
	public void doTransferFile(String remoteDir, String remoteFileName, String localDir) throws JSchException, SftpException {

		doTransferFile(remoteDir, remoteFileName, localDir, remoteFileName);

	}

	/**
	 * Transfere arquivo do diretorio remoto para o diretorio local.
	 * 
	 * @param remoteDir
	 *            Caminho do diretorio remoto.
	 * @param remoteFileName
	 *            Nome do arquivo remoto.
	 * @param localDir
	 *            Caminho do diretorio local.
	 * @param localFileName
	 *            Nome do arquivo local.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
	 */
	public void doTransferFile(String remoteDir, String remoteFileName, String localDir, String localFileName) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho remoto invalido: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho local invalido: " + localDir);
		}

		if (remoteFileName == null || remoteFileName.isEmpty()) {
			throw new IllegalArgumentException("Nome do arquivo remoto invalido: " + remoteFileName);
		}

		if (localFileName == null || localFileName.isEmpty()) {
			throw new IllegalArgumentException("Nome do arquivo local invalido: " + localFileName);
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
	 * Lista todos os arquivos do diretorio remoto.
	 * 
	 * @param remotePath
	 *            Caminho do diretorio remoto.
	 * @return Nome do arquivos remotos.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
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
	 * Tranfere os arquivos remotos para o diretorio local, utilizando a mesma
	 * sessao FTP.
	 * 
	 * Ao realizar a tranferencia por nome com diversos arquivos este metodo tem
	 * melhor performance, uma vez que a sessao e criada apenas uma vez.
	 * 
	 * @param remotePath
	 *            Caminho do diretorio remoto.
	 * @param localDir
	 *            Caminho do diretorio local.
	 * @param remoteFileNames
	 *            Nomes dos arquivos remotos.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
	 */
	public void doTransferFileList(String remoteDir, String localDir, String... remoteFileNames) throws JSchException, SftpException {

		if (remoteDir == null || remoteDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho remoto invalido: " + remoteDir);
		}

		if (localDir == null || localDir.isEmpty()) {
			throw new IllegalArgumentException("Caminho local invalido: " + localDir);
		}

		if (remoteFileNames == null || remoteFileNames.length == 0) {
			throw new IllegalArgumentException("Nome de arquivos remotos invalido: " + remoteFileNames);
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
	 * Tranfere os arquivos locais para o diretorio remoto, utilizando a mesma
	 * sessao FTP.
	 * 
	 * @param remotePath
	 *            Caminho do diretorio remoto.
	 * @param localDir
	 *            Caminho do diretorio local.
	 * @param localFileNames
	 *            Nomes dos arquivos remotos.
	 * @throws JSchException
	 *             Erro ao estabelecer sessao FTP.
	 * @throws SftpException
	 *             Erro na conexao segura FTP (SFTP).
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
