package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProfileFragment extends Fragment
             implements ProfileAdapter.ItemClickListener, DcEventCenter.DcEventDelegate {

  public static final String CHAT_ID_EXTRA = "chat_id";
  public static final String CONTACT_ID_EXTRA = "contact_id";

  private static final int REQUEST_CODE_PICK_CONTACT = 2;

  private ProfileAdapter adapter;
  private ActionMode             actionMode;
  private final ActionModeCallback actionModeCallback = new ActionModeCallback();


  private DcContext            dcContext;
  protected int                chatId;
  private int                  contactId;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    chatId = getArguments() != null ? getArguments().getInt(CHAT_ID_EXTRA, -1) : -1;
    contactId = getArguments().getInt(CONTACT_ID_EXTRA, -1);
    dcContext = DcHelper.getContext(requireContext());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.profile_fragment, container, false);
    adapter = new ProfileAdapter(this, GlideApp.with(this), this);

    RecyclerView list = ViewUtil.findById(view, R.id.recycler_view);

    // add padding to avoid content hidden behind system bars
    ViewUtil.applyWindowInsets(list);

    list.setAdapter(adapter);
    list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

    update();

    DcEventCenter eventCenter = DcHelper.getEventCenter(requireContext());
    eventCenter.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    return view;
  }

  @Override
  public void onDestroyView() {
    DcHelper.getEventCenter(requireContext()).removeObservers(this);
    super.onDestroyView();
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
    else if(contactId>0 && contactId!=DcContact.DC_CONTACT_ID_SELF) {
      sharedChats = dcContext.getChatlist(0, null, contactId);
    }

    adapter.changeData(memberList, dcContact, sharedChats, dcChat);
  }


  // handle events
  // =========================================================================

  @Override
  public void onSettingsClicked(int settingsId) {
    switch(settingsId) {
      case ProfileAdapter.ITEM_ALL_MEDIA_BUTTON:
        if (chatId > 0) {
          Intent intent = new Intent(getActivity(), AllMediaActivity.class);
          intent.putExtra(AllMediaActivity.CHAT_ID_EXTRA, chatId);
          startActivity(intent);
        }
        break;
      case ProfileAdapter.ITEM_SEND_MESSAGE_BUTTON:
        onSendMessage();
        break;
      case ProfileAdapter.ITEM_INTRODUCED_BY:
        onVerifiedByClicked();
        break;
    }
  }

  @Override
  public void onStatusLongClicked() {
      Context context = requireContext();
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
        if (dcChat.canSend() && dcChat.isEncrypted()) {
          adapter.toggleMemberSelection(contactId);
          actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
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

  @Override
  public void onAvatarClicked() {
    ProfileActivity activity = (ProfileActivity)getActivity();
    activity.onEnlargeAvatar();
  }

  public void onAddMember() {
    DcChat dcChat = dcContext.getChat(chatId);
    Intent intent = new Intent(getContext(), ContactMultiSelectionActivity.class);
    ArrayList<Integer> preselectedContacts = new ArrayList<>();
    for (int memberId : dcContext.getChatContacts(chatId)) {
      preselectedContacts.add(memberId);
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
    requireContext().startActivity(intent);
    requireActivity().finish();
  }

  private void onVerifiedByClicked() {
    DcContact dcContact = dcContext.getContact(contactId);
    int verifierId = dcContact.getVerifierId();
    if (verifierId != 0 && verifierId != DcContact.DC_CONTACT_ID_SELF) {
      Intent intent = new Intent(getContext(), ProfileActivity.class);
      intent.putExtra(ProfileActivity.CONTACT_ID_EXTRA, verifierId);
      startActivity(intent);
    }
  }

  private void onSendMessage() {
    DcContact dcContact = dcContext.getContact(contactId);
    int chatId = dcContext.createChatByContactId(dcContact.getId());
    if (chatId != 0) {
      Intent intent = new Intent(getActivity(), ConversationActivity.class);
      intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
      requireActivity().startActivity(intent);
      requireActivity().finish();
    }
  }

  private class ActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      mode.getMenuInflater().inflate(R.menu.profile_context, menu);
      menu.findItem(R.id.delete).setVisible(true);
      menu.findItem(R.id.details).setVisible(false);
      menu.findItem(R.id.show_in_chat).setVisible(false);
      menu.findItem(R.id.save).setVisible(false);
      menu.findItem(R.id.share).setVisible(false);
      menu.findItem(R.id.menu_resend).setVisible(false);
      menu.findItem(R.id.menu_select_all).setVisible(false);
      mode.setTitle("1");

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      if (menuItem.getItemId() == R.id.delete) {
        final Collection<Integer> toDelIds = adapter.getSelectedMembers();
        StringBuilder readableToDelList = new StringBuilder();
        for (Integer toDelId : toDelIds) {
          if (readableToDelList.length() > 0) {
            readableToDelList.append(", ");
          }
          readableToDelList.append(dcContext.getContact(toDelId).getDisplayName());
        }
        DcChat dcChat = dcContext.getChat(chatId);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
          .setPositiveButton(R.string.remove_desktop, (d, which) -> {
            for (Integer toDelId : toDelIds) {
              dcContext.removeContactFromChat(chatId, toDelId);
            }
            mode.finish();
          })
          .setNegativeButton(android.R.string.cancel, null)
          .setMessage(getString(dcChat.isOutBroadcast() ? R.string.ask_remove_from_channel : R.string.ask_remove_members, readableToDelList))
          .show();
        Util.redPositiveButton(dialog);
        return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      actionMode = null;
      adapter.clearSelection();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode==REQUEST_CODE_PICK_CONTACT && resultCode==Activity.RESULT_OK && data!=null) {
      List<Integer> selected = data.getIntegerArrayListExtra(ContactMultiSelectionActivity.CONTACTS_EXTRA);
      if(selected == null) return;
      Util.runOnAnyBackgroundThread(() -> {
        for (Integer contactId : selected) {
          if (contactId!=null) {
            dcContext.addContactToChat(chatId, contactId);
          }
        }
      });
    }
  }
}
