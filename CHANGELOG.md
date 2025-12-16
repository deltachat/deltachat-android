# Delta Chat Android Changelog

## v2.34.0
2025-12

* Allow to add relay from clipboard or image if camera permission is not granted
* Avoid crash in the "View Log" screen
* Update to core 2.34.0

## v2.33.1
2025-12

* Target Android 16
* Change color of links in text messages
* Improve edge-to-edge support
* Add the option (opt-in) to send anonymous statistics to Delta Chat's developers
* Metadata protection: protect message recipients
* Allow to withdraw channel invite links and QR codes
* Allow to open externally links clicked inside in-chat apps
* Do not show "1 member" when the process of joining the group is not finished
* Make search case-insensitive for non-ASCII chat and contact names
* Improve handling of video recoding
* Send .webm videos as file, they are not supported by all platforms
* Tweak advanced section and wording of some advanced options
* Fix: avoid crash in push notifications handling
* Fix: avoid freezing in background
* Fix: clean up web storage of deleted in-chat apps
* Fix: avoid crash when exporting some files with wrong image MIME type
* Expose new "Multi-device mode" option instead of "Delete from server" for chatmail profiles
* Opened in-chat apps got a 'About Apps' menu item
* Avoid gray avatar on profile creation
* Avoid last item in chat list being covered by the floating button
* Add disk usage statistics to log
* New experimental feature: several addresses per profile
* Update to core 2.33.0

## v2.25.0
2025-11

* Make it possible to invite members into a channel via a QR code,
  and make channels more secure
* metadata protection: protect Date header
* metadata protection: protect Autocrypt header
* better multi-device: synchronize group creation across devices
* data saving: do not send Autocrypt header in read receipts
* improve onboarding speed
* allow to save to storage files shared from inside in-chat apps
* reduce app size
* don't show badge counter in app icon for the permanent background notification
* fix sorting of old media in gallery
* fix text direction in "x members" subtitle for RTL languages
* fix group invite QR screen's layout
* tweak text hints in advanced classic e-mail configuration
* remove deprecated "companion app" code
* remove deprecated "Watch Sent Folder" preference
* remove deprecated "send self-report" preference
* don't show email address in shared vcard
* update to core 2.25.0

## v2.22.0
2025-10

* target Android 15
* improve readability of info messages in dark mode
* drop too short disappearing messages options
* fix Direct Share shortcuts
* fix: don't show error message when cancelling profile creation
* enable permanent notification by default if push notifications are not available
* hide "clone chat" and member list for incoming channels
* show warning if background notifications will be unreliable
* warn if the app has not been updated after 6 months instead of 1 year
* avoid "unknown sender for this chat" error
* properly display "Messages are end-to-end encrypted." in all encrypted groups
* show dialog if user has permanently denied camera permission and tries to take picture for group avatar
* several small fixes and improvements
* add experimental built-in calls
* update to core 2.22.0

## v2.11.0
2025-08

* add "After 1 year" option to disappearing messages
* improve image quality when setting group avatars
* add Estonian translation, update other translations
* allow to clone email chats
* fix some small bugs
* update to core 2.11.0

## v2.10.0
2025-08

* fix "Archived" item's layout in chat-list
* don't enlarge "Saved Messages" and "Devices Messages" avatars on click
* share email address for email contacts instead of vCard
* open existing encrypted chat when opening a mailto link or clicking an email address in a message bubble
* update to core 2.10.0

## v2.9.0
2025-07

* hide contact email addresses in search results
* disable non-functional message editing and ephemeral messages timer settings in classic email thread chat
* don't enlarge email chats avatar placeholder
* improve message date/status footer layout, also in RTL languages
* display correct text when receiving a "Disappearing messages enabled" system message
* Update to core 2.9.0

## v2.8.0
2025-07

* Profiles focus on recognizing contacts
* See the number of media directly in the profile, no need to tap around
* Clearer app lists by removing redundant "App" subtitle
* New button for quick access to the apps sent in current chat
* New icon for the in-chat apps button
* Improve hint for app drafts
* Add Text-To-Speech (TTS) support for in-chat apps
* New icon for the QR icon
* Start rebuilding the experimental broadcast lists
  into proper channels - note that this is work-in-progress
* Improved separation between unencrypted chats/contacts and encrypted ones, avoiding mixing of encrypted and unencrypted messages in the same chat
* Removed padlocks, as encrypted is the default "normal" state. Instead, unencrypted email is marked with a small email / letter  (✉️) icon
* Classic email chats/threads get a big email / letter icon making it easy to recognize
* After some time, add a device message asking to donate. Can't wait? Donate today at https://delta.chat/donate
* Allow to sort profiles up in the profile switcher
* Add new option to create unencrypted email thread
* Green checkmarks are removed where they mostly refer to guaranteed encryption, which is the default now. They are still used for profile's "Introduced by"
* Update to core 2.8.0

## v1.58.4
2025-05

* make in-chat apps properly work when they are not sent yet, in draft mode
* better avatar quality
* some more bug fixes and updated translations
* update to core 1.159.5

## v1.58.3
2025-05

* fix: webxdc.selfName uses the name otherwise displayed
* fix potential crash on startup
* add donation link to app settings
* update to core 1.159.3

## v1.58.2
2025-04

* fix draft writing area disappearing for some chats
* update to core 1.159.2

## v1.58.1
2025-04

* tapping info messages with contacts open the contact's profile
* hide superfluous "Show Classic E-mails" advanced setting for chatmail
* show profile bio/status under name in main settings screen
* remove mostly non-telling transport addresses when referring to a contact;
  the contact's profile gives a much better overview
* Disable AEAP to enable us to overhaul some things - there are big changes underway in this area, which will come in a few months
* don't display email address in contact list and member list for contacts with green-checkmark
* avoid crash in Notifications preferences if ringtone title can't be read
* don't display forwarded messages as "edited" if original message was edited
* support importing contact from ProtonMail vCard attachments received in chats
* send encrypted in the experimental broadcast lists feature
* wait for QR scan (or invite link click) process to complete before allowing to send messages
* show connectivity status dot when profile is connecting or not connected
* never send Autocrypt-Gossip in broadcast lists.
* update to core 1.159.1

## v1.56.1
2025-03

* ignore click in info-messages from deleted in-chat apps
* data saving: do not send messages to the server if user is the only member of the chat in single-device usage
* protect metadata: encrypt message's sent date
* do not fail to send messages in groups if some encryption keys are missing
* synchronize contact name changes across devices
* fix changing group names that was not working in some situations
* fix: do not show outdated message text in "Message Info" of an edited message
* some more small bug fixes and updated translations
* update to core 1.158.0

## v1.56.0
2025-03

* allow to edit messages
* allow to delete messages for everyone
* add mute option "8 hours"
* add menu option to easily save/unsave selected message
* improve deletion confirmation for "Device Messages"
* remove dangerous encryption options
* always paste as plain text in message draft area
* some small bug fixes and updated translations
* update to core 1.157.2

## v1.54.4
2025-03

* allow better avatar (profile picture) quality
* remove notifications from chat that was deleted from other device
* when a chat is deleted, also delete its messages from server
* avoid freezing when opening the app for the first time after install
* avoid crash when adding chat shortcut to home screen
* some small bug fixes and updated translations
* update to core 1.156.3

## v1.54.3
2025-03

* allow to add any chat to the home screen
* update "forward message" icon and organize the messages actions bar
* do not allow non-members to change ephemeral timer settings of groups
* properly display padlock when the message is not sent over the network
* sync message deletion to other devices
* sync chat deletion across devices
* Show sender in "Saved Messages"
* allow scanning multiple QR-invitation codes without needing to wait for completion to scan the next one
* when reactions are seen in one device, remove notification from your other devices
* don't disturb with notification when someone leave a group
* detect incompatible profiles from newer app version when importing them
* prepare the app for receiving edited messages
* prepare the app for receiving message deletion requests
* do some small bug fixes
* update translations
* update to core 1.156.2

## v1.54.0
2025-02

* enhanced "Saved Messages" feature, now when forwarding a message to "Saved Messages" chat, it retains the sender information and a button to jump to the original message
* Saved messages are marked by a bookmark sign
* improve explanation when blocking a contact
* improve wording in empty "apps" and "files" tabs in chat media screen
* remove deprecated/legacy built-in "half-camera"
* UI improvement: keep avatars aligned to message bubble when message has reactions
* fix problems when opening attachments in external apps
* fix a bug with some big images appearing as blank/transparent
* some other small bug fixes
* update translations
* update to core 1.155.4

## v1.52.1
2025-01

* the app now requires less storage on your SD card by deduplicating newly received/sent files
* some small bug fixes
* update translations
* update to core 1.155.1

## v1.52.0
2025-01

* new group consistency algorithm
* fix: don't show animated .webp stickers as static stickers
* fix the chat shortcuts (created via long-press in launcher) to properly support multi-profile
* fix some small bugs in certain android versions and special situations
* avoid the app freezing in slow phones in some situations
* improve menu in the help screen
* update translations
* update to core 1.155.0

## v1.50.5
2025-01

* fix push-notifications handling for certain devices where it was not working correctly
* update translations
* using core 1.153.0

## v1.50.4
2025-01

* properly send as animated stickers GIF files selected from keyboard
* improve emoji picker in landscape mode and when changing from landscape to portrait
* avoid crash when receiving push notifications if the user restricted the app from working in background
* improve UI when attaching a file or image to easily recognize it is attached but not sent yet
* avoid slow loading of in-chat apps in some devices when quickly re-opening an app after closing it
* allow to select multiple images at once in the media picker via "Gallery" button
* mark holiday notice messages as bot-generated
* don't mark contacts as bot when receiving location-only and sync messages
* prefer to encrypt even if peers have their preference to "no preference"
* start ephemeral messages timers when the chat is archived or noticed
* several bug fixes and updated translations
* update to core 1.153.0

## v1.50.3
2024-12

* Add in-chat apps picker to attachments options
* Notify replies and reactions to your messages in muted chats (can be disabled in settings)
* Cache HTTP GET requests (ex. when loading images from HTML messages)
* update to core 1.152.0

## v1.50.2
2024-12

* Encrypt notification tokens
* update to core 1.151.5

## v1.50.0
2024-12

* New emoji picker with support for more emojis
* Webxdc apps can now trigger notifications
* Webxdc apps can now deep-link to internal sections when you click their info-messages in chat
* Add "Show in Chat" to the menu of opened Webxdc apps
* Reverse order of messages in the notification group
* Notify reactions to own messages
* Improve the button to start Webxdc apps
* Make account deletion confirmation dialog faster
* Rename "Back up Chats to External Storage" to "Export Backup"
* Improve compatibility with classic email clients in the outgoing messages
* Removed internal font scaling setting in favor of the better system settings
* Use privacy-preserving webxdc addresses
* Use Rustls for connections with strict TLS
* QR codes for adding contacts and joining groups provide help when opened in a normal browser
* Mark Saved Messages chat as protected
* Allow the user to replace maps integration
* fix: Trim whitespace from scanned QR codes
* fix quotes: Line-before-quote may be up to 120 character long instead of 80
* fix: Prevent accidental wrong-password-notifications
* fix: Remove footers from "Show Full Message..."
* fix: Only add "member added/removed" messages if they actually do that
* fix: Update state of message when fully downloading it
* fix: send message: Do not fail if the message does not exist anymore
* fix: Do not percent-encode dot when passing to autoconfig server (so, fix handling of some servers)
* fix displaynames not being updated when intially scanned by a QR code
* several bug fixes
* update to core 1.151.3

## v1.48.3
2024-10

