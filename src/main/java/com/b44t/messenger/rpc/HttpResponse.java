package com.b44t.messenger.rpc;

import android.util.Base64;

public class HttpResponse {
    // base64-encoded response body.
    private final String blob;
    // MIME type, e.g. "text/plain" or "text/html".
    private final String mimetype;
    // Encoding, e.g. "utf-8".
    private final String encoding;

    public HttpResponse(String blob, String mimetype, String encoding) {
        this.blob = blob;
        this.mimetype = mimetype;
        this.encoding = encoding;
    }

    public byte[] getBlob() {
        if (blob == null) {
            return null;
        }
        return Base64.decode(blob, Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getEncoding() {
        return encoding;
    }
}
