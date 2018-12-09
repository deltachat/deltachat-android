package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.attachments.DatabaseAttachment;

import java.util.List;

public class MediaDatabase extends Database {

  MediaDatabase(Context context) {
    super(context);
  }

  public static class MediaRecord {

    private final DatabaseAttachment attachment;
    private final Address            address;
    private final long               date;
    private final boolean            outgoing;

    private MediaRecord(DatabaseAttachment attachment, @Nullable Address address, long date, boolean outgoing) {
      this.attachment = attachment;
      this.address    = address;
      this.date       = date;
      this.outgoing   = outgoing;
    }

    public static MediaRecord from(@NonNull Context context, @NonNull Cursor cursor) {
      //AttachmentDatabase       attachmentDatabase = DatabaseFactory.getAttachmentDatabase(context);
      List<DatabaseAttachment> attachments        = null;//attachmentDatabase.getAttachment(cursor);
      String                   serializedAddress  = null;//cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS));
      boolean                  outgoing           = false; //MessagingDatabase.Types.isOutgoingMessageType(cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX)));
      Address                  address            = null;

      if (serializedAddress != null) {
        address = Address.fromSerialized(serializedAddress);
      }

      long date;

      if (false) {
        date = cursor.getLong(cursor.getColumnIndexOrThrow(null));
      } else {
        date = cursor.getLong(cursor.getColumnIndexOrThrow(null));
      }

      return new MediaRecord(attachments != null && attachments.size() > 0 ? attachments.get(0) : null, address, date, outgoing);
    }

    public DatabaseAttachment getAttachment() {
      return attachment;
    }

    public String getContentType() {
      return attachment.getContentType();
    }

    public @Nullable Address getAddress() {
      return address;
    }

    public long getDate() {
      return date;
    }

    public boolean isOutgoing() {
      return outgoing;
    }

  }


}
