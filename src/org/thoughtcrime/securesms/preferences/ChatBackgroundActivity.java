package org.thoughtcrime.securesms.preferences;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.Prefs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutionException;

public class ChatBackgroundActivity extends BaseActionBarActivity {

    Button galleryButton;
    Button defaultButton;
    MenuItem acceptMenuItem;
    ImageView preview;

    String tempDestinationPath;
    Uri imageUri;
    Boolean imageUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_chat_background);

        defaultButton = findViewById(R.id.set_default_button);
        galleryButton = findViewById(R.id.from_gallery_button);
        preview = findViewById(R.id.preview);

        defaultButton.setOnClickListener(new DefaultClickListener());
        galleryButton.setOnClickListener(new GalleryClickListener());

        String backgroundImagePath = Prefs.getBackgroundImagePath(this);
        if(backgroundImagePath.isEmpty()){
            setDefaultLayoutBackgroundImage();
        }else {
            setLayoutBackgroundImage(backgroundImagePath);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.pref_background);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.chat_background, menu);
        acceptMenuItem = menu.findItem(R.id.apply_background);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.apply_background) {
            // handle confirmation button click here
            Context context = getApplicationContext();
            if(imageUpdate) {
                if (imageUri != null) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            String destination = context.getFilesDir().getAbsolutePath() + "/background";
                            Prefs.setBackgroundImagePath(context, destination);
                            scaleAndSaveImage(context, destination);
                        }
                    };
                    thread.start();
                } else {
                    Prefs.setBackgroundImagePath(context, "");
                }
            }
            finish();
            return true;
        } else if (id == android.R.id.home) {
            // handle close button click here
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void scaleAndSaveImage(Context context, String destinationPath) {
        try{
            Display display = ServiceUtil.getWindowManager(context).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            Bitmap scaledBitmap = GlideApp.with(context)
                    .asBitmap()
                    .load(imageUri)
                    .centerCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .submit(size.x, size.y)
                    .get();
            FileOutputStream outStream = new FileOutputStream(destinationPath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Prefs.setBackgroundImagePath(context, "");
            showBackgroundSaveError();
        } catch (ExecutionException e) {
            e.printStackTrace();
            Prefs.setBackgroundImagePath(context, "");
            showBackgroundSaveError();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Prefs.setBackgroundImagePath(context, "");
            showBackgroundSaveError();
        }
    }

    private void setLayoutBackgroundImage(String backgroundImagePath) {
        Drawable image = Drawable.createFromPath(backgroundImagePath);
        preview.setImageDrawable(image);
    }

    private void setDefaultLayoutBackgroundImage() {
        Drawable image = getResources().getDrawable(R.drawable.background_hd);
        preview.setImageDrawable(image);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Context context = getApplicationContext();
        if (data != null && context != null && resultCode == RESULT_OK && requestCode == ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND) {
            imageUri = data.getData();
            if (imageUri != null) {
                acceptMenuItem.setEnabled(true);
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        tempDestinationPath = context.getFilesDir().getAbsolutePath() + "/background-temp";
                        scaleAndSaveImage(context, tempDestinationPath);
                        runOnUiThread(() -> {
                            // Stuff that updates the UI
                            setLayoutBackgroundImage(tempDestinationPath);
                        });
                    }
                };
                thread.start();
            }
            imageUpdate=true;
        }
    }

    private void showBackgroundSaveError() {
        Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
    }

    private class DefaultClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            imageUri = null;
            tempDestinationPath = "";
            setDefaultLayoutBackgroundImage();
            imageUpdate=true;
        }

    }

    private class GalleryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND);
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND);
            }
        }
    }
}
