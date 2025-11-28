package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import androidx.loader.app.LoaderManager;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.thoughtcrime.securesms.components.AvatarSelector;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.mms.AttachmentManager;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.profiles.AvatarHelper;
import org.thoughtcrime.securesms.scribbles.ScribbleActivity;
import org.thoughtcrime.securesms.util.SelectedContactsAdapter;
import org.thoughtcrime.securesms.util.SelectedContactsAdapter.ItemClickListener;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import chat.delta.rpc.RpcException;

public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
                                 implements ItemClickListener
{

  public static final String EDIT_GROUP_CHAT_ID = "edit_group_chat_id";
  public static final String CREATE_BROADCAST = "create_broadcast";
  public static final String UNENCRYPTED = "unencrypted";
  public static final String CLONE_CHAT_EXTRA = "clone_chat";

  private static final int PICK_CONTACT = 1;
  private static final int REQUEST_CODE_AVATAR = 2759;

  private DcContext dcContext;

  private boolean unencrypted;
  private boolean broadcast;
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
    broadcast = getIntent().getBooleanExtra(CREATE_BROADCAST, false);
    unencrypted = getIntent().getBooleanExtra(UNENCRYPTED, false);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    groupChatId = getIntent().getIntExtra(EDIT_GROUP_CHAT_ID, 0);
    attachmentManager = new AttachmentManager(this, () -> {});
    avatarChanged = false;

    // groupChatId may be set during creation,
    // so always check isEdit()
    if(groupChatId !=0) {
      isEdit = true;
      DcChat dcChat = dcContext.getChat(groupChatId);
      broadcast = dcChat.isOutBroadcast();
      unencrypted = !dcChat.isEncrypted();
    }

    int chatId = getIntent().getIntExtra(CLONE_CHAT_EXTRA, 0);
    if (chatId != 0) {
      DcChat dcChat  = dcContext.getChat(chatId);
      broadcast = dcChat.isOutBroadcast();
      unencrypted = !dcChat.isEncrypted();
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
      title = getString(R.string.new_channel);
    }
    else if(unencrypted) {
      title = getString(R.string.new_email);
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
    TextView chatHints  = ViewUtil.findById(this, R.id.chat_hints);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(lv, false, false, false, true);
    // apply padding to root to avoid collision with system bars
    ViewUtil.applyWindowInsets(findViewById(R.id.root_layout), true, false, true, false);

    initializeAvatarView();

    SelectedContactsAdapter adapter = new SelectedContactsAdapter(this, GlideApp.with(this), broadcast, unencrypted);
    adapter.setItemClickListener(this);
    lv.setAdapter(adapter);

    int chatId = getIntent().getIntExtra(CLONE_CHAT_EXTRA, 0);
    if (chatId != 0) {
      DcChat dcChat = dcContext.getChat(chatId);
      groupName.setText(dcChat.getName());
      File file = new File(dcChat.getProfileImage());
      if (file.exists()) {
        setAvatarView(Uri.fromFile(file));
      }

      int[] contactIds = dcContext.getChatContacts(chatId);
      ArrayList<Integer> preselectedContactIds = new ArrayList<>(contactIds.length);
      for (int id : contactIds) {
        preselectedContactIds.add(id);
      }
      adapter.changeData(preselectedContactIds);
    } else {
      adapter.changeData(null);
    }

    if (broadcast) {
      groupName.setHint(R.string.channel_name);
      chatHints.setVisibility(View.VISIBLE);
    } else if (unencrypted) {
      avatar.setVisibility(View.GONE);
      groupName.setHint(R.string.subject);
      chatHints.setVisibility(View.GONE);
    } else {
      chatHints.setVisibility(View.GONE);
    }

    if(isEdit()) {
      groupName.setText(dcContext.getChat(groupChatId).getName());
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
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      finish();
      return true;
    } else if (itemId == R.id.menu_create_group) {
      String groupName = getGroupName();
      if (showGroupNameEmptyToast(groupName)) return true;

      if (groupChatId != 0) {
        updateGroup(groupName);
      } else {
        createGroup(groupName);
      }

      return true;
    }

    return false;
  }

  @Override
  public void onItemClick(int contactId) {
    if (contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER) {
      Intent intent = new Intent(this, ContactMultiSelectionActivity.class);
      intent.putExtra(ContactSelectionListFragment.SELECT_UNENCRYPTED_EXTRA, unencrypted);
      ArrayList<Integer> preselectedContacts = new ArrayList<>(getAdapter().getContacts());
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
      try {
        groupChatId = DcHelper.getRpc(this).createBroadcast(dcContext.getAccountId(), groupName);
      } catch (RpcException e) {
        e.printStackTrace();
        return;
      }
    } else if (unencrypted) {
      try {
        groupChatId = DcHelper.getRpc(this).createGroupChatUnencrypted(dcContext.getAccountId(), groupName);
      } catch (RpcException e) {
        e.printStackTrace();
        return;
      }
    } else {
      groupChatId = dcContext.createGroupChat(groupName);
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
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
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
        for (Integer contactId : Objects.requireNonNull(data.getIntegerArrayListExtra(ContactMultiSelectionActivity.CONTACTS_EXTRA))) {
          if(contactId != null) {
            contactIds.add(contactId);
          }
        }
        getAdapter().changeData(contactIds);
        break;

      case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
        setAvatarView(data.getData());
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
            .override(AvatarHelper.AVATAR_SIZE, AvatarHelper.AVATAR_SIZE)
            .into(new CustomTarget<Bitmap>() {
              @Override
              public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                setAvatar(output, resource);
              }

              @Override
              public void onLoadCleared(@Nullable Drawable placeholder) {}
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
