## Slash Commands

### Command name: `about copyright`
**Description**: Library, licensing, and copyright information

* **Arguments**:
None
---
### Command name: `about general`
**Description**: General information

* **Arguments**:
None
---
### Command name: `auto-threading enable`
**Description**: Automatically create a thread for each message sent in this channel.

**Required Member Permissions**: Manage Channels

* **Arguments**:
	* `role` - The role, if any, to invite to threads created in this channel. - Optional Role
	* `add-mods-and-role` - Whether to add moderators to the thread alongside the role - Defaulting Boolean
	* `prevent-duplicates` - If users should be stopped from having multiple open threads in this channel. Default false. - Defaulting Boolean
	* `archive` - If threads should be archived on creation to avoid filling the sidebar. Default false. - Defaulting Boolean
	* `content-aware-naming` - If Lily should use content-aware thread titles. Default false - Defaulting Boolean
	* `mention` - If the creator should be mentioned in new threads in this channel. Default false. - Defaulting Boolean
	* `message` - Whether to send a custom message on thread creation or not. Default false. - Defaulting Boolean

---
### Command name: `auto-threading disable`
**Description**: Stop automatically creating threads in this channel.

**Required Member Permissions**: Manage Channels

* **Arguments**:
None
---
### Command name: `auto-threading list`
**Description**: List all the auto-threaded channels in this server, if any.

* **Arguments**:
None
---
### Command name: `auto-threading view`
**Description**: View the settings of an auto-threaded channel

**Required Member Permissions**: Manage Channels

* **Arguments**:
	* `channel` - The channel to view the auto-threading settings for. - Channel

---
### Command name: `auto-threading add-roles`
**Description**: Add extra to threads in auto-threaded channels

**Additional Information**: This command will add roles to be pinged alongside the default ping role for this auto-threaded channel

**Required Member Permissions**: Manage Channels

* **Arguments**:
	* `role` - A role to invite to threads in this channel - Optional Role

---
### Command name: `auto-threading remove-roles`
**Description**: Remove extra from threads in auto-threaded channels

**Additional Information**: This command will remove roles that have been added to be pinged alongside the default ping role for this auto-threaded channel

**Required Member Permissions**: Manage Channels

* **Arguments**:
	* `role` - A role to invite to threads in this channel - Optional Role

---
### Command name: `clear count`
**Description**: Clear a specific count of messages

**Required Member Permissions**: Manage Messages

* **Arguments**:
	* `messages` - Number of messages to delete - Int
	* `author` - The author of the messages to clear - Optional User

---
### Command name: `clear before`
**Description**: Clear messages before a given message ID

**Required Member Permissions**: Manage Messages

* **Arguments**:
	* `before` - The ID of the message to clear before - Snowflake
	* `message-count` - The number of messages to clear - Optional Int/Long
	* `author` - The author of the messages to clear - Optional User

---
### Command name: `clear after`
**Description**: Clear messages before a given message ID

**Required Member Permissions**: Manage Messages

* **Arguments**:
	* `after` - The ID of the message to clear after - Snowflake
	* `message-count` - The number of messages to clear - Optional Int/Long
	* `author` - The author of the messages to clear - Optional User

---
### Command name: `clear between`
**Description**: Clear messages between 2 message IDs

**Required Member Permissions**: Manage Messages

* **Arguments**:
	* `after` - The ID of the message to clear after - Snowflake
	* `before` - The ID of the message to clear before - Snowflake
	* `author` - The author of the messages to clear - Optional User

---
### Command name: `config logging`
**Description**: Configure Lily's logging system

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `enable-delete-logs` - Enable logging of message deletions - Boolean
	* `enable-edit-logs` - Enable logging of message edits - Boolean
	* `enable-member-logging` - Enable logging of members joining and leaving the guild - Boolean
	* `enable-public-member-logging` - Enable logging of members joining and leaving the guild with a public message and ping if enabled - Boolean
	* `message-logs` - The channel for logging message deletions - Optional Channel
	* `member-log` - The channel for logging members joining and leaving the guild - Optional Channel
	* `public-member-log` - The channel for the public logging of members joining and leaving the guild - Optional Channel

