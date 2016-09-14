package com.axsoftware.sftpush.config;

public class PushConfig {

	private String host;

	private String username;

	private String password;

	private String ppk;

	private Integer port = 22;

	private Integer connectTimeout;

	public PushConfig(String username, String host, String password, String ppk, Integer port) {
		this.username = username;
		this.host = host;
		this.password = password;
		this.ppk = ppk;
		this.port = port;
	}

	public PushConfig(String username, String host, String password, String ppk) {
		this(username, host, password, ppk, 22);
	}

	public PushConfig(String username, String host, String ppk) {
		this(username, host, null, ppk, 22);
	}

	public PushConfig(String host, Integer port, String username, String password) {
		this(username, host, password, null, 21);
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPpk() {
		return ppk;
	}

	public void setPpk(String ppk) {
		this.ppk = ppk;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
