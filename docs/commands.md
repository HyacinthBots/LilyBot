## Slash Commands

### Parent command name: `config`
* **Parent command description**: Configure Lily's settings
	#### Sub-command name: `support`
	* **Sub-command description**: Configure Lily's support system
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: enable-support
			* **Description**: Whether to enable the support system
			* **Type**: Boolean
			* **Name**: custom-message
			* **Description**: True if you'd like to add a custom message, false if you'd like the default.
			* **Type**: Boolean
			* **Name**: support-channel
			* **Description**: The channel to be used for creating support threads in.
			* **Type**: Optional Channel
			* **Name**: support-role
			* **Description**: The role to add to support threads, when one is created.
			* **Type**: Optional Role

	#### Sub-command name: `moderation`
	* **Sub-command description**: Configure Lily's moderation system
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: enable-moderation
			* **Description**: Whether to enable the moderation system
			* **Type**: Boolean
			* **Name**: moderator-role
			* **Description**: The role of your moderators, used for pinging in message logs.
			* **Type**: Optional Role
			* **Name**: action-log
			* **Description**: The channel used to store moderator actions.
			* **Type**: Optional Channel
			* **Name**: quick-timeout-length
			* **Description**: The length of timeouts to use for quick timeouts
			* **Type**: Coalescing Optional Duration
			* **Name**: warn-auto-punishments
			* **Description**: Whether to automatically punish users for reach a certain threshold on warns
			* **Type**: Optional Boolean
			* **Name**: log-publicly
			* **Description**: Whether to log moderation publicly or not.
			* **Type**: Optional Boolean

	#### Sub-command name: `logging`
	* **Sub-command description**: Configure Lily's logging system
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: enable-delete-logs
			* **Description**: Enable logging of message deletions
			* **Type**: Boolean
			* **Name**: enable-edit-logs
			* **Description**: Enable logging of message edits
			* **Type**: Boolean
			* **Name**: enable-member-logging
			* **Description**: Enable logging of members joining and leaving the guild
			* **Type**: Boolean
			* **Name**: enable-public-member-logging
			* **Description**: Enable logging of members joining and leaving the guild with a public message and ping if enabled
			* **Type**: Boolean
			* **Name**: message-logs
			* **Description**: The channel for logging message deletions
			* **Type**: Optional Channel
			* **Name**: member-log
			* **Description**: The channel for logging members joining and leaving the guild
			* **Type**: Optional Channel
			* **Name**: public-member-log
			* **Description**: The channel for the public logging of members joining and leaving the guild
			* **Type**: Optional Channel

	#### Sub-command name: `utility`
	* **Sub-command description**: Configure Lily's utility settings
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: disable-log-uploading
			* **Description**: Enable or disable log uploading for this guild
			* **Type**: Boolean
			* **Name**: utility-log
			* **Description**: The channel to log various utility actions too.
			* **Type**: Optional Channel

	#### Sub-command name: `clear`
	* **Sub-command description**: Clear a config type
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: config-type
			* **Description**: The type of config to clear
			* **Type**: String Choice

	#### Sub-command name: `view`
	* **Sub-command description**: View the current config that you have set
	* Required Member Permissions: Manage Server

		* **Arguments**:
			* **Name**: config-type
			* **Description**: The type of config to clear
			* **Type**: String Choice

### Parent command name: `github`
* **Parent command description**: The parent command for all /github commands
	#### Sub-command name: `issue`
	* **Sub-command description**: Look up an issue on a specific repository

		* **Arguments**:
			* **Name**: issue-number
			* **Description**: The issue number you would like to search for
			* **Type**: Int
			* **Name**: repository
			* **Description**: The GitHub repository you would like to search if no default is set
			* **Type**: Optional String

	#### Sub-command name: `repo`
	* **Sub-command description**: Search GitHub for a specific repository

		* **Arguments**:
			* **Name**: repository
			* **Description**: The GitHub repository you would like to search if no default is set
			* **Type**: Optional String

	#### Sub-command name: `user`
	* **Sub-command description**: Search github for a User/Organisation

		* **Arguments**:
			* **Name**: username
			* **Description**: The name of the User/Organisation you wish to search for
			* **Type**: String

	#### Sub-command name: `default-repo`
	* **Sub-command description**: Set the default repo to look up issues in.
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: default-repo
			* **Description**: The default repo to look up issues in
			* **Type**: String

	#### Sub-command name: `remove-default-repo`
	* **Sub-command description**: Removes the default repo for this guild
	* Required Member Permissions: Moderate Members

		* **Arguments**:
