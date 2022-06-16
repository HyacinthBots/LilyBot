# Commands List

The following is a list of commands, their arguments, and what they do.

---


## Administration commands
These are commands for the maintenance of LilyBot. The can only be run by Server Managers or Admins

### Name: `config set`
**Arguments**:
* `moderatorRole` - Moderator role that will be pinged for reports - Role
* `modActionLog` - Channel for embeds created by mod actions to be sent - Channel
* `messageLogs` - Channel for deleted messages to be logged - Channel
* `joinChannel` - Channel for members joining or leaving to be logged - Channel
* `supportChannel` - Channel for the support auto-threading system to be used in - Optional Channel
* `supportTeamRole` - The role to be added to any threads created in the `supportchannel` - Optional Role

**Result**: Sets a configuration for the guild executed in.

**Required Permissions**: `Manage Guild`

**Command category**: `Administration commands`

---

### Name: `config clear`
**Arguments**:
None

**Result**: Clears the configuration for the guild executed in.

**Required Permissions**: `Manage Guild`

**Command category**: `Administration commands`

---

### Name: `set-status`
**Arguments**:
* `presence` - Value for Lily's status - String

**Result**: Lily's 'Now Playing:' status is set to `presence`.
This command can only be executed in the test guild specified in your `.env` file

**Required Permissions**: `Administrator`

**Command category**: `Administration commands`

---


## Moderation commands
These commands are for use by moderators. They utilize built-in permission checks. All moderation commands are logged to the modActionLog established in the config. A Direct Message is sent to the target user containing the sanction they received and the provided reason. If Lily fails to DM them, this failure will be noted in the logging embed.

### Name: `clear`
**Arguments**:
* `messages` - Number of messages to delete - Integer

**Result**: Deletes the `messages` latest messages from the channel executed in.

**Required Permissions**: `Manage Messages`

**Command category**: `Moderation commands`

---

### Name: `ban`
**Arguments**:
* `banUser` – Person to ban - User
* `messages` - Number of days of messages to delete - Integer
* `reason` - Reason for the ban - Optional String
* `image` - The URL to an image to provide extra context for the action - Optional String

**Result**: Bans `banUser` from the server with reason `reason` and deletes any messages they sent in the last `messages` day(s).

**Required Permissions**: `Ban Members`

**Command category**: `Moderation commands`

---

### Name: `unban`
**Arguments**:
* `unbanUserId ` - The Discord ID of the person to unban - User ID

**Result**: The user with the ID `unbanUserId` is unbanned.

**Required Permissions**: `Ban Members`

**Command category**: `Moderation commands`

---

### Name: `soft-ban`
**Arguments**:
* `softBanUser` - Person to soft ban - User
* `messages` - Number of days of messages to delete - Integer (default 3)
* `reason` - Reason for the ban - Optional String
* `image` - The URL to an image to provide extra context for the action - Optional String

**Result**: Bans `softBanUser`, deletes the last `messages` days of messages from them, and unbans them.

**Required Permissions**: `Ban Members`

**Command category**: `Moderation commands`

---

### Name: `warn`
**Arguments**:
* `warnUser` - Person to warn - User
* `reason` - Reason for warn - Optional String
* `image` - The URL to an image to provide extra context for the action - Optional String

**Result**: Warns `warnUser` with a DM and adds a strike to their points total. Depending on their new points total, action is taken based on the below table.

| Points |     Sanction     |
|:------:|:----------------:|
|   1    |      None.       |
|   2    | 3 hour timeout.  |
|   3    | 12 hour timeout. |
|   3+   |  3 day timeout.  |

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---

### Name: `timeout`
**Arguments**:
* `timeoutUser` - Person to timeout - User
* `duration` - Duration of timeout - Duration [e.g. 6h or 30s] (default 6h)
* `reason` - Reason for timeout - Optional String
* `image` - The URL to an image to provide extra context for the action - Optional String

**Result**: Times `timeoutUser` out for `duration`. A timeout is Discord's built-in mute function.

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---

### Name: `lock-channel`
**Arguments**:
* `channel` - Channel to lock - Channel (default executed channel)
* `reason` - Reason for locking the channel - Optional String

**Result**: Locks `channel` so only the moderator role can send messages, create threads, or add reactions.

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---

### Name: `lock-server`
**Arguments**:
* `reason` - Reason for locking the server - Optional String

