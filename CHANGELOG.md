# Delta Chat Android Changelog

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

The changes have been done Alexander Krotov, Ampli-fier, Andrei Guliaikin,
Asiel Díaz Benítez, Besnik, Björn Petersen, Calbasi, cyBerta, Daniel Boehrsi,
Dmitry Bogatov, dorheim, Enrico B., Ferhad Necef, Florian Bruhin,
Floris Bruynooghe, Friedel Ziegelmayer, Heimen Stoffels, Hocuri,
Holger Krekel, Jikstra, Lars-Magnus Skog, Lin Miaoski, Moo, Ole Carlsen,
Osoitz, Ozancan Karataş, Pedro Portela, polo lancien, Racer1, Simon Laux,
solokot, Waldemar Stoczkowski, Zkdc


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
* Optional a stronger image compression
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
* The system credentials has be be entered before exports
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
