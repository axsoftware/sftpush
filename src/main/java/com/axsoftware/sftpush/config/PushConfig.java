package com.axsoftware.sftpush.config;

public class PushConfig {

	private String host;

	private String username;

	private String password;

	private String ppk;

	private Integer port = 22;

	private Integer connectTimeout;

	public PushConfig(final String host, final String username, final String password, final String ppk, final Integer port) {
		this.username = username;
		this.host = host;
		this.password = password;
		this.ppk = ppk;
		this.port = port;
	}

	public PushConfig(final String host, final String username, final String password, final String ppk) {
		this(host, username, password, ppk, 22);
	}

	public PushConfig(final String host, final String username, final String ppk) {
		this(host, username,  null, ppk, 22);
	}

	public PushConfig(final String host, final String username, final String password, final Integer port) {
		this(host, username, password, null, port);
	}
	
	public String getHost() {
		return this.host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getPpk() {
		return this.ppk;
	}

	public void setPpk(final String ppk) {
		this.ppk = ppk;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}

	public Integer getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(final Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
