package com.axsoftware.sftpush.client.ftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.axsoftware.sftpush.config.ConfigTest;

import br.com.calcard.calintegrador.exception.IntegracaoException;

public final class FTP {

	private static final String ERROR_CD = "Erro ao alterar diretório atual para %s. ( %s )";
	private static final String ERROR_CONNECT = "Erro ao abrir conexão com endereço %s.( %s )";
	private static final String ERROR_DELETE = "Erro ao tentar excluir o arquivo informado.( %s )";
	private static final String ERROR_DOWNLOAD = "Erro ao efetuar download do arquivo informado.( %s )";
	private static final String ERROR_LOGIN = "Erro ao efetuar login.( %s )";
	private static final String ERROR_NOT_FOUND = "Arquivo %s não encontrado no FTP.";
	private static final String ERROR_QUIT = "Erro ao fechar conexão.( %s )";
	private static final String ERROR_UPLOAD = "Erro ao efetuar upload do conteúdo informado.( %s )";

	private FTPClient ftpClient;
	private ConfigTest ftpConfig;

	public FTP() {
	}

	public FTP(ConfigTest ftpConfig) {
		this.ftpConfig = ftpConfig;
	}

	/**
	 * Altera diretório atual na conexão de FTP informada.
	 * 
	 * @throws IntegracaoException
	 */
	public void changeDirectory(String directory) throws IntegracaoException {
		try {
			if (directory.startsWith("/")) {
				getFtpClient().changeWorkingDirectory("/");
			}

			for (String dir : directory.split("/")) {
				getFtpClient().changeWorkingDirectory(dir);
			}
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_CD, directory, e.getMessage()));
		}
	}

	/**
	 * Lista os arquivos do diretório.
	 * 
	 * @return
	 * @throws IntegracaoException
	 */
	public FTPFile[] listFiles() throws IntegracaoException {
		try {
			return getFtpClient().listFiles();
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_CD, e.getMessage()));
		}
	}

	/**
	 * Efetua conexão no FTP com base nas conifgurações do objeto FTPConfig.
	 * 
	 * @throws IntegracaoException
	 */
	public void connect() throws IntegracaoException {
		this.connect(getFtpConfig().getAddress(), getFtpConfig().getPort(), getFtpConfig().getUsername(), getFtpConfig().getPassword());
	}

	/**
	 * Efetua conexão no FTP considerando a porta padrão.
	 * 
	 * @throws IntegracaoException
	 */
	public void connect(String address, String username, String password) throws IntegracaoException {
		this.connect(address, null, username, password);
	}

	/**
	 * Efetua conexão no FTP conforme parâmetros indicados.
	 * 
	 * @throws IntegracaoException
	 */
	public void connect(String address, Integer port, String username, String password) throws IntegracaoException {
		// cria objeto para conexão
		ftpClient = new FTPClient();
		ftpClient.setConnectTimeout(5000);

		// conecta ao ftp
		try {
			if (port == null) {
				ftpClient.connect(address);
			} else {
				ftpClient.connect(address, port);
			}
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_CONNECT, address, e.getMessage()));
		}

		// efetua login no ftp
		try {
			ftpClient.login(username, password);
		} catch (IOException e) {
			throw new IntegracaoException(String.format(FTP.ERROR_LOGIN, e.getMessage()));
		}
	}

	/**
	 * Realiza a exclusão do arquivo informado.
	 * 
	 * @throws IntegracaoException
	 */
	public void delete(FTPFile file) throws IntegracaoException {
		this.delete(file.getName());
	}

	/**
	 * Realiza a exclusão do arquivo informado.
	 * 
	 * @throws IntegracaoException
	 */
	public void delete(String fileName) throws IntegracaoException {
		this.delete(fileName, ".");
	}

	/**
	 * Realiza a exclusão do arquivo informado. Se <strong>directory</strong>
	 * não contiver valor, a operação é realizada na pasta atual do FTP.
	 * 
	 * @throws IntegracaoException
	 */
	public void delete(String fileName, String directory) throws IntegracaoException {
		if (StringUtils.isNotEmpty(directory)) {
			changeDirectory(directory);
		}

		try {
			getFtpClient().deleteFile(fileName);
		} catch (IOException e) {
			throw new IntegracaoException(String.format(FTP.ERROR_DELETE, e.getMessage()));
		}
	}

	/**
	 * Efetua o download e retorna o conteúdo do arquivo informado, fechando o
	 * stream de saída.
	 * 
	 * @throws IntegracaoException
	 */
	public File download(FTPFile file) throws IntegracaoException {
		try {
			// cria arquivo temporário e stream para o download
			File outputFile = File.createTempFile(file.getName(), null);
			FileOutputStream output = new FileOutputStream(outputFile);

			// efetua o download do arquivo e fecha o stream de saída
			this.download(file, output);
			output.close();

			return outputFile;
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_DOWNLOAD, e.getMessage()));
		}
	}

	/**
	 * Efetua o download do arquivo <strong>file</strong>, jogando sua saída
	 * para <strong>output</strong>.
	 * 
	 * @throws IntegracaoException
	 */
	public void download(FTPFile file, OutputStream output) throws IntegracaoException {
		this.download(file.getName(), null, output);
	}

	/**
	 * Efetua o download do arquivo de nome <strong>fileName</strong>, jogando
	 * sua saída para <strong>output</strong>.
	 * 
	 * @throws IntegracaoException
	 */
	public void download(String fileName, OutputStream output) throws IntegracaoException {
		this.download(fileName, null, output);
	}

	/**
	 * Efetua o download do arquivo informado, "escrevendo" seu conteúdo no
	 * stream <strong>output</strong>. Se <strong>directory</strong> não
	 * contiver valor, a operação é realizada na pasta atual do FTP.
	 * 
	 * @throws IntegracaoException
	 */
	public void download(String fileName, String directory, OutputStream output) throws IntegracaoException {
		if (StringUtils.isNotEmpty(directory)) {
			changeDirectory(directory);
		}

		if (!fileExists(fileName, null)) {
			throw new IntegracaoException(String.format(ERROR_NOT_FOUND, fileName));
		}

		try {
			getFtpClient().retrieveFile(fileName, output);
		} catch (IOException ioe) {
			throw new IntegracaoException(String.format(FTP.ERROR_DOWNLOAD, ioe.getLocalizedMessage()));
		}
	}

	/**
	 * Efetua o download do arquivo informado, "escrevendo" seu conteúdo no
	 * stream <strong>output</strong>. Se <strong>directory</strong> não
	 * contiver valor, a operação é realizada na pasta atual do FTP.
	 * 
	 * @throws IntegracaoException
	 */
	public File download(String fileName, String directory) throws IntegracaoException {
		if (StringUtils.isNotEmpty(directory)) {
			changeDirectory(directory);
		}

		try {
			// cria arquivo temporário e stream para o download
			File outputFile = File.createTempFile(fileName, null);
			FileOutputStream output = new FileOutputStream(outputFile);

			// efetua o download do arquivo e fecha o stream de saída
			download(fileName, output);
			output.close();

			return outputFile;
		} catch (IntegracaoException ae) {
			throw ae;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Verifica se o arquivo de nome <strong>filename</strong> existe no
	 * diretório <strong>directory</strong> (ou no atual, caso este seja nulo).
	 * 
	 * @throws IntegracaoException
	 */
	public boolean fileExists(String filename, String directory) throws IntegracaoException {
		if (StringUtils.isNotEmpty(directory)) {
			changeDirectory(directory);
		}

		try {
			return (getFtpClient().listFiles(filename).length > 0);
		} catch (IOException ioe) {
			return false;
		}
	}

	/**
	 * Fecha a conexão com o FTP.
	 * 
	 * @throws IntegracaoException
	 */
	public void quit() throws IntegracaoException {
		try {
			if (getFtpClient() != null) {
				getFtpClient().quit();
			}
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_QUIT, e.getMessage()));
		}
	}

	/**
	 * Efetua o upload do conteúdo informado, criando um arquivo na pasta
	 * <strong>directory</strong> com nome <strong>fileName</strong>.
	 * 
	 * @throws IntegracaoException
	 */
	public void upload(String fileContent, String fileName, String directory) throws IntegracaoException {
		try {
			this.upload(new ByteArrayInputStream(fileContent.getBytes()), fileName, directory);
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_UPLOAD, e.getMessage()));
		}
	}

	/**
	 * Efetua o upload do arquivo informado, na pasta <strong>directory</strong>
	 * com nome <strong>fileName</strong>.
	 * 
	 * @throws IntegracaoException
	 */
	public void upload(File file, String fileName, String directory) throws IntegracaoException {
		FileInputStream content = null;

		try {
			content = new FileInputStream(file);
			this.upload(content, fileName, directory);
		} catch (Exception e) {
			throw new IntegracaoException(String.format(FTP.ERROR_UPLOAD, e.getMessage()));
		} finally {
			IOUtils.closeQuietly(content);
		}
	}

	/**
	 * Efetua o upload de um arquivo realizando e fechando a conexão neste
	 * escopo.
	 * 
	 * @throws IntegracaoException
	 */
	public void upload(ConfigTest ftpConfig, String fileContent, String fileName, String directory) throws IntegracaoException {
		try {
			this.ftpConfig = ftpConfig;
			this.connect();
			this.upload(fileContent, fileName, directory);
		} catch (Exception e) {
			throw new IntegracaoException(e.getMessage());
		} finally {
			quit();
		}
	}

	/**
	 * Efetua upload do conteúdo de arquivo infomado.
	 * 
	 * @throws IntegracaoException
	 */
	public void upload(InputStream content, String fileName, String directory) throws IOException, IntegracaoException {
		upload(content, fileName, directory, null, null, false);
	}

	public void upload(InputStream content, String fileName, String directory, Integer fileTransferMode, Integer fileType, boolean passiveMode) throws IOException, IntegracaoException {

		// altera o diretório caso tenha sido informado
		if (StringUtils.isNotEmpty(directory)) {
			changeDirectory(directory);
		}

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
			System.err.println("FTP server refused connection.");
		}
		ftpClient.storeFile(fileName, content);
	}

	/**
	 * Efetua a conexão, entra no diretório informado, faz o download do arquivo
	 * e fecha a conexão.
	 * 
	 * @throws IntegracaoException
	 */
	public static File download(ConfigTest config, String directory, String filename) throws IntegracaoException {
		FTP ftp = new FTP(config);
		ftp.connect();

		try {
			return ftp.download(filename, directory);
		} finally {
			ftp.quit();
		}
	}

	/**
	 * Efetua a conexão, entra no diretório informado, faz o upload do arquivo e
	 * fecha a conexão.
	 * 
	 * @throws IntegracaoException
	 */
	public static void upload(File file, ConfigTest config, String directory, String filename) throws IntegracaoException {
		FTP ftp = new FTP(config);
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

	public ConfigTest getFtpConfig() {
		return ftpConfig;
	}

}