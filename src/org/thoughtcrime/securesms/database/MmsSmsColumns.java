package org.thoughtcrime.securesms.database;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface MmsSmsColumns {

  public static final String ID                       = "_id";
  public static final String READ                     = "read";
  public static final String BODY                     = "body";

  public static class Types {
    // Base Types
    protected static final long BASE_TYPE_MASK                     = 0x1F;

    protected static final long OUTGOING_CALL_TYPE                 = 2;

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

    // Key Exchange Information
    protected static final long KEY_EXCHANGE_IDENTITY_VERIFIED_BIT = 0x4000;
    protected static final long KEY_EXCHANGE_IDENTITY_DEFAULT_BIT  = 0x2000;

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

    public static boolean isPendingMessageType(long type) {
      return
          (type & BASE_TYPE_MASK) == BASE_OUTBOX_TYPE ||
          (type & BASE_TYPE_MASK) == BASE_SENDING_TYPE;
    }

    public static boolean isPendingSecureSmsFallbackType(long type) {
      return (type & BASE_TYPE_MASK) == BASE_PENDING_SECURE_SMS_FALLBACK;
    }

    public static boolean isIdentityVerified(long type) {
      return (type & KEY_EXCHANGE_IDENTITY_VERIFIED_BIT) != 0;
    }

    public static boolean isIdentityDefault(long type) {
      return (type & KEY_EXCHANGE_IDENTITY_DEFAULT_BIT) != 0;
    }

  }
}