* new Proxy settings screen available at "Advanced / Proxy"
* manage a list of HTTP(S), SOCKS5 or Shadowsocks Proxies
* Proxies icon shown on the chatlist if proxies are used
* share Proxies by showing a QR code
* scan Proxies' QR code and use them
* make Proxy URLs inside Delta Chat tappable
* open Delta Chat when tapping Proxy URLs in other apps
* support for realtime webxdc apps moved out of experimental and enabled by default
* realtime webxdc apps can be disabled at "Settings / Advanced"
* "New Contact / Link" button to view, share or copy the invite line
* "New Contact / Scan" button to easier access the scanner functionality
* open "New Contact" scan/show activities directly, do not try to be too smart and open the last active tab
* allow to attach multiple images in one step
* to easier differ between multiple profiles, set a "Private Tag" (long tap profile switcher)
* "Private Tag" is shown in notifications
* improve profile deletion dialog: show name, size and avatar of the profile being deleted
* show profile name in title bar when the user has multiple profiles
* improve profile switcher layout
* improve notification: allow to "Mark Read" from the notification
* search for unread chats in the search's three-dot-menu
* allow pasting QR codes from "Add As Second Device" screen
* save traffic by supporting "IMAP COMPRESS"
* automatic reconfiguration, e.g. switching to implicit TLS if STARTTLS port stops working
* parallelize IMAP and SMTP connection attempts
* improve DNS caching
* always use preloaded DNS results
* prioritize cached results if DNS resolver returns many results
* always move auto-generated messages to DeltaChat folder
* ignore invalid securejoin messages silently
* delete messages from a chatmail server immediately by default
* make resending pending messages possible
* don't SMTP-send messages to self-chat if BccSelf is disabled
* HTTP(S) tunneling
* don't put displayname into From/To/Sender if it equals to address
* hide sync messages from INBOX (use IMAP APPEND command to upload sync messages)
* more verbose SMTP connection establishment errors
* add "Learn More" button to "Manage keys"
* visual feedback when tapping the action button of a message
* log unexpected message state when resending fails
* smoother backup and "Add Second Device" progress bars
* assign messages to ad-hoc group with matching name and members
* use stricter TLS checks for HTTPS downloads (images in HTML mails, Autoconfig)
* improve logging for failed QR code scans, AEAP, Autocrypt, notification permissions and sending errors
* improve logging of multi account setup (log account ID)
* show more context for the "Cannot establish guaranteed..." info message
* show file name in "Message Info"
* show root SMTP connection failure in connectivity view
* fix: Sort received outgoing message down if it's fresher than all non fresh messages
* fix: avoid app being killed when processing a PUSH notification
* fix crash when refreshing avatar
* fix crash in gallery
* fix: shorten message text in locally sent messages too
* fix: Set http I/O timeout to 1 minute rather than whole request timeout
* fix: don't sync QR code token before populating the group
* fix: do not get stuck if the message to download does not exist anymore
* fix: do not attempt to reference info messages
* fix: do not get stuck if there is an error transferring backup
* fix: make it possible to cancel ongoing backup transfer
* fix: reset quota when entering a new address
* fix: better detection of file extensions
* fix: "database locked" errors
* fix: never initialize realtime channels if realtime is disabled
* fix reception of realtime channels
* fix: normalize proxy URLs
* fix connections getting stuck in "Updating..." sometimes
* fix scanning "add second device" QR code from scanner above chatlist
* fix warning about wrong password
* fix app getting stale when receiving a PUSH notifications takes longer
* fix app getting stale on network changes
* fix: skip IDLE if we got unsolicited FETCH
* update translations and local help
* update to core 1.148.6


## v1.46.14
2024-09

* add monochrome/themed launcher icon support
* allow to remove the selected profile in "Switch Profile" dialog
* improve display of selected profile in "Switch Profile" dialog
* improve the hit/tap area to open "Switch Profile" dialog in the main screen's toolbar
* add support for system per-app language and remove in-app language selector
* remove the experimental "encrypt database" checkbox in classic registration screen
* fix various bugs
* update to core 1.142.12


## v1.46.13
2024-08

* improve contact profile's "Edit Name" dialog
* upgrade the status bar to modern Android look and feel
* add direct support for android14, required to be able to continue shipping to Google Play
* increase minimal supported android to 5; as required by updating several outdated dependencies
* drop support for gmail oauth2, gmail can still be used using "App Passwords", you'll get hints as needed
  (reason for dropping was unmaintainable bureaucracy and costs added by google)
* update various dependencies for added security and stability
* jcenter (a dependency origin) is closing, move dependencies to other origins
* when SOCKS5 is enabled, route autoconfig and oauth2 config there
* fix encryption compatibility with old Delta Chat clients
* fix crashes when opening log view with many lines
* fix: hide copy to clipboard while QR is not ready
* fix moving outgoing auto-generated messages to the "DeltaChat" folder
* fix: try to create "INBOX.DeltaChat" if "DeltaChat" is not possible for some provider
* fix receiving messages with "DeltaChat" folder cannot be selected
* fix: do not crash on unknown "Certificate Checks" values
* update provider database
* update to core 1.142.8


## v1.46.10
2024-08

* mark bots in chat titles and profiles as such
* if the experimental videochat is enabled, invitations can be sent via the "Attach" menu now
* show potentially dangerous buttons with red color
* focus on name and profile images in reaction details; the address is available on tap
* focus on name and profile image in profile switcher; the address is still shown for classic e-mail profiles
* add device message about new placement of "Switch Profile" if more than one profile is in use before update
* update translations
* using core 1.142.2


## v1.46.8
2024-08

* "Share Contact" directly from a contact's profile
* add "Share Invite Link" to "New Contact" screen
* add "Invite Friends" to main menu
* cleanup "Profile Switcher", long tap to delete profiles
* "Mute Notifications" via a long tap directly from "Profile Switcher"
* search non-english messages case-insensitive
* display attached contact's names in summaries and quotes
* protect From: and To: metadata where possible
* do not reveal sender's language metadata in read receipts
* allow importing contacts exported by Proton Mail
* for chatmail profile, hide error prone "add contact manually" in favor to invite links
* automatically expand "Password and Account / Advanced" if there were advanced options set before
* show potentially dangerous menu entries with red color
* remove "Switch Profile" from main menu, as this very often used option causes confusion with finger memory and other menus;
  instead, just tap your profile image in the upper left corner to add or to switch profiles
* prevent creating contact without encryption in chatmail profiles via mailto:-links
* no unarchiving of groups on member removal messages
* improve caching of DNS results
* focus on name for QR code titles
* report first error instead of the last on connection failure
* long tap email address in contact's profile for copying to clipboard
* fix battery drain due to endless IMAP loop
* fix: remove push notification toggle, it is not needed as raised false expectations
* fix: keep "chatmail" state after failed reconfiguration
* fix issues with failed backup imports
* fix: avoid group creation on member removal messages
* fix downloading partially downloaded messages
* fix various networking bugs
* update translations and local help
* update to core 1.142.2


## v1.46.7
2024-07

* add option to mark all selected chats as being "Read" (long tap a chat to start select mode)
* new, single-device chatmail profiles default to "Delete Messages after Download"
* when using a chatmail profile on multiple devices, deletion is changed to "Automatic"
  (deletion strategy is up to the server then)
* fix back-button behaviour in the welcome screen
* update translations and local help
* using core 1.140.2


## v1.46.5
2024-06

* support webxdc apps with experimental realtime channels ("Settings / Advanced / Realtime Webxdc Channels")
* fewer traffic in larger chatmail groups by allowing more than 50 recipients per time
* log debug level (mostly foreign modules) only if "Settings / Advanced / Developer Mode" is enabled
* fix: avoid asking to disable battery optimisations when creating the second profile
* fix hangs on low/no network during onboarding
* fix: cancel muting does not cancel selection in chatlist
* fix migrated address losing verified status and key on experimental AEAP
* fix: allow creation of groups by outgoing messages without recipients
* fix: avoid group splits by preferring ID from encrypted header over references for new groups
* fix: do not fail to send images with wrong extensions
* fix: retry sending MDNs on temporary error
* fix: do not miss new messages while expunging the folder
* fix missing logging info lines
* fix: remove group member locally even if sending fails
* fix: revert group member addition if the corresponding message couldn't be sent
* update translations and local help
* update to core 1.140.2


## v1.46.3
2024-06

* Disable FCM PUSH notification support for F-Droid and other non-Google-Play-builds
* using core 1.139.5


## v1.46.2
2024-06

* fix: create new profile when scanning/tapping QR codes outside "Add Profile"
* update translations and local help
* using core 1.139.5


## v1.46.1
2024-05

* new onboarding: you can create a new profile with one tap on "Create New Profile" -
  or use an existing login or second-device-setup as usual
* use FCM PUSH notification if supported by providers (as chatmail) and by the operating system
* do not ask for disabling "battery optimisations" when PUSH notifications are working
* add an option to disable PUSH notifications
* contacts can be attached as "Cards" at "Attach / Contact";
  when the receiver taps the cards, guaranteed end-to-end encrypted can be established
* "Profiles" are names as such throughout the app;
  note that these profiles exist on the device only, there is nothing persisted on the server
* adding contacts manually at "New Chat / New Contact / Add Contact Manually"
* send any emoji as reaction
* show reactions in summaries
* nicer summaries by using some emojis for attachment types
* pin/archive/etc chats directly from search result
* new map for - still experimental - location streaming (enable at "Settings / Advanced")
* ask for system unlock before accessing "password & account"
* advanced settings resorted, you'll also find "password & account" and "show classic emails" there
* improve resilience by adding references to the last three messages
* one-to-one chats are read-only during reasonable run of securejoin
* if securejoin is taking longer than expected, a warning is shown and messages can be sent
* improve resilience by including more entries in DNS fallback cache
* improve anonymous mailing lists by not adding hostname to Message-ID
* harden share-to-delta
* add second device's troubleshooting is always available offline now
* hide folder options if not supported by the used account
* allow to view password (after entering system secret)
* device update message is added as unread only for the first account
* share log to other chats or apps
* use colors for info/warning/error in the log
* fix: preserve upper-/lowercase of links from HTML-messages
* fix: rescan folders on "Watch Sent Folder" changes
* fix sometimes wrong sender name in "Message Info"
* fix: do not send avatar in securejoin messages before contact verification
* fix: avoid being re-added to groups just left
* fix: do not auto-delete webxdc apps that have recent updates
* fix: improve moving messages on gmail
* fix: improve chat assignments of not downloaded messages
* fix: do not create ad-hoc groups from partial downloads
* fix: improve connectivity on startup by adding backoff for IMAP connections
* fix: mark contact request messages as seen on IMAP server
* fix: convert images to RGB8 before encoding into JPEG to fix sending of large RGBA images
* fix showing large PNG files
* fix: do not convert large GIF to JPEG
* fix receiving Autocrypt Setup Messages from K-9
* fix: delete expired locations and POIs with deleted chats
* fix: send locations more reliable
* fix: use last known location if it is recent enough
* fix: do not fail to send encrypted quotes to unencrypted chats, replace quote by "..." instead
* fix: always use correct "Saved Messages" icon when the chat is recreated
* fix: add white background to transparent avatars
* fix crashes when exporting or importing huge accounts
* fix: remove leading whitespace from subject
* fix problem with sharing the same key by several accounts
* fix busy looping eg. during key import
* fix remote group membership changes always overriding local ones
* fix hint when adding a webxdc shortcut to the home page
* fix webxdc links for securejoin
* fix sending uncompressed images (bug introduced in beta 1.45 beta series)
* fix: hide not useful menu options in the QR screens
* fix scanning invite codes from the "New Chat" screen
* fix: use the last header of multiple ones with the same name; this is the one DKIM was using
* fix migration of legacy databases
* fix: on onboarding, keep entered name and avatar when scanning QR codes or going for other options
* fix broken "..." ellipsis for small screens
* fix: do not mark the message with locations as seen
* fix startup crash on android4
* fix location streaming crash introduced in 1.45 beta
* update translations and local help
* update to core 1.139.5


## v1.44.0
2024-03

* sync self-avatar and self-signature text across devices
* remove webxdc sending limit
* recognize "Trash" folder by name in case it is not flagged as such by the server
* send group avatars inline so that they do not appear as unexpected attachments
* "Settings / Advanced / Send statistics to Delta Chat's developers"
  now include number of protected/encrypted/unencrypted chats
* fix sending sync messages on updating self-name etc.
* fix sometimes slow reconnects
* more bug fixes
* update translations and local help
* update to core 1.136.2


## v1.43.1 Testrun
2024-02

* add "Settings / Advanced / Send statistics to Delta Chat's developers" to draft a message with statistic;
  the message is only sent if the user hits the "Send" button