---
### Command name: `config moderation`
**Description**: Configure Lily's moderation system

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `enable-moderation` - Whether to enable the moderation system - Boolean
	* `moderator-role` - The role of your moderators, used for pinging in message logs. - Optional Role
	* `action-log` - The channel used to store moderator actions. - Optional Channel
	* `quick-timeout-length` - The length of timeouts to use for quick timeouts - Coalescing Optional Duration
	* `warn-auto-punishments` - Whether to automatically punish users for reach a certain threshold on warns - Optional Boolean
	* `log-publicly` - Whether to log moderation publicly or not. - Optional Boolean
	* `dm-default` - The default value for whether to DM a user in a ban action or not. - Optional Boolean
	* `ban-dm-message` - A custom message to send to users when they are banned. - Optional String
	* `auto-invite-moderator-role` - Silently ping moderators to invite them to new threads. - Optional Boolean
	* `log-member-role-changes` - Whether to log changes to the roles members have in a guild. - Optional Boolean

---
### Command name: `config utility`
**Description**: Configure Lily's utility settings

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `utility-log` - The channel to log various utility actions too. - Optional Channel
	* `log-channel-updates` - Whether to log changes made to channels in this guild. - Defaulting Boolean
	* `log-event-updates` - Whether to log changes made to scheduled events in this guild. - Defaulting Boolean
	* `log-invite-updates` - Whether to log changes made to invites in this guild. - Defaulting Boolean
	* `log-role-updates` - Whether to log changes made to roles in this guild. - Defaulting Boolean

---
### Command name: `config clear`
**Description**: Clear a config type

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `config-type` - The type of config to clear - String Choice

---
### Command name: `config view`
**Description**: View the current config that you have set

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `config-type` - The type of config to clear - String Choice

---
### Command name: `gallery-channel set`
**Description**: Set a channel as a gallery channel

**Required Member Permissions**: Manage Server

* **Arguments**:
None
---
### Command name: `gallery-channel unset`
**Description**: Unset a channel as a gallery channel.

**Required Member Permissions**: Manage Server

* **Arguments**:
None
---
### Command name: `gallery-channel list`
**Description**: List all gallery channels in the guild

* **Arguments**:
None
---
### Command name: `github issue`
**Description**: Look up an issue on a specific repository

* **Arguments**:
	* `issue-number` - The issue number you would like to search for - Int
	* `repository` - The GitHub repository you would like to search if no default is set - Optional String

---
### Command name: `github repo`
**Description**: Search GitHub for a specific repository

* **Arguments**:
	* `repository` - The GitHub repository you would like to search if no default is set - Optional String

---
### Command name: `github user`
**Description**: Search GitHub for a User/Organisation

* **Arguments**:
	* `username` - The name of the User/Organisation you wish to search for - String

---
### Command name: `github default-repo`
**Description**: Set the default repo to look up issues in.

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `default-repo` - The default repo to look up issues in - String

---
### Command name: `github remove-default-repo`
**Description**: Removes the default repo for this guild

**Required Member Permissions**: Moderate Members

* **Arguments**:
None
---
### Command name: `announcement`
**Description**: Send an announcement to all guilds Lily is in

**Required Member Permissions**: Administrator

* Arguments:
	* `target-guild` - The guild to send the announcement too - Optional Snowflake

---
### Command name: `help`
**Description**: Get help with using Lily!

* Arguments:
None
---
### Command name: `invite`
**Description**: Get an invitation link for Lily!

* Arguments:
None
---
### Command name: `lock channel`
**Description**: Lock a channel so those with default permissions cannot send messages

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `channel` - Channel to lock. Defaults to current channel - Optional Channel
	* `reason` - Reason for locking the channel - Defaulting String

---
### Command name: `lock server`
**Description**: Lock the server so those with default permissions cannot send messages

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `reason` - Reason for locking the server - Defaulting String

---
### Command name: `unlock channel`
**Description**: Unlock a channel so everyone can send messages again

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `channel` - Channel to unlock. Defaults to current channel - Optional Channel

---
### Command name: `unlock server`
**Description**: Unlock the server so everyone can send messages again

**Required Member Permissions**: Moderate Members

* **Arguments**:
None
---
### Command name: `say`
**Description**: Say something through Lily.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `message` - The text of the message to be sent. - String
	* `channel` - The channel the message should be sent in. - Optional Channel
	* `embed` - If the message should be sent as an embed. - Defaulting Boolean
	* `timestamp` - If the message should be sent with a timestamp. Only works with embeds. - Defaulting Boolean
	* `color` - The color of the embed. Can be either a hex code or one of Discord's supported colors. Embeds only - Defaulting Color

---
### Command name: `edit-say`
**Description**: Edit a message created by /say

