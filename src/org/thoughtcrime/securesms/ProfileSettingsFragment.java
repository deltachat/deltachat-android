package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.qr.QrShowActivity;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ProfileSettingsFragment extends Fragment
             implements ProfileSettingsAdapter.ItemClickListener, DcEventCenter.DcEventDelegate {

  public static final String LOCALE_EXTRA  = "locale_extra";
  public static final String CHAT_ID_EXTRA = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";

  private static final int REQUEST_CODE_PICK_CONTACT = 2;

  private RecyclerView           list;
  private StickyHeaderDecoration listDecoration;
  private ProfileSettingsAdapter adapter;
  private ActionMode             actionMode;
  private ActionModeCallback     actionModeCallback = new ActionModeCallback();


  private Locale               locale;
  private DcContext            dcContext;
  protected int                chatId;
  private int                  contactId;

  protected ActionMode getActionMode() {
    return actionMode;
  }

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    locale = (Locale)getArguments().getSerializable(LOCALE_EXTRA);
    if (locale == null) throw new AssertionError();
    chatId = getArguments().getInt(CHAT_ID_EXTRA, -1);
    contactId = getArguments().getInt(CONTACT_ID_EXTRA, -1);
    dcContext = DcHelper.getContext(getContext());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.profile_settings_fragment, container, false);
    adapter = new ProfileSettingsAdapter(getContext(), GlideApp.with(this), locale,this);

    list  = ViewUtil.findById(view, R.id.recycler_view);
    list.setAdapter(adapter);
    list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    listDecoration = new StickyHeaderDecoration(adapter, false, true);
    list.addItemDecoration(listDecoration);

    update();

    DcEventCenter eventCenter = DcHelper.getEventCenter(getContext());
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    return view;
  }

  @Override
  public void onDestroyView() {
    DcHelper.getEventCenter(getContext()).removeObservers(this);
    super.onDestroyView();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    listDecoration.onConfigurationChanged(newConfig);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    update();
  }

  private void update()
  {
    int[]      memberList = null;
    DcChatlist sharedChats = null;

    DcChat dcChat = null;
    DcContact dcContact = null;
    if (contactId>0) { dcContact = dcContext.getContact(contactId); }
    if (chatId>0)    { dcChat    = dcContext.getChat(chatId); }

    if(dcChat!=null && dcChat.isMultiUser()) {
      memberList = dcContext.getChatContacts(chatId);
    }
    else if(contactId>0) {
      sharedChats = dcContext.getChatlist(0, null, contactId);
    }

    adapter.changeData(memberList, dcContact, sharedChats, dcChat);
    listDecoration.invalidateLayouts();
  }


  // handle events
  // =========================================================================

  @Override
  public void onSettingsClicked(int settingsId) {
    switch(settingsId) {
      case ProfileSettingsAdapter.INFO_SEND_MESSAGE_BUTTON:
        onSendMessage();
        break;
      case ProfileSettingsAdapter.INFO_VERIFIED:
        onVerifiedByClicked();
        break;
    }
  }

  @Override
  public void onStatusLongClicked() {
      Context context = getContext();
      new AlertDialog.Builder(context)
        .setTitle(R.string.pref_default_status_label)
        .setItems(new CharSequence[]{
            context.getString(R.string.menu_copy_to_clipboard)
          },
          (dialogInterface, i) -> {
            Util.writeTextToClipboard(context, adapter.getStatusText());
            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
          })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  @Override
  public void onMemberLongClicked(int contactId) {
    if (contactId>DcContact.DC_CONTACT_ID_LAST_SPECIAL || contactId==DcContact.DC_CONTACT_ID_SELF) {
      if (actionMode==null) {
        DcChat dcChat = dcContext.getChat(chatId);
        if (dcChat.canSend()) {
          adapter.toggleMemberSelection(contactId);
          actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
      } else {
        onMemberClicked(contactId);
      }
    }
  }

  @Override
  public void onMemberClicked(int contactId) {
    if (actionMode!=null) {
      if (contactId>DcContact.DC_CONTACT_ID_LAST_SPECIAL || contactId==DcContact.DC_CONTACT_ID_SELF) {
        adapter.toggleMemberSelection(contactId);
        if (adapter.getSelectedMembersCount() == 0) {
          actionMode.finish();
          actionMode = null;
        } else {
          actionMode.setTitle(String.valueOf(adapter.getSelectedMembersCount()));
        }
      }
    }
    else if(contactId==DcContact.DC_CONTACT_ID_ADD_MEMBER) {
      onAddMember();
    }
    else if(contactId==DcContact.DC_CONTACT_ID_QR_INVITE) {
      onQrInvite();
    }
    else if(contactId>DcContact.DC_CONTACT_ID_LAST_SPECIAL) {
      Intent intent = new Intent(getContext(), ProfileActivity.class);
      intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, contactId);
      startActivity(intent);
    }
  }

  public void onAddMember() {
    DcChat dcChat = dcContext.getChat(chatId);
    Intent intent = new Intent(getContext(), ContactMultiSelectionActivity.class);
    intent.putExtra(ContactSelectionListFragment.SELECT_VERIFIED_EXTRA, dcChat.isProtected());
    ArrayList<String> preselectedContacts = new ArrayList<>();
    int[] memberIds = dcContext.getChatContacts(chatId);
    for (int memberId : memberIds) {
      preselectedContacts.add(dcContext.getContact(memberId).getAddr());
    }
    intent.putExtra(ContactSelectionListFragment.PRESELECTED_CONTACTS, preselectedContacts);
    startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
  }

  public void onQrInvite() {
    Intent qrIntent = new Intent(getContext(), QrShowActivity.class);
    qrIntent.putExtra(QrShowActivity.CHAT_ID, chatId);
    startActivity(qrIntent);
  }

  @Override
  public void onSharedChatClicked(int chatId) {
    Intent intent = new Intent(getContext(), ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
    getContext().startActivity(intent);
    getActivity().finish();
  }

  private void onVerifiedByClicked() {
    DcContact dcContact = dcContext.getContact(contactId);
    if (dcContact.isVerified()) {
      int verifierId = dcContact.getVerifierId();
      if (verifierId != 0 && verifierId != contactId) {
        Intent intent = new Intent(getContext(), ProfileActivity.class);
        intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, verifierId);
        startActivity(intent);
      }
     }
  }

  private void onSendMessage() {
    DcContact dcContact = dcContext.getContact(contactId);
    int chatId = dcContext.createChatByContactId(dcContact.getId());
    if (chatId != 0) {
      Intent intent = new Intent(getActivity(), ConversationActivity.class);
      intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
      getActivity().startActivity(intent);
      getActivity().finish();
    }
  }

  private class ActionModeCallback implements ActionMode.Callback {

    private int originalStatusBarColor;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      mode.getMenuInflater().inflate(R.menu.profile_context, menu);
      menu.findItem(R.id.delete).setVisible(true);
      menu.findItem(R.id.details).setVisible(false);
      menu.findItem(R.id.show_in_chat).setVisible(false);
      menu.findItem(R.id.save).setVisible(false);
      menu.findItem(R.id.share).setVisible(false);
      mode.setTitle("1");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Window window = getActivity().getWindow();
        originalStatusBarColor = window.getStatusBarColor();
        window.setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
      }
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      switch (menuItem.getItemId()) {
        case R.id.delete:
          final Collection<Integer> toDelIds = adapter.getSelectedMembers();
          StringBuilder readableToDelList = new StringBuilder();
          for (Integer toDelId : toDelIds) {
            if(readableToDelList.length()>0) {
              readableToDelList.append(", ");
            }
            readableToDelList.append(dcContext.getContact(toDelId).getDisplayName());
          }
          DcChat dcChat = dcContext.getChat(chatId);
          new AlertDialog.Builder(getContext())
              .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                for (Integer toDelId : toDelIds) {
                  dcContext.removeContactFromChat(chatId, toDelId);
                }
                mode.finish();
              })
              .setNegativeButton(android.R.string.cancel, null)
              .setMessage(getString(dcChat.isBroadcast()? R.string.ask_remove_from_broadcast : R.string.ask_remove_members, readableToDelList))
              .show();
          return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      actionMode = null;
      adapter.clearSelection();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        getActivity().getWindow().setStatusBarColor(originalStatusBarColor);
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode==REQUEST_CODE_PICK_CONTACT && resultCode==Activity.RESULT_OK && data!=null) {
      List<String> selected = data.getStringArrayListExtra("contacts");
      for (String addr : selected) {
        if (addr!=null) {
          int toAddId = dcContext.createContact(null, addr);
          dcContext.addContactToChat(chatId, toAddId);
        }
      }
    }
  }
}