* add device message if outgoing messages are undecryptable
* "Settings / Advanced / Read System Address Book" is remembered per-account
* add link to troubleshooting for "Add as Second Device" on welcome screen and update troubleshooting
* fix compatibility issue with 1.42 when using "Add Second Device" or backups
* fix sometimes mangled links
* fix sometimes wrongly marked gossiped keys
* fix: guarantee immediate message deletion if "Delete Messages from Server" is set to "At once"
* fix: Never allow a message timestamp to be a lot in the future
* fix: make IMAP folder handling more resilient
* update translations and local help
* update to core 1.135.0


## v1.43.0 Testrun
2024-02

* add "Reactions": long tap a message to react to it ❤️
* reactions from others are shown below the messages
* tap a reaction below a message to get reaction details
* sharing QR code now shares "Invite Link":
  if tapped by with Delta Chat users, Delta Chat opens; otherwise the browser opens;
  the server does not get any information about the link details (as "Fragment" is not sent to server)
* copying/pasting QR code data now also supports invite links
* when using multiple accounts,
  the avatar in the upper left corner now shows the number of unread messages in other account
* updated "welcome message" now focuses about how to get in contact
* add meaningful info message if provider does not allow unencrypted messages
* long-tapping chatlist items now allow to mute/unmute chats directly
* ask for system unlock secret before opening "Password & Account"
* add 'Learn More' to ephemeral messages dialog
* mark data as being "fragile", supporting systems now allows the data to be kept, making reinstalls easier
* new option "Settings / Advanced / Read System Address Book":
  when enabled, the address book addresses are added to the "New Chat" activity
* faster reconnects when switching from a bad or offline network to a working network
* add "From:" to protected headers for signed-only messages generated by some apps
* sync user actions for ad-hoc groups across devices
* sync contact creation/rename across devices
* encrypt read receipts
* only try to configure non-strict TLS checks if explicitly set
* accept i.delta.chat as well as openpgp4fpr: links
* force a display name to be set when using an instant onboarding QR code
* focus on name and state for guaranteed e2ee chats; email address and other data are available in the profile
* improve navigation on account creation by adding a title and a back button to the welcome screen
* fix: delete resent messages on receiver side
* fix: do not drop unknown report attachments, such as TLS reports
* fix: be graceful with systems mangling the qr-code-date (macOS, iOS)
* fix unexpected line breaks in messages (by using Quoted-Printable MIME)
* fix: avoid retry sending for servers not returning a response code in time (force BCC-self)
* fix partially downloaded messages getting stuck in "Downloading..."
* fix inconsistent QR scan states (track forward and backward verification separately, mark 1:1 chat as verified as early as possible)
* fix duplicated messages for some providers as "QQ Mail"
* fix: do not remove contents from unencrypted Schleuder mailing lists messages
* fix: reset message error when scheduling resending
* fix marking some one-to-one chats as guaranteed
* fix: avoid multiple resending of messages on slow SMTP servers
* fix: more reliable connectivity information
* fix: delete received outgoing messages from SMTP queue
* fix timestamp of guaranteed e2ee info message for correct message ordering after backup restore
* fix: add padlock to empty part if the whole message is empty
* fix IDLE timeout renewal on keepalives and reduce it to 5 minutes
* fix: fail fast on LIST errors to avoid busy loop when connection is lost
* fix: improve checking if all members of a chat are verified
* fix: same "green checkmark" message order on all platforms
* fix CI by increasing TCP timeouts from 30 to 60 seconds
* update translations and local help
* update to core 1.134.0


## v1.42.6
2023-11

* sync changes on "Your Profile Name", "Show Class Mails", "Read Receipts" options across devices
* remove receiver limit on .xdc size
* fix decryption errors when using multiple private keys
* fix more log in errors for providers as 163.com; this was introduced in 1.41
* fix: database locked errors on webxdc updates
* update translations and local help
* update to core 1.131.9


## v1.42.4
2023-11

* fix battery draining due to active IMAP loop on some providers; this was introduced in 1.41
* fix log in error on some providers as 163.com; this was introduced in 1.41
* fix "Learn More" buttons that opened the help always in english
* update local help
* update to core 1.131.7


## v1.42.3
2023-11

* fix: avoid infinite loop by failing fast on IMAP FETCH parsing errors
* update translations
* update to core 1.131.6


## v1.42.2
2023-11

* fix contact creation using outdated names sometimes
* fix: do not replace the message with an error in square brackets
  when the sender is not a member of the protected group
* fix: compare addresses on QR code scans and at similar places case-insensitively
* fix: normalize addresses to lower case to catch unrecoverable typos and other rare errors
* fix: fetch contact addresses in a single query
* fix: sync chat name to other devices
* update translations and local help
* update to core 1.131.5


## v1.42.1
2023-11

* fix "Member added" message not being a system message sometimes
* update translations and local help
* update to core 1.131.4


## v1.42.0
2023-11

* fix download button shown when download could be decrypted
* using core 1.131.3


## v1.41.9 Testrun
2023-11

* fix missing messages because of misinterpreted server responses (ignore EOF on FETCH)
* fix: re-gossip keys if a group member changed setup
* fix: skip sync when chat name is set to the current one
* fix: ignore unknown sync items to provide forward compatibility
  and to avoid creating empty message bubbles in "Saved Messages"
* update translations and local help
* update to core 1.131.3


## v1.41.8 Testrun
2023-11

* use local help for guaranteed end-to-end encryption "Learn More" links
* do not post "NAME verified" messages on QR scan success
* improve system message wording
* fix: allow to QR scan groups when 1:1 chat with the inviter is a contact request
* fix: add "Setup Changed" message before the message
* fix: read receipts created or unblock 1:1 chats sometimes
* add Vietnamese translation, update other translations and local help
* update to core 1.131.2


## v1.41.7 Testrun
2023-11

* synchronize "Broadcast Lists" (experimental) across devices
* add "Scan QR Code" button to "New Chat / New Contact" dialog
* fix: do not skip actual message parts when group change messages are inserted
* fix broken chat names (encode names in the List-ID to avoid SMTPUTF8 errors)
* update translations
* update to core 1.131.1


## v1.41.6 Testrun
2023-11

* simplify adding new contacts: "New Chat / Add Contact" button is now always present
* add a QR icon beside the "Show QR invite code" option
* add info messages about implicitly added members
* improve handling of various partly broken encryption states by adding a secondary verified key
* fix: mark 1:1 chat as protected when joining a group
* fix: raise lower auto-download limit to 160k
* fix: remove Reporting-UA from read receipt
* fix: do not apply group changes to special chats; avoid adding members to trashed chats
* fix: protect better against duplicate UIDs reported by IMAP servers
* fix more cases for the accidentally hidden title bar on android14
* update provider database
* update translations
* update to core 1.130.0


## v1.41.5 Testrun
2023-11

* sync Accept/Blocked, Archived, Pinned and Mute across devices
* add "group created instructions" as info message to new chats
* clone group in the group's profile menu
* add hardcoded fallback DNS cache
* improve group creation and make it more obvious that a group is created
* auto-detect if a group with guaranteed end-to-end encryption can be created
* more graceful ratelimit for .testrun.org subdomains
* faster message detection on the server
* fix accidentally hidden title bar on android14
* fix: more reliable group consistency by always automatically downloading messages up to 160k
* fix: properly abort backup process if there is some failure
* fix: make sure, a QR scan succeeds if there is some leftover from a previously broken scan
* fix: allow other guaranteed e2ee group recipients to be unverified, only check the sender verification
* fix: switch to "Mutual" encryption preference on a receipt of encrypted+signed message
* fix hang in receiving messages when accidentally going IDLE
* fix: allow verified key changes via "member added" message
* fix: partial messages do not change group state
* fix: don't implicitly delete members locally, add absent ones instead
* update translations
* update to core 1.129.1


## v1.41.3 Testrun
2023-10

* allow to export all backups together
* "New Group" offers to create verified groups if all members are verified
* verified groups: show all contacts when adding members and explain how to verify unverified ones
* "QR Invite Code" is available after group creation in the group's profile
* update translations
* using core 1.127.2


## v1.41.2 Testrun
2023-10

* guarantee end-to-end-encryption in one-to-one chats, if possible
* if end-to-end-encryption cannot be guaranteed eg. due to key changes,
  the chat requires a confirmation of the user
* "verified groups" are no longer experimental
* backup filenames include the account name now
* "Broadcast Lists" (experimental) create their own chats on the receiver site
* tapping the title bar always opens account switcher; from there you can open connectivity
* add "Deactivate QR code" option when showing QR codes
  (in addition to deactivate and reactivate QR codes by scanning them)
* show name and e-mail address of verifiers
* fix stale app on configuration screen if DNS is not available
* fix: keep showing old email address if configuring a new one fails
* fix starting chats from the system's phone app (by improving mailto: handling)
* fix unresponsiveness when opening "Connectivity View" when offline
* fix configure error with "Winmail Pro Mail Server"
* fix: set maximal memory usage for the internal database
* fix: allow setting a draft if verification is broken
* fix joining verified group via QR if contact is not already verified
* fix: sort old incoming messages below all outgoing ones
* fix: do not mark non-verified group chats as verified when using securejoin
* fix: show only chats where we can send to on forwarding or sharing
* fix: improve removing accounts in case the filesystem is busy
* fix: don't show a contact as verified if their key changed since the verification
* update translations
* update to core 1.127.2


## v1.41.1 Testrun
2023-10

* tweak action bar color in dark mode
* fix asking for permissions on Android 11; these bugs were introduced by 1.41.0
* fix chatlist showing sometimes chats from other accounts after clicking notifications
* fix crash when clicking a notification sometimes
* fix crash when selecting a background image sometimes
* fix long-taps on audio message's controls in multi-select mode
* fix dark mode's color of "encrypt" checkbox in welcome screen
* fix sorting error with downloaded manually messages
* fix group creation when the initial group message is downloaded manually
* fix connectivity status view for servers not supporting IMAP IDLE
* fix: don't try to send more read receipts if there's a temporary SMTP error
* fix "Verified by" information showing an error instead of the verifier sometimes
* update translations
* update to core 1.125.0


## v1.41.0 Testrun
2023-10

* keep screen on while playing voice messages
* pause background music when starting voice messages
* use the system camera as default; the old built-in camera can be enabled at "Settings / Advanced"
* add "Verified by" information to contact profiles
* screen reader: read out message types
* screen reader: allow tapping anywhere in the  message to start voice or audio playback
* set different wallpapers for different accounts
* add "Select All" to gallery and to file lists
* resend attachments from profile (long tap, then "Resend" in the menu)
* allow to import a key file instead of a folder containing keys
* search in "Attach Contact" dialog
* improve landscape mode for webxdc apps
* adapt webxdc loading screen to dark mode
* add file name to dialog shown if a webxdc app wants to share information
* add app icon to webxdc info messages and improve webxdc app icon layout
* improve layout of input bar when system emojis are used
* ask for permissions before adding notifications on Android 13 (needed by the required update to API 33)
* switch account if needed when opening webxdc app on the system's home screen
* improve video error messages and logging
* fix sometimes wrong avatar shown in notifications when using multiple accounts
* fix: save map preferences per account to avoid resetting location and zoom
* fix: play audio and voice messages: do not show progress in unrelated messages
* fix: update relative times directly after entering chatlist, do not wait for a minute
* fix issues when after selecting a non-system-language, system-language strings still show up
* fix: only jump to message if info message is from webxdc
* fix: update webxdc document name in titles immediately
* fix: do not open Connectivity when tapping forward/share titles
* fix starting conversation with contact from the phone contacts app
* fix WASM support for some webxdc apps
* fix off-by-one mismatch in manual language selection
* fix: sanitize invalid filename we get from some camera apps
* fix: display sticker footer properly
* fix: webxdc apps starting twice sometimes
* fix sending images and other files in location steaming mode
* fix connectivity view layout if eg. storage shows values larger than 100%
* fix scanning account-QR-codes on older phones that miss the Let's Encrypt system certificate
* fix: make Thunderbird show encrypted subjects
* fix: do not forward document name when forwarding only a webxdc app
* fix: do not create new groups if someone replies to a group message with status "failed"
* fix: do not block new group chats if 1:1 chat is blocked
* fix "Show full message" showing a black screen for some messages received from Microsoft Exchange
* fix: skip read-only mailing lists from forwarding/share chat lists
* fix: do not allow dots at the end of email addresses
* fix: do not send images pasted from the keyboard unconditionally as stickers
* fix: forbid membership changes from possible non-members, allow from possible members
* fix: improve group consistency across members
* fix: delete messages from SMTP queue only on user demand
* fix: improve wrapping of email messages on the wire
* fix memory leak in IMAP
* update translations and local help
* update to core 1.124.1


