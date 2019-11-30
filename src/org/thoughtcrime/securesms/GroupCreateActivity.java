package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.qr.QrShowActivity;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.SelectedRecipientsAdapter;
import org.thoughtcrime.securesms.util.SelectedRecipientsAdapter.OnRecipientDeletedListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;
import org.thoughtcrime.securesms.util.guava.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_SELF;

public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
                                 implements OnRecipientDeletedListener, DcEventCenter.DcEventDelegate
{

  public static final String EDIT_GROUP_CHAT_ID = "edit_group_chat_id";
  public static final String GROUP_CREATE_VERIFIED_EXTRA  = "group_create_verified";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT = 1;
  public static final  int AVATAR_SIZE  = 210;

  private ApplicationDcContext dcContext;

  private boolean verified;
  private EditText     groupName;
  private ListView     lv;
  private ImageView    avatar;
  private Bitmap       avatarBmp;
  private int          groupChatId;
  private boolean      isEdit;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    dcContext = DcHelper.getContext(this);
    setContentView(R.layout.group_create_activity);
    //noinspection ConstantConditions
    verified = getIntent().getBooleanExtra(GROUP_CREATE_VERIFIED_EXTRA, false);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    groupChatId = getIntent().getIntExtra(EDIT_GROUP_CHAT_ID, 0);

    // groupChatId may be set during creation,
    // so always check isEdit()
    if(groupChatId !=0) {
      isEdit = true;
    }

    initializeExistingGroup();
    initializeResources();

    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    dcContext.eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    updateViewState();
  }

  @Override
  protected void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @SuppressWarnings("ConstantConditions")
  private void updateViewState() {
    avatar.setEnabled(true);
    groupName.setEnabled(true);

    String title;
    if(isEdit()) {
      title = getString(R.string.menu_edit_group);
    }
    else if(verified) {
      title = getString(R.string.menu_new_verified_group);
    }
    else {
      title = getString(R.string.menu_new_group);
    }
    getSupportActionBar().setTitle(title);
  }

  private void addSelectedContacts(@NonNull Recipient... recipients) {
    getAdapter().clear();
    new AddMembersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipients);
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    if (eventId== DcContext.DC_EVENT_CHAT_MODIFIED || eventId==DcContext.DC_EVENT_CONTACTS_CHANGED) {
      initializeExistingGroup();
    }
  }

  private static class AddMembersTask extends AsyncTask<Recipient,Void,List<AddMembersTask.Result>> {
    static class Result {
      Optional<Recipient> recipient;
      String              reason;

      public Result(@Nullable Recipient recipient, @Nullable String reason) {
        this.recipient = Optional.fromNullable(recipient);
        this.reason    = reason;
      }
    }

    private GroupCreateActivity activity;

    public AddMembersTask(@NonNull GroupCreateActivity activity) {
      this.activity      = activity;
    }

    @Override
    protected List<Result> doInBackground(Recipient... recipients) {
      final List<Result> results = new LinkedList<>();

      for (Recipient recipient : recipients) {
        results.add(new Result(recipient, null));
      }
      return results;
    }

    @Override
    protected void onPostExecute(List<Result> results) {
      if (activity.isFinishing()) return;
      for (Result result : results) {
        Recipient recipient = result.recipient.get();
        Address address = recipient.getAddress();
        if(address.isDcContact()) {
          activity.getAdapter().add(recipient, true);
        }
      }
      activity.updateViewState();
    }
  }

  private void initializeResources() {
    lv           = ViewUtil.findById(this, R.id.selected_contacts_list);
    avatar       = ViewUtil.findById(this, R.id.avatar);
    groupName    = ViewUtil.findById(this, R.id.group_name);
    List<Recipient> initList = new LinkedList<>();
    DcContact self = dcContext.getContact(DC_CONTACT_ID_SELF);
    initList.add(dcContext.getRecipient(self));
    SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this, initList);
    adapter.setOnRecipientDeletedListener(this);
    lv.setAdapter(adapter);

    findViewById(R.id.add_member_button).setOnClickListener(new AddRecipientButtonListener());
    ViewUtil.findById(this, R.id.verify_button).setOnClickListener(new ShowQrButtonListener());
    initializeAvatarView();

    if(isEdit()) {
      lv.setVisibility(View.GONE);
      findViewById(R.id.add_member_button).setVisibility(View.GONE);
      findViewById( R.id.verify_button).setVisibility(View.GONE);
    }
  }

  private void initializeAvatarView() {
    boolean imageLoaded = false;
    if (groupChatId != 0) {
      String avatarPath = dcContext.getChat(groupChatId).getProfileImage();
      File avatarFile = new File(avatarPath);
      if (avatarFile.exists()) {
        imageLoaded = true;
        GlideApp.with(this)
                .load(avatarFile)
                .circleCrop()
                .into(avatar);
      }
    }
    if (!imageLoaded) {
      avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_group_white_24dp).asDrawable(this, ThemeUtil.getDummyContactColor(this)));
    }
    avatar.setOnClickListener(view -> Crop.pickImage(GroupCreateActivity.this));
  }

  private void initializeExistingGroup() {
    if (groupChatId != 0) {
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      new FillExistingGroupInfoAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, groupChatId);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.group_create, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.menu_create_group:
        if (groupChatId!=0) {
          groupUpdateInDb();
        }
        else {
          groupCreateInDb();
        }

        if (groupChatId==0) // Group still hasn't been created e.g. due to empty name
          return true;

        if(isEdit()) {
          groupUpdateDone();
        }
        else {
          groupCreateDone();
        }

        return true;
    }

    return false;
  }

  @Override
  public void onRecipientDeleted(Recipient recipient) {
    getAdapter().remove(recipient);
    updateViewState();
  }

  private void groupCreateInDb() {
    String groupName = getGroupName();
    if (showGroupNameEmptyToast(groupName)) return;

    groupChatId = dcContext.createGroupChat(verified, groupName);

    Set<Recipient> recipients = getAdapter().getRecipients();
    for (Recipient recipient : recipients) {
      Address address = recipient.getAddress();
      if (address.isDcContact()) {
        int contactId = address.getDcContactId();
        dcContext.addContactToChat(groupChatId, contactId);
      }
    }
    AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);
  }

  private void groupCreateDone() {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, groupChatId);
    startActivity(intent);
    finish();
  }

  private boolean showGroupNameEmptyToast(String groupName) {
    if(groupName == null) {
      Toast.makeText(this, getString(R.string.group_please_enter_group_name), Toast.LENGTH_LONG).show();
      return true;
    }
    return false;
  }

  private void groupUpdateInDb() {
    if (groupChatId == 0) {
      return;
    }
    String groupName = getGroupName();
    if (showGroupNameEmptyToast(groupName)) {
      return;
    }
    dcContext.setChatName(groupChatId, groupName);
    updateGroupParticipants();

    AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);
  }

  private void groupUpdateDone() {
    Intent intent = new Intent();
    intent.putExtra(GroupCreateActivity.EDIT_GROUP_CHAT_ID, groupChatId);
    setResult(RESULT_OK, intent);
    finish();
  }

  private void updateGroupParticipants() {
    SparseBooleanArray currentChatContactIds = new SparseBooleanArray();
    for(int chatContactId : dcContext.getChatContacts(groupChatId)) {
      currentChatContactIds.put(chatContactId, false);
    }

    Set<Recipient> recipients = getAdapter().getRecipients();
    for(Recipient recipient : recipients) {
      Address address = recipient.getAddress();
      if(address.isDcContact()) {
        int contactId = address.getDcContactId();
        if(currentChatContactIds.indexOfKey(contactId) < 0) {
          dcContext.addContactToChat(groupChatId, contactId);
        } else {
          currentChatContactIds.put(contactId, true);
        }
      }
    }
    for(int index = 0; index < currentChatContactIds.size(); index++) {
      if (!currentChatContactIds.valueAt(index)) {
        dcContext.removeContactFromChat(groupChatId, currentChatContactIds.keyAt(index));
      }
    }
  }

  private SelectedRecipientsAdapter getAdapter() {
    return (SelectedRecipientsAdapter)lv.getAdapter();
  }

  private @Nullable String getGroupName() {
    String ret = groupName.getText() != null ? groupName.getText().toString() : null;
    if(ret!=null) {
      ret = ret.trim();
      if(ret.isEmpty()) {
        ret = null;
      }
    }
    return ret;
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));

    if (data == null || resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case PICK_CONTACT:
        List<String> selected = data.getStringArrayListExtra("contacts");

        for (String contact : selected) {
          if(contact!=null) {
            Address address = Address.fromSerialized(contact);
            Recipient recipient = Recipient.from(this, address);
            addSelectedContacts(recipient);
          }
        }
        break;

      case Crop.REQUEST_PICK:
        new Crop(data.getData()).output(outputFile).asSquare().start(this);
        break;
      case Crop.REQUEST_CROP:
        setAvatarView(data);
    }
  }

  private void setAvatarView(Intent data) {
    final Uri output = Crop.getOutput(data);
    GlideApp.with(this)
            .asBitmap()
            .load(output)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
            .override(AVATAR_SIZE, AVATAR_SIZE)
            .into(new SimpleTarget<Bitmap>() {
              @Override
              public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                setAvatar(output, resource);
              }
            });
  }

  private class AddRecipientButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(GroupCreateActivity.this, ContactMultiSelectionActivity.class);
      intent.putExtra(ContactSelectionListFragment.SELECT_VERIFIED_EXTRA, verified);
      ArrayList<String> preselectedContacts = new ArrayList<>();
      Set<Recipient> recipients = GroupCreateActivity.this.getAdapter().getRecipients();
      for (Recipient r : recipients) {
        if(r.getAddress().isDcContact()) {
          preselectedContacts.add(dcContext.getContact(r.getAddress().getDcContactId()).getAddr());
        }
      }
      intent.putExtra(ContactSelectionListFragment.PRESELECTED_CONTACTS, preselectedContacts);
      startActivityForResult(intent, PICK_CONTACT);
    }
  }

  private class ShowQrButtonListener implements View.OnClickListener {

    @Override
    public void onClick(View view) {
        if(groupChatId==0) {
          groupCreateInDb();
        }
        else {
          groupUpdateInDb();
        }

        if(groupChatId==0) {
          return;
        }
        Intent qrIntent = new Intent(GroupCreateActivity.this, QrShowActivity.class);
        qrIntent.putExtra(QrShowActivity.CHAT_ID, groupChatId);
        startActivity(qrIntent);
        initializeExistingGroup(); // To reread the recipients from the newly created group.
    }
  }

  private static class FillExistingGroupInfoAsyncTask extends ProgressDialogAsyncTask<Integer,Void,Recipient> {

    GroupCreateActivity activity;

    FillExistingGroupInfoAsyncTask(GroupCreateActivity activity) {
      super(activity,
            R.string.one_moment,
            R.string.one_moment);
      this.activity = activity;
    }

    @Override
    protected Recipient doInBackground(Integer... recipientIds) {
      Integer recipientsId = recipientIds[0];
      return activity.dcContext.getRecipient(ApplicationDcContext.RECIPIENT_TYPE_CHAT, recipientsId);
    }

    @Override
    protected void onPostExecute(Recipient recipient) {
      super.onPostExecute(recipient);
      activity.fillExistingGroup(recipient);
    }
  }

  private void fillExistingGroup(Recipient recipient) {
    List<Recipient> participants = recipient.loadParticipants(this);
    Recipient[] participantsArray = new Recipient[participants.size()];
    participantsArray = participants.toArray(participantsArray);
    if (!isFinishing()) {
      addSelectedContacts(participantsArray);
      groupName.setText(recipient.getName());
      if (isEdit() && recipient.getName()!=null) {
        groupName.setSelection(recipient.getName().length(), recipient.getName().length());
      }
      SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this, participants);
      adapter.setOnRecipientDeletedListener(this);
      lv.setAdapter(adapter);
      updateViewState();
    }
  }

  private <T> void setAvatar(T model, Bitmap bitmap) {
    avatarBmp = bitmap;
    GlideApp.with(this)
            .load(model)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(avatar);
  }

  private boolean isEdit() {
    return isEdit;
  }

}
