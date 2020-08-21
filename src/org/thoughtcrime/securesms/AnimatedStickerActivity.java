package org.thoughtcrime.securesms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
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
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class AnimatedStickerActivity extends PassphraseRequiredActionBarActivity implements OnClickCategory {
  private static final String STICKER_PACK_SELECTED = "STICKER_PACK_SELECTED";
  private static final String STICKERS_PACKS = "STICKERS_PACKS";
  private static final int STICKER_REQUEST_CODE = 13;
  private int stickerPackSelection = -1;


  private final DynamicTheme dynamicTheme = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();
  private @NonNull
  RecyclerView recycler;
  boolean canceled = false;
  Menu menu;
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

    if (stickers == null) {
      stickers = getStickers(Environment.getExternalStorageDirectory());
    }
    recycler.post(() -> {
      recycler.setLayoutManager(new GridLayoutManager(AnimatedStickerActivity.this, 2));
      recycler.setAdapter(new AnimatedStickersAdapter(getFiles(), AnimatedStickerActivity.this));
      if (stickerPackSelection != -1) {
        ((AnimatedStickersAdapter) recycler.getAdapter()).setCategory(true);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.stickers_menu, menu);
    this.menu=menu;
    menu.getItem(0).setOnMenuItemClickListener(item -> {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("application/zip");
      startActivityForResult(Intent.createChooser(intent, "Buscar pack con..."), STICKER_REQUEST_CODE);
      return true;
    });
 
    menu.getItem(1).setOnMenuItemClickListener(item -> {
      if (stickerPackSelection != -1) {
        new AlertDialog.Builder(this).setMessage("¿Desea eliminar este pack de stickers?").setPositiveButton("Aceptar", (dialog, which) -> {
          StickerPack removed = stickers.remove(stickerPackSelection);
          for (Uri file : removed.files) {
            File file1 = new File(file.getPath());
            if (file1.exists()) {
              file1.delete();
            }
          }
          removed.folder.delete();
          stickerPackSelection=-1;
          if(menu!=null){
            changeMenuVisibility();
          }
          ((AnimatedStickersAdapter)recycler.getAdapter()).setCategory(false);
          ((AnimatedStickersAdapter)recycler.getAdapter()).changeData(getFiles());
          dialog.dismiss();
        }).setNegativeButton("Cancelar", (dialog, which) -> {
          dialog.dismiss();
        }).setOnCancelListener((dialogInterface) -> {
          dialogInterface.dismiss();
        }).show() ;
      }
      return true;
     });
    changeMenuVisibility();
    return super.onCreateOptionsMenu(menu);
  }

  private void changeMenuVisibility() {
    if (stickerPackSelection == -1) {
      menu.getItem(0).setVisible(true);
      menu.getItem(1).setVisible(false);
    } else {
      menu.getItem(0).setVisible(false);
      menu.getItem(1).setVisible(true);
    }
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && requestCode == STICKER_REQUEST_CODE) {
      Uri uri = data.getData();
      long length = DocumentFile.fromSingleUri(this, uri).length();
      if ((((length / 1024) / 1024) / 2) > 1) {
        Toast.makeText(this, "El archivo es muy grande para ser un pack de stickers \uD83E\uDD14️", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Agregando el pack", Toast.LENGTH_SHORT).show();
        File cacheDir = new File(getCacheDir(), "tgscache");
        if(!cacheDir.exists()){
          cacheDir.mkdir();
        }
        AsyncTask.execute(() -> {
          try {
            File cache = File.createTempFile("cache", ".zip", cacheDir);
            Util.copy(getContentResolver().openInputStream(uri), new FileOutputStream(cache));
            ZipFile zipFile = new ZipFile(cache);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ArrayList<File> TGSfiles = new ArrayList<>();
            while (entries.hasMoreElements()) {
              ZipEntry zipEntry = entries.nextElement();
              if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".tgs")) {
                InputStream stream = zipFile.getInputStream(zipEntry);
                String[] split = zipEntry.getName().split("/");
                File tgs = new File(cacheDir, split[split.length-1]);
                if (!tgs.exists()) {
                  tgs.createNewFile();
                  Util.copy(stream, new FileOutputStream(tgs));
                  TGSfiles.add(tgs);
                }
              }
            }
            cache.delete();
            if (TGSfiles.size() > 0) {
              String folder = String.valueOf(new Date().getTime());
              File deltaNewStickers = new File(new File(Environment.getExternalStorageDirectory(), "DeltaStickers"), folder);
              if (!deltaNewStickers.exists()) {
                deltaNewStickers.mkdir();
              }
              List<Uri> files = new ArrayList<>();
              for (File tgSfile : TGSfiles) {
                File file = new File(deltaNewStickers, tgSfile.getName());
                files.add(Uri.fromFile(file));
                if (!file.exists()) {
                  file.createNewFile();
                }
                Util.copy(new FileInputStream(tgSfile), new FileOutputStream(file));
                tgSfile.delete();
              }
              if (!canceled) {
                stickers.add(new StickerPack(deltaNewStickers, files));
                recycler.post(() -> {
                  ((AnimatedStickersAdapter) recycler.getAdapter()).changeData(getFiles());
                });
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
            if (this != null) {
              recycler.post(() -> {
                Toast.makeText(this, "Hubo un problema al procesar el archivo \uD83D\uDC1E️", Toast.LENGTH_SHORT).show();
              });
            }
          }
        });
      }
    }
  }


  @Override
  public void onBackPressed() {
    if (stickerPackSelection != -1) {
      stickerPackSelection = -1;
      try {
        ((AnimatedStickersAdapter) recycler.getAdapter()).setCategory(false);
        ((AnimatedStickersAdapter) recycler.getAdapter()).changeData(getFiles());
        if(menu!=null){
            changeMenuVisibility();
        }
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
      if(menu!=null){
        changeMenuVisibility();
      }
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
