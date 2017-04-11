/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.messenger;

import java.util.ArrayList;
import java.util.HashMap;

public class TLRPC {

    public static final int MESSAGE_FLAG_FWD                = 0x00000004;
    public static final int MESSAGE_FLAG_REPLY              = 0x00000008;
    public static final int MESSAGE_FLAG_HAS_FROM_ID        = 0x00000100;
    public static final int MESSAGE_FLAG_HAS_MEDIA          = 0x00000200;

	public static class DraftMessage extends TLObject {
		public int flags;
		public int reply_to_msg_id;
		public String message;
		public int date;
	}

    public static class ChatPhoto extends TLObject {
		public FileLocation photo_big;
	}

	public static class DocumentAttribute extends TLObject {
		public int w;
		public int h;
		public int duration;
		public String alt;
		public InputStickerSet stickerset;
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

	public static class Peer extends TLObject {
		public int user_id;
		public final int chat_id = 0;
	}

	public static class TL_peerUser extends Peer {
	}

	public static class TL_messageMediaEmpty extends MessageMedia {
	}

	public static class TL_messageMediaDocument extends MessageMedia {
	}

	public static class TL_messageMediaPhoto extends MessageMedia {
	}

	public static class TL_messages_stickerSet extends TLObject {
		public StickerSet set;
		public ArrayList<Document> documents = new ArrayList<>();
	}

	public static class Audio extends TLObject {
		public long id;
		public int date;
		public int duration;
		public String mime_type;
		public int size;
		public int user_id;
	}

	public static class User extends TLObject {
		public int id;
		public String first_name;
		public String last_name;
		public String username;
		public UserProfilePhoto photo;
        public int flags;
    }

	public static class Video extends TLObject {
		public long id;
		public int user_id;
		public int date;
		public int duration;
		public int size;
		public PhotoSize thumb;
		public int w;
		public int h;
		public String mime_type;
		public String caption;
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

	public static class InputStickerSet extends TLObject {
		public long id;
		public String short_name;
	}

	public static class TL_inputStickerSetEmpty extends InputStickerSet {
	}

	public static class UserProfilePhoto extends TLObject {
		public FileLocation photo_small;
		public FileLocation photo_big;
	}

	public static class Photo extends TLObject {
		public long id;
		public int user_id;
		public int date;
		public String caption;
		public ArrayList<PhotoSize> sizes = new ArrayList<>();
	}

	public static class TL_photo extends Photo {
	}

	public static class TL_photoEmpty extends Photo {
	}

	public static class Chat extends TLObject {
		public int flags;
		public int id;
		public String title;
		public final ChatPhoto photo = null;
		public int date;
		public final int version = 0;
		public final boolean editor = false;
		public String username;
		public final boolean signatures = false;
		public final boolean min = false;
		public final String address = null;
	}

	public static class StickerSet extends TLObject {
		public long id;
		public String title;
		public int flags;
		public boolean installed;
		public boolean disabled;
		public boolean official;
		public int count;
		public int hash;
	}

	public static class TL_messageFwdHeader extends TLObject {
		public String m_name;
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
		public String type; // s, m, x, y, w for small, medium, ... widest
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

	public static class InputFile extends TLObject {
		public long id;
		public int parts;
		public String name;
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

	public static class MessageMedia extends TLObject {
		public byte[] bytes;
		public Photo photo;
		public String title;
		public String address;
		public String provider;
		public Document document;
		public String caption;
		public int user_id;
	}

    public static class Message extends TLObject {
        public int id;
        public int from_id;
        public Peer to_id;
        public int date;
        public final int reply_to_msg_id = 0;
        public String message;
        public MessageMedia media;
        public int flags;
		public boolean media_unread;
		public boolean out;
		public boolean unread;
		public final int views = 0;
		public final boolean silent = false;
		public final boolean post = false;// ? true=avatar wird in gruppen nicht angezeigt, wird aber in isFromUser() auch überprüft...
		public TL_messageFwdHeader fwd_from;
        public int send_state = 0; //custom
        public String attachPath = ""; //custom
		public HashMap<String, String> params; //custom
        public long dialog_id; //custom
        public final int layer = 0; //custom
		public boolean created_by_mr;
    }

	public static class TL_message extends Message {
	}

	public static class TL_dialog extends TLObject {
		final public int flags = 0;
		public Peer peer;
		public final int top_message = 0;
		public final int unread_count = 0;
		public final int pts = 0;
		public DraftMessage draft;
		public long id; //custom
	}
}
