package com.axsoftware.sftpush.client.ftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.axsoftware.sftpush.config.PushConfig;
import com.axsoftware.sftpush.exception.SFTPushException;

public final class FTPushClient {

	private static final Logger logger = Logger.getLogger(FTPushClient.class.getName()); 
	
	private static final String DOT = ".";
	private static final String ERROR_CHANGE_FOLDER 	= "Error on select folder %s. ( %s )";
	private static final String ERROR_CONNECT_FTP 		= "Error on connect FTP %s.( %s )";
	private static final String ERROR_REMOVE_FILE 		= "Error on remove file.( %s )";
	private static final String ERROR_DOWNLOAD_FILE 	= "Error on download file.( %s )";
	private static final String ERROR_AUTHENTICATE_USER = "Error in authenticate FTP user ( %s )";
	private static final String ERROR_FILE_NOT_FOUND 	= "File %s not found.";
	private static final String ERROR_QUIT_CONNECTION 	= "Error on close connection.( %s )";
	private static final String ERROR_UPLOAD_FILE 		= "Error on upload file.( %s )";

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
		
		if(directory != null){
			logger.info(String.format("Using directory %s", directory));
			try {
				if (directory.startsWith(File.separator)) {
					getFtpClient().changeWorkingDirectory(File.separator);
				}
				
				for (final String dir : directory.split(File.separator)) {
					getFtpClient().changeWorkingDirectory(dir);
				}
			} catch (Exception e) {
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
		} catch (Exception e) {
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
		
		logger.info(String.format("Connect FTO usign params > host: %s - port: %s - username: %s - password: %s", host,port,username,password));
		
		ftpClient = new FTPClient();
		ftpClient.setConnectTimeout(5000);

		try {
			if (port == null) {
				ftpClient.connect(host);
			} else {
				ftpClient.connect(host, port);
			}
		} catch (Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_CONNECT_FTP, host, e));
		}

		try {
			ftpClient.login(username, password);
		} catch (IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_AUTHENTICATE_USER, username, e));
		}
	}

	/**
	 * Remove FTP file.
	 * 
	 * @throws SFTPushException
	 */
	public void delete(FTPFile file) throws SFTPushException {
		this.delete(file.getName());
	}

	/**
	 * Remove file.
	 * 
	 * @throws SFTPushException
	 */
	public void delete(String fileName) throws SFTPushException {
		this.delete(fileName, DOT);
	}

	/**
	 * Remove file in folder
	 * 
	 * @throws SFTPushException
	 */
	public void delete(String fileName, String directory) throws SFTPushException {
		
		logger.info(String.format("Delete file filename: %s in directory %s", fileName, directory));
		changeDirectory(directory);
		try {
			getFtpClient().deleteFile(fileName);
		} catch (IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_REMOVE_FILE, fileName, e));
		}
	}

	/**
	 * Send the file and return object 
	 *  
	 * @throws SFTPushException
	 */
	public File download(FTPFile file) throws SFTPushException {
		try {
			
			final File outputFile = File.createTempFile(file.getName(), null);
			final FileOutputStream output = new FileOutputStream(outputFile);
			
			this.download(file, output);
			output.close();

			return outputFile;
		} catch (Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_DOWNLOAD_FILE, file.getName(), e));
		}
	}

	
	/**
	 * Download the file and put in OutputStream 
	 * 
	 * @throws SFTPushException
	 */
	public void download(FTPFile file, OutputStream output) throws SFTPushException {
		this.download(file.getName(), null, output);
	}
	

	/**
	 * Download the file and put in OutputStream
	 * 
	 * @throws SFTPushException
	 */
	public void download(String fileName, OutputStream output) throws SFTPushException {
		this.download(fileName, null, output);
	}

	/**
	 * Download the file and put in OutputStream
	 * 
	 * @throws SFTPushException
	 */
	public void download(String fileName, String directory, OutputStream output) throws SFTPushException {
		
		logger.info(String.format("Download file filename: %s in directory %s", fileName, directory));
		changeDirectory(directory);

		if (!fileExists(fileName, null)) {
			throw new SFTPushException(String.format(ERROR_FILE_NOT_FOUND, fileName));
		}

		try {
			getFtpClient().retrieveFile(fileName, output);
		} catch (IOException e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_DOWNLOAD_FILE, fileName, e));
		}
	}

	/**
	 * Download the file and put in OutputStream
	 * 
	 * @throws SFTPushException
	 */
	public File download(String fileName, String directory) throws SFTPushException {
		
		changeDirectory(directory);
		try {
			final File file = File.createTempFile(fileName, null);
			final FileOutputStream output = new FileOutputStream(file);

			download(fileName, output);
			output.close();

			return file;
		} catch (SFTPushException ae) {
			throw ae;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Check if file exist in FTP folder
	 * 
	 * @throws SFTPushException
	 */
	public boolean fileExists(String filename, String directory) throws SFTPushException {
		
		changeDirectory(directory);

		try {
			return (getFtpClient().listFiles(filename).length > 0);
		} catch (IOException ioe) {
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
		} catch (Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_QUIT_CONNECTION, e.getMessage()));
		}
	}

	/**
	 * Execute download of FTP file and put in local folder  
	 * 
	 * @throws SFTPushException
	 */
	public void upload(String fileContent, String fileName, String directory) throws SFTPushException {
		try {
			this.upload(new ByteArrayInputStream(fileContent.getBytes()), fileName, directory);
		} catch (Exception e) {
			throw new SFTPushException(String.format(FTPushClient.ERROR_UPLOAD_FILE, e.getMessage()));
		}
	}

	/**
	 * Download FTP file and put in local folder
	 *  
	 * @throws SFTPushException
	 */
	public void upload(File file, String fileName, String directory) throws SFTPushException {
		try {
			final FileInputStream content = new FileInputStream(file);
			this.upload(content, fileName, directory);
		} catch (Exception e) {
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
	public void upload(PushConfig ftpConfig, String fileContent, String fileName, String directory) throws SFTPushException {
		try {
			this.ftpConfig = ftpConfig;
			this.connect();
			this.upload(fileContent, fileName, directory);
		} catch (Exception e) {
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
	public void upload(InputStream content, String fileName, String directory) throws IOException, SFTPushException {
		upload(content, fileName, directory, null, null, false);
	}

	public void upload(InputStream content, String fileName, String directory, Integer fileTransferMode, Integer fileType, boolean passiveMode) throws IOException, SFTPushException {

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

		int reply = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftpClient.disconnect();
		}
		ftpClient.storeFile(fileName, content);
	}

	/**
	 * Connect with FTP
	 * Change FTP folder
	 * Download file
	 * Close connection
	 * 
	 * @throws SFTPushException
	 */
	public static File download(PushConfig config, String directory, String filename) throws SFTPushException {
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
	public static void upload(File file, PushConfig config, String directory, String filename) throws SFTPushException {
		final FTPushClient ftp = new FTPushClient(config);
		ftp.connect();
		try {
			ftp.upload(file, filename, directory);
		} finally {
			ftp.quit();
		}
	}

	public FTPClient getFtpClient() {
		return ftpClient;
	}

	public PushConfig getFtpConfig() {
		return ftpConfig;
	}
}