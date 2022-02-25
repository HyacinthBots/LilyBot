# Commands List

The following is a list of commands, their arguments, and what they do.

## Administration Commands
These are commands for the maintenance of LilyBot. They can only be run by server Admins.

### Set Config
Name: `config set`

Arguments:
* `guildId` - ID of the guild to set a config for - Guild ID
* `moderatorRole` - Moderator role that will be pinged for reports - Role
* `modActionLog` - Channel for embeds created by mod actions to be sent - Channel
* `messageLogs` - Channel for deleted messages to be logged - Channel
* `joinChannel` - Channel for members joining or leaving to be logged - Channel
* `supportChannel` - Channel for the support auto-threading system to be used in - Optional Channel
* `supportTeamRole` - The role to be added to any threads created in the `supportchannel` - Optional Role

Result: Sets a configuration in the database for `guildId`.

### Clear Config
Name: `config clear`

Arguments:
* `guildId` - ID of the guild to clear the config for - Guild ID

Result: Clears the configuration for `guildId`.

### Presence
Name: `set-status`

Arguments:
* `presence` - Value for Lily's status - String

Result: Lily's "Now Playing:" status is set to `presence`.

## Moderation Commands
These commands are for use by moderators.
They utilize built-in permission checks.
All moderation commands are logged to the action log.

### Clear
Name: `clear`

Arguments:
* `messages` – Number of messages to delete - Integer

Result: Deletes the `messages` latest messages from the channel executed in.

### Kick
Name: `kick`

Arguments:
* `kickUser` – Person to kick - User
* `reason` - Reason for the Kick - Optional String

Result: Kicks `kickUser` from the server with reason `reason`.

### Ban
Name: `ban`

Arguments:
* `banUser` – Person to ban - User
* `messages` - Number of days of messages to delete - Integer (default 1)
* `reason` - Reason for the ban - Optional String

Result: Bans `banUser` from the server with reason `reason` and deletes any messages they sent in the last `messages` day(s).

### Unban
Name: `unban`

Arguments:
* `unbanUserId ` - The Discord ID of the person to unban - User ID

Result: The user with the ID `unbanUserId` is unbanned.

### Soft Ban
Name: `soft-ban`

Arguments:
* `softBanUser` - Person to soft ban - User
* `messages` - Number of days of messages to delete - Integer (default 3)
* `reason` - Reason for the ban - Optional String

Result: Bans `softBanUser`, deletes the last `messages` days of messages from them, and unbans them.

### Warn
Name: `warn`

Arguments:
* `warnUser` - Person to warn - User
* `points` - Amount of points to add - Integer (default 10)
* `reason` - Reason for warn - Optional String

Result: Warns `warnUser` with a DM and adds `points` points to their points total.
Depending on their new points total, action is taken based on the below table.

| Points | Action          |
|--------|-----------------|
| 75     | 3 hour timeout  |
| 125    | 12 hour timeout |
| 200    | Ban             |


### Timeout
Name: `timeout`

Arguments:
* `timeoutUser` - Person to timeout - User
* `duration` - Duration of timeout - Duration [e.g. 6h or 30s] (default 6h)
* `reason` - Reason for timeout - Optional String

Result: Times `timeoutUser` out for `duration`. A timeout is Discord's built-in mute function.





## Utility Commands
These commands are just handy to have around. Moderator only commands are at the top and clearly marked.

### Create Role Menu (MODS ONLY)
Name: `role-mennu`

Arguments:
* `role` - Role users will be able to select through the menu - Role
* `title` - Title of the embed to be created along with the menu - Optional String (default: Role Selection Menu)
* `description` - Text of the embed to be created along with the menu - Optional String (default: Use the button below to add/remove the `role` role.)
* `channel` - Channel for the embed and menu to be created in - Optional Channel (default: channel executed in)
* `color` - Color for the embed - Optional Color (default: black)

Result: Creates a menu with buttons to add and remove `role` in `channel` along with an `color` colored embed with description `description` and title `title`.

### Say
Name: `say`

Arguments:
* `message` - Message to be sent - String
* `embed` - If the message should be sent as an embed - Boolean

Result: Sends a message in the executed channel with content `message`. This message will be an embed if `embed` is true.

### Manual Report
Name: `manual-report`

Arguments:
* `message-link` - Link to the message to report - String

Result: Reports the message pointed to by `message-link` by pinging `moderatorRole` in `messageLogs`.

### GitHub Issue
Name: `github issue`

Arguments:
* `repository` - GitHub repository the issue is in, by the format "USER-NAME/REPO-NAME" - String
* `issue-number` - Number of the issue to be searched for - Integer

### GitHub Repo
Name: `github repo`

Arguments:
* `repository` - GitHub repository to be searched for, by the format "USER-NAME/REPO-NAME" - String

### GitHub User
Name: `github user`

Arguments:
* `username` - GitHub user or repository to be searched for - String

### Archive Thread
Name: `thread archive`

Arguments:
* `lock` - If the thread executed in should be locked - Boolean (default: false)

Result: Archives the thread executed in if executed by a moderator or the thread owner. Locks the thread if executed by a moderator and `lock` is true.

### Rename Thread
Name: `thread rename`

Arguments:
* `newName` - New name for the thread executed in - String

Result: Renames the thread executed in if executed by a moderator or the thread owner.
