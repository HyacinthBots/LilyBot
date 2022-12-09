package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions

/**
 * This is a check to verify that no element of the guild config is null, since these are all non-nullable values, if
 * any one of them is null, we fail with the unable to access config error message.
 *
 * @param configOptions The config options to check the database for.
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun CheckContext<*>.requiredConfigs(vararg configOptions: ConfigOptions) {
	if (!passed) {
		return
	}

	// Prevent commands being run in DMs, although [anyGuild] should still be used as backup
	guildFor(event) ?: fail("Must be in a server")

	if (configOptions.isEmpty()) {
		fail("There are no config options provided in the code. Please inform the developers immediately!")
	}

	// Look at the config options and check the presence of the config in the database.
	for (option in configOptions) {
		when (option) {
			ConfigOptions.SUPPORT_ENABLED -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (!supportConfig.enabled) {
					fail("Support is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.SUPPORT_CHANNEL -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (supportConfig.channel == null) {
					fail("A support channel has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.SUPPORT_ROLE -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (supportConfig.role == null) {
					fail("A support role has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MODERATION_ENABLED -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (!moderationConfig.enabled) {
					fail("Moderation is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MODERATOR_ROLE -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.role == null) {
					fail("A moderator role has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.ACTION_LOG -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.channel == null) {
					fail("An action log has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.LOG_PUBLICLY -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.publicLogging == null) {
					fail("Public logging has not been enabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMessageDeleteLogs) {
					fail("Message delete logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMessageEditLogs) {
					fail("Message edit logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_LOG -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (loggingConfig.messageChannel == null) {
					fail("A message log has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MEMBER_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMemberLogs) {
					fail("Member logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MEMBER_LOG -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (loggingConfig.memberLog == null) {
					fail("A member log has not been set for this guild")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.LOG_UPLOADS_ENABLED -> {
				val utilityConfig = UtilityConfigCollection().getConfig(guildFor(event)!!.id)
				if (utilityConfig == null) {
					fail("Unable to access utility config for this guild! Please inform a member of staff.")
					break
				} else if (utilityConfig.disableLogUploading) {
					fail("Log uploads are disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.UTILITY_LOG -> {
				val utilityConfig = UtilityConfigCollection().getConfig(guildFor(event)!!.id)
				if (utilityConfig == null) {
					fail("Unable to access utility config for this guild! Please inform a member of staff.")
					break
				} else if (utilityConfig.utilityLogChannel == null) {
					fail("A utility log has not been set for this guild")
					break
				} else {
					pass()
				}
			}
		}
	}
}

/**
 * This function checks if a single config exists and is valid. Returns true if it is or false otherwise.
 *
 * @param option The config option to check the database for. Only takes a single option.
 * @return True if the selected [option] is valid and enabled and false if it isn't
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun configIsUsable(option: ConfigOptions, guildId: Snowflake): Boolean {
	when (option) {
		ConfigOptions.SUPPORT_ENABLED -> return SupportConfigCollection().getConfig(guildId)?.enabled ?: false

		ConfigOptions.SUPPORT_CHANNEL -> {
			val supportConfig = SupportConfigCollection().getConfig(guildId) ?: return false
			return supportConfig.channel != null
		}

		ConfigOptions.SUPPORT_ROLE -> {
			val supportConfig = SupportConfigCollection().getConfig(guildId) ?: return false
			return supportConfig.role != null
		}

		ConfigOptions.MODERATION_ENABLED -> return ModerationConfigCollection().getConfig(guildId)?.enabled ?: false

		ConfigOptions.MODERATOR_ROLE -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.role != null
		}

		ConfigOptions.ACTION_LOG -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.channel != null
		}

		ConfigOptions.LOG_PUBLICLY -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.publicLogging != null
		}

		ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED ->
			return LoggingConfigCollection().getConfig(guildId)?.enableMessageDeleteLogs ?: false

		ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED ->
			return LoggingConfigCollection().getConfig(guildId)?.enableMessageEditLogs ?: false

		ConfigOptions.MESSAGE_LOG -> {
			val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
			return loggingConfig.messageChannel != null
		}

		ConfigOptions.MEMBER_LOGGING_ENABLED -> return LoggingConfigCollection().getConfig(guildId)?.enableMemberLogs
			?: false

		ConfigOptions.MEMBER_LOG -> {
			val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
			return loggingConfig.memberLog != null
		}

		ConfigOptions.LOG_UPLOADS_ENABLED -> {
			val utilityConfig = UtilityConfigCollection().getConfig(guildId) ?: return false
			return utilityConfig.disableLogUploading
		}

		ConfigOptions.UTILITY_LOG -> {
			val utilityConfig = UtilityConfigCollection().getConfig(guildId) ?: return false
			return utilityConfig.utilityLogChannel != null
		}
	}
}
