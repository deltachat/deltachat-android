/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;

public class FileLoadOperation {

    private static class RequestInfo {
        private int requestToken;
        private int offset;
    }

    private final static int stateIdle = 0;
    private final static int stateDownloading = 1;
    private final static int stateFailed = 2;
    private final static int stateFinished = 3;

    private final static int downloadChunkSize = 1024 * 32;
    private final static int downloadChunkSizeBig = 1024 * 128;
    private final static int maxDownloadRequests = 4;
    private final static int maxDownloadRequestsBig = 2;
    private final static int bigFileSizeFrom = 1024 * 1024;

    private int datacenter_id;
    private TLRPC.InputFileLocation location;
    private volatile int state = stateIdle;
    private int downloadedBytes;
    private int totalBytesCount;
    private int bytesCountPadding;
    private FileLoadOperationDelegate delegate;
    private byte[] key;
    private byte[] iv;
    private int currentDownloadChunkSize;
    private int currentMaxDownloadRequests;
    private int requestsCount;
    private int renameRetryCount;

    private int nextDownloadOffset;
    private ArrayList<RequestInfo> requestInfos;
    private ArrayList<RequestInfo> delayedRequestInfos;

    private File cacheFileTemp;
    private File cacheFileFinal;
    private File cacheIvTemp;

    private String ext;
    private RandomAccessFile fileOutputStream;
    private RandomAccessFile fiv;
    private File storePath;
    private File tempPath;
    private boolean isForceRequest;

    public interface FileLoadOperationDelegate {
        void didFinishLoadingFile(FileLoadOperation operation, File finalFile);
        void didFailedLoadingFile(FileLoadOperation operation, int state);
        void didChangedLoadProgress(FileLoadOperation operation, float progress);
    }

    public FileLoadOperation(TLRPC.FileLocation photoLocation, String extension, int size) {
        /*if (photoLocation instanceof TLRPC.TL_fileEncryptedLocation) {
            location = new TLRPC.TL_inputEncryptedFileLocation();
            location.id = photoLocation.volume_id;
            location.volume_id = photoLocation.volume_id;
            location.access_hash = photoLocation.secret;
            location.local_id = photoLocation.local_id;
            iv = new byte[32];
            System.arraycopy(photoLocation.iv, 0, iv, 0, iv.length);
            key = photoLocation.key;
            datacenter_id = photoLocation.dc_id;
        } else*/ if (photoLocation instanceof TLRPC.TL_fileLocation) {
            location = new TLRPC.TL_inputFileLocation();
            location.volume_id = photoLocation.volume_id;
            location.local_id = photoLocation.local_id;
            datacenter_id = photoLocation.dc_id;
        }
        totalBytesCount = size;
        ext = extension != null ? extension : "jpg";
    }