None
### Parent command name: `gallery-channel`
* **Parent command description**: The parent command for image channel setting
	#### Sub-command name: `set`
	* **Sub-command description**: Set a channel as a gallery channel
	* Required Member Permissions: Manage Server

		* **Arguments**:
None
	#### Sub-command name: `unset`
	* **Sub-command description**: Unset a channel as a gallery channel.
	* Required Member Permissions: Manage Server

		* **Arguments**:
None
	#### Sub-command name: `list`
	* **Sub-command description**: List all gallery channels in the guild

		* **Arguments**:
None
### Command name: `command-list`
* Description: Show a list of Lily's commands!

	* Arguments:
None
### Command name: `help`
* Description: Get help with using Lily!

	* Arguments:
None
### Command name: `info`
* Description: Learn about Lily, and get uptime data!

	* Arguments:
None
### Command name: `invite`
* Description: Get an invite link for Lily!

	* Arguments:
None
### Command name: `announcement`
* Description: Send an announcement to all guilds Lily is in
	* Required Member Permissions: Administrator

	* Arguments:
None
### Parent command name: `lock`
* **Parent command description**: The parent command for all locking commands
	#### Sub-command name: `channel`
	* **Sub-command description**: Lock a channel so those with default permissions cannot send messages
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel to lock. Defaults to current channel
			* **Type**: Optional Channel
			* **Name**: reason
			* **Description**: Reason for locking the channel
			* **Type**: Defaulting String

	#### Sub-command name: `server`
	* **Sub-command description**: Lock the server so those with default permissions cannot send messages
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: reason
			* **Description**: Reason for locking the server
			* **Type**: Defaulting String

### Parent command name: `unlock`
* **Parent command description**: The parent command for all unlocking commands
	#### Sub-command name: `channel`
	* **Sub-command description**: Unlock a channel so everyone can send messages again
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel to unlock. Defaults to current channel
			* **Type**: Optional Channel

	#### Sub-command name: `server`
	* **Sub-command description**: Unlock the server so everyone can send messages again
	* Required Member Permissions: Moderate Members

		* **Arguments**:
None
### Parent command name: `log-uploading`
* **Parent command description**: The parent command for blacklisting channels from running the log uploading code
	#### Sub-command name: `blacklist-add`
	* **Sub-command description**: Add a channel to the log uploading blacklist
	* Required Member Permissions: Moderate Members

		* **Arguments**:
None
	#### Sub-command name: `blacklist-remove`
	* **Sub-command description**: Remove a channel from the log uploading blacklist
	* Required Member Permissions: Moderate Members

		* **Arguments**:
None
	#### Sub-command name: `blacklist-list`
	* **Sub-command description**: List all channels that block log uploading
	* Required Member Permissions: Moderate Members

		* **Arguments**:
None
### Command name: `ban`
* Description: Bans a user.
	* Required Member Permissions: Ban Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to ban
		* **Type**: User
		* **Name**: delete-message-days
		* **Description**: The number of days worth of messages to delete
		* **Type**: Int
		* **Name**: reason
		* **Description**: The reason for the ban
		* **Type**: Defaulting String
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean
		* **Name**: image
		* **Description**: An image you'd like to provide as extra context for the action
		* **Type**: Optional Attachment

### Command name: `soft-ban`
* Description: Soft-bans a user.
	* Required Member Permissions: Ban Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to Soft ban
		* **Type**: User
		* **Name**: delete-message-days
		* **Description**: The number of days worth of messages to delete
		* **Type**: Optional Int/Long
		* **Name**: reason
		* **Description**: The reason for the ban
		* **Type**: Defaulting String
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean
		* **Name**: image
		* **Description**: An image you'd like to provide as extra context for the action
		* **Type**: Optional Attachment

### Command name: `unban`
* Description: Unbans a user.
	* Required Member Permissions: Ban Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to un-ban
		* **Type**: User
		* **Name**: reason
		* **Description**: The reason for the un-ban
		* **Type**: Defaulting String

### Command name: `kick`
* Description: Kicks a user.
	* Required Member Permissions: Kick Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to kick
		* **Type**: User
		* **Name**: reason
		* **Description**: The reason for the Kick
		* **Type**: Defaulting String
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean
		* **Name**: image
		* **Description**: An image you'd like to provide as extra context for the action
		* **Type**: Optional Attachment

### Command name: `clear`
* Description: Clears messages from a channel.
	* Required Member Permissions: Manage Messages

	* Arguments:
		* **Name**: messages
		* **Description**: Number of messages to delete
		* **Type**: Int