**Required Member Permissions**: Moderate Members

* Arguments:
	* `message-to-edit` - The ID of the message you'd like to edit - Snowflake
	* `new-content` - The new content of the message - Optional String
	* `new-color` - The new color of the embed. Embeds only - Optional Color
	* `channel-of-message` - The channel of the message - Optional Channel
	* `timestamp` - Whether to timestamp the embed or not. Embeds only - Optional Boolean

---
### Command name: `status set`
**Description**: Set a custom status for Lily.

**Required Member Permissions**: Administrator

* **Arguments**:
	* `presence` - The new value Lily's presence should be set to - String

---
### Command name: `status reset`
**Description**: Reset Lily's presence to the default status.

**Required Member Permissions**: Administrator

* **Arguments**:
None
---
### Command name: `reset`
**Description**: 'Resets' Lily for this guild by deleting all database information relating to this guild

**Required Member Permissions**: Administrator

* Arguments:
None
---
### Command name: `ban`
**Description**: Bans a user.

**Required Member Permissions**: Ban Members

* Arguments:
	* `user` - Person to ban - User
	* `delete-message-days` - The number of days worth of messages to delete - Int
	* `reason` - The reason for the ban - Defaulting String
	* `soft-ban` - Weather to soft-ban this user (unban them once messages are deleted) - Defaulting Boolean
	* `dm` - Whether to send a direct message to the user about the ban - Optional Boolean
	* `image` - An image you'd like to provide as extra context for the action - Optional Attachment

---
### Command name: `temp-ban add`
**Description**: Temporarily bans a user

**Required Member Permissions**: Ban Members

* **Arguments**:
	* `user` - Person to ban - User
	* `delete-message-days` - The number of days worth of messages to delete - Int
	* `duration` - The duration of the temporary ban. - Coalescing Duration
	* `reason` - The reason for the ban - Defaulting String
	* `dm` - Whether to send a direct message to the user about the ban - Optional Boolean
	* `image` - An image you'd like to provide as extra context for the action - Optional Attachment

---
### Command name: `temp-ban view-all`
**Description**: View all temporary bans for this guild

**Required Member Permissions**: Ban Members

* **Arguments**:
None
---
### Command name: `unban`
**Description**: Unbans a user.

**Required Member Permissions**: Ban Members

* Arguments:
	* `user` - Person to un-ban - User
	* `reason` - The reason for the un-ban - Defaulting String

---
### Command name: `kick`
**Description**: Kicks a user.

**Required Member Permissions**: Kick Members

* Arguments:
	* `user` - Person to kick - User
	* `reason` - The reason for the Kick - Defaulting String
	* `dm` - Whether to send a direct message to the user about the kick - Optional Boolean
	* `image` - An image you'd like to provide as extra context for the action - Optional Attachment

---
### Command name: `timeout`
**Description**: Times out a user.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `user` - Person to timeout - User
	* `duration` - Duration of timeout - Coalescing Optional Duration
	* `reason` - Reason for timeout - Defaulting String
	* `dm` - Whether to send a direct message to the user about the timeout - Optional Boolean
	* `image` - An image you'd like to provide as extra context for the action - Optional Attachment

---
### Command name: `remove-timeout`
**Description**: Removes a timeout from a user

**Required Member Permissions**: Moderate Members

* Arguments:
	* `user` - Person to remove timeout from - User
	* `dm` - Whether to dm the user about this or not - Optional Boolean

---
### Command name: `warn`
**Description**: Warns a user.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `user` - Person to warn - User
	* `reason` - Reason for warning - Defaulting String
	* `dm` - Whether to send a direct message to the user about the warning - Optional Boolean
	* `image` - An image you'd like to provide as extra context for the action - Optional Attachment

---
### Command name: `remove-warn`
**Description**: Removes a user's warnings

**Required Member Permissions**: Moderate Members

* Arguments:
	* `user` - Person to remove warn from - User
	* `dm` - Whether to send a direct message to the user about the warning - Defaulting Boolean

---
### Command name: `news-publishing set`
**Description**: Set this channel to automatically publish messages.

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `channel` - The channel to set auto-publishing for - Channel

---
### Command name: `news-publishing remove`
**Description**: Stop a news channel from auto-publishing messages

**Required Member Permissions**: Manage Server

* **Arguments**:
	* `channel` - The channel to stop auto-publishing for - Channel

---
### Command name: `news-publishing list`
**Description**: List Auto-publishing channels

