# LilyBot Privacy Policy

For an overview of LilyBot's features and capabilities, please visit our [README.md](https://github.com/HyacinthBots/LilyBot/blob/main/README.md).

The developers of LilyBot are not lawyers and do not have the resources to hire lawyers. We have, to the best of our ability, written this document clearly, concisely, and accurately. If you have any questions, please see the `Contact & Data Access or Removal` sections.

That said, we have created LilyBot in a way to utilize the least possible amount of data storage required. All data we collect is directly linked to various features, and data collected by or provided to LilyBot will never be sold or provided to a third party. The only people with access to your data are our hosting provider and, under certain circumstances, the developers. This is both for ethical and infrastructure reasons.

## Automatically Collected Data

The following data is automatically collected by LilyBot.
- **Thread Data** is stored when LilyBot creates a thread for a support channel. The owner of the thread is stored, allowing the owner to use thread commands.
- **Guild Leave Data** is stored when LilyBot leaves a guild. It contains LilyBot's time of departure and allows us to delete old data associated with a guild.
- **Left Member Data** is stored when a member leaves a guild. It contains their userID and the guildID. It is used to prevent other events from triggering, relating to leaving. The data is deleted immediately after use.
- **Moderation Action Data** is stored when a moderation action is carried out (i.e. Ban, Timeout). The ID of the user moderated, the guild the event occurred in are stored. Data is deleted after the event completes.

## Manually Provided Data

The following data is not automatically collected by LilyBot and will only be stored when manually provided, for example through a slash command.
- **Configuration Data** includes things like the channel set for moderation logs or the moderator role.
- **GitHub Data** includes a default repository to use with the GitHub commands. It is attached to the guild ID of the guild it is set in.
- **News Channel Publishing Data** is for automatic message publishing. It contains the guild the channel is in and the channel to publish from.
- **Reminders Data** includes data about reminders that have been set by users.
- **Role Menu Data** contains things like the IDs of roles attached to the menus.
- **Subscribable Roles** contains a list of Role IDs the users can assign themselves with commands.
- **Tag Data** holds the tags that have been created by guild owners for their guild.
- **Temporary Ban Data** holds information on temporary bans. This includes the ID of the banned user, the ID of the guild they're banned from and the ID of the person that banned them. This is deleted when the temporary ban expires
- **Thread Data** contains support threads, or threads transferred to a new user by the original owner They are stored to allow the use of the thread archive and rename commands.
- **Warning Data** is a list of users and the number of warning strikes they have accumulated in each guild.

## Data Retention

Data is checked for requirement daily with the initial check occurring when the bot starts.

- **Thread Data**. Data associated with threads that have been inactive for more than 7 days is deleted from LilyBot's database.

- **Guild Data**. Guild Leave Data is used to delete all data from a guild that LilyBot left over 30 days prior.

## Contact & Data Access or Removal
For questions or requests for access or removal of personal data, you may email us at hyacinthbots@outlook.com. You may also contact NoComment1105 on Discord.

Please note that, under normal circumstances, the lead developers of the bot do not have access to data that LilyBot stores. If requests are made, we will need to contact our hosting provider.
