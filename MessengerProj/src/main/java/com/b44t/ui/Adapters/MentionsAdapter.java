/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.UserObject;
import com.b44t.messenger.support.widget.RecyclerView;
import com.b44t.messenger.ConnectionsManager;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Cells.ContextLinkCell;
import com.b44t.ui.Cells.MentionCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MentionsAdapter extends BaseSearchAdapterRecycler {

    public interface MentionsAdapterDelegate {
        void needChangePanelVisibility(boolean show);
    }

    public class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }

    private Context mContext;
    //private long dialog_id;
    private TLRPC.ChatFull info;
    private ArrayList<TLRPC.User> searchResultUsernames;
    private ArrayList<String> searchResultHashtags;
    private ArrayList<String> searchResultCommands;
    private ArrayList<String> searchResultCommandsHelp;
    private ArrayList<TLRPC.User> searchResultCommandsUsers;
    private MentionsAdapterDelegate delegate;
    private int resultStartPosition;
    private boolean allowNewMentions = true;
    private int resultLength;
    private String lastText;
    private int lastPosition;
    private ArrayList<MessageObject> messages;
    private boolean needUsernames = true;
    private boolean isDarkTheme;

    private int contextUsernameReqid;
    private int contextQueryReqid;
    private Runnable contextQueryRunnable;
    //private Location lastKnownLocation;

    private BaseFragment parentFragment;

    /*
    private SendMessagesHelper.LocationProvider locationProvider = new SendMessagesHelper.LocationProvider(new SendMessagesHelper.LocationProvider.LocationProviderDelegate() {
        @Override
        public void onLocationAcquired(Location location) {
            if (foundContextBot != null && foundContextBot.bot_inline_geo) {
                lastKnownLocation = location;
                //searchForContextBotResults(foundContextBot, searchingContextQuery, "");
            }
        }

        @Override
        public void onUnableLocationAcquire() {
            onLocationUnavailable();
        }
    }) {
        @Override
        public void stop() {
            super.stop();
            lastKnownLocation = null;
        }
    };
    */

    public MentionsAdapter(Context context, boolean darkTheme, long did, MentionsAdapterDelegate mentionsAdapterDelegate) {
        mContext = context;
        delegate = mentionsAdapterDelegate;
        isDarkTheme = darkTheme;
        //dialog_id = did;
    }

    public void onDestroy() {
        /*if (locationProvider != null) {
            locationProvider.stop();
        }*/
        if (contextQueryRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(contextQueryRunnable);
            contextQueryRunnable = null;
        }
        if (contextUsernameReqid != 0) {
            ConnectionsManager.getInstance().cancelRequest(contextUsernameReqid, true);
            contextUsernameReqid = 0;
        }
        if (contextQueryReqid != 0) {
            ConnectionsManager.getInstance().cancelRequest(contextQueryReqid, true);
            contextQueryReqid = 0;
        }
    }

    public void setAllowNewMentions(boolean value) {
        allowNewMentions = value;
    }

    public void setParentFragment(BaseFragment fragment) {
        parentFragment = fragment;
    }

    public void setChatInfo(TLRPC.ChatFull chatParticipants) {
        info = chatParticipants;
        if (lastText != null) {
            searchUsernameOrHashtag(lastText, lastPosition, messages);
        }
    }

    public void setNeedUsernames(boolean value) {
        needUsernames = value;
    }

    @Override
    public void clearRecentHashtags() {
        super.clearRecentHashtags();
        searchResultHashtags.clear();
        notifyDataSetChanged();
        if (delegate != null) {
            delegate.needChangePanelVisibility(false);
        }
    }

    @Override
    protected void setHashtags(ArrayList<HashtagObject> arrayList, HashMap<String, HashtagObject> hashMap) {
        super.setHashtags(arrayList, hashMap);
        if (lastText != null) {
            searchUsernameOrHashtag(lastText, lastPosition, messages);
        }
    }

    /*
    private void onLocationUnavailable() {
        if (foundContextBot != null && foundContextBot.bot_inline_geo) {
            lastKnownLocation = new Location("network");
            lastKnownLocation.setLatitude(-1000);
            lastKnownLocation.setLongitude(-1000);
            searchForContextBotResults(foundContextBot, searchingContextQuery, "");
        }
    }
    */

    /*
    private void checkLocationPermissionsOrStart() {
        if (parentFragment == null || parentFragment.getParentActivity() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && parentFragment.getParentActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            parentFragment.getParentActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            return;
        }
        if (foundContextBot != null && foundContextBot.bot_inline_geo) {
            locationProvider.start();
        }
    }
    */

    public void searchUsernameOrHashtag(String text, int position, ArrayList<MessageObject> messageObjects) {
        if (text == null || text.length() == 0) {
            //searchForContextBot(null, null);
            delegate.needChangePanelVisibility(false);
            lastText = null;
            return;
        }
        int searchPostion = position;
        if (text.length() > 0) {
            searchPostion--;
        }
        lastText = null;
        StringBuilder result = new StringBuilder();
        int foundType = -1;
        boolean hasIllegalUsernameCharacters = false;
        int dogPostion = -1;
        for (int a = searchPostion; a >= 0; a--) {
            if (a >= text.length()) {
                continue;
            }
            char ch = text.charAt(a);
            if (a == 0 || text.charAt(a - 1) == ' ' || text.charAt(a - 1) == '\n') {
                if (ch == '@') {
                    if (needUsernames /*|| needBotContext && a == 0*/ ) {
                        if (hasIllegalUsernameCharacters) {
                            delegate.needChangePanelVisibility(false);
                            return;
                        }
                        if (info == null && a != 0) {
                            lastText = text;
                            lastPosition = position;
                            messages = messageObjects;
                            delegate.needChangePanelVisibility(false);
                            return;
                        }
                        dogPostion = a;
                        foundType = 0;
                        resultStartPosition = a;
                        resultLength = result.length() + 1;
                        break;
                    }
                } else if (ch == '#') {
                    if (!hashtagsLoadedFromDb) {
                        lastText = text;
                        lastPosition = position;
                        messages = messageObjects;
                        delegate.needChangePanelVisibility(false);
                        return;
                    }
                    foundType = 1;
                    resultStartPosition = a;
                    resultLength = result.length() + 1;
                    result.insert(0, ch);
                    break;
                } /*else if (a == 0 && botInfo != null && ch == '/') {
                    foundType = 2;
                    resultStartPosition = a;
                    resultLength = result.length() + 1;
                    break;
                }*/
            }
            if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_')) {
                hasIllegalUsernameCharacters = true;
            }
            result.insert(0, ch);
        }
        if (foundType == -1) {
            delegate.needChangePanelVisibility(false);
            return;
        }
        if (foundType == 0) {
            final ArrayList<Integer> users = new ArrayList<>();
            for (int a = 0; a < Math.min(100, messageObjects.size()); a++) {
                int from_id = messageObjects.get(a).messageOwner.from_id;
                if (!users.contains(from_id)) {
                    users.add(from_id);
                }
            }
            String usernameString = result.toString().toLowerCase();
            ArrayList<TLRPC.User> newResult = new ArrayList<>();
            final HashMap<Integer, TLRPC.User> newResultsHashMap = new HashMap<>();
            /*if (needBotContext && dogPostion == 0 && !SearchQuery.inlineBots.isEmpty()) {
                int count = 0;
                for (int a = 0; a < SearchQuery.inlineBots.size(); a++) {
                    TLRPC.User user = MessagesController.getInstance().getUser(SearchQuery.inlineBots.get(a).peer.user_id);
                    if (user == null) {
                        continue;
                    }
                    if (user.username != null && user.username.length() > 0 && (usernameString.length() > 0 && user.username.toLowerCase().startsWith(usernameString) || usernameString.length() == 0)) {
                        newResult.add(user);
                        newResultsHashMap.put(user.id, user);
                        count++;
                    }
                    if (count == 5) {
                        break;
                    }
                }
            }*/
            if (info != null && info.participants != null) {
                for (int a = 0; a < info.participants.participants.size(); a++) {
                    TLRPC.ChatParticipant chatParticipant = info.participants.participants.get(a);
                    TLRPC.User user = MessagesController.getInstance().getUser(chatParticipant.user_id);
                    if (user == null || UserObject.isUserSelf(user) || newResultsHashMap.containsKey(user.id)) {
                        continue;
                    }
                    if (usernameString.length() == 0) {
                        if ( (allowNewMentions || !allowNewMentions && user.username != null && user.username.length() != 0)) {
                            newResult.add(user);
                        }
                    } else {
                        if (user.username != null && user.username.length() > 0 && user.username.toLowerCase().startsWith(usernameString)) {
                            newResult.add(user);
                        } else {
                            if (!allowNewMentions && (user.username == null || user.username.length() == 0)) {
                                continue;
                            }
                            if (user.first_name != null && user.first_name.length() > 0 && user.first_name.toLowerCase().startsWith(usernameString)) {
                                newResult.add(user);
                            } else if (user.last_name != null && user.last_name.length() > 0 && user.last_name.toLowerCase().startsWith(usernameString)) {
                                newResult.add(user);
                            }
                        }
                    }
                }
            }
            searchResultHashtags = null;
            searchResultCommands = null;
            searchResultCommandsHelp = null;
            searchResultCommandsUsers = null;
            searchResultUsernames = newResult;
            Collections.sort(searchResultUsernames, new Comparator<TLRPC.User>() {
                @Override
                public int compare(TLRPC.User lhs, TLRPC.User rhs) {
                    if (newResultsHashMap.containsKey(lhs.id) && newResultsHashMap.containsKey(rhs.id)) {
                        return 0;
                    } else if (newResultsHashMap.containsKey(lhs.id)) {
                        return -1;
                    } else if (newResultsHashMap.containsKey(rhs.id)) {
                        return 1;
                    }
                    int lhsNum = users.indexOf(lhs.id);
                    int rhsNum = users.indexOf(rhs.id);
                    if (lhsNum != -1 && rhsNum != -1) {
                        return lhsNum < rhsNum ? -1 : (lhsNum == rhsNum ? 0 : 1);
                    } else if (lhsNum != -1 && rhsNum == -1) {
                        return -1;
                    } else if (lhsNum == -1 && rhsNum != -1) {
                        return 1;
                    }
                    return 0;
                }
            });
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!newResult.isEmpty());
        } else if (foundType == 1) {
            ArrayList<String> newResult = new ArrayList<>();
            String hashtagString = result.toString().toLowerCase();
            for (int a = 0; a < hashtags.size(); a++) {
                HashtagObject hashtagObject = hashtags.get(a);
                if (hashtagObject != null && hashtagObject.hashtag != null && hashtagObject.hashtag.startsWith(hashtagString)) {
                    newResult.add(hashtagObject.hashtag);
                }
            }
            searchResultHashtags = newResult;
            searchResultUsernames = null;
            searchResultCommands = null;
            searchResultCommandsHelp = null;
            searchResultCommandsUsers = null;
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!newResult.isEmpty());
        } else if (foundType == 2) {
            ArrayList<String> newResult = new ArrayList<>();
            ArrayList<String> newResultHelp = new ArrayList<>();
            ArrayList<TLRPC.User> newResultUsers = new ArrayList<>();
            String command = result.toString().toLowerCase();
            /*for (HashMap.Entry<Integer, TLRPC.BotInfo> entry : botInfo.entrySet()) {
                TLRPC.BotInfo botInfo = entry.getValue();
                for (int a = 0; a < botInfo.commands.size(); a++) {
                    TLRPC.TL_botCommand botCommand = botInfo.commands.get(a);
                    if (botCommand != null && botCommand.command != null && botCommand.command.startsWith(command)) {
                        newResult.add("/" + botCommand.command);
                        newResultHelp.add(botCommand.description);
                        newResultUsers.add(MessagesController.getInstance().getUser(botInfo.user_id));
                    }
                }
            }*/
            searchResultHashtags = null;
            searchResultUsernames = null;
            searchResultCommands = newResult;
            searchResultCommandsHelp = newResultHelp;
            searchResultCommandsUsers = newResultUsers;
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!newResult.isEmpty());
        }
    }

    public int getResultStartPosition() {
        return resultStartPosition;
    }

    public int getResultLength() {
        return resultLength;
    }

    @Override
    public int getItemCount() {
        /*if (searchResultBotContext != null) {
            return searchResultBotContext.size() + (searchResultBotContextSwitch != null ? 1 : 0);
        } else */ if (searchResultUsernames != null) {
            return searchResultUsernames.size();
        } else if (searchResultHashtags != null) {
            return searchResultHashtags.size();
        } else if (searchResultCommands != null) {
            return searchResultCommands.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        /*if (searchResultBotContext != null) {
            if (position == 0 && searchResultBotContextSwitch != null) {
                return 2;
            }
            return 1;
        } else */ {
            return 0;
        }
    }

    public Object getItem(int i) {
        /*if (searchResultBotContext != null) {
            if (searchResultBotContextSwitch != null) {
                if (i == 0) {
                    return searchResultBotContextSwitch;
                } else {
                    i--;
                }
            }
            if (i < 0 || i >= searchResultBotContext.size()) {
                return null;
            }
            return searchResultBotContext.get(i);
        } else */ if (searchResultUsernames != null) {
            if (i < 0 || i >= searchResultUsernames.size()) {
                return null;
            }
            return searchResultUsernames.get(i);
        } else if (searchResultHashtags != null) {
            if (i < 0 || i >= searchResultHashtags.size()) {
                return null;
            }
            return searchResultHashtags.get(i);
        } else if (searchResultCommands != null) {
            if (i < 0 || i >= searchResultCommands.size()) {
                return null;
            }
            if (searchResultCommandsUsers != null /*&& (botsCount != 1 || info instanceof TLRPC.TL_channelFull)*/) {
                if (searchResultCommandsUsers.get(i) != null) {
                    return String.format("%s@%s", searchResultCommands.get(i), searchResultCommandsUsers.get(i) != null ? searchResultCommandsUsers.get(i).username : "");
                } else {
                    return String.format("%s", searchResultCommands.get(i));
                }
            }
            return searchResultCommands.get(i);
        }
        return null;
    }

    public boolean isLongClickEnabled() {
        return searchResultHashtags != null || searchResultCommands != null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = new ContextLinkCell(mContext);
            ((ContextLinkCell) view).setDelegate(new ContextLinkCell.ContextLinkCellDelegate() {
                @Override
                public void didPressedImage(ContextLinkCell cell) {
                    //delegate.onContextClick(null);
                }
            });
        //} else if (viewType == 2) {
        //    view = new BotSwitchCell(mContext);
        } else {
            view = new MentionCell(mContext);
            ((MentionCell) view).setIsDarkTheme(isDarkTheme);
        }
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        /*if (searchResultBotContext != null) {
            boolean hasTop = searchResultBotContextSwitch != null;
            if (holder.getItemViewType() == 2) {
                if (hasTop) {
                    ((BotSwitchCell) holder.itemView).setText(searchResultBotContextSwitch.text);
                }
            } else {
                if (hasTop) {
                    position--;
                }
                ((ContextLinkCell) holder.itemView).setLink(searchResultBotContext.get(position), contextMedia, position != searchResultBotContext.size() - 1, hasTop && position == 0);
            }
        } else*/ {
            if (searchResultUsernames != null) {
                ((MentionCell) holder.itemView).setUser(searchResultUsernames.get(position));
            } else if (searchResultHashtags != null) {
                ((MentionCell) holder.itemView).setText(searchResultHashtags.get(position));
            } else if (searchResultCommands != null) {
                ((MentionCell) holder.itemView).setBotCommand(searchResultCommands.get(position), searchResultCommandsHelp.get(position), searchResultCommandsUsers != null ? searchResultCommandsUsers.get(position) : null);
            }
        }
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 2) {
            /*
            if (foundContextBot != null && foundContextBot.bot_inline_geo) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationProvider.start();
                } else {
                    onLocationUnavailable();
                }
            }
            */
        }
    }
}
