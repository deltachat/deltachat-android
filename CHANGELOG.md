# Delta Chat Android Changelog

## v1.27.0
2022-01

* add option to create encrypted database at "Add Account / Advanced",
  the database passphrase is generated automatically and is stored in the system's keychain,
  subsequent versions will probably get more options to handle passphrases
* add initial support for Webxdc extensions
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
* "Broadcast Lists", as kown from other messengers, added as an experimental feature
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
* fix: make the the quote dissmiss button better clickable again
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
* direct forwarding to "saved messags" - save one tap and stay in context :)
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
* show experimental disappearing-messags state in chat's title bar
* improve sending large messages
* improve receiving messages
* improve error handling when there is no network
* use correct aspect ratio of background images
* fix sending umcompressed images
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

* by default, the permantent notification is no longer shown;
  the background fetch realibility depends on the system and the
  permantent notification can be enabled at "Settings / Notifications" as needed
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
* to improve background fetch, show a permantent notification by default
* the permantent notification can be disabled at "Settins / Notifications"
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
* several bug fixes, eg. on sending and receivind messages, see
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
* fix flakyness when receiving messages
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
* Keep chat-scroll-postion on incoming messages
* Clean up settings dialog
* More general "outgoing media quality" option (replaces image-quality option)
* Improve quality of voice messages
* More touch-friendly layout
* Add an experimental option to delete e-mails from server
* Improve compatibility with older phones
* Show a warning if the app is too old and won't be updated automatically
  (done just by date comparision, no data is sent anywhere)
* New option to save the log to a file
* Make input text field a bit larger
* Add Traditional Chinese and Simplified Chinese translations
* Update Albanian, Azerbaijani, Basque, Brazilian Portuguese, Catalan, Danish,
  Dutch, French, German, Italien, Japanese, Lithuanian, Polish, Portuguese,
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
* Improved Accessiblity eg. for screen readers
* Dark theme
* Support right-to-left languages
* Relative time display
* Chatlist and contat list support a long click for several operations
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
* Speed up by making database-locks unneccessary
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
* Show more detailed reasons about failed end-to-end-encryptions
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
