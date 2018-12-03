package org.thoughtcrime.securesms.database;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface MmsSmsColumns {

  public static final String ID                       = "_id";
  public static final String NORMALIZED_DATE_SENT     = "date_sent";
  public static final String NORMALIZED_DATE_RECEIVED = "date_received";
  public static final String THREAD_ID                = "thread_id";
  public static final String READ                     = "read";
  public static final String BODY                     = "body";
  public static final String ADDRESS                  = "address";
  public static final String ADDRESS_DEVICE_ID        = "address_device_id";
  public static final String DELIVERY_RECEIPT_COUNT   = "delivery_receipt_count";
  public static final String READ_RECEIPT_COUNT       = "read_receipt_count";
  public static final String MISMATCHED_IDENTITIES    = "mismatched_identities";
  public static final String UNIQUE_ROW_ID            = "unique_row_id";
  public static final String SUBSCRIPTION_ID          = "subscription_id";
  public static final String EXPIRES_IN               = "expires_in";
  public static final String EXPIRE_STARTED           = "expire_started";
  public static final String NOTIFIED                 = "notified";

  public static class Types {
    // Base Types
    protected static final long BASE_TYPE_MASK                     = 0x1F;

    protected static final long INCOMING_CALL_TYPE                 = 1;
    protected static final long OUTGOING_CALL_TYPE                 = 2;
    protected static final long MISSED_CALL_TYPE                   = 3;
    protected static final long JOINED_TYPE                        = 4;

    protected static final long BASE_OUTBOX_TYPE                   = 21;
    protected static final long BASE_SENDING_TYPE                  = 22;
    protected static final long BASE_SENT_TYPE                     = 23;
    protected static final long BASE_SENT_FAILED_TYPE              = 24;
    protected static final long BASE_PENDING_SECURE_SMS_FALLBACK   = 25;
    protected static final long BASE_PENDING_INSECURE_SMS_FALLBACK = 26;

    protected static final long[] OUTGOING_MESSAGE_TYPES = {BASE_OUTBOX_TYPE, BASE_SENT_TYPE,
                                                            BASE_SENDING_TYPE, BASE_SENT_FAILED_TYPE,
                                                            BASE_PENDING_SECURE_SMS_FALLBACK,
                                                            BASE_PENDING_INSECURE_SMS_FALLBACK,
                                                            OUTGOING_CALL_TYPE};

    // Message attributes
    protected static final long MESSAGE_FORCE_SMS_BIT  = 0x40;

    // Key Exchange Information
    protected static final long KEY_EXCHANGE_BIT                   = 0x8000;
    protected static final long KEY_EXCHANGE_IDENTITY_VERIFIED_BIT = 0x4000;
    protected static final long KEY_EXCHANGE_IDENTITY_DEFAULT_BIT  = 0x2000;
    protected static final long KEY_EXCHANGE_CORRUPTED_BIT         = 0x1000;
    protected static final long KEY_EXCHANGE_INVALID_VERSION_BIT   = 0x800;
    protected static final long KEY_EXCHANGE_BUNDLE_BIT            = 0x400;
    protected static final long KEY_EXCHANGE_IDENTITY_UPDATE_BIT   = 0x200;
    protected static final long KEY_EXCHANGE_CONTENT_FORMAT        = 0x100;

    // Secure Message Information
    protected static final long SECURE_MESSAGE_BIT = 0x800000;
    protected static final long END_SESSION_BIT    = 0x400000;
    protected static final long PUSH_MESSAGE_BIT   = 0x200000;

    // Group Message Information
    protected static final long GROUP_UPDATE_BIT            = 0x10000;
    protected static final long GROUP_QUIT_BIT              = 0x20000;
    protected static final long EXPIRATION_TIMER_UPDATE_BIT = 0x40000;

    // Encrypted Storage Information XXX
    // public    static final long ENCRYPTION_SYMMETRIC_BIT         = 0x80000000; Deprecated
    // protected static final long ENCRYPTION_ASYMMETRIC_BIT        = 0x40000000; Deprecated
    protected static final long ENCRYPTION_REMOTE_BIT            = 0x20000000;
    protected static final long ENCRYPTION_REMOTE_FAILED_BIT     = 0x10000000;
    protected static final long ENCRYPTION_REMOTE_NO_SESSION_BIT = 0x08000000;
    protected static final long ENCRYPTION_REMOTE_DUPLICATE_BIT  = 0x04000000;
    protected static final long ENCRYPTION_REMOTE_LEGACY_BIT     = 0x02000000;

    public static boolean isFailedMessageType(long type) {
      return (type & BASE_TYPE_MASK) == BASE_SENT_FAILED_TYPE;
    }

    public static boolean isOutgoingMessageType(long type) {
      for (long outgoingType : OUTGOING_MESSAGE_TYPES) {
        if ((type & BASE_TYPE_MASK) == outgoingType)
          return true;
      }

      return false;
    }

    public static boolean isForcedSms(long type) {
      return (type & MESSAGE_FORCE_SMS_BIT) != 0;
    }

    public static boolean isPendingMessageType(long type) {
      return
          (type & BASE_TYPE_MASK) == BASE_OUTBOX_TYPE ||
          (type & BASE_TYPE_MASK) == BASE_SENDING_TYPE;
    }

    public static boolean isPendingSecureSmsFallbackType(long type) {
      return (type & BASE_TYPE_MASK) == BASE_PENDING_SECURE_SMS_FALLBACK;
    }

    public static boolean isJoinedType(long type) {
      return (type & BASE_TYPE_MASK) == JOINED_TYPE;
    }

    public static boolean isSecureType(long type) {
      return (type & SECURE_MESSAGE_BIT) != 0;
    }

    public static boolean isPushType(long type) {
      return (type & PUSH_MESSAGE_BIT) != 0;
    }

    public static boolean isEndSessionType(long type) {
      return (type & END_SESSION_BIT) != 0;
    }

    public static boolean isKeyExchangeType(long type) {
      return (type & KEY_EXCHANGE_BIT) != 0;
    }

    public static boolean isIdentityVerified(long type) {
      return (type & KEY_EXCHANGE_IDENTITY_VERIFIED_BIT) != 0;
    }

    public static boolean isIdentityDefault(long type) {
      return (type & KEY_EXCHANGE_IDENTITY_DEFAULT_BIT) != 0;
    }

    public static boolean isCorruptedKeyExchange(long type) {
      return (type & KEY_EXCHANGE_CORRUPTED_BIT) != 0;
    }

    public static boolean isInvalidVersionKeyExchange(long type) {
      return (type & KEY_EXCHANGE_INVALID_VERSION_BIT) != 0;
    }

    public static boolean isBundleKeyExchange(long type) {
      return (type & KEY_EXCHANGE_BUNDLE_BIT) != 0;
    }

    public static boolean isContentBundleKeyExchange(long type) {
      return (type & KEY_EXCHANGE_CONTENT_FORMAT) != 0;
    }

    public static boolean isIdentityUpdate(long type) {
      return (type & KEY_EXCHANGE_IDENTITY_UPDATE_BIT) != 0;
    }

    public static boolean isCallLog(long type) {
      return type == INCOMING_CALL_TYPE || type == OUTGOING_CALL_TYPE || type == MISSED_CALL_TYPE;
    }

    public static boolean isExpirationTimerUpdate(long type) {
      return (type & EXPIRATION_TIMER_UPDATE_BIT) != 0;
    }

    public static boolean isIncomingCall(long type) {
      return type == INCOMING_CALL_TYPE;
    }

    public static boolean isOutgoingCall(long type) {
      return type == OUTGOING_CALL_TYPE;
    }

    public static boolean isMissedCall(long type) {
      return type == MISSED_CALL_TYPE;
    }

    public static boolean isGroupUpdate(long type) {
      return (type & GROUP_UPDATE_BIT) != 0;
    }

    public static boolean isGroupQuit(long type) {
      return (type & GROUP_QUIT_BIT) != 0;
    }

    public static boolean isFailedDecryptType(long type) {
      return (type & ENCRYPTION_REMOTE_FAILED_BIT) != 0;
    }

    public static boolean isDuplicateMessageType(long type) {
      return (type & ENCRYPTION_REMOTE_DUPLICATE_BIT) != 0;
    }

    public static boolean isNoRemoteSessionType(long type) {
      return (type & ENCRYPTION_REMOTE_NO_SESSION_BIT) != 0;
    }

    public static boolean isLegacyType(long type) {
      return (type & ENCRYPTION_REMOTE_LEGACY_BIT) != 0 ||
             (type & ENCRYPTION_REMOTE_BIT) != 0;
    }
  }
}