## v1.40.1
2023-08

* fix: correct core-submodule picked up by f-droid
* update to core119.1


## v1.40.0
2023-08

* use image editor for avatar selection when possible
* allow media from blob: and data: in webxdc
* optimized native library size
* improve loading screen in dark mode
* improve IMAP logs
* update "verified icon"
* fix webxdc issues with dark mode
* fix crash in android 4.2 or older when opening a HTML message in full message view
* fix: avoid IMAP move loops when DeltaChat folder is aliased
* fix: accept webxdc updates in mailing lists
* fix: delete webxdc status updates together with webxdc instance
* fix: prevent corruption of large unencrypted webxdc updates
* fix "Member added by me" message appearing sometimes within wrong context
* fix core panic after sending 29 offline messages
* fix: make avatar in qr-codes work on more platforms
* fix: preserve indentation when converting plaintext to HTML
* fix: remove superfluous spaces at start of lines when converting HTML to plaintext
* fix: always rewrite and translate member added/removed messages
* add Luri Bakhtiari translation, update other translations and local help
* update to core119


## v1.38.2
2023-06

* fix version code issue with google play
* using core117.0


## v1.38.1
2023-06

* update translations
* using core117.0


## v1.38.0
2023-06

* improve group membership consistency
* fix verification issues because of email addresses compared case-sensitive sometimes
* fix empty lines in HTML view
* fix empty links in HTML view
* fix displaying of smaller images that were shown just white sometimes
* fix android4 HTML view; bug introduced in v1.37.0
* update translations
* update to core117.0


## v1.37.0 Testrun
2023-06

* new webxdc APIs: importFiles() and sendToChat()
* remove upper size limit of attachments
* save local storage: compress HTML emails in the database
* save traffic and storage: recode large PNG and other supported image formats
  (large JPEG were always recoded; images send as "File" are still not recorded or changed otherwise)
* also strip metadata from images before sending
  in case they're already small enough and do not require recoding
* strip unicode sequences that are useless but may trick the user (RTLO attacks)
* set a draft when scanning a QR code containing compatible mailto: data
* tweak colors: make titles more visible in dark mode
* bigger scroll-to-bottom button
* fix appearance of verified icons
* fix some bugs with handling of forward/share views
* fix: exiting messages are no longer downloaded after configuration
* fix: don't allow blocked contacts to create groups
* fix: do not send messages when sending was cancelled while being offline
* fix various bugs and improve logging
* update to core116.0


## v1.36.5
2023-04

* use SOCKS5 configuration also for loading remote images in HTML mails
* bug fixes
* update translations and local help
* update to core112.8


## v1.36.4
2023-04

* start with light/dark theme depending on system theme
* fix verification icons for one-to-one chats
* fix fetch errors due to erroneous EOF detection in long IMAP responses
* more bug fixes
* update translations and local help
* update to core112.7


## v1.36.2
2023-04

* add a device message after setting up a second device
* speed up "Add as Second Device" connection time significantly on the getter side
* if possible, show Wi-Fi-name directly after scanning an "Add Second Device" QR code
* fix immediate restarts of "Add Second Device"
* fix: do not show just trashed media in "All Media" view
* fix: update database if needed after "Add Second Device"
* update translations and local help
* update to core112.6


## v1.36.0
2023-03

* new, easy method of adding a second device to your account:
  select "Add as Second Device" after installation and scan a QR code from the old device
