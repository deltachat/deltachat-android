package org.thoughtcrime.securesms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;

import org.thoughtcrime.securesms.components.AudioView;
import org.thoughtcrime.securesms.components.DocumentView;
import org.thoughtcrime.securesms.components.WebxdcView;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import org.thoughtcrime.securesms.mms.AudioSlide;
import org.thoughtcrime.securesms.mms.DocumentSlide;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.util.DateUtils;
import org.thoughtcrime.securesms.util.MediaUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class AllMediaDocumentsAdapter extends StickyHeaderGridAdapter {

  private final Context             context;
  private final ItemClickListener   itemClickListener;
  private final Set<DcMsg>          selected;

  private  BucketedThreadMedia media;

  private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
    private final DocumentView documentView;
    private final AudioView    audioView;
    private final WebxdcView   webxdcView;
    private final TextView     date;

    public ViewHolder(View v) {
      super(v);
      documentView      = v.findViewById(R.id.document_view);
      audioView         = v.findViewById(R.id.audio_view);
      webxdcView        = v.findViewById(R.id.webxdc_view);
      date              = v.findViewById(R.id.date);
    }
  }

  private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
    final TextView textView;

    HeaderHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.label);
    }
  }

  AllMediaDocumentsAdapter(@NonNull Context context,
                           BucketedThreadMedia media,
                           ItemClickListener clickListener)
  {
    this.context           = context;
    this.media             = media;
    this.itemClickListener = clickListener;
    this.selected          = new HashSet<>();
  }

  public void setMedia(BucketedThreadMedia media) {
    this.media = media;
  }

  @Override
  public StickyHeaderGridAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
    return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.contact_selection_list_divider, parent, false));
  }

  @Override
  public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
    return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_document_item, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
    ((HeaderHolder)viewHolder).textView.setText(media.getName(section));
  }

  @Override
  public void onBindItemViewHolder(ItemViewHolder itemViewHolder, int section, int offset) {
    ViewHolder    viewHolder        = ((ViewHolder)itemViewHolder);
    DcMsg         dcMsg             = media.get(section, offset);
    Slide         slide             = MediaUtil.getSlideForMsg(context, dcMsg);

    if (slide != null && slide.hasAudio()) {
      viewHolder.documentView.setVisibility(View.GONE);
      viewHolder.webxdcView.setVisibility(View.GONE);

      viewHolder.audioView.setVisibility(View.VISIBLE);
      viewHolder.audioView.setAudio((AudioSlide)slide, dcMsg.getDuration());
      viewHolder.audioView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.audioView.setOnLongClickListener(view -> { itemClickListener.onMediaLongClicked(dcMsg); return true; });
      viewHolder.audioView.disablePlayer(!selected.isEmpty());
      viewHolder.itemView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.date.setVisibility(View.VISIBLE);
    }
    else if (slide != null && slide.isWebxdcDocument()) {
      viewHolder.audioView.setVisibility(View.GONE);
      viewHolder.documentView.setVisibility(View.GONE);

      viewHolder.webxdcView.setVisibility(View.VISIBLE);
      viewHolder.webxdcView.setWebxdc(dcMsg, "");
      viewHolder.webxdcView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.webxdcView.setOnLongClickListener(view -> { itemClickListener.onMediaLongClicked(dcMsg); return true; });
      viewHolder.itemView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.date.setVisibility(View.GONE);
    }
    else if (slide != null && slide.hasDocument()) {
      viewHolder.audioView.setVisibility(View.GONE);
      viewHolder.webxdcView.setVisibility(View.GONE);

      viewHolder.documentView.setVisibility(View.VISIBLE);
      viewHolder.documentView.setDocument((DocumentSlide)slide);
      viewHolder.documentView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.documentView.setOnLongClickListener(view -> { itemClickListener.onMediaLongClicked(dcMsg); return true; });
      viewHolder.itemView.setOnClickListener(view -> itemClickListener.onMediaClicked(dcMsg));
      viewHolder.date.setVisibility(View.VISIBLE);
    }
    else {
      viewHolder.documentView.setVisibility(View.GONE);
      viewHolder.audioView.setVisibility(View.GONE);
      viewHolder.webxdcView.setVisibility(View.GONE);
      viewHolder.date.setVisibility(View.GONE);
    }

    viewHolder.itemView.setOnLongClickListener(view -> { itemClickListener.onMediaLongClicked(dcMsg); return true; });
    viewHolder.itemView.setSelected(selected.contains(dcMsg));

    viewHolder.date.setText(DateUtils.getBriefRelativeTimeSpanString(context, dcMsg.getTimestamp()));
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

  public void selectAll() {
    selected.clear();
    selected.addAll(media.getAll());
    notifyDataSetChanged();
  }

  public int getSelectedMediaCount() {
    return selected.size();
  }

  public Set<DcMsg> getSelectedMedia() {
    return Collections.unmodifiableSet(new HashSet<>(selected));
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
