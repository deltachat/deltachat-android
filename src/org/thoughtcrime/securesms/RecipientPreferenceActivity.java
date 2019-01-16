package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.preferences.CorrectedPreferenceFragment;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Prefs.VibrateState;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

@SuppressLint("StaticFieldLeak")
public class RecipientPreferenceActivity extends PassphraseRequiredActionBarActivity implements DcEventCenter.DcEventDelegate
{
  private static final String TAG = RecipientPreferenceActivity.class.getSimpleName();

  public static final String ADDRESS_EXTRA                = "recipient_address";

  private static final String PREFERENCE_MUTED           = "pref_key_recipient_mute";
  private static final String PREFERENCE_MESSAGE_TONE    = "pref_key_recipient_ringtone";
  private static final String PREFERENCE_MESSAGE_VIBRATE = "pref_key_recipient_vibrate";
  private static final String PREFERENCE_BLOCK           = "pref_key_recipient_block";
  private static final String PREFERENCE_ENCRYPTION      = "pref_key_recipient_encryption_info";

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ImageView               avatar;
  private GlideRequests           glideRequests;
  private Address                 address;
  private CollapsingToolbarLayout toolbarLayout;

  private ApplicationDcContext dcContext;

  private static final int REQUEST_CODE_PICK_RINGTONE = 1;

  @Override
  public void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  public void onCreate(Bundle instanceState, boolean ready) {
    dcContext = DcHelper.getContext(this);

    setContentView(R.layout.recipient_preference_activity);
    this.glideRequests = GlideApp.with(this);
    this.address       = getIntent().getParcelableExtra(ADDRESS_EXTRA);

    Recipient recipient = Recipient.from(this, address);

    initializeToolbar();
    setHeader(recipient);

    Bundle bundle = new Bundle();
    bundle.putParcelable(ADDRESS_EXTRA, address);
    initFragment(R.id.preference_fragment, new RecipientPreferenceFragment(), null, bundle);

    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
  }