* view "All Media" of all chats by the corresponding option in the chat list's menu
* add "Clear Chat" option to remove all messages from a chat
* show non-deltachat emails by default for new installations
  (you can change this at "Settings / Chats and Media)
* show notifications for all accounts
* make better use of dark/light mode in "Show full message"
* show icon beside info messages of apps
* resilience against outages by caching DNS results for SMTP connections
  (IMAP connections are already cached since 1.34.11)
* prefer TLS over STARTTLS during autoconfiguration, set minimum TLS version to 1.2
* use SOCKS5 configuration also for HTTP requests
* make invite QR codes even prettier
* improve speed by reorganizing the database connection pool
* improve speed by decrypting messages in parallel
* improve reliability by using read/write instead of per-command timeouts for SMTP
* improve reliability by closing databases sooner
* improve compatibility with encrypted messages from non-deltachat clients
* fix: Skip "Show full message" if the additional text is only a footer already shown in the profile
* fix verifications when using for multiple devices
* fix backup imports for backups seemingly work at first
* fix a problem with gmail where (auto-)deleted messages would get archived instead of deleted
* fix deletion of more than 32000 messages at the same time
* update provider database
* update translations and local help
* update to core112.1


## v1.34.13
2023-02

* fix sending status updates of private apps
* show full messages: do not load remote content for requests automatically
* using core107.1


## v1.34.12
2023-02

* disable SMTP pipelining for now
* fix various bugs and improve logging
* update translations
* update to core107.1


## v1.34.11
2023-01

* add SOCKS5 options to "Add Account" and "Configure"
* introduce DNS cache: if DNS stops working on a network,
  Delta Chat will still be able to connect to IMAP by using previous IP addresses
* speed up sending and improve usability in flaky networks by using SMTP pipelining
* fix SOCKS5 connection handling
* fix various bugs and improve logging
* update translations
* update to core107


## v1.34.10
2023-01

* fix: make archived chats visible that don't get unarchived automatically (muted chats):
  add an unread counter and move the archive to the top
* fix: send AVIF, HEIC, TXT, PPT, XLS, XML files as such
* fix: trigger reconnection when failing to fetch existing messages
* fix: do not retry fetching existing messages after failure, prevents infinite reconnection loop
* fix: do not add an error if the message is encrypted but not signed
* fix: do not strip leading spaces from message lines
* fix corner cases on sending quoted texts
* fix STARTTLS connection
* fix: do not treat invalid email addresses as an exception
* fix: flush relative database paths introduced in 1.34.8 in time
* faster updates of chat lists and contact list
* update translations
* update to core106


## v1.34.8
2022-12

* If a classical-email-user sends an email to a group and adds new recipients,
  the new recipients will become group members
* treat attached PGP keys from classical-email-user as a signal to prefer mutual encryption
* treat encrypted or signed messages from classical-email-user as a signal to prefer mutual encryption
* fix migration of old databases
* fix: send ephemeral timer change messages only of the chat is already known by other members
* fix: use relative paths to database and avoid problems eg. on migration to other devices or paths
* fix read/write timeouts for IMAP over SOCKS5
* fix: do not send "group name changes" if no character was modified
* add Greek translation, update other translations
* update to core104


## v1.34.7 Testrun
2022-12

* prevent From:-forgery attacks
* disable Autocrypt & Authres-checking for mailing lists because they don't work well with mailing lists
* small speedups
* improve logging
* fix detection of "All mail", "Trash", "Junk" etc folders
* fix reactions on partially downloaded messages by fetching messages sequentially
* fix a bug where one malformed message blocked receiving any further messages
* fix: set read/write timeouts for IMAP over SOCKS5
* update translations
* update to core103


## v1.34.5
2022-11

* allow removal of referenced contacts from the "New Chat" list
* show more debug info in message info
* improve IMAP logging
* show versionCode in log
* fix potential busy loop freeze when marking messages as seen
* fix build issue for F-Droid
* update translations
* update to core101


## v1.34.4
2022-11

* fix opening chats for android4 (bug introduced with 1.34.3)
* fix adding notifications on some android versions
* update translations
* using core98


## v1.34.3
2022-10

* fix Share-to-Delta and calling Delta otherwise for android12
* using core98


## v1.34.2 Testrun
2022-10

* fix messages not arriving on newer androids by switching to more modern APIs
* fix "recently seen" indicator for right-to-left languages
* fix message bubble corner for right-to-left languages
* fix: suppress welcome messages after account import
* fix: apply language changes to all accounts
* update dependencies and set targetSdkVersion to 32
* update translations and local help
* update to core98


## v1.34.1
2022-10

* more visible "recently seen" indicator
* fix: hide "disappearing messages" options for mailing lists
* update translations
* using core95


## v1.34.0 Testrun
2022-10

* start using "Private Apps" as a more user friendly term for the technical "Webxdc" term
* add "Private Apps" to the home screen from the app's menu,
  allowing easy access and integration with "normal" apps
* "Private Apps" and "Audio" are shown as a separate tabs in chat profile
* show a "recently seen" dot on avatars if the contact was seen within ten minutes
* order contact and members lists by "last seen"
* show mailing list addresses in profile
* user friendlier system messages as "You changed the group image."
* introduce a "Login" QR code that can be generated by providers for easy log in
* allow scanning of "Accounts" and "Logins" QR codes using supported system cameras
* truncate incoming messages by lines instead of just length
* for easier multi device setup, "Send Copy To Self" is enabled by default now
* fix: hide "Resend" option for messages that cannot be resent
* fix: hide "Leave group" option for mailing lists
* fix: mark "group image changed" as system message on receiver side
* fix: improved error handling for account setup from QR code
* fix: do not emit notifications for blocked chats
* fix: show attached .eml files correctly
* fix: don't prepend the subject to chat messages in mailing lists
* fix: reject private app updates from contacts who are not group members
* update translations
* update to core95


## v1.32.0
2022-07

* update Maplibre
* update translations
* using core90


## v1.31.1 Testrun
2022-07

* AEAP: show confirmation dialog before changing e-mail address
* AEAP: add a device message after changing e-mail address
* AEAP replaces e-mail addresses only in verified groups for now
* fix: handle updates for not yet downloaded webxdc instances
* fix: better information on several configuration and non-delivery errors
* update translations, revise english source
* update to core90


## v1.31.0 Testrun
2022-07

* experimental "Automatic E-mail Address Porting" (AEAP):
  You can configure a new address now, and when receivers get messages
  they will automatically recognize your moving to a new address
* combine read receipts and webxdc updates and avoid sending too many messages
* message lines starting with `>` are sent as quotes to non-Delta-Chat clients
* support IMAP ID extension that is required by some providers
* forward info messages as plain text
* allow mailto: links in webxdc
* fix: allow sharing filenames containing the character `~`
* fix: allow DeltaChat folder being hidden
* fix: cleanup read receipts storage
* fix: mailing list: remove square-brackets only for first name
* fix: do not use footers from mailinglists as the contact status
* update to core88


## v1.30.3
2022-06

* cleanup series of webxdc-info-messages
* fix: make chat names always searchable
* fix: do not reset database if backup cannot be decrypted
* fix: do not add legacy info-messages on resending webxdc
* fix: webxdc "back" button always closes webxdc
* fix: let "Only Fetch from DeltaChat Folder" ignore other folders
* fix: Autocrypt Setup Messages updates own key immediately
* fix: do not skip Sent and Spam folders on gmail
* fix: cleanup read-receipts saved by gmail to the Sent folder
* fix: handle decryption errors explicitly and don't get confused by encrypted mail attachments
* update provider database, add hermes.radio subdomains
* update translations
* update to core86


## v1.30.2
2022-05

* show document and chat name in webxdc titles
* add menu entry access the webxdc's source code
* remove anyway unused com.google.android.gms from binary to avoid being flagged
* send normal messages with higher priority than read receipts
* improve chat encryption info, make it easier to find contacts without keys
* improve error reporting when creating a folder fails
* fix: repair encrypted mails "mixed up" by Google Workspace "Append footer" function
* fix: use same contact-color if email address differ only in upper-/lowercase
* update translations
* update to core83


## v1.30.1
2022-05

* fix wrong language in read receipts
* fix encoding issue in QR code descriptions
* webxdc: allow internal pages
* update translations and local help
* update provider database
* update to core80


## v1.30.0
2022-05

* speed up loading of chat messages by a factor of 20
* speed up finding the correct server after logging in
* speed up marking messages as being seen and use fewer network data by batch processing
* speed up messages deletion and use fewer network data for that
* speed up webxdc parsing by not loading the whole file into memory
* speed up message receiving a bit
* speed up chat list
* speed up opening chat
* speed up various parts by caching config values
* revamped welcome screen
* archived+muted chats are no longer unarchived when new messages arrive;
  this behavior is also known by other messengers
* warn when enabling "Only Fetch from DeltaChat Folder"
* fix: do not create empty contact requests with "setup changed" messages;
  instead, send a "setup changed" message into all chats we share with the peer
* fix an issue where the app crashes when trying to export a backup
* fix outgoing messages appearing twice with Amazon SES
* fix unwanted deletion of messages that have no Message-ID set or are duplicated otherwise
* fix: assign replies from a different email address to the correct chat
* fix: assign outgoing private replies to the correct chat
* fix: ensure ephemeral timer is started eventually also on rare states
* fix: do not try to use stale SMTP connections
* fix: retry message sending automatically and do not wait for the next message being sent
* fix a bug where sometimes the file extension of a long filename containing a dot was cropped
* fix messages being treated as spam by placing small MIME-headers before the larger Autocrypt:-header
* fix: keep track of QR code joins in database to survive restarts
* fix: automatically accept chats with outgoing messages
* fix connectivity view's "One moment..." message being stuck when there is no network
* fix wrong avatar rotation when selecting self-avatar from gallery
* fix wrong font size in app title
* fix quitting app when forwarding on android4 and android11+
* fix emojis on android4
* fix: do not disable fullscreen keyboard
* fix: mark messages as seen more reliable and faster
* fix sound notifications, allow to set to "silent"
* fix ux issue in the forward dialog
* fix: update search results when the chatlist changes
* fix: show download failures
* fix sending webxdc via share-to-delta
* fix potential webxdc id collision
* fix: send locations in the background regardless of other sending activity
* fix rare crashes when stopping IMAP and SMTP
* fix correct message escaping consisting of a dot in SMTP protocol
* fix: don't jump to parent message if parent message is not a webxdc
* fix webxdc background mode so that music stops playing
* webxdc: improve display of webxdc items in the gallery's "docs" tab
* webxdc: show icon in quotes
* webxdc: long-tap on a message allows resending own messages
* webxdc: allow sessionStorage, localStorage and IndexedDB
* webxdc: remove getAllUpdates(), setUpdateListener() improved
* webxdc: option to set minimal API in the manifests
* add finnish translation, update other translations
* update to core79


## v1.28.3
2022-02

* faster message moving and deletion on the server
* parse MS Exchange read receipts and mark the original message as read
* fix a bug where messages in the Spam folder created contact requests
* fix a bug where drafts disappeared after some days
* fix: do not retry message sending infinitely in case of permanent SMTP failure
* fix: set message state to failed when retry limit is exceeded
* fix: avoid archived, fresh chats
* update translations
* update to core76


## v1.28.1
2022-02

* update translations, thanks a lot to all translators,
  porting Delta Chat to so many languages <3


## v1.28.0
2022-01

* add option "Advanced / Only Fetch from DeltaChat Folder";
  this is useful if you can configure your server to move chat messages to the DeltaChat folder
* to safe traffic and connections, "Advanced / Watch Sent Folder" is disabled by default;
  as all other IMAP folders, the folder is still checked on a regular base
* fix: use Webxdc name in chatlist, quotes and drafts
* fix splitting off text from Webxdc messages
* fix: show correct Webxdc summary on drafts
* fix: speed up folder scanning
* fix: make it possible to cancel message sending by removing the message;
  this was temporarily impossible since 1.27.0
* fix: avoid endless reconnection loop
* fix display of qr-group-invite code text
* update translations
* update provider-database
* update to core75


## v1.27.2 Testrun Release
2022-01

* improve Webxdc bubble layout
* async Webxdc API and reworked Webxdc properties
* fix: do not share cached files between Webxdc's
* fix: do not force dark mode for Webxdc and HTML-messages


## v1.27.1 Testrun Release
2022-01

* fix backup import issue introduced in 1.27.0
* update to core72


## v1.27.0 Testrun Release
2022-01

* add option to create encrypted database at "Add Account / Advanced",
  the database passphrase is generated automatically and is stored in the system's keychain,
  subsequent versions will probably get more options to handle passphrases
* add experimental support for Webxdc extensions
* add "Advanced / Developer Mode" to help on creating Webxdc extensions
* add writing support for supported mailinglist types; other mailinglist types stay read-only
* "Message Info" show routes
* explicit "Watch Inbox folder" and "Watch DeltaChat folder" settings no longer required;
  the folders are watched automatically as needed
* detect correctly signed messages from Thunderbird and show them as such
* synchronize Seen status across devices
* more reliable group memberlist and group avatar updates
* recognize MS Exchange read receipts as such
* fix leaving groups
* fix unread count issues in account switcher
* fix crash when selecting thumbnail image
* fix add POI if the user cannot send in a chat
* fix "Reply Privately" in contact request chats
* add Bulgarian translations, update other translations and local help
* update provider-database
* update to core71


## v1.26.2
2021-12

* re-layout all QR codes and unify appearance among the different platforms
* show when a contact was "Last seen" in the contact's profile
* group creation: skip presetting a draft that is deleted most times anyway
* display auto-generated avatars and unread counters similar across platforms
* fix chat assignment when forwarding
* fix layout bug in chatlist title
* fix crashes when opening map
* fix group-related system messages appearing as normal messages in multi-device setups
* fix removing members if the corresponding messages arrive disordered
* fix potential issue with disappearing avatars on downgrades
* fix log in failures for "Google Workspace" (former "G Suite") addresses using oauth2
* switch from Mapbox to Maplibre
* update translations
* update to core70


## v1.24.4
2021-11

* fix accidental disabling of ephemeral timers when a message is not auto-downloaded
* fix: apply existing ephemeral timer also to partially downloaded messages;
  after full download, the ephemeral timer starts over
* update translations and local help
* update to core65


## v1.24.3
2021-11

* fix crash when exporting several attachments at the same time
* fix messages added on scanning the QR code of an contact
* fix incorrect assignment of Delta Chat replies to classic email threads
* add basic support for remove account creation
* update translations and local help


## v1.24.2
2021-11

* show the currently selected account in the chatlist;
  a tap on it shows the new, improved account selector dialog
* new option "Auto-Download Messages": Define the max. messages size to be downloaded automatically -
  larger messages, as videos or large images, can be downloaded manually by a simple tap then
* long tap the app icon to go directly to one of the recent chats
  (requires Android 7 and a compatible launcher)
* much more QR code options: copy, paste, save as image, import from image
* new: much easier joining of groups via qr-code: nothing blocks
  and you get all progress information in the immediately created group
* new: get warnings before your server runs out of space (if quota is supported by your provider)
* messages are marked as "being read" already when the first recipient opened the message
  (before, that requires 50% of the recipients to open the message)
* contact requests are notified as usual now
* force strict certificate checks when a strict certificate was seen on first login
* do not forward group names on forwarding messages
* "Broadcast Lists", as known from other messengers, added as an experimental feature
  (you can enable it at "Settings / Advanced")
* improve accessibility: add some button descriptions
* remove "view profile" from the chat menu; just tap the chat name to open the profile
* accept contact request before replying from notification
* improve selected recipients list on group creation
* from within a contact's profile, offer group creation with that contact ("New Group or Subject")
* fix: disappearing messages timer now synced more reliable in groups
* fix: improve detection of some mailing list names
* fix "QR process failed" error
* fix DNS and certificate issues
* fix: if account creation was aborted, go to the previously selected account, not to the first
* fix back button not working in connectivity view sometimes
* fix: disable chat editing options if oneself is not a member of the group
* fix shared image being set as draft repeatedly
* fix: hide keyboard when compose panel is hidden
* fix "jump to section" links html-messages
* fix: allow to select audio files in multi-select mode in Docs tab
* fix fullscreen input issues by disabling this mode
* fix: group creating: don't add members if back button is pressed
* update provider-database
* update translations and local help


## v1.22.1
2021-08

* update translations


## v1.22.0
2021-08

* added: connectivity view shows quota information, if supported by the provider
* fix editing shared images
* fix account migration, updates are displayed instantly now
* fix forwarding mails containing only quotes
* fix ordering of some system messages
* fix handling of gmail labels
* fix connectivity display for outgoing messages
* update translations and provider database


## v1.21.2 Testrun Release
2021-08

* fix: allow dotless email address being added to groups
* fix: keep selection when migrating several accounts
* fix crash when going back to the chatlist
* update translations


## v1.21.1 Testrun Release
2021-08

* fix: avoid possible data loss when the app was not closed gracefully before;
  this bug was introduced in 1.21.0 and not released outside testing groups -
  thanks to all testers!


## v1.21.0 Testrun Release
2021-08

* added: every new "contact request" is shown as a separate chat now,
  you can block or accept or archive or pin them
  (old contact requests are available in "Archived Chats")
* added: the title bar shows if the app is not connected
* added: a tap in the title bar shows connectivity details (also available in settings)
* deactivate and reactivate your own QR codes by just scanning them
* when using multiple accounts, the background-accounts now also fetch messages
  that way, account switching is much faster than before
  and the destination account is usually directly usable
* allow dotless email address and localhost server,
  this allows using eg. yggmail addresses
* images from "Image keyboards" are sent as stickers now
* unify appearance of user-generated links
* don't open chat directly if user clicks in blocked addresses
* let openpgp4fpr:-links work in html-messages
* speedup chatlist while messages are downloaded
* fix: make log view's scroll to top/bottom work
* fix sharing files with "%" in their name
* fix: welcome-screen respects dark mode now
* fix: html-views respect app/system theme
* fix: hide unnecessary controls if you can't send in a chat
* fix: disable location service if it is not used anymore


## v1.20.5
2021-06

* fix downscaling images
* fix outgoing messages popping up in "Saved messages" for some providers
* fix: do not allow deleting contacts with ongoing chats
* fix: ignore drafts folder when scanning
* fix: scan folders also when inbox is not watched
* fix: adapt attached audio's background to theme
* fix: request composer's focus after recording is done
* fix sharing messages with attachments
* fix highlighting messages in search results
* fix: set correct navigation bar color in dark mode
* fix: use the same emoji theme throughout the app
* in in-chat search, start searching at the most recent message
* improve error handling and logging
* remove screen lock as announced in v1.14.0
* update translations and provider database


## v1.20.2
2021-05

* fix crash when receiving some special messages
* fix downloading some messages multiple times
* fix formatting of read receipt texts
* update translations


## v1.20.1
2021-05

* improved accessibility and screen reader support
* use the same emoji style everywhere across the app
* allow to select and copy text from "message details" and error dialogs
* show hints about how location data are used
* fix: don't collapse search menu on group changes
* add Indonesian, Polish, Ukrainian local help, update other translations


## v1.19.2 Preview Release
2021-04

* opening the contact request chat marks all contact requests as noticed
  and removes the sticky hint from the chatlist
* if "Show classic mails" is enabled,
  the contact request hint in the corresponding chat
* speedup global search
* show system message status while sending and on errors
* improve quote style when replying with a sticker
* fix clicks on system messages
* fix sticker scaling
* fix: disable "reply privately" in contact requests chat


## v1.19.1 Preview Release
2021-04

* show answers to generic support-addresses as info@example.com in context
* allow different sender for answers to support-addresses as info@example.com
* add APNG and animated webp support
* allow videochat-invites for groups
* let stickers handle taps
* add more options to Gallery and Documents long-tap menus
* allow to add POI with text of any length
* improve detection of quotes
* ignore classical mails from spam-folder
* hide share button in media previews, draft images or avatars
* fix crash when profile tabs are changed during some items are selected
* add Czech translation, update other translations
* add Chinese and French local help, update other local helps


## v1.17.0 Preview Release
2021-04

* new mailinglist and better bot support
* add option to view original-/html-mails
* check all imap folders for new messages from time to time
* use more colors for user avatars
* improve e-mail compatibility
* improve compatibility with Outlook.com
  and other providers changing message headers
* swipe up the voice message record button to lock recording
* show stickers as such
* show status/footer messages in contact profiles
* scale avatars based on media-quality, fix avatar rotation
* export backups as .tar files
* enable strict TLS for known providers by default
* improve and harden secure join
* new gallery options "Show in chat" and "Share from Delta Chat"
* display forwarded messages in quotes as such
* show name of forwarder in groups
* add chat encryption info
* tweak ephemeral timeout options
* show message delivery errors directly when tapping on a message
* add option to follow system light/dark settings
* better profile and group picture selection by using attachment selector
* make the upper left back button return to chat list even if the keyboard is open
* fix decoding of attachment filenames
* fix: exclude muted chats from notify-badge/dot
* fix: do not return quoted messages from the trash chat
* fix text width for messages with tall images
* fix disappearing drafts
* much more bug fixes
* add Khmer and Kurdish translations, update other translations
* add Czech local help, update other local help


## v1.14.5
2020-11

* show impact of the "Delete messages from server" option more clearly
* fix: do not fetch from INBOX if "Watch Inbox folder" is disabled
  and do not fetch messages arriving before re-enabling
* fix: do not use STARTTLS when PLAIN connection is requested
  and do not allow downgrade if STARTTLS is not available
* update translations


## v1.14.4
2020-11

* fix input line height when using system-emojis
* fix crash on receiving certain messages with quotes


## v1.14.3
2020-11

* add timestamps to image and video filenames
* fix: preserve quotes in messages with attachments


## v1.14.2
2020-11

* make quote animation faster
* fix maybe stuck notifications
* fix: close keyboard when a quotes is opened in another chat
* fix: do not cut the document icon in quotes
* fix: make the the quote dismiss button better clickable again
* update translations


## v1.14.1
2020-11

* improve display of subseconds while recording voice messages
* disable useless but confusing forwarding of info-messages
* fix: show image editor "Done" button also on small screen
* fix: show more characters of chat names before truncating
* fix crash in image editor


## v1.14.0
2020-11

* new swipe-to-reply option
* disappearing messages: select for any chat the lifetime of the messages
* chat opens at the position of the first unseen message
* add known contacts from the IMAP-server to the local addressbook on configure
* direct forwarding to "saved messages" - save one tap and stay in context :)
* long tap in contact-list allows opening "profile" directly
* allow forwarding to multiple archived chats
* enable encryption in groups if preferred by the majority of recipients
  (previously, encryption was only enabled if everyone preferred it)
