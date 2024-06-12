package org.thoughtcrime.securesms.attachments;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.database.AttachmentDatabase;

public class DcAttachment extends Attachment {

  private final DcMsg dcMsg;

  public DcAttachment(DcMsg dcMsg) {
    super(dcMsg.getFilemime(), AttachmentDatabase.TRANSFER_PROGRESS_DONE, dcMsg.getFilebytes(),
        dcMsg.getFilename(),
        Uri.fromFile(new File(dcMsg.getFile())).toString(),
        null, dcMsg.getType() == DcMsg.DC_MSG_VOICE,
        0, 0);
    this.dcMsg = dcMsg;
  }

  @Nullable
  @Override
  public Uri getDataUri() {
    return Uri.fromFile(new File(dcMsg.getFile()));
  }

  @Nullable
  @Override
  public Uri getThumbnailUri() {
    if(dcMsg.getType()==DcMsg.DC_MSG_VIDEO) {
      return Uri.fromFile(new File(dcMsg.getFile()+"-preview.jpg"));
    }
    return getDataUri();
  }

  @Override
  public String getRealPath(Context context) {
    return dcMsg.getFile();
  }
}
