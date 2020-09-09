package org.thoughtcrime.securesms.util;

import java.io.FileDescriptor;

public class FileUtils {

  public static native int getFileDescriptorOwner(FileDescriptor fileDescriptor);

}
