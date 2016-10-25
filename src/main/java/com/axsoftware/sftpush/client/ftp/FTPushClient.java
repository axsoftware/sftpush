package com.axsoftware.sftpush.client.ftp;

import com.axsoftware.sftpush.config.PushConfig;
import com.axsoftware.sftpush.exception.SFTPushException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.logging.Logger;

public final class FTPushClient {

	private static final Logger logger = Logger.getLogger(FTPushClient.class.getName());

	private static final String DOT = ".";
	private static final String ERROR_CHANGE_FOLDER = "Error on select folder %s. ( %s )";
	private static final String ERROR_CONNECT_FTP = "Error on connect FTP %s.( %s )";
	private static final String ERROR_REMOVE_FILE = "Error on remove file.( %s )";
	private static final String ERROR_DOWNLOAD_FILE = "Error on download file.( %s )";
	private static final String ERROR_AUTHENTICATE_USER = "Error in authenticate FTP user ( %s )";
	private static final String ERROR_FILE_NOT_FOUND = "File %s not found.";
	private static final String ERROR_QUIT_CONNECTION = "Error on close connection.( %s )";
	private static final String ERROR_UPLOAD_FILE = "Error on upload file.( %s )";

	private FTPClient ftpClient;
	private PushConfig ftpConfig;

	public FTPushClient() {

	}

	public FTPushClient(final PushConfig ftpConfig) {
		this.ftpConfig = ftpConfig;
	}

	/**
	 * Change FTP folder
	 *
	 * @throws SFTPushException
	 */
	public void changeDirectory(final String directory) throws SFTPushException {

		if (directory != null) {
			logger.info(String.format("Using directory %s", directory));
			try {
				if (directory.startsWith(File.separator)) {
					getFtpClient().changeWorkingDirectory(File.separator);
				}

				for (final String dir : directory.split(File.separator)) {
					getFtpClient().changeWorkingDirectory(dir);
				}
			} catch (final Exception e) {
				throw new SFTPushException(String.format(FTPushClient.ERROR_CHANGE_FOLDER, directory, e.getMessage()));
			}
		}
	}

