/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.b44t.messenger;

import java.io.FileInputStream;

public class FileUploadOperation {

    public int state = 0;
    public FileUploadOperationDelegate delegate;
    private int requestToken = 0;
    private long totalFileSize = 0;
    private boolean isEncrypted = false;
    private int estimatedSize = 0;
    private FileInputStream stream;

    public interface FileUploadOperationDelegate {
        void didFinishUploadingFile(FileUploadOperation operation, TLRPC.InputFile inputFile, TLRPC.InputEncryptedFile inputEncryptedFile, byte[] key, byte[] iv);
        void didFailedUploadingFile(FileUploadOperation operation);
        void didChangedUploadProgress(FileUploadOperation operation, float progress);
    }

    public FileUploadOperation(String location, boolean encrypted, int estimated) {
        isEncrypted = encrypted;
        estimatedSize = estimated;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public void start() {
    }

    public void cancel() {
        if (state == 3) {
            return;
        }
        state = 2;
        if (requestToken != 0) {
            ConnectionsManager.getInstance().cancelRequest(requestToken, true);
        }
        delegate.didFailedUploadingFile(this);
        cleanup();
    }

    private void cleanup() {
    }

    /*
    protected void checkNewDataAvailable(final long finalSize) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (estimatedSize != 0 && finalSize != 0) {
                    estimatedSize = 0;
                    totalFileSize = finalSize;
                    totalPartsCount = (int) (totalFileSize + uploadChunkSize - 1) / uploadChunkSize;
                    if (started) {
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("uploadinfo", Activity.MODE_PRIVATE);
                        storeFileUploadInfo(preferences);
                    }
                }
                if (requestToken == 0) {
                    startUploadRequest();
                }
            }
        });
    }
    */


}
