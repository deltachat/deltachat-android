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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.attachments.Attachment;

import java.util.LinkedList;
import java.util.List;

public class SlideDeck {

  private final List<Slide> slides = new LinkedList<>();

  public SlideDeck() {
  }

  public void clear() {
    slides.clear();
  }

  @NonNull
  public List<Attachment> asAttachments() {
    List<Attachment> attachments = new LinkedList<>();

    for (Slide slide : slides) {
      attachments.add(slide.asAttachment());
    }

    return attachments;
  }

  public void addSlide(Slide slide) {
    slides.add(slide);
  }

  public List<Slide> getSlides() {
    return slides;
  }

  public boolean containsMediaSlide() {
    for (Slide slide : slides) {
      if (slide.hasImage() || slide.hasVideo() || slide.hasAudio() || slide.hasDocument()) {
        return true;
      }
    }
    return false;
  }

  public @Nullable DocumentSlide getDocumentSlide() {
    for (Slide slide: slides) {
      if (slide.hasDocument()) {
        return (DocumentSlide)slide;
      }
    }

    return null;
  }

  // Webxdc requires draft-ids to be used; this function returns the previously used draft-id, if any.
  public int getWebxdctDraftId() {
    for (Slide slide: slides) {
      if (slide.isWebxdcDocument()) {
        return slide.dcMsgId;
      }
    }
    return 0;
  }
}
