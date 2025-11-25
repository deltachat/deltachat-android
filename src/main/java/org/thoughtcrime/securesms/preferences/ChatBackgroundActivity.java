package org.thoughtcrime.securesms.preferences;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ServiceUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutionException;

public class ChatBackgroundActivity extends PassphraseRequiredActionBarActivity {

    Button galleryButton;
    Button defaultButton;
    ImageView preview;

    String tempDestinationPath;
    Uri imageUri;
    Boolean imageUpdate = false;

    private int accountId;

    @Override
    protected void onCreate(Bundle savedInstanceState, boolean ready) {
        setContentView(R.layout.activity_select_chat_background);

        defaultButton = findViewById(R.id.set_default_button);
        galleryButton = findViewById(R.id.from_gallery_button);
        preview = findViewById(R.id.preview);
        accountId = DcHelper.getContext(getApplicationContext()).getAccountId();

        defaultButton.setOnClickListener(new DefaultClickListener());
        galleryButton.setOnClickListener(new GalleryClickListener());

        String backgroundImagePath = Prefs.getBackgroundImagePath(this, accountId);
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
                            String destination = context.getFilesDir().getAbsolutePath() + "/background."+ accountId;
                            Prefs.setBackgroundImagePath(context, accountId, destination);
                            scaleAndSaveImage(context, destination);
                        }
                    };
                    thread.start();
                } else {
                    Prefs.setBackgroundImagePath(context, accountId, "");
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
            // resize so that the larger side fits the screen accurately
            int largerSide = Math.max(size.x, size.y);
            Bitmap scaledBitmap = GlideApp.with(context)
                    .asBitmap()
                    .load(imageUri)
                    .fitCenter()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .submit(largerSide, largerSide)
                    .get();
            FileOutputStream outStream = new FileOutputStream(destinationPath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Prefs.setBackgroundImagePath(context, accountId, "");
            showBackgroundSaveError();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Prefs.setBackgroundImagePath(context, accountId, "");
            showBackgroundSaveError();
        }
    }

    private void setLayoutBackgroundImage(String backgroundImagePath) {
        Drawable image = Drawable.createFromPath(backgroundImagePath);
        preview.setImageDrawable(image);
    }

    private void setDefaultLayoutBackgroundImage() {
        if(DynamicTheme.isDarkTheme(this)) {
            Drawable image = getResources().getDrawable(R.drawable.background_hd_dark);
            preview.setImageDrawable(image);
        }
        else {
            Drawable image = getResources().getDrawable(R.drawable.background_hd);
            preview.setImageDrawable(image);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Context context = getApplicationContext();
        if (data != null && context != null && resultCode == RESULT_OK && requestCode == ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND) {
            imageUri = data.getData();
            if (imageUri != null) {
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
            AttachmentManager.selectImage(ChatBackgroundActivity.this, ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND);
        }
    }
}
