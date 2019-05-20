package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;

import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.MediaUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class ProfileDocumentsAdapter extends StickyHeaderGridAdapter {

  @SuppressWarnings("unused")
  private static final String TAG = ProfileDocumentsAdapter.class.getSimpleName();

  private final Context             context;
  private final Locale              locale;
  private final ItemClickListener   itemClickListener;
  private final Set<DcMsg>          selected;

  private  BucketedThreadMedia media;

  private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
    private final DocumentView documentView;
    private final AudioView    audioView;
    private final TextView     date;

    public ViewHolder(View v) {
      super(v);
      documentView      = v.findViewById(R.id.document_view);
      audioView         = v.findViewById(R.id.audio_view);
      date              = v.findViewById(R.id.date);
    }
  }

  private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
    TextView textView;

    HeaderHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.text);
    }
  }

  ProfileDocumentsAdapter(@NonNull Context context,
                        BucketedThreadMedia media,
                        Locale locale,
                        ItemClickListener clickListener)
  {
    this.context           = context;
    this.locale            = locale;
    this.media             = media;
    this.itemClickListener = clickListener;
    this.selected          = new HashSet<>();
  }

  public void setMedia(BucketedThreadMedia media) {
    this.media = media;
  }

  @Override
  public StickyHeaderGridAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
    return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.profile_item_header, parent, false));
  }

  @Override
  public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
    return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_document_item, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
    ((HeaderHolder)viewHolder).textView.setText(media.getName(section, locale));
  }

  @Override
  public void onBindItemViewHolder(ItemViewHolder itemViewHolder, int section, int offset) {
    ViewHolder    viewHolder        = ((ViewHolder)itemViewHolder);
    DcMsg         dcMsg             = media.get(section, offset);
    Slide         slide             = MediaUtil.getSlideForMsg(context, dcMsg);

    if (slide != null && slide.hasAudio()) {
      viewHolder.documentView.setVisibility(View.GONE);

      viewHolder.audioView.setVisibility(View.VISIBLE);
      viewHolder.audioView.setAudio((AudioSlide)slide, dcMsg.getDuration());
    }
    else if (slide != null && slide.hasDocument()) {
      viewHolder.documentView.setVisibility(View.VISIBLE);
      viewHolder.documentView.setDocument((DocumentSlide)slide);
      viewHolder.documentView.setOnClickListener(view -> {
        ApplicationDcContext dcContext = DcHelper.getContext(context);
        dcContext.openForViewOrShare(dcMsg.getId(), Intent.ACTION_VIEW);
      });

      viewHolder.audioView.setVisibility(View.GONE);
    }
    else {
      viewHolder.documentView.setVisibility(View.GONE);
      viewHolder.audioView.setVisibility(View.GONE);
    }

    viewHolder.date.setText(DateUtils.getBriefRelativeTimeSpanString(context, locale, dcMsg.getTimestamp()));
  }

  @Override
  public int getSectionCount() {
    return media.getSectionCount();
  }

  @Override
  public int getSectionItemCount(int section) {
    return media.getSectionItemCount(section);
  }

  public void toggleSelection(@NonNull DcMsg mediaRecord) {
    if (!selected.remove(mediaRecord)) {
      selected.add(mediaRecord);
    }
    notifyDataSetChanged();
  }

  public int getSelectedMediaCount() {
    return selected.size();
  }

  @NonNull
  public Collection<DcMsg> getSelectedMedia() {
    return new HashSet<>(selected);
  }

  public void clearSelection() {
    selected.clear();
    notifyDataSetChanged();
  }

  interface ItemClickListener {
    void onMediaClicked(@NonNull DcMsg mediaRecord);
    void onMediaLongClicked(DcMsg mediaRecord);
  }
}

/*
public class ProfileDocumentsAdapter extends CursorRecyclerViewAdapter<ViewHolder> implements StickyHeaderDecoration.StickyHeaderAdapter<HeaderViewHolder> {

  private final Calendar     calendar;
  private final Locale       locale;

  ProfileDocumentsAdapter(Context context, Cursor cursor, Locale locale) {
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
*/