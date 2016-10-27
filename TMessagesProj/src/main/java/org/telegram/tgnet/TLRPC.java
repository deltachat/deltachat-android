/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.tgnet;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class TLRPC {

    public static final int CHAT_FLAG_IS_PUBLIC             = 0x00000040;
    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    public static final int MESSAGE_FLAG_HAS_MARKUP         = 0x00000040;
    public static final int MESSAGE_FLAG_HAS_ENTITIES       = 0x00000080;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;
	public static final int MESSAGE_FLAG_HAS_BOT_ID         = 0x00000800;
	public static final int MESSAGE_FLAG_EDITED             = 0x00008000;

    public static final int LAYER = 53;

	public static class DraftMessage extends TLObject {
		public int flags;
		public boolean no_webpage;
		public int reply_to_msg_id;
		public String message;
		public ArrayList<MessageEntity> entities = new ArrayList<>();
		public int date;

		public static DraftMessage TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			DraftMessage result = null;
			return result;
		}
	}

    public static class ChatPhoto extends TLObject {
		public FileLocation photo_small;
		public FileLocation photo_big;

		public static ChatPhoto TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ChatPhoto result = null;
			return result;
		}
	}

	public static class TL_chatPhotoEmpty extends ChatPhoto {
		public static int constructor = 0x37c1011c;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_error extends TLObject {
		public static int constructor = 0xc4b9f9bb;

		public int code;
		public String text;

		public static TL_error TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_error.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_error", constructor));
				} else {
					return null;
				}
			}
			TL_error result = new TL_error();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
            code = stream.readInt32(exception);
			text = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(code);
			stream.writeString(text);
		}
	}

	public static class DocumentAttribute extends TLObject {
		public int w;
		public int h;
		public int duration;
		public String alt;
		public InputStickerSet stickerset;
		public int flags;
		public boolean voice;
		public String title;
		public String performer;
		public byte[] waveform;
		public String file_name;

		public static DocumentAttribute TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			DocumentAttribute result = null;
			return result;
		}
	}

	public static class TL_documentAttributeAnimated extends DocumentAttribute {
		public static int constructor = 0x11b58939;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_documentAttributeImageSize extends DocumentAttribute {
		public static int constructor = 0x6c37c15c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(w);
			stream.writeInt32(h);
		}
	}

	public static class TL_documentAttributeAudio_old extends TL_documentAttributeAudio {
		public static int constructor = 0x51448e5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			duration = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(duration);
		}
	}

	public static class TL_documentAttributeSticker extends DocumentAttribute {
		public static int constructor = 0x3a556302;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			alt = stream.readString(exception);
			stickerset = InputStickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(alt);
			stickerset.serializeToStream(stream);
		}
	}

	public static class TL_documentAttributeVideo extends DocumentAttribute {
		public static int constructor = 0x5910cccb;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			duration = stream.readInt32(exception);
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(duration);
			stream.writeInt32(w);
			stream.writeInt32(h);
		}
	}

	public static class TL_documentAttributeAudio extends DocumentAttribute {
		public static int constructor = 0x9852f9c6;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			voice = (flags & 1024) != 0;
			duration = stream.readInt32(exception);
			if ((flags & 1) != 0) {
				title = stream.readString(exception);
			}
			if ((flags & 2) != 0) {
				performer = stream.readString(exception);
			}
			if ((flags & 4) != 0) {
				waveform = stream.readByteArray(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = voice ? (flags | 1024) : (flags &~ 1024);
			stream.writeInt32(flags);
			stream.writeInt32(duration);
			if ((flags & 1) != 0) {
				stream.writeString(title);
			}
			if ((flags & 2) != 0) {
				stream.writeString(performer);
			}
			if ((flags & 4) != 0) {
				stream.writeByteArray(waveform);
			}
		}
	}

	public static class TL_documentAttributeFilename extends DocumentAttribute {
		public static int constructor = 0x15590068;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			file_name = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(file_name);
		}
	}

	public static class TL_contactStatus extends TLObject {
		public static int constructor = 0xd3680c61;

		public int user_id;
		public UserStatus status;

		public static TL_contactStatus TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_contactStatus.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_contactStatus", constructor));
				} else {
					return null;
				}
			}
			TL_contactStatus result = new TL_contactStatus();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			status.serializeToStream(stream);
		}
	}

	public static class TL_auth_authorization extends TLObject {
		public static int constructor = 0xff036af1;

		public User user;

		public static TL_auth_authorization TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_auth_authorization.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_auth_authorization", constructor));
				} else {
					return null;
				}
			}
			TL_auth_authorization result = new TL_auth_authorization();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			user = User.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			user.serializeToStream(stream);
		}
	}

    public static class messages_Messages extends TLObject {
        public ArrayList<Message> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int flags;
        public int pts;
        public int count;

        public static messages_Messages TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            messages_Messages result = null;
            return result;
        }
    }

    public static class TL_messages_messages extends messages_Messages {
        public static int constructor = 0x8c718e87;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Message object = Message.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                messages.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                chats.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                users.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = messages.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                messages.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = chats.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                chats.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = users.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                users.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_messages_messagesSlice extends messages_Messages {
        public static int constructor = 0xb446ae3;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            count = stream.readInt32(exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Message object = Message.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                messages.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                chats.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                users.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(count);
            stream.writeInt32(0x1cb5c415);
            int count = messages.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                messages.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = chats.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                chats.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = users.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                users.get(a).serializeToStream(stream);
            }
        }
    }

	public static class Peer extends TLObject {
		public int channel_id;
		public int user_id;
		public int chat_id;

		public static Peer TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Peer result = null;
			return result;
		}
	}

	public static class TL_peerUser extends Peer {
		public static int constructor = 0x9db1bc6d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
		}
	}

	public static class TL_peerChat extends Peer {
		public static int constructor = 0xbad0e5bb;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			chat_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(chat_id);
		}
	}

	public static class PrivacyKey extends TLObject {

		public static PrivacyKey TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			PrivacyKey result = null;
			return result;
		}
	}

	public static class GeoPoint extends TLObject {
		public double _long;
		public double lat;

		public static GeoPoint TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			GeoPoint result = null;
			return result;
		}
	}

	public static class TL_geoPoint extends GeoPoint {
		public static int constructor = 0x2049d70c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			_long = stream.readDouble(exception);
            lat = stream.readDouble(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeDouble(_long);
			stream.writeDouble(lat);
		}
	}

	public static class TL_account_privacyRules extends TLObject {
		public static int constructor = 0x554abb6f;

		public ArrayList<PrivacyRule> rules = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();

		public static TL_account_privacyRules TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_account_privacyRules.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_account_privacyRules", constructor));
				} else {
					return null;
				}
			}
			TL_account_privacyRules result = new TL_account_privacyRules();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				PrivacyRule object = PrivacyRule.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				rules.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = rules.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				rules.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
		}
	}

	public static class ChatInvite extends TLObject {
		public int flags;
		public boolean channel;
		public boolean broadcast;
		public boolean megagroup;
		public String title;
		public Chat chat;

		public static ChatInvite TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ChatInvite result = null;
			return result;
		}
	}

	public static class SendMessageAction extends TLObject {
		public int progress;

		public static SendMessageAction TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			SendMessageAction result = null;
			return result;
		}
	}

	public static class auth_SentCodeType extends TLObject {
		public int length;
		public String pattern;

		public static auth_SentCodeType TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			auth_SentCodeType result = null;
			return result;
		}
	}

	public static class TL_auth_sentCodeTypeApp extends auth_SentCodeType {
		public static int constructor = 0x3dbb5986;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(length);
		}
	}

	public static class TL_auth_sentCodeTypeCall extends auth_SentCodeType {
		public static int constructor = 0x5353e5a7;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(length);
		}
	}

	public static class TL_auth_sentCodeTypeFlashCall extends auth_SentCodeType {
		public static int constructor = 0xab03c6d9;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			pattern = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(pattern);
		}
	}

	public static class TL_auth_sentCodeTypeSms extends auth_SentCodeType {
		public static int constructor = 0xc000bba2;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(length);
		}
	}

	public static class TL_inputPhoneContact extends TLObject {
		public static int constructor = 0xf392b7f4;

		public long client_id;
		public String phone;
		public String first_name;
		public String last_name;

		public static TL_inputPhoneContact TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_inputPhoneContact.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_inputPhoneContact", constructor));
				} else {
					return null;
				}
			}
			TL_inputPhoneContact result = new TL_inputPhoneContact();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			client_id = stream.readInt64(exception);
			phone = stream.readString(exception);
			first_name = stream.readString(exception);
			last_name = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(client_id);
			stream.writeString(phone);
			stream.writeString(first_name);
			stream.writeString(last_name);
		}
	}

	public static class PrivacyRule extends TLObject {
		public ArrayList<Integer> users = new ArrayList<>();

		public static PrivacyRule TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			PrivacyRule result = null;
			return result;
		}
	}

	public static class TL_privacyValueAllowUsers extends PrivacyRule {
		public static int constructor = 0x4d5bbe0c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				users.add(stream.readInt32(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(users.get(a));
			}
		}
	}

	public static class TL_privacyValueDisallowAll extends PrivacyRule {
		public static int constructor = 0x8b73e763;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_privacyValueAllowAll extends PrivacyRule {
		public static int constructor = 0x65427b82;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_privacyValueDisallowUsers extends PrivacyRule {
		public static int constructor = 0xc7f49b7;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				users.add(stream.readInt32(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(users.get(a));
			}
		}
	}

	public static class TL_messageMediaUnsupported extends MessageMedia {
		public static int constructor = 0x9f84f49e;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageMediaEmpty extends MessageMedia {
		public static int constructor = 0x3ded6320;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageMediaVenue extends MessageMedia {
		public static int constructor = 0x7912b71f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
			title = stream.readString(exception);
			address = stream.readString(exception);
			provider = stream.readString(exception);
			venue_id = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			geo.serializeToStream(stream);
			stream.writeString(title);
			stream.writeString(address);
			stream.writeString(provider);
			stream.writeString(venue_id);
		}
	}

	public static class TL_messageMediaDocument extends MessageMedia {
		public static int constructor = 0xf3e02ea8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			document.serializeToStream(stream);
			stream.writeString(caption);
		}
	}

	public static class TL_messageMediaContact extends MessageMedia {
		public static int constructor = 0x5e7d2f39;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			phone_number = stream.readString(exception);
			first_name = stream.readString(exception);
			last_name = stream.readString(exception);
			user_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(first_name);
			stream.writeString(last_name);
			stream.writeInt32(user_id);
		}
	}

	public static class TL_messageMediaPhoto extends MessageMedia {
		public static int constructor = 0x3d8ce53d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			photo.serializeToStream(stream);
			stream.writeString(caption);
		}
	}

	public static class TL_messageMediaGeo extends MessageMedia {
		public static int constructor = 0x56e0d474;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			geo = GeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			geo.serializeToStream(stream);
		}
	}

	public static class TL_messageMediaWebPage extends MessageMedia {
		public static int constructor = 0xa32dd600;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			webpage.serializeToStream(stream);
		}
	}

	public static class TL_auth_sentCode extends TLObject {
		public static int constructor = 0x5e002502;

		public int flags;
		public boolean phone_registered;
		public auth_SentCodeType type;
		public String phone_code_hash;
		public auth_CodeType next_type;
		public int timeout;

		public static TL_auth_sentCode TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_auth_sentCode.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_auth_sentCode", constructor));
				} else {
					return null;
				}
			}
			TL_auth_sentCode result = new TL_auth_sentCode();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			phone_registered = (flags & 1) != 0;
			type = auth_SentCodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
			phone_code_hash = stream.readString(exception);
			if ((flags & 2) != 0) {
				next_type = auth_CodeType.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 4) != 0) {
				timeout = stream.readInt32(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = phone_registered ? (flags | 1) : (flags &~ 1);
			stream.writeInt32(flags);
			type.serializeToStream(stream);
			stream.writeString(phone_code_hash);
			if ((flags & 2) != 0) {
				next_type.serializeToStream(stream);
			}
			if ((flags & 4) != 0) {
				stream.writeInt32(timeout);
			}
		}
	}

	public static class BotInlineResult extends TLObject {
		public int flags;
		public String id;
		public String type;
		public String title;
		public String description;
		public String url;
		public String thumb_url;
		public String content_url;
		public String content_type;
		public int w;
		public int h;
		public int duration;
		public Photo photo;
		public Document document;

		public static BotInlineResult TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			BotInlineResult result = null;
			return result;
		}
	}


	public static class PeerNotifySettings extends TLObject {
		public int flags;
		public boolean silent;
		public int mute_until;
		public String sound;

		public static PeerNotifySettings TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			PeerNotifySettings result = null;
			return result;
		}
	}

	public static class TL_peerNotifySettings extends PeerNotifySettings {
		public static int constructor = 0x9acda4c0;

		public boolean show_previews;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			show_previews = (flags & 1) != 0;
			silent = (flags & 2) != 0;
			mute_until = stream.readInt32(exception);
			sound = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = show_previews ? (flags | 1) : (flags &~ 1);
			flags = silent ? (flags | 2) : (flags &~ 2);
			stream.writeInt32(flags);
			stream.writeInt32(mute_until);
			stream.writeString(sound);
		}
	}

	public static class TL_peerNotifySettingsEmpty extends PeerNotifySettings {
		public static int constructor = 0x70a68512;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class contacts_Blocked extends TLObject {
		public ArrayList<TL_contactBlocked> blocked = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();
		public int count;

		public static contacts_Blocked TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			contacts_Blocked result = null;
			return result;
		}
	}

	public static class TL_messages_stickerSet extends TLObject {
		public static int constructor = 0xb60a24a6;

		public StickerSet set;
		public ArrayList<TL_stickerPack> packs = new ArrayList<>();
		public ArrayList<Document> documents = new ArrayList<>();

		public static TL_messages_stickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			TL_messages_stickerSet result = new TL_messages_stickerSet();
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			set = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_stickerPack object = TL_stickerPack.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				packs.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Document object = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				documents.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			set.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
			int count = packs.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				packs.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = documents.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				documents.get(a).serializeToStream(stream);
			}
		}
	}

	public static class InputGeoPoint extends TLObject {
		public double lat;
		public double _long;

        public static InputGeoPoint TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputGeoPoint result = null;
			return result;
		}
	}

	public static class TL_inputGeoPoint extends InputGeoPoint {
		public static int constructor = 0xf3b7acc9;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			lat = stream.readDouble(exception);
			_long = stream.readDouble(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeDouble(lat);
			stream.writeDouble(_long);
		}
	}

	public static class TL_inputGeoPointEmpty extends InputGeoPoint {
		public static int constructor = 0xe4c123d6;


		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
		}
	}

	public static class Audio extends TLObject {
		public long id;
		public int date;
		public int duration;
		public String mime_type;
		public int size;
		public int dc_id;
		public int user_id;
		public byte[] key;
		public byte[] iv;

		public static Audio TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Audio result = null;
			return result;
		}
	}

	public static class BotInfo extends TLObject {
		public int user_id;
		public String description;
		public ArrayList<TL_botCommand> commands = new ArrayList<>();
		public int version;

		public static BotInfo TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			BotInfo result = null;
			return result;
		}
	}

	public static class contacts_Contacts extends TLObject {
		public ArrayList<TL_contact> contacts = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();

		public static contacts_Contacts TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			contacts_Contacts result = null;
			return result;
		}
	}

	public static class TL_contacts_contactsNotModified extends contacts_Contacts {
		public static int constructor = 0xb74ba9d2;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class InputPrivacyKey extends TLObject {

		public static InputPrivacyKey TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputPrivacyKey result = null;
			return result;
		}
	}

	public static class TL_inputPrivacyKeyChatInvite extends InputPrivacyKey {
		public static int constructor = 0xbdfb0426;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputPrivacyKeyStatusTimestamp extends InputPrivacyKey {
		public static int constructor = 0x4f96cb18;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class photos_Photos extends TLObject {
		public ArrayList<Photo> photos = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();
		public int count;

		public static photos_Photos TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			photos_Photos result = null;
			return result;
		}
	}

	public static class TL_photos_photos extends photos_Photos {
		public static int constructor = 0x8dca6aa5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Photo object = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				photos.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
			int count = photos.size();
			stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
				photos.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
		}
	}

	public static class ChatFull extends TLObject {
		public int flags;
		public boolean can_view_participants;
		public boolean can_set_username;
		public int id;
		public String about;
		public int participants_count;
		public int admins_count;
		public int kicked_count;
		public int read_inbox_max_id;
		public int read_outbox_max_id;
		public int unread_count;
		public Photo chat_photo;
		public PeerNotifySettings notify_settings;
		public ExportedChatInvite exported_invite;
		public ArrayList<BotInfo> bot_info = new ArrayList<>();
		public int migrated_from_chat_id;
		public int migrated_from_max_id;
		public int pinned_msg_id;
		public ChatParticipants participants;

		public static ChatFull TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ChatFull result = null;
			return result;
		}
	}

	public static class TL_channelFull extends ChatFull {
		public static int constructor = 0xc3d5512f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			can_view_participants = (flags & 8) != 0;
			can_set_username = (flags & 64) != 0;
			id = stream.readInt32(exception);
			about = stream.readString(exception);
			if ((flags & 1) != 0) {
				participants_count = stream.readInt32(exception);
			}
			if ((flags & 2) != 0) {
				admins_count = stream.readInt32(exception);
			}
			if ((flags & 4) != 0) {
				kicked_count = stream.readInt32(exception);
			}
			read_inbox_max_id = stream.readInt32(exception);
			read_outbox_max_id = stream.readInt32(exception);
			unread_count = stream.readInt32(exception);
			chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
			notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
			exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				BotInfo object = BotInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				bot_info.add(object);
			}
			if ((flags & 16) != 0) {
				migrated_from_chat_id = stream.readInt32(exception);
			}
			if ((flags & 16) != 0) {
				migrated_from_max_id = stream.readInt32(exception);
			}
			if ((flags & 32) != 0) {
				pinned_msg_id = stream.readInt32(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = can_view_participants ? (flags | 8) : (flags &~ 8);
			flags = can_set_username ? (flags | 64) : (flags &~ 64);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			stream.writeString(about);
			if ((flags & 1) != 0) {
				stream.writeInt32(participants_count);
			}
			if ((flags & 2) != 0) {
				stream.writeInt32(admins_count);
			}
			if ((flags & 4) != 0) {
				stream.writeInt32(kicked_count);
			}
			stream.writeInt32(read_inbox_max_id);
			stream.writeInt32(read_outbox_max_id);
			stream.writeInt32(unread_count);
			chat_photo.serializeToStream(stream);
			notify_settings.serializeToStream(stream);
			exported_invite.serializeToStream(stream);
			stream.writeInt32(0x1cb5c415);
			int count = bot_info.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				bot_info.get(a).serializeToStream(stream);
			}
			if ((flags & 16) != 0) {
				stream.writeInt32(migrated_from_chat_id);
			}
			if ((flags & 16) != 0) {
				stream.writeInt32(migrated_from_max_id);
			}
			if ((flags & 32) != 0) {
				stream.writeInt32(pinned_msg_id);
			}
		}
	}

	public static class TL_null extends TLObject {
		public static int constructor = 0x56730bcc;


		public static TL_null TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_null.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_null", constructor));
				} else {
					return null;
				}
			}
			TL_null result = new TL_null();
			result.readParams(stream, exception);
			return result;
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_chatFull extends ChatFull {
		public static int constructor = 0x2e02a614;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			participants = ChatParticipants.TLdeserialize(stream, stream.readInt32(exception), exception);
			chat_photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
			notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
			exported_invite = ExportedChatInvite.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				BotInfo object = BotInfo.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				bot_info.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
			participants.serializeToStream(stream);
			chat_photo.serializeToStream(stream);
			notify_settings.serializeToStream(stream);
			exported_invite.serializeToStream(stream);
			stream.writeInt32(0x1cb5c415);
			int count = bot_info.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				bot_info.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_topPeerCategoryPeers extends TLObject {
		public static int constructor = 0xfb834291;

		public TopPeerCategory category;
		public int count;
		public ArrayList<TL_topPeer> peers = new ArrayList<>();

		public static TL_topPeerCategoryPeers TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_topPeerCategoryPeers.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_topPeerCategoryPeers", constructor));
				} else {
					return null;
				}
			}
			TL_topPeerCategoryPeers result = new TL_topPeerCategoryPeers();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			category = TopPeerCategory.TLdeserialize(stream, stream.readInt32(exception), exception);
			count = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_topPeer object = TL_topPeer.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				peers.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			category.serializeToStream(stream);
			stream.writeInt32(count);
			stream.writeInt32(0x1cb5c415);
			int count = peers.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				peers.get(a).serializeToStream(stream);
			}
		}
	}

	public static class InputUser extends TLObject {
		public int user_id;
		public long access_hash;

		public static InputUser TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputUser result = null;
			return result;
		}
	}

	public static class TL_inputUserEmpty extends InputUser {
		public static int constructor = 0xb98886cf;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputUserSelf extends InputUser {
		public static int constructor = 0xf7c1b13f;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputUser extends InputUser {
		public static int constructor = 0xd8292816;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt64(access_hash);
		}
	}

	public static class Bool extends TLObject {

		public static Bool TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Bool result = null;
			return result;
		}
	}

	public static class TL_boolTrue extends Bool {
		public static int constructor = 0x997275b5;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class WebPage extends TLObject {
		public int flags;
		public long id;
		public String url;
		public String display_url;
		public String type;
		public String site_name;
		public String title;
		public String description;
		public Photo photo;
		public String embed_url;
		public String embed_type;
		public int embed_width;
		public int embed_height;
		public int duration;
		public String author;
		public int date;
		public Document document;

		public static WebPage TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			WebPage result = null;
			return result;
		}
	}

	public static class TL_webPagePending extends WebPage {
		public static int constructor = 0xc586da1c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
			date = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt32(date);
		}
	}

	public static class TL_webPageEmpty extends WebPage {
		public static int constructor = 0xeb1477e8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
		}
	}

	public static class TL_webPageUrlPending extends WebPage {
		public static int constructor = 0xd41a5167;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			url = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(url);
		}
	}

    public static class TL_webPage extends WebPage {
        public static int constructor = 0xca820ed7;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception);
            id = stream.readInt64(exception);
            url = stream.readString(exception);
            display_url = stream.readString(exception);
            if ((flags & 1) != 0) {
				type = stream.readString(exception);
			}
            if ((flags & 2) != 0) {
                site_name = stream.readString(exception);
			}
            if ((flags & 4) != 0) {
				title = stream.readString(exception);
            }
            if ((flags & 8) != 0) {
                description = stream.readString(exception);
            }
			if ((flags & 16) != 0) {
                photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if ((flags & 32) != 0) {
                embed_url = stream.readString(exception);
            }
            if ((flags & 32) != 0) {
                embed_type = stream.readString(exception);
            }
            if ((flags & 64) != 0) {
				embed_width = stream.readInt32(exception);
			}
            if ((flags & 64) != 0) {
                embed_height = stream.readInt32(exception);
            }
            if ((flags & 128) != 0) {
                duration = stream.readInt32(exception);
            }
            if ((flags & 256) != 0) {
                author = stream.readString(exception);
            }
            if ((flags & 512) != 0) {
                document = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(flags);
            stream.writeInt64(id);
            stream.writeString(url);
            stream.writeString(display_url);
			if ((flags & 1) != 0) {
                stream.writeString(type);
			}
			if ((flags & 2) != 0) {
                stream.writeString(site_name);
            }
            if ((flags & 4) != 0) {
                stream.writeString(title);
            }
			if ((flags & 8) != 0) {
                stream.writeString(description);
			}
			if ((flags & 16) != 0) {
                photo.serializeToStream(stream);
            }
			if ((flags & 32) != 0) {
				stream.writeString(embed_url);
            }
			if ((flags & 32) != 0) {
                stream.writeString(embed_type);
            }
			if ((flags & 64) != 0) {
                stream.writeInt32(embed_width);
            }
            if ((flags & 64) != 0) {
                stream.writeInt32(embed_height);
            }
            if ((flags & 128) != 0) {
                stream.writeInt32(duration);
            }
            if ((flags & 256) != 0) {
                stream.writeString(author);
            }
            if ((flags & 512) != 0) {
                document.serializeToStream(stream);
			}
        }
    }

	public static class TL_auth_passwordRecovery extends TLObject {
		public static int constructor = 0x137948a5;

		public String email_pattern;

		public static TL_auth_passwordRecovery TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_auth_passwordRecovery.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_auth_passwordRecovery", constructor));
				} else {
					return null;
				}
			}
			TL_auth_passwordRecovery result = new TL_auth_passwordRecovery();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			email_pattern = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(email_pattern);
		}
	}

	public static class TL_botCommand extends TLObject {
		public static int constructor = 0xc27ac8c7;

		public String command;
		public String description;

		public static TL_botCommand TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_botCommand.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_botCommand", constructor));
				} else {
					return null;
				}
			}
			TL_botCommand result = new TL_botCommand();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			command = stream.readString(exception);
			description = stream.readString(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(command);
			stream.writeString(description);
		}
	}

    public static class InputFileLocation extends TLObject {
		public long id;
		public long access_hash;
		public long volume_id;
		public int local_id;
		public long secret;

		public static InputFileLocation TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputFileLocation result = null;
			return result;
		}
	}

	public static class TL_inputEncryptedFileLocation extends InputFileLocation {
		public static int constructor = 0xf5235d55;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_inputDocumentFileLocation extends InputFileLocation {
        public static int constructor = 0x4e45abe9;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_inputFileLocation extends InputFileLocation {
		public static int constructor = 0x14637196;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			volume_id = stream.readInt64(exception);
			local_id = stream.readInt32(exception);
			secret = stream.readInt64(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(volume_id);
			stream.writeInt32(local_id);
			stream.writeInt64(secret);
		}
	}

	public static class TL_photos_photo extends TLObject {
		public static int constructor = 0x20212ca8;

		public Photo photo;
		public ArrayList<User> users = new ArrayList<>();

        public static TL_photos_photo TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_photos_photo.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_photos_photo", constructor));
				} else {
					return null;
				}
			}
			TL_photos_photo result = new TL_photos_photo();
			result.readParams(stream, exception);
			return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
			photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            photo.serializeToStream(stream);
			stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                users.get(a).serializeToStream(stream);
			}
		}
	}

	public static class User extends TLObject {
		public int id;
		public String first_name;
		public String last_name;
		public String username;
		public long access_hash;
        public String phone;
		public UserProfilePhoto photo;
		public UserStatus status;
		public boolean inactive;
        public int flags;
		public boolean self;
		public boolean contact;
		public boolean mutual_contact;
		public boolean deleted;
		public boolean bot;
		public boolean bot_chat_history;
		public boolean bot_nochats;
		public boolean verified;
		public boolean explicit_content;
		public int bot_info_version;
		public boolean restricted;
		public boolean min;
		public boolean bot_inline_geo;
		public String restriction_reason;
		public String bot_inline_placeholder;

		public static User TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			User result = null;
			return result;
        }
    }

	public static class TL_userContact_old2 extends User {
		public static int constructor = 0xcab35e18;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt32(exception);
			first_name = stream.readString(exception);
			last_name = stream.readString(exception);
			username = stream.readString(exception);
            access_hash = stream.readInt64(exception);
			phone = stream.readString(exception);
			photo = UserProfilePhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeInt32(id);
			stream.writeString(first_name);
			stream.writeString(last_name);
            stream.writeString(username);
			stream.writeInt64(access_hash);
			stream.writeString(phone);
			photo.serializeToStream(stream);
			status.serializeToStream(stream);
		}
	}

	public static class TL_userSelf_old3 extends User {
		public static int constructor = 0x1c60e608;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			first_name = stream.readString(exception);
            last_name = stream.readString(exception);
			username = stream.readString(exception);
			phone = stream.readString(exception);
            photo = UserProfilePhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
            status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
			stream.writeString(first_name);
            stream.writeString(last_name);
            stream.writeString(username);
			stream.writeString(phone);
            photo.serializeToStream(stream);
			status.serializeToStream(stream);
		}
	}

	public static class TL_userEmpty extends User {
		public static int constructor = 0x200250ba;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
		}
	}

	public static class TL_userForeign_old2 extends User {
		public static int constructor = 0x75cf7a8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			first_name = stream.readString(exception);
			last_name = stream.readString(exception);
			username = stream.readString(exception);
			access_hash = stream.readInt64(exception);
			photo = UserProfilePhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			status = UserStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
            stream.writeString(first_name);
            stream.writeString(last_name);
			stream.writeString(username);
			stream.writeInt64(access_hash);
			photo.serializeToStream(stream);
			status.serializeToStream(stream);
		}
	}

	public static class MessageAction extends TLObject {
		public String title;
		public String address;
		public DecryptedMessageAction encryptedAction;
		public ArrayList<Integer> users = new ArrayList<>();
		public int channel_id;
		public Photo photo;
		public int chat_id;
		public int user_id;
		public UserProfilePhoto newUserPhoto;
		public int inviter_id;
		public int ttl;

		public static MessageAction TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			MessageAction result = null;
			return result;
		}
	}

	public static class TL_messageActionLoginUnknownLocation extends MessageAction {
		public static int constructor = 0x555555F5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			title = stream.readString(exception);
			address = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(title);
			stream.writeString(address);
		}
	}

	public static class TL_messageEncryptedAction extends MessageAction {
		public static int constructor = 0x555555F7;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			encryptedAction = DecryptedMessageAction.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			encryptedAction.serializeToStream(stream);
		}
	}

	public static class TL_messageActionChatCreate extends MessageAction {
		public static int constructor = 0xa6638b9a;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			title = stream.readString(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				users.add(stream.readInt32(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(title);
			stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(users.get(a));
			}
		}
	}

	public static class TL_messageActionHistoryClear extends MessageAction {
		public static int constructor = 0x9fbab604;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageActionChatEditPhoto extends MessageAction {
		public static int constructor = 0x7fcb13a8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			photo = Photo.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			photo.serializeToStream(stream);
		}
	}

	public static class TL_messageActionChatAddUser extends MessageAction {
		public static int constructor = 0x488a7337;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				users.add(stream.readInt32(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(users.get(a));
			}
		}
	}

	public static class TL_messageActionChatDeleteUser extends MessageAction {
		public static int constructor = 0xb2ae9b0c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
		}
	}

	public static class TL_messageActionCreatedBroadcastList extends MessageAction {
		public static int constructor = 0x55555557;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageActionUserJoined extends MessageAction {
		public static int constructor = 0x55555550;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageActionUserUpdatedPhoto extends MessageAction {
		public static int constructor = 0x55555551;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			newUserPhoto = UserProfilePhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			newUserPhoto.serializeToStream(stream);
		}
	}

	public static class TL_messageActionChatJoinedByLink extends MessageAction {
		public static int constructor = 0xf89cf5e8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			inviter_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(inviter_id);
		}
	}

	public static class TL_messageActionTTLChange extends MessageAction {
		public static int constructor = 0x55555552;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			ttl = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(ttl);
		}
	}

	public static class TL_messageActionChannelCreate extends MessageAction {
		public static int constructor = 0x95d2ac92;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			title = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(title);
		}
	}

	public static class TL_messageActionPinMessage extends MessageAction {
		public static int constructor = 0x94bd38ed;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageActionChatDeletePhoto extends MessageAction {
		public static int constructor = 0x95e3fbef;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageActionChatEditTitle extends MessageAction {
		public static int constructor = 0xb5a1ce5a;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			title = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(title);
		}
	}

	public static class TL_messageActionEmpty extends MessageAction {
		public static int constructor = 0xb6aef7b0;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class DecryptedMessage extends TLObject {
		public long random_id;
		public int ttl;
		public String message;
		public DecryptedMessageMedia media;
		public DecryptedMessageAction action;
		public byte[] random_bytes;
		public int flags;
		public ArrayList<MessageEntity> entities = new ArrayList<>();
		public String via_bot_name;
		public long reply_to_random_id;

		public static DecryptedMessage TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			DecryptedMessage result = null;
			return result;
		}
	}

	public static class TL_decryptedMessage_layer17 extends TL_decryptedMessage {
		public static int constructor = 0x204d3878;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			random_id = stream.readInt64(exception);
			ttl = stream.readInt32(exception);
			message = stream.readString(exception);
			media = DecryptedMessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(random_id);
			stream.writeInt32(ttl);
			stream.writeString(message);
			media.serializeToStream(stream);
		}
	}

	public static class TL_decryptedMessageService extends DecryptedMessage {
		public static int constructor = 0x73164160;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			random_id = stream.readInt64(exception);
			action = DecryptedMessageAction.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(random_id);
			action.serializeToStream(stream);
		}
	}

	public static class TL_decryptedMessageService_layer8 extends TL_decryptedMessageService {
		public static int constructor = 0xaa48327d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			random_id = stream.readInt64(exception);
			random_bytes = stream.readByteArray(exception);
			action = DecryptedMessageAction.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(random_id);
			stream.writeByteArray(random_bytes);
			action.serializeToStream(stream);
		}
	}

	public static class TL_decryptedMessage_layer8 extends TL_decryptedMessage {
		public static int constructor = 0x1f814f1f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			random_id = stream.readInt64(exception);
			random_bytes = stream.readByteArray(exception);
			message = stream.readString(exception);
			media = DecryptedMessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(random_id);
			stream.writeByteArray(random_bytes);
			stream.writeString(message);
			media.serializeToStream(stream);
		}
	}

	public static class TL_decryptedMessage extends DecryptedMessage {
		public static int constructor = 0x36b091de;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			random_id = stream.readInt64(exception);
			ttl = stream.readInt32(exception);
			message = stream.readString(exception);
			if ((flags & 512) != 0) {
				media = DecryptedMessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 128) != 0) {
				int magic = stream.readInt32(exception);
				if (magic != 0x1cb5c415) {
					if (exception) {
						throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
					}
					return;
				}
				int count = stream.readInt32(exception);
				for (int a = 0; a < count; a++) {
					MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
					if (object == null) {
						return;
					}
					entities.add(object);
				}
			}
			if ((flags & 2048) != 0) {
				via_bot_name = stream.readString(exception);
			}
			if ((flags & 8) != 0) {
				reply_to_random_id = stream.readInt64(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(flags);
			stream.writeInt64(random_id);
			stream.writeInt32(ttl);
			stream.writeString(message);
			if ((flags & 512) != 0) {
				media.serializeToStream(stream);
			}
			if ((flags & 128) != 0) {
				stream.writeInt32(0x1cb5c415);
				int count = entities.size();
				stream.writeInt32(count);
				for (int a = 0; a < count; a++) {
					entities.get(a).serializeToStream(stream);
				}
			}
			if ((flags & 2048) != 0) {
				stream.writeString(via_bot_name);
			}
			if ((flags & 8) != 0) {
				stream.writeInt64(reply_to_random_id);
			}
		}
	}

	public static class Video extends TLObject {
		public long id;
		public long access_hash;
		public int user_id;
		public int date;
		public int duration;
		public int size;
		public PhotoSize thumb;
		public int dc_id;
		public int w;
		public int h;
		public String mime_type;
		public String caption;
		public byte[] key;
		public byte[] iv;

		public static Video TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Video result = null;
			return result;
		}
	}

	public static class TopPeerCategory extends TLObject {

		public static TopPeerCategory TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			TopPeerCategory result = null;
			return result;
		}
	}

	public static class TL_topPeerCategoryCorrespondents extends TopPeerCategory {
		public static int constructor = 0x637b7ed;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_topPeerCategoryBotsInline extends TopPeerCategory {
		public static int constructor = 0x148677e2;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_contactBlocked extends TLObject {
		public static int constructor = 0x561bc879;

		public int user_id;
		public int date;

		public static TL_contactBlocked TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_contactBlocked.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_contactBlocked", constructor));
				} else {
					return null;
				}
			}
			TL_contactBlocked result = new TL_contactBlocked();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			date = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt32(date);
		}
	}

	public static class InputDocument extends TLObject {
		public long id;
		public long access_hash;

		public static InputDocument TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputDocument result = null;
			return result;
		}
	}

	public static class TL_inputDocument extends InputDocument {
		public static int constructor = 0x18798952;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
		}
	}

	public static class Document extends TLObject {
		public long id;
		public long access_hash;
		public int user_id;
		public int date;
		public String file_name;
		public String mime_type;
		public int size;
		public PhotoSize thumb;
		public int dc_id;
		public byte[] key;
		public byte[] iv;
		public String caption;
		public ArrayList<DocumentAttribute> attributes = new ArrayList<>();

		public static Document TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Document result = null;
			return result;
		}
	}

	public static class TL_documentEmpty extends Document {
		public static int constructor = 0x36f8c871;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
		}
	}

	public static class TL_documentEncrypted extends Document {
		public static int constructor = 0x55555558;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
			date = stream.readInt32(exception);
			int startReadPosiition = stream.getPosition(); //TODO remove this hack after some time
			try {
				mime_type = stream.readString(true);
			} catch (Exception e) {
				mime_type = "audio/ogg";
				if (stream instanceof NativeByteBuffer) {
					((NativeByteBuffer) stream).position(startReadPosiition);
				}
			}
			size = stream.readInt32(exception);
			thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
			dc_id = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				attributes.add(object);
			}
			key = stream.readByteArray(exception);
			iv = stream.readByteArray(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
			stream.writeInt32(date);
			stream.writeString(mime_type);
			stream.writeInt32(size);
			thumb.serializeToStream(stream);
			stream.writeInt32(dc_id);
			stream.writeInt32(0x1cb5c415);
			int count = attributes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				attributes.get(a).serializeToStream(stream);
			}
			stream.writeByteArray(key);
			stream.writeByteArray(iv);
		}
	}

	public static class TL_document extends Document {
		public static int constructor = 0xf9a39f4f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
			date = stream.readInt32(exception);
			mime_type = stream.readString(exception);
			size = stream.readInt32(exception);
			thumb = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
			dc_id = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				attributes.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt64(id);
            stream.writeInt64(access_hash);
			stream.writeInt32(date);
            stream.writeString(mime_type);
			stream.writeInt32(size);
            thumb.serializeToStream(stream);
			stream.writeInt32(dc_id);
            stream.writeInt32(0x1cb5c415);
			int count = attributes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				attributes.get(a).serializeToStream(stream);
			}
		}
	}

	public static class InputMedia extends TLObject {
		public InputFile file;
		public InputFile thumb;
		public String mime_type;
		public ArrayList<DocumentAttribute> attributes = new ArrayList<>();
		public String caption;
		public InputGeoPoint geo_point;
		public String title;
		public String address;
		public String provider;
		public String venue_id;
		public String phone_number;
		public String first_name;
		public String last_name;
		public String url;
		public String q;

		public static InputMedia TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputMedia result = null;
			return result;
		}
	}

	public static class TL_inputMediaContact extends InputMedia {
		public static int constructor = 0xa6e45987;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			phone_number = stream.readString(exception);
			first_name = stream.readString(exception);
			last_name = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(first_name);
			stream.writeString(last_name);
		}
	}

	public static class TL_inputMediaUploadedThumbDocument extends InputMedia {
		public static int constructor = 0xad613491;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			file = InputFile.TLdeserialize(stream, stream.readInt32(exception), exception);
			thumb = InputFile.TLdeserialize(stream, stream.readInt32(exception), exception);
			mime_type = stream.readString(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				attributes.add(object);
			}
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			file.serializeToStream(stream);
			thumb.serializeToStream(stream);
			stream.writeString(mime_type);
			stream.writeInt32(0x1cb5c415);
			int count = attributes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				attributes.get(a).serializeToStream(stream);
			}
			stream.writeString(caption);
		}
	}

	public static class TL_inputMediaDocument extends InputMedia {
		public static int constructor = 0x1a77f29c;

		public InputDocument id;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = InputDocument.TLdeserialize(stream, stream.readInt32(exception), exception);
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
			stream.writeString(caption);
		}
	}

	public static class TL_inputMediaGifExternal extends InputMedia {
		public static int constructor = 0x4843b0fd;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			url = stream.readString(exception);
			q = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(url);
			stream.writeString(q);
		}
	}

	public static class TL_inputMediaGeoPoint extends InputMedia {
		public static int constructor = 0xf9c44144;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			geo_point = InputGeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			geo_point.serializeToStream(stream);
		}
	}

	public static class TL_inputMediaUploadedPhoto extends InputMedia {
		public static int constructor = 0xf7aff1c0;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			file = InputFile.TLdeserialize(stream, stream.readInt32(exception), exception);
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			file.serializeToStream(stream);
			stream.writeString(caption);
		}
	}

	public static class TL_inputMediaVenue extends InputMedia {
		public static int constructor = 0x2827a81a;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			geo_point = InputGeoPoint.TLdeserialize(stream, stream.readInt32(exception), exception);
			title = stream.readString(exception);
			address = stream.readString(exception);
			provider = stream.readString(exception);
			venue_id = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			geo_point.serializeToStream(stream);
			stream.writeString(title);
			stream.writeString(address);
			stream.writeString(provider);
			stream.writeString(venue_id);
		}
	}

	public static class TL_inputMediaUploadedDocument extends InputMedia {
		public static int constructor = 0x1d89306d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			file = InputFile.TLdeserialize(stream, stream.readInt32(exception), exception);
			mime_type = stream.readString(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				DocumentAttribute object = DocumentAttribute.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				attributes.add(object);
			}
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			file.serializeToStream(stream);
			stream.writeString(mime_type);
			stream.writeInt32(0x1cb5c415);
			int count = attributes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				attributes.get(a).serializeToStream(stream);
			}
			stream.writeString(caption);
		}
	}

	public static class TL_inputMediaPhoto extends InputMedia {
		public static int constructor = 0xe9bfb4f3;

		public InputPhoto id;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = InputPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			caption = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
			stream.writeString(caption);
		}
	}

	public static class InputStickerSet extends TLObject {
		public long id;
		public long access_hash;
		public String short_name;

		public static InputStickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputStickerSet result = null;
			return result;
		}
	}

	public static class TL_inputStickerSetEmpty extends InputStickerSet {
		public static int constructor = 0xffb62b95;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputStickerSetID extends InputStickerSet {
		public static int constructor = 0x9de7a269;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_inputStickerSetShortName extends InputStickerSet {
		public static int constructor = 0x861cc8a0;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			short_name = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(short_name);
		}
	}

	public static class UserStatus extends TLObject {
		public int expires;

		public static UserStatus TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			UserStatus result = null;
			return result;
		}
	}

	public static class TL_userStatusLastWeek extends UserStatus {
		public static int constructor = 0x7bf09fc;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_userStatusLastMonth extends UserStatus {
		public static int constructor = 0x77ebc742;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
        }
    }

	public static class TL_userStatusRecently extends UserStatus {
		public static int constructor = 0xe26f42f1;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_contacts_importedContacts extends TLObject {
		public static int constructor = 0xad524315;

		public ArrayList<TL_importedContact> imported = new ArrayList<>();
		public ArrayList<Long> retry_contacts = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();

		public static TL_contacts_importedContacts TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_contacts_importedContacts.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_contacts_importedContacts", constructor));
				} else {
					return null;
				}
			}
			TL_contacts_importedContacts result = new TL_contacts_importedContacts();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_importedContact object = TL_importedContact.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				imported.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				retry_contacts.add(stream.readInt64(exception));
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = imported.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				imported.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = retry_contacts.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt64(retry_contacts.get(a));
			}
			stream.writeInt32(0x1cb5c415);
			count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_disabledFeature extends TLObject {
		public static int constructor = 0xae636f24;

		public String feature;
		public String description;

		public static TL_disabledFeature TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_disabledFeature.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_disabledFeature", constructor));
				} else {
					return null;
				}
			}
			TL_disabledFeature result = new TL_disabledFeature();
			result.readParams(stream, exception);
			return result;
		}

        public void readParams(AbstractSerializedData stream, boolean exception) {
            feature = stream.readString(exception);
			description = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeString(feature);
			stream.writeString(description);
		}
	}

	public static class TL_inlineBotSwitchPM extends TLObject {
		public static int constructor = 0x3c20629f;

		public String text;
		public String start_param;

		public static TL_inlineBotSwitchPM TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_inlineBotSwitchPM.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_inlineBotSwitchPM", constructor));
				} else {
					return null;
				}
			}
			TL_inlineBotSwitchPM result = new TL_inlineBotSwitchPM();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			text = stream.readString(exception);
			start_param = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(text);
			stream.writeString(start_param);
		}
	}

	public static class Update extends TLObject {
		public int chat_id;
		public int user_id;
		public int inviter_id;
		public int date;
		public int version;
		public DraftMessage draft;
		public int pts;
		public int pts_count;
		public long query_id;
		public byte[] data;
		public int flags;
		public String query;
		public GeoPoint geo;
		public String offset;
		public int channel_id;
		public SendMessageAction action;
		public boolean blocked;
		public String location;
		public int max_id;
		public int qts;
		public boolean enabled;
		public PrivacyKey key;
		public ArrayList<PrivacyRule> rules = new ArrayList<>();
		public UserStatus status;
		public int views;
		public String type;
		public MessageMedia media;
		public String first_name;
		public String last_name;
		public String username;
		public ArrayList<Integer> messages = new ArrayList<>();
		public String phone;
		public WebPage webpage;
		public EncryptedChat chat;
		public ArrayList<Long> order = new ArrayList<>();
		public UserProfilePhoto photo;
		public boolean previous;

		public static Update TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Update result = null;
			return result;
		}
	}

	public static class TL_updateReadHistoryInbox extends Update {
		public static int constructor = 0x9961fd5c;

		public Peer peer;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			max_id = stream.readInt32(exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			peer.serializeToStream(stream);
			stream.writeInt32(max_id);
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateEditMessage extends Update {
		public static int constructor = 0xe40370a3;

		public Message message;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			message = Message.TLdeserialize(stream, stream.readInt32(exception), exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			message.serializeToStream(stream);
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateWebPage extends Update {
		public static int constructor = 0x7f891213;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			webpage = WebPage.TLdeserialize(stream, stream.readInt32(exception), exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			webpage.serializeToStream(stream);
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateNewMessage extends Update {
		public static int constructor = 0x1f2b0afd;

		public Message message;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			message = Message.TLdeserialize(stream, stream.readInt32(exception), exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			message.serializeToStream(stream);
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateReadMessagesContents extends Update {
		public static int constructor = 0x68c13933;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				messages.add(stream.readInt32(exception));
			}
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = messages.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(messages.get(a));
			}
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateDeleteMessages extends Update {
		public static int constructor = 0xa20db0e5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				messages.add(stream.readInt32(exception));
			}
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = messages.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt32(messages.get(a));
			}
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class TL_updateReadHistoryOutbox extends Update {
		public static int constructor = 0x2f2f21bf;

		public Peer peer;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			max_id = stream.readInt32(exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			peer.serializeToStream(stream);
			stream.writeInt32(max_id);
			stream.writeInt32(pts);
			stream.writeInt32(pts_count);
		}
	}

	public static class InputEncryptedFile extends TLObject {
		public long id;
		public long access_hash;
		public int parts;
		public int key_fingerprint;
		public String md5_checksum;

        public static InputEncryptedFile TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputEncryptedFile result = null;
			return result;
		}
	}

	public static class TL_inputEncryptedFileBigUploaded extends InputEncryptedFile {
		public static int constructor = 0x2dc173c8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
            parts = stream.readInt32(exception);
			key_fingerprint = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(id);
			stream.writeInt32(parts);
			stream.writeInt32(key_fingerprint);
		}
	}

	public static class TL_inputEncryptedFileUploaded extends InputEncryptedFile {
		public static int constructor = 0x64bd0306;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			parts = stream.readInt32(exception);
			md5_checksum = stream.readString(exception);
			key_fingerprint = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt32(parts);
			stream.writeString(md5_checksum);
			stream.writeInt32(key_fingerprint);
		}
	}

	public static class messages_AllStickers extends TLObject {
		public String hash;
		public ArrayList<StickerSet> sets = new ArrayList<>();
		public ArrayList<TL_stickerPack> packs = new ArrayList<>();
		public ArrayList<Document> documents = new ArrayList<>();

		public static messages_AllStickers TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			messages_AllStickers result = null;
			return result;
		}
	}

	public static class TL_messages_allStickers extends messages_AllStickers {
		public static int constructor = 0xedfd405f;

		public int hash;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			hash = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				StickerSet object = StickerSet.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				sets.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(hash);
			stream.writeInt32(0x1cb5c415);
			int count = sets.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				sets.get(a).serializeToStream(stream);
			}
		}
	}

	public static class DecryptedMessageAction extends TLObject {
		public int ttl_seconds;
		public int layer;
		public ArrayList<Long> random_ids = new ArrayList<>();
		public SendMessageAction action;

		public static DecryptedMessageAction TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			DecryptedMessageAction result = null;
			return result;
		}
	}

	public static class TL_decryptedMessageActionSetMessageTTL extends DecryptedMessageAction {
		public static int constructor = 0xa1733aec;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			ttl_seconds = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(ttl_seconds);
		}
	}

	public static class TL_decryptedMessageActionScreenshotMessages extends DecryptedMessageAction {
		public static int constructor = 0x8ac1f475;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				random_ids.add(stream.readInt64(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
			int count = random_ids.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt64(random_ids.get(a));
			}
		}
    }

	public static class account_Password extends TLObject {
		public byte[] current_salt;
		public byte[] new_salt;
		public String hint;
		public boolean has_recovery;
		public String email_unconfirmed_pattern;

		public static account_Password TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            account_Password result = null;
			return result;
		}
	}

	public static class TL_account_password extends account_Password {
		public static int constructor = 0x7c18141c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			current_salt = stream.readByteArray(exception);
			new_salt = stream.readByteArray(exception);
			hint = stream.readString(exception);
			has_recovery = stream.readBool(exception);
			email_unconfirmed_pattern = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeByteArray(current_salt);
			stream.writeByteArray(new_salt);
			stream.writeString(hint);
			stream.writeBool(has_recovery);
			stream.writeString(email_unconfirmed_pattern);
		}
	}

	public static class UserProfilePhoto extends TLObject {
		public long photo_id;
		public FileLocation photo_small;
		public FileLocation photo_big;

		public static UserProfilePhoto TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			UserProfilePhoto result = null;
			return result;
		}
	}

	public static class TL_userProfilePhotoEmpty extends UserProfilePhoto {
		public static int constructor = 0x4f11bae1;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_userProfilePhoto extends UserProfilePhoto {
        public static int constructor = 0xd559d8c8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			photo_id = stream.readInt64(exception);
			photo_small = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
			photo_big = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(photo_id);
			photo_small.serializeToStream(stream);
			photo_big.serializeToStream(stream);
		}
	}

	public static class MessageEntity extends TLObject {
		public int offset;
		public int length;
		public String url;
		public String language;

		public static MessageEntity TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			MessageEntity result = null;
			return result;
		}
	}

	public static class TL_messageEntityTextUrl extends MessageEntity {
		public static int constructor = 0x76a6d327;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
			url = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
			stream.writeString(url);
		}
	}

	public static class TL_messageEntityBotCommand extends MessageEntity {
		public static int constructor = 0x6cef8ac7;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityEmail extends MessageEntity {
		public static int constructor = 0x64e475c2;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityPre extends MessageEntity {
		public static int constructor = 0x73924be0;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
			language = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
			stream.writeString(language);
		}
	}

	public static class TL_messageEntityUrl extends MessageEntity {
		public static int constructor = 0x6ed02538;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityItalic extends MessageEntity {
		public static int constructor = 0x826f8b60;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityMention extends MessageEntity {
		public static int constructor = 0xfa04579d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityMentionName extends MessageEntity {
		public static int constructor = 0x352dca58;

		public int user_id;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
			user_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
			stream.writeInt32(user_id);
		}
	}

	public static class TL_inputMessageEntityMentionName extends MessageEntity {
		public static int constructor = 0x208e68c9;

		public InputUser user_id;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
			user_id = InputUser.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
			user_id.serializeToStream(stream);
		}
	}

	public static class TL_messageEntityBold extends MessageEntity {
		public static int constructor = 0xbd610bc9;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityHashtag extends MessageEntity {
		public static int constructor = 0x6f635b0d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class TL_messageEntityCode extends MessageEntity {
		public static int constructor = 0x28a20571;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			offset = stream.readInt32(exception);
			length = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(length);
		}
	}

	public static class Photo extends TLObject {
		public long id;
		public long access_hash;
		public int user_id;
		public int date;
		public String caption;
		public GeoPoint geo;
		public ArrayList<PhotoSize> sizes = new ArrayList<>();

		public static Photo TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Photo result = null;
			return result;
		}
	}

	public static class TL_photo extends Photo {
		public static int constructor = 0xcded42fe;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
			date = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
                PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				sizes.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
			stream.writeInt32(date);
			stream.writeInt32(0x1cb5c415);
            int count = sizes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
                sizes.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_photoEmpty extends Photo {
        public static int constructor = 0x2331b22d;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
		}
	}

	public static class TL_encryptedChat extends EncryptedChat {
		public static int constructor = 0xfa56ce36;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt32(exception);
			access_hash = stream.readInt64(exception);
			date = stream.readInt32(exception);
			admin_id = stream.readInt32(exception);
            participant_id = stream.readInt32(exception);
			g_a_or_b = stream.readByteArray(exception);
			key_fingerprint = stream.readInt64(exception);
        }

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(id);
			stream.writeInt64(access_hash);
			stream.writeInt32(date);
			stream.writeInt32(admin_id);
			stream.writeInt32(participant_id);
			stream.writeByteArray(g_a_or_b);
			stream.writeInt64(key_fingerprint);
		}
	}

	public static class TL_contact extends TLObject {
		public static int constructor = 0xf911c994;

		public int user_id;
		public boolean mutual;

		public static TL_contact TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_contact.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_contact", constructor));
				} else {
					return null;
				}
			}
            TL_contact result = new TL_contact();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			mutual = stream.readBool(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeBool(mutual);
		}
	}

	public static class TL_config extends TLObject {
		public static int constructor = 0xc9411388;

		public int date;
		public int expires;
		public boolean test_mode;
		public int this_dc;
		public ArrayList<TL_dcOption> dc_options = new ArrayList<>();
		public int chat_size_max;
		public int megagroup_size_max;
		public int forwarded_count_max;
		public int online_update_period_ms;
		public int offline_blur_timeout_ms;
		public int offline_idle_timeout_ms;
		public int online_cloud_timeout_ms;
		public int notify_cloud_delay_ms;
		public int notify_default_delay_ms;
		public int chat_big_size;
		public int push_chat_period_ms;
		public int push_chat_limit;
		public int saved_gifs_limit;
		public int edit_time_limit;
		public int rating_e_decay;
		public ArrayList<TL_disabledFeature> disabled_features = new ArrayList<>();

		public static TL_config TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_config.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_config", constructor));
				} else {
					return null;
				}
			}
			TL_config result = new TL_config();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			date = stream.readInt32(exception);
			expires = stream.readInt32(exception);
			test_mode = stream.readBool(exception);
			this_dc = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_dcOption object = TL_dcOption.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				dc_options.add(object);
			}
			chat_size_max = stream.readInt32(exception);
			megagroup_size_max = stream.readInt32(exception);
			forwarded_count_max = stream.readInt32(exception);
			online_update_period_ms = stream.readInt32(exception);
			offline_blur_timeout_ms = stream.readInt32(exception);
			offline_idle_timeout_ms = stream.readInt32(exception);
			online_cloud_timeout_ms = stream.readInt32(exception);
			notify_cloud_delay_ms = stream.readInt32(exception);
			notify_default_delay_ms = stream.readInt32(exception);
			chat_big_size = stream.readInt32(exception);
			push_chat_period_ms = stream.readInt32(exception);
			push_chat_limit = stream.readInt32(exception);
			saved_gifs_limit = stream.readInt32(exception);
			edit_time_limit = stream.readInt32(exception);
			rating_e_decay = stream.readInt32(exception);
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_disabledFeature object = TL_disabledFeature.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				disabled_features.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(date);
			stream.writeInt32(expires);
			stream.writeBool(test_mode);
			stream.writeInt32(this_dc);
			stream.writeInt32(0x1cb5c415);
			int count = dc_options.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				dc_options.get(a).serializeToStream(stream);
			}
			stream.writeInt32(chat_size_max);
			stream.writeInt32(megagroup_size_max);
			stream.writeInt32(forwarded_count_max);
			stream.writeInt32(online_update_period_ms);
			stream.writeInt32(offline_blur_timeout_ms);
			stream.writeInt32(offline_idle_timeout_ms);
			stream.writeInt32(online_cloud_timeout_ms);
			stream.writeInt32(notify_cloud_delay_ms);
			stream.writeInt32(notify_default_delay_ms);
			stream.writeInt32(chat_big_size);
			stream.writeInt32(push_chat_period_ms);
			stream.writeInt32(push_chat_limit);
			stream.writeInt32(saved_gifs_limit);
			stream.writeInt32(edit_time_limit);
			stream.writeInt32(rating_e_decay);
			stream.writeInt32(0x1cb5c415);
			count = disabled_features.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				disabled_features.get(a).serializeToStream(stream);
			}
		}
	}

	public static class contacts_TopPeers extends TLObject {
		public ArrayList<TL_topPeerCategoryPeers> categories = new ArrayList<>();
		public ArrayList<Chat> chats = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();

		public static contacts_TopPeers TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			contacts_TopPeers result = null;
			return result;
		}
	}

	public static class TL_contacts_topPeers extends contacts_TopPeers {
		public static int constructor = 0x70b772a8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_topPeerCategoryPeers object = TL_topPeerCategoryPeers.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				categories.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				chats.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = categories.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				categories.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = chats.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				chats.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
		}
	}

    public static class InputChannel extends TLObject {
        public int channel_id;
        public long access_hash;

        public static InputChannel TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            InputChannel result = null;
            return result;
        }
    }

    public static class TL_inputChannelEmpty extends InputChannel {
        public static int constructor = 0xee8c1e86;


        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
		}
	}

    public static class TL_inputChannel extends InputChannel {
        public static int constructor = 0xafeb712e;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            channel_id = stream.readInt32(exception);
            access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(channel_id);
            stream.writeInt64(access_hash);
		}
	}

    public static class TL_contacts_resolvedPeer extends TLObject {
        public static int constructor = 0x7f077ad9;

        public Peer peer;
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_contacts_resolvedPeer TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_contacts_resolvedPeer.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_contacts_resolvedPeer", constructor));
                } else {
                    return null;
                }
            }
            TL_contacts_resolvedPeer result = new TL_contacts_resolvedPeer();
			result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                chats.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                users.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
            int count = chats.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                chats.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = users.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                users.get(a).serializeToStream(stream);
            }
        }
    }

    public static class TL_contacts_found extends TLObject {
        public static int constructor = 0x1aa1f784;

        public ArrayList<Peer> results = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();

        public static TL_contacts_found TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_contacts_found.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_contacts_found", constructor));
                } else {
                    return null;
                }
            }
            TL_contacts_found result = new TL_contacts_found();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            int magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            int count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Peer object = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                results.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                chats.add(object);
            }
            magic = stream.readInt32(exception);
            if (magic != 0x1cb5c415) {
                if (exception) {
                    throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
                }
                return;
            }
            count = stream.readInt32(exception);
            for (int a = 0; a < count; a++) {
                User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
                if (object == null) {
                    return;
                }
                users.add(object);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
            int count = results.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                results.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = chats.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                chats.get(a).serializeToStream(stream);
            }
            stream.writeInt32(0x1cb5c415);
            count = users.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                users.get(a).serializeToStream(stream);
            }
        }
    }

	public static class ChatParticipants extends TLObject {
		public int flags;
		public int chat_id;
		public ChatParticipant self_participant;
		public ArrayList<ChatParticipant> participants = new ArrayList<>();
		public int version;
		public int admin_id;

		public static ChatParticipants TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ChatParticipants result = null;
			return result;
		}
	}

	public static class TL_chatParticipantsForbidden extends ChatParticipants {
		public static int constructor = 0xfc900c2b;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			chat_id = stream.readInt32(exception);
			if ((flags & 1) != 0) {
				self_participant = ChatParticipant.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(flags);
			stream.writeInt32(chat_id);
			if ((flags & 1) != 0) {
				self_participant.serializeToStream(stream);
			}
		}
	}

	public static class TL_chatParticipants extends ChatParticipants {
		public static int constructor = 0x3f460fed;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			chat_id = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				ChatParticipant object = ChatParticipant.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				participants.add(object);
			}
			version = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(chat_id);
			stream.writeInt32(0x1cb5c415);
			int count = participants.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				participants.get(a).serializeToStream(stream);
			}
			stream.writeInt32(version);
		}
	}

	public static class DecryptedMessageMedia extends TLObject {
		public int duration;
		public String mime_type;
		public int size;
		public byte[] key;
		public byte[] iv;
		public double lat;
		public double _long;
		public String phone_number;
		public String first_name;
		public String last_name;
		public int user_id;
		public int thumb_w;
		public int thumb_h;
		public ArrayList<DocumentAttribute> attributes = new ArrayList<>();
		public String caption;
		public String url;
		public int w;
		public int h;
		public String file_name;
		public String title;
		public String address;
		public String provider;
		public String venue_id;
		public long id;
		public long access_hash;
		public int date;
		public int dc_id;

		public static DecryptedMessageMedia TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			DecryptedMessageMedia result = null;
			return result;
		}
	}

	public static class ChatParticipant extends TLObject {
		public int user_id;
		public int inviter_id;
		public int date;

		public static ChatParticipant TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ChatParticipant result = null;
			return result;
		}
	}

	public static class TL_chatParticipant extends ChatParticipant {
		public static int constructor = 0xc8d7493e;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			inviter_id = stream.readInt32(exception);
			date = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt32(inviter_id);
			stream.writeInt32(date);
		}
	}

	public static class TL_chatParticipantCreator extends ChatParticipant {
		public static int constructor = 0xda13538a;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
		}
	}

	public static class TL_chatParticipantAdmin extends ChatParticipant {
		public static int constructor = 0xe2d6e436;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			inviter_id = stream.readInt32(exception);
			date = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt32(inviter_id);
			stream.writeInt32(date);
		}
	}

	public static class Chat extends TLObject {
		public int flags;
		public boolean creator;
		public boolean kicked;
		public boolean admins_enabled;
		public boolean admin;
		public boolean deactivated;
		public int id;
		public String title;
		public ChatPhoto photo;
		public int participants_count;
		public int date;
		public int version;
		public boolean editor;
		public boolean moderator;
		public boolean broadcast;
		public boolean verified;
		public boolean megagroup;
		public boolean explicit_content;
		public boolean left;
		public long access_hash;
		public String username;
		public boolean restricted;
		public boolean democracy;
		public boolean signatures;
		public String restriction_reason;
		public boolean min;
		public InputChannel migrated_to;
		public String address;

		public static Chat TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Chat result = null;
			return result;
		}
	}

	public static class TL_channelForbidden extends Chat {
		public static int constructor = 0x8537784f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			broadcast = (flags & 32) != 0;
			megagroup = (flags & 256) != 0;
			id = stream.readInt32(exception);
			access_hash = stream.readInt64(exception);
			title = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = broadcast ? (flags | 32) : (flags &~ 32);
			flags = megagroup ? (flags | 256) : (flags &~ 256);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			stream.writeInt64(access_hash);
			stream.writeString(title);
		}
	}

	public static class TL_chatForbidden extends Chat {
		public static int constructor = 0x7328bdb;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			title = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
			stream.writeString(title);
		}
	}

	public static class TL_channel extends Chat {
		public static int constructor = 0xa14dca52;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			creator = (flags & 1) != 0;
			kicked = (flags & 2) != 0;
			left = (flags & 4) != 0;
			editor = (flags & 8) != 0;
			moderator = (flags & 16) != 0;
			broadcast = (flags & 32) != 0;
			verified = (flags & 128) != 0;
			megagroup = (flags & 256) != 0;
			restricted = (flags & 512) != 0;
			democracy = (flags & 1024) != 0;
			signatures = (flags & 2048) != 0;
			min = (flags & 4096) != 0;
			id = stream.readInt32(exception);
			if ((flags & 8192) != 0) {
				access_hash = stream.readInt64(exception);
			}
			title = stream.readString(exception);
			if ((flags & 64) != 0) {
				username = stream.readString(exception);
			}
			photo = ChatPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			date = stream.readInt32(exception);
			version = stream.readInt32(exception);
			if ((flags & 512) != 0) {
				restriction_reason = stream.readString(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = creator ? (flags | 1) : (flags &~ 1);
			flags = kicked ? (flags | 2) : (flags &~ 2);
			flags = left ? (flags | 4) : (flags &~ 4);
			flags = editor ? (flags | 8) : (flags &~ 8);
			flags = moderator ? (flags | 16) : (flags &~ 16);
			flags = broadcast ? (flags | 32) : (flags &~ 32);
			flags = verified ? (flags | 128) : (flags &~ 128);
			flags = megagroup ? (flags | 256) : (flags &~ 256);
			flags = restricted ? (flags | 512) : (flags &~ 512);
			flags = democracy ? (flags | 1024) : (flags &~ 1024);
			flags = signatures ? (flags | 2048) : (flags &~ 2048);
			flags = min ? (flags | 4096) : (flags &~ 4096);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			if ((flags & 8192) != 0) {
				stream.writeInt64(access_hash);
			}
			stream.writeString(title);
			if ((flags & 64) != 0) {
				stream.writeString(username);
			}
			photo.serializeToStream(stream);
			stream.writeInt32(date);
			stream.writeInt32(version);
			if ((flags & 512) != 0) {
				stream.writeString(restriction_reason);
			}
		}
	}

	public static class TL_chat extends Chat {
		public static int constructor = 0xd91cdd54;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			creator = (flags & 1) != 0;
			kicked = (flags & 2) != 0;
			left = (flags & 4) != 0;
			admins_enabled = (flags & 8) != 0;
			admin = (flags & 16) != 0;
			deactivated = (flags & 32) != 0;
			id = stream.readInt32(exception);
			title = stream.readString(exception);
			photo = ChatPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			participants_count = stream.readInt32(exception);
			date = stream.readInt32(exception);
			version = stream.readInt32(exception);
			if ((flags & 64) != 0) {
				migrated_to = InputChannel.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = creator ? (flags | 1) : (flags &~ 1);
			flags = kicked ? (flags | 2) : (flags &~ 2);
			flags = left ? (flags | 4) : (flags &~ 4);
			flags = admins_enabled ? (flags | 8) : (flags &~ 8);
			flags = admin ? (flags | 16) : (flags &~ 16);
			flags = deactivated ? (flags | 32) : (flags &~ 32);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			stream.writeString(title);
			photo.serializeToStream(stream);
			stream.writeInt32(participants_count);
			stream.writeInt32(date);
			stream.writeInt32(version);
			if ((flags & 64) != 0) {
				migrated_to.serializeToStream(stream);
			}
		}
	}

	public static class StickerSet extends TLObject {
		public long id;
		public long access_hash;
		public String title;
		public String short_name;
		public int flags;
		public boolean installed;
		public boolean disabled;
		public boolean official;
		public int count;
		public int hash;

		public static StickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			StickerSet result = null;
			return result;
		}
	}

	public static class storage_FileType extends TLObject {

		public static storage_FileType TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			storage_FileType result = null;
			return result;
		}
	}

	public static class auth_CodeType extends TLObject {

		public static auth_CodeType TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			auth_CodeType result = null;
			return result;
		}
	}

	public static class TL_auth_codeTypeSms extends auth_CodeType {
		public static int constructor = 0x72a3158c;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_auth_codeTypeCall extends auth_CodeType {
		public static int constructor = 0x741cd3e3;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_auth_codeTypeFlashCall extends auth_CodeType {
		public static int constructor = 0x226ccefb;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class MessagesFilter extends TLObject {

		public static MessagesFilter TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			MessagesFilter result = null;
			return result;
		}
	}

	public static class TL_inputMessagesFilterDocument extends MessagesFilter {
		public static int constructor = 0x9eddf188;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterMusic extends MessagesFilter {
		public static int constructor = 0x3751b49e;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterChatPhotos extends MessagesFilter {
		public static int constructor = 0x3a20ecb8;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterUrl extends MessagesFilter {
		public static int constructor = 0x7ef0dd87;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterVoice extends MessagesFilter {
		public static int constructor = 0x50f5c392;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterEmpty extends MessagesFilter {
		public static int constructor = 0x57e2f66c;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputMessagesFilterPhotoVideo extends MessagesFilter {
		public static int constructor = 0x56e9f0e4;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_messageFwdHeader extends TLObject {
		public static int constructor = 0xc786ddcb;

		public int flags;
		public int from_id;
		public int date;
		public int channel_id;
		public int channel_post;

		public static TL_messageFwdHeader TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_messageFwdHeader.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_messageFwdHeader", constructor));
				} else {
					return null;
				}
			}
			TL_messageFwdHeader result = new TL_messageFwdHeader();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			if ((flags & 1) != 0) {
				from_id = stream.readInt32(exception);
			}
			date = stream.readInt32(exception);
			if ((flags & 2) != 0) {
				channel_id = stream.readInt32(exception);
			}
			if ((flags & 4) != 0) {
				channel_post = stream.readInt32(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(flags);
			if ((flags & 1) != 0) {
				stream.writeInt32(from_id);
			}
			stream.writeInt32(date);
			if ((flags & 2) != 0) {
				stream.writeInt32(channel_id);
			}
			if ((flags & 4) != 0) {
				stream.writeInt32(channel_post);
			}
		}
	}

	public static class FileLocation extends TLObject {
		public int dc_id;
		public long volume_id;
		public int local_id;
        public long secret;
		public byte[] key;
        public byte[] iv;

		public static FileLocation TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            FileLocation result = null;
			return result;
		}
	}

	public static class TL_fileLocation extends FileLocation {
		public static int constructor = 0x53d69076;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			dc_id = stream.readInt32(exception);
			volume_id = stream.readInt64(exception);
			local_id = stream.readInt32(exception);
			secret = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(dc_id);
			stream.writeInt64(volume_id);
			stream.writeInt32(local_id);
			stream.writeInt64(secret);
		}
	}

	public static class TL_fileEncryptedLocation extends FileLocation {
		public static int constructor = 0x55555554;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			dc_id = stream.readInt32(exception);
			volume_id = stream.readInt64(exception);
            local_id = stream.readInt32(exception);
            secret = stream.readInt64(exception);
			key = stream.readByteArray(exception);
			iv = stream.readByteArray(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(dc_id);
			stream.writeInt64(volume_id);
            stream.writeInt32(local_id);
            stream.writeInt64(secret);
			stream.writeByteArray(key);
			stream.writeByteArray(iv);
		}
	}

	public static class TL_fileLocationUnavailable extends FileLocation {
		public static int constructor = 0x7c596b46;


		public void readParams(AbstractSerializedData stream, boolean exception) {
            volume_id = stream.readInt64(exception);
			local_id = stream.readInt32(exception);
			secret = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(volume_id);
			stream.writeInt32(local_id);
			stream.writeInt64(secret);
		}
	}

	public static class messages_SavedGifs extends TLObject {
		public int hash;
		public ArrayList<Document> gifs = new ArrayList<>();

		public static messages_SavedGifs TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			messages_SavedGifs result = null;
			return result;
		}
	}

	public static class TL_messages_savedGifs extends messages_SavedGifs {
		public static int constructor = 0x2e0709a5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			hash = stream.readInt32(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Document object = Document.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				gifs.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(hash);
			stream.writeInt32(0x1cb5c415);
			int count = gifs.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				gifs.get(a).serializeToStream(stream);
			}
		}
	}

	public static class PhotoSize extends TLObject {
		public String type;
		public FileLocation location;
		public int w;
		public int h;
		public int size;
		public byte[] bytes;

		public static PhotoSize TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			PhotoSize result = null;
			return result;
		}
	}

	public static class TL_photoSize extends PhotoSize {
		public static int constructor = 0x77bfb61b;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			type = stream.readString(exception);
			location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
			size = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(type);
			location.serializeToStream(stream);
			stream.writeInt32(w);
			stream.writeInt32(h);
			stream.writeInt32(size);
		}
	}

	public static class TL_photoSizeEmpty extends PhotoSize {
		public static int constructor = 0xe17e23c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int startReadPosiition = stream.getPosition(); //TODO remove this hack after some time
			try {
				type = stream.readString(true);
				if (type.length() > 1 || !type.equals("") && !type.equals("s") && !type.equals("x") && !type.equals("m") && !type.equals("y") && !type.equals("w")) {
					type = "s";
					if (stream instanceof NativeByteBuffer) {
						((NativeByteBuffer) stream).position(startReadPosiition);
					}
				}
			} catch (Exception e) {
				type = "s";
				if (stream instanceof NativeByteBuffer) {
					((NativeByteBuffer) stream).position(startReadPosiition);
				}
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(type);
		}
	}

	public static class TL_photoCachedSize extends PhotoSize {
		public static int constructor = 0xe9a734fa;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			type = stream.readString(exception);
			location = FileLocation.TLdeserialize(stream, stream.readInt32(exception), exception);
			w = stream.readInt32(exception);
			h = stream.readInt32(exception);
			bytes = stream.readByteArray(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(type);
			location.serializeToStream(stream);
            stream.writeInt32(w);
			stream.writeInt32(h);
            stream.writeByteArray(bytes);
		}
	}

	public static class ExportedChatInvite extends TLObject {
		public String link;

		public static ExportedChatInvite TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			ExportedChatInvite result = null;
			return result;
		}
	}

    public static class TL_chatInviteExported extends ExportedChatInvite {
		public static int constructor = 0xfc2e05bc;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			link = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(link);
		}
	}

	public static class TL_chatInviteEmpty extends ExportedChatInvite {
		public static int constructor = 0x69df3769;


        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class InputFile extends TLObject {
		public long id;
		public int parts;
		public String name;
		public String md5_checksum;

		public static InputFile TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputFile result = null;
			return result;
		}
	}

	public static class TL_inputFileBig extends InputFile {
		public static int constructor = 0xfa4f0bb5;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			parts = stream.readInt32(exception);
			name = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt32(parts);
			stream.writeString(name);
		}
	}

	public static class TL_inputFile extends InputFile {
		public static int constructor = 0xf52ff27f;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			parts = stream.readInt32(exception);
			name = stream.readString(exception);
			md5_checksum = stream.readString(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt32(parts);
			stream.writeString(name);
			stream.writeString(md5_checksum);
		}
	}

	public static class Updates extends TLObject {
		public ArrayList<Update> updates = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();
		public ArrayList<Chat> chats = new ArrayList<>();
		public int date;
		public int seq;
		public int flags;
		public boolean out;
		public boolean mentioned;
		public boolean media_unread;
		public boolean silent;
		public int id;
		public int user_id;
		public String message;
		public int pts;
		public int pts_count;
		public TL_messageFwdHeader fwd_from;
		public int via_bot_id;
		public int reply_to_msg_id;
		public ArrayList<MessageEntity> entities = new ArrayList<>();
		public MessageMedia media;
		public Update update;
		public int from_id;
		public int chat_id;
		public int seq_start;

		public static Updates TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			Updates result = null;
			return result;
		}
	}

	public static class TL_updates extends Updates {
		public static int constructor = 0x74ae4240;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Update object = Update.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				updates.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				chats.add(object);
			}
			date = stream.readInt32(exception);
			seq = stream.readInt32(exception);
		}
	}

	public static class TL_updateShortMessage extends Updates {
		public static int constructor = 0x914fbf11;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
			silent = (flags & 8192) != 0;
			id = stream.readInt32(exception);
			user_id = stream.readInt32(exception);
			message = stream.readString(exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
			date = stream.readInt32(exception);
			if ((flags & 4) != 0) {
				fwd_from = TL_messageFwdHeader.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 2048) != 0) {
				via_bot_id = stream.readInt32(exception);
			}
			if ((flags & 8) != 0) {
				reply_to_msg_id = stream.readInt32(exception);
			}
			if ((flags & 128) != 0) {
				int magic = stream.readInt32(exception);
				if (magic != 0x1cb5c415) {
					if (exception) {
						throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
					}
					return;
				}
				int count = stream.readInt32(exception);
				for (int a = 0; a < count; a++) {
					MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
					if (object == null) {
						return;
					}
					entities.add(object);
				}
			}
		}
	}

	public static class TL_updateShortSentMessage extends Updates {
		public static int constructor = 0x11f1331c;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			out = (flags & 2) != 0;
			id = stream.readInt32(exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
			date = stream.readInt32(exception);
			if ((flags & 512) != 0) {
				media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 128) != 0) {
				int magic = stream.readInt32(exception);
				if (magic != 0x1cb5c415) {
					if (exception) {
						throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
					}
					return;
				}
				int count = stream.readInt32(exception);
				for (int a = 0; a < count; a++) {
					MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
					if (object == null) {
						return;
					}
					entities.add(object);
				}
			}
		}
	}

	public static class TL_updateShort extends Updates {
		public static int constructor = 0x78d4dec1;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			update = Update.TLdeserialize(stream, stream.readInt32(exception), exception);
			date = stream.readInt32(exception);
		}
	}

	public static class TL_updateShortChatMessage extends Updates {
		public static int constructor = 0x16812688;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
			silent = (flags & 8192) != 0;
			id = stream.readInt32(exception);
			from_id = stream.readInt32(exception);
			chat_id = stream.readInt32(exception);
			message = stream.readString(exception);
			pts = stream.readInt32(exception);
			pts_count = stream.readInt32(exception);
			date = stream.readInt32(exception);
			if ((flags & 4) != 0) {
				fwd_from = TL_messageFwdHeader.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 2048) != 0) {
				via_bot_id = stream.readInt32(exception);
			}
			if ((flags & 8) != 0) {
				reply_to_msg_id = stream.readInt32(exception);
			}
			if ((flags & 128) != 0) {
				int magic = stream.readInt32(exception);
				if (magic != 0x1cb5c415) {
					if (exception) {
						throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
					}
					return;
				}
				int count = stream.readInt32(exception);
				for (int a = 0; a < count; a++) {
					MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
					if (object == null) {
						return;
					}
					entities.add(object);
				}
			}
		}
	}

	public static class TL_updatesCombined extends Updates {
		public static int constructor = 0x725b04c3;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Update object = Update.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				updates.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				chats.add(object);
			}
			date = stream.readInt32(exception);
			seq_start = stream.readInt32(exception);
			seq = stream.readInt32(exception);
		}
	}

	public static class TL_updatesTooLong extends Updates {
		public static int constructor = 0xe317af7e;
	}

	public static class WallPaper extends TLObject {
		public int id;
		public String title;
		public ArrayList<PhotoSize> sizes = new ArrayList<>();
		public int color;
		public int bg_color;

		public static WallPaper TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			WallPaper result = null;
			return result;
		}
	}

	public static class TL_wallPaper extends WallPaper {
		public static int constructor = 0xccb03657;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			title = stream.readString(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				PhotoSize object = PhotoSize.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				sizes.add(object);
			}
			color = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
			stream.writeString(title);
			stream.writeInt32(0x1cb5c415);
			int count = sizes.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				sizes.get(a).serializeToStream(stream);
			}
			stream.writeInt32(color);
		}
	}

	public static class TL_wallPaperSolid extends WallPaper {
		public static int constructor = 0x63117f24;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			title = stream.readString(exception);
			bg_color = stream.readInt32(exception);
			color = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(id);
			stream.writeString(title);
			stream.writeInt32(bg_color);
			stream.writeInt32(color);
		}
	}

	public static class TL_stickerPack extends TLObject {
		public static int constructor = 0x12b299d4;

		public String emoticon;
		public ArrayList<Long> documents = new ArrayList<>();

		public static TL_stickerPack TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_stickerPack.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_stickerPack", constructor));
				} else {
					return null;
				}
			}
			TL_stickerPack result = new TL_stickerPack();
			result.readParams(stream, exception);
			return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
			emoticon = stream.readString(exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				documents.add(stream.readInt64(exception));
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(emoticon);
			stream.writeInt32(0x1cb5c415);
			int count = documents.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				stream.writeInt64(documents.get(a));
			}
		}
	}

	public static class InputChatPhoto extends TLObject {
		public InputPhoto id;
		public InputPhotoCrop crop;
		public InputFile file;

		public static InputChatPhoto TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputChatPhoto result = null;
			return result;
		}
	}

	public static class TL_inputChatPhoto extends InputChatPhoto {
		public static int constructor = 0xb2e1bf08;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = InputPhoto.TLdeserialize(stream, stream.readInt32(exception), exception);
			crop = InputPhotoCrop.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
			crop.serializeToStream(stream);
		}
	}

	public static class TL_inputChatPhotoEmpty extends InputChatPhoto {
		public static int constructor = 0x1ca48f57;


        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputChatUploadedPhoto extends InputChatPhoto {
		public static int constructor = 0x94254732;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			file = InputFile.TLdeserialize(stream, stream.readInt32(exception), exception);
			crop = InputPhotoCrop.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			file.serializeToStream(stream);
			crop.serializeToStream(stream);
		}
	}

	public static class InputPhoto extends TLObject {
		public long id;
		public long access_hash;

		public static InputPhoto TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputPhoto result = null;
			return result;
		}
	}

	public static class TL_inputPhotoEmpty extends InputPhoto {
		public static int constructor = 0x1cd7bf0d;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_inputPhoto extends InputPhoto {
		public static int constructor = 0xfb95c6c4;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt64(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt64(id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_importedContact extends TLObject {
		public static int constructor = 0xd0028438;

		public int user_id;
		public long client_id;

		public static TL_importedContact TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_importedContact.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_importedContact", constructor));
				} else {
					return null;
				}
			}
			TL_importedContact result = new TL_importedContact();
			result.readParams(stream, exception);
			return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			client_id = stream.readInt64(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt64(client_id);
		}
	}

	public static class TL_accountDaysTTL extends TLObject {
		public static int constructor = 0xb8d0afdf;

		public int days;

		public static TL_accountDaysTTL TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_accountDaysTTL.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_accountDaysTTL", constructor));
				} else {
					return null;
				}
			}
			TL_accountDaysTTL result = new TL_accountDaysTTL();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			days = stream.readInt32(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(days);
		}
	}

	public static class messages_Stickers extends TLObject {
		public String hash;
		public ArrayList<Document> stickers = new ArrayList<>();

		public static messages_Stickers TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			messages_Stickers result = null;
			return result;
		}
	}

	public static class InputPeer extends TLObject {
		public int user_id;
		public long access_hash;
		public int chat_id;
		public int channel_id;

		public static InputPeer TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputPeer result = null;
			return result;
		}
	}

	public static class TL_inputPeerUser extends InputPeer {
		public static int constructor = 0x7b8e7de6;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			user_id = stream.readInt32(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(user_id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_inputPeerChat extends InputPeer {
		public static int constructor = 0x179be863;


        public void readParams(AbstractSerializedData stream, boolean exception) {
			chat_id = stream.readInt32(exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(chat_id);
		}
	}

	public static class TL_inputPeerEmpty extends InputPeer {
		public static int constructor = 0x7f3b18ea;


		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
		}
	}

	public static class TL_inputPeerChannel extends InputPeer {
		public static int constructor = 0x20adaef8;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			channel_id = stream.readInt32(exception);
			access_hash = stream.readInt64(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(channel_id);
			stream.writeInt64(access_hash);
		}
	}

	public static class TL_dcOption extends TLObject {
		public static int constructor = 0x5d8c6cc;

		public int flags;
		public boolean ipv6;
		public boolean media_only;
		public boolean tcpo_only;
		public int id;
		public String ip_address;
		public int port;

		public static TL_dcOption TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_dcOption.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_dcOption", constructor));
				} else {
					return null;
				}
			}
			TL_dcOption result = new TL_dcOption();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			ipv6 = (flags & 1) != 0;
			media_only = (flags & 2) != 0;
			tcpo_only = (flags & 4) != 0;
			id = stream.readInt32(exception);
			ip_address = stream.readString(exception);
			port = stream.readInt32(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = ipv6 ? (flags | 1) : (flags &~ 1);
			flags = media_only ? (flags | 2) : (flags &~ 2);
			flags = tcpo_only ? (flags | 4) : (flags &~ 4);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			stream.writeString(ip_address);
			stream.writeInt32(port);
		}
	}

	public static class TL_decryptedMessageLayer extends TLObject {
		public static int constructor = 0x1be31789;

		public byte[] random_bytes;
		public int layer;
		public int in_seq_no;
		public int out_seq_no;
		public DecryptedMessage message;

		public static TL_decryptedMessageLayer TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_decryptedMessageLayer.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_decryptedMessageLayer", constructor));
				} else {
					return null;
				}
			}
			TL_decryptedMessageLayer result = new TL_decryptedMessageLayer();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			random_bytes = stream.readByteArray(exception);
			layer = stream.readInt32(exception);
            in_seq_no = stream.readInt32(exception);
            out_seq_no = stream.readInt32(exception);
			message = DecryptedMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeByteArray(random_bytes);
            stream.writeInt32(layer);
			stream.writeInt32(in_seq_no);
			stream.writeInt32(out_seq_no);
			message.serializeToStream(stream);
		}
	}

	public static class TL_topPeer extends TLObject {
		public static int constructor = 0xedcdc05b;

		public Peer peer;
		public double rating;

		public static TL_topPeer TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_topPeer.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_topPeer", constructor));
				} else {
					return null;
				}
			}
			TL_topPeer result = new TL_topPeer();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			rating = stream.readDouble(exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			peer.serializeToStream(stream);
			stream.writeDouble(rating);
		}
	}

	public static class InputPhotoCrop extends TLObject {
		public double crop_left;
		public double crop_top;
		public double crop_width;

		public static InputPhotoCrop TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			InputPhotoCrop result = null;
			return result;
		}
	}

	public static class TL_inputPhotoCropAuto extends InputPhotoCrop {
		public static int constructor = 0xade6b004;


		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class messages_Dialogs extends TLObject {
		public ArrayList<TL_dialog> dialogs = new ArrayList<>();
		public ArrayList<Message> messages = new ArrayList<>();
		public ArrayList<Chat> chats = new ArrayList<>();
		public ArrayList<User> users = new ArrayList<>();
		public int count;

		public static messages_Dialogs TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			messages_Dialogs result = null;
			return result;
		}
	}

	public static class TL_messages_dialogs extends messages_Dialogs {
		public static int constructor = 0x15ba6c40;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				TL_dialog object = TL_dialog.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				dialogs.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Message object = Message.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				messages.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				Chat object = Chat.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				chats.add(object);
			}
			magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				User object = User.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				users.add(object);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = dialogs.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				dialogs.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = messages.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				messages.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = chats.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				chats.get(a).serializeToStream(stream);
			}
			stream.writeInt32(0x1cb5c415);
			count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_auth_sendCode extends TLObject {
		public static int constructor = 0x86aef0ec;

		public int flags;
		public boolean allow_flashcall;
		public String phone_number;
		public boolean current_number;
		public int api_id;
		public String api_hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_sentCode.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = allow_flashcall ? (flags | 1) : (flags &~ 1);
			stream.writeInt32(flags);
			stream.writeString(phone_number);
			if ((flags & 1) != 0) {
				stream.writeBool(current_number);
			}
			stream.writeInt32(api_id);
			stream.writeString(api_hash);
		}
	}

	public static class TL_auth_signUp extends TLObject {
		public static int constructor = 0x1b067634;

		public String phone_number;
		public String phone_code_hash;
		public String phone_code;
		public String first_name;
		public String last_name;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_authorization.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(phone_code_hash);
			stream.writeString(phone_code);
			stream.writeString(first_name);
			stream.writeString(last_name);
		}
	}

	public static class TL_auth_signIn extends TLObject {
		public static int constructor = 0xbcd51581;

		public String phone_number;
		public String phone_code_hash;
		public String phone_code;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_authorization.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(phone_code_hash);
			stream.writeString(phone_code);
		}
	}

	public static class TL_account_resetNotifySettings extends TLObject {
		public static int constructor = 0xdb7e1747;


		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_account_updateStatus extends TLObject {
		public static int constructor = 0x6628562c;

		public boolean offline;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeBool(offline);
		}
	}

	public static class TL_contacts_getStatuses extends TLObject {
		public static int constructor = 0xc4a353ee;


		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			Vector vector = new Vector();
			int size = stream.readInt32(exception);
			for (int a = 0; a < size; a++) {
				TL_contactStatus object = TL_contactStatus.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return vector;
				}
				vector.objects.add(object);
            }
            return vector;
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_contacts_getContacts extends TLObject {
		public static int constructor = 0x22c6aa08;

		public String hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return contacts_Contacts.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(hash);
		}
	}

	public static class TL_contacts_importContacts extends TLObject {
		public static int constructor = 0xda30b32d;

		public ArrayList<TL_inputPhoneContact> contacts = new ArrayList<>();
		public boolean replace;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_contacts_importedContacts.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(0x1cb5c415);
			int count = contacts.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				contacts.get(a).serializeToStream(stream);
			}
			stream.writeBool(replace);
		}
	}

	public static class TL_contacts_deleteContacts extends TLObject {
		public static int constructor = 0x59ab389e;

		public ArrayList<InputUser> id = new ArrayList<>();

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
			int count = id.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				id.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_contacts_block extends TLObject {
		public static int constructor = 0x332b49fc;

		public InputUser id;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
		}
	}

	public static class TL_contacts_unblock extends TLObject {
		public static int constructor = 0xe54100bd;

		public InputUser id;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
		}
	}

	public static class TL_contacts_getBlocked extends TLObject {
        public static int constructor = 0xf57c350f;

		public int offset;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return contacts_Blocked.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			stream.writeInt32(offset);
			stream.writeInt32(limit);
		}
	}

	public static class TL_messages_getDialogs extends TLObject {
		public static int constructor = 0x6b47f94d;

		public int offset_date;
		public int offset_id;
		public InputPeer offset_peer;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_Dialogs.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(offset_date);
			stream.writeInt32(offset_id);
			offset_peer.serializeToStream(stream);
			stream.writeInt32(limit);
		}
	}

	public static class TL_messages_getHistory extends TLObject {
		public static int constructor = 0xafa92846;

		public InputPeer peer;
		public int offset_id;
		public int offset_date;
		public int add_offset;
		public int limit;
		public int max_id;
		public int min_id;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_Messages.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			peer.serializeToStream(stream);
			stream.writeInt32(offset_id);
			stream.writeInt32(offset_date);
			stream.writeInt32(add_offset);
			stream.writeInt32(limit);
			stream.writeInt32(max_id);
			stream.writeInt32(min_id);
		}
	}

	public static class TL_messages_search extends TLObject {
		public static int constructor = 0xd4569248;

		public int flags;
		public InputPeer peer;
		public String q;
		public MessagesFilter filter;
		public int min_date;
		public int max_date;
		public int offset;
		public int max_id;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_Messages.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(flags);
			peer.serializeToStream(stream);
			stream.writeString(q);
			filter.serializeToStream(stream);
			stream.writeInt32(min_date);
			stream.writeInt32(max_date);
			stream.writeInt32(offset);
			stream.writeInt32(max_id);
			stream.writeInt32(limit);
		}
	}

	public static class TL_messages_searchGlobal extends TLObject {
		public static int constructor = 0x9e3cacb0;

		public String q;
		public int offset_date;
		public InputPeer offset_peer;
		public int offset_id;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_Messages.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(q);
			stream.writeInt32(offset_date);
			offset_peer.serializeToStream(stream);
			stream.writeInt32(offset_id);
			stream.writeInt32(limit);
		}
	}

    public static class TL_messages_editChatPhoto extends TLObject {
        public static int constructor = 0xca4c79d8;

        public int chat_id;
        public InputChatPhoto photo;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return Updates.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(chat_id);
            photo.serializeToStream(stream);
        }
    }

	public static class TL_messages_createChat extends TLObject {
		public static int constructor = 0x9cb126e;

		public ArrayList<InputUser> users = new ArrayList<>();
		public String title;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Updates.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
			int count = users.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				users.get(a).serializeToStream(stream);
			}
			stream.writeString(title);
		}
	}

    public static class TL_photos_updateProfilePhoto extends TLObject {
		public static int constructor = 0xeef579a0;

		public InputPhoto id;
		public InputPhotoCrop crop;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return UserProfilePhoto.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
			crop.serializeToStream(stream);
		}
	}

	public static class TL_photos_uploadProfilePhoto extends TLObject {
        public static int constructor = 0xd50f9c88;

		public InputFile file;
        public String caption;
		public InputGeoPoint geo_point;
		public InputPhotoCrop crop;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_photos_photo.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			file.serializeToStream(stream);
			stream.writeString(caption);
			geo_point.serializeToStream(stream);
			crop.serializeToStream(stream);
		}
	}

	public static class TL_photos_deletePhotos extends TLObject {
		public static int constructor = 0x87cf7f2f;

		public ArrayList<InputPhoto> id = new ArrayList<>();

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			Vector vector = new Vector();
			int size = stream.readInt32(exception);
			for (int a = 0; a < size; a++) {
				vector.objects.add(stream.readInt64(exception));
			}
            return vector;
		}

		public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(0x1cb5c415);
			int count = id.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
                id.get(a).serializeToStream(stream);
			}
		}
	}

	public static class TL_upload_getFile extends TLObject {
		public static int constructor = 0xe3a6cfb5;

		public InputFileLocation location;
		public int offset;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_upload_file.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			location.serializeToStream(stream);
			stream.writeInt32(offset);
			stream.writeInt32(limit);
        }
	}

	public static class TL_photos_getUserPhotos extends TLObject {
		public static int constructor = 0x91cd32a8;

		public InputUser user_id;
		public int offset;
		public long max_id;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return photos_Photos.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            user_id.serializeToStream(stream);
			stream.writeInt32(offset);
			stream.writeInt64(max_id);
			stream.writeInt32(limit);
		}
	}

	public static class TL_contacts_search extends TLObject {
		public static int constructor = 0x11f812d8;

		public String q;
		public int limit;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_contacts_found.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(q);
			stream.writeInt32(limit);
		}
	}

	public static class TL_account_getPrivacy extends TLObject {
        public static int constructor = 0xdadbc950;

		public InputPrivacyKey key;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_account_privacyRules.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			key.serializeToStream(stream);
		}
	}

    public static class TL_account_deleteAccount extends TLObject {
		public static int constructor = 0x418d4e0b;

		public String reason;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(reason);
		}
	}

	public static class TL_account_getAccountTTL extends TLObject {
		public static int constructor = 0x8fc711d;


		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_accountDaysTTL.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

    public static class TL_contacts_resolveUsername extends TLObject {
        public static int constructor = 0xf93ccba3;

        public String username;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return TL_contacts_resolvedPeer.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeString(username);
        }
    }

	public static class TL_contacts_getTopPeers extends TLObject {
		public static int constructor = 0xd4982db5;

		public int flags;
		public boolean correspondents;
		public boolean bots_pm;
		public boolean bots_inline;
		public boolean groups;
		public boolean channels;
		public int offset;
		public int limit;
		public int hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return contacts_TopPeers.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = correspondents ? (flags | 1) : (flags &~ 1);
			flags = bots_pm ? (flags | 2) : (flags &~ 2);
			flags = bots_inline ? (flags | 4) : (flags &~ 4);
			flags = groups ? (flags | 1024) : (flags &~ 1024);
			flags = channels ? (flags | 32768) : (flags &~ 32768);
			stream.writeInt32(flags);
			stream.writeInt32(offset);
			stream.writeInt32(limit);
			stream.writeInt32(hash);
		}
	}

	public static class TL_contacts_resetTopPeerRating extends TLObject {
		public static int constructor = 0x1ae373ac;

		public TopPeerCategory category;
		public InputPeer peer;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			category.serializeToStream(stream);
			peer.serializeToStream(stream);
		}
	}

	public static class TL_messages_getAllStickers extends TLObject {
		public static int constructor = 0x1c9618b1;

		public int hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_AllStickers.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(hash);
		}
	}

	public static class TL_messages_getWebPagePreview extends TLObject {
		public static int constructor = 0x25223e24;

		public String message;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return MessageMedia.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(message);
		}
	}

 	public static class TL_account_getPassword extends TLObject {
        public static int constructor = 0x548a30f5;


		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return account_Password.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
		}
	}

	public static class TL_auth_checkPassword extends TLObject {
		public static int constructor = 0xa63011e;

		public byte[] password_hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_authorization.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
            stream.writeByteArray(password_hash);
		}
	}

	public static class TL_auth_requestPasswordRecovery extends TLObject {
		public static int constructor = 0xd897bc66;


		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_passwordRecovery.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
        }
    }

    public static class TL_auth_recoverPassword extends TLObject {
		public static int constructor = 0x4ea56e92;

		public String code;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_authorization.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(code);
		}
	}

	public static class TL_auth_resendCode extends TLObject {
		public static int constructor = 0x3ef1a9bf;

		public String phone_number;
		public String phone_code_hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_auth_sentCode.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(phone_code_hash);
		}
	}

	public static class TL_auth_cancelCode extends TLObject {
		public static int constructor = 0x1f040578;

		public String phone_number;
		public String phone_code_hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(phone_number);
			stream.writeString(phone_code_hash);
		}
	}

	public static class TL_messages_checkChatInvite extends TLObject {
		public static int constructor = 0x3eadb1bb;

		public String hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return ChatInvite.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(hash);
		}
	}

	public static class TL_messages_importChatInvite extends TLObject {
		public static int constructor = 0x6c50051c;

		public String hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Updates.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeString(hash);
		}
	}

	public static class TL_messages_getStickerSet extends TLObject {
		public static int constructor = 0x2619a90e;

		public InputStickerSet stickerset;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return TL_messages_stickerSet.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stickerset.serializeToStream(stream);
		}
	}

	public static class TL_messages_installStickerSet extends TLObject {
		public static int constructor = 0x7b30c3a6;

		public InputStickerSet stickerset;
		public boolean disabled;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stickerset.serializeToStream(stream);
			stream.writeBool(disabled);
		}
	}

	public static class TL_messages_uninstallStickerSet extends TLObject {
		public static int constructor = 0xf96e55de;

		public InputStickerSet stickerset;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

        public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stickerset.serializeToStream(stream);
		}
	}

    public static class TL_messages_getMessagesViews extends TLObject {
        public static int constructor = 0xc4c8a55d;

        public InputPeer peer;
        public ArrayList<Integer> id = new ArrayList<>();
        public boolean increment;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            Vector vector = new Vector();
            int size = stream.readInt32(exception);
            for (int a = 0; a < size; a++) {
                vector.objects.add(stream.readInt32(exception));
            }
            return vector;
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            peer.serializeToStream(stream);
            stream.writeInt32(0x1cb5c415);
			int count = id.size();
            stream.writeInt32(count);
            for (int a = 0; a < count; a++) {
                stream.writeInt32(id.get(a));
            }
            stream.writeBool(increment);
        }
    }

	public static class TL_messages_getSavedGifs extends TLObject {
		public static int constructor = 0x83bf3d52;

		public int hash;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return messages_SavedGifs.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(hash);
		}
	}

	public static class TL_messages_saveGif extends TLObject {
		public static int constructor = 0x327a30cb;

		public InputDocument id;
		public boolean unsave;

		public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
			return Bool.TLdeserialize(stream, constructor, exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			id.serializeToStream(stream);
			stream.writeBool(unsave);
		}
	}

    //manually created

	//MessageMedia start
	public static class MessageMedia extends TLObject {
		public byte[] bytes;
		public Audio audio_unused;
		public Photo photo;
		public GeoPoint geo;
		public String title;
		public String address;
		public String provider;
		public String venue_id;
		public Video video_unused;
		public Document document;
		public String caption;
		public String phone_number;
		public String first_name;
		public String last_name;
		public int user_id;
		public WebPage webpage;

		public static MessageMedia TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			MessageMedia result = null;
			return result;
		}
	}
	//MessageMedia end

    //EncryptedChat start
    public static class EncryptedChat extends TLObject {
        public int id;
        public long access_hash;
        public int date;
        public int admin_id;
        public int participant_id;
        public byte[] g_a;
        public byte[] nonce;
        public byte[] g_a_or_b;
        public long key_fingerprint;
        public byte[] auth_key; //custom
        public int user_id; //custom
        public int ttl; //custom
        public int layer; //custom
        public byte[] key_hash; //custom

        public static EncryptedChat TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			EncryptedChat result = null;
            return result;
        }
    }
    //EncryptedChat end

    //Message start
    public static class Message extends TLObject {
        public int id;
        public int from_id;
        public Peer to_id;
        public int date;
        public MessageAction action;
        public int reply_to_msg_id;
		public long reply_to_random_id;
        public String message;
        public MessageMedia media;
        public int flags;
		public boolean mentioned;
		public boolean media_unread;
		public boolean out;
		public boolean unread;
        public ArrayList<MessageEntity> entities = new ArrayList<>();
		public String via_bot_name;
		public int views;
		public int edit_date;
		public boolean silent;
		public boolean post;
		public TL_messageFwdHeader fwd_from;
		public int via_bot_id;
        public int send_state = 0; //custom
        public int fwd_msg_id = 0; //custom
        public String attachPath = ""; //custom
		public HashMap<String, String> params; //custom
        public long random_id; //custom
        //public int local_id = 0; //custom
        public long dialog_id; //custom
        public int ttl; //custom
        public int destroyTime; //custom
        public int layer; //custom
        //public int seq_in; //custom
        //public int seq_out; //custom
        public TLRPC.Message replyMessage; //custom

        public static Message TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            Message result = null;
            return result;
        }
    }

	public static class TL_messageEmpty extends Message {
		public static int constructor = 0x83e5de54;


		public void readParams(AbstractSerializedData stream, boolean exception) {
			id = stream.readInt32(exception);
			to_id = new TL_peerUser();
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(id);
		}
	}

	public static class TL_message extends Message {
		public static int constructor = 0xc09be45f;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			unread = (flags & 1) != 0;
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
			silent = (flags & 8192) != 0;
			post = (flags & 16384) != 0;
			id = stream.readInt32(exception);
			if ((flags & 256) != 0) {
				from_id = stream.readInt32(exception);
			}
			to_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			if (from_id == 0) {
				if (to_id.user_id != 0) {
					from_id = to_id.user_id;
				} else {
					from_id = -to_id.channel_id;
				}
			}
			if ((flags & 4) != 0) {
				fwd_from = TL_messageFwdHeader.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
			if ((flags & 2048) != 0) {
				via_bot_id = stream.readInt32(exception);
			}
			if ((flags & 8) != 0) {
				reply_to_msg_id = stream.readInt32(exception);
			}
			date = stream.readInt32(exception);
			message = stream.readString(exception);
			if ((flags & 512) != 0) {
				media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
			} else {
				media = new TL_messageMediaEmpty();
			}
			if ((flags & 128) != 0) {
				int magic = stream.readInt32(exception);
				if (magic != 0x1cb5c415) {
					if (exception) {
						throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
					}
					return;
				}
				int count = stream.readInt32(exception);
				for (int a = 0; a < count; a++) {
					MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
					if (object == null) {
						return;
					}
					entities.add(object);
				}
			}
			if ((flags & 1024) != 0) {
				views = stream.readInt32(exception);
			}
			if ((flags & 32768) != 0) {
				edit_date = stream.readInt32(exception);
			}
			if (id < 0 || (media != null && !(media instanceof TL_messageMediaEmpty) && !(media instanceof TL_messageMediaWebPage) && message != null && message.length() != 0 && message.startsWith("-1"))) {
				attachPath = stream.readString(exception);
				if (id < 0 && attachPath.startsWith("||")) {
					String args[] = attachPath.split("\\|\\|");
					if (args.length > 0) {
						params = new HashMap<>();
						for (int a = 1; a < args.length - 1; a++) {
							String args2[] = args[a].split("\\|=\\|");
							if (args2.length == 2) {
								params.put(args2[0], args2[1]);
							}
						}
						attachPath = args[args.length - 1];
					}
				}
			}
			if ((flags & MESSAGE_FLAG_FWD) != 0 && id < 0) {
				fwd_msg_id = stream.readInt32(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = unread ? (flags | 1) : (flags &~ 1);
			flags = out ? (flags | 2) : (flags &~ 2);
			flags = mentioned ? (flags | 16) : (flags &~ 16);
			flags = media_unread ? (flags | 32) : (flags &~ 32);
			flags = silent ? (flags | 8192) : (flags &~ 8192);
			flags = post ? (flags | 16384) : (flags &~ 16384);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			if ((flags & 256) != 0) {
				stream.writeInt32(from_id);
			}
			to_id.serializeToStream(stream);
			if ((flags & 4) != 0) {
				fwd_from.serializeToStream(stream);
			}
			if ((flags & 2048) != 0) {
				stream.writeInt32(via_bot_id);
			}
			if ((flags & 8) != 0) {
				stream.writeInt32(reply_to_msg_id);
			}
			stream.writeInt32(date);
			stream.writeString(message);
			if ((flags & 512) != 0) {
				media.serializeToStream(stream);
			}
			if ((flags & 128) != 0) {
				stream.writeInt32(0x1cb5c415);
				int count = entities.size();
				stream.writeInt32(count);
				for (int a = 0; a < count; a++) {
					entities.get(a).serializeToStream(stream);
				}
			}
			if ((flags & 1024) != 0) {
				stream.writeInt32(views);
			}
			if ((flags & 32768) != 0) {
				stream.writeInt32(edit_date);
			}
			String path = attachPath;
			if (id < 0 && params != null && params.size() > 0) {
				for (HashMap.Entry<String, String> entry : params.entrySet()) {
					path = entry.getKey() + "|=|" + entry.getValue() + "||" + path;
				}
				path = "||" + path;
			}
			stream.writeString(path);
			if ((flags & MESSAGE_FLAG_FWD) != 0 && id < 0) {
				stream.writeInt32(fwd_msg_id);
			}
		}
	}

	public static class TL_message_secret extends TL_message {
		public static int constructor = 0x555555f9;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			unread = (flags & 1) != 0;
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
			id = stream.readInt32(exception);
			ttl = stream.readInt32(exception);
			from_id = stream.readInt32(exception);
			to_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			date = stream.readInt32(exception);
			message = stream.readString(exception);
			media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
			int magic = stream.readInt32(exception);
			if (magic != 0x1cb5c415) {
				if (exception) {
					throw new RuntimeException(String.format("wrong Vector magic, got %x", magic));
				}
				return;
			}
			int count = stream.readInt32(exception);
			for (int a = 0; a < count; a++) {
				MessageEntity object = MessageEntity.TLdeserialize(stream, stream.readInt32(exception), exception);
				if (object == null) {
					return;
				}
				entities.add(object);
			}
			if ((flags & 2048) != 0) {
				via_bot_name = stream.readString(exception);
			}
			if ((flags & 8) != 0) {
				reply_to_random_id = stream.readInt64(exception);
			}
			if (id < 0 || (media != null && !(media instanceof TL_messageMediaEmpty) && !(media instanceof TL_messageMediaWebPage) && message != null && message.length() != 0 && message.startsWith("-1"))) {
				attachPath = stream.readString(exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = unread ? (flags | 1) : (flags &~ 1);
			flags = out ? (flags | 2) : (flags &~ 2);
			flags = mentioned ? (flags | 16) : (flags &~ 16);
			flags = media_unread ? (flags | 32) : (flags &~ 32);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			stream.writeInt32(ttl);
			stream.writeInt32(from_id);
			to_id.serializeToStream(stream);
			stream.writeInt32(date);
			stream.writeString(message);
			media.serializeToStream(stream);
			stream.writeInt32(0x1cb5c415);
			int count = entities.size();
			stream.writeInt32(count);
			for (int a = 0; a < count; a++) {
				entities.get(a).serializeToStream(stream);
			}
			if ((flags & 2048) != 0) {
				stream.writeString(via_bot_name);
			}
			if ((flags & 8) != 0) {
				stream.writeInt64(reply_to_random_id);
			}
			stream.writeString(attachPath);
		}
	}

    public static class TL_message_secret_old extends TL_message_secret {
        public static int constructor = 0x555555F8;

        public void readParams(AbstractSerializedData stream, boolean exception) {
            flags = stream.readInt32(exception) | MESSAGE_FLAG_HAS_FROM_ID | MESSAGE_FLAG_HAS_MEDIA;
			unread = (flags & 1) != 0;
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
            id = stream.readInt32(exception);
            ttl = stream.readInt32(exception);
            from_id = stream.readInt32(exception);
            to_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
            date = stream.readInt32(exception);
            message = stream.readString(exception);
            media = MessageMedia.TLdeserialize(stream, stream.readInt32(exception), exception);
            if (id < 0 || (media != null && !(media instanceof TL_messageMediaEmpty) && !(media instanceof TL_messageMediaWebPage) && message != null && message.length() != 0 && message.startsWith("-1"))) {
                attachPath = stream.readString(exception);
            }
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
			flags = unread ? (flags | 1) : (flags &~ 1);
			flags = out ? (flags | 2) : (flags &~ 2);
			flags = mentioned ? (flags | 16) : (flags &~ 16);
			flags = media_unread ? (flags | 32) : (flags &~ 32);
            stream.writeInt32(flags);
            stream.writeInt32(id);
            stream.writeInt32(ttl);
            stream.writeInt32(from_id);
            to_id.serializeToStream(stream);
            stream.writeInt32(date);
            stream.writeString(message);
            media.serializeToStream(stream);
            stream.writeString(attachPath);
        }
    }

	public static class TL_messageService extends Message {
		public static int constructor = 0x9e19a1f6;

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			unread = (flags & 1) != 0;
			out = (flags & 2) != 0;
			mentioned = (flags & 16) != 0;
			media_unread = (flags & 32) != 0;
			silent = (flags & 8192) != 0;
			post = (flags & 16384) != 0;
			id = stream.readInt32(exception);
			if ((flags & 256) != 0) {
				from_id = stream.readInt32(exception);
			}
			to_id = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			if ((flags & 8) != 0) {
				reply_to_msg_id = stream.readInt32(exception);
			}
			date = stream.readInt32(exception);
			action = MessageAction.TLdeserialize(stream, stream.readInt32(exception), exception);
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			flags = unread ? (flags | 1) : (flags &~ 1);
			flags = out ? (flags | 2) : (flags &~ 2);
			flags = mentioned ? (flags | 16) : (flags &~ 16);
			flags = media_unread ? (flags | 32) : (flags &~ 32);
			flags = silent ? (flags | 8192) : (flags &~ 8192);
			flags = post ? (flags | 16384) : (flags &~ 16384);
			stream.writeInt32(flags);
			stream.writeInt32(id);
			if ((flags & 256) != 0) {
				stream.writeInt32(from_id);
			}
			to_id.serializeToStream(stream);
			if ((flags & 8) != 0) {
				stream.writeInt32(reply_to_msg_id);
			}
			stream.writeInt32(date);
			action.serializeToStream(stream);
		}
	}
    //Message end

    //TL_dialog start
	public static class TL_dialog extends TLObject {
		public static int constructor = 0x66ffba14;

		public int flags;
		public Peer peer;
		public int top_message;
		public int read_inbox_max_id;
		public int read_outbox_max_id;
		public int unread_count;
		public PeerNotifySettings notify_settings;
		public int pts;
		public DraftMessage draft;
		public int last_message_date; //custom
		public long id; //custom

		public static TL_dialog TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
			if (TL_dialog.constructor != constructor) {
				if (exception) {
					throw new RuntimeException(String.format("can't parse magic %x in TL_dialog", constructor));
				} else {
					return null;
				}
			}
			TL_dialog result = new TL_dialog();
			result.readParams(stream, exception);
			return result;
		}

		public void readParams(AbstractSerializedData stream, boolean exception) {
			flags = stream.readInt32(exception);
			peer = Peer.TLdeserialize(stream, stream.readInt32(exception), exception);
			top_message = stream.readInt32(exception);
			read_inbox_max_id = stream.readInt32(exception);
			read_outbox_max_id = stream.readInt32(exception);
			unread_count = stream.readInt32(exception);
			notify_settings = PeerNotifySettings.TLdeserialize(stream, stream.readInt32(exception), exception);
			if ((flags & 1) != 0) {
				pts = stream.readInt32(exception);
			}
			if ((flags & 2) != 0) {
				draft = DraftMessage.TLdeserialize(stream, stream.readInt32(exception), exception);
			}
		}

		public void serializeToStream(AbstractSerializedData stream) {
			stream.writeInt32(constructor);
			stream.writeInt32(flags);
			peer.serializeToStream(stream);
			stream.writeInt32(top_message);
			stream.writeInt32(read_inbox_max_id);
			stream.writeInt32(read_outbox_max_id);
			stream.writeInt32(unread_count);
			notify_settings.serializeToStream(stream);
			if ((flags & 1) != 0) {
				stream.writeInt32(pts);
			}
			if ((flags & 2) != 0) {
				draft.serializeToStream(stream);
			}
		}
	}
    //TL_dialog end

    //Chat start
    public static class TL_chatEmpty extends Chat {
        public static int constructor = 0x9ba2d800;


        public void readParams(AbstractSerializedData stream, boolean exception) {
            id = stream.readInt32(exception);

            title = "DELETED";
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt32(id);
        }
    }
    //Chat end

    //functions memory optimize
    public static class TL_upload_saveFilePart extends TLObject {
        public static int constructor = 0xb304a621;

        public long file_id;
        public int file_part;
        public NativeByteBuffer bytes;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return Bool.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(file_id);
            stream.writeInt32(file_part);
            stream.writeByteBuffer(bytes);
        }

        @Override
        public void freeResources() {
            if (disableFree) {
                return;
            }
            if (bytes != null) {
				bytes.reuse();
                bytes = null;
            }
        }
    }

    public static class TL_upload_saveBigFilePart extends TLObject {
        public static int constructor = 0xde7b673d;

        public long file_id;
        public int file_part;
        public int file_total_parts;
        public NativeByteBuffer bytes;

        public TLObject deserializeResponse(AbstractSerializedData stream, int constructor, boolean exception) {
            return Bool.TLdeserialize(stream, constructor, exception);
        }

        public void serializeToStream(AbstractSerializedData stream) {
            stream.writeInt32(constructor);
            stream.writeInt64(file_id);
            stream.writeInt32(file_part);
            stream.writeInt32(file_total_parts);
            stream.writeByteBuffer(bytes);
        }

        @Override
        public void freeResources() {
            if (disableFree) {
                return;
            }
            if (bytes != null) {
                bytes.reuse();
                bytes = null;
            }
        }
    }

    public static class TL_upload_file extends TLObject {
        public static int constructor = 0x96a18d5;

        public storage_FileType type;
        public int mtime;
        public NativeByteBuffer bytes;

        public static TL_upload_file TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
            if (TL_upload_file.constructor != constructor) {
                if (exception) {
                    throw new RuntimeException(String.format("can't parse magic %x in TL_upload_file", constructor));
                } else {
                    return null;
                }
            }
            TL_upload_file result = new TL_upload_file();
            result.readParams(stream, exception);
            return result;
        }

        public void readParams(AbstractSerializedData stream, boolean exception) {
            type = storage_FileType.TLdeserialize(stream, stream.readInt32(exception), exception);
            mtime = stream.readInt32(exception);
            bytes = stream.readByteBuffer(exception);
        }

        @Override
        public void freeResources() {
            if (disableFree) {
                return;
            }
            if (bytes != null) {
                bytes.reuse();
                bytes = null;
            }
        }
    }

    //functions

    public static class Vector extends TLObject {
        public static int constructor = 0x1cb5c415;
        public ArrayList<Object> objects = new ArrayList<>();
    }
}