**Result**: Locks the whole server so only members with the moderator role can send messages, create threads, or add reactions.

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---

### Name: `unlock-channel`
**Arguments**:
* `channel` - Channel to unlock - Channel (default executed channel)

**Result**: Unlocks `channel` so anyone can send messages, create threads, or add reactions.

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---

### Name: `unlock-server`
**Arguments**:
None

**Result**: Unlocks the whole server so anyone can send messages, create threads, or add reations.

**Required Permissions**: `Moderate Members`

**Command category**: `Moderation commands`

---


## Utility commands
These commands are just handy to have around. Moderator only commands are at the top and clearly marked.

### Name: `role-menu create`
**Arguments**:
(Moderator only)
* `role` - A role to start the menu with - Role
* `content` - Content of the embed to be created along with the menu - String
* `embed` - If the message should be an embed` - Optional Boolean (default: true)
* `color` - Color for the embed - Optional Color (default: black)

**Result**: Creates a menu with a button attached to a `color` colored embed with content `content`. Pressing the button allows the user to select roles.

**Required Permissions**: `Manage Roles`

**Command category**: `Utility commands`

---

### Name: `role-menu add`
**Arguments**:
(Moderator only)
* `menuId` - The message ID of the role menu to edit - Snowflake
* `role` - The role to add to the menu - String

**Result**: Adds the `role` to the menu associated with `menuId`.

**Required Permissions**: `Manage Roles`

**Command category**: `Utility commands`

---

### Name: `role-menu remove`
**Arguments**:
(Moderator only)
* `menuId` - The message ID of the role menu to edit - Snowflake
* `role` - The role to remove from the menu - String

**Result**: Removes the `role` from the menu associated with `menuId`.

**Required Permissions**: `Manage Roles`

**Command category**: `Utility commands`

---

### Name: `role-menu pronouns`
**Arguments**:
None

**Result**: Creates a role menu and associated roles (if needed) to select pronouns.

**Required Permissions**: `Manage Roles`

**Command category**: `Utility commands`

---

### Name: `say`
**Arguments**:
(Moderator only)
* `message` - Message to be sent - String
* `embed` - If the message should be sent as an embed - Boolean

**Result**: Sends a message in the executed channel with content `message`. This message will be an embed if `embed` is true.

**Required Permissions**: `Moderate Members`

**Command category**: `Utility commands`

---

### Name: `edit-say`
**Arguments**:
(Moderators only)
* `messageToEdit` - The ID of the message contain the embed you'd like to edit - Snowflake
* `newContent` - The new content for the message - Optional String
* `newColor` - The new color for the embed - Optional Color (default: Blurple)
* `channelOfMessage` - The channel the embed was originally sent in - Optional channel (default: Channel command was executed in)
* `timestamp` - Whether to add the timestamp of when the message was originally sent or not - Optional boolean (default: true)

**Result**: Edited message/embed

**Required Permissions**: `Moderate Members`

**Command category**: `Utility commands`

---

### Name: `manual-report`
**Arguments**:
* `message-link` - Link to the message to report - String

**Result**: Reports the message pointed to by `message-link` by pinging `moderatorRole` in `messageLogs`.

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `thread archive`
**Arguments**:
* `lock` - If the thread executed in should be locked - Boolean (default: false)

**Result**: Archives the thread executed in **if executed by a moderator or the thread owner**. Locks the thread if executed by a moderator and `lock` is true.

**Required Permissions**: `none (Manage Threads for locking)`

**Command category**: `Utility commands`

---

### Name: `thread rename`
**Arguments**:
* `newName` - New name for the thread executed in - String

**Result**: Renames the thread executed in **if executed by a moderator or the thread owner**.

**Required Permissions**: `none (Manage Threads if not owner)`

**Command category**: `Utility commands`

---

### Name: `thread transfer`
**Arguments**:
* `newOwner` - The person you want to transfer ownership of the thread to - User

**Result**: Transfers ownership of the thread executed in to `newOwner` **if executed by a moderator or the thread owner**. Creates a message in the executed thread noting this transfer.

**Required Permissions**: `none (Manage Threads if not owner)`

**Command category**: `Utility commands`

---

### Name: `thread prevent-archiving`
**Arguments**:
None

**Result**: Prevents the thread the command was run in from being archived

**Required Permissions**: `none (Manage Threads if not owner)`

**Command category**: `Utility commands`

---

### Name: `nickname request`
**Arguments**:
* `nickname` - The new nickname you are requesting - String

**Result**: Sends a request to the moderators for a new nickname. This feature is designed for servers that disable nickname change permissions on users

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `nickname clear`
**Arguments**:
None

**Result**: Clears the nickname of the user that ran the command

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `reminder set`
**Arguments**:
* `time` - The time until the bot should send the reminder - Coalescing Duration
* `customMessage` - A custom message to attach to the reminder - Optional String

**Result**: Sets a reminder that will be sent in the channel the reminder was set in, once the set duration has passed

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `remind list`
**Arguments**:
None

**Result**: Sends an embed containing all the reminders you have set in that guild. If there are none, it returns a messages saying so.

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `reminder remove`
**Arguments**:
None

**Result**: Allows the user to input the number ID of the reminder they would like to delete. It is advised to use `/reminder list` to find out what reminder id is.

**Required Permissions**: `None`

**Command category**: `Utility commands`

---

### Name: `reminder remove-all`
**Arguments**:
* `type` - The type of reminder to remove all of, can be `repeating`, `non-repeating` or `all` - String Choice

**Result**: Removes all the specific `type` of reminders for the user from the guild the command was executed in.

**Required Permissions**: `None`

**Command category**: `Utility commands`

---


## GitHub commands
The GitHub commands allow users to query GitHub for things such as issues, pull requests, repositories and more. They take in a string in the format 'USER-NAME/REPO-NAME' for example or a URL.

### Name: `github issue`
**Arguments**:
* `repository` - GitHub repository the issue is in, by the format 'USER-NAME/REPO-NAME' or URL - String
* `issue-number` - Number of the issue to be searched for - Integer

**Result**: An embed containing the information about the queryed issue.

**Required Permissions**: `None`

**Command category**: `GitHub commands`

---

### Name: `github repo`
**Arguments**:
* `repository` - GitHub repository to be searched for, by the format 'USER-NAME/REPO-NAME' or URL - String

**Result**: An embed containing the information about the queryed repository

**Required Permissions**: `None`

**Command category**: `GitHub commands`

---

### Name: `github user`
**Arguments**:
* `username` - GitHub user or repository to be searched for, can be a username or URL - String

**Result**: An embed containing the information about the queryed user.

**Required Permissions**: `None`

**Command category**: `Github commands`

---


## Tag commands
Tag commands are guild specific commands, that can be added at runtime. They are all embed commands. You will be assisted by auto-complete when typing these commands.

### Name: `tag-create`
**Arguments**:
(Moderators only)
* `tagName` - The named identifier of the tag you wish to create. - String
* `tagTitle` - The tag embed title - String
* `tagValue` - The tag embed description - String

**Result**: Creates a tag for the guild you ran this command in

**Required Permissions**: `Moderate Members`

**Command category**: `Tag commands`

---

### Name: `tag-delete`
**Arguments**:
* `tagName` - The named identifier of the tag you wish to delete - String

**Result**: Deletes the tag for the guild you ran this command in

**Required Permissions**: `Moderate members`

**Command category**: `Tag commands`

---

### Name: `tag`
**Arguments**:
* `tagName` - The named identifier of the tag you wish to run - String

**Result**: Posts the tag embed you requested

**Required Permissions**: `None`

**Command category**: `Tag commands`

---

### Name: `tag-help`
**Arguments**:
None

**Result**: Displays a help command with all this information, in greater detail.

**Required Permissions**: `None`

**Command category**: `Tag commands`

---


## Gallery channel commands
Gallery channels are channels that only allow attachments or links to be sent within them, deleting messages that don't contain either of these things.

### Name: `gallery-channel set`
**Arguments**:
none

**Result**: Sets the channel you are in a Gallery channel

**Required Permissions**: `None`

**Command category**: `Gallery channel commands`

---

### Name: `gallery-channel unset`
**Arguments**:
none

**Result**: Unsets the channel you are in a Gallery channel

**Required Permissions**: `None`

**Command category**: `Gallery channel commands`

---

### Name: `gallery-channel list`
**Arguments**:
none

**Result**: Displays an embed of the image channels for the current guild

**Required Permissions**: `None`

**Command category**: `Gallery channel commands`

---

