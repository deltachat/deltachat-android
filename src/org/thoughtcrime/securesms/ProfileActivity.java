package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import com.b44t.messenger.DcEvent;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.File;
import java.util.ArrayList;

public class ProfileActivity extends PassphraseRequiredActionBarActivity
                             implements DcEventCenter.DcEventDelegate
{

  @SuppressWarnings("unused")
  private final static String TAG = ProfileActivity.class.getSimpleName();

  public static final String CHAT_ID_EXTRA    = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";
  public static final String FORCE_TAB_EXTRA  = "force_tab";
  public static final String FROM_CHAT        = "from_chat";

  public static final int TAB_SETTINGS = 10;
  public static final int TAB_GALLERY  = 20;
  public static final int TAB_DOCS     = 30;
  public static final int TAB_LINKS    = 40;
  public static final int TAB_MAP      = 50;

  private static final int REQUEST_CODE_PICK_RINGTONE = 1;

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ApplicationDcContext dcContext;
  private int                  chatId;
  private boolean              chatIsGroup;
  private boolean              chatIsDeviceTalk;
  private boolean              chatIsMailingList;
  private int                  contactId;
  private boolean              fromChat;

  private ArrayList<Integer> tabs = new ArrayList<>();
  private Toolbar            toolbar;
  private ConversationTitleView titleView;
  private TabLayout          tabLayout;
  private ViewPager          viewPager;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    dcContext = DcHelper.getContext(this);
  }

  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    setContentView(R.layout.profile_activity);

    initializeResources();

    setSupportActionBar(this.toolbar);
    ActionBar supportActionBar = getSupportActionBar();
    supportActionBar.setDisplayHomeAsUpEnabled(false);
    supportActionBar.setCustomView(R.layout.conversation_title_view);
    supportActionBar.setDisplayShowCustomEnabled(true);
    supportActionBar.setDisplayShowTitleEnabled(false);
    Toolbar parent = (Toolbar) supportActionBar.getCustomView().getParent();
    parent.setPadding(0,0,0,0);
    parent.setContentInsetsAbsolute(0,0);

    titleView = (ConversationTitleView) supportActionBar.getCustomView();
    titleView.setOnBackClickedListener(view -> onBackPressed());
    titleView.setOnClickListener(view -> onEnlargeAvatar());

    updateToolbar();

    this.tabLayout.setupWithViewPager(viewPager);
    this.viewPager.setAdapter(new ProfilePagerAdapter(getSupportFragmentManager()));
    int forceTab = getIntent().getIntExtra(FORCE_TAB_EXTRA, -1);
    if (forceTab != -1) {
      int forceIndex = tabs.indexOf(forceTab);
      if (forceIndex != -1) {
        this.viewPager.setCurrentItem(forceIndex);
      }
    }

    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (!isSelfProfile()) {
      getMenuInflater().inflate(R.menu.profile_common, menu);

      if (chatId != 0) {
        if (chatIsDeviceTalk) {
          menu.findItem(R.id.edit_name).setVisible(false);
          menu.findItem(R.id.show_encr_info).setVisible(false);
        } else if (chatIsGroup) {
          menu.findItem(R.id.edit_name).setTitle(R.string.menu_group_name_and_image);
        }
      } else {
        menu.findItem(R.id.menu_mute_notifications).setVisible(false);
        menu.findItem(R.id.menu_sound).setVisible(false);
        menu.findItem(R.id.menu_vibrate).setVisible(false);
      }

      if (!isContactProfile() || chatIsDeviceTalk) {
        menu.findItem(R.id.block_contact).setVisible(false);
      }
    }

    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem item = menu.findItem(R.id.block_contact);
    if(item!=null) {
      item.setTitle(dcContext.getContact(contactId).isBlocked()? R.string.menu_unblock_contact : R.string.menu_block_contact);
    }

    item = menu.findItem(R.id.menu_mute_notifications);
    if(item!=null) {
      item.setTitle(Prefs.isChatMuted(dcContext.getChat(chatId))? R.string.menu_unmute : R.string.menu_mute);
    }

    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  boolean backPressed = false;
  @Override
  public void onBackPressed() {
    backPressed = true;
    super.onBackPressed();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (backPressed && fromChat) {
      overridePendingTransition(0, 0);
    }
  }

  @Override
  public void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void handleEvent(DcEvent event) {
    updateToolbar();
  }

  private void initializeResources() {
    chatId           = getIntent().getIntExtra(CHAT_ID_EXTRA, 0);
    contactId        = getIntent().getIntExtra(CONTACT_ID_EXTRA, 0);
    chatIsGroup      = false;
    chatIsDeviceTalk = false;
    chatIsMailingList= false;
    fromChat         = getIntent().getBooleanExtra(FROM_CHAT, false);

    if (contactId!=0) {
      chatId = dcContext.getChatIdByContactId(contactId);
    }
    else if(chatId!=0) {
      DcChat dcChat = dcContext.getChat(chatId);
      chatIsGroup = dcChat.isGroup();
      chatIsDeviceTalk = dcChat.isDeviceTalk();
      chatIsMailingList = dcChat.isMailingList();
      if(!chatIsGroup) {
        final int[] members = dcContext.getChatContacts(chatId);
        contactId = members.length>=1? members[0] : 0;
      }
    }

    if(!isGlobalProfile() && !isSelfProfile() && !chatIsDeviceTalk && !chatIsMailingList) {
      tabs.add(TAB_SETTINGS);
    }
    tabs.add(TAB_GALLERY);
    tabs.add(TAB_DOCS);
    //tabs.add(TAB_LINKS);
    //if(Prefs.isLocationStreamingEnabled(this)) {
    //  tabs.add(TAB_MAP);
    //}

    this.viewPager = ViewUtil.findById(this, R.id.pager);
    this.toolbar   = ViewUtil.findById(this, R.id.toolbar);
    this.tabLayout = ViewUtil.findById(this, R.id.tab_layout);
  }

  private void updateToolbar() {
    if (isGlobalProfile()){
      getSupportActionBar().setTitle(R.string.menu_all_media);
    }
    else if (chatId > 0) {
      DcChat dcChat  = dcContext.getChat(chatId);
      titleView.setTitle(GlideApp.with(this), dcChat, false);
    }
    else if (isContactProfile()){
      titleView.setTitle(GlideApp.with(this), dcContext.getContact(contactId));
    }
  }

  private boolean isGlobalProfile() {
    return contactId==0 && chatId==0;
  }

  private boolean isContactProfile() {
    // there may still be a single-chat lined to the contact profile
    return contactId!=0 && (chatId==0 || !chatIsGroup);
  }

  private boolean isSelfProfile() {
    return isContactProfile() && contactId==DcContact.DC_CONTACT_ID_SELF;
  }

  private class ProfilePagerAdapter extends FragmentStatePagerAdapter {

    ProfilePagerAdapter(FragmentManager fragmentManager) {
      super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
      int tabId = tabs.get(position);
      Fragment fragment;
      Bundle args = new Bundle();

      switch(tabId) {
        case TAB_SETTINGS:
          fragment = new ProfileSettingsFragment();
          args.putInt(ProfileSettingsFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putInt(ProfileSettingsFragment.CONTACT_ID_EXTRA, (contactId==0&&!isGlobalProfile())? -1 : contactId);
          args.putSerializable(ProfileSettingsFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        case TAB_GALLERY:
          fragment = new ProfileGalleryFragment();
          args.putInt(ProfileGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putSerializable(ProfileGalleryFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;

        default:
          fragment = new ProfileDocumentsFragment();
          args.putInt(ProfileGalleryFragment.CHAT_ID_EXTRA, (chatId==0&&!isGlobalProfile())? -1 : chatId);
          args.putSerializable(ProfileDocumentsFragment.LOCALE_EXTRA, dynamicLanguage.getCurrentLocale());
          break;
      }

      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getCount() {
      return tabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      int tabId = tabs.get(position);
      switch(tabId) {
        case TAB_SETTINGS:
          if(isContactProfile()) {
            return getString(contactId==DcContact.DC_CONTACT_ID_SELF? R.string.self : R.string.tab_contact);
          }
          else if (chatIsMailingList) {
            return getString(R.string.mailing_list);
          } else {
            return getString(R.string.tab_group);
          }

        case TAB_GALLERY:
          return getString(R.string.tab_gallery);

        case TAB_DOCS:
          return getString(R.string.tab_docs);

        case TAB_LINKS:
          return getString(R.string.tab_links);

        case TAB_MAP:
          return getString(R.string.tab_map);

        default:
          throw new AssertionError();
      }
    }
  }


  // handle events
  // =========================================================================

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home:
        backPressed = true;
        finish();
        return true;
      case R.id.menu_mute_notifications:
        onNotifyOnOff();
        break;
      case R.id.menu_sound:
        onSoundSettings();
        break;
      case R.id.menu_vibrate:
        onVibrateSettings();
        break;
      case R.id.edit_name:
        onEditName();
        break;
      case R.id.show_encr_info:
        onEncrInfo();
        break;
      case R.id.block_contact:
        onBlockContact();
        break;
    }

    return false;
  }

  public void onNotifyOnOff() {
    if (Prefs.isChatMuted(dcContext.getChat(chatId))) {
      setMuted(0);
    }
    else {
      MuteDialog.show(this, duration -> setMuted(duration));
    }
  }

  private void setMuted(final long duration) {
    if (chatId != 0) {
      Prefs.setChatMuteDuration(dcContext, chatId, duration);
    }
  }

  public void onSoundSettings() {
    Uri current = Prefs.getChatRingtone(this, chatId);
    Uri defaultUri = Prefs.getNotificationRingtone(this);

    if      (current == null)              current = Settings.System.DEFAULT_NOTIFICATION_URI;
    else if (current.toString().isEmpty()) current = null;

    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);

    startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
  }

  public void onVibrateSettings() {
    int checkedItem = Prefs.getChatVibrate(this, chatId).getId();
    int[] selectedChoice = new int[]{checkedItem};
    new AlertDialog.Builder(this)
            .setTitle(R.string.pref_vibrate)
            .setSingleChoiceItems(R.array.recipient_vibrate_entries, checkedItem,
                    (dialog, which) -> selectedChoice[0] = which)
            .setPositiveButton(R.string.ok,
                    (dialog, which) -> Prefs.setChatVibrate(this, chatId, Prefs.VibrateState.fromId(selectedChoice[0])))
            .setNegativeButton(R.string.cancel, null)
            .show();
  }

  public void onEnlargeAvatar() {
    String profileImagePath;
    String title;
    Uri profileImageUri;
    if(chatId!=0) {
      DcChat dcChat = dcContext.getChat(chatId);
      profileImagePath = dcChat.getProfileImage();
      title = dcChat.getName();
    } else {
      DcContact dcContact = dcContext.getContact(contactId);
      profileImagePath = dcContact.getProfileImage();
      title = dcContact.getDisplayName();
    }

    File file = new File(profileImagePath);
    if (!file.exists()) return;

    profileImageUri = Uri.fromFile(file);
    String type = "image/" + profileImagePath.substring(profileImagePath.lastIndexOf(".") +1);

    Intent intent = new Intent(this, MediaPreviewActivity.class);
    intent.setDataAndType(profileImageUri, type);
    intent.putExtra(MediaPreviewActivity.ACTIVITY_TITLE_EXTRA, title);
    intent.putExtra(MediaPreviewActivity.EDIT_AVATAR_CHAT_ID, chatIsGroup ? chatId : 0); // shows edit-button, might be 0 for a contact-profile
    startActivity(intent);
  }

  public void onEditName() {
    if (chatIsGroup) {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      intent.putExtra(GroupCreateActivity.EDIT_GROUP_CHAT_ID, chatId);
      if (dcContext.getChat(chatId).isProtected()) {
        intent.putExtra(GroupCreateActivity.GROUP_CREATE_VERIFIED_EXTRA, true);
      }
      startActivity(intent);
    }
    else {
      DcContact dcContact = dcContext.getContact(contactId);
      View gl = View.inflate(this, R.layout.single_line_input, null);
      EditText inputField = gl.findViewById(R.id.input_field);
      inputField.setText(dcContact.getName());
      inputField.setSelection(inputField.getText().length());
      new AlertDialog.Builder(this)
          .setTitle(R.string.menu_edit_name)
          .setView(gl)
          .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
            String newName = inputField.getText().toString();
            dcContext.createContact(newName, dcContact.getAddr());
          })
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  public void onEncrInfo() {
    String info_str = isContactProfile() ?
      dcContext.getContactEncrInfo(contactId) : dcContext.getChatEncrInfo(chatId);
    new AlertDialog.Builder(this)
        .setMessage(info_str)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  public void onBlockContact() {
    DcContact dcContact = dcContext.getContact(contactId);
    if(dcContact.isBlocked()) {
      new AlertDialog.Builder(this)
          .setMessage(R.string.ask_unblock_contact)
          .setCancelable(true)
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton(R.string.menu_unblock_contact, (dialog, which) -> {
            dcContext.blockContact(contactId, 0);
          }).show();
    }
    else {
      new AlertDialog.Builder(this)
          .setMessage(R.string.ask_block_contact)
          .setCancelable(true)
          .setNegativeButton(android.R.string.cancel, null)
          .setPositiveButton(R.string.menu_block_contact, (dialog, which) -> {
            dcContext.blockContact(contactId, 1);
          }).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode==REQUEST_CODE_PICK_RINGTONE && resultCode== Activity.RESULT_OK && data!=null) {
      Uri value = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
      Uri defaultValue = Prefs.getNotificationRingtone(this);

      if (defaultValue.equals(value)) value = null;
      else if (value == null)         value = Uri.EMPTY;

      Prefs.setChatRingtone(this, chatId, value);
    }
  }

}
