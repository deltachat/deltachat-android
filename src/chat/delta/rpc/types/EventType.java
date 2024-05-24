/* Autogenerated file, do not edit manually */
package chat.delta.rpc.types;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.PROPERTY, property="kind")
@JsonSubTypes({@Type(EventType.Info.class), @Type(EventType.SmtpConnected.class), @Type(EventType.ImapConnected.class), @Type(EventType.SmtpMessageSent.class), @Type(EventType.ImapMessageDeleted.class), @Type(EventType.ImapMessageMoved.class), @Type(EventType.ImapInboxIdle.class), @Type(EventType.NewBlobFile.class), @Type(EventType.DeletedBlobFile.class), @Type(EventType.Warning.class), @Type(EventType.Error.class), @Type(EventType.ErrorSelfNotInGroup.class), @Type(EventType.MsgsChanged.class), @Type(EventType.ReactionsChanged.class), @Type(EventType.IncomingMsg.class), @Type(EventType.IncomingMsgBunch.class), @Type(EventType.MsgsNoticed.class), @Type(EventType.MsgDelivered.class), @Type(EventType.MsgFailed.class), @Type(EventType.MsgRead.class), @Type(EventType.MsgDeleted.class), @Type(EventType.ChatModified.class), @Type(EventType.ChatEphemeralTimerModified.class), @Type(EventType.ContactsChanged.class), @Type(EventType.LocationChanged.class), @Type(EventType.ConfigureProgress.class), @Type(EventType.ImexProgress.class), @Type(EventType.ImexFileWritten.class), @Type(EventType.SecurejoinInviterProgress.class), @Type(EventType.SecurejoinJoinerProgress.class), @Type(EventType.ConnectivityChanged.class), @Type(EventType.SelfavatarChanged.class), @Type(EventType.ConfigSynced.class), @Type(EventType.WebxdcStatusUpdate.class), @Type(EventType.WebxdcRealtimeData.class), @Type(EventType.WebxdcInstanceDeleted.class), @Type(EventType.AccountsBackgroundFetchDone.class), @Type(EventType.ChatlistChanged.class), @Type(EventType.ChatlistItemChanged.class), @Type(EventType.EventChannelOverflow.class)})
public abstract class EventType {

/**
 * The library-user may write an informational string to the log.
 *
 * This event should *not* be reported to the end-user using a popup or something like that.
 */
  public static class Info extends EventType {
    public String msg;
  }

/* Emitted when SMTP connection is established and login was successful. */
  public static class SmtpConnected extends EventType {
    public String msg;
  }

/* Emitted when IMAP connection is established and login was successful. */
  public static class ImapConnected extends EventType {
    public String msg;
  }

/* Emitted when a message was successfully sent to the SMTP server. */
  public static class SmtpMessageSent extends EventType {
    public String msg;
  }

/* Emitted when an IMAP message has been marked as deleted */
  public static class ImapMessageDeleted extends EventType {
    public String msg;
  }

/* Emitted when an IMAP message has been moved */
  public static class ImapMessageMoved extends EventType {
    public String msg;
  }

/* Emitted before going into IDLE on the Inbox folder. */
  public static class ImapInboxIdle extends EventType {
  }

/* Emitted when an new file in the $BLOBDIR was created */
  public static class NewBlobFile extends EventType {
    public String file;
  }

/* Emitted when an file in the $BLOBDIR was deleted */
  public static class DeletedBlobFile extends EventType {
    public String file;
  }

/**
 * The library-user should write a warning string to the log.
 *
 * This event should *not* be reported to the end-user using a popup or something like that.
 */
  public static class Warning extends EventType {
    public String msg;
  }

/**
 * The library-user should report an error to the end-user.
 *
 * As most things are asynchronous, things may go wrong at any time and the user should not be disturbed by a dialog or so.  Instead, use a bubble or so.
 *
 * However, for ongoing processes (eg. configure()) or for functions that are expected to fail (eg. autocryptContinueKeyTransfer()) it might be better to delay showing these events until the function has really failed (returned false). It should be sufficient to report only the *last* error in a messasge box then.
 */
  public static class Error extends EventType {
    public String msg;
  }

/* An action cannot be performed because the user is not in the group. Reported eg. after a call to setChatName(), setChatProfileImage(), addContactToChat(), removeContactFromChat(), and messages sending functions. */
  public static class ErrorSelfNotInGroup extends EventType {
    public String msg;
  }

/**
 * Messages or chats changed.  One or more messages or chats changed for various reasons in the database: - Messages sent, received or removed - Chats created, deleted or archived - A draft has been set
 *
 * `chatId` is set if only a single chat is affected by the changes, otherwise 0. `msgId` is set if only a single message is affected by the changes, otherwise 0.
 */
  public static class MsgsChanged extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/* Reactions for the message changed. */
  public static class ReactionsChanged extends EventType {
    public Integer chatId;
    public Integer contactId;
    public Integer msgId;
  }

/**
 * There is a fresh message. Typically, the user will show an notification when receiving this message.
 *
 * There is no extra #DC_EVENT_MSGS_CHANGED event sent together with this event.
 */
  public static class IncomingMsg extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/* Downloading a bunch of messages just finished. This is an event to allow the UI to only show one notification per message bunch, instead of cluttering the user with many notifications. */
  public static class IncomingMsgBunch extends EventType {
  }

/* Messages were seen or noticed. chat id is always set. */
  public static class MsgsNoticed extends EventType {
    public Integer chatId;
  }

/* A single message is sent successfully. State changed from  DC_STATE_OUT_PENDING to DC_STATE_OUT_DELIVERED, see `Message.state`. */
  public static class MsgDelivered extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/* A single message could not be sent. State changed from DC_STATE_OUT_PENDING or DC_STATE_OUT_DELIVERED to DC_STATE_OUT_FAILED, see `Message.state`. */
  public static class MsgFailed extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/* A single message is read by the receiver. State changed from DC_STATE_OUT_DELIVERED to DC_STATE_OUT_MDN_RCVD, see `Message.state`. */
  public static class MsgRead extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/* A single message is deleted. */
  public static class MsgDeleted extends EventType {
    public Integer chatId;
    public Integer msgId;
  }

/**
 * Chat changed.  The name or the image of a chat group was changed or members were added or removed. Or the verify state of a chat has changed. See setChatName(), setChatProfileImage(), addContactToChat() and removeContactFromChat().
 *
 * This event does not include ephemeral timer modification, which is a separate event.
 */
  public static class ChatModified extends EventType {
    public Integer chatId;
  }

/* Chat ephemeral timer changed. */
  public static class ChatEphemeralTimerModified extends EventType {
    public Integer chatId;
    public Integer timer;
  }

/**
 * Contact(s) created, renamed, blocked or deleted.
 *
 * @param data1 (int) If set, this is the contact_id of an added contact that should be selected.
 */
  public static class ContactsChanged extends EventType {
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
    public Integer contactId;
  }

/**
 * Location of one or more contact has changed.
 *
 * @param data1 (u32) contact_id of the contact for which the location has changed. If the locations of several contacts have been changed, this parameter is set to `None`.
 */
  public static class LocationChanged extends EventType {
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
    public Integer contactId;
  }

/* Inform about the configuration progress started by configure(). */
  public static class ConfigureProgress extends EventType {
    /* Progress comment or error, something to display to the user. */
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
    public String comment;
    /**
     * Progress.
     *
     * 0=error, 1-999=progress in permille, 1000=success and done
     */
    public Integer progress;
  }

/**
 * Inform about the import/export progress started by imex().
 *
 * @param data1 (usize) 0=error, 1-999=progress in permille, 1000=success and done @param data2 0
 */
  public static class ImexProgress extends EventType {
    public Integer progress;
  }

/**
 * A file has been exported. A file has been written by imex(). This event may be sent multiple times by a single call to imex().
 *
 * A typical purpose for a handler of this event may be to make the file public to some system services.
 *
 * @param data2 0
 */
  public static class ImexFileWritten extends EventType {
    public String path;
  }

/**
 * Progress information of a secure-join handshake from the view of the inviter (Alice, the person who shows the QR code).
 *
 * These events are typically sent after a joiner has scanned the QR code generated by getChatSecurejoinQrCodeSvg().
 *
 * @param data1 (int) ID of the contact that wants to join. @param data2 (int) Progress as: 300=vg-/vc-request received, typically shown as "bob@addr joins". 600=vg-/vc-request-with-auth received, vg-member-added/vc-contact-confirm sent, typically shown as "bob@addr verified". 800=vg-member-added-received received, shown as "bob@addr securely joined GROUP", only sent for the verified-group-protocol. 1000=Protocol finished for this contact.
 */
  public static class SecurejoinInviterProgress extends EventType {
    public Integer contactId;
    public Integer progress;
  }

/* Progress information of a secure-join handshake from the view of the joiner (Bob, the person who scans the QR code). The events are typically sent while secureJoin(), which may take some time, is executed. @param data1 (int) ID of the inviting contact. @param data2 (int) Progress as: 400=vg-/vc-request-with-auth sent, typically shown as "alice@addr verified, introducing myself." (Bob has verified alice and waits until Alice does the same for him) */
  public static class SecurejoinJoinerProgress extends EventType {
    public Integer contactId;
    public Integer progress;
  }

/* The connectivity to the server changed. This means that you should refresh the connectivity view and possibly the connectivtiy HTML; see getConnectivity() and getConnectivityHtml() for details. */
  public static class ConnectivityChanged extends EventType {
  }

/* Deprecated by `ConfigSynced`. */
  public static class SelfavatarChanged extends EventType {
  }

/* A multi-device synced config value changed. Maybe the app needs to refresh smth. For uniformity this is emitted on the source device too. The value isn't here, otherwise it would be logged which might not be good for privacy. */
  public static class ConfigSynced extends EventType {
    /* Configuration key. */
    public String key;
  }

