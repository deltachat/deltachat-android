/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.android.mms.pdu_alt.PduHeaders;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.attachments.DatabaseAttachment;
import org.thoughtcrime.securesms.attachments.MmsNotificationAttachment;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.database.documents.NetworkFailure;
import org.thoughtcrime.securesms.database.documents.NetworkFailureList;
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper;
import org.thoughtcrime.securesms.database.model.MediaMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.NotificationMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.Quote;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thoughtcrime.securesms.contactshare.Contact.Avatar;
import static org.thoughtcrime.securesms.contactshare.Contact.deserialize;

public class MmsDatabase extends MessagingDatabase {

  private static final String TAG = MmsDatabase.class.getSimpleName();

  public  static final String TABLE_NAME         = "mms";
          static final String DATE_SENT          = "date";
          static final String DATE_RECEIVED      = "date_received";
  public  static final String MESSAGE_BOX        = "msg_box";
          static final String CONTENT_LOCATION   = "ct_l";
          static final String EXPIRY             = "exp";
  public  static final String MESSAGE_TYPE       = "m_type";
          static final String MESSAGE_SIZE       = "m_size";
          static final String STATUS             = "st";
          static final String TRANSACTION_ID     = "tr_id";
          static final String PART_COUNT         = "part_count";
          static final String NETWORK_FAILURE    = "network_failures";

          static final String QUOTE_ID         = "quote_id";
          static final String QUOTE_AUTHOR     = "quote_author";
          static final String QUOTE_BODY       = "quote_body";
          static final String QUOTE_ATTACHMENT = "quote_attachment";

          static final String SHARED_CONTACTS  = "shared_contacts";

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, "                          +
    THREAD_ID + " INTEGER, " + DATE_SENT + " INTEGER, " + DATE_RECEIVED + " INTEGER, " + MESSAGE_BOX + " INTEGER, " +
    READ + " INTEGER DEFAULT 0, " + "m_id" + " TEXT, " + "sub" + " TEXT, "                +
    "sub_cs" + " INTEGER, " + BODY + " TEXT, " + PART_COUNT + " INTEGER, "               +
    "ct_t" + " TEXT, " + CONTENT_LOCATION + " TEXT, " + ADDRESS + " TEXT, "               +
    ADDRESS_DEVICE_ID + " INTEGER, "                                                            +
    EXPIRY + " INTEGER, " + "m_cls" + " TEXT, " + MESSAGE_TYPE + " INTEGER, "             +
    "v" + " INTEGER, " + MESSAGE_SIZE + " INTEGER, " + "pri" + " INTEGER, "          +
    "rr" + " INTEGER, " + "rpt_a" + " INTEGER, " + "resp_st" + " INTEGER, " +
    STATUS + " INTEGER, " + TRANSACTION_ID + " TEXT, " + "retr_st" + " INTEGER, "         +
    "retr_txt" + " TEXT, " + "retr_txt_cs" + " INTEGER, " + "read_status" + " INTEGER, "    +
    "ct_cls" + " INTEGER, " + "resp_txt" + " TEXT, " + "d_tm" + " INTEGER, "     +
    DELIVERY_RECEIPT_COUNT + " INTEGER DEFAULT 0, " + MISMATCHED_IDENTITIES + " TEXT DEFAULT NULL, "     +
    NETWORK_FAILURE + " TEXT DEFAULT NULL," + "d_rpt" + " INTEGER, " +
    SUBSCRIPTION_ID + " INTEGER DEFAULT -1, " + EXPIRES_IN + " INTEGER DEFAULT 0, " +
    EXPIRE_STARTED + " INTEGER DEFAULT 0, " + NOTIFIED + " INTEGER DEFAULT 0, " +
    READ_RECEIPT_COUNT + " INTEGER DEFAULT 0, " + QUOTE_ID + " INTEGER DEFAULT 0, " +
    QUOTE_AUTHOR + " TEXT, " + QUOTE_BODY + " TEXT, " + QUOTE_ATTACHMENT + " INTEGER DEFAULT -1, " +
    SHARED_CONTACTS + " TEXT);";

  public static final String[] CREATE_INDEXS = {
    "CREATE INDEX IF NOT EXISTS mms_thread_id_index ON " + TABLE_NAME + " (" + THREAD_ID + ");",
    "CREATE INDEX IF NOT EXISTS mms_read_index ON " + TABLE_NAME + " (" + READ + ");",
    "CREATE INDEX IF NOT EXISTS mms_read_and_notified_and_thread_id_index ON " + TABLE_NAME + "(" + READ + "," + NOTIFIED + "," + THREAD_ID + ");",
    "CREATE INDEX IF NOT EXISTS mms_message_box_index ON " + TABLE_NAME + " (" + MESSAGE_BOX + ");",
    "CREATE INDEX IF NOT EXISTS mms_date_sent_index ON " + TABLE_NAME + " (" + DATE_SENT + ");",
    "CREATE INDEX IF NOT EXISTS mms_thread_date_index ON " + TABLE_NAME + " (" + THREAD_ID + ", " + DATE_RECEIVED + ");"
  };

