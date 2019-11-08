package org.thoughtcrime.securesms.components;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.MediaStoreSignature;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.CursorRecyclerViewAdapter;
import org.thoughtcrime.securesms.database.loaders.RecentPhotosLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;

public class RecentPhotoViewRail extends FrameLayout implements LoaderManager.LoaderCallbacks<Cursor> {

  @NonNull  private final RecyclerView          recyclerView;
  @Nullable private       OnItemClickedListener listener;

  public RecentPhotoViewRail(Context context) {
    this(context, null);
  }

  public RecentPhotoViewRail(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RecentPhotoViewRail(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    inflate(context, R.layout.recent_photo_view, this);

    this.recyclerView = ViewUtil.findById(this, R.id.photo_list);
    this.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    this.recyclerView.setItemAnimator(new DefaultItemAnimator());
  }

  public void setListener(@Nullable OnItemClickedListener listener) {
    this.listener = listener;

    if (this.recyclerView.getAdapter() != null) {
      ((RecentPhotoAdapter)this.recyclerView.getAdapter()).setListener(listener);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new RecentPhotosLoader(getContext());
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    this.recyclerView.setAdapter(new RecentPhotoAdapter(getContext(), data, RecentPhotosLoader.BASE_URL, listener));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    ((CursorRecyclerViewAdapter)this.recyclerView.getAdapter()).changeCursor(null);
  }

  private static class RecentPhotoAdapter extends CursorRecyclerViewAdapter<RecentPhotoAdapter.RecentPhotoViewHolder> {

    @SuppressWarnings("unused")
    private static final String TAG = RecentPhotoAdapter.class.getName();

    @NonNull  private final Uri baseUri;
    @Nullable private OnItemClickedListener clickedListener;

    private RecentPhotoAdapter(@NonNull Context context, @NonNull Cursor cursor, @NonNull Uri baseUri, @Nullable OnItemClickedListener listener) {
      super(context, cursor);
      this.baseUri         = baseUri;
      this.clickedListener = listener;
    }

    @Override
    public RecentPhotoViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
      View itemView = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.recent_photo_view_item, parent, false);

      return new RecentPhotoViewHolder(itemView);
    }

    @Override
    public void onBindItemViewHolder(RecentPhotoViewHolder viewHolder, @NonNull Cursor cursor) {
      viewHolder.imageView.setImageDrawable(null);

      long   id           = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
      long   dateTaken    = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN));
      long   dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED));
      String mimeType     = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE));
      int    orientation  = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));

      final Uri uri = Uri.withAppendedPath(baseUri, Long.toString(id));

      Key signature = new MediaStoreSignature(mimeType, dateModified, orientation);

      GlideApp.with(getContext().getApplicationContext())
              .load(uri)
              .signature(signature)
              .diskCacheStrategy(DiskCacheStrategy.NONE)
              .into(viewHolder.imageView);

      viewHolder.imageView.setOnClickListener(v -> {
        if (clickedListener != null) clickedListener.onItemClicked(uri);
      });

    }

    public void setListener(@Nullable OnItemClickedListener listener) {
      this.clickedListener = listener;
    }

    static class RecentPhotoViewHolder extends RecyclerView.ViewHolder {

      ImageView imageView;

      RecentPhotoViewHolder(View itemView) {
        super(itemView);

        this.imageView = ViewUtil.findById(itemView, R.id.thumbnail);
      }
    }
  }

  public interface OnItemClickedListener {
    void onItemClicked(Uri uri);
  }
}