**Required Member Permissions**: Manage Server

* **Arguments**:
None
---
### Command name: `news-publishing remove-all`
**Description**: Remove all auto-publishing channels for this guild

**Required Member Permissions**: Manage Server

* **Arguments**:
None
---
### Command name: `ping`
**Description**: Am I alive?

* Arguments:
None
---
### Command name: `nickname request`
**Description**: Request a new nickname for the server!

* **Arguments**:
	* `nickname` - The new nickname you would like - String

---
### Command name: `nickname clear`
**Description**: Clear your current nickname

* **Arguments**:
None
---
### Command name: `reminder set`
**Description**: Set a reminder for some time in the future!

* **Arguments**:
	* `time` - How long until reminding? Format: 1d12h30m / 3d / 26m30s - Coalescing Duration
	* `remind-in-dm` - Whether to remind in DMs or not - Boolean
	* `custom-message` - A message to attach to your reminder - Optional String
	* `repeat` - Whether to repeat the reminder or not - Defaulting Boolean
	* `repeat-interval` - The interval to repeat the reminder at. Format: 1d / 1h / 5d - Coalescing Optional Duration

---
### Command name: `reminder list`
**Description**: List your reminders for this guild

* **Arguments**:
None
---
### Command name: `reminder remove`
**Description**: Remove a reminder you have set from this guild

* **Arguments**:
	* `reminder-number` - The number of the reminder to remove. Use '/reminder list' to get this - Long

---
### Command name: `reminder remove-all`
**Description**: Remove all a specific type of reminder from this guild

* **Arguments**:
	* `reminder-type` - The type of reminder to remove - String Choice

---
### Command name: `reminder mod-list`
**Description**: List all reminders for a user, if you're a moderator

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `user` - The user to view reminders for - User

---
### Command name: `reminder mod-remove`
**Description**: Remove a reminder for a user, if you're a moderator

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `user` - The user to remove the reminder for - User
	* `reminder-number` - The number of the reminder to remove. Use '/reminder mod-list' to get this - Long

---
### Command name: `reminder mod-remove-all`
**Description**: Remove all a specific type of reminder for a user, if you're a moderator

**Required Member Permissions**: Moderate Members

* **Arguments**:
	* `user` - The user to remove the reminders for - User
	* `reminder-type` - The type of reminder to remove - String Choice

---
### Command name: `manual-report`
**Description**: Report a message, using a link instead of the message command

* Arguments:
	* `message-link` - Link to the message to report - String

---
### Command name: `role-menu create`
**Description**: Create a new role menu in this channel. A channel can have any number of role menus.

**Required Member Permissions**: Manage Roles

* **Arguments**:
	* `role` - The first role to start the menu with. Add more via `/role-menu add` - Role
	* `content` - The content of the embed or message. - String
	* `embed` - If the message containing the role menu should be sent as an embed. - Defaulting Boolean
	* `color` - The color for the message to be. Embed only. - Defaulting Color

---
### Command name: `role-menu add`
**Description**: Add a role to the existing role menu in this channel.

**Required Member Permissions**: Manage Roles

* **Arguments**:
	* `menu-id` - The message ID of the role menu you'd like to edit. - Snowflake
	* `role` - The role you'd like to add to the selected role menu. - Role

---
### Command name: `role-menu remove`
**Description**: Remove a role from the existing role menu in this channel.

**Required Member Permissions**: Manage Messages

* **Arguments**:
	* `menu-id` - The message ID of the menu you'd like to edit. - Snowflake
	* `role` - The role you'd like to remove from the selected role menu. - Role

---
### Command name: `role-menu pronouns`
**Description**: Create a pronoun selection role menu and the roles to go with it.

**Required Member Permissions**: Manage Messages

* **Arguments**:
None
---
### Command name: `role-subscription update`
**Description**: Update your role subscription

* **Arguments**:
None
---
### Command name: `role-subscription add-role`
**Description**: Add a role that can be added through role subscription commands

**Required Member Permissions**: Manage Server, Manage Roles

* **Arguments**:
	* `role` - A role to add or remove from the subscribable roles - Role

---
### Command name: `role-subscription remove-role`
**Description**: Remove a role that can be added through role subscription commands

**Required Member Permissions**: Manage Server, Manage Roles

* **Arguments**:
	* `role` - A role to add or remove from the subscribable roles - Role

---
### Command name: `tag-preview`
**Description**: Preview a tag's contents without sending it publicly.

