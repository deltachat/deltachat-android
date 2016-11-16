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

public class TLRPC {

    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;
	public static final int MESSAGE_FLAG_EDITED             = 0x00008000;

	public static class DraftMessage extends TLObject {
		public int flags;
		public boolean no_webpage;
		public int reply_to_msg_id;
		public String message;
		public ArrayList<MessageEntity> entities = new ArrayList<>();
		public int date;
	}

    public static class ChatPhoto extends TLObject {
		public FileLocation photo_small;
		public FileLocation photo_big;
	}

	public static class TL_chatPhotoEmpty extends ChatPhoto {
	}

	public static class TL_error extends TLObject {
		public int code;
		public String text;
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
	}

	public static class TL_documentAttributeAnimated extends DocumentAttribute {
	}

	public static class TL_documentAttributeImageSize extends DocumentAttribute {
	}

	public static class TL_documentAttributeSticker extends DocumentAttribute {
	}

	public static class TL_documentAttributeVideo extends DocumentAttribute {
	}

	public static class TL_documentAttributeAudio extends DocumentAttribute {
	}

	public static class TL_documentAttributeFilename extends DocumentAttribute {
	}

    public static class messages_Messages extends TLObject {
        public ArrayList<Message> messages = new ArrayList<>();
        public ArrayList<Chat> chats = new ArrayList<>();
        public ArrayList<User> users = new ArrayList<>();
        public int flags;
        public int pts;
        public int count;
    }

    public static class TL_messages_messages extends messages_Messages {
    }

	public static class Peer extends TLObject {
		public final int channel_id = 0;
		public int user_id;
		public int chat_id;
	}

	public static class TL_peerUser extends Peer {
	}

	public static class TL_peerChat extends Peer {
	}

	public static class GeoPoint extends TLObject {
		public double _long;
		public double lat;
	}

	public static class TL_geoPoint extends GeoPoint {
	}

	public static class SendMessageAction extends TLObject {
		public int progress;
	}

	public static class TL_inputPhoneContact extends TLObject {
		public long client_id;
		public String phone;
		public String first_name;
		public String last_name;
	}

	public static class PrivacyRule extends TLObject {
		public ArrayList<Integer> users = new ArrayList<>();

	}

	public static class TL_privacyValueAllowUsers extends PrivacyRule {
	}

	public static class TL_privacyValueDisallowAll extends PrivacyRule {
	}

	public static class TL_privacyValueAllowAll extends PrivacyRule {
	}

	public static class TL_privacyValueDisallowUsers extends PrivacyRule {
	}

	public static class TL_messageMediaEmpty extends MessageMedia {
	}

	public static class TL_messageMediaVenue extends MessageMedia {
	}

	public static class TL_messageMediaDocument extends MessageMedia {
	}

	public static class TL_messageMediaContact extends MessageMedia {
	}

	public static class TL_messageMediaPhoto extends MessageMedia {
	}

	public static class TL_messageMediaGeo extends MessageMedia {
	}

	public static class TL_messageMediaWebPage extends MessageMedia {
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
	}


	public static class PeerNotifySettings extends TLObject {
		public int flags;
		public boolean silent;
		public int mute_until;
		public String sound;
	}

	public static class TL_peerNotifySettings extends PeerNotifySettings {
	}

	public static class TL_messages_stickerSet extends TLObject {
		public StickerSet set;
		public ArrayList<TL_stickerPack> packs = new ArrayList<>();
		public ArrayList<Document> documents = new ArrayList<>();
	}

	public static class InputGeoPoint extends TLObject {
		public double lat;
		public double _long;
	}

	public static class TL_inputGeoPoint extends InputGeoPoint {
	}

	public static class TL_inputGeoPointEmpty extends InputGeoPoint {
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
	}

	public static class ChatFull extends TLObject {
		public int flags;
		public int id;
		public String about;
		public int pinned_msg_id;
		public ChatParticipants participants;
	}

	public static class TL_topPeerCategoryPeers extends TLObject {
		public TopPeerCategory category;
		public int count;
	}

	public static class InputUser extends TLObject {
		public int user_id;
		public long access_hash;
	}

	public static class TL_inputUserEmpty extends InputUser {
	}

	public static class TL_inputUserSelf extends InputUser {
	}

	public static class TL_inputUser extends InputUser {
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
		public int embed_width;
		public int embed_height;
		public int duration;
		public String author;
		public int date;
		public Document document;
	}