    public FileLoadOperation(TLRPC.Document documentLocation) {
        try {
            /*if (documentLocation instanceof TLRPC.TL_documentEncrypted) {
                location = new TLRPC.TL_inputEncryptedFileLocation();
                location.id = documentLocation.id;
                location.access_hash = documentLocation.access_hash;
                datacenter_id = documentLocation.dc_id;
                iv = new byte[32];
                System.arraycopy(documentLocation.iv, 0, iv, 0, iv.length);
                key = documentLocation.key;
            } else*/ if (documentLocation instanceof TLRPC.TL_document) {
                location = new TLRPC.TL_inputDocumentFileLocation();
                location.id = documentLocation.id;
                location.access_hash = documentLocation.access_hash;
                datacenter_id = documentLocation.dc_id;
            }
            totalBytesCount = documentLocation.size;
            if (key != null) {
                int toAdd = 0;
                if (totalBytesCount % 16 != 0) {
                    bytesCountPadding = 16 - totalBytesCount % 16;
                    totalBytesCount += bytesCountPadding;
                }
            }
            ext = FileLoader.getDocumentFileName(documentLocation);
            int idx;
            if (ext == null || (idx = ext.lastIndexOf('.')) == -1) {
                ext = "";
            } else {
                ext = ext.substring(idx);
            }
            if (ext.length() <= 1) {
                if (documentLocation.mime_type != null) {
                    switch (documentLocation.mime_type) {
                        case "video/mp4":
                            ext = ".mp4";
                            break;
                        case "audio/ogg":
                            ext = ".ogg";
                            break;
                        default:
                            ext = "";
                            break;
                    }
                } else {
                    ext = "";
                }
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
            state = stateFailed;
            cleanup();
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                }
            });
        }
    }

    public void setForceRequest(boolean forceRequest) {
        isForceRequest = forceRequest;
    }

    public boolean isForceRequest() {
        return isForceRequest;
    }

    public void setPaths(File store, File temp) {
        storePath = store;
        tempPath = temp;
    }

    public void start() {
        if (state != stateIdle) {
            return;
        }
        currentDownloadChunkSize = totalBytesCount >= bigFileSizeFrom ? downloadChunkSizeBig : downloadChunkSize;
        currentMaxDownloadRequests = totalBytesCount >= bigFileSizeFrom ? maxDownloadRequestsBig : maxDownloadRequests;
        requestInfos = new ArrayList<>(currentMaxDownloadRequests);
        delayedRequestInfos = new ArrayList<>(currentMaxDownloadRequests - 1);
        state = stateDownloading;
        if (location == null) {
            cleanup();
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                }
            });
            return;
        }
        String fileNameFinal;
        String fileNameTemp;
        String fileNameIv = null;
        if (location.volume_id != 0 && location.local_id != 0) {
            fileNameTemp = location.volume_id + "_" + location.local_id + ".temp";
            fileNameFinal = location.volume_id + "_" + location.local_id + "." + ext;
            if (key != null) {
                fileNameIv = location.volume_id + "_" + location.local_id + ".iv";
            }
            if (datacenter_id == Integer.MIN_VALUE || location.volume_id == Integer.MIN_VALUE || datacenter_id == 0) {
                cleanup();
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                    }
                });
                return;
            }
        } else {
            fileNameTemp = datacenter_id + "_" + location.id + ".temp";
            fileNameFinal = datacenter_id + "_" + location.id + ext;
            if (key != null) {
                fileNameIv = datacenter_id + "_" + location.id + ".iv";
            }
            if (datacenter_id == 0 || location.id == 0) {
                cleanup();
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                    }
                });
                return;
            }
        }

        cacheFileFinal = new File(storePath, fileNameFinal);
        boolean exist = cacheFileFinal.exists();
        if (exist && totalBytesCount != 0 && totalBytesCount != cacheFileFinal.length()) {
            cacheFileFinal.delete();
        }

        if (!cacheFileFinal.exists()) {
            cacheFileTemp = new File(tempPath, fileNameTemp);
            if (cacheFileTemp.exists()) {
                downloadedBytes = (int) cacheFileTemp.length();
                nextDownloadOffset = downloadedBytes = downloadedBytes / currentDownloadChunkSize * currentDownloadChunkSize;
            }

            if (BuildVars.DEBUG_VERSION) {
                FileLog.d("messenger", "start loading file to temp = " + cacheFileTemp + " final = " + cacheFileFinal);
            }

            if (fileNameIv != null) {
                cacheIvTemp = new File(tempPath, fileNameIv);
                try {
                    fiv = new RandomAccessFile(cacheIvTemp, "rws");
                    long len = cacheIvTemp.length();
                    if (len > 0 && len % 32 == 0) {
                        fiv.read(iv, 0, 32);
                    } else {
                        downloadedBytes = 0;
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                    downloadedBytes = 0;
                }
            }
            try {
                fileOutputStream = new RandomAccessFile(cacheFileTemp, "rws");
                if (downloadedBytes != 0) {
                    fileOutputStream.seek(downloadedBytes);
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            if (fileOutputStream == null) {
                cleanup();
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                    }
                });
                return;
            }
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (totalBytesCount != 0 && downloadedBytes == totalBytesCount) {
                        try {
                            onFinishLoadingFile();
                        } catch (Exception e) {
                            delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                        }
                    } else {
                        startDownloadRequest();
                    }
                }
            });
        } else {
            try {
                onFinishLoadingFile();
            } catch (Exception e) {
                delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
            }
        }
    }

    public void cancel() {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (state == stateFinished || state == stateFailed) {
                    return;
                }
                state = stateFailed;
                cleanup();
                if (requestInfos != null) {
                    for (int a = 0; a < requestInfos.size(); a++) {
                        RequestInfo requestInfo = requestInfos.get(a);
                        if (requestInfo.requestToken != 0) {
                            ConnectionsManager.getInstance().cancelRequest(requestInfo.requestToken, true);
                        }
                    }
                }
                delegate.didFailedLoadingFile(FileLoadOperation.this, 1);
            }
        });
    }

    private void cleanup() {
        try {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.getChannel().close();
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                fileOutputStream.close();
                fileOutputStream = null;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }

        try {
            if (fiv != null) {
                fiv.close();
                fiv = null;
            }
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        if (delayedRequestInfos != null) {
            for (int a = 0; a < delayedRequestInfos.size(); a++) {
                RequestInfo requestInfo = delayedRequestInfos.get(a);
                /*if (requestInfo.response != null) {
                    requestInfo.response.disableFree = false;
                    requestInfo.response.freeResources();
                }*/
            }
            delayedRequestInfos.clear();
        }
    }

    private void onFinishLoadingFile() throws Exception {
        if (state != stateDownloading) {
            return;
        }
        state = stateFinished;
        cleanup();
        if (cacheIvTemp != null) {
            cacheIvTemp.delete();
            cacheIvTemp = null;
        }
        if (cacheFileTemp != null) {
            boolean renameResult = cacheFileTemp.renameTo(cacheFileFinal);
            if (!renameResult) {
                if (BuildVars.DEBUG_VERSION) {
                    FileLog.e("messenger", "unable to rename temp = " + cacheFileTemp + " to final = " + cacheFileFinal + " retry = " + renameRetryCount);
                }
                renameRetryCount++;
                if (renameRetryCount < 3) {
                    state = stateDownloading;
                    Utilities.stageQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                onFinishLoadingFile();
                            } catch (Exception e) {
                                delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                            }
                        }
                    }, 200);
                    return;
                }
                cacheFileFinal = cacheFileTemp;
            }
        }
        if (BuildVars.DEBUG_VERSION) {
            FileLog.e("messenger", "finished downloading file to " + cacheFileFinal);
        }
        delegate.didFinishLoadingFile(FileLoadOperation.this, cacheFileFinal);
    }



    private void startDownloadRequest() {
        if (state != stateDownloading || totalBytesCount > 0 && nextDownloadOffset >= totalBytesCount || requestInfos.size() + delayedRequestInfos.size() >= currentMaxDownloadRequests) {
            return;
        }
        int count = 1;
        if (totalBytesCount > 0) {
            count = Math.max(0, currentMaxDownloadRequests - requestInfos.size() - delayedRequestInfos.size());
        }

        for (int a = 0; a < count; a++) {
            if (totalBytesCount > 0 && nextDownloadOffset >= totalBytesCount) {
                break;
            }
            boolean isLast = totalBytesCount <= 0 || a == count - 1 || totalBytesCount > 0 && nextDownloadOffset + currentDownloadChunkSize >= totalBytesCount;
            //TLRPC.TL_upload_getFile req = new TLRPC.TL_upload_getFile();
            //req.location = location;
            //req.offset = nextDownloadOffset;
            //req.limit = currentDownloadChunkSize;
            nextDownloadOffset += currentDownloadChunkSize;

            final RequestInfo requestInfo = new RequestInfo();
            requestInfos.add(requestInfo);
            requestInfo.offset = nextDownloadOffset;
            requestInfo.requestToken = 0;/*ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {
                    requestInfo.response = (TLRPC.TL_upload_file) response;
                    processRequestResult(requestInfo, error);
                }
            }, null, (isForceRequest ? ConnectionsManager.RequestFlagForceDownload : 0) | ConnectionsManager.RequestFlagFailOnServerErrors, datacenter_id, requestsCount % 2 == 0 ? ConnectionsManager.ConnectionTypeDownload : ConnectionsManager.ConnectionTypeDownload2, isLast);*/
            requestsCount++;
        }
    }

    public void setDelegate(FileLoadOperationDelegate delegate) {
        this.delegate = delegate;
    }
}
