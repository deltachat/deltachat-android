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
package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.UriAttachment;
import org.thoughtcrime.securesms.database.AttachmentDatabase;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public abstract class Slide {

  protected int              dcMsgId;
  protected final Attachment attachment;
  protected final Context    context;

  public Slide(@NonNull Context context, @NonNull Attachment attachment) {
    this.context    = context;
    this.attachment = attachment;

  }

  public int getDcMsgId() {
    return dcMsgId;
  }

  public String getContentType() {
    return attachment.getContentType();
  }

  @Nullable
  public Uri getUri() {
    return attachment.getDataUri();
  }

  @Nullable
  public Uri getThumbnailUri() {
    return attachment.getThumbnailUri();
  }

  @NonNull
  public Optional<String> getFileName() {
    return Optional.fromNullable(attachment.getFileName());
  }

  @Nullable
  public String getFastPreflightId() {
    return attachment.getFastPreflightId();
  }

  public long getFileSize() {
    return attachment.getSize();
  }

  /* Return true if this slide has a thumbnail when being quoted, false otherwise */
  public boolean hasQuoteThumbnail() {
      return (hasImage() || hasVideo() || hasSticker() || isWebxdcDocument() || isVcard()) && getUri() != null;
  }

  public boolean hasImage() {
    return false;
  }

  public boolean hasSticker() { return false; }

  public boolean hasVideo() {
    return false;
  }

  public boolean hasAudio() {
    return false;
  }

  public boolean hasDocument() {
    return false;
  }

  public boolean isWebxdcDocument() {
    return false;
  }

  public boolean isVcard() {
    return false;
  }

  public boolean hasLocation() {
    return false;
  }

  public Attachment asAttachment() {
    return attachment;
  }

  public long getTransferState() {
    return attachment.getTransferState();
  }

  public boolean hasPlayOverlay() {
    return false;
  }

  protected static Attachment constructAttachmentFromUri(@NonNull  Context context,
                                                         @NonNull  Uri     uri,
                                                         @NonNull  String  defaultMime,
                                                                   long     size,
                                                                   int      width,
                                                                   int      height,
                                                         @Nullable Uri      thumbnailUri,
                                                         @Nullable String   fileName,
                                                                   boolean  voiceNote)
  {
    try {
      String                 resolvedType    = Optional.fromNullable(MediaUtil.getMimeType(context, uri)).or(defaultMime);
      String                 fastPreflightId = String.valueOf(SecureRandom.getInstance("SHA1PRNG").nextLong());
      return new UriAttachment(uri,
                               thumbnailUri,
                               resolvedType,
                               AttachmentDatabase.TRANSFER_PROGRESS_STARTED,
                               size,
                               width,
                               height,
                               fileName,
                               fastPreflightId,
                               voiceNote);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)             return false;
    if (!(other instanceof Slide)) return false;

    Slide that = (Slide)other;

    return Util.equals(this.getContentType(), that.getContentType()) &&
           this.hasAudio() == that.hasAudio()                        &&
           this.hasImage() == that.hasImage()                        &&
           this.hasVideo() == that.hasVideo()                        &&
           this.getTransferState() == that.getTransferState()        &&
           Util.equals(this.getUri(), that.getUri())                 &&
           Util.equals(this.getThumbnailUri(), that.getThumbnailUri());
  }

  @Override
  public int hashCode() {
    return Util.hashCode(getContentType(), hasAudio(), hasImage(),
                         hasVideo(), getUri(), getThumbnailUri(), getTransferState());
  }
}
