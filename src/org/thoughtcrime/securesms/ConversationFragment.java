/*
 * Copyright (C) 2015 Open Whisper Systems
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ConversationAdapter.HeaderViewHolder;
import org.thoughtcrime.securesms.ConversationAdapter.ItemClickListener;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.DcMsgListLoader;
import org.thoughtcrime.securesms.contactshare.Contact;
import org.thoughtcrime.securesms.contactshare.ContactUtil;
import org.thoughtcrime.securesms.contactshare.SharedContactDetailsActivity;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.jobs.DirectoryRefreshJob;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.OutgoingMediaMessage;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.thoughtcrime.securesms.util.CommunicationActions;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.ViewUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressLint("StaticFieldLeak")
public class ConversationFragment extends Fragment
  implements LoaderManager.LoaderCallbacks<int[]>,
             DcEventCenter.DcEventDelegate
{
  private static final String TAG       = ConversationFragment.class.getSimpleName();
  private static final String KEY_LIMIT = "limit";

  private static final int SCROLL_ANIMATION_THRESHOLD = 50;
  private static final int CODE_ADD_EDIT_CONTACT      = 77;

  private final ActionModeCallback actionModeCallback     = new ActionModeCallback();
  private final ItemClickListener  selectionClickListener = new ConversationFragmentItemClickListener();

  private ConversationFragmentListener listener;

  private Recipient                   recipient;
  private long                        threadId;
  private long                        lastSeen;
  private int                         startingPosition;
  private int                         previousOffset;
  private boolean                     firstLoad;
  private long                        loaderStartTime;
  private ActionMode                  actionMode;
  private Locale                      locale;
  private RecyclerView                list;
  private RecyclerView.ItemDecoration lastSeenDecoration;
  private View                        scrollToBottomButton;
  private TextView                    scrollDateHeader;

  private ApplicationDcContext        dcContext;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    this.locale = (Locale) getArguments().getSerializable(PassphraseRequiredActionBarActivity.LOCALE_EXTRA);
    this.dcContext = DcHelper.getContext(getContext());

    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_INCOMING_MSG);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSGS_CHANGED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_DELIVERED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_FAILED);
    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_MSG_READ);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    final View view = inflater.inflate(R.layout.conversation_fragment, container, false);
    list                 = ViewUtil.findById(view, android.R.id.list);
    scrollToBottomButton = ViewUtil.findById(view, R.id.scroll_to_bottom_button);
    scrollDateHeader     = ViewUtil.findById(view, R.id.scroll_date_header);

    scrollToBottomButton.setOnClickListener(v -> scrollToBottom());

    final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true);
    list.setHasFixedSize(false);
    list.setLayoutManager(layoutManager);
    list.setItemAnimator(null);

    // setLayerType() is needed to allow larger items (long texts in our case)
    // with hardware layers, drawing may result in errors as "OpenGLRenderer: Path too large to be rendered into a texture"
    if (android.os.Build.VERSION.SDK_INT >= 11)
    {
      list.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    return view;
  }

  @Override
  public void onActivityCreated(Bundle bundle) {
    super.onActivityCreated(bundle);

    initializeResources();
    initializeListAdapter();
  }

  @Override
  public void onDestroy() {
    dcContext.eventCenter.removeObservers(this);
    super.onDestroy();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    this.listener = (ConversationFragmentListener)activity;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (list.getAdapter() != null) {
      list.getAdapter().notifyDataSetChanged();
    }
  }

  public void onNewIntent() {
    if (actionMode != null) {
      actionMode.finish();
    }

    initializeResources();
    initializeListAdapter();

    if (threadId == -1) {
      reloadList();
    }
  }

  public void moveToLastSeen() {
    if (lastSeen <= 0) {
      Log.i(TAG, "No need to move to last seen.");
      return;
    }

    if (list == null || getListAdapter() == null) {
      Log.w(TAG, "Tried to move to last seen position, but we hadn't initialized the view yet.");
      return;
    }

    int position = getListAdapter().findLastSeenPosition(lastSeen);
    scrollToLastSeenPosition(position);
  }

  private void initializeResources() {
    this.recipient         = Recipient.from(getActivity(), getActivity().getIntent().getParcelableExtra(ConversationActivity.ADDRESS_EXTRA), true);
    this.threadId          = this.getActivity().getIntent().getLongExtra(ConversationActivity.THREAD_ID_EXTRA, -1);
    this.lastSeen          = this.getActivity().getIntent().getLongExtra(ConversationActivity.LAST_SEEN_EXTRA, -1);
    this.startingPosition  = this.getActivity().getIntent().getIntExtra(ConversationActivity.STARTING_POSITION_EXTRA, -1);
    this.firstLoad         = true;

    OnScrollListener scrollListener = new ConversationScrollListener(getActivity());
    list.addOnScrollListener(scrollListener);
  }

  private void initializeListAdapter() {
    if (this.recipient != null && this.threadId != -1) {
      ConversationAdapter adapter = new ConversationAdapter(getActivity(), dcContext.getChat((int)this.threadId), GlideApp.with(this), locale, selectionClickListener, this.recipient);
      list.setAdapter(adapter);
      list.addItemDecoration(new StickyHeaderDecoration(adapter, false, false));

      setLastSeen(lastSeen);
      reloadList();
    }
  }

  private void setCorrectMenuVisibility(Menu menu) {
    Set<DcMsg>         messageRecords = getListAdapter().getSelectedItems();
    boolean            actionMessage  = false;
    boolean            hasText        = false;

    if (actionMode != null && messageRecords.size() == 0) {
      actionMode.finish();
      return;
    }

    for (DcMsg messageRecord : messageRecords) {
      if (messageRecord.isGroupAction() ||
          messageRecord.isJoined() || messageRecord.isExpirationTimerUpdate() ||
          messageRecord.isEndSession() || messageRecord.isIdentityUpdate() ||
          messageRecord.isIdentityVerified() || messageRecord.isIdentityDefault())
      {
        actionMessage = true;
      }
      if (messageRecord.getBody().length() > 0) {
        hasText = true;
      }
      if (actionMessage && hasText) {
        break;
      }
    }

    if (messageRecords.size() > 1) {
      menu.findItem(R.id.menu_context_forward).setVisible(false);
      menu.findItem(R.id.menu_context_reply).setVisible(false);
      menu.findItem(R.id.menu_context_details).setVisible(false);
      menu.findItem(R.id.menu_context_save_attachment).setVisible(false);
      menu.findItem(R.id.menu_context_resend).setVisible(false);
    } else {
      DcMsg messageRecord = messageRecords.iterator().next();

      menu.findItem(R.id.menu_context_resend).setVisible(false/*messageRecord.isFailed()*/);
      menu.findItem(R.id.menu_context_save_attachment).setVisible(messageRecord.hasFile());

      menu.findItem(R.id.menu_context_forward).setVisible(!actionMessage);
      menu.findItem(R.id.menu_context_details).setVisible(!actionMessage);
      menu.findItem(R.id.menu_context_reply).setVisible(false/*!actionMessage             &&
                                                        !messageRecord.isPending() &&
                                                        !messageRecord.isFailed()  &&
                                                        messageRecord.isSecure()*/);
    }
    menu.findItem(R.id.menu_context_copy).setVisible(!actionMessage && hasText);
  }

  private ConversationAdapter getListAdapter() {
    return (ConversationAdapter) list.getAdapter();
  }

  private DcMsg getSelectedMessageRecord() {
    Set<DcMsg> messageRecords = getListAdapter().getSelectedItems();

    if (messageRecords.size() == 1) return messageRecords.iterator().next();
    else                            throw new AssertionError();
  }

  public void reload(Recipient recipient, long threadId) {
    this.recipient = recipient;

    if (this.threadId != threadId) {
      this.threadId = threadId;
      initializeListAdapter();
    }
  }

  public void scrollToBottom() {
    if (((LinearLayoutManager) list.getLayoutManager()).findFirstVisibleItemPosition() < SCROLL_ANIMATION_THRESHOLD) {
      list.smoothScrollToPosition(0);
    } else {
      list.scrollToPosition(0);
    }
  }

  public void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
    if (lastSeenDecoration != null) {
      list.removeItemDecoration(lastSeenDecoration);
    }

    lastSeenDecoration = new ConversationAdapter.LastSeenHeader(getListAdapter(), lastSeen);
    list.addItemDecoration(lastSeenDecoration);
  }

  private String getMessageContent(DcMsg msg, DcMsg prev_msg)
  {
    String ret = "";

    if (msg.getFromId() != prev_msg.getFromId()) {
      DcContact contact = dcContext.getContact(msg.getFromId());
      ret += contact.getDisplayName() + ":\n";
    }

    if( msg.getType()==DcMsg.DC_MSG_TEXT ) {
      ret += msg.getText();
    }
    else {
      ret += msg.getSummarytext(1000);
    }

    return ret;
  }

  private void handleCopyMessage(final Set<DcMsg> dcMsgsSet) {
    List<DcMsg> dcMsgsList = new LinkedList<>(dcMsgsSet);
    Collections.sort(dcMsgsList, new Comparator<DcMsg>() {
      @Override
      public int compare(DcMsg lhs, DcMsg rhs) {
        if      (lhs.getDateReceived() < rhs.getDateReceived())  return -1;
        else if (lhs.getDateReceived() == rhs.getDateReceived()) return 0;
        else                                                     return 1;
      }
    });

    StringBuilder result = new StringBuilder();

    DcMsg prevMsg = new DcMsg(dcContext, DcMsg.DC_MSG_TEXT);
    for (DcMsg msg : dcMsgsList) {
      if (result.length()>0) {
        result.append("\n\n");
      }
      result.append(getMessageContent(msg, prevMsg));
      prevMsg = msg;
    }

    if (result.length()>0) {
      try {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(result.toString());
        Toast.makeText(getActivity(), getActivity().getResources().getQuantityString(R.plurals.ConversationFragment_n_selected_messages_copied_to_clipboard, dcMsgsList.size(), dcMsgsList.size()), Toast.LENGTH_LONG).show();
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void handleDeleteMessages(final Set<DcMsg> messageRecords) {
    int                 messagesCount = messageRecords.size();
    AlertDialog.Builder builder       = new AlertDialog.Builder(getActivity());

    builder.setIconAttribute(R.attr.dialog_alert_icon);
    builder.setTitle(getActivity().getResources().getQuantityString(R.plurals.ConversationFragment_delete_selected_messages, messagesCount, messagesCount));
    builder.setMessage(getActivity().getResources().getQuantityString(R.plurals.ConversationFragment_this_will_delete_n_selected_messages_here_and_on_server, messagesCount, messagesCount));
    builder.setCancelable(true);

    builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        int[] ids = DcMsg.msgSetToIds(messageRecords);
        dcContext.deleteMsgs(ids);
      }
    });

    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }

  private void handleDisplayDetails(DcMsg dcMsg) {
    String info_str = dcContext.getMsgInfo(dcMsg.getId());
    new AlertDialog.Builder(getActivity())
      .setMessage(info_str)
      .setPositiveButton(android.R.string.ok, null)
      .show();
  }

  private void handleForwardMessage(DcMsg message) {
    Intent composeIntent = new Intent(getActivity(), ShareActivity.class);
    composeIntent.putExtra(Intent.EXTRA_TEXT, message.getDisplayBody().toString());
    /* TODO: handle fowarding media
    if (message.isMms()) {
      MmsMessageRecord mediaMessage = (MmsMessageRecord) message;
      if (mediaMessage.containsMediaSlide()) {
        Slide slide = mediaMessage.getSlideDeck().getSlides().get(0);
        composeIntent.putExtra(Intent.EXTRA_STREAM, slide.getUri());
        composeIntent.setType(slide.getContentType());
      }
    }*/
    startActivity(composeIntent);
  }

  private void handleResendMessage(final DcMsg message) {
    // TODO
    /*
    final Context context = getActivity().getApplicationContext();
    new AsyncTask<MessageRecord, Void, Void>() {
      @Override
      protected Void doInBackground(MessageRecord... messageRecords) {
        MessageSender.resend(context, messageRecords[0]);
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
    */
  }

  private void handleReplyMessage(final DcMsg message) {
    // TODO
    // listener.handleReplyMessage(message);
  }

  private void handleSaveAttachment(final DcMsg message) {
    /* TODO
    SaveAttachmentTask.showWarningDialog(getActivity(), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        for (Slide slide : message.getSlideDeck().getSlides()) {
          if ((slide.hasImage() || slide.hasVideo() || slide.hasAudio() || slide.hasDocument()) && slide.getUri() != null) {
            SaveAttachmentTask saveTask = new SaveAttachmentTask(getActivity());
            saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Attachment(slide.getUri(), slide.getContentType(), message.getDateReceived(), slide.getFileName().orNull()));
            return;
          }
        }

        Log.w(TAG, "No slide with attachable media found, failing nicely.");
        Toast.makeText(getActivity(),
                       getResources().getQuantityString(R.plurals.ConversationFragment_error_while_saving_attachments_to_sd_card, 1),
                       Toast.LENGTH_LONG).show();
      }
    });
    */
  }

  public void reloadList() {
    ConversationAdapter adapter = getListAdapter();
    if (adapter == null) {
      return;
    }

    // just for testing, here are two variants.
    final boolean loadSynchronous = true;
    if (loadSynchronous) {
      // this typically takes <1 ms ...
      loaderStartTime = System.currentTimeMillis();
      int[] msgs = DcHelper.getContext(getContext()).getChatMsgs((int)threadId, 0, 0);
      onLoadFinished(null, msgs);
    }
    else {
      // ... while this takes >100 ms
      LoaderManager loaderManager = getLoaderManager();
      if (loaderManager != null) {
        loaderManager.restartLoader(0, Bundle.EMPTY, this);
      }
    }
  }

  @Override
  public Loader<int[]> onCreateLoader(int id, Bundle args) {
    Log.w(TAG, "onCreateLoader");
    loaderStartTime = System.currentTimeMillis();

    return new DcMsgListLoader(getActivity(), (int)threadId, 0, 0);
  }

  @Override
  public void onLoadFinished(Loader<int[]> arg0, int[] dcMsgList) {
    long loadTime = System.currentTimeMillis() - loaderStartTime;
    int  count    = dcMsgList.length;
    Log.w(TAG, "onLoadFinished - took " + loadTime + " ms to load a message list of size " + count);

    ConversationAdapter adapter = getListAdapter();
    if (adapter == null) {
      return;
    }

    if (lastSeen == -1) {
      //setLastSeen(loader.getLastSeen()); -- TODO
    }

    adapter.changeData(dcMsgList);

    int lastSeenPosition = adapter.findLastSeenPosition(lastSeen);

    if (firstLoad) {
      if (startingPosition >= 0) {
        scrollToStartingPosition(startingPosition);
      } else {
        scrollToLastSeenPosition(lastSeenPosition);
      }
      firstLoad = false;
    } else if (previousOffset > 0) {
      int scrollPosition = previousOffset + ((LinearLayoutManager) list.getLayoutManager()).findFirstVisibleItemPosition();
      scrollPosition = Math.min(scrollPosition, count - 1);

      View firstView = list.getLayoutManager().getChildAt(scrollPosition);
      int pixelOffset = (firstView == null) ? 0 : (firstView.getBottom() - list.getPaddingBottom());

      ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(scrollPosition, pixelOffset);
      previousOffset = 0;
    }

    if (lastSeenPosition <= 0) {
      setLastSeen(0);
    }
  }

  @Override
  public void onLoaderReset(Loader<int[]> arg0) {
    if (list.getAdapter() != null) {
      getListAdapter().changeData(null);
    }
  }

  public long stageOutgoingMessage(OutgoingMediaMessage message) {
    MessageRecord messageRecord = DatabaseFactory.getMmsDatabase(getContext()).readerFor(message, threadId).getCurrent();

    if (getListAdapter() != null) {
      //getListAdapter().setHeaderView(null);
      setLastSeen(0);
      getListAdapter().addFastRecord(messageRecord);
    }

    return messageRecord.getId();
  }

  public long stageOutgoingMessage(OutgoingTextMessage message) {
    MessageRecord messageRecord = DatabaseFactory.getSmsDatabase(getContext()).readerFor(message, threadId).getCurrent();

    if (getListAdapter() != null) {
      //getListAdapter().setHeaderView(null);
      setLastSeen(0);
      getListAdapter().addFastRecord(messageRecord);
    }

    return messageRecord.getId();
  }

  public void releaseOutgoingMessage(long id) {
    if (getListAdapter() != null) {
      getListAdapter().releaseFastRecord(id);
    }
  }

  private void scrollToStartingPosition(final int startingPosition) {
    list.post(() -> {
      list.getLayoutManager().scrollToPosition(startingPosition);
      getListAdapter().pulseHighlightItem(startingPosition);
    });
  }

  private void scrollToLastSeenPosition(final int lastSeenPosition) {
    if (lastSeenPosition > 0) {
      list.post(() -> ((LinearLayoutManager)list.getLayoutManager()).scrollToPositionWithOffset(lastSeenPosition, list.getHeight()));
    }
  }

  public interface ConversationFragmentListener {
    void setThreadId(long threadId);
    void handleReplyMessage(MessageRecord messageRecord);
  }

  private class ConversationScrollListener extends OnScrollListener {

    private final Animation              scrollButtonInAnimation;
    private final Animation              scrollButtonOutAnimation;
    private final ConversationDateHeader conversationDateHeader;

    private boolean wasAtBottom           = true;
    private boolean wasAtZoomScrollHeight = false;
    private long    lastPositionId        = -1;

    ConversationScrollListener(@NonNull Context context) {
      this.scrollButtonInAnimation  = AnimationUtils.loadAnimation(context, R.anim.fade_scale_in);
      this.scrollButtonOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_scale_out);
      this.conversationDateHeader   = new ConversationDateHeader(context, scrollDateHeader);

      this.scrollButtonInAnimation.setDuration(100);
      this.scrollButtonOutAnimation.setDuration(50);
    }

    @Override
    public void onScrolled(final RecyclerView rv, final int dx, final int dy) {
      boolean currentlyAtBottom           = isAtBottom();
      boolean currentlyAtZoomScrollHeight = isAtZoomScrollHeight();
      int     positionId                  = getHeaderPositionId();

      if (currentlyAtBottom && !wasAtBottom) {
        ViewUtil.animateOut(scrollToBottomButton, scrollButtonOutAnimation, View.INVISIBLE);
      }

      if (currentlyAtZoomScrollHeight && !wasAtZoomScrollHeight) {
        ViewUtil.animateIn(scrollToBottomButton, scrollButtonInAnimation);
      }

      if (positionId != lastPositionId) {
        bindScrollHeader(conversationDateHeader, positionId);
      }

      wasAtBottom           = currentlyAtBottom;
      wasAtZoomScrollHeight = currentlyAtZoomScrollHeight;
      lastPositionId        = positionId;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        conversationDateHeader.show();
      } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        conversationDateHeader.hide();
      }
    }

    private boolean isAtBottom() {
      if (list.getChildCount() == 0) return true;

      View    bottomView       = list.getChildAt(0);
      int     firstVisibleItem = ((LinearLayoutManager) list.getLayoutManager()).findFirstVisibleItemPosition();
      boolean isAtBottom       = (firstVisibleItem == 0);

      return isAtBottom && bottomView.getBottom() <= list.getHeight();
    }

    private boolean isAtZoomScrollHeight() {
      return ((LinearLayoutManager) list.getLayoutManager()).findFirstCompletelyVisibleItemPosition() > 4;
    }

    private int getHeaderPositionId() {
      return ((LinearLayoutManager)list.getLayoutManager()).findLastVisibleItemPosition();
    }

    private void bindScrollHeader(HeaderViewHolder headerViewHolder, int positionId) {
      if (((ConversationAdapter)list.getAdapter()).getHeaderId(positionId) != -1) {
        ((ConversationAdapter) list.getAdapter()).onBindHeaderViewHolder(headerViewHolder, positionId);
      }
    }
  }


  void querySetupCode(final DcMsg dcMsg, String[] preload)
  {
    if( !dcMsg.isSetupMessage()) {
      return;
    }

    View gl = View.inflate(getActivity(), R.layout.setup_code_grid, null);
    final EditText[] editTexts = {
        (EditText) gl.findViewById(R.id.setupCode0), (EditText) gl.findViewById(R.id.setupCode1), (EditText) gl.findViewById(R.id.setupCode2),
        (EditText) gl.findViewById(R.id.setupCode3), (EditText) gl.findViewById(R.id.setupCode4), (EditText) gl.findViewById(R.id.setupCode5),
        (EditText) gl.findViewById(R.id.setupCode6), (EditText) gl.findViewById(R.id.setupCode7), (EditText) gl.findViewById(R.id.setupCode8)
    };
    android.app.AlertDialog.Builder builder1 = new android.app.AlertDialog.Builder(getActivity());
    builder1.setView(gl);
    editTexts[0].setText(dcMsg.getSetupCodeBegin());
    editTexts[0].setSelection(editTexts[0].getText().length());

    for( int i = 0; i < 9; i++ ) {
      if( preload != null && i < preload.length ) {
        editTexts[i].setText(preload[i]);
        editTexts[i].setSelection(editTexts[i].getText().length());
      }
      editTexts[i].addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          if( s.length()==4 ) {
            for ( int i = 0; i < 8; i++ ) {
              if( editTexts[i].hasFocus() && editTexts[i+1].getText().length()<4 ) {
                editTexts[i+1].requestFocus();
                break;
              }
            }
          }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
      });
    }

    builder1.setTitle(getActivity().getString(R.string.autocrypt__continue_transfer_title));
    builder1.setMessage(getActivity().getString(R.string.autocrypt__continue_transfer_please_enter_code));
    builder1.setNegativeButton(android.R.string.cancel, null);
    builder1.setCancelable(false); // prevent the dialog from being dismissed accidentally (when the dialog is closed, the setup code is gone forever and the user has to create a new setup message)
    builder1.setPositiveButton(android.R.string.ok, (dialog, which) -> {
      String setup_code = "";
      final String[] preload1 = new String[9];
      for ( int i = 0; i < 9; i++ ) {
        preload1[i] = editTexts[i].getText().toString();
        setup_code += preload1[i];
      }
      boolean success = dcContext.continueKeyTransfer(dcMsg.getId(), setup_code);

      android.app.AlertDialog.Builder builder2 = new android.app.AlertDialog.Builder(getActivity());
      builder2.setTitle(getActivity().getString(R.string.autocrypt__continue_transfer_title));
      builder2.setMessage(getActivity().getString(success? R.string.autocrypt__continue_transfer_succeeded : R.string.autocrypt__continue_transfer_bad_code));
      if( success ) {
        builder2.setPositiveButton(android.R.string.ok, null);
      }
      else {
        builder2.setNegativeButton(android.R.string.cancel, null);
        builder2.setPositiveButton(R.string.autocrypt__continue_button_retry, (dialog1, which1) -> querySetupCode(dcMsg, preload1));
      }
      builder2.show();
    });
    builder1.show();
  }

  private class ConversationFragmentItemClickListener implements ItemClickListener {

    @Override
    public void onItemClick(DcMsg messageRecord) {
      if (actionMode != null) {
        ((ConversationAdapter) list.getAdapter()).toggleSelection(messageRecord);
        list.getAdapter().notifyDataSetChanged();

        if (getListAdapter().getSelectedItems().size() == 0) {
          actionMode.finish();
        } else {
          setCorrectMenuVisibility(actionMode.getMenu());
          actionMode.setTitle(String.valueOf(getListAdapter().getSelectedItems().size()));
        }
      }
      else if(messageRecord.isSetupMessage()) {
        querySetupCode(messageRecord,null);
      }
    }

    @Override
    public void onItemLongClick(DcMsg messageRecord) {
      if (actionMode == null) {
        ((ConversationAdapter) list.getAdapter()).toggleSelection(messageRecord);
        list.getAdapter().notifyDataSetChanged();

        actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(actionModeCallback);
      }
    }

    @Override
    public void onQuoteClicked(DcMsg messageRecord) {
      if (messageRecord.getQuote() == null) {
        Log.w(TAG, "Received a 'quote clicked' event, but there's no quote...");
        return;
      }

      new AsyncTask<Void, Void, Integer>() {
        @Override
        protected Integer doInBackground(Void... voids) {
          if (getActivity() == null || getActivity().isFinishing()) {
            Log.w(TAG, "Task to retrieve quote position started after the fragment was detached.");
            return 0;
          }
          return DatabaseFactory.getMmsSmsDatabase(getContext())
                                .getQuotedMessagePosition(threadId,
                                                          messageRecord.getQuote().getId(),
                                                          messageRecord.getQuote().getAuthor(),
                                                          getListAdapter().getItemCount());
        }

        @Override
        protected void onPostExecute(Integer position) {
          if (getActivity() == null || getActivity().isFinishing()) {
            Log.w(TAG, "Task to retrieve quote position finished after the fragment was detached.");
            return;
          }

          if (position >= 0 && position < getListAdapter().getItemCount()) {
            list.scrollToPosition(position);
            getListAdapter().pulseHighlightItem(position);
          } else {
            Toast.makeText(getContext(), getResources().getText(R.string.ConversationFragment_quoted_message_not_found), Toast.LENGTH_SHORT).show();
            if (position < 0) {
              Log.w(TAG, "Tried to navigate to quoted message, but it was deleted.");
            } else {
              Log.w(TAG, "Tried to navigate to quoted message, but it was out of the bounds of the adapter.");
            }
          }
        }
      }.execute();
    }

    @Override
    public void onSharedContactDetailsClicked(@NonNull Contact contact, @NonNull View avatarTransitionView) {
      if (getContext() != null && getActivity() != null) {
        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), avatarTransitionView, "avatar").toBundle();
        ActivityCompat.startActivity(getActivity(), SharedContactDetailsActivity.getIntent(getContext(), contact), bundle);
      }
    }

    @Override
    public void onAddToContactsClicked(@NonNull Contact contactWithAvatar) {
      if (getContext() != null) {
        new AsyncTask<Void, Void, Intent>() {
          @Override
          protected Intent doInBackground(Void... voids) {
            return ContactUtil.buildAddToContactsIntent(getContext(), contactWithAvatar);
          }

          @Override
          protected void onPostExecute(Intent intent) {
            startActivityForResult(intent, CODE_ADD_EDIT_CONTACT);
          }
        }.execute();
      }
    }

    @Override
    public void onMessageSharedContactClicked(@NonNull List<Recipient> choices) {
      if (getContext() == null) return;

      ContactUtil.selectRecipientThroughDialog(getContext(), choices, locale, recipient -> {
        CommunicationActions.startConversation(getContext(), recipient, null);
      });
    }

    @Override
    public void onInviteSharedContactClicked(@NonNull List<Recipient> choices) {
      if (getContext() == null) return;

      ContactUtil.selectRecipientThroughDialog(getContext(), choices, locale, recipient -> {
        CommunicationActions.composeSmsThroughDefaultApp(getContext(), recipient.getAddress(), getString(R.string.InviteActivity_lets_switch_to_signal, "https://sgnl.link/1KpeYmF"));
      });
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == CODE_ADD_EDIT_CONTACT && getContext() != null) {
      ApplicationContext.getInstance(getContext().getApplicationContext())
                        .getJobManager()
                        .add(new DirectoryRefreshJob(getContext().getApplicationContext(), false));
    }
  }

  private class ActionModeCallback implements ActionMode.Callback {

    private int statusBarColor;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      MenuInflater inflater = mode.getMenuInflater();
      inflater.inflate(R.menu.conversation_context, menu);

      mode.setTitle("1");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Window window = getActivity().getWindow();
        statusBarColor = window.getStatusBarColor();
        window.setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
      }

      setCorrectMenuVisibility(menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      ((ConversationAdapter)list.getAdapter()).clearSelection();
      list.getAdapter().notifyDataSetChanged();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        getActivity().getWindow().setStatusBarColor(statusBarColor);
      }

      actionMode = null;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      switch(item.getItemId()) {
        case R.id.menu_context_copy:
          handleCopyMessage(getListAdapter().getSelectedItems());
          actionMode.finish();
          return true;
        case R.id.menu_context_delete_message:
          handleDeleteMessages(getListAdapter().getSelectedItems());
          actionMode.finish();
          return true;
        case R.id.menu_context_details:
          handleDisplayDetails(getSelectedMessageRecord());
          actionMode.finish();
          return true;
        case R.id.menu_context_forward:
          handleForwardMessage(getSelectedMessageRecord());
          actionMode.finish();
          return true;
        case R.id.menu_context_resend:
          handleResendMessage(getSelectedMessageRecord());
          actionMode.finish();
          return true;
        case R.id.menu_context_save_attachment:
          handleSaveAttachment(getSelectedMessageRecord());
          actionMode.finish();
          return true;
        case R.id.menu_context_reply:
          handleReplyMessage(getSelectedMessageRecord());
          actionMode.finish();
          return true;
      }

      return false;
    }
  }

  private static class ConversationDateHeader extends HeaderViewHolder {

    private final Animation animateIn;
    private final Animation animateOut;

    private boolean pendingHide = false;

    private ConversationDateHeader(Context context, TextView textView) {
      super(textView);
      this.animateIn  = AnimationUtils.loadAnimation(context, R.anim.slide_from_top);
      this.animateOut = AnimationUtils.loadAnimation(context, R.anim.slide_to_top);

      this.animateIn.setDuration(100);
      this.animateOut.setDuration(100);
    }

    public void show() {
      if (pendingHide) {
        pendingHide = false;
      } else {
        ViewUtil.animateIn(textView, animateIn);
      }
    }

    public void hide() {
      pendingHide = true;

      textView.postDelayed(new Runnable() {
        @Override
        public void run() {
          if (pendingHide) {
            pendingHide = false;
            ViewUtil.animateOut(textView, animateOut, View.GONE);
          }
        }
      }, 400);
    }
  }

  @Override
  public void handleEvent(int eventId, Object data1, Object data2) {
    if(eventId== DcContext.DC_EVENT_MSG_DELIVERED) {
      Log.w(TAG, "DC_EVENT_MSG_DELIVERED reveived for msg#"+(Long)data1);
    }
    reloadList();
  }
}
