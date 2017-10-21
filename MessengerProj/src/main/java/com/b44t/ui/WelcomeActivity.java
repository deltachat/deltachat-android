/*******************************************************************************
 *
 *                              Delta Chat Android
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.BuildConfig;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.Theme;

import java.io.File;

import static java.lang.Math.min;

public class WelcomeActivity extends Activity implements NotificationCenter.NotificationCenterDelegate {

    private boolean isAbout = false;
    private final static int NUM_PAGES = 7;
    private final static int REQ_PERMISSION_CODE = 42;

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MessengerProj);
        super.onCreate(savedInstanceState);
        Theme.loadRecources(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.imexProgress);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.imexEnded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.configureEnded);

        Bundle extras = getIntent().getExtras();
        if( extras != null && extras.getBoolean("com.b44t.ui.IntroActivity.isAbout") ) {
            isAbout = true;
        }

        float heightOnLandscape = min(AndroidUtilities.displaySize.x,AndroidUtilities.displaySize.y)/AndroidUtilities.density;
        if( heightOnLandscape < 450 ) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.welcome_layout);

        ViewPager viewPager = (ViewPager) findViewById(R.id.welcome_pager);
        Button primaryButton = (Button) findViewById(R.id.welcome_primary_button);
        TextView secondaryButton = (TextView) findViewById(R.id.welcome_secondary_button);

        if( isAbout ) {
            primaryButton.setText(ApplicationLoader.applicationContext.getString(R.string.OK));
            primaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            secondaryButton.setText("v" + getVersion() + " | " + ApplicationLoader.applicationContext.getString(R.string.Info));
            secondaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String msgtext = "© 2017 Delta Chat contributors" + "\n\n" + MrMailbox.getInfo() + "\n\n" + getAndroidInfo();
                    new AlertDialog.Builder(WelcomeActivity.this)
                        .setTitle(ApplicationLoader.applicationContext.getString(R.string.AppName) + " v" + getVersion())
                        .setMessage(msgtext)
                        .setPositiveButton(R.string.OK, null)
                        .setNeutralButton(R.string.CopyToClipboard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AndroidUtilities.addToClipboard(msgtext);
                                AndroidUtilities.showDoneHint(WelcomeActivity.this);
                            }
                        })
                        .show();
                }
            });
        }
        else {
            primaryButton.setText(ApplicationLoader.applicationContext.getString(R.string.IntroStartMessaging));
            primaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent2 = new Intent(WelcomeActivity.this, LaunchActivity.class);
                    intent2.putExtra("fromIntro", true);
                    startActivity(intent2);
                }
            });

            secondaryButton.setText(ApplicationLoader.applicationContext.getString(R.string.ImportBackup));
            secondaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMISSION_CODE);
                        return;
                    }
                    tryToStartImport();
                }
            });
        }

        viewPager.setAdapter(new IntroAdapter());
        viewPager.setPageMargin(0);
        viewPager.setCurrentItem(LocaleController.isRTL? NUM_PAGES-1 : 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.imexProgress);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.imexEnded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.configureEnded);
    }

    private class IntroAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = View.inflate(container.getContext(), R.layout.welcome_page_layout, null);
            ImageView imgView = (ImageView)view.findViewById(R.id.welcome_img);
            TextView titleView = (TextView) view.findViewById(R.id.welcome_title);
            TextView msgView = (TextView) view.findViewById(R.id.welcome_msg);
            container.addView(view, 0);

            int icons[] = new int[]{R.drawable.intro1, R.drawable.intro2, R.drawable.intro3, R.drawable.intro4, R.drawable.intro5, R.drawable.intro6, R.drawable.intro7 };
            int titles[] = new int[]{ R.string.Intro1Headline, R.string.Intro2Headline, R.string.Intro3Headline,  R.string.Intro4Headline, R.string.Intro5Headline, R.string.Intro6Headline, R.string.Intro7Headline };
            int messages[] = new int[]{ R.string.Intro1Message, R.string.Intro2Message, R.string.Intro3Message,R.string.Intro4Message,  R.string.Intro5Message, R.string.Intro6Message, R.string.Intro7Message };

            if( LocaleController.isRTL ) {
                position = NUM_PAGES-1-position;
            }

            imgView.setImageResource(icons[position]);
            titleView.setText(getString(titles[position]));
            msgView.setText(AndroidUtilities.replaceTags(getString(messages[position])));

            return view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ViewGroup bottomPages = (ViewGroup) findViewById(R.id.bottom_pages);
            int count = bottomPages.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = bottomPages.getChildAt(a);
                if (a == position) {
                    child.setBackgroundColor(0xff2ca5e0);
                } else {
                    child.setBackgroundColor(0xffcccccc);
                }
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }


    /* Specials for the use as the about-screen
     **********************************************************************************************/

    public static String getVersion()
    {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionName;
        }
        catch(Exception e) {
            return "ErrVersion";
        }
    }

    private String getAndroidInfo()
    {
        String abi = "ErrAbi";
        int versionCode = 0, ignoreBatteryOptimizations = -1;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            versionCode = pInfo.versionCode;
            switch (versionCode % 10) {
                case 0:
                    abi = "arm";
                    break;
                case 1:
                    abi = "arm-v7a";
                    break;
                case 2:
                    abi = "x86";
                    break;
                case 3:
                    abi = "universal";
                    break;
            }
            if( Build.VERSION.SDK_INT >= 23 ) {
                PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                ignoreBatteryOptimizations = pm.isIgnoringBatteryOptimizations(ApplicationLoader.applicationContext.getPackageName())? 1 : 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot get package info.", e);
        }

        return      "SDK_INT="                    + Build.VERSION.SDK_INT
                + "\nMANUFACTURER="               + Build.MANUFACTURER
                + "\nMODEL="                      + Build.MODEL
                + "\nAPPLICATION_ID="             + BuildConfig.APPLICATION_ID
                + "\nBUILD_TYPE="                 + BuildConfig.BUILD_TYPE
                + "\nABI="                        + abi // ABI = Application Binary Interface
                + "\nignoreBatteryOptimizations=" + ignoreBatteryOptimizations
                + "\nversionCode="                + versionCode;
    }


    /* Specials for importing and configuration
     **********************************************************************************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // this may forward the notification to fragments
        if (requestCode == REQ_PERMISSION_CODE
         && grantResults.length > 0
         && grantResults[0]==PackageManager.PERMISSION_GRANTED ) {
            tryToStartImport();
        }
        else {
            new AlertDialog.Builder(this)
                .setMessage(ApplicationLoader.applicationContext.getString(R.string.PermissionStorage))
                .setPositiveButton(R.string.PermissionOpenSettings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            // the settings button is needed as the user may have selected "do not ask again" and
                            // may get in trouble to activate the feature otherwise ...
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Cannot open app settings.", e);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
        }
    }


    private void tryToStartImport() {
        File imexDir = SettingsAdvFragment.getImexDir();
        final String backupFile = MrMailbox.imexHasBackup(imexDir.getAbsolutePath());
        if (backupFile != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ImportBackup)
                    .setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.ImportBackupAsk), backupFile)))
                    .setNegativeButton(R.string.Cancel, null)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startImport(backupFile);
                        }
                    })
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ImportBackup)
                    .setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.NoBackupFound), imexDir.getAbsolutePath())))
                    .setPositiveButton(R.string.OK, null)
                    .show();
        }
    }

    private ProgressDialog progressDialog = null;
    private void startImport(String backupFile)
    {
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ApplicationLoader.applicationContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MrMailbox.imex(0, null);
            }
        });
        progressDialog.show();

        synchronized (MrMailbox.m_lastErrorLock) {
            MrMailbox.m_showNextErrorAsToast = false;
            MrMailbox.m_lastErrorString = "";
        }

        MrMailbox.imex(MrMailbox.MR_IMEX_IMPORT_BACKUP, backupFile);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.imexProgress) {
            // we want the spinner together with a progress info
            int percent = (Integer)args[0] / 10;
            progressDialog.setMessage(ApplicationLoader.applicationContext.getString(R.string.OneMoment)+String.format(" %d%%", percent));
        } else if (id == NotificationCenter.imexEnded) {

            final String errorString;

            synchronized (MrMailbox.m_lastErrorLock) {
                MrMailbox.m_showNextErrorAsToast = true;
                errorString = MrMailbox.m_lastErrorString;
            }

            if( progressDialog!=null ) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if( (int)args[0]==1 ) {
                startActivity(new Intent(WelcomeActivity.this, LaunchActivity.class));
                finish();
            }
            else if( !errorString.isEmpty() /*usually empty if export is cancelled by the user*/ ) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.ImportBackup)
                    .setMessage(errorString)
                    .setPositiveButton(R.string.OK, null)
                    .show();
            }
        }
        else if(id == NotificationCenter.configureEnded ) {
            // no need to start a new activitiy, this is done in SettingsAccountActivity(), just close self
            finish();
        }
    }
}
