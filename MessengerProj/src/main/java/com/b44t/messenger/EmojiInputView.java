/*******************************************************************************
 *
 *                              Delta Chat Android
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


package com.b44t.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.b44t.messenger.Components.LayoutHelper;
import com.b44t.messenger.Components.PagerSlidingTabStrip;

import com.amulyakhare.textdrawable.TextDrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class EmojiInputView extends FrameLayout {

    public interface Listener {
        boolean onBackspace();
        void onEmojiSelected(String emoji);
        void onClearEmojiRecent();
    }

    private static final String[][] predefinedEmojis = {
        new String[]{
            "😀", "😬", "😁", "😂", "😃", "😄", "😅", "😆", "😇", "😉", "😊", "🙂", "🙃", "☺", "😋",
            "😌", "😍", "😘", "😗", "😙", "😚", "😜", "😝", "😛", "🤑", "🤓", "😎", "🤗", "😏", "😶",
            "😐", "😑", "😒", "🙄", "🤔", "😳", "😞", "😟", "😠", "😡", "😔", "😕", "🙁", "☹", "😣",
            "😖", "😫", "😩", "😤", "😮", "😱", "😨", "😰", "😯", "😦", "😧", "😢", "😥", "😪", "😓",
            "😭", "😵", "😲", "🤐", "😷", "🤒", "🤕", "😴", "💤", "💩", "😈", "👿", "👹", "👺", "💀",
            "👻", "👽", "🤖", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾", "🙌", "👏", "👋",
            "👍", "👎", "👊", "✊", "✌", "👌", "✋", "👐", "💪", "🙏", "☝", "👆", "👇", "👈", "👉", "🖕", "🖐",
            "🤘", "🖖", "✍", "💅", "👄", "👅", "👂", "👃", "👁", "👀", "👤", "👥", "🗣", "👶", "👦", "👧",
            "👨", "👩", "👱", "👴", "👵", "👲", "👳", "👮", "👷", "💂", "🕵", "🎅", "👼", "👸", "👰", "🚶",
            "🏃", "💃", "👯", "👫", "👬", "👭", "🙇", "💁", "🙅", "🙆", "🙋", "🙎", "🙍", "💇", "💆", "💑",
            "👩‍❤‍👩", "👨‍❤‍👨", "💏", "👩‍❤‍💋‍👩", "👨‍❤‍💋‍👨", "👪", "👨‍👩‍👧", "👨‍👩‍👧‍👦", "👨‍👩‍👦‍👦", "👨‍👩‍👧‍👧",
            "👩‍👩‍👦", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧", "👨‍👨‍👦", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦‍👦",
            "👨‍👨‍👧‍👧", "👚", "👕", "👖", "👔", "👗", "👙", "👘", "💄", "💋", "👣", "👠", "👡", "👢", "👞",
            "👟", "👒", "🎩", "🎓", "👑", "⛑", "🎒", "👝", "👛", "👜", "💼", "👓", "🕶", "💍", "🌂", "❤",
            "💛", "💚", "💙", "💜", "💔", "❣", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
        },
        new String[]{
            "🐶", "🐱", "🐭", "🐹", "🐰", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐙",
            "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🐺", "🐗", "🐴", "🦄", "🐝",
            "🐛", "🐌", "🐞", "🐜", "🕷", "🦂", "🦀", "🐍", "🐢", "🐠", "🐟", "🐡", "🐬", "🐳", "🐋",
            "🐊", "🐆", "🐅", "🐃", "🐂", "🐄", "🐪", "🐫", "🐘", "🐐", "🐏", "🐑", "🐎", "🐖", "🐀",
            "🐁", "🐓", "🦃", "🕊", "🐕", "🐩", "🐈", "🐇", "🐿", "🐾", "🐉", "🐲", "🌵", "🎄", "🌲", "🌳",
            "🌴", "🌱", "🌿", "☘", "🍀", "🎍", "🎋", "🍃", "🍂", "🍁", "🌾", "🌺", "🌻", "🌹", "🌷", "🌼",
            "🌸", "💐", "🍄", "🌰", "🎃", "🐚", "🕸", "🌎", "🌍", "🌏", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒",
            "🌓", "🌔", "🌚", "🌝", "🌛", "🌜", "🌞", "🌙", "⭐", "🌟", "💫", "✨", "☄", "☀", "🌤", "⛅",
            "🌥", "🌦", "☁", "🌧", "⛈", "🌩", "⚡", "🔥", "💥", "❄", "🌨", "☃", "⛄", "🌬", "💨", "🌪",
            "🌫", "☂", "☔", "💧", "💦", "🌊"
        },
        new String[]{
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈", "🍒", "🍑", "🍍", "🍅", "🍆", "🌶",
            "🌽", "🍠", "🍯", "🍞", "🧀", "🍗", "🍖", "🍤", "🍳", "🍔", "🍟", "🌭", "🍕", "🍝", "🌮", "🌯",
            "🍜", "🍲", "🍥", "🍣", "🍱", "🍛", "🍙", "🍚", "🍘", "🍢", "🍡", "🍧", "🍨", "🍦", "🍰", "🎂",
            "🍮", "🍬", "🍭", "🍫", "🍿", "🍩", "🍪", "🍺", "🍻", "🍷", "🍸", "🍹", "🍾", "🍶", "🍵", "☕",
            "🍼", "🍴", "🍽", "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "⛳", "🏌", "🏓", "🏸", "🏒",
            "🏑", "🏏", "🎿", "⛷", "🏂", "⛸", "🏹", "🎣", "🚣", "🏊", "🏄", "🛀", "⛹", "🏋", "🚴", "🚵",
            "🏇", "🕴", "🏆", "🎽", "🏅", "🎖", "🎗", "🏵", "🎫", "🎟", "🎭", "🎨", "🎪", "🎤", "🎧", "🎼",
            "🎹", "🎷", "🎺", "🎸", "🎻", "🎬", "🎮", "👾", "🎯", "🎲", "🎰", "🎳", "⌚", "📱", "📲", "💻",
            "⌨", "🖥", "🖨", "🖱", "🖲", "🕹", "🗜", "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥",
            "📽", "🎞", "📞", "☎", "📟", "🎛", "⏱", "⏲", "⏰", "🕰", "⏳", "⌛", "📡", "🔋", "🔌", "💡",
            "🔦", "🕯", "🗑", "🛢", "💸", "💵", "💴", "💶", "💷", "💰", "💳", "💎", "⚖", "🔧", "🔨", "⚒",
            "🛠", "⛏", "🔩", "⚙", "⛓", "🔫", "💣", "🔪", "🗡", "⚔", "🛡", "🚬", "☠", "⚰", "⚱", "🏺",
            "🔮", "📿", "💈", "⚗", "🔭", "🔬", "🕳", "💊", "💉", "🌡", "🏷", "🔖", "🚽", "🚿", "🛁", "🔑",
            "🗝", "🛋", "🛌", "🛏", "🚪", "🛎", "🖼", "🗺", "⛱", "🗿", "🛍", "🎈", "🎏", "🎀", "🎁",
            "🎊", "🎉", "🎎", "🎐", "🎌", "🏮", "✉", "📩", "📨", "📧", "💌", "📮", "📪", "📫", "📬", "📭",
            "📦", "📯", "📥", "📤", "📜", "📃", "📑", "📊", "📈", "📉", "📄", "📅", "📆", "🗓", "📇", "🗃",
            "🗳", "🗄", "📋", "🗒", "📁", "📂", "🗂", "🗞", "📰", "📓", "📕", "📗", "📘", "📙", "📔", "📒",
            "📚", "📖", "🔗", "📎", "🖇", "✂", "📐", "📏", "📌", "📍", "🚩", "🏳", "🏴", "🔐", "🔒", "🔓",
            "🔏", "🖊", "🖋", "✒", "📝", "✏", "🖍", "🖌", "🔍", "🔎",
        },
        new String[]{
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎", "🚓", "🚑", "🚒", "🚐", "🚚", "🚛", "🚜", "🏍", "🚲",
            "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃", "🚋", "🚝", "🚄", "🚅", "🚈", "🚞", "🚂",
            "🚆", "🚇", "🚊", "🚉", "🚁", "🛩", "✈", "🛫", "🛬", "⛵", "🛥", "🚤", "⛴", "🛳", "🚀", "🛰",
            "💺", "⚓", "🚧", "⛽", "🚏", "🚦", "🚥", "🏁", "🚢", "🎡", "🎢", "🎠", "🏗", "🌁", "🗼", "🏭",
            "⛲", "🎑", "⛰", "🏔", "🗻", "🌋", "🗾", "🏕", "⛺", "🏞", "🛣", "🛤", "🌅", "🌄", "🏜",
            "🏖", "🏝", "🌇", "🌆", "🏙", "🌃", "🌉", "🌌", "🌠", "🎇", "🎆", "🌈", "🏘", "🏰", "🏯", "🏟",
            "🗽", "🏠", "🏡", "🏚", "🏢", "🏬", "🏣", "🏤", "🏥", "🏦", "🏨", "🏪", "🏫", "🏩", "💒", "🏛",
            "⛪", "🕌", "🕍", "🕋", "⛩", "🇦🇺", "🇦🇹", "🇦🇿", "🇦🇽", "🇦🇱", "🇩🇿", "🇦🇸", "🇦🇮",
            "🇦🇴", "🇦🇩", "🇦🇶", "🇦🇬", "🇦🇷", "🇦🇲", "🇦🇼", "🇦🇫", "🇧🇸", "🇧🇩", "🇧🇧", "🇧🇭",
            "🇧🇾", "🇧🇿", "🇧🇪", "🇧🇯", "🇧🇲", "🇧🇬", "🇧🇴", "🇧🇶", "🇧🇦", "🇧🇼", "🇧🇷", "🇮🇴",
            "🇧🇳", "🇧🇫", "🇧🇮", "🇧🇹", "🇻🇺", "🇻🇦", "🇬🇧", "🇭🇺", "🇻🇪", "🇻🇬", "🇻🇮", "🇹🇱",
            "🇻🇳", "🇬🇦", "🇭🇹", "🇬🇾", "🇬🇲", "🇬🇭", "🇬🇵", "🇬🇹", "🇬🇳", "🇬🇼", "🇩🇪", "🇬🇬",
            "🇬🇮", "🇭🇳", "🇭🇰", "🇬🇩", "🇬🇱", "🇬🇷", "🇬🇪", "🇬🇺", "🇩🇰", "🇯🇪", "🇩🇯", "🇩🇲",
            "🇩🇴", "🇪🇺", "🇪🇬", "🇿🇲", "🇪🇭", "🇿🇼", "🇮🇱", "🇮🇳", "🇮🇩", "🇯🇴", "🇮🇶", "🇮🇷",
            "🇮🇪", "🇮🇸", "🇪🇸", "🇮🇹", "🇾🇪", "🇨🇻", "🇰🇿", "🇰🇾", "🇰🇭", "🇨🇲", "🇨🇦", "🇮🇨",
            "🇶🇦", "🇰🇪", "🇨🇾", "🇰🇬", "🇰🇮", "🇨🇳", "🇰🇵", "🇨🇨", "🇨🇴", "🇰🇲", "🇨🇬", "🇨🇩",
            "🇽🇰", "🇨🇷", "🇨🇮", "🇨🇺", "🇰🇼", "🇨🇼", "🇱🇦", "🇱🇻", "🇱🇸", "🇱🇷", "🇱🇧", "🇱🇾",
            "🇱🇹", "🇱🇮", "🇱🇺", "🇲🇺", "🇲🇷", "🇲🇬", "🇾🇹", "🇲🇴", "🇲🇰", "🇲🇼", "🇲🇾", "🇲🇱",
            "🇲🇻", "🇲🇹", "🇲🇦", "🇲🇶", "🇲🇭", "🇲🇽", "🇫🇲", "🇲🇿", "🇲🇩", "🇲🇨", "🇲🇳", "🇲🇸",
            "🇲🇲", "🇳🇦", "🇳🇷", "🇳🇵", "🇳🇪", "🇳🇬", "🇳🇱", "🇳🇮", "🇳🇺", "🇳🇿", "🇳🇨", "🇳🇴",
            "🇮🇲", "🇳🇫", "🇨🇽", "🇸🇭", "🇨🇰", "🇹🇨", "🇦🇪", "🇴🇲", "🇵🇰", "🇵🇼", "🇵🇸", "🇵🇦",
            "🇵🇬", "🇵🇾", "🇵🇪", "🇵🇳", "🇵🇱", "🇵🇹", "🇵🇷", "🇰🇷", "🇷🇪", "🇷🇺", "🇷🇼", "🇷🇴",
            "🇸🇻", "🇼🇸", "🇸🇲", "🇸🇹", "🇸🇦", "🇸🇿", "🇲🇵", "🇸🇨", "🇧🇱", "🇵🇲", "🇸🇳", "🇻🇨",
            "🇰🇳", "🇱🇨", "🇷🇸", "🇸🇬", "🇸🇽", "🇸🇾", "🇸🇰", "🇸🇮", "🇺🇸", "🇸🇧", "🇸🇴", "🇸🇩",
            "🇸🇷", "🇸🇱", "🇹🇯", "🇹🇭", "🇹🇼", "🇹🇿", "🇹🇬", "🇹🇰", "🇹🇴", "🇹🇹", "🇹🇻", "🇹🇳",
            "🇹🇲", "🇹🇷", "🇺🇬", "🇺🇿", "🇺🇦", "🇼🇫", "🇺🇾", "🇫🇴", "🇫🇯", "🇵🇭", "🇫🇮", "🇫🇰",
            "🇫🇷", "🇬🇫", "🇵🇫", "🇹🇫", "🇭🇷", "🇨🇫", "🇹🇩", "🇲🇪", "🇨🇿", "🇨🇱", "🇨🇭", "🇸🇪",
            "🇱🇰", "🇪🇨", "🇬🇶", "🇪🇷", "🇪🇪", "🇪🇹", "🇿🇦", "🇬🇸", "🇸🇸", "🇯🇲", "🇯🇵"
        },
        new String[]{
            "💟", "☮", "✝", "☪", "🕉", "☸", "✡", "🔯", "🕎", "☯", "☦", "🛐", "⛎", "♈", "♉", "♊", "♋",
            "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "🆔", "⚛", "🈳", "🈹", "☢", "☣", "📴", "📳", "🈶",
            "🈚", "🈸", "🈺", "🈷", "✴", "🆚", "🉑", "💮", "🉐", "㊙", "㊗", "🈴", "🈵", "🈲", "🅰", "🅱",
            "🆎", "🆑", "🅾", "🆘", "⛔", "📛", "🚫", "❌", "⭕", "💢", "♨", "🚷", "🚯", "🚳", "🚱", "🔞",
            "📵", "❗", "❕", "❓", "❔", "‼", "⁉", "💯", "🔅", "🔆", "🔱", "⚜", "〽", "⚠", "🚸", "🔰", "♻",
            "🈯", "💹", "❇", "✳", "❎", "✅", "💠", "🌀", "➿", "🌐", "Ⓜ", "🏧", "🈂", "🛂", "🛃", "🛄",
            "🛅", "♿", "🚭", "🚾", "🅿", "🚰", "🚹", "🚺", "🚼", "🚻", "🚮", "🎦", "📶", "🈁", "🆖", "🆗",
            "🆙", "🆒", "🆕", "🆓", "0⃣", "1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣", "🔟", "🔢", "▶",
            "⏸", "⏯", "⏹", "⏺", "⏭", "⏮", "⏩", "⏪", "🔀", "🔁", "🔂", "◀", "🔼", "🔽", "⏫", "⏬", "➡",
            "⬅", "⬆", "⬇", "↗", "↘", "↙", "↖", "↕", "↔", "🔄", "↪", "↩", "⤴", "⤵", "#⃣", "*⃣", "ℹ",
            "🔤", "🔡", "🔠", "🔣", "🎵", "🎶", "〰", "➰", "✔", "🔃", "➕", "➖", "➗", "✖", "💲", "💱",
            "©", "®", "™", "🔚", "🔙", "🔛", "🔝", "🔜", "☑", "🔘", "⚪", "⚫", "🔴", "🔵", "🔸", "🔹",
            "🔶", "🔷", "🔺", "▪", "▫", "⬛", "⬜", "🔻", "◼", "◻", "◾", "◽", "🔲", "🔳", "🔈", "🔉", "🔊",
            "🔇", "📣", "📢", "🔔", "🔕", "🃏", "🀄", "♠", "♣", "♥", "♦", "🎴", "👁‍🗨", "💭", "🗯", "💬",
            "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛", "🕜", "🕝", "🕞", "🕟",
            "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦", "🕧"
        }
    };

    private ArrayList<EmojiGridAdapter>    adapters = new ArrayList<>();
    private HashMap<String, Integer>       recentUsageCounts = new HashMap<>();
    private ArrayList<String>              recentChars = new ArrayList<>();

    private Listener            listener;
    private ViewPager           pager;
    private FrameLayout         recentsWrap;
    private ArrayList<GridView> views = new ArrayList<>();
    private ImageView           backspaceButton;
    private LinearLayout        pagerSlidingTabStripContainer;

    private int                 oldWidth;
    private int                 lastNotifyWidth;

    private boolean             backspacePressed;
    private boolean             backspaceOnce;

    public EmojiInputView(final Context context) {
        super(context);

        for (int i = 0; i < predefinedEmojis.length + 1/*add one for the recent page*/; i++) {
            GridView gridView = new GridView(context);
            gridView.setColumnWidth(AndroidUtilities.dp(45));
            gridView.setNumColumns(-1);
            views.add(gridView);

            EmojiGridAdapter emojiGridAdapter = new EmojiGridAdapter(i - 1); // -1: recent, >=0: predefined
            gridView.setAdapter(emojiGridAdapter);
            adapters.add(emojiGridAdapter);
        }

        setBackgroundColor(0xfff5f6f7);

        pager = new ViewPager(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        pager.setAdapter(new EmojiPagerAdapter());

        pagerSlidingTabStripContainer = new LinearLayout(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onInterceptTouchEvent(ev);
            }
        };
        pagerSlidingTabStripContainer.setOrientation(LinearLayout.HORIZONTAL);
        pagerSlidingTabStripContainer.setBackgroundColor(0xfff5f6f7);
        addView(pagerSlidingTabStripContainer, LayoutHelper.createFrame(LayoutParams.MATCH_PARENT, 48));

        PagerSlidingTabStrip pagerSlidingTabStrip = new PagerSlidingTabStrip(context);
        pagerSlidingTabStrip.setViewPager(pager);
        pagerSlidingTabStrip.setShouldExpand(true);
        pagerSlidingTabStrip.setIndicatorHeight(AndroidUtilities.dp(2));
        pagerSlidingTabStrip.setUnderlineHeight(AndroidUtilities.dp(1));
        pagerSlidingTabStrip.setIndicatorColor(0xff2b96e2);
        pagerSlidingTabStrip.setUnderlineColor(0xffe2e5e7);
        pagerSlidingTabStripContainer.addView(pagerSlidingTabStrip, LayoutHelper.createLinear(0, 48, 1.0f));

        FrameLayout frameLayout = new FrameLayout(context);
        pagerSlidingTabStripContainer.addView(frameLayout, LayoutHelper.createLinear(52, 48));

        backspaceButton = new ImageView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    backspacePressed = true;
                    backspaceOnce = false;
                    postBackspaceRunnable(350);
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                    backspacePressed = false;
                    if (!backspaceOnce) {
                        if (listener != null && listener.onBackspace()) {
                            backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        }
                    }
                }
                super.onTouchEvent(event);
                return true;
            }
        };
        backspaceButton.setImageResource(R.drawable.ic_smiles_backspace);
        backspaceButton.setBackgroundResource(R.drawable.ic_emoji_backspace);
        backspaceButton.setScaleType(ImageView.ScaleType.CENTER);
        frameLayout.addView(backspaceButton, LayoutHelper.createFrame(52, 48));

        View view = new View(context);
        view.setBackgroundColor(0xffe2e5e7);
        frameLayout.addView(view, LayoutHelper.createFrame(52, 1, Gravity.START | Gravity.BOTTOM));

        recentsWrap = new FrameLayout(context);
        recentsWrap.addView(views.get(0));

        TextView textView = new TextView(context);
        textView.setText(context.getString(R.string.NoRecentEmoji));
        textView.setTextSize(18);
        textView.setTextColor(0xff888888);
        textView.setGravity(Gravity.CENTER);
        recentsWrap.addView(textView);
        views.get(0).setEmptyView(textView);

        addView(pager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.TOP, 0, 48, 0, 0));

        loadRecentUsageCounts();
        sortRecentEmoji();
        adapters.get(0).notifyDataSetChanged();

        // normally, we open the emoji-input-keyboard with the most recent page (index 0)
        // if there are no most recent emojis, open the first page (index 1) which contains smilies then
        if( recentChars.size() == 0 ) {
            pager.setCurrentItem(1);
        }
    }

    private void postBackspaceRunnable(final int time) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!backspacePressed) {
                    return;
                }
                if (listener != null && listener.onBackspace()) {
                    backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                backspaceOnce = true;
                postBackspaceRunnable(Math.max(50, time - 100));
            }
        }, time);
    }

    public void clearRecent() {
        recentUsageCounts.clear();
        recentChars.clear();
        saveRecentUsageCounts();
        adapters.get(0).notifyDataSetChanged();
    }

    private void saveRecentUsageCounts() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        StringBuilder stringBuilder = new StringBuilder();
        for (HashMap.Entry<String, Integer> entry : recentUsageCounts.entrySet()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
        }
        preferences.edit().putString("emojis", stringBuilder.toString()).apply();
    }

    public void loadRecentUsageCounts() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        String str;
        try {
            recentUsageCounts.clear();
            str = preferences.getString("emojis", "");
            if (str.length() > 0) {
                String[] args = str.split(",");
                for (String arg : args) {
                    String[] args2 = arg.split("=");
                    recentUsageCounts.put(args2[0], Utilities.parseInt(args2[1]));
                }
            }
        } catch (Exception e) {

        }
    }

    private void sortRecentEmoji() {
        recentChars.clear();
        for (HashMap.Entry<String, Integer> entry : recentUsageCounts.entrySet()) {
            recentChars.add(entry.getKey());
        }
        Collections.sort(recentChars, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                Integer count1 = recentUsageCounts.get(lhs);
                Integer count2 = recentUsageCounts.get(rhs);
                if (count1 == null) {
                    count1 = 0;
                }
                if (count2 == null) {
                    count2 = 0;
                }
                if (count1 > count2) {
                    return -1;
                } else if (count1 < count2) {
                    return 1;
                }
                return 0;
            }
        });
        while (recentChars.size() > 50) {
            recentChars.remove(recentChars.size() - 1);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) pagerSlidingTabStripContainer.getLayoutParams();
        layoutParams.width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (layoutParams.width != oldWidth) {
            pagerSlidingTabStripContainer.setLayoutParams(layoutParams);
            oldWidth = layoutParams.width;
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (lastNotifyWidth != right - left) {
            lastNotifyWidth = right - left;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setListener(Listener value) {
        listener = value;
    }

    public void invalidateViews() {
        for (GridView gridView : views) {
            if (gridView != null) {
                gridView.invalidateViews();
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != GONE) {
            sortRecentEmoji();
            adapters.get(0).notifyDataSetChanged();
        }
    }

    private class SingleEmojiView extends ImageView {

        public SingleEmojiView(Context context) {
            super(context);

            setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    sendEmoji();
                }
            });
            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (pager.getCurrentItem() == 0) {
                        listener.onClearEmojiRecent();
                    }
                    return false;
                }
            });
            setBackgroundResource(R.drawable.list_selector);
            setScaleType(ImageView.ScaleType.CENTER);
        }

        private void sendEmoji() {
            String code = (String) getTag();

            Integer count = recentUsageCounts.get(code);
            if (count == null) {
                count = 0;
            }
            if (count == 0 && recentUsageCounts.size() > 50) {
                for (int a = recentChars.size() - 1; a >= 0; a--) {
                    String emoji = recentChars.get(a);
                    recentUsageCounts.remove(emoji);
                    recentChars.remove(a);
                    if (recentUsageCounts.size() <= 50) {
                        break;
                    }
                }
            }
            recentUsageCounts.put(code, ++count);
            if (pager.getCurrentItem() != 0) {
                sortRecentEmoji();
                adapters.get(0).notifyDataSetChanged();
                views.get(0).invalidateViews();
            }
            saveRecentUsageCounts();
            if (listener != null) {
                listener.onEmojiSelected(fixEmoji(code));
            }
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(widthMeasureSpec));
        }
    }

    private class EmojiGridAdapter extends BaseAdapter {

        private int emojiPage; // -1: recent, >= 0: predefined from predefinedEmojis

        public EmojiGridAdapter(int page) {
            emojiPage = page;
        }

        public int getCount() {
            int ret = emojiPage == -1? recentChars.size() :  predefinedEmojis[emojiPage].length;
            return ret;
        }

        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int i, View view, ViewGroup paramViewGroup) {
            SingleEmojiView imageView = (SingleEmojiView) view;
            if (imageView == null) {
                imageView = new SingleEmojiView(getContext());
            }
            String code;
            String coloredCode;
            if (emojiPage == -1) {
                coloredCode = code = recentChars.get(i);
            } else {
                coloredCode = code = predefinedEmojis[emojiPage][i];
            }

            // Draw native Emojis using TextDrawable, not imageView.setImageDrawable(Emoji.getEmojiBigDrawable(code)). See:
            // https://github.com/Jamesits/Moegram/commit/8e52c6222cf00bc9a86d52fed4c06558f48c345c
            // and https://github.com/amulyakhare/TextDrawable
            imageView.setTag(code);
            int bigImgSize;
            bigImgSize = AndroidUtilities.dp(32);
            imageView.setImageDrawable(TextDrawable.builder().beginConfig().textColor(Color.BLACK).fontSize(bigImgSize).endConfig().buildRect(coloredCode, Color.TRANSPARENT));
            return imageView;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

    private class EmojiPagerAdapter extends PagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        public void destroyItem(ViewGroup viewGroup, int position, Object object) {
            View view;
            if (position == 0) {
                view = recentsWrap;
            } else {
                view = views.get(position);
            }
            viewGroup.removeView(view);
        }

        public int getCount() {
            return views.size();
        }

        public int getPageIconResId(int position) {
            int[] icons = {
                    R.drawable.ic_emoji_recent,
                    R.drawable.ic_emoji_smile,
                    R.drawable.ic_emoji_flower,
                    R.drawable.ic_emoji_bell,
                    R.drawable.ic_emoji_car,
                    R.drawable.ic_emoji_symbol};
            return icons[position>=0 && position<icons.length? position : 0];
        }

        public Object instantiateItem(ViewGroup viewGroup, int position) {
            View view;
            if (position == 0) {
                view = recentsWrap;
            } else {
                view = views.get(position);
            }
            viewGroup.addView(view);
            return view;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

    private static final char[] insertVariationSelector = {
        0x2B50, 0x2600, 0x26C5, 0x2601, 0x26A1, 0x2744, 0x26C4, 0x2614, 0x2708, 0x26F5, 0x2693,
        0x26FD, 0x26F2, 0x26FA, 0x26EA, 0x2615, 0x26BD, 0x26BE, 0x26F3, 0x231A, 0x260E, 0x231B,
        0x2709, 0x2702, 0x2712, 0x270F, 0x2648, 0x2649, 0x264A, 0x264B, 0x264C, 0x264D, 0x264E,
        0x264F, 0x2650, 0x2651, 0x2652, 0x2653, 0x2734, 0x3299, 0x3297, 0x26D4, 0x2B55, 0x2668,
        0x2757, 0x203C, 0x2049, 0x303D, 0x26A0, 0x267B, 0x2747, 0x2733, 0x24C2, 0x267F, 0x25B6,
        0x25C0, 0x27A1, 0x2B05, 0x2B06, 0x2B07, 0x2197, 0x2198, 0x2199, 0x2196, 0x2195, 0x2194,
        0x21AA, 0x21A9, 0x2934, 0x2935, 0x2139, 0x2714, 0x2716, 0x2611, 0x26AA, 0x26AB, 0x25AA,
        0x25AB, 0x2B1B, 0x2B1C, 0x25FC, 0x25FB, 0x25FE, 0x25FD, 0x2660, 0x2663, 0x2665, 0x2666,
        0x263A, 0x2639, 0x270C, 0x261D, 0x2764
    };
    private static final HashMap<Character, Boolean> insertVariationSelectorMap = new HashMap<>(insertVariationSelector.length);
    static {
        for (int a = 0; a < insertVariationSelector.length; a++) {
            insertVariationSelectorMap.put(insertVariationSelector[a], true);
        }
    }
    private static String fixEmoji(String emoji) {
        char ch;
        int lenght = emoji.length();
        for (int a = 0; a < lenght; a++) {
            ch = emoji.charAt(a);
            if (ch >= 0xD83C && ch <= 0xD83E) {
                if (ch == 0xD83C && a < lenght - 1) {
                    ch = emoji.charAt(a + 1);
                    if (ch == 0xDE2F || ch == 0xDC04 || ch == 0xDE1A || ch == 0xDD7F) {
                        emoji = emoji.substring(0, a + 2) + "\uFE0F"/*VARIATION SELECTOR-16*/ + emoji.substring(a + 2);
                        lenght++;
                        a += 2;
                    } else {
                        a++;
                    }
                } else {
                    a++;
                }
            } else if (ch == 0x20E3 /*COMBINING ENCLOSING KEYCAP*/) {
                return emoji;
            } if (insertVariationSelectorMap.containsKey(ch)) {
                emoji = emoji.substring(0, a + 1) + "\uFE0F"/*VARIATION SELECTOR-16*/ + emoji.substring(a + 1);
                lenght++;
                a++;
            }
        }
        return emoji;
    }

    public static CharSequence replaceEmoji(CharSequence cs, boolean createNew) {
        if (cs == null || cs.length() == 0) {
            return cs;
        }
        Spannable s;
        if (!createNew && cs instanceof Spannable) {
            s = (Spannable) cs;
        } else {
            s = Spannable.Factory.getInstance().newSpannable(cs.toString());
        }

        return s;
    }
}
