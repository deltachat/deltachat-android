package org.thoughtcrime.securesms.preferences;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.thoughtcrime.securesms.ApplicationPreferencesActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static android.app.Activity.RESULT_OK;

public class AppearancePreferenceFragment extends ListSummaryPreferenceFragment {

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);

    this.findPreference(TextSecurePreferences.THEME_PREF).setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(TextSecurePreferences.LANGUAGE_PREF).setOnPreferenceChangeListener(new ListSummaryListener());
    initializeListSummary((ListPreference)findPreference(TextSecurePreferences.THEME_PREF));
    initializeListSummary((ListPreference)findPreference(TextSecurePreferences.LANGUAGE_PREF));
    this.findPreference(TextSecurePreferences.BACKGROUND_PREF).setOnPreferenceClickListener(new BackgroundClickListener());
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_appearance);
  }

  @Override
  public void onStart() {
    super.onStart();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener((ApplicationPreferencesActivity)getActivity());
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__appearance);
  }

  @Override
  public void onStop() {
    super.onStop();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener((ApplicationPreferencesActivity) getActivity());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (data != null && getContext() != null && resultCode == RESULT_OK && requestCode == ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND) {
      Uri imageUri = data.getData();
      if (imageUri != null) {
              Thread thread = new Thread(){
                @Override
                public void run() {
                  try {
                    Display display = ServiceUtil.getWindowManager(getContext()).getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    Bitmap scaledBitmap = GlideApp.with(getContext())
                            .asBitmap()
                            .load(imageUri)
                            .centerCrop()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .submit(size.x, size.y)
                            .get();
                    String destination = getContext().getFilesDir().getAbsolutePath() + "background";
                    FileOutputStream outStream = new FileOutputStream(destination);
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                    showBackgroundSaveError();
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                    showBackgroundSaveError();
                  } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    showBackgroundSaveError();
                  }
                }
              };
          thread.start();
      }
    }

  }

  private void showBackgroundSaveError() {
    Toast.makeText(getActivity(), R.string.AppearancePreferencesFragment_background_save_error, Toast.LENGTH_LONG).show();
  }

  public static CharSequence getSummary(Context context) {
    String[] languageEntries     = context.getResources().getStringArray(R.array.language_entries);
    String[] languageEntryValues = context.getResources().getStringArray(R.array.language_values);
    String[] themeEntries        = context.getResources().getStringArray(R.array.pref_theme_entries);
    String[] themeEntryValues    = context.getResources().getStringArray(R.array.pref_theme_values);

    int langIndex  = Arrays.asList(languageEntryValues).indexOf(TextSecurePreferences.getLanguage(context));
    int themeIndex = Arrays.asList(themeEntryValues).indexOf(TextSecurePreferences.getTheme(context));

    if (langIndex == -1)  langIndex = 0;
    if (themeIndex == -1) themeIndex = 0;

    return context.getString(R.string.ApplicationPreferencesActivity_appearance_summary,
                             themeEntries[themeIndex],
                             languageEntries[langIndex]);
  }

  private class BackgroundClickListener implements Preference.OnPreferenceClickListener {
    @Override
    public boolean onPreferenceClick(Preference preference) {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("image/*");
      getActivity().startActivityForResult(intent, ApplicationPreferencesActivity.REQUEST_CODE_SET_BACKGROUND);

      return true;
    }
  }
}