### Command name: `timeout`
* Description: Times out a user.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to timeout
		* **Type**: User
		* **Name**: duration
		* **Description**: Duration of timeout
		* **Type**: Coalescing Optional Duration
		* **Name**: reason
		* **Description**: Reason for timeout
		* **Type**: Defaulting String
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean
		* **Name**: image
		* **Description**: An image you'd like to provide as extra context for the action
		* **Type**: Optional Attachment

### Command name: `remove-timeout`
* Description: Removes a timeout from a user
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to remove timeout from
		* **Type**: User
		* **Name**: dm
		* **Description**: Whether to dm the user about this or not
		* **Type**: Defaulting Boolean

### Command name: `warn`
* Description: Warns a user.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to warn
		* **Type**: User
		* **Name**: reason
		* **Description**: Reason for warn
		* **Type**: Defaulting String
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean
		* **Name**: image
		* **Description**: An image you'd like to provide as extra context for the action
		* **Type**: Optional Attachment

### Command name: `remove-warn`
* Description: Removes a user's warnings
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: user
		* **Description**: Person to remove warn from
		* **Type**: User
		* **Name**: dm
		* **Description**: Whether to send a direct message to the user about the warn
		* **Type**: Defaulting Boolean

### Command name: `say`
* Description: Say something through Lily.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: message
		* **Description**: The text of the message to be sent.
		* **Type**: String
		* **Name**: channel
		* **Description**: The channel the message should be sent in.
		* **Type**: Optional Channel
		* **Name**: embed
		* **Description**: If the message should be sent as an embed.
		* **Type**: Defaulting Boolean
		* **Name**: timestamp
		* **Description**: If the message should be sent with a timestamp. Only works with embeds.
		* **Type**: Defaulting Boolean
		* **Name**: color
		* **Description**: The color of the embed. Can be either a hex code or one of Discord's supported colors. Embeds only
		* **Type**: Defaulting Color

### Command name: `edit-say`
* Description: Edit a message created by /say
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: message-to-edit
		* **Description**: The ID of the message you'd like to edit
		* **Type**: Snowflake
		* **Name**: new-content
		* **Description**: The new content of the message
		* **Type**: Optional String
		* **Name**: new-color
		* **Description**: The new color of the embed. Embeds only
		* **Type**: Optional Color
		* **Name**: channel-of-message
		* **Description**: The channel of the message
		* **Type**: Optional Channel
		* **Name**: timestamp
		* **Description**: Whether to timestamp the embed or not. Embeds only
		* **Type**: Optional Boolean

### Parent command name: `status`
* **Parent command description**: Set Lily's current presence/status.
	#### Sub-command name: `set`
	* **Sub-command description**: Set a custom status for Lily.
	* Required Member Permissions: Administrator

		* **Arguments**:
			* **Name**: presence
			* **Description**: The new value Lily's presence should be set to
			* **Type**: String

	#### Sub-command name: `reset`
	* **Sub-command description**: Reset Lily's presence to the default status.
	* Required Member Permissions: Administrator

		* **Arguments**:
None
### Command name: `reset`
* Description: 'Resets' Lily for this guild by deleting all database information relating to this guild
	* Required Member Permissions: Administrator

	* Arguments:
None
### Command name: `ping`
* Description: Am I alive?

	* Arguments:
None
### Parent command name: `nickname`
* **Parent command description**: The parent command for all nickname commands
	#### Sub-command name: `request`
	* **Sub-command description**: Request a new nickname for the server!

		* **Arguments**:
			* **Name**: nickname
			* **Description**: The new nickname you would like
			* **Type**: String

	#### Sub-command name: `clear`
	* **Sub-command description**: Clear your current nickname

		* **Arguments**:
None
### Parent command name: `reminder`
* **Parent command description**: The parent command for all reminder commands
	#### Sub-command name: `set`
	* **Sub-command description**: Set a reminder for some time in the future!

		* **Arguments**:
			* **Name**: time
			* **Description**: How long until reminding? Format: 1d12h30m / 3d / 26m30s
			* **Type**: Coalescing Duration
			* **Name**: remind-in-dm
			* **Description**: Whether to remind in DMs or not
			* **Type**: Boolean
			* **Name**: custom-message
			* **Description**: A message to attach to your reminder
			* **Type**: Optional String
			* **Name**: repeat
			* **Description**: Whether to repeat the reminder or not
			* **Type**: Defaulting Boolean
			* **Name**: repeat-interval
			* **Description**: The interval to repeat the reminder at. Format: 1d / 1h / 5d
			* **Type**: Coalescing Optional Duration

	#### Sub-command name: `list`
	* **Sub-command description**: List your reminders for this guild

		* **Arguments**:
