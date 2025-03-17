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

    // If true, login via OAUTH2 (not recommended anymore)
    private final boolean oauth2;

    public EnteredLoginParam(
            String addr,
            EnteredServerLoginParam imap,
            EnteredServerLoginParam smtp,
            EnteredCertificateChecks certificateChecks,
            boolean oauth2
    ) {
        this.addr = addr;
        this.imap = imap;
        this.smtp = smtp;
        this.certificateChecks = certificateChecks;
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

    public boolean isOauth2() {
        return oauth2;
    }

    public enum EnteredCertificateChecks {
        automatic, strict, acceptInvalidCertificates,
    }

    public static EnteredCertificateChecks getCertificateChecks(int position) {
        switch (position) {
            case 0: return EnteredCertificateChecks.automatic;
            case 1: return EnteredCertificateChecks.strict;
            case 2: return EnteredCertificateChecks.acceptInvalidCertificates;
        }
        throw new IllegalArgumentException("Invalid certificate position: " + position);
    }
}