	public static class TL_webPageEmpty extends WebPage {
	}

    public static class TL_webPage extends WebPage {
    }

    public static class InputFileLocation extends TLObject {
		public long id;
		public long access_hash;
		public long volume_id;
		public int local_id;
		public long secret;
	}

	public static class TL_inputDocumentFileLocation extends InputFileLocation {
	}

	public static class TL_inputFileLocation extends InputFileLocation {
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
        public int flags;
		public boolean self;
		public boolean contact;
		public boolean mutual_contact;
		public boolean deleted;
		public boolean bot;
		public boolean verified;
		public boolean restricted;
		public boolean min;
		public String restriction_reason;
		public String bot_inline_placeholder;
    }

	public static class TL_userContact_old2 extends User {
	}

	public static class MessageAction extends TLObject {
		public String title;
		public String address;
		public ArrayList<Integer> users = new ArrayList<>();
		public final int channel_id = 0;
		public Photo photo;
		public int chat_id;
		public int user_id;
		public UserProfilePhoto newUserPhoto;
	}

	public static class TL_messageActionChatCreate extends MessageAction {
	}

	public static class TL_messageActionHistoryClear extends MessageAction {
	}

	public static class TL_messageActionChatEditPhoto extends MessageAction {
	}

	public static class TL_messageActionChatAddUser extends MessageAction {
	}

	public static class TL_messageActionChatDeleteUser extends MessageAction {
	}

	public static class TL_messageActionUserUpdatedPhoto extends MessageAction {
	}

	public static class TL_messageActionChatDeletePhoto extends MessageAction {
	}

	public static class TL_messageActionChatEditTitle extends MessageAction {
	}

	public static class TL_messageActionEmpty extends MessageAction {
	}

	public static class Video extends TLObject {
		public long id;
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
	}

	public static class TopPeerCategory extends TLObject {
	}

	public static class InputDocument extends TLObject {
		public long id;
		public long access_hash;
	}

	public static class TL_inputDocument extends InputDocument {
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
		public String mr_path;
	}

	public static class TL_document extends Document {
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
	}

	public static class TL_inputMediaContact extends InputMedia {
	}

	public static class TL_inputMediaUploadedThumbDocument extends InputMedia {
	}

	public static class TL_inputMediaDocument extends InputMedia {
		public InputDocument id;
	}

	public static class TL_inputMediaGifExternal extends InputMedia {
	}

	public static class TL_inputMediaGeoPoint extends InputMedia {
	}

	public static class TL_inputMediaUploadedPhoto extends InputMedia {
	}

	public static class TL_inputMediaVenue extends InputMedia {
	}

	public static class TL_inputMediaUploadedDocument extends InputMedia {
	}

	public static class TL_inputMediaPhoto extends InputMedia {
		public InputPhoto id;
	}

	public static class InputStickerSet extends TLObject {
		public long id;
		public long access_hash;
		public String short_name;
	}

	public static class TL_inputStickerSetEmpty extends InputStickerSet {
	}

	public static class TL_inputStickerSetID extends InputStickerSet {
	}

	public static class TL_inputStickerSetShortName extends InputStickerSet {
	}

	public static class UserStatus extends TLObject {
		public int expires;
	}

	public static class TL_userStatusLastWeek extends UserStatus {
	}

	public static class TL_userStatusLastMonth extends UserStatus {
    }

	public static class TL_userStatusRecently extends UserStatus {
	}

	public static class Update extends TLObject {
		public int chat_id;
		public int user_id;
		public int date;
		public int version;
		public DraftMessage draft;
		public int pts;
		public byte[] data;
		public int flags;
		public String query;
		public String offset;
		public int channel_id;
		public SendMessageAction action;
		public boolean blocked;
		public String location;
		public boolean enabled;
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
	}

	public static class InputEncryptedFile extends TLObject {
		public long id;
		public int parts;
	}

	public static class UserProfilePhoto extends TLObject {
		public FileLocation photo_small;
		public FileLocation photo_big;
	}

	public static class MessageEntity extends TLObject {
		public int offset;
		public int length;
		public String url;
		public String language;
	}

	public static class TL_messageEntityTextUrl extends MessageEntity {
	}

	public static class TL_messageEntityEmail extends MessageEntity {
	}

	public static class TL_messageEntityPre extends MessageEntity {
	}

