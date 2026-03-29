package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.util.Util;

public class StickerPickerView extends RecyclerView {

  private static final String STICKER_FOLDER = "stickers";

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
    stickerDir = new File(context.getFilesDir(), STICKER_FOLDER);
    setAdapter(new StickerAdapter(context));
    setLayoutManager(new GridLayoutManager(context, 4));
    loadStickers();
  }

  public void setStickerPickerListener(@Nullable StickerPickerListener listener) {
    assert getAdapter() != null;
    ((StickerAdapter) getAdapter()).setStickerPickerListener(listener);
  }

  public void loadStickers() {
    assert getAdapter() != null;
    ((StickerAdapter) getAdapter()).changeData(getSavedStickers());
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

    // Sort stickers just to provide consistent order
    Collections.sort(stickerFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

    return stickerFiles;
  }

  public interface StickerPickerListener {
    void onStickerSelected(@NonNull File stickerFile);

    void onStickerDeleted(@NonNull File stickerFile);
  }

  static class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerViewHolder> {

    private final Context context;
    private final GlideRequests glideRequests;
    private final LayoutInflater layoutInflater;
    private StickerPickerListener listener;
    private List<File> stickerFiles = new ArrayList<>();

    StickerAdapter(@NonNull Context context) {
      this.context = context;
      this.glideRequests = GlideApp.with(context);
      this.layoutInflater = LayoutInflater.from(context);
    }

    public void changeData(@NonNull List<File> stickerFiles) {
      this.stickerFiles = stickerFiles;
      notifyDataSetChanged();
    }

    public void setStickerPickerListener(@Nullable StickerPickerListener listener) {
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

      glideRequests.load(stickerFile).diskCacheStrategy(DiskCacheStrategy.NONE).into(holder.image);
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
        AlertDialog dialog =
            new AlertDialog.Builder(context)
                .setMessage(R.string.ask_delete_sticker)
                .setPositiveButton(
                    R.string.delete,
                    (d, which) -> {
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
        Util.redPositiveButton(dialog);
      }
    }

    class StickerViewHolder extends RecyclerView.ViewHolder {

      private File stickerFile;
      private final ImageView image;

      StickerViewHolder(View itemView) {
        super(itemView);
        image = itemView.findViewById(R.id.sticker_image);
        itemView.setOnClickListener(
            view -> {
              if (listener != null && stickerFile != null) {
                listener.onStickerSelected(stickerFile);
              }
            });
        itemView.setOnLongClickListener(
            view -> {
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
