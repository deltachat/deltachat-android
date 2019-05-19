package org.thoughtcrime.securesms;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.thoughtcrime.securesms.MediaDocumentsAdapter.HeaderViewHolder;
import org.thoughtcrime.securesms.MediaDocumentsAdapter.ViewHolder;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.CursorRecyclerViewAdapter;
import org.thoughtcrime.securesms.database.MediaDatabase;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MediaDocumentsAdapter extends CursorRecyclerViewAdapter<ViewHolder> implements StickyHeaderDecoration.StickyHeaderAdapter<HeaderViewHolder> {

  private final Calendar     calendar;
  private final Locale       locale;

  MediaDocumentsAdapter(Context context, Cursor cursor, Locale locale) {
    super(context, cursor);

    this.calendar     = Calendar.getInstance();
    this.locale       = locale;
  }

  @Override
  public ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.profile_document_item, parent, false));
  }

  @Override
  public void onBindItemViewHolder(ViewHolder viewHolder, @NonNull Cursor cursor) {
    MediaDatabase.MediaRecord mediaRecord = MediaDatabase.MediaRecord.from(getContext(), cursor);
    Slide                     slide       = MediaUtil.getSlideForAttachment(getContext(), mediaRecord.getAttachment());

    if (slide != null && slide.hasDocument()) {
      viewHolder.documentView.setDocument((DocumentSlide)slide);
      viewHolder.date.setText(DateUtils.getRelativeDate(getContext(), locale, mediaRecord.getDate()));
      viewHolder.documentView.setVisibility(View.VISIBLE);
      viewHolder.date.setVisibility(View.VISIBLE);
      viewHolder.documentView.setOnClickListener(view -> {
        int msgId = slide.getDcMsgId();
        ApplicationDcContext dcContext = DcHelper.getContext(getContext());
        dcContext.openForViewOrShare(msgId, Intent.ACTION_VIEW);
      });
    } else {
      viewHolder.documentView.setVisibility(View.GONE);
      viewHolder.date.setVisibility(View.GONE);
    }
  }

  @Override
  public long getHeaderId(int position) {
    if (!isActiveCursor())          return -1;
    if (isHeaderPosition(position)) return -1;
    if (isFooterPosition(position)) return -1;
    if (position >= getItemCount()) return -1;
    if (position < 0)               return -1;

    Cursor                    cursor      = getCursorAtPositionOrThrow(position);
    MediaDatabase.MediaRecord mediaRecord = MediaDatabase.MediaRecord.from(getContext(), cursor);

    calendar.setTime(new Date(mediaRecord.getDate()));
    return Util.hashCode(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
  }

  @Override
  public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.profile_document_item_header, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(HeaderViewHolder viewHolder, int position) {
    Cursor                    cursor      = getCursorAtPositionOrThrow(position);
    MediaDatabase.MediaRecord mediaRecord = MediaDatabase.MediaRecord.from(getContext(), cursor);
    viewHolder.textView.setText(DateUtils.getRelativeDate(getContext(), locale, mediaRecord.getDate()));
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final DocumentView documentView;
    private final TextView date;

    public ViewHolder(View itemView) {
      super(itemView);
      this.documentView = itemView.findViewById(R.id.document_view);
      this.date         = itemView.findViewById(R.id.date);
    }
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {

    private final TextView textView;

    HeaderViewHolder(View itemView) {
      super(itemView);
      this.textView = itemView.findViewById(R.id.text);
    }
  }

}