* Arguments:
	* `name` - The name of the tag - String

---
### Command name: `tag`
**Description**: Call a tag from this guild! Use /tag-help for more info.

* Arguments:
	* `name` - The name of the tag you want to call - String
	* `user` - The user to mention with the tag (optional) - Optional User

---
### Command name: `tag-help`
**Description**: Explains how the tag command works!

* Arguments:
None
---
### Command name: `tag-create`
**Description**: Create a tag for your guild! Use /tag-help for more info.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `name` - The name of the tag you're making - String
	* `title` - The title of the tag embed you're making - String
	* `value` - The content of the tag embed you're making - String
	* `appearance` - The appearance of the tag embed you're making - String Choice

---
### Command name: `tag-delete`
**Description**: Delete a tag from your guild. Use /tag-help for more info.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `name` - The name of the tag - String

---
### Command name: `tag-edit`
**Description**: Edit a tag in your guild. Use /tag-help for more info.

**Required Member Permissions**: Moderate Members

* Arguments:
	* `name` - The name of the tag you're editing - String
	* `new-name` - The new name for the tag you're editing - Optional String
	* `new-title` - The new title for the tag you're editing - Optional String
	* `new-value` - The new value for the tag you're editing - Optional String
	* `new-appearance` - The new appearance for the tag you're editing - Optional String

---
### Command name: `tag-list`
**Description**: List all tags for this guild

* Arguments:
None
---
### Command name: `thread rename`
**Description**: Rename a thread!

* **Arguments**:
	* `new-name` - The new name to give to the thread - String

---
### Command name: `thread archive`
**Description**: Archive this thread

* **Arguments**:
	* `lock` - Whether to lock the thread if you are a moderator. Default is false - Defaulting Boolean

---
### Command name: `thread transfer`
**Description**: Transfer ownership of this thread

* **Arguments**:
	* `new-owner` - The user you want to transfer ownership of the thread to - Member

---
### Command name: `thread prevent-archiving`
**Description**: Stop a thread from being archived

* **Arguments**:
None
---
### Command name: `blocks`
**Description**: Get a list of the configured blocks.

* Arguments:
	* `channel` - Message channel representing a Welcome Channel. - Channel

---
### Command name: `welcome-channels delete`
**Description**: Delete a Welcome Channel configuration.

* **Arguments**:
	* `channel` - Message channel representing a Welcome Channel. - Channel

---
### Command name: `welcome-channels get`
**Description**: Get the url for a configured Welcome Channel.

* **Arguments**:
	* `channel` - Message channel representing a Welcome Channel. - Channel

---
### Command name: `welcome-channels refresh`
**Description**: Manually repopulate the given Welcome Channel.

* **Arguments**:
	* `channel` - Message channel representing a Welcome Channel. - Channel
	* `clear` - Whether to clear and repopulate the channel instead of updating it. - Defaulting Boolean

---
### Command name: `welcome-channels set`
**Description**: Set the URL for a Welcome Channel and populate it.

* **Arguments**:
	* `channel` - Message channel representing a Welcome Channel. - Channel
	* `url` - Public link to a Welcome Channel configuration YAML file. - String
	* `clear` - Whether to clear and repopulate the channel instead of updating it. - Defaulting Boolean

---
### Command name: `url-safety-check`
**Description**: Check whether a given domain is a known unsafe domain

* Arguments:
	* `domain` - Domain to check - String

---
### Command name: `pluralkit api-url`
**Description**: Set a custom API URL, "reset" to reset

* **Arguments**:
	* `api-url` - Set an alternative API URL, or "reset" to use the default - Optional String

---
### Command name: `pluralkit bot`
**Description**: Pick your custom PluralKit instance, if you have one

* **Arguments**:
	* `bot` - Select an alternative PK instance, if needed - Optional User

---
### Command name: `pluralkit status`
**Description**: Check the settings for this server's PluralKit integration

* **Arguments**:
None
---
### Command name: `pluralkit toggle`
**Description**: Disable or enable the PluralKit integration as required

* **Arguments**:
	* `enable` - Set whether the PK integration should be used on this server - Optional Boolean

---
### Command name: `command-list`
**Description**: Shows a list of LilyBot's commands!

* Arguments:
None
---
## Message Commands

### Message Command: `Moderate`

**Required Member Permissions**: Kick Members, Ban Members, Moderate Members

---
### Message Command: `Report`

---
### Message Command: `URL Safety Check`

---
## User Commands

None

---