	/**
	 * List all files from root folder.
	 *
	 * @return
	 * @throws SFTPushException
	 */
	public FTPFile[] listFiles() throws SFTPushException {
		try {
			return getFtpClient().listFiles();
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_CHANGE_FOLDER, e.getMessage()));
		}
	}

	/**
	 * FTP Connect
	 *
	 * @throws SFTPushException
	 */
	public void connect() throws SFTPushException {
		this.connect(getFtpConfig().getHost(), getFtpConfig().getPort(), getFtpConfig().getUsername(), getFtpConfig().getPassword());
	}

	/**
	 * FTP Connect with default port
	 *
	 * @throws SFTPushException
	 */
	public void connect(final String address, final String username, final String password) throws SFTPushException {
		this.connect(address, null, username, password);
	}

	/**
	 * Connect FTP
	 *
	 * @throws SFTPushException
	 */
	public void connect(final String host, final Integer port, final String username, final String password) throws SFTPushException {

		logger.info(String.format("Connect FTO usign params > host: %s - port: %s - username: %s - password: %s", host, port, username, password));

		this.ftpClient = new FTPClient();
		this.ftpClient.setConnectTimeout(5000);

		try {
			if (port == null) {
				this.ftpClient.connect(host);
			} else {
				this.ftpClient.connect(host, port);
			}
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_CONNECT_FTP, host, e));
		}

		try {
			this.ftpClient.login(username, password);
		} catch (final IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_AUTHENTICATE_USER, username, e));
		}
	}

	/**
	 * Remove FTP file.
	 *
	 * @throws SFTPushException
	 */
	public void delete(final FTPFile file) throws SFTPushException {
		this.delete(file.getName());
	}

	/**
	 * Remove file.
	 *
	 * @throws SFTPushException
	 */
	public void delete(final String fileName) throws SFTPushException {
		this.delete(fileName, DOT);
	}

	/**
	 * Remove file in folder
	 *
	 * @throws SFTPushException
	 */
	public void delete(final String fileName, final String directory) throws SFTPushException {

		logger.info(String.format("Delete file filename: %s in directory %s", fileName, directory));
		changeDirectory(directory);
		try {
			getFtpClient().deleteFile(fileName);
		} catch (final IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_REMOVE_FILE, fileName, e));
		}
	}

	/**
	 * Send the file and return object
	 *
	 * @throws SFTPushException
	 */
	public File download(final FTPFile file) throws SFTPushException {
		try {

			final File outputFile = File.createTempFile(file.getName(), null);
			final FileOutputStream output = new FileOutputStream(outputFile);

			this.download(file, output);
			output.close();

			return outputFile;
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_DOWNLOAD_FILE, file.getName(), e));
		}
	}


	/**
	 * Download the file and put in OutputStream
	 *
	 * @throws SFTPushException
	 */
	public void download(final FTPFile file, final OutputStream output) throws SFTPushException {
		this.download(file.getName(), null, output);
	}


	/**
	 * Download the file and put in OutputStream
	 *
	 * @throws SFTPushException
	 */
	public void download(final String fileName, final OutputStream output) throws SFTPushException {
		this.download(fileName, null, output);
	}

	/**
	 * Download the file and put in OutputStream
	 *
	 * @throws SFTPushException
	 */
	public void download(final String fileName, final String directory, final OutputStream output) throws SFTPushException {

		logger.info(String.format("Download file filename: %s in directory %s", fileName, directory));
		changeDirectory(directory);

		if (!fileExists(fileName, null)) {
			throw new SFTPushException(String.format(ERROR_FILE_NOT_FOUND, fileName));
		}

		try {
			getFtpClient().retrieveFile(fileName, output);
		} catch (final IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_DOWNLOAD_FILE, fileName, e));
		}
	}

	/**
	 * Download the file and put in OutputStream
	 *
	 * @throws SFTPushException
	 */
	public File download(final String fileName, final String directory) throws SFTPushException {

		changeDirectory(directory);
		try {
			final File file = File.createTempFile(fileName, null);
			final FileOutputStream output = new FileOutputStream(file);

			download(fileName, output);
			output.close();

			return file;
		} catch (final SFTPushException ae) {
			throw ae;
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * Check if file exist in FTP folder
	 *
	 * @throws SFTPushException
	 */
	public boolean fileExists(final String filename, final String directory) throws SFTPushException {

		changeDirectory(directory);

		try {
			return (getFtpClient().listFiles(filename).length > 0);
		} catch (final IOException ioe) {
			return false;
		}
	}

	/**
	 * Close FTP connection.
	 *
	 * @throws SFTPushException
	 */
	public void quit() throws SFTPushException {
		try {
			if (getFtpClient() != null) {
				logger.info("Quit connection");
				getFtpClient().quit();
			}
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_QUIT_CONNECTION, e.getMessage()));
		}
	}

	/**
	 * Execute download of FTP file and put in local folder
	 *
	 * @throws SFTPushException
	 */
	public void upload(final String fileContent, final String fileName, final String directory) throws SFTPushException {
		try {
			this.upload(new ByteArrayInputStream(fileContent.getBytes()), fileName, directory);
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_UPLOAD_FILE, e.getMessage()));
		}
	}

	/**
	 * Download FTP file and put in local folder
	 *
	 * @throws SFTPushException
	 */
	public void upload(final File file, final String fileName, final String directory) throws SFTPushException {
		try {
			final FileInputStream content = new FileInputStream(file);
			this.upload(content, fileName, directory);
		} catch (final Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_UPLOAD_FILE, e.getMessage()));
		} finally {
			file.deleteOnExit();
		}
	}

	/**
	 * Execute download of FTP file and put in local folder
	 * Close FTP connection
	 *
	 * @throws SFTPushException
	 */
	public void upload(final PushConfig ftpConfig, final String fileContent, final String fileName, final String directory) throws SFTPushException {
		try {
			this.ftpConfig = ftpConfig;
			this.connect();
			this.upload(fileContent, fileName, directory);
		} catch (final Exception e) {
			throw new SFTPushException(e.getMessage());
		} finally {
			quit();
		}
	}

	/**
	 * Simple upload files
	 *
	 * @throws SFTPushException
	 */
	public void upload(final InputStream content, final String fileName, final String directory) throws IOException, SFTPushException {
		upload(content, fileName, directory, null, null, false);
	}

	/**
	 * Send a file as stream to a remote server
	 *
	 * @param content          File contents
	 * @param fileName         File name to be created on server
	 * @param directory        Directory nome to be stored on server
	 * @param fileTransferMode FTP transfer mode
	 * @param fileType         FTP file format (Use binary for .zip, .tar)
	 * @param passiveMode      Enable passive mode
	 * @return True if was transferred with success. Otherwise, False.
	 * @throws IOException      File treatment error
	 * @throws SFTPushException Network connection error
	 */
	public void upload(final InputStream content, final String fileName, final String directory, final Integer fileTransferMode, final Integer fileType, final boolean passiveMode) throws IOException, SFTPushException {

		logger.info(String.format("Upload file params: fileName:%s, directory:%s, fileTransferMode:%s, fileType:%s, passiveMode:%s", fileName, directory, fileTransferMode, fileType, passiveMode));
		changeDirectory(directory);

		final FTPClient ftpClient = getFtpClient();

		if (passiveMode) {
			ftpClient.enterLocalPassiveMode();
		}

		if (fileTransferMode != null) {
			ftpClient.setFileTransferMode(fileTransferMode);
		}

		if (fileType != null) {
			ftpClient.setFileType(fileType);
		}

		final int reply = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftpClient.disconnect();
			throw new SFTPushException("Could not complete connection: Reply code: " + reply);
		}
		if (!ftpClient.storeFile(fileName, content)) {
			throw new SFTPushException("Could not upload stream: Corrupted file");
		}
	}

	/**
	 * Connect with FTP
	 * Change FTP folder
	 * Download file
	 * Close connection
	 *
	 * @throws SFTPushException
	 */
	public static File download(final PushConfig config, final String directory, final String filename) throws SFTPushException {
		final FTPushClient ftp = new FTPushClient(config);
		ftp.connect();
		try {
			return ftp.download(filename, directory);
		} finally {
			ftp.quit();
		}
	}

	/**
	 * Connect with FTP
	 * Change FTP folder
	 * Upload file
	 * Close connection
	 *
	 * @throws SFTPushException
	 */
	public static void upload(final File file, final PushConfig config, final String directory, final String filename) throws SFTPushException {
		final FTPushClient ftp = new FTPushClient(config);
		ftp.connect();
		try {
			ftp.upload(file, filename, directory);
		} finally {
			ftp.quit();
		}
	}

	public FTPClient getFtpClient() {
		return this.ftpClient;
	}

	public PushConfig getFtpConfig() {
		return this.ftpConfig;
	}
}