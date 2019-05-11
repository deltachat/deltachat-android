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
import android.content.res.Resources.Theme;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.DcAttachment;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ResUtil;

import java.io.File;

public class VideoSlide extends Slide {

  private static Attachment constructVideoAttachment(Context context, Uri uri, long dataSize)
  {
    Uri thumbnailUri = Uri.fromFile(new File(DcHelper.getContext(context).getBlobdirFile("temp-preview.jpg")));
    MediaUtil.ThumbnailSize retWh = new MediaUtil.ThumbnailSize(0, 0);
    MediaUtil.createVideoThumbnailIfNeeded(context, uri, thumbnailUri, retWh);
    return constructAttachmentFromUri(context, uri, MediaUtil.VIDEO_UNSPECIFIED, dataSize, retWh.width, retWh.height, thumbnailUri, null, false);
  }

  public VideoSlide(Context context, Uri uri, long dataSize) {
    super(context, constructVideoAttachment(context, uri, dataSize));
  }

  public VideoSlide(Context context, DcMsg dcMsg) {
    super(context, new DcAttachment(dcMsg));
    dcMsgId = dcMsg.getId();
  }

  public VideoSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }

  @Override
  public boolean hasPlayOverlay() {
    return true;
  }

  @Override
  public boolean hasPlaceholder() {
    return true;
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    // the placeholder is shown when a video is staged before sending.
    // TODO: VIDEO: not needed, we should show the normal thumbnail plus transparent play or edit buttons
    return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_video);
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @Override
  public boolean hasVideo() {
    return true;
  }

  @NonNull @Override
  public String getContentDescription() {
    return context.getString(R.string.video);
  }
}
