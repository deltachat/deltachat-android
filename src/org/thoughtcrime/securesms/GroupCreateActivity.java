package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.mms.AttachmentManager;
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
import org.thoughtcrime.securesms.util.guava.Optional;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;

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
  public static final String CREATE_BROADCAST  = "group_create_broadcast";
  public static final String SUGGESTED_CONTACT_IDS = "suggested_contact_ids";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT = 1;
  public static final  int AVATAR_SIZE  = 210;
  private static final int REQUEST_CODE_AVATAR = 2759;

  private DcContext dcContext;

  private boolean verified;
  private boolean      broadcast;
  private EditText     groupName;
  private ListView     lv;
  private ImageView    avatar;
  private Bitmap       avatarBmp;
  private int          groupChatId;
  private boolean      isEdit;
  private boolean      avatarChanged;
  private boolean      imageLoaded;
  private boolean      hasSuggestedContacts;
  private AttachmentManager attachmentManager;

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
    broadcast = getIntent().getBooleanExtra(CREATE_BROADCAST, false);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    groupChatId = getIntent().getIntExtra(EDIT_GROUP_CHAT_ID, 0);
    attachmentManager = new AttachmentManager(this, () -> {});
    avatarChanged = false;

    // groupChatId may be set during creation,
    // so always check isEdit()
    if(groupChatId !=0) {
      isEdit = true;
      DcChat dcChat = dcContext.getChat(groupChatId);
      verified = dcChat.isProtected();
      broadcast = dcChat.isBroadcast();
    }

    initializeExistingGroup();
    initializeResources();

    DcEventCenter eventCenter = DcHelper.getEventCenter(this);
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
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
    DcHelper.getEventCenter(this).removeObservers(this);
    super.onDestroy();
  }

  @SuppressWarnings("ConstantConditions")
  private void updateViewState() {
    avatar.setEnabled(true);
    groupName.setEnabled(true);

    String title;
    if(isEdit()) {
      title = getString(R.string.global_menu_edit_desktop);
    }
    else if(broadcast) {
      title = getString(R.string.new_broadcast_list);
    } else if (hasSuggestedContacts) {
      title = getString(R.string.new_group_or_subject);
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
  public void handleEvent(@NonNull DcEvent event) {
    int eventId = event.getId();
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
          activity.getAdapter().add(recipient);
        }
      }
      activity.updateViewState();
    }
  }

  private void initializeResources() {
    lv                  = ViewUtil.findById(this, R.id.selected_contacts_list);
    avatar              = ViewUtil.findById(this, R.id.avatar);
    groupName           = ViewUtil.findById(this, R.id.group_name);
    TextView groupHints = ViewUtil.findById(this, R.id.group_hints);

    List<Recipient> initList = new LinkedList<>();
    if (!broadcast) {
      DcContact self = dcContext.getContact(DC_CONTACT_ID_SELF);
      initList.add(new Recipient(this, self));
    }
    ArrayList<Integer> suggestedContactIds = getIntent().getIntegerArrayListExtra(SUGGESTED_CONTACT_IDS);
    if (suggestedContactIds != null && !suggestedContactIds.isEmpty()) {
      hasSuggestedContacts = true;
      for (Integer contactId : suggestedContactIds) {
        initList.add(new Recipient(this, dcContext.getContact(contactId)));
      }
    }
    SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this, GlideApp.with(this), initList);
    adapter.setOnRecipientDeletedListener(this);
    lv.setAdapter(adapter);

    findViewById(R.id.add_member_button).setOnClickListener(new AddRecipientButtonListener());
    ViewUtil.findById(this, R.id.verify_button).setOnClickListener(new ShowQrButtonListener());
    initializeAvatarView();

    if (broadcast) {
      ViewUtil.findById(this, R.id.group_image_holder).setVisibility(isEdit()? View.VISIBLE : View.GONE);
      avatar.setVisibility(View.GONE);
      groupName.setVisibility(isEdit()? View.VISIBLE : View.GONE);
      groupName.setHint(R.string.name_desktop);
      groupHints.setText(R.string.chat_new_broadcast_hint);
      groupHints.setVisibility(isEdit()? View.GONE : View.VISIBLE);
      ViewUtil.findById(this, R.id.verify_button).setVisibility(View.INVISIBLE);
      ((TextView)ViewUtil.findById(this, R.id.add_member_button)).setText(R.string.add_recipients);
    } else if (!verified) {
      groupHints.setVisibility(View.GONE);
    }

    if(isEdit()) {
      lv.setVisibility(View.GONE);
      findViewById(R.id.add_member_button).setVisibility(View.GONE);
      findViewById( R.id.verify_button).setVisibility(View.GONE);
    }
  }

  private void initializeAvatarView() {
    imageLoaded = false;
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
    avatar.setOnClickListener(view ->
            new AvatarSelector(this, LoaderManager.getInstance(this), new AvatarSelectedListener(), imageLoaded)
                    .show(this, avatar)
    );
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
        attachmentManager.cleanup();

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
    if (broadcast) {
      groupChatId = dcContext.createBroadcastList();
    } else {
      String groupName = getGroupName();
      if (showGroupNameEmptyToast(groupName)) return;
      groupChatId = dcContext.createGroupChat(verified, groupName);
    }

    Set<Recipient> recipients = getAdapter().getRecipients();
    for (Recipient recipient : recipients) {
      Address address = recipient.getAddress();
      if (address.isDcContact()) {
        int contactId = address.getDcContactId();
        dcContext.addContactToChat(groupChatId, contactId);
      }
    }
    if (avatarBmp!=null) {
      AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);
    }
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

    if (avatarChanged) AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);
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

    if (resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case REQUEST_CODE_AVATAR:
        Uri inputFile  = (data != null ? data.getData() : null);
        onFileSelected(inputFile);
        break;

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
      return new Recipient(activity, activity.dcContext.getChat(recipientsId));
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
      SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this, GlideApp.with(this), participants);
      adapter.setOnRecipientDeletedListener(this);
      lv.setAdapter(adapter);
      updateViewState();
    }
  }

  private <T> void setAvatar(T model, Bitmap bitmap) {
    avatarBmp = bitmap;
    avatarChanged = true;
    imageLoaded = true;
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


  private class AvatarSelectedListener implements AvatarSelector.AttachmentClickedListener {
    @Override
    public void onClick(int type) {
      switch (type) {
        case AvatarSelector.ADD_GALLERY:
          AttachmentManager.selectImage(GroupCreateActivity.this, REQUEST_CODE_AVATAR);
          break;
        case AvatarSelector.REMOVE_PHOTO:
          avatarBmp = null;
          imageLoaded = false;
          avatarChanged = true;
          avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_group_white_24dp).asDrawable(GroupCreateActivity.this, ThemeUtil.getDummyContactColor(GroupCreateActivity.this)));
          break;
        case AvatarSelector.TAKE_PHOTO:
          attachmentManager.capturePhoto(GroupCreateActivity.this, REQUEST_CODE_AVATAR);
          break;
      }
    }

    @Override
    public void onQuickAttachment(Uri inputFile) {
      onFileSelected(inputFile);
    }
  }

  private void onFileSelected(Uri inputFile) {
    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));
    if (inputFile == null) {
      inputFile = attachmentManager.getImageCaptureUri();
    }

    new Crop(inputFile).output(outputFile).asSquare().start(this);
  }
}