None
	#### Sub-command name: `remove`
	* **Sub-command description**: Remove a reminder you have set from this guild

		* **Arguments**:
			* **Name**: reminder-number
			* **Description**: The number of the reminder to remove. Use '/reminder list' to get this
			* **Type**: Long

	#### Sub-command name: `remove-all`
	* **Sub-command description**: Remove all a specific type of reminder from this guild

		* **Arguments**:
			* **Name**: reminder-type
			* **Description**: The type of reminder to remove
			* **Type**: String Choice

	#### Sub-command name: `mod-list`
	* **Sub-command description**: List all reminders for a user, if you're a moderator
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: user
			* **Description**: The user to view reminders for
			* **Type**: User

	#### Sub-command name: `mod-remove`
	* **Sub-command description**: Remove a reminder for a user, if you're a moderator
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: user
			* **Description**: The user to remove the reminder for
			* **Type**: User
			* **Name**: reminder-number
			* **Description**: The number of the reminder to remove. Use '/reminder mod-list' to get this
			* **Type**: Long

	#### Sub-command name: `mod-remove-all`
	* **Sub-command description**: Remove all a specific type of reminder for a user, if you're a moderator
	* Required Member Permissions: Moderate Members

		* **Arguments**:
			* **Name**: user
			* **Description**: The user to remove the reminders for
			* **Type**: User
			* **Name**: reminder-type
			* **Description**: The type of reminder to remove
			* **Type**: String Choice

### Command name: `manual-report`
* Description: Report a message, using a link instead of the message command

	* Arguments:
		* **Name**: message-link
		* **Description**: Link to the message to report
		* **Type**: String

### Parent command name: `role-menu`
* **Parent command description**: The parent command for managing role menus.
	#### Sub-command name: `create`
	* **Sub-command description**: Create a new role menu in this channel. A channel can have any number of role menus.
	* Required Member Permissions: Manage Roles

		* **Arguments**:
			* **Name**: role
			* **Description**: The first role to start the menu with. Add more via `/role-menu add`
			* **Type**: Role
			* **Name**: content
			* **Description**: The content of the embed or message.
			* **Type**: String
			* **Name**: embed
			* **Description**: If the message containing the role menu should be sent as an embed.
			* **Type**: Defaulting Boolean
			* **Name**: color
			* **Description**: The color for the message to be. Embed only.
			* **Type**: Defaulting Color

	#### Sub-command name: `add`
	* **Sub-command description**: Add a role to the existing role menu in this channel.
	* Required Member Permissions: Manage Roles

		* **Arguments**:
			* **Name**: menu-id
			* **Description**: The message ID of the role menu you'd like to edit.
			* **Type**: Snowflake
			* **Name**: role
			* **Description**: The role you'd like to add to the selected role menu.
			* **Type**: Role

	#### Sub-command name: `remove`
	* **Sub-command description**: Remove a role from the existing role menu in this channel.
	* Required Member Permissions: Manage Messages

		* **Arguments**:
			* **Name**: menu-id
			* **Description**: The message ID of the menu you'd like to edit.
			* **Type**: Snowflake
			* **Name**: role
			* **Description**: The role you'd like to remove from the selected role menu.
			* **Type**: Role

	#### Sub-command name: `pronouns`
	* **Sub-command description**: Create a pronoun selection role menu and the roles to go with it.
	* Required Member Permissions: Manage Messages

		* **Arguments**:
None
### Command name: `tag-preview`
* Description: Preview a tag's contents without sending it publicly.

	* Arguments:
		* **Name**: name
		* **Description**: The name of the tag
		* **Type**: String

### Command name: `tag`
* Description: Call a tag from this guild! Use /tag-help for more info.

	* Arguments:
		* **Name**: name
		* **Description**: The name of the tag you want to call
		* **Type**: String
		* **Name**: user
		* **Description**: The user to mention with the tag (optional)
		* **Type**: Optional User

### Command name: `tag-help`
* Description: Explains how the tag command works!

	* Arguments:
None
### Command name: `tag-create`
* Description: Create a tag for your guild! Use /tag-help for more info.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: name
		* **Description**: The name of the tag you're making
		* **Type**: String
		* **Name**: title
		* **Description**: The title of the tag embed you're making
		* **Type**: String
		* **Name**: value
		* **Description**: The content of the tag embed you're making
		* **Type**: String
		* **Name**: appearance
		* **Description**: The appearance of the tag embed you're making
		* **Type**: String Choice