* add explicit switches for handling background connections
  at "Settings / Notifications"
* ask directly after configure for the permission to run in background
  to get notifications
* speed up chatlist-view
* speed up configuration
* try multiple servers from autoconfig
* prefix log by a hint about sensitive information
* check system clock and app date for common issues
* prepare to remove screen lock as it adds only few protection
  while having issues on its own
* improve multi-device notification handling
* improve detection and handling of video and audio messages
* hide unused functions in "Saved messages" and "Device chat" profiles
* remove unneeded information when copying a single message to the clipboard
* bypass some limits for maximum number of recipients
* fix launch if there is an ongoing process
* fix: update relative times in chatlist once a minute
* fix: hide keyboard when leaving edit-name
* fix: connect immediately to an account scanned from a qr-code
* fix errors that are not shown during configuring
* fix keyboard position on Android Q
* fix mistakenly unarchived chats
* fix: tons of improvements affecting sending and receiving messages, see
  https://github.com/deltachat/deltachat-core-rust/blob/master/CHANGELOG.md
* update provider database and dependencies
* add Slovak translation, update other translations


## v1.12.5
2020-08

* fix notifications for Android 4
* fix and streamline querying permissions
* fix removing POIs from map
* fix emojis displayed on map
* fix: connect directly after account qr-scan
* make bot-commands such as /echo clickable


## v1.12.3
2020-08

* more generous acceptance of entered webrtc-servers names
* allow importing backups in the upcoming .tar format
* remove X-Mailer debug header
* try various server domains on configuration
* improve guessing message types from extension
* make links in error messages clickable
* fix rotation when taking photos with internal camera
* fix and improve sharing and sendto/mailto-handling
* fix oauth2 issues
* fix threading in interaction with non-delta-clients
* fix showing unprotected subjects in encrypted messages
* more fixes, update provider database and dependencies


## v1.12.2
2020-08

* fix improvements for sending larger mails
* fix a crash related to muted chats
* fix incorrect dimensions sometimes reported for images
* improve linebreak-handling in HTML mails
* improve footer detection in plain text email
* define own jitsi-servers by the prefix `jitsi:`
* fix deletion of multiple messages
* more bug fixes


## v1.12.1
2020-07

* show a device message when the password was changed on the server
* videochats introduced as experimental feature
* show experimental disappearing-messages state in chat's title bar
* improve sending large messages
* improve receiving messages
* improve error handling when there is no network
* use correct aspect ratio of background images
* fix sending uncompressed images
* fix emojis for android 4
* more bug fixes


## v1.10.5
2020-07

* forward and share to multiple contacts in one step
* disappearing messages added as an experimental feature
* fix profile image selection
* fix blurring
* improve message processing
* improve overall stability


## v1.10.4
2020-06

* add device message, summing up changes
* update translations and help


## v1.10.3
2020-06

* with this version, Delta Chat enters a whole new level of speed,
  messages will be downloaded and sent way faster -
  technically, this was introduced by using so called "async-processing"
* avatars can be enlarged
* add simplified login for gsuite email addresses
* new emoji selector - including new and diversified emojis
* you can now "blur" areas in an image before sending
* new default wallpaper
* if a message cannot be delivered to a recipient
  and the server replies with an error report message,
  the error is shown beside the message itself in more cases
* backup now includes the mute-state of chats
* notifications now use one system-editable channel per chat,
  this fix various notification bugs
* android 7 and newer groups notifications
* multi-account is an officially supported feature now
* default to "Strict TLS" for some known providers
* improve reconnection handling
* improve interaction with conventional email programs
  by showing better subjects
* allow calling the app from others apps with a standard email intent
* fix issues with database locking
* fix importing addresses
* lots of other fixes


## v1.8.1
2020-05-14

* fix a bug that could led to load if the server does not use sent-folder
* fix bug on sharing
* improve polling when background-connection is unreliable
* since 1.6.0, changing group-name removed the group-avatar sometimes, fixed


## v1.8.0
2020-05-11

* by default, the permanent notification is no longer shown;
  the background fetch reliability depends on the system and the
  permanent notification can be enabled at "Settings / Notifications" as needed
* fix a bug that stops receiving messages under some circumstances
* more bug fixes
* update translations


## v1.6.2
2020-05-02

* expunge deleted messages more frequently
* bug fixes
* update translations


## v1.6.0
2020-04-29

* new options to auto-delete messages from the device or from your server
  see "Settings / Chats and media"
* to save traffic and time, smaller and faster Ed25519 keys are used by default
* in-chat search
* search inside the integrated help
* new experimental feature that allows switching the account in use
* improve interaction with traditional mail clients
* improved onboarding when the provider returns a link
* to improve background fetch, show a permanent notification by default
* the permanent notification can be disabled at "Settings / Notifications"
* bug fixes
* add Indonesian and Persian translations, update other translations


## v1.3.0
2020-03-25

* on forwarding, "Saved messages" will be always shown at the top of the list
* streamline confirmation dialogs on chat creation and on forwarding to "Saved messages"
* cleanup settings
* improve interoperability eg. with Cyrus server
* fix group creation if group was created by non-delta clients
* fix showing replies from non-delta clients
* fix crash when using empty groups
* several other fixes
* add Sardinian translation, update other translations and help


## v1.2.1
2020-03-04

* on log in, for known providers, detailed information are shown if needed;
* in these cases, also the log in is faster
  as needed settings are available in-app
* save traffic: messages are downloaded only if really needed,
* chats can now be pinned so that they stay sticky atop of the chat list
* a 'setup contact' qr scan is now instant and works even when offline -
  the verification is done in background
* unified 'send message' option in all user profiles
* streamline onboarding
* add an option to create an account by scanning a qr code, of course,
  this has to be supported by the used provider
* lower minimal requirements, Delta Chat now also runs on Android 4.1 Jelly Bean
* fix updating names from incoming mails
* fix encryption to Ed25519 keys that will be used in one of the next releases
* several bug fixes, eg. on sending and receiving messages, see
  https://github.com/deltachat/deltachat-core-rust/blob/master/CHANGELOG.md#1250
  for details on that
* add Croatian and Esperanto translations, update other translations and help

The changes have been done by Alexander Krotov, Allan Nordhøy, Ampli-fier,
Angelo Fuchs, Andrei Guliaikin, Asiel Díaz Benítez, Besnik, Björn Petersen,
ButterflyOfFire, Calbasi, cloudieg, Dmitry Bogatov, dorheim, Emil Lefherz,
Enrico B., Ferhad Necef, Florian Bruhin, Floris Bruynooghe, Friedel Ziegelmayer,
Heimen Stoffels, Hocuri, Holger Krekel, Jikstra, Lin Miaoski, Moo, Nico de Haen,
Ole Carlsen, Osoitz, Ozancan Karataş, Pablo, Paula Petersen, Pedro Portela,
polo lancien, Racer1, Simon Laux, solokot, Waldemar Stoczkowski, Xosé M. Lamas,
Zkdc


## v1.1.2
2020-01-26

* fix draft saving
* fix oauth2 issue introduced in 1.1.0
* several other fixes
* update translations, update local help


## v1.1.1
2020-01-24

* fix draft saving


## v1.1.0
2020-01-21

* integrate the help to the app
  so that it is also available when the device is offline
* rework qr-code scanning: there is now one activity with two tabs
* reduce traffic by combining read receipts and some other tweaks
* improve background-fetch on Android 9
* fix deleting messages from server
* fix saving drafts
* other fixes
* add Korean, Serbian, Tamil, Telugu and Bokmål translations,
  update other translations

The changes have been done by Alexander Krotov, Allan Nordhøy, Angelo Fuchs,
Andrei Guliaikin, Asiel Díaz Benítez, Besnik, Björn Petersen, ButterflyOfFire,
Calbasi, cyBerta, Dmitry Bogatov, dorheim, Emil Lefherz, Enrico B.,
Ferhad Necef, Florian Bruhin, Floris Bruynooghe, Friedel Ziegelmayer,
Heimen Stoffels, Hocuri, Holger Krekel, Jikstra, Lin Miaoski, Moo, Nico de Haen,
Ole Carlsen, Osoitz, Ozancan Karataş, Pablo, Pedro Portela, polo lancien,
Racer1, Simon Laux, solokot, Waldemar Stoczkowski, Xosé M. Lamas, Zkdc


## v1.0.3
2019-12-22

* do not try to recode videos attached as files
* check write-permissions before trying to save a log
* enable some linker optimizations and make the apk about 11 mb smaller
* fix issues with some email providers
* reset device-chat on import, this removes useless or unfunctional messages
  and allows messages being added again


## v1.0.2
2019-12-20

* fix opening attachments on newer android versions
* fix accidentally shown device-chat-system-notifications on older androids
* fix sending images and other attachments for some providers
* don't recreate and thus break group membership if an unknown
  sender (or mailer-daemon) sends a message referencing the group chat
* fix yandex/oauth


## v1.0.1
2019-12-19

* fix OAauth2/GMail
* fix group members not appearing in contact list
* fix hangs appearing under some circumstances
* retry sending already after 30 seconds
* improve html parsing


## v1.0.0
2019-12-17

Finally, after months of coding and fixing bugs, here it is: Delta Chat 1.0 :)
An overview over the changes since v0.510:

* support for user avatars: select your profile image
  at "My profile info" and it will be sent out to people you write to
* introduce a new "Device Chat" that informs the user about app changes
  and, in the future, problems on the device
* new "Saved messages" chat
* add "Certificate checks" options to "Login / Advanced"
* if "Show classic emails" is set to "All",
  emails pop up as contact requests directly in the chatlist
* add "Send copy to self" switch
* rework welcome screen
* a new core: for better stability, speed and future maintainability,
  the core is written completely in the Rust programming language now
* for end-to-end-encryption, rPGP is used now;
  the rPGP library got a first independent security review mid 2019
* improved behavior of sending and receiving messages in flaky networks
* more reliable background fetch on newer Android versions
* native 64bit support
* minimum requirement is Android 4.3 Jelly Bean
* tons of bug fixes

