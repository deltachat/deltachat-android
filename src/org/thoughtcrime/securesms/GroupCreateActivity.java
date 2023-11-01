package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.loader.app.LoaderManager;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.SelectedContactsAdapter;
import org.thoughtcrime.securesms.util.SelectedContactsAdapter.ItemClickListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
                                 implements ItemClickListener
{

  public static final String EDIT_GROUP_CHAT_ID = "edit_group_chat_id";
  public static final String GROUP_CREATE_VERIFIED_EXTRA  = "group_create_verified";
  public static final String CREATE_BROADCAST  = "group_create_broadcast";
  public static final String SUGGESTED_CONTACT_IDS = "suggested_contact_ids";

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
  private AttachmentManager attachmentManager;

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

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateViewState();
  }

  @Override
  protected void onDestroy() {
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
    } else if(verified) {
      title = getString(R.string.menu_new_verified_group);
    }
    else {
      title = getString(R.string.menu_new_group);
    }
    getSupportActionBar().setTitle(title);
  }

  private void initializeResources() {
    lv                  = ViewUtil.findById(this, R.id.selected_contacts_list);
    avatar              = ViewUtil.findById(this, R.id.avatar);
    groupName           = ViewUtil.findById(this, R.id.group_name);
    TextView groupHints = ViewUtil.findById(this, R.id.group_hints);

    SelectedContactsAdapter adapter = new SelectedContactsAdapter(this, GlideApp.with(this), broadcast);
    adapter.setItemClickListener(this);
    lv.setAdapter(adapter);

    ArrayList<Integer> suggestedContactIds = getIntent().getIntegerArrayListExtra(SUGGESTED_CONTACT_IDS);
    adapter.changeData(suggestedContactIds);
    initializeAvatarView();

    if (broadcast) {
      avatar.setVisibility(View.GONE);
      groupName.setHint(R.string.broadcast_list_name);
      groupHints.setText(R.string.chat_new_broadcast_hint);
      groupHints.setVisibility(isEdit()? View.GONE : View.VISIBLE);
    } else if (!verified) {
      groupHints.setVisibility(View.GONE);
    }

    if(isEdit()) {
      lv.setVisibility(View.GONE);
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

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.group_create, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.menu_create_group:
        String groupName = getGroupName();
        if (showGroupNameEmptyToast(groupName)) return true;

        if (groupChatId!=0) {
          updateGroup(groupName);
        } else {
          if (!broadcast && allMembersVerified()) {
            new AlertDialog.Builder(this)
              .setMessage(R.string.create_verified_group_ask)
              .setNeutralButton(R.string.learn_more, (d, w) -> IntentUtils.showBrowserIntent(this, "https://delta.chat/en/help#verifiedchats"))
              .setPositiveButton(R.string.yes, (d, w) -> {
                  verified = true;
                  createGroup(groupName);
              })
              .setNegativeButton(R.string.no, (d, w) -> {
                  createGroup(groupName);
              })
              .setCancelable(true)
              .show();
            return true;
          }
          createGroup(groupName);
        }

        return true;
    }

    return false;
  }

  private boolean allMembersVerified() {
    for (int id : getAdapter().getContacts()) {
      DcContact contact = dcContext.getContact(id);
      if (contact != null && !contact.isVerified()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void onItemClick(int contactId) {
    if (contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
      Intent intent = new Intent(this, ContactMultiSelectionActivity.class);
      intent.putExtra(ContactSelectionListFragment.SELECT_VERIFIED_EXTRA, verified);
      ArrayList<String> preselectedContacts = new ArrayList<>();
      for (int id : getAdapter().getContacts()) {
        preselectedContacts.add(dcContext.getContact(id).getAddr());
      }
      intent.putExtra(ContactSelectionListFragment.PRESELECTED_CONTACTS, preselectedContacts);
      startActivityForResult(intent, PICK_CONTACT);
    }
  }

  @Override
  public void onItemDeleteClick(int contactId) {
    getAdapter().remove(contactId);
  }

  private void createGroup(String groupName) {
    if (broadcast) {
      groupChatId = dcContext.createBroadcastList();
      dcContext.setChatName(groupChatId, groupName);
    } else {
      groupChatId = dcContext.createGroupChat(verified, groupName);
    }

    for (int contactId : getAdapter().getContacts()) {
      dcContext.addContactToChat(groupChatId, contactId);
    }
    if (avatarBmp!=null) {
      AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);
    }

    attachmentManager.cleanup();
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

  private void updateGroup(String groupName) {
    if (groupChatId == 0) {
      return;
    }
    dcContext.setChatName(groupChatId, groupName);

    if (avatarChanged) AvatarHelper.setGroupAvatar(this, groupChatId, avatarBmp);

    attachmentManager.cleanup();
    Intent intent = new Intent();
    intent.putExtra(GroupCreateActivity.EDIT_GROUP_CHAT_ID, groupChatId);
    setResult(RESULT_OK, intent);
    finish();
  }

  private SelectedContactsAdapter getAdapter() {
    return (SelectedContactsAdapter)lv.getAdapter();
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

    if (resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case REQUEST_CODE_AVATAR:
        Uri inputFile  = (data != null ? data.getData() : null);
        onFileSelected(inputFile);
        break;

      case PICK_CONTACT:
        ArrayList<Integer> contactIds = new ArrayList<>();
        for (String addr : Objects.requireNonNull(data.getStringArrayListExtra("contacts"))) {
          if(addr != null) {
            contactIds.add(dcContext.createContact(null, addr));
          }
        }
        getAdapter().changeData(contactIds);
        break;

      case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
        setAvatarView(data.getData());
        break;

      case Crop.REQUEST_CROP:
        setAvatarView(Crop.getOutput(data));
        break;
    }
  }

  private void setAvatarView(Uri output) {
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
    if (inputFile == null) {
      inputFile = attachmentManager.getImageCaptureUri();
    }

    AvatarHelper.cropAvatar(this, inputFile);
  }
}