### Command name: `tag-delete`
* Description: Delete a tag from your guild. Use /tag-help for more info.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: name
		* **Description**: The name of the tag
		* **Type**: String

### Command name: `tag-edit`
* Description: Edit a tag in your guild. Use /tag-help for more info.
	* Required Member Permissions: Moderate Members

	* Arguments:
		* **Name**: name
		* **Description**: The name of the tag you're editing
		* **Type**: String
		* **Name**: new-name
		* **Description**: The new name for the tag you're editing
		* **Type**: Optional String
		* **Name**: new-title
		* **Description**: The new title for the tag you're editing
		* **Type**: Optional String
		* **Name**: new-value
		* **Description**: The new value for the tag you're editing
		* **Type**: Optional String
		* **Name**: new-appearance
		* **Description**: The new appearance for the tag you're editing
		* **Type**: Optional String

### Command name: `tag-list`
* Description: List all tags for this guild

	* Arguments:
None
### Parent command name: `thread`
* **Parent command description**: The parent command for all /thread commands
	#### Sub-command name: `rename`
	* **Sub-command description**: Rename a thread!

		* **Arguments**:
			* **Name**: new-name
			* **Description**: The new name to give to the thread
			* **Type**: String

	#### Sub-command name: `archive`
	* **Sub-command description**: Archive this thread

		* **Arguments**:
			* **Name**: lock
			* **Description**: Whether to lock the thread if you are a moderator. Default is false
			* **Type**: Defaulting Boolean

	#### Sub-command name: `transfer`
	* **Sub-command description**: Transfer ownership of this thread

		* **Arguments**:
			* **Name**: new-owner
			* **Description**: The user you want to transfer ownership of the thread to
			* **Type**: Member

	#### Sub-command name: `prevent-archiving`
	* **Sub-command description**: Stop a thread from being archived

		* **Arguments**:
None
### Command name: `blocks`
* Description: Get a list of the configured blocks

	* Arguments:
		* **Name**: channel
		* **Description**: Channel representing a welcome channel
		* **Type**: Channel

### Parent command name: `welcome-channels`
* **Parent command description**: Manage welcome channels
	#### Sub-command name: `delete`
	* **Sub-command description**: Delete a welcome channel configuration

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel representing a welcome channel
			* **Type**: Channel

	#### Sub-command name: `get`
	* **Sub-command description**: Get the url for a welcome channel, if it's configured

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel representing a welcome channel
			* **Type**: Channel

	#### Sub-command name: `refresh`
	* **Sub-command description**: Manually repopulate the given welcome channel

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel representing a welcome channel
			* **Type**: Channel
			* **Name**: clear
			* **Description**: Whether to clear the channel before repopulating it
			* **Type**: Defaulting Boolean

	#### Sub-command name: `set`
	* **Sub-command description**: Set the URL for a welcome channel, and populate it

		* **Arguments**:
			* **Name**: channel
			* **Description**: Channel representing a welcome channel
			* **Type**: Channel
			* **Name**: url
			* **Description**: Public link to a YAML file used to configure a welcome channel
			* **Type**: String
			* **Name**: clear
			* **Description**: Whether to clear the channel before repopulating it
			* **Type**: Defaulting Boolean

### Command name: `phishing-check`
* Description: Check whether a given domain is a known phishing domain.

	* Arguments:
		* **Name**: domain
		* **Description**: Domain to check
		* **Type**: String

### Parent command name: `command.pluralkit.name`
* **Parent command description**: command.pluralkit.description
	#### Sub-command name: `command.pluralkit.api-url.name`
	* **Sub-command description**: command.pluralkit.api-url.description

		* **Arguments**:
			* **Name**: argument.api-url.name
			* **Description**: argument.api-url.description
			* **Type**: Optional String

	#### Sub-command name: `command.pluralkit.bot.name`
	* **Sub-command description**: command.pluralkit.bot.description

		* **Arguments**:
			* **Name**: argument.bot.name
			* **Description**: argument.bot.description
			* **Type**: Optional User

	#### Sub-command name: `command.pluralkit.status.name`
	* **Sub-command description**: command.pluralkit.status.description

		* **Arguments**:
None
	#### Sub-command name: `command.pluralkit.toggle-support.name`
	* **Sub-command description**: command.pluralkit.toggle-support.description

		* **Arguments**:
			* **Name**: argument.toggle.name
			* **Description**: argument.toggle.description
			* **Type**: Optional Boolean

## Message Commands

### Message Command: `Moderate`
	* Required Member Permissions: Kick Members, Ban Members, Moderate Members
### Message Command: `Report`
### Message Command: `Phishing Check`
## User Commands

