/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
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
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.MailTo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.ImageLoader;
import com.b44t.messenger.KeepAliveService;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.SendMessagesHelper;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.browser.Browser;
import com.b44t.messenger.UserConfig;
import com.b44t.ui.Adapters.DrawerLayoutAdapter;
import com.b44t.ui.ActionBar.ActionBarLayout;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.ActionBar.DrawerLayoutContainer;
import com.b44t.ui.Components.LayoutHelper;
import com.b44t.ui.Components.PasscodeView;
import com.b44t.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class LaunchActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate, NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate {

    private boolean finished;
    private String videoPath;
    private String sendingText;
    private ArrayList<Uri> photoPathsArray;
    private ArrayList<String> documentsPathsArray;
    private ArrayList<Uri> documentsUrisArray;
    private String documentsMimeType;
    private ArrayList<String> documentsOriginalPathsArray;
    private ArrayList<Integer> contactsToSend;
    private static ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private static ArrayList<BaseFragment> layerFragmentsStack = new ArrayList<>();
    private static ArrayList<BaseFragment> rightFragmentsStack = new ArrayList<>();
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    private ActionBarLayout actionBarLayout;
    private ActionBarLayout layersActionBarLayout;
    private ActionBarLayout rightActionBarLayout;
    private FrameLayout shadowTablet;
    private FrameLayout shadowTabletSide;
    private ImageView backgroundTablet;
    protected DrawerLayoutContainer drawerLayoutContainer;
    private DrawerLayoutAdapter drawerLayoutAdapter;
    private PasscodeView passcodeView;
    private AlertDialog visibleDialog;

    private Intent passcodeSaveIntent;
    private boolean passcodeSaveIntentIsNew;
    private boolean passcodeSaveIntentIsRestore;

    private boolean tabletFullSize;

    private Runnable lockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if( MrMailbox.isConfigured()==0 ) {
            Intent intent = getIntent();
            if (intent != null && !intent.getBooleanExtra("fromIntro", false)) {
                Intent intent2 = new Intent(this, IntroActivity.class);
                startActivity(intent2);
                super.onCreate(savedInstanceState);
                finish();
                return;
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.Theme_MessengerProj);
        getWindow().setBackgroundDrawableResource(R.drawable.transparent);

        super.onCreate(savedInstanceState);
        Theme.loadRecources(this);

        if (UserConfig.passcodeHash.length() != 0 && UserConfig.appLocked) {
            UserConfig.lastPauseTime = MrMailbox.getCurrentTime();
        }

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            AndroidUtilities.statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        actionBarLayout = new ActionBarLayout(this);

        drawerLayoutContainer = new DrawerLayoutContainer(this);
        setContentView(drawerLayoutContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (AndroidUtilities.isTablet()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            RelativeLayout launchLayout = new RelativeLayout(this);
            drawerLayoutContainer.addView(launchLayout);
            FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) launchLayout.getLayoutParams();
            layoutParams1.width = LayoutHelper.MATCH_PARENT;
            layoutParams1.height = LayoutHelper.MATCH_PARENT;
            launchLayout.setLayoutParams(layoutParams1);

            backgroundTablet = new ImageView(this);
            backgroundTablet.setScaleType(ImageView.ScaleType.CENTER_CROP);
            backgroundTablet.setImageResource(R.drawable.background_hd);
            launchLayout.addView(backgroundTablet);
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) backgroundTablet.getLayoutParams();
            relativeLayoutParams.width = LayoutHelper.MATCH_PARENT;
            relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
            backgroundTablet.setLayoutParams(relativeLayoutParams);

            launchLayout.addView(actionBarLayout);
            relativeLayoutParams = (RelativeLayout.LayoutParams) actionBarLayout.getLayoutParams();
            relativeLayoutParams.width = LayoutHelper.MATCH_PARENT;
            relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
            actionBarLayout.setLayoutParams(relativeLayoutParams);

            rightActionBarLayout = new ActionBarLayout(this);
            launchLayout.addView(rightActionBarLayout);
            relativeLayoutParams = (RelativeLayout.LayoutParams)rightActionBarLayout.getLayoutParams();
            relativeLayoutParams.width = AndroidUtilities.dp(320);
            relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
            rightActionBarLayout.setLayoutParams(relativeLayoutParams);
            rightActionBarLayout.init(rightFragmentsStack);
            rightActionBarLayout.setDelegate(this);

            shadowTabletSide = new FrameLayout(this);
            shadowTabletSide.setBackgroundColor(0x40295274);
            launchLayout.addView(shadowTabletSide);
            relativeLayoutParams = (RelativeLayout.LayoutParams) shadowTabletSide.getLayoutParams();
            relativeLayoutParams.width = AndroidUtilities.dp(1);
            relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
            shadowTabletSide.setLayoutParams(relativeLayoutParams);

            shadowTablet = new FrameLayout(this);
            shadowTablet.setVisibility(View.GONE);
            shadowTablet.setBackgroundColor(0x7F000000);
            launchLayout.addView(shadowTablet);
            relativeLayoutParams = (RelativeLayout.LayoutParams) shadowTablet.getLayoutParams();
            relativeLayoutParams.width = LayoutHelper.MATCH_PARENT;
            relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
            shadowTablet.setLayoutParams(relativeLayoutParams);
            shadowTablet.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!actionBarLayout.fragmentsStack.isEmpty() && event.getAction() == MotionEvent.ACTION_UP) {
                        float x = event.getX();
                        float y = event.getY();
                        int location[] = new int[2];
                        layersActionBarLayout.getLocationOnScreen(location);
                        int viewX = location[0];
                        int viewY = location[1];

                        if (layersActionBarLayout.checkTransitionAnimation() || x > viewX && x < viewX + layersActionBarLayout.getWidth() && y > viewY && y < viewY + layersActionBarLayout.getHeight()) {
                            return false;
                        } else {
                            if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                                for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                                    layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                                    a--;
                                }
                                layersActionBarLayout.closeLastFragment(true);
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });

            shadowTablet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            layersActionBarLayout = new ActionBarLayout(this);
            layersActionBarLayout.setRemoveActionBarExtraHeight(true);
            layersActionBarLayout.setBackgroundView(shadowTablet);
            layersActionBarLayout.setUseAlphaAnimations(true);
            layersActionBarLayout.setBackgroundResource(R.drawable.boxshadow);
            launchLayout.addView(layersActionBarLayout);
            relativeLayoutParams = (RelativeLayout.LayoutParams)layersActionBarLayout.getLayoutParams();
            relativeLayoutParams.width = AndroidUtilities.dp(530);
            relativeLayoutParams.height = AndroidUtilities.dp(528);
            layersActionBarLayout.setLayoutParams(relativeLayoutParams);
            layersActionBarLayout.init(layerFragmentsStack);
            layersActionBarLayout.setDelegate(this);
            layersActionBarLayout.setDrawerLayoutContainer(drawerLayoutContainer);
            layersActionBarLayout.setVisibility(View.GONE);
        } else {
            drawerLayoutContainer.addView(actionBarLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        ListView listView = new ListView(this) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }
        };
        listView.setBackgroundColor(0xffffffff);
        listView.setAdapter(drawerLayoutAdapter = new DrawerLayoutAdapter(this));
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        drawerLayoutContainer.setDrawerLayout(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        Point screenSize = AndroidUtilities.getRealScreenSize();

        // Set the width of the drawer
        layoutParams.width = AndroidUtilities.isTablet() ?
                    AndroidUtilities.dp(285)
                :   Math.min( AndroidUtilities.dp(285), Math.min(screenSize.x,screenSize.y)-AndroidUtilities.dp(56) );

        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == DrawerLayoutAdapter.ROW_NEW_CHAT) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactsActivity.SELECT_CONTACT_FOR_NEW_CHAT);
                    presentFragment(new ContactsActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                }
                else if (position == DrawerLayoutAdapter.ROW_NEW_GROUP) {
                    Bundle args = new Bundle();
                    args.putInt("do_what", ContactsActivity.SELECT_CONTACTS_FOR_NEW_GROUP);
                    presentFragment(new ContactsActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                }
                else if (position == DrawerLayoutAdapter.ROW_INVITE) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, MrMailbox.getInviteText());
                        startActivity(Intent.createChooser(intent, ApplicationLoader.applicationContext.getString(R.string.InviteMenuEntry)));
                    } catch (Exception e) {
                    }
                    drawerLayoutContainer.closeDrawer(false);
                }
                else if (position == DrawerLayoutAdapter.ROW_DEADDROP) {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", MrChat.MR_CHAT_ID_DEADDROP);
                    presentFragment(new ChatActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                }
                else if (position == DrawerLayoutAdapter.ROW_SETTINGS) {
                    presentFragment(new SettingsActivity());
                    drawerLayoutContainer.closeDrawer(false);
                }
                else if (position == DrawerLayoutAdapter.ROW_FAQ) {
                    String helpUrl = ApplicationLoader.applicationContext.getString(R.string.HelpUrl);
                    Browser.openUrl(LaunchActivity.this, helpUrl);
                    drawerLayoutContainer.closeDrawer(false);
                }
            }
        });

        drawerLayoutContainer.setParentActionBarLayout(actionBarLayout);
        actionBarLayout.setDrawerLayoutContainer(drawerLayoutContainer);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);

        ApplicationLoader.loadWallpaper();

        passcodeView = new PasscodeView(this);
        drawerLayoutContainer.addView(passcodeView);
        FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) passcodeView.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = LayoutHelper.MATCH_PARENT;
        passcodeView.setLayoutParams(layoutParams1);

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeOtherAppActivities, this);

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeOtherAppActivities);

        if (actionBarLayout.fragmentsStack.isEmpty()) {
            if ( MrMailbox.isConfigured()==0 ) {
                Bundle args = new Bundle();
                args.putBoolean("fromIntro", true);
                actionBarLayout.addFragmentToStack(new SettingsAccountActivity(args));
                drawerLayoutContainer.setAllowOpenDrawer(false, false);
            } else {
                actionBarLayout.addFragmentToStack(new DialogsActivity(null));
                drawerLayoutContainer.setAllowOpenDrawer(true, false);
            }

            try {
                if (savedInstanceState != null) {
                    String fragmentName = savedInstanceState.getString("fragment");
                    if (fragmentName != null) {
                        Bundle args = savedInstanceState.getBundle("args");
                        switch (fragmentName) {
                            case "chat":
                                if (args != null) {
                                    ChatActivity chat = new ChatActivity(args);
                                    if (actionBarLayout.addFragmentToStack(chat)) {
                                        chat.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "settings": {
                                SettingsActivity settings = new SettingsActivity();
                                actionBarLayout.addFragmentToStack(settings);
                                settings.restoreSelfArgs(savedInstanceState);
                                break;
                            }
                            case "group":
                                if (args != null) {
                                    GroupCreateFinalActivity group = new GroupCreateFinalActivity(args);
                                    if (actionBarLayout.addFragmentToStack(group)) {
                                        group.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "chat_profile":
                                if (args != null) {
                                    ProfileActivity profile = new ProfileActivity(args);
                                    if (actionBarLayout.addFragmentToStack(profile)) {
                                        profile.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "wallpapers": {
                                WallpapersActivity settings = new WallpapersActivity();
                                actionBarLayout.addFragmentToStack(settings);
                                settings.restoreSelfArgs(savedInstanceState);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        } else {
            boolean allowOpen = true;
            if (AndroidUtilities.isTablet()) {
                allowOpen = actionBarLayout.fragmentsStack.size() <= 1 && layersActionBarLayout.fragmentsStack.isEmpty();
            }
            drawerLayoutContainer.setAllowOpenDrawer(allowOpen, false);
        }

        handleIntent(getIntent(), false, savedInstanceState != null, false);
        needLayout();

        final View view = getWindow().getDecorView().getRootView();
        view.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = view.getMeasuredHeight();
                if (height > AndroidUtilities.dp(100) && height < AndroidUtilities.displaySize.y && height + AndroidUtilities.dp(100) > AndroidUtilities.displaySize.y) {
                    AndroidUtilities.displaySize.y = height;
                    Log.w("DeltaChat", "fix display size y to " + AndroidUtilities.displaySize.y);
                }
            }
        });
    }

    private void showPasscodeActivity() {
        if (passcodeView == null) {
            return;
        }
        UserConfig.appLocked = true;
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(false, true);
        }
        passcodeView.onShow();
        UserConfig.isWaitingForPasscodeEnter = true;
        drawerLayoutContainer.setAllowOpenDrawer(false, false);
        passcodeView.setDelegate(new PasscodeView.PasscodeViewDelegate() {
            @Override
            public void didAcceptedPassword() {
                UserConfig.isWaitingForPasscodeEnter = false;
                if (passcodeSaveIntent != null) {
                    handleIntent(passcodeSaveIntent, passcodeSaveIntentIsNew, passcodeSaveIntentIsRestore, true);
                    passcodeSaveIntent = null;
                }
                drawerLayoutContainer.setAllowOpenDrawer(true, false);
                actionBarLayout.showLastFragment();
                if (AndroidUtilities.isTablet()) {
                    layersActionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                }
            }
        });
    }

    private class VcardData {
        String name;
        ArrayList<String> emails = new ArrayList<>();
    }

    private boolean handleIntent(Intent intent, boolean isNew, boolean restore, boolean fromPassword) {

        if (!fromPassword && (AndroidUtilities.needShowPasscode(true) || UserConfig.isWaitingForPasscodeEnter)) {
            showPasscodeActivity();
            passcodeSaveIntent = intent;
            passcodeSaveIntentIsNew = isNew;
            passcodeSaveIntentIsRestore = restore;
            UserConfig.saveConfig();
            return false;
        }

        if( intent==null ) {
            return false;
        }

        int flags = intent.getFlags();
        boolean pushOpened = false;

        Integer push_chat_id = 0;
        long dialogId = intent.getExtras() != null ? intent.getExtras().getLong("dialogId", 0) : 0;
        boolean showDialogsList = false;

        photoPathsArray = null;
        videoPath = null;
        sendingText = null;
        documentsPathsArray = null;
        documentsOriginalPathsArray = null;
        documentsMimeType = null;
        documentsUrisArray = null;
        contactsToSend = null;
        String createChatWith = null;

        if ( (flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0)
        {
            if (intent.getAction() != null && !restore)
            {
                if (Intent.ACTION_SEND.equals(intent.getAction()))
                {
                    boolean error = false;
                    String type = intent.getType();
                    if (type != null && type.equals(ContactsContract.Contacts.CONTENT_VCARD_TYPE)) {
                        try {
                            Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                            if (uri != null) {
                                ContentResolver cr = getContentResolver();
                                InputStream stream = cr.openInputStream(uri);
                                ArrayList<VcardData> vcardDatas = new ArrayList<>();
                                VcardData currentData = null;

                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                                String line;
                                while ((line = bufferedReader.readLine()) != null) {
                                    String[] args = line.split(":");
                                    if (args.length != 2) {
                                        continue;
                                    }
                                    if (args[0].equals("BEGIN") && args[1].equals("VCARD")) {
                                        vcardDatas.add(currentData = new VcardData());
                                    } else if (args[0].equals("END") && args[1].equals("VCARD")) {
                                        currentData = null;
                                    }
                                    if (currentData == null) {
                                        continue;
                                    }
                                    if (args[0].startsWith("FN") || args[0].startsWith("ORG") && TextUtils.isEmpty(currentData.name)) {
                                        String nameEncoding = null;
                                        String nameCharset = null;
                                        String[] params = args[0].split(";");
                                        for (String param : params) {
                                            String[] args2 = param.split("=");
                                            if (args2.length != 2) {
                                                continue;
                                            }
                                            if (args2[0].equals("CHARSET")) {
                                                nameCharset = args2[1];
                                            } else if (args2[0].equals("ENCODING")) {
                                                nameEncoding = args2[1];
                                            }
                                        }
                                        currentData.name = args[1];
                                        if (nameEncoding != null && nameEncoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
                                            while (currentData.name.endsWith("=") && nameEncoding != null) {
                                                currentData.name = currentData.name.substring(0, currentData.name.length() - 1);
                                                line = bufferedReader.readLine();
                                                if (line == null) {
                                                    break;
                                                }
                                                currentData.name += line;
                                            }
                                            byte[] bytes = AndroidUtilities.decodeQuotedPrintable(currentData.name.getBytes());
                                            if (bytes != null && bytes.length != 0) {
                                                String decodedName = new String(bytes, nameCharset);
                                                if (decodedName != null) {
                                                    currentData.name = decodedName;
                                                }
                                            }
                                        }
                                    } else if (args[0].startsWith("EMAIL")) {
                                        String email = args[1];
                                        if (email.length() > 0) {
                                            currentData.emails.add(email);
                                        }
                                    }
                                }
                                try {
                                    bufferedReader.close();
                                    stream.close();
                                } catch (Exception e) {

                                }
                                for (int a = 0; a < vcardDatas.size(); a++) {
                                    VcardData vcardData = vcardDatas.get(a);
                                    if (vcardData.name != null && !vcardData.emails.isEmpty()) {
                                        if (contactsToSend == null) {
                                            contactsToSend = new ArrayList<>();
                                        }

                                        for (int b = 0; b < vcardData.emails.size(); b++) {
                                            int contact_to_send_id = MrMailbox.createContact(vcardData.name, vcardData.emails.get(b));
                                            contactsToSend.add(contact_to_send_id);
                                        }
                                    }
                                }
                                if( contactsToSend==null ) {
                                    Toast.makeText(this, ApplicationLoader.applicationContext.getString(R.string.BadEmailAddress), Toast.LENGTH_SHORT).show();;
                                }
                            } else {
                                error = true;
                            }
                        } catch (Exception e) {

                            error = true;
                        }
                    } else {
                        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                        if (text == null) {
                            CharSequence textSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                            if (textSequence != null) {
                                text = textSequence.toString();
                            }
                        }
                        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                        if (text != null && text.length() != 0) {
                            if ((text.startsWith("http://") || text.startsWith("https://")) && subject != null && subject.length() != 0) {
                                text = subject + "\n" + text;
                            }
                            sendingText = text;
                        } else if (subject != null && subject.length() > 0) {
                            sendingText = subject;
                        }

                        Parcelable parcelable = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (parcelable != null) {
                            String path;
                            if (!(parcelable instanceof Uri)) {
                                parcelable = Uri.parse(parcelable.toString());
                            }
                            Uri uri = (Uri) parcelable;
                            if (uri != null) {
                                if (AndroidUtilities.isInternalUri(uri)) {
                                    error = true;
                                }
                            }
                            if (!error) {
                                if (uri != null && (type != null && type.startsWith("image/") || uri.toString().toLowerCase().endsWith(".jpg"))) {
                                    if (photoPathsArray == null) {
                                        photoPathsArray = new ArrayList<>();
                                    }
                                    photoPathsArray.add(uri);
                                } else {
                                    path = AndroidUtilities.getPath(uri);
                                    if (path != null) {
                                        if (path.startsWith("file:")) {
                                            path = path.replace("file://", "");
                                        }
                                        if (type != null && type.startsWith("video/")) {
                                            videoPath = path;
                                        } else {
                                            if (documentsPathsArray == null) {
                                                documentsPathsArray = new ArrayList<>();
                                                documentsOriginalPathsArray = new ArrayList<>();
                                            }
                                            documentsPathsArray.add(path);
                                            documentsOriginalPathsArray.add(uri.toString());
                                        }
                                    } else {
                                        if (documentsUrisArray == null) {
                                            documentsUrisArray = new ArrayList<>();
                                        }
                                        documentsUrisArray.add(uri);
                                        documentsMimeType = type;
                                    }
                                }
                            }
                        } else if (sendingText == null) {
                            error = true;
                        }
                    }
                    if (error) {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                }
                else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
                {
                    boolean error = false;
                    try {
                        ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                        String type = intent.getType();
                        if (uris != null) {
                            for (int a = 0; a < uris.size(); a++) {
                                Parcelable parcelable = uris.get(a);
                                if (!(parcelable instanceof Uri)) {
                                    parcelable = Uri.parse(parcelable.toString());
                                }
                                Uri uri = (Uri) parcelable;
                                if (uri != null) {
                                    if (AndroidUtilities.isInternalUri(uri)) {
                                        uris.remove(a);
                                        a--;
                                    }
                                }
                            }
                            if (uris.isEmpty()) {
                                uris = null;
                            }
                        }
                        if (uris != null) {
                            if (type != null && type.startsWith("image/")) {
                                for (int a = 0; a < uris.size(); a++) {
                                    Parcelable parcelable = uris.get(a);
                                    if (!(parcelable instanceof Uri)) {
                                        parcelable = Uri.parse(parcelable.toString());
                                    }
                                    Uri uri = (Uri) parcelable;
                                    if (photoPathsArray == null) {
                                        photoPathsArray = new ArrayList<>();
                                    }
                                    photoPathsArray.add(uri);
                                }
                            } else {
                                for (int a = 0; a < uris.size(); a++) {
                                    Parcelable parcelable = uris.get(a);
                                    if (!(parcelable instanceof Uri)) {
                                        parcelable = Uri.parse(parcelable.toString());
                                    }
                                    String path = AndroidUtilities.getPath((Uri) parcelable);
                                    String originalPath = parcelable.toString();
                                    if (originalPath == null) {
                                        originalPath = path;
                                    }
                                    if (path != null) {
                                        if (path.startsWith("file:")) {
                                            path = path.replace("file://", "");
                                        }
                                        if (documentsPathsArray == null) {
                                            documentsPathsArray = new ArrayList<>();
                                            documentsOriginalPathsArray = new ArrayList<>();
                                        }
                                        documentsPathsArray.add(path);
                                        documentsOriginalPathsArray.add(originalPath);
                                    }
                                }
                            }
                        } else {
                            error = true;
                        }
                    } catch (Exception e) {

                        error = true;
                    }
                    if (error) {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                }
                else if( Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SENDTO.equals(intent.getAction()) )
                {
                    // handle links
                    try {
                        Uri uri = intent.getData();
                        if( uri != null ) {
                            String scheme = uri.getScheme();
                            if( scheme != null && scheme.equals("mailto") ) {
                                // handle mailto:-links
                                MailTo mailto = MailTo.parse(uri.toString());
                                String toList = mailto.getTo(); // toList contains comma-separated email addresses
                                if( toList!=null && !toList.isEmpty() ) {
                                    String[] toArr = toList.split(",");
                                    createChatWith = toArr[0];
                                }
                                String subject = mailto.getSubject();
                                String body = mailto.getBody();
                                if (subject != null || body != null) {
                                    sendingText = subject + ((subject != null && body != null) ? " \u2013 " : "") + body;
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        Log.e("DeltaChat", "mailto failed", e);
                    }
                }
                else if (intent.getAction().startsWith("com.b44t.messenger.openchat"))
                {
                    String temp = intent.getAction().substring(27);
                    int chatId = 0;
                    try {
                        chatId = Integer.parseInt(temp, 10);
                    } catch(Exception e) {
                    }

                    if (chatId != 0) {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                        push_chat_id = chatId;
                    } else {
                        showDialogsList = true;
                    }
                }
            }
        }

        if( createChatWith!=null /*should be first as it may be combined with sendingText */ )
        {
            final String createChatWithFinal = createChatWith;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(ApplicationLoader.applicationContext.getString(R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int chatId = MrMailbox.createChatByContactId(MrMailbox.createContact("", createChatWithFinal));
                    if( chatId != 0 ) {
                        if( sendingText!=null ) { MrMailbox.getChat(chatId).setDraft(sendingText, 0); }
                        Bundle args = new Bundle();
                        args.putInt("chat_id", chatId);
                        boolean removeLast = actionBarLayout.fragmentsStack.size() > 1 && actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1) instanceof ChatActivity;
                        actionBarLayout.presentFragment(new ChatActivity(args), removeLast, true, true);
                    }

                }
            });
            builder.setNegativeButton(ApplicationLoader.applicationContext.getString(R.string.Cancel), null);
            builder.setMessage(AndroidUtilities.replaceTags(String.format(ApplicationLoader.applicationContext.getString(R.string.AskStartChatWith), createChatWith)));
            builder.show();
        }
        else if (push_chat_id != 0)
        {
            Bundle args = new Bundle();
            args.putInt("chat_id", push_chat_id);
            ChatActivity fragment = new ChatActivity(args);
            if (actionBarLayout.presentFragment(fragment, false, true, true)) {
                pushOpened = true;
            }
        }
        else if (showDialogsList)
        {
            if (!AndroidUtilities.isTablet()) {
                actionBarLayout.removeAllFragments();
            } else {
                if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                    for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                        layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                        a--;
                    }
                    layersActionBarLayout.closeLastFragment(false);
                }
            }
            pushOpened = false;
            isNew = false;
        }
        else if (videoPath != null || photoPathsArray != null || sendingText != null || documentsPathsArray != null || contactsToSend != null || documentsUrisArray != null)
        {
            if (!AndroidUtilities.isTablet()) {
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
            }
            if (dialogId == 0) {
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                args.putString("selectAlertString", ApplicationLoader.applicationContext.getString(R.string.SendMessagesTo));
                args.putString("selectAlertPreviewString", sendingText /*may be NULL*/);
                args.putString("selectAlertOkButtonString", ApplicationLoader.applicationContext.getString(R.string.Send));
                DialogsActivity fragment = new DialogsActivity(args);
                fragment.setDelegate(this);
                boolean removeLast;
                if (AndroidUtilities.isTablet()) {
                    removeLast = layersActionBarLayout.fragmentsStack.size() > 0 && layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                } else {
                    removeLast = actionBarLayout.fragmentsStack.size() > 1 && actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                }
                actionBarLayout.presentFragment(fragment, removeLast, true, true);
                pushOpened = true;
                if (PhotoViewer.getInstance().isVisible()) {
                    PhotoViewer.getInstance().closePhoto(false, true);
                }

                drawerLayoutContainer.setAllowOpenDrawer(false, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
            } else {
                didSelectDialog(null, dialogId, false);
            }
        }

        if (!pushOpened && !isNew)
        {
            if (AndroidUtilities.isTablet()) {
                if (actionBarLayout.fragmentsStack.isEmpty()) {
                    actionBarLayout.addFragmentToStack(new DialogsActivity(null));
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
            }
            else {
                if (actionBarLayout.fragmentsStack.isEmpty()) {
                    actionBarLayout.addFragmentToStack(new DialogsActivity(null));
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
            }
            actionBarLayout.showLastFragment();
            if (AndroidUtilities.isTablet()) {
                layersActionBarLayout.showLastFragment();
                rightActionBarLayout.showLastFragment();
            }
        }

        intent.setAction(null);
        return pushOpened;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent, true, false, false);
    }

    @Override
    public void didSelectDialog(DialogsActivity dialogsFragment, long dialog_id, boolean param) {
        if (dialog_id != 0) {
            Bundle args = new Bundle();
            args.putBoolean("scrollToTopOnResume", true);
            if (!AndroidUtilities.isTablet()) {
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
            }

            args.putInt("chat_id", (int)dialog_id);
            ChatActivity fragment = new ChatActivity(args);

            if (videoPath != null) {
                if(android.os.Build.VERSION.SDK_INT >= 16) {
                    if (AndroidUtilities.isTablet()) {
                        actionBarLayout.presentFragment(fragment, false, true, true);
                    } else {
                        actionBarLayout.addFragmentToStack(fragment, actionBarLayout.fragmentsStack.size() - 1);
                    }

                    if (!fragment.openVideoEditor(videoPath, dialogsFragment != null, false) && dialogsFragment != null) {
                        if (!AndroidUtilities.isTablet()) {
                            dialogsFragment.finishFragment(true);
                        }
                    }
                } else {
                    actionBarLayout.presentFragment(fragment, dialogsFragment != null, dialogsFragment == null, true);
                    SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id);
                }
            } else {
                actionBarLayout.presentFragment(fragment, dialogsFragment != null, dialogsFragment == null, true);

                if (photoPathsArray != null) {
                    ArrayList<String> captions = null;
                    if (sendingText != null && photoPathsArray.size() == 1) {
                        captions = new ArrayList<>();
                        captions.add(sendingText);
                        sendingText = null;
                    }
                    SendMessagesHelper.prepareSendingPhotos(null, photoPathsArray, dialog_id, captions);
                }

                if (sendingText != null) {
                    SendMessagesHelper.prepareSendingText(sendingText, dialog_id);
                }

                if (documentsPathsArray != null || documentsUrisArray != null) {
                    SendMessagesHelper.prepareSendingDocuments(documentsPathsArray, documentsOriginalPathsArray, documentsUrisArray, documentsMimeType, dialog_id);
                }
                if (contactsToSend != null && !contactsToSend.isEmpty()) {
                    for (int user_id : contactsToSend) {
                        SendMessagesHelper.getInstance().sendMessageContact(user_id, (int)dialog_id);
                    }
                }
            }

            photoPathsArray = null;
            videoPath = null;
            sendingText = null;
            documentsPathsArray = null;
            documentsOriginalPathsArray = null;
            contactsToSend = null;
        }
    }

    private void onFinish() {
        if (finished) {
            return;
        }
        finished = true;
        if (lockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeOtherAppActivities);
    }

    public void presentFragment(BaseFragment fragment) {
        actionBarLayout.presentFragment(fragment);
    }

    public boolean presentFragment(final BaseFragment fragment, final boolean removeLast, boolean forceWithoutAnimation) {
        return actionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, true);
    }

    public void needLayout() {
        if (AndroidUtilities.isTablet()) {
            RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) layersActionBarLayout.getLayoutParams();
            relativeLayoutParams.leftMargin = (AndroidUtilities.displaySize.x - relativeLayoutParams.width) / 2;
            int y = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
            relativeLayoutParams.topMargin = y + (AndroidUtilities.displaySize.y - relativeLayoutParams.height - y) / 2;
            layersActionBarLayout.setLayoutParams(relativeLayoutParams);


            if (!AndroidUtilities.isSmallTablet() || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                tabletFullSize = false;
                int leftWidth = AndroidUtilities.displaySize.x / 100 * 35;
                if (leftWidth < AndroidUtilities.dp(320)) {
                    leftWidth = AndroidUtilities.dp(320);
                }

                relativeLayoutParams = (RelativeLayout.LayoutParams) actionBarLayout.getLayoutParams();
                relativeLayoutParams.width = leftWidth;
                relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
                actionBarLayout.setLayoutParams(relativeLayoutParams);

                relativeLayoutParams = (RelativeLayout.LayoutParams) shadowTabletSide.getLayoutParams();
                relativeLayoutParams.leftMargin = leftWidth;
                shadowTabletSide.setLayoutParams(relativeLayoutParams);

                relativeLayoutParams = (RelativeLayout.LayoutParams) rightActionBarLayout.getLayoutParams();
                relativeLayoutParams.width = AndroidUtilities.displaySize.x - leftWidth;
                relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
                relativeLayoutParams.leftMargin = leftWidth;
                rightActionBarLayout.setLayoutParams(relativeLayoutParams);

                if (AndroidUtilities.isSmallTablet() && actionBarLayout.fragmentsStack.size() >= 2) {
                    for (int a = 1; a < actionBarLayout.fragmentsStack.size(); a++) {
                        BaseFragment chatFragment = actionBarLayout.fragmentsStack.get(a);
                        chatFragment.onPause();
                        actionBarLayout.fragmentsStack.remove(a);
                        rightActionBarLayout.fragmentsStack.add(chatFragment);
                        a--;
                    }
                    if (passcodeView.getVisibility() != View.VISIBLE) {
                        actionBarLayout.showLastFragment();
                        rightActionBarLayout.showLastFragment();
                    }
                }

                rightActionBarLayout.setVisibility(rightActionBarLayout.fragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);
                backgroundTablet.setVisibility(rightActionBarLayout.fragmentsStack.isEmpty() ? View.VISIBLE : View.GONE);
                shadowTabletSide.setVisibility(!actionBarLayout.fragmentsStack.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                tabletFullSize = true;

                relativeLayoutParams = (RelativeLayout.LayoutParams) actionBarLayout.getLayoutParams();
                relativeLayoutParams.width = LayoutHelper.MATCH_PARENT;
                relativeLayoutParams.height = LayoutHelper.MATCH_PARENT;
                actionBarLayout.setLayoutParams(relativeLayoutParams);

                shadowTabletSide.setVisibility(View.GONE);
                rightActionBarLayout.setVisibility(View.GONE);
                backgroundTablet.setVisibility(!actionBarLayout.fragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);

                if (!rightActionBarLayout.fragmentsStack.isEmpty()) {
                    for (int a = 0; a < rightActionBarLayout.fragmentsStack.size(); a++) {
                        BaseFragment chatFragment = rightActionBarLayout.fragmentsStack.get(a);
                        chatFragment.onPause();
                        rightActionBarLayout.fragmentsStack.remove(a);
                        actionBarLayout.fragmentsStack.add(chatFragment);
                        a--;
                    }
                    if (passcodeView.getVisibility() != View.VISIBLE) {
                        actionBarLayout.showLastFragment();
                    }
                }
            }
        }
    }

    public void fixLayout() {
        if (!AndroidUtilities.isTablet()) {
            return;
        }
        if (actionBarLayout == null) {
            return;
        }
        actionBarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        needLayout();
                    }
                });
                if (actionBarLayout != null) {
                    if (Build.VERSION.SDK_INT < 16) {
                        actionBarLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        actionBarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (UserConfig.passcodeHash.length() != 0 && UserConfig.lastPauseTime != 0) {
            UserConfig.lastPauseTime = 0;
            UserConfig.saveConfig();
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (actionBarLayout.fragmentsStack.size() != 0) {
            BaseFragment fragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
            fragment.onActivityResultFragment(requestCode, resultCode, data);
        }
        if (AndroidUtilities.isTablet()) {
            if (rightActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                fragment.onActivityResultFragment(requestCode, resultCode, data);
            }
            if (layersActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                fragment.onActivityResultFragment(requestCode, resultCode, data);
            }
        }
    }

    public static final int REQ_CONTACT_N_STORAGE_PERMISON_ID = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CONTACT_N_STORAGE_PERMISON_ID || requestCode == 3 || requestCode == 4 || requestCode == 5) {
            int grantedCount = 0;
            String msg = "";
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantedCount++;
                }
                else {
                    switch (permissions[i]) {
                        case Manifest.permission.READ_CONTACTS:
                            msg += "- " + ApplicationLoader.applicationContext.getString(R.string.PermissionContacts) + "\n\n";
                            break;
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            msg += "- " + ApplicationLoader.applicationContext.getString(R.string.PermissionStorage) + "\n\n";
                            break;
                        case Manifest.permission.RECORD_AUDIO:
                            msg += "- " + ApplicationLoader.applicationContext.getString(R.string.PermissionNoAudio) + "\n\n";
                            break;
                    }
                }
            }

            if (grantedCount==grantResults.length) {
                if (requestCode == 4) {
                    ImageLoader.getInstance().checkMediaPaths();
                }
                return; // everyting granted
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(msg.trim());

            builder.setPositiveButton(ApplicationLoader.applicationContext.getString(R.string.PermissionOpenSettings), new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.GINGERBREAD)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // the settings button is needed as the user may have selected "do not ask again" and
                        // may get in trouble to activate the feature otherwise ...
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {

                    }
                }
            });
            builder.setNegativeButton(ApplicationLoader.applicationContext.getString(R.string.Cancel), null);
            builder.show();
            return;
        }
        if (actionBarLayout.fragmentsStack.size() != 0) {
            BaseFragment fragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
            fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
        if (AndroidUtilities.isTablet()) {
            if (rightActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
            }
            if (layersActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onPause() {
        MrMailbox.log_i("DeltaChat", "*** LaunchActivity.onPause()");
        super.onPause();
        ApplicationLoader.mainInterfacePaused = true;
        onPasscodePause();
        actionBarLayout.onPause();
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onPause();
            layersActionBarLayout.onPause();
        }
        if (passcodeView != null) {
            passcodeView.onPause();
        }
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onPause();
        }
        ApplicationLoader.stayAwakeForAMoment();
    }

    /*@Override
    protected void onStart() {
        Log.i("DeltaChat", "*** LaunchActivity.onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i("DeltaChat", "*** LaunchActivity.onStop()");
        super.onStop();
    }*/

    @Override
    protected void onDestroy() {
        MrMailbox.log_i("DeltaChat", "*** LaunchActivity.onDestroy()");
        PhotoViewer.getInstance().destroyPhotoViewer();
        StickerPreviewViewer.getInstance().destroy();
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {

        }
        try {
            if (onGlobalLayoutListener != null) {
                final View view = getWindow().getDecorView().getRootView();
                if (Build.VERSION.SDK_INT < 16) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                }
            }
        } catch (Exception e) {

        }
        super.onDestroy();
        onFinish();
    }

    @Override
    protected void onResume() {
        MrMailbox.log_i("DeltaChat", "*** LaunchActivity.onResume()");
        super.onResume();
        ApplicationLoader.mainInterfacePaused = false;
        onPasscodeResume();
        if (passcodeView.getVisibility() != View.VISIBLE) {
            actionBarLayout.onResume();
            if (AndroidUtilities.isTablet()) {
                rightActionBarLayout.onResume();
                layersActionBarLayout.onResume();
            }
        } else {
            passcodeView.onResume();
        }
        ContactsController.cleanupAvatarCache();
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onResume();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        AndroidUtilities.checkDisplaySize();
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeOtherAppActivities) {
            if (args[0] != this) {
                onFinish();
                finish();
            }
        } else if (id == NotificationCenter.mainUserInfoChanged) {
            drawerLayoutAdapter.notifyDataSetChanged();
            KeepAliveService kas = KeepAliveService.getInstance();
            if( kas != null ) {
                kas.updateForegroundNotification();
            }
        }
    }

    private void onPasscodePause() {
        if (lockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        if (UserConfig.passcodeHash.length() != 0) {
            UserConfig.lastPauseTime = MrMailbox.getCurrentTime();
            lockRunnable = new Runnable() {
                @Override
                public void run() {
                    if (lockRunnable == this) {
                        if (AndroidUtilities.needShowPasscode(true)) {
                            //Log.i("DeltaChat", "lock app");
                            showPasscodeActivity();
                        } else {
                            //Log.i("DeltaChat", "didn't pass lock check");
                        }
                        lockRunnable = null;
                    }
                }
            };
            if (UserConfig.appLocked) {
                AndroidUtilities.runOnUIThread(lockRunnable, 1000);
            } else if (UserConfig.autoLockIn != 0) {
                AndroidUtilities.runOnUIThread(lockRunnable, (long) UserConfig.autoLockIn * 1000 + 1000);
            }
        } else {
            UserConfig.lastPauseTime = 0;
        }
        UserConfig.saveConfig();
    }

    private void onPasscodeResume() {
        if (lockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        if (AndroidUtilities.needShowPasscode(true)) {
            showPasscodeActivity();
        }
        if (UserConfig.lastPauseTime != 0) {
            UserConfig.lastPauseTime = 0;
            UserConfig.saveConfig();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            BaseFragment lastFragment = null;
            if (AndroidUtilities.isTablet()) {
                if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                } else if (!rightActionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                } else if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
                }
            } else {
                if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
                }
            }

            if (lastFragment != null) {
                Bundle args = lastFragment.getArguments();
                if (lastFragment instanceof ChatActivity && args != null) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "chat");
                } else if (lastFragment instanceof SettingsActivity) {
                    outState.putString("fragment", "settings");
                } else if (lastFragment instanceof GroupCreateFinalActivity && args != null) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "group");
                } else if (lastFragment instanceof WallpapersActivity) {
                    outState.putString("fragment", "wallpapers");
                } else if (lastFragment instanceof ProfileActivity && ((ProfileActivity) lastFragment).isChat() && args != null) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "chat_profile");
                }
                lastFragment.saveSelfArgs(outState);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        if (passcodeView.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
        } else if (drawerLayoutContainer.isDrawerOpened()) {
            drawerLayoutContainer.closeDrawer(false);
        } else if (AndroidUtilities.isTablet()) {
            if (layersActionBarLayout.getVisibility() == View.VISIBLE) {
                layersActionBarLayout.onBackPressed();
            } else {
                boolean cancel = false;
                if (rightActionBarLayout.getVisibility() == View.VISIBLE && !rightActionBarLayout.fragmentsStack.isEmpty()) {
                    BaseFragment lastFragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                    cancel = !lastFragment.onBackPressed();
                }
                if (!cancel) {
                    actionBarLayout.onBackPressed();
                }
            }
        } else {
            actionBarLayout.onBackPressed();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        actionBarLayout.onLowMemory();
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onLowMemory();
            layersActionBarLayout.onLowMemory();
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeStarted(mode);
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onActionModeStarted(mode);
            layersActionBarLayout.onActionModeStarted(mode);
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeFinished(mode);
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onActionModeFinished(mode);
            layersActionBarLayout.onActionModeFinished(mode);
        }
    }

    @Override
    public boolean onPreIme() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && !UserConfig.isWaitingForPasscodeEnter) {
            if (AndroidUtilities.isTablet()) {
                if (layersActionBarLayout.getVisibility() == View.VISIBLE && !layersActionBarLayout.fragmentsStack.isEmpty()) {
                    layersActionBarLayout.onKeyUp(keyCode, event);
                } else if (rightActionBarLayout.getVisibility() == View.VISIBLE && !rightActionBarLayout.fragmentsStack.isEmpty()) {
                    rightActionBarLayout.onKeyUp(keyCode, event);
                } else {
                    actionBarLayout.onKeyUp(keyCode, event);
                }
            } else {
                if (actionBarLayout.fragmentsStack.size() == 1) {
                    if (!drawerLayoutContainer.isDrawerOpened()) {
                        if (getCurrentFocus() != null) {
                            AndroidUtilities.hideKeyboard(getCurrentFocus());
                        }
                        drawerLayoutContainer.openDrawer(false);
                    } else {
                        drawerLayoutContainer.closeDrawer(false);
                    }
                } else {
                    actionBarLayout.onKeyUp(keyCode, event);
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation, ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            drawerLayoutContainer.setAllowOpenDrawer(layersActionBarLayout.getVisibility() != View.VISIBLE, true);
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity)fragment;
                if (dialogsActivity.isMainDialogList() && layout != actionBarLayout) {
                    actionBarLayout.removeAllFragments();
                    actionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, false);
                    layersActionBarLayout.removeAllFragments();
                    layersActionBarLayout.setVisibility(View.GONE);
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    if (!tabletFullSize) {
                        shadowTabletSide.setVisibility(View.VISIBLE);
                        if (rightActionBarLayout.fragmentsStack.isEmpty()) {
                            backgroundTablet.setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            }
            if (fragment instanceof ChatActivity) {
                if (!tabletFullSize && layout == rightActionBarLayout || tabletFullSize && layout == actionBarLayout) {
                    boolean result = !(tabletFullSize && layout == actionBarLayout && actionBarLayout.fragmentsStack.size() == 1);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    if (!result) {
                        actionBarLayout.presentFragment(fragment, false, forceWithoutAnimation, false);
                    }
                    return result;
                } else if (!tabletFullSize && layout != rightActionBarLayout) {
                    rightActionBarLayout.setVisibility(View.VISIBLE);
                    backgroundTablet.setVisibility(View.GONE);
                    rightActionBarLayout.removeAllFragments();
                    rightActionBarLayout.presentFragment(fragment, removeLast, true, false);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    return false;
                } else if (tabletFullSize && layout != actionBarLayout) {
                    actionBarLayout.presentFragment(fragment, actionBarLayout.fragmentsStack.size() > 1, forceWithoutAnimation, false);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    return false;
                } else {
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    actionBarLayout.presentFragment(fragment, actionBarLayout.fragmentsStack.size() > 1, forceWithoutAnimation, false);
                    return false;
                }
            } else if (layout != layersActionBarLayout) {
                layersActionBarLayout.setVisibility(View.VISIBLE);
                drawerLayoutContainer.setAllowOpenDrawer(false, true);
                    shadowTablet.setBackgroundColor(0x7F000000);
                layersActionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, false);
                return false;
            }
            return true;
        } else {
            drawerLayoutContainer.setAllowOpenDrawer(true, false);
            return true;
        }
    }

    @Override
    public boolean needAddFragmentToStack(BaseFragment fragment, ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            drawerLayoutContainer.setAllowOpenDrawer(layersActionBarLayout.getVisibility() != View.VISIBLE, true);
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity)fragment;
                if (dialogsActivity.isMainDialogList() && layout != actionBarLayout) {
                    actionBarLayout.removeAllFragments();
                    actionBarLayout.addFragmentToStack(fragment);
                    layersActionBarLayout.removeAllFragments();
                    layersActionBarLayout.setVisibility(View.GONE);
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    if (!tabletFullSize) {
                        shadowTabletSide.setVisibility(View.VISIBLE);
                        if (rightActionBarLayout.fragmentsStack.isEmpty()) {
                            backgroundTablet.setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            } else if (fragment instanceof ChatActivity) {
                if (!tabletFullSize && layout != rightActionBarLayout) {
                    rightActionBarLayout.setVisibility(View.VISIBLE);
                    backgroundTablet.setVisibility(View.GONE);
                    rightActionBarLayout.removeAllFragments();
                    rightActionBarLayout.addFragmentToStack(fragment);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(true);
                    }
                    return false;
                } else if (tabletFullSize && layout != actionBarLayout) {
                    actionBarLayout.addFragmentToStack(fragment);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(true);
                    }
                    return false;
                }
            } else if (layout != layersActionBarLayout) {
                layersActionBarLayout.setVisibility(View.VISIBLE);
                drawerLayoutContainer.setAllowOpenDrawer(false, true);
                    shadowTablet.setBackgroundColor(0x7F000000);
                layersActionBarLayout.addFragmentToStack(fragment);
                return false;
            }
            return true;
        } else {
            drawerLayoutContainer.setAllowOpenDrawer(true, false);
            return true;
        }
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            if (layout == actionBarLayout && layout.fragmentsStack.size() <= 1) {
                onFinish();
                finish();
                return false;
            } else if (layout == rightActionBarLayout) {
                if (!tabletFullSize) {
                    backgroundTablet.setVisibility(View.VISIBLE);
                }
            } else if (layout == layersActionBarLayout && actionBarLayout.fragmentsStack.isEmpty() && layersActionBarLayout.fragmentsStack.size() == 1) {
                onFinish();
                finish();
                return false;
            }
        } else {
            if (layout.fragmentsStack.size() <= 1) {
                onFinish();
                finish();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            if (layout == layersActionBarLayout) {
                rightActionBarLayout.rebuildAllFragmentViews(true);
                rightActionBarLayout.showLastFragment();
                actionBarLayout.rebuildAllFragmentViews(true);
                actionBarLayout.showLastFragment();
            }
        }
        drawerLayoutAdapter.notifyDataSetChanged();
    }
}