  @Override
  protected void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.preference_fragment);
    fragment.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    if(eventId==DcContext.DC_EVENT_CONTACTS_CHANGED) {
      Recipient recipient = Recipient.from(this, address);
      setHeader(recipient);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    finish();
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
  }

  private void initializeToolbar() {
    this.toolbarLayout        = ViewUtil.findById(this, R.id.collapsing_toolbar);
    this.avatar               = ViewUtil.findById(this, R.id.avatar);

    this.toolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.white));
    this.toolbarLayout.setCollapsedTitleTextColor(getResources().getColor(R.color.white));

    Toolbar toolbar = ViewUtil.findById(this, R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setLogo(null);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
  }

  private void setHeader(@NonNull Recipient recipient) {
    glideRequests.load(recipient.getContactPhoto(this))
                 .fallback(recipient.getFallbackContactPhoto().asCallCard(this))
                 .error(recipient.getFallbackContactPhoto().asCallCard(this))
                 .diskCacheStrategy(DiskCacheStrategy.ALL)
                 .into(this.avatar);

    if (recipient.getContactPhoto(this) == null) this.avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    else                                     this.avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

    this.avatar.setBackgroundColor(recipient.getFallbackAvatarColor(this));
    this.toolbarLayout.setTitle(recipient.toShortString());
    this.toolbarLayout.setContentScrimColor(recipient.getFallbackAvatarColor(this));
  }


  // the fragment
  //////////////////////////////////////////////////////////////////////////////////////////////////

  public static class RecipientPreferenceFragment
      extends    CorrectedPreferenceFragment
      implements DcEventCenter.DcEventDelegate
  {
    private ApplicationDcContext dcContext;
    private Recipient profileRecipient;    // the recipient the profile was opened for, may be a chat or a contact
    private DcChat    chatToEdit;          // may be an invalid chat that returns 0 as id
    private Recipient chatToEditRecipient;
    private Recipient contactToEditRecipient;

    @Override
    public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      dcContext = DcHelper.getContext(getActivity());
      profileRecipient = Recipient.from(getActivity(), getArguments().getParcelable(ADDRESS_EXTRA));


      // set the recipient to edit as the chat.
      // when the profile shows a contact, this is _not_ the contact but the chat with the contact (if exists)
      int chatToEditId = profileRecipient.getAddress().isDcChat()? profileRecipient.getAddress().getDcChatId() : 0;
      if(chatToEditId==0 && profileRecipient.getAddress().isDcContact()) {
        chatToEditId = dcContext.getChatIdByContactId(profileRecipient.getAddress().getDcContactId());
      }
      chatToEdit = dcContext.getChat(chatToEditId);
      chatToEditRecipient = Recipient.from(getActivity(), Address.fromChat(chatToEditId));

      // set the recipent really to edit as the contact
      int contactToEditId = profileRecipient.getAddress().isDcContact()? profileRecipient.getAddress().getDcContactId() : 0;
      if(contactToEditId==0 && !chatToEdit.isGroup() ) {
        int members[] = dcContext.getChatContacts(chatToEdit.getId());
        if(members.length>=1) {
          contactToEditId = members[0];
        }
      }
      contactToEditRecipient = Recipient.from(getActivity(), Address.fromContact(contactToEditId));


      this.findPreference(PREFERENCE_MESSAGE_TONE)
          .setOnPreferenceChangeListener(new RingtoneChangeListener());
      this.findPreference(PREFERENCE_MESSAGE_TONE)
          .setOnPreferenceClickListener(new RingtoneClickedListener());
      this.findPreference(PREFERENCE_MESSAGE_VIBRATE)
          .setOnPreferenceChangeListener(new VibrateChangeListener());
      this.findPreference(PREFERENCE_MUTED)
          .setOnPreferenceClickListener(new MuteClickedListener());
      this.findPreference(PREFERENCE_BLOCK)
          .setOnPreferenceClickListener(new BlockClickedListener());

      dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
      Log.w(TAG, "onCreatePreferences...");
      addPreferencesFromResource(R.xml.recipient_preferences);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
      Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
      super.onResume();
      setSummaries();
    }

    @Override
    public void onDestroy() {
      dcContext.eventCenter.removeObservers(this);
      super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_CODE_PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        findPreference(PREFERENCE_MESSAGE_TONE).getOnPreferenceChangeListener().onPreferenceChange(findPreference(PREFERENCE_MESSAGE_TONE), uri);
      }
    }

    private void setSummaries() {

      // chat settings
      PreferenceCategory    notificationCategory      = (PreferenceCategory)this.findPreference("notification_settings");
      CheckBoxPreference    mutePreference            = (CheckBoxPreference) this.findPreference(PREFERENCE_MUTED);
      Preference            ringtoneMessagePreference = this.findPreference(PREFERENCE_MESSAGE_TONE);
      ListPreference        vibrateMessagePreference  = (ListPreference) this.findPreference(PREFERENCE_MESSAGE_VIBRATE);

      // contact settings
      PreferenceCategory    contactInfoCategory       = (PreferenceCategory) this.findPreference("contact_info");
      PreferenceCategory    contactInfoDivider        = (PreferenceCategory) this.findPreference("contact_divider");
      Preference            addrPreference            = this.findPreference("pref_key_recipient_addr");
      Preference            encryptionPreference      = this.findPreference(PREFERENCE_ENCRYPTION);
      Preference            editNamePreference        = this.findPreference("pref_key_recipient_edit_name");
      Preference            blockPreference           = this.findPreference(PREFERENCE_BLOCK);

      editNamePreference.setOnPreferenceClickListener(new EditContactNameListener());
      encryptionPreference.setOnPreferenceClickListener(new ShowEncrInfoListener());

      mutePreference.setChecked(Prefs.isChatMuted(getContext(), chatToEdit.getId()));

      ringtoneMessagePreference.setSummary(getRingtoneSummary(getContext(), chatToEditRecipient.getMessageRingtone()));

      VibrateState vibrateState = Prefs.getChatVibrate(getContext(), chatToEdit.getId());
      Pair<String, Integer> vibrateMessageSummary = getVibrateSummary(getContext(), vibrateState);

      vibrateMessagePreference.setSummary(vibrateMessageSummary.first);
      vibrateMessagePreference.setValueIndex(vibrateMessageSummary.second);

      if (chatToEdit.getId()!=0 && chatToEdit.isGroup()) {
        // group
        if (contactInfoCategory != null) contactInfoCategory.setVisible(false);
        if (addrPreference       != null) addrPreference.setVisible(false);
        if (encryptionPreference != null) encryptionPreference.setVisible(false);
        if (editNamePreference   != null) editNamePreference.setVisible(false); // group name is currently somewhere else ...
        if (blockPreference      != null) blockPreference.setVisible(false);

        if (contactInfoDivider != null) contactInfoDivider.setVisible(false);
      }
      else {
        // contact view
        DcContact contact = getProfileContact();
        String address = contact.getAddr();
        addrPreference.setTitle(address);
        addrPreference.setOnPreferenceClickListener(preference -> {
          new AlertDialog.Builder(getContext())
              .setTitle(address)
              .setItems(new CharSequence[]{
                getContext().getString(R.string.menu_copy_to_clipboard)
              },
              (dialogInterface, i) -> {
                Util.writeTextToClipboard(getContext(), address);
                Toast.makeText(getContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
              })
              .setNegativeButton(R.string.cancel, null)
              .show();
          return false;
        });

        this.findPreference("pref_key_new_chat").setOnPreferenceClickListener(preference -> {
          new AlertDialog.Builder(getActivity())
              .setMessage(getActivity().getString(R.string.ask_start_chat_with, contact.getNameNAddr()))
              .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                int chatId = dcContext.createChatByContactId(contact.getId());
                if (chatId != 0) {
                  Intent intent = new Intent(getActivity(), ConversationActivity.class);
                  intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, chatId);
                  getActivity().startActivity(intent);
                  getActivity().finish();
                }
              })
              .setNegativeButton(R.string.cancel, null)
              .show();
          return false;
        });

        if (contactToEditRecipient.isBlocked()) blockPreference.setTitle(R.string.menu_unblock_contact);
        else                                    blockPreference.setTitle(R.string.menu_block_contact);

        if(chatToEdit.getId()==0) {
          mutePreference.setVisible(false);
          ringtoneMessagePreference.setVisible(false);
          vibrateMessagePreference.setVisible(false);
          notificationCategory.setVisible(false);
        }
      }
    }

    private DcContact getProfileContact() {
      if(profileRecipient.getAddress().isDcContact()) {
        return dcContext.getContact(profileRecipient.getAddress().getDcContactId());
      }
      else if(profileRecipient.getAddress().isDcChat()) {
        int members[] = dcContext.getChatContacts(profileRecipient.getAddress().getDcChatId());
        if(members.length>=1) {
          return dcContext.getContact(members[0]);
        }
      }
      return dcContext.getContact(0);
    }

    private @NonNull String getRingtoneSummary(@NonNull Context context, @Nullable Uri ringtone) {
      if (ringtone == null) {
        return context.getString(R.string.def);
      } else if (ringtone.toString().isEmpty()) {
        return context.getString(R.string.pref_silent);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), ringtone);

        if (tone != null) {
          return tone.getTitle(context);
        }
      }

      return context.getString(R.string.def);
    }

    private @NonNull Pair<String, Integer> getVibrateSummary(@NonNull Context context, @NonNull VibrateState vibrateState) {
      if (vibrateState == VibrateState.DEFAULT) {
        return new Pair<>(context.getString(R.string.def), 0);
      } else if (vibrateState == VibrateState.ENABLED) {
        return new Pair<>(context.getString(R.string.on), 1);
      } else {
        return new Pair<>(context.getString(R.string.off), 2);
      }
    }

    @Override
    public void handleEvent(int eventId, Object data1, Object data2) {
      if(eventId==DcContext.DC_EVENT_CONTACTS_CHANGED) {
        setSummaries();
      }
    }

    private class RingtoneChangeListener implements Preference.OnPreferenceChangeListener {

      RingtoneChangeListener() {
      }

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        Uri value = (Uri)newValue;

        Uri defaultValue = Prefs.getNotificationRingtone(getContext());

        if (defaultValue.equals(value)) value = null;
        else if (value == null)         value = Uri.EMPTY;

        Prefs.setChatRingtone(getContext(), chatToEdit.getId(), value);

        return false;
      }
    }

    private class RingtoneClickedListener implements Preference.OnPreferenceClickListener {

      RingtoneClickedListener() {
      }

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Uri current = chatToEditRecipient.getMessageRingtone();
        Uri defaultUri = Prefs.getNotificationRingtone(getContext());

        if      (current == null)              current = Settings.System.DEFAULT_NOTIFICATION_URI;
        else if (current.toString().isEmpty()) current = null;

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);

        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);

        return true;
      }
    }

    private class VibrateChangeListener implements Preference.OnPreferenceChangeListener {

      VibrateChangeListener() {
      }

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
              int          value        = Integer.parseInt((String) newValue);
        final VibrateState vibrateState = VibrateState.fromId(value);

        Prefs.setChatVibrate(getContext(), chatToEdit.getId(), vibrateState);

        return false;
      }
    }

    private class MuteClickedListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (Prefs.isChatMuted(getContext(), chatToEdit.getId())) {
          handleUnmute();
        }
        else {
          handleMute();
        }

        return true;
      }

      private void handleMute() {
        MuteDialog.show(getActivity(), until -> setMuted(until));
      }

      private void handleUnmute() {
        setMuted(0);
      }

      private void setMuted(final long until) {
        if(chatToEditRecipient.getAddress().isDcChat()) {
          Prefs.setChatMutedUntil(getActivity(), chatToEditRecipient.getAddress().getDcChatId(), until);
          setSummaries();
        }
      }
    }

    private class ShowEncrInfoListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        String info_str = dcContext.getContactEncrInfo(contactToEditRecipient.getAddress().getDcContactId());
        new AlertDialog.Builder(getActivity())
            .setMessage(info_str)
            .setPositiveButton(android.R.string.ok, null)
            .show();
        return true;
      }
    }

    private class EditContactNameListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        final DcContact dcContact = dcContext.getContact(contactToEditRecipient.getAddress().getDcContactId());
        final EditText txt = new EditText(getActivity());
        txt.setText(dcContact.getName());
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.menu_edit_name)
            .setView(txt)
            .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
              String newName = txt.getText().toString();
              dcContext.createContact(newName, dcContact.getAddr());
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        return true;
      }
    }

    private class BlockClickedListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (contactToEditRecipient.isBlocked()) handleUnblock();
        else                                    handleBlock();

        return true;
      }

      private void handleBlock() {
        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.ask_block_contact)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.menu_block_contact, (dialog, which) -> setBlocked(true)).show();
      }

      private void handleUnblock() {
        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.ask_unblock_contact)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.menu_unblock_contact, (dialog, which) -> setBlocked(false)).show();
      }

      private void setBlocked(final boolean blocked) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            dcContext.blockContact(contactToEditRecipient.getAddress().getDcContactId(), blocked ? 1 : 0);
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    }
  }
}
