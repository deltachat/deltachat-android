package com.b44t.messenger.rpc;

public class EnteredLoginParam {
    // Email address.
    private final String addr;

    // Password.
    private final String password;

    // ============ IMAP settings ============

    // Server hostname or IP address.
    private final String imapServer;

    // Server port.
    private final int imapPort;

    // Socket security.
    private final SocketSecurity imapSecurity;

    // Username.
    private final String imapUser;

    // ============ SMTP settings ============

    // Server hostname or IP address.
    private final String smtpServer;

    // Server port.
    private final int smtpPort;

    // Socket security.
    private final SocketSecurity smtpSecurity;

    // Username.
    private final String smtpUser;

    // SMTP Password. Only needs to be specified if different than IMAP password.
    private final String smtpPassword;

    // TLS options: whether to allow invalid certificates and/or
    // invalid hostnames
    private final EnteredCertificateChecks certificateChecks;

    // If true, login via OAUTH2 (not recommended anymore)
    private final boolean oauth2;

    public EnteredLoginParam(String addr,
                             String password,
                             String imapServer,
                             int imapPort,
                             SocketSecurity imapSecurity,
                             String imapUser,
                             String smtpServer,
                             int smtpPort,
                             SocketSecurity smtpSecurity,
                             String smtpUser,
                             String smtpPassword,
                             EnteredCertificateChecks certificateChecks,
                             boolean oauth2) {
        this.addr = addr;
        this.password = password;
        this.imapServer = imapServer;
        this.imapPort = imapPort;
        this.imapSecurity = imapSecurity;
        this.imapUser = imapUser;
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpSecurity = smtpSecurity;
        this.smtpUser = smtpUser;
        this.smtpPassword = smtpPassword;
        this.certificateChecks = certificateChecks;
        this.oauth2 = oauth2;
    }

    public String getAddr() {
        return addr;
    }

    public String getPassword() {
        return password;
    }

    public String getImapServer() {
        return imapServer;
    }

    public int getImapPort() {
        return imapPort;
    }

    public SocketSecurity getImapSecurity() {
        return imapSecurity;
    }

    public String getImapUser() {
        return imapUser;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public SocketSecurity getSmtpSecurity() {
        return smtpSecurity;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public EnteredCertificateChecks getCertificateChecks() {
        return certificateChecks;
    }

    public boolean isOauth2() {
        return oauth2;
    }

    public enum EnteredCertificateChecks {
        automatic, strict, acceptInvalidCertificates,
    }

    public static EnteredCertificateChecks certificateChecksFromInt(int position) {
        switch (position) {
            case 0:
                return EnteredCertificateChecks.automatic;
            case 1:
                return EnteredCertificateChecks.strict;
            case 2:
                return EnteredCertificateChecks.acceptInvalidCertificates;
        }
        throw new IllegalArgumentException("Invalid certificate position: " + position);
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

    public static SocketSecurity socketSecurityFromInt(int position) {
        switch (position) {
            case 0:
                return SocketSecurity.automatic;
            case 1:
                return SocketSecurity.ssl;
            case 2:
                return SocketSecurity.starttls;
            case 3:
                return SocketSecurity.plain;
        }
        throw new IllegalArgumentException("Invalid socketSecurity position: " + position);
    }
}
