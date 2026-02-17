package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StickerPickerView extends RecyclerView {

  private static final String TAG = StickerPickerView.class.getSimpleName();
  private static final String STICKER_FOLDER = "stickers";

  @Nullable private StickerPickerListener listener;
  private GlideRequests glideRequests;
  private File stickerDir;

  public StickerPickerView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public StickerPickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    glideRequests = GlideApp.with(context);
    stickerDir = new File(context.getFilesDir(), STICKER_FOLDER);
    setLayoutManager(new GridLayoutManager(context, 4));
    loadStickers();
  }

  public void setStickerPickerListener(@Nullable StickerPickerListener listener) {
    this.listener = listener;
  }

  public void loadStickers() {
    List<File> stickerFiles = getSavedStickers();
    setAdapter(new StickerAdapter(getContext(), glideRequests, stickerFiles, listener));
  }

  private List<File> getSavedStickers() {
    List<File> stickerFiles = new ArrayList<>();
    
    if (!stickerDir.exists()) {
      return stickerFiles;
    }

    File[] files = stickerDir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile()) {
          stickerFiles.add(file);
        }
      }
    }

    return stickerFiles;
  }

  public interface StickerPickerListener {
    void onStickerSelected(@NonNull File stickerFile);
    void onStickerDeleted(@NonNull File stickerFile);
  }

  static class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerViewHolder> {

    private final Context context;
    private final GlideRequests glideRequests;
    private final List<File> stickerFiles;
    private final LayoutInflater layoutInflater;
    private final StickerPickerListener listener;

    StickerAdapter(@NonNull Context context, 
                    @NonNull GlideRequests glideRequests, 
                    @NonNull List<File> stickerFiles,
                    @Nullable StickerPickerListener listener) {
      this.context = context;
      this.glideRequests = glideRequests;
      this.stickerFiles = stickerFiles;
      this.layoutInflater = LayoutInflater.from(context);
      this.listener = listener;
    }

    @Override
    public @NonNull StickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = layoutInflater.inflate(R.layout.sticker_picker_item, parent, false);
      return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerViewHolder holder, int position) {
      File stickerFile = stickerFiles.get(position);
      holder.stickerFile = stickerFile;

      glideRequests.load(stickerFile)
                   .diskCacheStrategy(DiskCacheStrategy.NONE)
                   .into(holder.image);
    }

    @Override
    public int getItemCount() {
      return stickerFiles.size();
    }

    @Override
    public void onViewRecycled(@NonNull StickerViewHolder holder) {
      super.onViewRecycled(holder);
      glideRequests.clear(holder.image);
    }

    private void deleteSticker(File stickerFile) {
      if (stickerFile != null && stickerFile.exists()) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
          .setTitle(R.string.delete)
          .setMessage(R.string.ask_delete_sticker)
          .setPositiveButton(R.string.delete, (dialog, which) -> {
            if (stickerFile.delete()) {
              int position = stickerFiles.indexOf(stickerFile);
              if (position >= 0) {
                stickerFiles.remove(position);
                notifyItemRemoved(position);
              }
              if (listener != null) {
                listener.onStickerDeleted(stickerFile);
              }
            }
          })
          .setNegativeButton(R.string.cancel, null)
          .show();
      }
    }

    class StickerViewHolder extends RecyclerView.ViewHolder {

      private File stickerFile;
      private final ImageView image;

      StickerViewHolder(View itemView) {
        super(itemView);
        image = itemView.findViewById(R.id.sticker_image);
        itemView.setOnClickListener(view -> {
          if (listener != null && stickerFile != null) {
            listener.onStickerSelected(stickerFile);
          }
        });
        itemView.setOnLongClickListener(view -> {
          if (stickerFile != null) {
            deleteSticker(stickerFile);
            return true;
          }
          return false;
        });
      }
    }
  }
}