  public MmsDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public int getMessageCountForThread(long threadId) {
    SQLiteDatabase db = databaseHelper.getReadableDatabase();
    Cursor cursor     = null;

    try {
      cursor = db.query(TABLE_NAME, new String[] {"COUNT(*)"}, THREAD_ID + " = ?", new String[] {threadId+""}, null, null, null);

      if (cursor != null && cursor.moveToFirst())
        return cursor.getInt(0);
    } finally {
      if (cursor != null)
        cursor.close();
    }

    return 0;
  }

  public long getThreadIdForMessage(long id) {
    String sql        = "SELECT " + THREAD_ID + " FROM " + TABLE_NAME + " WHERE " + ID + " = ?";
    String[] sqlArgs  = new String[] {id+""};
    SQLiteDatabase db = databaseHelper.getReadableDatabase();

    Cursor cursor = null;

    try {
      cursor = db.rawQuery(sql, sqlArgs);
      if (cursor != null && cursor.moveToFirst())
        return cursor.getLong(0);
      else
        return -1;
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  private List<Contact> getSharedContacts(@NonNull Cursor cursor, @NonNull List<DatabaseAttachment> attachments) {
    String serializedContacts = cursor.getString(cursor.getColumnIndexOrThrow(SHARED_CONTACTS));

    if (TextUtils.isEmpty(serializedContacts)) {
      return Collections.emptyList();
    }

    Map<AttachmentId, DatabaseAttachment> attachmentIdMap = new HashMap<>();
    for (DatabaseAttachment attachment : attachments) {
      attachmentIdMap.put(attachment.getAttachmentId(), attachment);
    }

    try {
      List<Contact> contacts     = new LinkedList<>();
      JSONArray     jsonContacts = new JSONArray(serializedContacts);

      for (int i = 0; i < jsonContacts.length(); i++) {
        Contact contact = deserialize(jsonContacts.getJSONObject(i).toString());

        if (contact.getAvatar() != null && contact.getAvatar().getAttachmentId() != null) {
          DatabaseAttachment attachment    = attachmentIdMap.get(contact.getAvatar().getAttachmentId());
          Avatar             updatedAvatar = new Avatar(contact.getAvatar().getAttachmentId(),
                                                        attachment,
                                                        contact.getAvatar().isProfile());
          contacts.add(new Contact(contact, updatedAvatar));
        } else {
          contacts.add(contact);
        }
      }

      return contacts;
    } catch (JSONException | IOException e) {
      Log.w(TAG, "Failed to parse shared contacts.", e);
    }

    return Collections.emptyList();
  }

  public Reader readerFor(Cursor cursor) {
    return new Reader(cursor);
  }

  public static class Status {
    public static final int DOWNLOAD_INITIALIZED     = 1;
    public static final int DOWNLOAD_NO_CONNECTIVITY = 2;
    public static final int DOWNLOAD_CONNECTING      = 3;
  }

  public class Reader implements Closeable {

    private final Cursor cursor;

    public Reader(Cursor cursor) {
      this.cursor = cursor;
    }

    public MessageRecord getNext() {
      if (cursor == null || !cursor.moveToNext())
        return null;

      return getCurrent();
    }

    public MessageRecord getCurrent() {
      long mmsType = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_TYPE));

      if (mmsType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
        return getNotificationMmsMessageRecord(cursor);
      } else {
        return getMediaMmsMessageRecord(cursor);
      }
    }

    private NotificationMmsMessageRecord getNotificationMmsMessageRecord(Cursor cursor) {
      long      id                   = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.ID));
      long      dateSent             = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_SENT));
      long      dateReceived         = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_RECEIVED));
      long      threadId             = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.THREAD_ID));
      long      mailbox              = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX));
      String    address              = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS));
      int       addressDeviceId      = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS_DEVICE_ID));
      Recipient recipient            = getRecipientFor(address);

      String    contentLocation      = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.CONTENT_LOCATION));
      String    transactionId        = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.TRANSACTION_ID));
      long      messageSize          = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_SIZE));
      long      expiry               = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.EXPIRY));
      int       status               = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.STATUS));
      int       deliveryReceiptCount = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.DELIVERY_RECEIPT_COUNT));
      int       readReceiptCount     = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.READ_RECEIPT_COUNT));
      int       subscriptionId       = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.SUBSCRIPTION_ID));