The changes of this version and the last beta versions have been done by
Alexander Krotov, Allan Nordhøy, Ampli-fier, Andrei Guliaikin,
Asiel Díaz Benítez, Besnik, Björn Petersen, ButterflyOfFire, Calbasi, cyBerta,
Daniel Boehrsi, Dmitry Bogatov, dorheim, Emil Lefherz, Enrico B., Ferhad Necef,
Florian Bruhin, Floris Bruynooghe, Friedel Ziegelmayer, Heimen Stoffels, Hocuri,
Holger Krekel, Jikstra, Lars-Magnus Skog, Lin Miaoski, Moo, Nico de Haen,
Ole Carlsen, Osoitz, Ozancan Karataş, Pablo, Pedro Portela, polo lancien,
Racer1, Simon Laux, solokot, Waldemar Stoczkowski, Xosé M. Lamas, Zkdc


## v0.982.0
2019-12-16

* move doze-reminder to device-chat
* improve logging
* update translations
* fix crashes on connecting to some imap and smtp servers


## v0.981.0
2019-12-15

* avatar recoding to 192x192 to keep file sizes small
* fix read-receipts appearing as normal messages
* fix smtp crash
* fix group name handling if the name contains special characters
* various other bug fixes


## v0.980.0
2019-12-14

* support for user avatars: select your profile image
  at "settings / my profile info"
  and it will be sent out to people you write to
* previously selected avatars will not be used automatically,
  you have to select a new avatar
* rework tls stack
* alleviate login problems with providers which only support RSA10
* prototype a provider-database with a testprovider
* improve key gossiping
* bug fixes


## v0.973.0
2019-12-10

* names show up correctly again
* html-attachments are possible again
* improve adding/removing group members
* improve connection handling and reconnects
* update translations


## v0.971.0
2019-12-06

* rework welcome screen
* update translations
* improve reconnecting
* various bug fixes


## v0.970.0
2019-12-04

* introduce a new "Device Chat" that informs the user about app changes
  and, in the future, problems on the device
* rename the "Me"-chat to "Saved messages",
  add a fresh icon and make it visible by default.
* add Arabic translation
* add Galician translation
* update translations
* use the rust-language for the mail-parsing and -generating part,
  introducing a vastly improved reliability
* fix moving messages
* fix flakiness when receiving messages
  and in the secure-join process
* more bug fixes


## v0.960.0
2019-11-24

* update translations
* more reliable background fetch on newer Android versions
* bug fixes
* minimum requirement is now Android 4.3 Jelly Bean


## v0.950.0
2019-11-05

* add "Certificate checks" options to "Login / Advanced"
* update translations
* bug fixes


## v0.940.2
2019-10-31

* re-implement "delete mails from server"
* if "Show classic emails" is set to "All",
  emails pop up as contact requests directly in the chatlist
* fix android9 voice-recording issues
* update translations
* various bug fixes


## v0.930.2
2019-10-22

* add "send copy to self" switch
* rework android4 emoji-sending
* rework android9 background-fetch
* fix 64bit issues
* fix oauth2 issues
* target api level 28 (android9, pie)
* update translations
* various bug fixes


## v0.920.0
2019-10-10

* improve onboarding error messages
* update translations
* various bug fixes


## v0.910.0
2019-10-07

* after months of hard work, this release is finally
  based on the new rust-core that brings improved security and speed,
  solves build-problems and also makes future developments much easier.
  there is much more to tell on that than fitting reasonably in a changelog :)
* this is also the first release including native code for 64bit systems
* minor ui improvements
* add Hungarian translation
* update translations


## v0.510.1
2019-07-09

* new image cropping feature: crop images before sending them
* updated image editing user interface
* update Chinese (zh-cn and zh-tw), Italian, Dutch, Turkish translations
* remove swipe to archive and swipe to unarchive chats
* improve UX to discard contact requests
* improve UX to block contacts
* bugfixes

The changes have been done by Björn Petersen, cyBerta, Enrico B.,
Heimen Stoffels, Lin Miaoski, Ozancan Karataş, Zkdc


## v0.500.0
2019-06-27

* New chat-profile: Gallery, documents, shared chats and members at a glance
* Add video recording and recoding
* Show video thumbnails
* Forward/Share: Add searching and forward/share to new contact/chat
* Share: Support direct sharing to a recently used chats
* New notification handling, including a mute-forever option :)
* Optional plipp-plop sounds in chats
* Better document- and music-files view
* Add new-messages marker
* Keep chat-scroll-position on incoming messages
* Clean up settings dialog
* More general "outgoing media quality" option (replaces image-quality option)
* Improve quality of voice messages
* More touch-friendly layout
* Add an experimental option to delete e-mails from server
* Improve compatibility with older phones
* Show a warning if the app is too old and won't be updated automatically
  (done just by date comparison, no data is sent anywhere)
* New option to save the log to a file
* Make input text field a bit larger
* Add Traditional Chinese and Simplified Chinese translations
* Update Albanian, Azerbaijani, Basque, Brazilian Portuguese, Catalan, Danish,
  Dutch, French, German, Italian, Japanese, Lithuanian, Polish, Portuguese,
  Russian, Spanish, Swedish, Turkish and Ukrainian translations
* Bugfixes

The changes have been done by Allan Nordhøy, Ampli-fier, Andrei Guliaikin,
Anna Ayala, Asiel Díaz Benítez, Besnik, Björn Petersen, Boehrsi, Calbasi,
Christian Schneider, cyBerta, Enrico B., Eric Lavarde, Ferhad Necef,
Floris Bruynooghe, Friedel Ziegelmayer, Heimen Stoffels, Holger Krekel,
Iskatel Istiny, Jikstra, Lars-Magnus Skog, Lin Miaoski, Luis, Moo, Ole Carlsen,
Osoitz, Ozancan Karataş, Racer, Sebek, Yuriy, Zkdc


## v0.304.0
2019-05-07

* Add Catalan translation
* Update several other translations
* Bugfixes

The changes have been done by Ampli-fier, Andrei Guliaikin, Asiel Díaz Benítez,
Björn Petersen, Calbasi, Enrico B., ferhad.necef, Heimen Stoffels, link2xt,
Maverick2k, Ole Carlsen, Osoitz, Ozancan Karataş, Racer1, Webratte


## v0.303.0
2019-05-01

* Add labels to map markers
* Always show self-position on map
* Tweak Log UI
* Bugfixes

The changes have been done by Ampli-fier, Björn Petersen, cyBerta


## v0.302.1

2019-04-27

* add POIs on maps
* Tweak Log UI
* add location indicator in chat messages
* bugfixes

The changes have been done by Björn Petersen, cyBerta, Daniel Boehrsi.


## v0.301.1
2019-04-22

* Fix chat view and log for Android 4.4 (Kitkat)


## v0.301.0
2019-04-20

* Experimental location-streaming can be enabled in the advanced settings;
  when enabled, you can optionally stream your location to a group
  and view a map with the members that are also streaming their location
* Tweaked dark-mode
* Improved account setup and profile dialogs
* Show and hide the virtual keyboard more gracefully
* Speed up program start
* Speed up message sending
* Handle Webp-Images and Vcard-files
* Add Japanese and Brazilian Portuguese translations
* Update several other translations
* Bug fixes

The changes have been done by Alexander, Ampli-fier, Angelo Fuchs,
Asiel Díaz Benítez, Besnik, Björn Petersen, cyBerta, Daniel Böhrs, Enrico B.,
ferhad.necef, Floris Bruynooghe, Friedel Ziegelmayer, Heimen Stoffels,
Holger Krekel, Janka, Jikstra, Luis, Moo, Nico de Haen, Ole Carlsen, Osoitz,
Ozancan Karataş, Racer1, sebek, Viktor Pracht, Webratte and others


## v0.200.0
2019-03-14

* Simplified setup (OAuth2) for google.com and yandex.com
* Improved setup for many other providers
* Decide, which e-mails should appear - "Chats only", "Accepted contacts" or "All"
* Improve moving chat messages to the DeltaChat folder
* Option for a stronger image compression
* Smaller message sizes in groups
* Share files from other apps to Delta Chat
* Share texts from mailto:-links
* Log can be opened from setup screen
* Add Lithuanian translation
* Update several other translations
* Bug fixes

The changes have been done by Alexandex, Angelo Fuchs, Asiel Díaz Benítez,
Björn Petersen, Besnik, Christian Klump, cyBerta, Daniel Böhrs, Enrico B.,
ferhad.necef, Florian Haar, Floris Bruynooghe, Friedel Ziegelmayer,
Heimen Stoffels, Holger Krekel, Iskatel Istiny, Lech Rowerski, Moo,
Ole Carlsen, violoncelloCH and others


## v0.101.0
2019-02-12

* First Play Store release, optimisations for Android O
* Ask to disable battery optimisations
* Start Azerbaijani and Swedish translations
* Update several other translations
* Many bug fixes

The changes have been done by Ampli-fier, Angelo Fuchs, Asiel Díaz Benítez,
Besnik, Björn Petersen, Christian Klump, Daniel Böhrs, Enrico B., ferhad.necef,
Florian Haar, Floris Bruynooghe, Heimen Stoffels, Holger Krekel,
Iskatel Istiny, Lech Rowerski, violoncelloCH and others.


## v0.100.0
2019-01-23

* Complete rework of the ui using pure material design
* Images and other files can be sent together with a description
* Images can be modified before sending, eg. text can be added or
  hand-drawn signs
* Image and media gallery for each chat
* Embedded camera, new camera icon directly in input field
* Embedded video player
* New emoticons
* Contacts and groups can be joined with a QR-code-scan
* Options for watching several IMAP-folders
* Option to move messages to the DeltaChat-folder
* Improved multi-device behavior
* Improved Accessibility eg. for screen readers
* Dark theme
* Support right-to-left languages
* Relative time display
* Chatlist and contact list support a long click for several operations
* Archive chats by swiping a chat right out of the chatlist
* Show date always atop of the chat
* Fix redraw problems with hidden system status or navigation bar
* Reply directly from within notification
* The system credentials have to be entered before exports
* The app can be protected by the system credentials
* Hide the permanent notification more reliable
* Improved resending of messages
* Allow password starting/ending with whitespaces
* Bug fixes
* Probably more i forgot

The changes have been done by Ampli-fier, Angelo Fuchs, Asiel Díaz Benítez,
Björn Petersen, chklump, Daniel Böhrs, Florian Haar, Hocceruser, Holger Krekel,
Lars-Magnus Skog

Translations are still in progress and video-recording is not yet re-implemented.
Help is very welcome -:)


## v0.20.0
2018-08-14

* Check size before sending videos, files and other attachments
* On sending problems, try over an appropriate number of times; then give up
* Detect sending problems related to the message size,
  show an error and do not try over
* Show message errors in the message info
* Add user forum to website
* Update python bindings
* Seed node.js bindings and a CLI version based on this
* Prepare Android bindings update
* Update Danish, Italian and Russian translations

The changes have been done by Andrei Guliaikin, Angelo Fuchs, Björn Petersen,
compl4xx, Boehrsi, Enrico B., Floris Bruynooghe, Holger Krekel, Janka, Jikstra,
Karissa McKelvey, Lars-Magnus Skog, Ole Carlsen


## v0.19.0
2018-07-10

* Give advices for Google users
* Speed up by making database-locks unnecessary
* Fix drafts appearing twice
* Update Albanian, Basque, Catalan, Danish, Dutch, English,
  Italian, Polish, Russian, and Turkish translations
* Update website

The changes have been done by Allan Nordhøy, Angelo Fuchs, Besnik,
Björn Petersen, Calbasi, Claudio Arseni, guland2000, Heimen Stoffels,
Holger Krekel, Luis Fernando Stürmer da Rosa, Mahmut Özcan, Ole Carlsen,
Osoitz, sebek, Thomas Oster


## v0.18.2
2018-06-23

* Fix initial configure process to hang at 95% under some circumstances


## v0.18.0
2018-06-21

* Speed up message sending/receiving
* Retry failed sending/receiving jobs just in the moment
  the networks becomes available again
