/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import org.thoughtcrime.securesms.components.PushRecipientsPanel;
import org.thoughtcrime.securesms.components.PushRecipientsPanel.RecipientsPanelChangedListener;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.contacts.ContactsCursorLoader.DisplayMode;
import org.thoughtcrime.securesms.contacts.RecipientsEditor;
import org.thoughtcrime.securesms.contacts.avatars.ContactColors;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.GroupDatabase.GroupRecord;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.SelectedRecipientsAdapter;
import org.thoughtcrime.securesms.util.SelectedRecipientsAdapter.OnRecipientDeletedListener;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.thoughtcrime.securesms.util.task.ProgressDialogAsyncTask;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Activity to create and update groups
 *
 * @author Jake McGinty
 */
public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
                                 implements OnRecipientDeletedListener,
                                            RecipientsPanelChangedListener
{

  private final static String TAG = GroupCreateActivity.class.getSimpleName();

  public static final String GROUP_ADDRESS_EXTRA = "group_recipient";
  public static final String GROUP_THREAD_EXTRA  = "group_thread";
  public static final String GROUP_CREATE_VERIFIED_EXTRA  = "group_create_verified";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT = 1;
  public static final  int AVATAR_SIZE  = 210;

  private ApplicationDcContext dcContext;

  private boolean      createVerified;
  private EditText     groupName;
  private ListView     lv;
  private ImageView    avatar;
  private TextView     creatingText;
  private Bitmap       avatarBmp;

  @NonNull private Optional<GroupData> groupToUpdate = Optional.absent();

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
    createVerified = getIntent().getBooleanExtra(GROUP_CREATE_VERIFIED_EXTRA, false);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    initializeResources();
    initializeExistingGroup();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    updateViewState();
  }

  @SuppressWarnings("ConstantConditions")
  private void updateViewState() {
    avatar.setEnabled(true);
    groupName.setEnabled(true);

    String title;
    if(groupToUpdate.isPresent()) {
      title = getString(R.string.GroupCreateActivity_actionbar_edit_title);
    }
    else if(createVerified) {
      title = getString(R.string.GroupCreateActivity_actionbar_verified_title);
    }
    else {
      title = getString(R.string.GroupCreateActivity_actionbar_title);
    }
    getSupportActionBar().setTitle(title);
  }

  private void addSelectedContacts(@NonNull Recipient... recipients) {
    new AddMembersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipients);
  }

  private void addSelectedContacts(@NonNull Collection<Recipient> recipients) {
    addSelectedContacts(recipients.toArray(new Recipient[recipients.size()]));
  }

  private void initializeResources() {
    RecipientsEditor    recipientsEditor = ViewUtil.findById(this, R.id.recipients_text);
    PushRecipientsPanel recipientsPanel  = ViewUtil.findById(this, R.id.recipients);
    lv           = ViewUtil.findById(this, R.id.selected_contacts_list);
    avatar       = ViewUtil.findById(this, R.id.avatar);
    groupName    = ViewUtil.findById(this, R.id.group_name);
    creatingText = ViewUtil.findById(this, R.id.creating_group_text);
    SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this);
    adapter.setOnRecipientDeletedListener(this);
    lv.setAdapter(adapter);
    recipientsEditor.setHint(R.string.recipients_panel__add_members);
    recipientsPanel.setPanelChangeListener(this);
    findViewById(R.id.contacts_button).setOnClickListener(new AddRecipientButtonListener());
    avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_group_white_24dp).asDrawable(this, ContactColors.UNKNOWN_COLOR.toConversationColor(this)));
    avatar.setOnClickListener(view -> Crop.pickImage(GroupCreateActivity.this));
  }

  private void initializeExistingGroup() {
    final Address groupAddress = getIntent().getParcelableExtra(GROUP_ADDRESS_EXTRA);

    if (groupAddress != null) {
      new FillExistingGroupInfoAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, groupAddress.toGroupString());
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
        if (groupToUpdate.isPresent()) handleGroupUpdate();
        else                           handleGroupCreate();
        return true;
    }

    return false;
  }

  @Override
  public void onRecipientDeleted(Recipient recipient) {
    getAdapter().remove(recipient);
    updateViewState();
  }

  @Override
  public void onRecipientsPanelUpdate(List<Recipient> recipients) {
    if (recipients != null && !recipients.isEmpty()) addSelectedContacts(recipients);
  }

  private void handleGroupCreate() {
    String groupName = getGroupName();
    if(groupName==null) {
      Toast.makeText(this, getString(R.string.GroupCreateActivity_please_enter_group_name), Toast.LENGTH_LONG).show();
      return;
    }

    int chatId = dcContext.createGroupChat(createVerified, groupName);

    Set<Recipient> members = getAdapter().getRecipients();
    for(Recipient member : members) {
      Address address = member.getAddress();
      if(address.isDcContact()) {
        int contactId = address.getDcContactId();
        dcContext.addContactToChat(chatId, contactId);
      }
    }

    // TODO: handle avatarBmp

    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, (long)chatId);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
    intent.putExtra(ConversationActivity.ADDRESS_EXTRA, Address.fromChat(dcContext.getChat((int)chatId)));
    startActivity(intent);
    finish();
  }

  private void handleGroupUpdate() {
    int chatId = 0;// TODO: get correct id from groupToUpdate.get().id; or so

    String groupName = getGroupName();
    if(groupName!=null) {
      dcContext.setChatName(chatId, groupName);
    }

    // TODO: compare dcContext.getChatContacts(chatId); against getAdapter().getRecipients();
    // and add/remove contacts


    // TODO: handle avatarBmp

    finish();
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
          Address   address   = Address.fromExternal(this, contact);
          Recipient recipient = Recipient.from(this, address, false);

          addSelectedContacts(recipient);
        }
        break;

      case Crop.REQUEST_PICK:
        new Crop(data.getData()).output(outputFile).asSquare().start(this);
        break;
      case Crop.REQUEST_CROP:
        GlideApp.with(this)
                .asBitmap()
                .load(Crop.getOutput(data))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .override(AVATAR_SIZE, AVATAR_SIZE)
                .into(new SimpleTarget<Bitmap>() {
                  @Override
                  public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                    setAvatar(Crop.getOutput(data), resource);
                  }
                });
    }
  }

  private class AddRecipientButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(GroupCreateActivity.this, PushContactSelectionActivity.class);
      intent.putExtra(ContactSelectionListFragment.SELECT_VERIFIED_EXTRA, createVerified);
      if (groupToUpdate.isPresent()) {
        intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_PUSH);
      } else {
        intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_PUSH | DisplayMode.FLAG_SMS);
      }
      startActivityForResult(intent, PICK_CONTACT);
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
      ApplicationDcContext dcContext = DcHelper.getContext(activity);

      for (Result result : results) {
        Recipient recipient = result.recipient.get();
        Address address = recipient.getAddress();
        if(address.isDcContact()) {
          activity.getAdapter().add(recipient, true);
        }
        else if(dcContext.mayBeValidAddr(address.toString())) {
          int contactId = DcHelper.getContext(activity).createContact(null, address.toString());
          activity.getAdapter().add(dcContext.getRecipient(dcContext.getContact(contactId)), true);
        }
      }
      activity.updateViewState();
    }
  }

  private static class FillExistingGroupInfoAsyncTask extends ProgressDialogAsyncTask<String,Void,Optional<GroupData>> {
    private GroupCreateActivity activity;

    public FillExistingGroupInfoAsyncTask(GroupCreateActivity activity) {
      super(activity,
            R.string.GroupCreateActivity_loading_group_details,
            R.string.please_wait);
      this.activity = activity;
    }

    @Override
    protected Optional<GroupData> doInBackground(String... groupIds) {
      final GroupDatabase         db               = DatabaseFactory.getGroupDatabase(activity);
      final List<Recipient>       recipients       = db.getGroupMembers(groupIds[0], false);
      final Optional<GroupRecord> group            = db.getGroup(groupIds[0]);
      final Set<Recipient>        existingContacts = new HashSet<>(recipients.size());
      existingContacts.addAll(recipients);

      if (group.isPresent()) {
        return Optional.of(new GroupData(groupIds[0],
                                         existingContacts,
                                         BitmapUtil.fromByteArray(group.get().getAvatar()),
                                         group.get().getAvatar(),
                                         group.get().getTitle()));
      } else {
        return Optional.absent();
      }
    }

    @Override
    protected void onPostExecute(Optional<GroupData> group) {
      super.onPostExecute(group);

      if (group.isPresent() && !activity.isFinishing()) {
        activity.groupToUpdate = group;

        activity.groupName.setText(group.get().name);
        if (group.get().avatarBmp != null) {
          activity.setAvatar(group.get().avatarBytes, group.get().avatarBmp);
        }
        SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(activity, group.get().recipients);
        adapter.setOnRecipientDeletedListener(activity);
        activity.lv.setAdapter(adapter);
        activity.updateViewState();
      }
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

  private static class GroupData {
    String         id;
    Set<Recipient> recipients;
    Bitmap         avatarBmp;
    byte[]         avatarBytes;
    String         name;

    public GroupData(String id, Set<Recipient> recipients, Bitmap avatarBmp, byte[] avatarBytes, String name) {
      this.id          = id;
      this.recipients  = recipients;
      this.avatarBmp   = avatarBmp;
      this.avatarBytes = avatarBytes;
      this.name        = name;
    }
  }
}
