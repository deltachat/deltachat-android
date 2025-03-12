package com.b44t.messenger.rpc;

public class EnteredLoginParam {
    // Email address.
    private final String addr;

    // IMAP settings.
    private final EnteredServerLoginParam imap;

    // SMTP settings.
    private final EnteredServerLoginParam smtp;

    // TLS options: whether to allow invalid certificates and/or
    // invalid hostnames
    private final EnteredCertificateChecks certificateChecks;

    // Proxy configuration.
    private final String proxyConfig;

    // If true, login via OAUTH2 (not recommended anymore)
    private final boolean oauth2;

    public EnteredLoginParam(
            String addr,
            EnteredServerLoginParam imap,
            EnteredServerLoginParam smtp,
            EnteredCertificateChecks certificateChecks,
            String proxyConfig,
            boolean oauth2
    ) {
        this.addr = addr;
        this.imap = imap;
        this.smtp = smtp;
        this.certificateChecks = certificateChecks;
        this.proxyConfig = proxyConfig;
        this.oauth2 = oauth2;
    }

    public String getAddr() {
        return addr;
    }

    public EnteredServerLoginParam getImap() {
        return imap;
    }

    public EnteredServerLoginParam getSmtp() {
        return smtp;
    }

    public EnteredCertificateChecks getCertificateChecks() {
        return certificateChecks;
    }

    public String getProxyConfig() {
        return proxyConfig;
    }

    public boolean isOauth2() {
        return oauth2;
    }

    public enum EnteredCertificateChecks {
        automatic, strict, acceptInvalidCertificates,
    }
}