//      if (!TextSecurePreferences.isReadReceiptsEnabled(context)) {
//        readReceiptCount = 0;
//      }

      byte[]contentLocationBytes = null;
      byte[]transactionIdBytes   = null;

      if (!TextUtils.isEmpty(contentLocation))
        contentLocationBytes = org.thoughtcrime.securesms.util.Util.toIsoBytes(contentLocation);

      if (!TextUtils.isEmpty(transactionId))
        transactionIdBytes = org.thoughtcrime.securesms.util.Util.toIsoBytes(transactionId);

      SlideDeck slideDeck = new SlideDeck(context, new MmsNotificationAttachment(status, messageSize));


      return new NotificationMmsMessageRecord(context, id, recipient, recipient,
                                              addressDeviceId, dateSent, dateReceived, deliveryReceiptCount, threadId,
                                              contentLocationBytes, messageSize, expiry, status,
                                              transactionIdBytes, mailbox, subscriptionId, slideDeck,
                                              readReceiptCount);
    }

    private MediaMmsMessageRecord getMediaMmsMessageRecord(Cursor cursor) {
      long               id                   = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.ID));
      long               dateSent             = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_SENT));
      long               dateReceived         = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_RECEIVED));
      long               box                  = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX));
      long               threadId             = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.THREAD_ID));
      String             address              = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS));
      int                addressDeviceId      = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS_DEVICE_ID));
      int                deliveryReceiptCount = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.DELIVERY_RECEIPT_COUNT));
      int                readReceiptCount     = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.READ_RECEIPT_COUNT));
      String             body                 = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.BODY));
      int                partCount            = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.PART_COUNT));
      String             networkDocument      = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.NETWORK_FAILURE));
      int                subscriptionId       = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.SUBSCRIPTION_ID));
      long               expiresIn            = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.EXPIRES_IN));
      long               expireStarted        = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.EXPIRE_STARTED));

//      if (!TextSecurePreferences.isReadReceiptsEnabled(context)) {
//        readReceiptCount = 0;
//      }

      Recipient                 recipient          = getRecipientFor(address);
      List<NetworkFailure>      networkFailures    = getFailures(networkDocument);
      List<DatabaseAttachment>  attachments        = DatabaseFactory.getAttachmentDatabase(context).getAttachment(cursor);
      List<Contact>             contacts           = getSharedContacts(cursor, attachments);
      Set<Attachment>           contactAttachments = new HashSet<>(Stream.of(contacts).map(Contact::getAvatarAttachment).filter(a -> a != null).toList());
      SlideDeck                 slideDeck          = getSlideDeck(Stream.of(attachments).filterNot(contactAttachments::contains).toList());
      Quote                     quote              = getQuote(cursor);

      return new MediaMmsMessageRecord(context, id, recipient, recipient,
                                       addressDeviceId, dateSent, dateReceived, deliveryReceiptCount,
                                       threadId, body, slideDeck, partCount, box, new LinkedList<>(),
                                       networkFailures, subscriptionId, expiresIn, expireStarted,
                                       readReceiptCount, quote, contacts);
    }

    private Recipient getRecipientFor(String serialized) {
      Address address;

      if (TextUtils.isEmpty(serialized) || "insert-address-token".equals(serialized)) {
        address = Address.UNKNOWN;
      } else {
        address = Address.fromSerialized(serialized);

      }
      return Recipient.from(context, address, true);
    }

    private List<NetworkFailure> getFailures(String document) {
      if (!TextUtils.isEmpty(document)) {
        try {
          return JsonUtils.fromJson(document, NetworkFailureList.class).getList();
        } catch (IOException ioe) {
          Log.w(TAG, ioe);
        }
      }

      return new LinkedList<>();
    }

    private SlideDeck getSlideDeck(@NonNull List<DatabaseAttachment> attachments) {
      List<? extends Attachment> messageAttachmnets = Stream.of(attachments).filterNot(Attachment::isQuote).toList();
      return new SlideDeck(context, messageAttachmnets);
    }

    private @Nullable Quote getQuote(@NonNull Cursor cursor) {
      long                       quoteId          = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.QUOTE_ID));
      String                     quoteAuthor      = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.QUOTE_AUTHOR));
      String                     quoteText        = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.QUOTE_BODY));
      List<DatabaseAttachment>   attachments      = DatabaseFactory.getAttachmentDatabase(context).getAttachment(cursor);
      List<? extends Attachment> quoteAttachments = Stream.of(attachments).filter(Attachment::isQuote).toList();
      SlideDeck                  quoteDeck        = new SlideDeck(context, quoteAttachments);

      if (quoteId > 0 && !TextUtils.isEmpty(quoteAuthor)) {
        return new Quote(quoteId, Address.fromExternal(context, quoteAuthor), quoteText, quoteDeck);
      } else {
        return null;
      }
    }

    @Override
    public void close() {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
}
