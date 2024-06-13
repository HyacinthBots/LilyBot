package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasPermission
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.entity.Message
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.ConfigMetaCollection
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.GuildLeaveTimeCollection
import org.hyacinthbots.lilybot.database.collections.LockedChannelCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.MainMetaCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.NewsChannelPublishingCollection
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.ReminderRestrictionCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.RoleSubscriptionCollection
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UptimeCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.koin.dsl.bind

@PublishedApi
internal val utilsLogger = KotlinLogging.logger("Checks Logger")

/**
 * Check if a role is mentionable by Lily.
 *
 * @param role The role to check
 * @return A Boolean of whether it is pingable or not
 *
 * @author NoComment1105
 * @since 4.1.0
 */
suspend inline fun canPingRole(role: RoleBehavior?, guildId: Snowflake, kord: Kord) =
	if (kord.getSelf().asMemberOrNull(guildId)?.hasPermission(Permission.MentionEveryone) == true) {
		true
	} else {
		role != null && role.guild.getRoleOrNull(role.id)?.mentionable == true
	}

/**
 * Get the number of guilds the bot is in.
 *
 * @return The number of guilds the bot is in.
 * @author NoComment1105
 * @since 3.4.5
 */
suspend inline fun Extension.getGuildCount() = kord.with(EntitySupplyStrategy.cacheWithRestFallback).guilds.count()

/**
 * Get the member count of the given Guild.
 *
 * @return The number of members in this guild.
 * @author NoComment1105
 * @since 4.4.3
 */
suspend inline fun <T : GuildBehavior> T.getMemberCount() =
	kord.getGuildOrNull(this.id)!!.withStrategy(EntitySupplyStrategy.rest).members.count()

/**
 * Checks a string to see if it fits the in a discord embed field.
 *
 * @return True, if the given string fits and embed, false if not
 * @author NoComment1105
 * @since 4.2.0
 */
fun String?.fitsEmbedField(): Boolean? {
	this ?: return null
	return this.length <= 1024
}

/**
 * Utility to get a string or a default value.
 * Basically String.ifEmpty but works with nullable strings
 *
 * @return This, or defaultValue if this is null or empty
 * @author trainb0y
 * @since 4.1.0
 * @see String.ifEmpty
 */
fun String?.ifNullOrEmpty(defaultValue: () -> String): String =
	if (this.isNullOrEmpty()) {
		defaultValue()
	} else {
		this
	}

/**
 * Get this message's contents, trimmed to 1024 characters.
 * If the message exceeds that length, it will be truncated and an ellipsis appended.
 * @author trainb0y
 * @since 4.1.0
 */
fun Message?.trimmedContents(): String? {
	this ?: return null
	return if (this.content.length > 1024) {
		this.content.substring(0, 1021) + "..."
	} else {
		this.content
	}
}

/**
 * @see trimmedContents
 * @author trainb0y
 * @since 4.2.0
 */
fun String?.trimmedContents(): String? {
	this ?: return null
	return if (this.length > 1024) {
		this.substring(0, 1021) + "..."
	} else {
		this
	}
}

/**
 * Converts a [DateTimePeriod] into a [String] interval at which it repeats at.
 *
 * @return The string interval the DateTimePeriod repeats at
 * @author NoComment1105
 * @since 4.2.0
 */
fun DateTimePeriod?.interval(): String? {
	this ?: return null
	return this.toString().lowercase().replace("pt", "").replace("p", "")
}

/**
 * Get this message's contents, trimmed to the [desiredLength] of characters.
 * If the message exceeds that length, it will be truncated and an ellipsis appended.
 * If the message is smaller than the [desiredLength], the content length is used and an ellipsis appended
 *
 * @param desiredLength The desired length to limit the string too
 * @author NoComment1105
 * @since 4.2.0
 */
fun Message?.trimmedContents(desiredLength: Int): String? {
	this ?: return null
	val useRegularLength = this.content.length < desiredLength.coerceIn(1, 1020)
	return if (this.content.length > desiredLength.coerceIn(1, 1020)) {
		this.content.substring(0, if (useRegularLength) this.content.length else desiredLength.minus(3)) + "..."
	} else {
		this.content
	}
}

/**
 * @see trimmedContents
 * @author NoComment1105
 * @since 4.2.0
 */
fun String?.trimmedContents(desiredLength: Int): String? {
	this ?: return null
	val useRegularLength = this.length < desiredLength.coerceIn(1, 1020)
	return if (this.length > desiredLength.coerceIn(1, 1020)) {
		this.substring(0, if (useRegularLength) this.length else desiredLength.minus(3)) + "..."
	} else {
		this
	}
}

/**
 * Update the presence to reflect the new number of guilds, if the presence is set to "default".
 * @author NoComment1105
 * @since 3.4.5
 */
suspend inline fun Extension.updateDefaultPresence() {
	if (StatusCollection().getStatus() != null) {
		return
	}

	kord.editPresence {
		watching("${getGuildCount()} servers")
	}
}

/**
 * A quick function to generate the contents of the bulk delete file.
 *
 * @param messages The set of message being deleted
 * @return A string of the file content
 *
 * @author NoComment1105
 * @since 4.7.0
 */
fun generateBulkDeleteFile(messages: Set<Message>): String? =
	if (messages.isNotEmpty()) {
		"# Messages\n\n**Total:** ${messages.size}\n\n" +
			messages.reversed().joinToString("\n") { // Reversed for chronology
				"*  [${
					it.timestamp.toLocalDateTime(TimeZone.UTC).toString().replace("T", " @ ")
				} UTC]  **${it.author?.username}**  (${it.author?.id})  »  ${it.content}"
			}
	} else {
		null
	}

/**
 * This function loads the database and checks if it is up-to-date. If it isn't, it will update the database via
 * migrations.
 *
 * @since 4.0.0
 */
suspend inline fun ExtensibleBotBuilder.database(migrate: Boolean) {
	val db = Database()

	hooks {
		beforeKoinSetup {
			loadModule {
				single { db } bind Database::class
			}

			loadModule {
				single { AutoThreadingCollection() } bind AutoThreadingCollection::class
				single { ConfigMetaCollection() } bind ConfigMetaCollection::class
				single { GalleryChannelCollection() } bind GalleryChannelCollection::class
				single { GithubCollection() } bind GithubCollection::class
				single { GuildLeaveTimeCollection() } bind GuildLeaveTimeCollection::class
				single { LockedChannelCollection() } bind LockedChannelCollection::class
				single { LoggingConfigCollection() } bind LoggingConfigCollection::class
				single { MainMetaCollection() } bind MainMetaCollection::class
				single { ModerationConfigCollection() } bind ModerationConfigCollection::class
				single { NewsChannelPublishingCollection() } bind NewsChannelPublishingCollection::class
				single { ReminderCollection() } bind ReminderCollection::class
				single { ReminderRestrictionCollection() } bind ReminderRestrictionCollection::class
				single { RoleMenuCollection() } bind RoleMenuCollection::class
				single { RoleSubscriptionCollection() } bind RoleSubscriptionCollection::class
				single { StatusCollection() } bind StatusCollection::class
				single { TagsCollection() } bind TagsCollection::class
				single { ThreadsCollection() } bind ThreadsCollection::class
				single { UptimeCollection() } bind UptimeCollection::class
				single { UtilityConfigCollection() } bind UtilityConfigCollection::class
				single { WarnCollection() } bind WarnCollection::class
				single { WelcomeChannelCollection() } bind WelcomeChannelCollection::class
			}

			if (migrate) {
				runBlocking {
					db.migrate()
				}
			}
		}
	}
}
