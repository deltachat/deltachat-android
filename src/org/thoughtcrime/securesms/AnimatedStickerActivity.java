package org.thoughtcrime.securesms;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.RenderMode;

import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.FileProviderUtil;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class AnimatedStickerActivity extends PassphraseRequiredActionBarActivity implements OnClickCategory {
  private static final String STICKER_PACK_SELECTED = "STICKER_PACK_SELECTED";
  private static final String STICKERS_PACKS = "STICKERS_PACKS";
  private int stickerPackSelection = -1;


  private final DynamicTheme dynamicTheme = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();
  private @NonNull
  RecyclerView recycler;
  boolean canceled = false;
  ArrayList<StickerPack> stickers;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }


  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    if (savedInstanceState != null) {
      stickerPackSelection = savedInstanceState.getInt(STICKER_PACK_SELECTED, -1);
      stickers = savedInstanceState.getParcelableArrayList(STICKERS_PACKS);
    }
    super.onCreate(savedInstanceState, ready);
    setContentView(R.layout.activity_select_animated_stickers);
    recycler = findViewById(R.id.animated_sticker_list);

    ProgressDialog progressDialog = ProgressDialog.show(this, "Cargando Stickers", "Este proceso puede demorar algun tiempo", true, true, dialog -> {
      dialog.dismiss();
      AnimatedStickerActivity.this.finish();
      canceled = true;
    });
    AsyncTask.execute(() -> {
      if (stickers == null) {
        stickers = getStickers(Environment.getExternalStorageDirectory());
      }
      recycler.post(() -> {
        if (!canceled) {
          progressDialog.dismiss();
          recycler.setLayoutManager(new GridLayoutManager(AnimatedStickerActivity.this, 2));
          recycler.setAdapter(new AnimatedStickersAdapter(AnimatedStickerActivity.this.getFiles(), AnimatedStickerActivity.this));
          if (stickerPackSelection != -1) {
            ((AnimatedStickersAdapter) recycler.getAdapter()).setCategory(true);
          }
        }
      });

    });
  }

  private ArrayList<StickerPack> getStickers(File internal) {
    ArrayList<StickerPack> stickerPacks = new ArrayList<>();
    ArrayList<Uri> files = new ArrayList<>();
    File stickerDeltaFiles = new File(internal, "DeltaStickers");
    if (!stickerDeltaFiles.exists()) {
      stickerDeltaFiles.mkdir();
    }
    if (stickerDeltaFiles.isDirectory()) {
      File[] folders = stickerDeltaFiles.listFiles(pathname -> pathname.isDirectory());
      for (File folder : folders) {
        File[] animatedFiles = folder.listFiles(pathname -> pathname.isFile() && pathname.getPath().endsWith(".tgs"));
        for (File a : animatedFiles) {
          files.add(Uri.fromFile(a));
        }
        if (files.size() > 0) {
          stickerPacks.add(new StickerPack(folder, files));
          files = new ArrayList<>();
        }
      }
    }
    return stickerPacks;
  }


  private @NonNull
  List<Uri> getFiles() {
    if (stickerPackSelection == -1) {
      ArrayList<Uri> files = new ArrayList<>();
      for (StickerPack i : stickers) {
        files.add(i.getCategoryPreview());
      }
      return files;
    } else {
      return stickers.get(stickerPackSelection).files;
    }
  }


  @Override
  public void onBackPressed() {
    if (stickerPackSelection != -1) {
      stickerPackSelection = -1;
      try {
        ((AnimatedStickersAdapter) recycler.getAdapter()).setCategory(false);
        ((AnimatedStickersAdapter) recycler.getAdapter()).changeData(getFiles());
      } catch (NullPointerException e) {
      }
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putInt(STICKER_PACK_SELECTED, stickerPackSelection);
    outState.putParcelableArrayList(STICKERS_PACKS, stickers);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onClickCategory(boolean isCategory, int position) {
    if (isCategory) {
      setResult(RESULT_OK, new Intent().setData(FileProviderUtil.getUriFor(this, new File(stickers.get(stickerPackSelection).files.get(position).getPath()))));
      finish();
    } else {
      stickerPackSelection = position;
      ((AnimatedStickersAdapter) recycler.getAdapter()).setCategory(true);
      ((AnimatedStickersAdapter) recycler.getAdapter()).changeData(getFiles());
      getFiles();
    }
  }


  private class AnimatedStickersAdapter extends RecyclerView.Adapter<AnimatedStickersAdapter.AnimatedStickersViewHolder> {
    private @NonNull
    List<Uri> files;
    private boolean isCategory = false;

    private OnClickCategory clickCategory;

    protected void changeData(List<Uri> files) {
      this.files = files;
      this.notifyDataSetChanged();
    }


    protected AnimatedStickersAdapter(List<Uri> files, OnClickCategory onClickCategory) {
      this.files = files;
      this.clickCategory = onClickCategory;

    }

    public void setCategory(boolean category) {
      isCategory = category;
    }

    public boolean isCategory() {
      return isCategory;
    }

    @NonNull
    @Override
    public AnimatedStickersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      return new AnimatedStickersViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.sticker_animation_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AnimatedStickersViewHolder holder, int position) {
      //the same as in document
      LottieComposition composedAnimation = getComposedAnimation(files.get(position));
      if (composedAnimation == null) {
        holder.lottie.setImageDrawable(getResources().getDrawable(R.drawable.crop__ic_cancel));
        File file = new File(files.get(position).getPath());
        if (file.exists()) {
          file.delete();
        }
      } else {
        holder.lottie.setComposition(composedAnimation);
      }
      holder.lottie.setRenderMode(RenderMode.AUTOMATIC);
      holder.lottie.setOnClickListener((v) -> {
        clickCategory.onClickCategory(isCategory, position);
      });
      holder.lottie.setFrame(1);
      holder.lottie.setOnLongClickListener((view) -> {
        holder.lottie.playAnimation();
        return true;
      });
    }

    @Override
    public int getItemCount() {
      return files.size();
    }

    private class AnimatedStickersViewHolder extends RecyclerView.ViewHolder {
      public View itemView;
      public LottieAnimationView lottie;

      public AnimatedStickersViewHolder(@NonNull View itemView) {
        super(itemView);
        this.lottie = itemView.findViewById(R.id.lottie_animation_selector);
        this.itemView = itemView;
      }
    }

    public LottieComposition getComposedAnimation(Uri uri) {
      if (uri != null) {
        try {
          //fixed for use Lottie Parser
          LottieResult<LottieComposition> composed = LottieCompositionFactory.fromJsonInputStreamSync(new GZIPInputStream(new FileInputStream(uri.getPath())), uri.toString());
          if (composed.getValue() != null) {
            if (composed.getValue() instanceof LottieComposition) {
              return composed.getValue();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }
  }
}
