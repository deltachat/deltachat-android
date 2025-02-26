package org.thoughtcrime.securesms.mms;


import androidx.annotation.Nullable;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.attachments.Attachment;

import java.util.List;

public class QuoteModel {

  private final DcContact author;
  private final String           text;
  private final List<Attachment> attachments;
  private final DcMsg quotedMsg;

  public QuoteModel(DcContact author, String text, @Nullable List<Attachment> attachments, DcMsg quotedMsg) {
    this.author      = author;
    this.text        = text;
    this.attachments = attachments;
    this.quotedMsg = quotedMsg;
  }

  public DcContact getAuthor() {
    return author;
  }

  public String getText() {
    return text;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public DcMsg getQuotedMsg() {
    return quotedMsg;
  }
}