	public static class TL_messageEntityUrl extends MessageEntity {
	}

	public static class TL_messageEntityItalic extends MessageEntity {
	}

	public static class TL_messageEntityMention extends MessageEntity {
	}

	public static class TL_messageEntityMentionName extends MessageEntity {
		public int user_id;
	}

	public static class TL_inputMessageEntityMentionName extends MessageEntity {
		public InputUser user_id;
	}

	public static class TL_messageEntityBold extends MessageEntity {
	}

	public static class TL_messageEntityHashtag extends MessageEntity {
	}

	public static class TL_messageEntityCode extends MessageEntity {
	}

	public static class Photo extends TLObject {
		public long id;
		public long access_hash;
		public int user_id;
		public int date;
		public String caption;
		public ArrayList<PhotoSize> sizes = new ArrayList<>();
	}

	public static class TL_photo extends Photo {
	}

	public static class TL_photoEmpty extends Photo {
	}

	public static class TL_contact extends TLObject {
		public int user_id;
		public boolean mutual;
	}

    public static class InputChannel extends TLObject {
        public int channel_id;
    }

	public static class ChatParticipants extends TLObject {
		public int flags;
		public int chat_id;
		public ArrayList<ChatParticipant> participants = new ArrayList<>();
		public int version;
	}

	public static class ChatParticipant extends TLObject {
		public int user_id;
		public int date;
	}

	public static class Chat extends TLObject {
		public int flags;
		public boolean creator;
		public boolean kicked;
		public boolean deactivated;
		public int id;
		public String title;
		public ChatPhoto photo;
		public int participants_count;
		public int date;
		public int version;
		public boolean editor;
		public boolean broadcast;
		public boolean verified;
		public boolean megagroup;
		public boolean left;
		public String username;
		public boolean restricted;
		public boolean democracy;
		public boolean signatures;
		public String restriction_reason;
		public boolean min;
		public InputChannel migrated_to;
		public String address;
	}

	public static class StickerSet extends TLObject {
		public long id;
		public long access_hash;
		public String title;
		public int flags;
		public boolean installed;
		public boolean disabled;
		public boolean official;
		public int count;
		public int hash;
	}

	public static class MessagesFilter extends TLObject {
	}

	public static class TL_inputMessagesFilterDocument extends MessagesFilter {
	}

	public static class TL_inputMessagesFilterMusic extends MessagesFilter {
	}

	public static class TL_inputMessagesFilterUrl extends MessagesFilter {
	}

	public static class TL_inputMessagesFilterVoice extends MessagesFilter {
	}

	public static class TL_inputMessagesFilterEmpty extends MessagesFilter {
	}

	public static class TL_inputMessagesFilterPhotoVideo extends MessagesFilter {
	}

	public static class TL_messageFwdHeader extends TLObject {
		public int flags;
		public int from_id;
		public int date;
		public int channel_id;
		public int channel_post;
	}

	public static class FileLocation extends TLObject {
		public int dc_id;
		public long volume_id;
		public int local_id;
		public String mr_path; // if set, this superseeds the calculation of the path from dc_id/volume_id/local_id
		public byte[] key;
        public byte[] iv;
	}

	public static class TL_fileLocation extends FileLocation {
	}

	public static class TL_fileLocationUnavailable extends FileLocation {
	}

	public static class PhotoSize extends TLObject {
		public String type;
		public FileLocation location;
		public int w;
		public int h;
		public int size;
		public byte[] bytes;
	}

	public static class TL_photoSizeEmpty extends PhotoSize { // used as an indicator via instanceof
	}

	public static class TL_photoSize extends PhotoSize {
	}


	public static class TL_photoCachedSize extends PhotoSize {
	}

	public static class ExportedChatInvite extends TLObject {
		public String link;
	}

	public static class InputFile extends TLObject {
		public long id;
		public int parts;
		public String name;
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
		public boolean silent;
		public int id;
		public int user_id;
		public String message;
		public int pts;
		public MessageMedia media;
		public Update update;
		public int from_id;
		public int chat_id;
	}

	public static class WallPaper extends TLObject {
		public int id;
		public String title;
		public ArrayList<PhotoSize> sizes = new ArrayList<>();
		public int color;
		public int bg_color;
	}

	public static class TL_wallPaper extends WallPaper {
	}

	public static class TL_wallPaperSolid extends WallPaper {
	}

