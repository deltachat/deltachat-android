package com.b44t.messenger.rpc;

public class EnteredServerLoginParam {
    // Server hostname or IP address.
    private final String server;

    // Server port.
    //
    // 0 if not specified.
    private final int port;

    // Socket security.
    private final SocketSecurity security;

    // Username.
    //
    // Empty string if not specified.
    private final String user;

    // Password.
    private final String password;

    public EnteredServerLoginParam(String server, int port, SocketSecurity security, String user, String password) {
        this.server = server;
        this.port = port;
        this.security = security;
        this.user = user;
        this.password = password;
    }

    public String getServer() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public SocketSecurity getSecurity() {
        return security;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public enum SocketSecurity {
        // Unspecified socket security, select automatically.
        automatic,

        // TLS connection.
        ssl,

        // STARTTLS connection.
        starttls,

        // No TLS, plaintext connection.
        plain,
    }
}