* Make message sending/receiving more reliable
* Handle attachment file names with non-ASCII characters correctly
* Paging through images made available by Angelo Fuchs
* Several connection issues with different configurations
  were fixed by Thomas Oster
* Improve chat-folder creation by Thomas Oster
* Request permissions before using the camera; added by Thomas Oster
* Key import improved by Thomas Oster
* Improve background and foreground message fetching reliability
* Try to use the permanent notification only when really needed
* Update internal sqlite library from 3.22.0 to 3.23.1
* Update internal libEtPan library from 1.7 to 1.8
* Add Danish translation from Ole Carlsen
* Update Albanian, Basque, Danish, Italian, Norwegian, Dutch, Polish,
  Portuguese, Russian and Telugu translations


## v0.17.3
2018-05-17

* Fix system messages appearing twice
* Fix: Use all gossipped verifications in verified groups
* Update Basque, Polish, Russian and Ukrainian translations


## v0.17.2
2018-05-15

* Fix problem with adding formerly uncontacted members to groups
* Unblock manually blocked members when they are added manually as contact again


## v0.17.1
2018-05-11

* Improve QR code scanning screens
* Add a labs-option to disabled the new QR logo overlay
* Update Russian translations


## v0.17.0
2018-05-07

* Show shared chats in user profiles
* If a contact has changed his encryption setups,
  this is shown as a system messages in the middle of the chat view
* Show added group members, changed group titles etc. as system messages
* Show direct buttons to create a new group or contact in the "New Chat" dialog
* Improve "Add contact" dialog
* Move subject and most chat metadata to the encrypted part
  following the "Memoryhole" proposal
* Show read-timestamps in message info
* Do not add contacts from Bcc to group-memberlist 
  to avoid privacy leaks and to get a unique memberlist for all group-members
* In a mail contains plaintext and encrypted parts, 
  the whole mail is treated as not being encrypted correctly
* Restructure settings and advanced settings
* Fix problems with Office 365 and similar services
* Fix a problem where incoming messages are shown as being sent by oneself
* Experimental QR code scanning options can be enabled in the advanced settings
* Update Albanian, Catalan, Dutch, French, German, Italian, Norwegian, Polish,
  Russian, Spanish, Turkish and Ukrainian translations
* Add Basque translation
* Add Chinese translation
* Add Japanese translation


## v0.16.0
2018-03-19

* Messages from normal clients to more than one recipient
  create an implicit "ad-hoc group"
* Allow group creation though contact requests
* Always display the _sending_ time in the chat list;
  the list itself is sorted by _receiving_ time
  and "Message info" shows both times now
* If parts but the footnote are cut from mails,
  this is indicated "..."; use "Message info" to get the full text
* Highlight the subject in the "Message info"
* Autoconfigure prefers 'https' over 'http'
* Bug fixes, eg. avoid freezes if the connection is lost
* Update Russian, Tamil and Turkish translations


## v0.15.0
2018-02-27

* Render the waveform for voice messages
* Fix problems with voice messages on various devices
* Improve deletion of message that were moved around by another e-mail client
* Really delete messages on the server, do not only mark them for deletion
* Ignore subsequent keys or blocks in OpenPGP files
* Leave incoming Autocrypt Setup Messages in the inbox 
  so that any number of other e-mail-clients can process them
* Avoid messages sent to the "Me" chat appearing twice in other e-mail clients
* Update Albanian translation


## v0.14.0
2018-02-20

* Evaluate gossiped keys
* Option to transfer the Autocrypt Setup to another device or e-mail client
* Accept Autocrypt Setup transferred from other devices or e-mail client
* Send any data from device to device
  using the chat "Me - Messages I sent to myself"
* Do not send messages when there is an access error
* Request for contact permissions only once
* Bug fixes
* Update French and Turkish translations


## v0.13.0
2018-01-18

* Reply encrypted if the sender has enabled encryption manually
  (esp. useful when chatting with clients as K-9 or Enigmail)
* Update welcome screen graphics
* Update Norwegian, Russian and Turkish translations


## v0.12.0
2018-01-07

* Gossip keys of other group members in the encrypted payload
  (will also be evaluated in one of the next versions)
* Use SHA-256 instead of SHA-1 in signatures
* Make the permanent notification clickable
* Update permanent notification after import
* Fix rendering of system messages
* Various bug fixes
* Update Albanian, French, Italian, Norwegian, Polish, Russian
  and Turkish translations


## v0.11.4
2017-12-17

* Add option to initiate Autocrypt Key Transfer
* Connect after importing a backup
* Reading memory hole headers
* Add Albanian translation
* Update German, Italian, Polish, Portuguese, Russian, Turkish
  and Ukrainian translations


## v0.10.0
2017-11-29

* Fix usage of multiple private keys
* Fix various memory leaks
* Update English, Portuguese and Turkish translations


## v0.9.9
2017-11-18

* Alternate include order for F-Droid
* Add Serbian translation
* Update Catalan, Dutch, English, French, German, Hungarian, Italian, Polish,
  Portuguese, Russian, Spanish, Tamil, Telugu and Ukrainian translations


## v0.9.8
2017-11-15

* Fix a bug that avoids chat creation under some circumstances
  (bug introduced in 0.9.7)


## v0.9.7
2017-11-14

* Archive chats or delete chats by a long press
* Notify the user in the chatlist about contact requests
  of known users or of other Delta Chat clients
* Show messages only for explicitly wanted chats
* Show more detailed reasons about failed end-to-end-encryption
* Explicit option to leave a group
* Do not show the padlock if end-to-end-encryption is disabled by the user
* Import images from a backup when using a different device with different paths
* Add copy-to-clipboard function for "About / Info"
* Rework Emoji-code
* Add Norwegian Bokmål translation
* Add Tamil translation
* Add Turkish translation
* Update Catalan, German, French, Italian, Korean, Dutch, Polish, Portuguese,
  Russian, Telugu and Ukrainian translations


## v0.9.6
2017-10-18

* Support keys generated with multiple subkeys eg. from K-9
* Show PDFs and other attachments with bad names
* Bug fixes


## v0.9.5
2017-10-08

* Backup export and import function
* Query password before export
* Move replies from normal E-Mail-Clients to the "Chats" folder
* Improve helping MUAs on showing chat threads
* Improve onboarding
* Add URL to default footer
* Test a different approach for battery saving in this release
* Update French, Italian, German, Polish, Portuguese, Russian
  and Ukrainian translations


## v0.9.4
2017-08-23

* Introduce an editable "Status" field that is shown eg. in email footers
* Editable and synchronized group images
* Show the subject of messages that cannot be decrypted
* Do not send "Read receipts" when decryption fails
* Do not request "Read receipts" from normal MUAs 
  as there are too many MUAs responding with weird, non-standard formats
* Deleting a chat always deletes all messages from the device permanently
* Ignore messages from mailing lists
* Do not spread the original authors name nor address on forwarding
* Encrypt mails send to SMTP and to IMAP the same way
* Improve showing HTML-mails
* Cleanup Android code
* Remove badge counter on app restart
* Add Ukrainian translation
* Add Telugu translation
* Add Catalan translation
* Update German, Spanish, French, Hungarian, Italian, Polish, Portuguese
  and Russian translations


## v0.9.3
2017-07-13

* Introduce "Read receipts" and avoid social pressure to leave it activated
* Improve encryption dialog in profile
* Fix marking messages as "seen" when opening the contact requests
* Ignore signature.asc files of signed-only messages
* Update Polish, Portuguese and Russian translations


## v0.9.2
2017-06-28

* Encrypt group chats
* Cryptographically sign messages
* Validate signatures of incoming messages ("Info" shows the state)
* Show lock beside end-to-end-encrypted messages with a validated signature
* If end-to-end-encryption is available on sending time,
  guarantee the message not to be sent without end-to-end-encryption later
* Show special characters in HTML-mails
* Help MUAs on showing chat threads
* Show attachments from multipart/alternative structures
* Upgrade from Autocrypt Level 0 to Level 1;
  as the levels are not compatible, encryption on mixed setups does not happen
* Update Polish, Portuguese, Spanish and French translations


## v0.9.1
2017-06-04

* Profile: Improve encryption state dialog
* Improved video quality of short clips
* Make encryption-dialog localizable
* Update Russian translation


## v0.9.0
2017-06-01

* Add end-to-end-encrypting following the OpenPGP and Autocrypt standards
* Add a function to compare keys
* Profile: Add option to copy the email address to the clipboard
* Pimp GUI


## v0.1.36
2017-05-04

* Support camera on Android Nougat


## v0.1.34
2017-05-03

* Link to new homepage https://delta.chat
* Localizable Help-URLs


## v0.1.33
2017-04-29

* Better support for right-to-left (RTL) languages, taking advantage of
  Android 4.2 (Jelly Bean MR1, API level 17).
* Send PNG files without resizing and converting to JPEG
* If JPEG files are send without compression,
  they still appear as image, not as attached files
* Raise-to-speak defaults to false
* Unify long click behaviour
* Support Android's system function "Delete data"
* Replies to messages pop up automatically
  even if send from other email addresses (typical scenario for alias addresses)
* Fix group-replies from normal email-clients. 


## v0.1.32
2017-04-22

* Update Spanish and Portuguese translations
* Update internal sqlite library to version 3.18.0, released on 2017-03-28
* Remove more of the custom language handling, use Android's routines instead
* General code cleanup
* Play GIF files
* Option to disable autoplaying GIF files
* When sending contacts, only use the names the receivers have set themselves
* Show some hints when long-pressing icons in the action bar


## v0.1.29
2017-04-19

* Add Russian translation
* For outgoing (group-)messages,
  only use the names the receivers have set themselves


## v0.1.28
2017-04-14

* Pimp notifications
* Bug fixes


## v0.1.27
2017-04-12

* Use a permanent foreground service for reliable notifications
* Monitor the IMAP-IDLE thread and reconnect if IMAP-IDLE seems to hang
* Various battery and background optimizations


## v0.1.25
2017-04-04

* Use system or user selected video player.
* Do not connect if not configured (avoids a warning on the first time startup)
* Add  vertical scrollbar, eg. to settings activities.
* Pimp GUI and logo.
* Update Korean.


## v0.1.24
2017-03-31

* Share images and documents from other apps to Delta Chat
* Offer to mailto:-link-support to other apps
* Ignore implausible sending time of incoming messages;
  use the receive time in these rare cases
* Show errors only when Delta Chat is in foreground
* Dynamically adapt video bitrate for longer videos 
  to an attachment-size of max. 25 MB


## v0.1.23
2017-03-28

* Retry connecting to IMAP if there is not network available on the first try
* Notify about new messages if the app is not active for hours,
  optimize battery consumption


## v0.1.22
2017-03-22

* Show HTML-only messages
* Show connection errors
* Add options for SSL/TLS and STARTTLS
* Automatic account configuration, if possible
* Recode large videos
* Add Hungarian translation
* Add Korean translation


## v0.1.21
2017-03-10

* Record and send voice messages
* Record and send videos
* Send and play music
* Send contacts and email addresses
* Sending and opening attachments of any type
* Share and open commands for all attachments
* Accept VCards send to us by other apps
* Clickable email addresses
* Update Polish translation
* Fix tablet startup bug
* Close the app when using the lock-app-via-pincode function
* Protect data by using a content provider for sharing
* Try to clear the task switcher's screenshots when locking the app via pincode
* Pimp GUI


## v0.1.20
2017-02-16

* Avoid unwanted downloads of lots of old messages
* Make the "Chats" folder visible if the server hides new folders by default
* Fix a crash when the server returns empty folders
* Update Polish and Portuguese translations
* Use API level 25 (Nougat 7.1) as target


## v0.1.18
2017-02-11

* Add Polish translation
* Use a new default background for chats
* Improve typography by using the system font instead of a custom resource font
* Remove custom plural handling, use Android's routines instead
* Remove unused source code and strings
* More fixes of lint errors and warnings


## v0.1.17
2017-02-07

* Drop two unnecessary permissions
  ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION
* Really add French translation
* Update Portuguese translation
* Start fixing translation handling of the program
* Remove special "foss" build, because the whole program is free now.


## v0.1.16
2017-02-06

* Add French translation
* Fix some lint errors and warnings


## v0.1.15
2017-01-31

* Prepare for first release on F-Droid