  public static class WebxdcStatusUpdate extends EventType {
    public Integer msgId;
    public Integer statusUpdateSerial;
  }

/* Data received over an ephemeral peer channel. */
  public static class WebxdcRealtimeData extends EventType {
    public java.util.List<Integer> data;
    public Integer msgId;
  }

/* Inform that a message containing a webxdc instance has been deleted */
  public static class WebxdcInstanceDeleted extends EventType {
    public Integer msgId;
  }

/**
 * Tells that the Background fetch was completed (or timed out). This event acts as a marker, when you reach this event you can be sure that all events emitted during the background fetch were processed.
 *
 * This event is only emitted by the account manager
 */
  public static class AccountsBackgroundFetchDone extends EventType {
  }

/**
 * Inform that set of chats or the order of the chats in the chatlist has changed.
 *
 * Sometimes this is emitted together with `UIChatlistItemChanged`.
 */
  public static class ChatlistChanged extends EventType {
  }

/* Inform that a single chat list item changed and needs to be rerendered. If `chat_id` is set to None, then all currently visible chats need to be rerendered, and all not-visible items need to be cleared from cache if the UI has a cache. */
  public static class ChatlistItemChanged extends EventType {
    @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SET)
    public Integer chatId;
  }

/* Inform than some events have been skipped due to event channel overflow. */
  public static class EventChannelOverflow extends EventType {
    public Integer n;
  }

}