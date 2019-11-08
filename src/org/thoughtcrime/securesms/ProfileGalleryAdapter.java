package org.thoughtcrime.securesms;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.b44t.messenger.DcMsg;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;

import org.thoughtcrime.securesms.components.ThumbnailView;
import org.thoughtcrime.securesms.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.util.MediaUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class ProfileGalleryAdapter extends StickyHeaderGridAdapter {

  @SuppressWarnings("unused")
  private static final String TAG = ProfileGalleryAdapter.class.getSimpleName();

  private final Context             context;
  private final GlideRequests       glideRequests;
  private final Locale              locale;
  private final ItemClickListener   itemClickListener;
  private final Set<DcMsg>    selected;

  private  BucketedThreadMedia media;

  private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
    ThumbnailView imageView;
    View          selectedIndicator;

    ViewHolder(View v) {
      super(v);
      imageView         = v.findViewById(R.id.image);
      selectedIndicator = v.findViewById(R.id.selected_indicator);
    }
  }

  private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
    TextView textView;

    HeaderHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.label);
    }
  }

  ProfileGalleryAdapter(@NonNull Context context,
                        @NonNull GlideRequests glideRequests,
                        BucketedThreadMedia media,
                        Locale locale,
                        ItemClickListener clickListener)
  {
    this.context           = context;
    this.glideRequests     = glideRequests;
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
    return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.contact_selection_list_divider, parent, false));
  }

  @Override
  public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
    return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_gallery_item, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
    ((HeaderHolder)viewHolder).textView.setText(media.getName(section, locale));
  }

  @Override
  public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int offset) {
    DcMsg         mediaRecord       = media.get(section, offset);
    ThumbnailView thumbnailView     = ((ViewHolder)viewHolder).imageView;
    View          selectedIndicator = ((ViewHolder)viewHolder).selectedIndicator;
    Slide         slide             = MediaUtil.getSlideForMsg(context, mediaRecord);

    if (slide != null) {
      thumbnailView.setImageResource(glideRequests, slide);
    }

    thumbnailView.setOnClickListener(view -> itemClickListener.onMediaClicked(mediaRecord));
    thumbnailView.setOnLongClickListener(view -> {
      itemClickListener.onMediaLongClicked(mediaRecord);
      return true;
    });

    selectedIndicator.setVisibility(selected.contains(mediaRecord) ? View.VISIBLE : View.GONE);
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