	public static class TL_stickerPack extends TLObject {
		public ArrayList<Long> documents = new ArrayList<>();
	}

	public static class InputChatPhoto extends TLObject {
		public InputPhoto id;
		public InputPhotoCrop crop;
		public InputFile file;
	}

	public static class TL_inputChatPhotoEmpty extends InputChatPhoto {
	}

	public static class TL_inputChatUploadedPhoto extends InputChatPhoto {
	}

	public static class InputPhoto extends TLObject {
		public long id;
		public long access_hash;
	}

	public static class TL_inputPhoto extends InputPhoto {
	}

	public static class InputPeer extends TLObject {
		public int user_id;
		public long access_hash;
		public int chat_id;
		public int channel_id;
	}

	public static class TL_inputPeerUser extends InputPeer {
	}

	public static class TL_inputPeerEmpty extends InputPeer {
	}

	public static class TL_topPeer extends TLObject {
		public Peer peer;
	}

	public static class InputPhotoCrop extends TLObject {
		public double crop_left;
		public double crop_top;
	}

	public static class TL_inputPhotoCropAuto extends InputPhotoCrop {
	}

	public static class TL_contacts_importContacts extends TLObject {
		public ArrayList<TL_inputPhoneContact> contacts = new ArrayList<>();
		public boolean replace;
	}

	public static class TL_contacts_deleteContacts extends TLObject {
		public ArrayList<InputUser> id = new ArrayList<>();
	}

	public static class TL_contacts_block extends TLObject {
		public InputUser id;
	}

	public static class TL_contacts_unblock extends TLObject {
		public InputUser id;
	}

	public static class TL_contacts_getBlocked extends TLObject {
		public int offset;
		public int limit;
	}

	public static class TL_messages_getDialogs extends TLObject {
		public int offset_date;
		public int offset_id;
		public InputPeer offset_peer;
		public int limit;
	}

	public static class TL_messages_search extends TLObject {
		public int flags;
		public InputPeer peer;
		public String q;
		public MessagesFilter filter;
		public int offset;
		public int max_id;
		public int limit;
	}

	public static class TL_messages_searchGlobal extends TLObject {
		public String q;
		public int offset_date;
		public InputPeer offset_peer;
		public int offset_id;
		public int limit;
	}

    public static class TL_messages_editChatPhoto extends TLObject {
        public int chat_id;
        public InputChatPhoto photo;
    }

	public static class TL_photos_uploadProfilePhoto extends TLObject {
		public InputFile file;
        public String caption;
		public InputGeoPoint geo_point;
		public InputPhotoCrop crop;
	}

    public static class TL_contacts_resolveUsername extends TLObject {
        public String username;
    }

	public static class TL_messages_checkChatInvite extends TLObject {
		public String hash;
	}

	public static class TL_messages_importChatInvite extends TLObject {
		public String hash;
	}

	public static class MessageMedia extends TLObject {
		public byte[] bytes;
		public Photo photo;
		public GeoPoint geo;
		public String title;
		public String address;
		public String provider;
		public String venue_id;
		public Document document;
		public String caption;
		public String phone_number;
		public String first_name;
		public String last_name;
		public int user_id;
		public WebPage webpage;
	}

    public static class EncryptedChat extends TLObject {
        public int id;
        public int date;
        public byte[] nonce;
        public byte[] auth_key; //custom
        public int user_id; //custom
        public int ttl; //custom
        public int layer; //custom
        public byte[] key_hash; //custom
    }

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
        public String attachPath = ""; //custom
		public HashMap<String, String> params; //custom
        public long random_id; //custom
        public long dialog_id; //custom
        public int ttl; //custom
        public int destroyTime; //custom
        public int layer; //custom
        public TLRPC.Message replyMessage; //custom
		public boolean created_by_mr;
    }

	public static class TL_messageEmpty extends Message {
	}

	public static class TL_message extends Message {
	}

	public static class TL_messageService extends Message {
	}

	public static class TL_dialog extends TLObject {
		final public int flags = 0;
		public Peer peer;
		public int top_message;
		public int unread_count;
		public PeerNotifySettings notify_settings;
		public int pts;
		public DraftMessage draft;
		public int last_message_date; //custom
		public long id; //custom
	}

    public static class TL_chatEmpty extends Chat {
    }
    public static class Vector extends TLObject {
        public ArrayList<Object> objects = new ArrayList<>();
    }
}
