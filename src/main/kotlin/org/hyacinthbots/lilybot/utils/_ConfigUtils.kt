package org.hyacinthbots.lilybot.utils

import dev.kord.common.entity.Snowflake
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.types.CheckContext
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
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

    val translations = Translations.Checks.RequiredConfigs

    // Prevent commands being run in DMs, although [anyGuild] should still be used as backup
    guildFor(event) ?: fail(translations.inServer)

    if (configOptions.isEmpty()) {
        fail(translations.noConfigInCode)
    }

    val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
    if (moderationConfig == null) {
        fail(translations.cantModConfig)
        return
    }

    val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
    if (loggingConfig == null) {
        fail(translations.cantLoggingConfig)
        return
    }

    val utilityConfig = UtilityConfigCollection().getConfig(guildFor(event)!!.id)
    if (utilityConfig == null) {
        fail(translations.cantUtilityConfig)
        return
    }

    // Look at the config options and check the presence of the config in the database.
    for (option in configOptions) {
        when (option) {
            ConfigOptions.MODERATION_ENABLED -> {
                if (!moderationConfig.enabled) {
                    fail(translations.modDisabled)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MODERATOR_ROLE -> {
                if (moderationConfig.role == null) {
                    fail(translations.noModRole)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.ACTION_LOG -> {
                if (moderationConfig.channel == null) {
                    fail(translations.noModLog)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.LOG_PUBLICLY -> {
                if (moderationConfig.publicLogging == null) {
                    fail(translations.logPubliclyOff)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED -> {
                if (!loggingConfig.enableMessageDeleteLogs) {
                    fail(translations.noDeleteLogging)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED -> {
                if (!loggingConfig.enableMessageEditLogs) {
                    fail(translations.noEditLogging)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MESSAGE_LOG -> {
                if (loggingConfig.messageChannel == null) {
                    fail(translations.noMessageLogging)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MEMBER_LOGGING_ENABLED -> {
                if (!loggingConfig.enableMemberLogs) {
                    fail(translations.noMemberLogging)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.MEMBER_LOG -> {
                if (loggingConfig.memberLog == null) {
                    fail(translations.noMemberLogSet)
                    break
                } else {
                    pass()
                }
            }

            ConfigOptions.UTILITY_LOG -> {
                if (utilityConfig.utilityLogChannel == null) {
                    fail(translations.noUtilityLog)
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
suspend inline fun configIsUsable(guildId: Snowflake, option: ConfigOptions): Boolean {
    when (option) {
        ConfigOptions.MODERATION_ENABLED -> return ModerationConfigCollection().getConfig(guildId)?.enabled == true

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
            return LoggingConfigCollection().getConfig(guildId)?.enableMessageDeleteLogs == true

        ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED ->
            return LoggingConfigCollection().getConfig(guildId)?.enableMessageEditLogs == true

        ConfigOptions.MESSAGE_LOG -> {
            val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
            return loggingConfig.messageChannel != null
        }

        ConfigOptions.MEMBER_LOGGING_ENABLED ->
            return LoggingConfigCollection().getConfig(guildId)?.enableMemberLogs == true

        ConfigOptions.MEMBER_LOG -> {
            val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
            return loggingConfig.memberLog != null
        }

        ConfigOptions.UTILITY_LOG -> {
            val utilityConfig = UtilityConfigCollection().getConfig(guildId) ?: return false
            return utilityConfig.utilityLogChannel != null
        }
    }
}

/**
 * Allows you to check if multiple configs are usable in a given guild. It will return a map containing the config and
 * its "usability".
 *
 * @param guildId The ID of the guild to check hte configs for
 * @param configs The [ConfigOptions] you wish to check
 * @return A [Map] of [ConfigOptions] and [Boolean] based on whether the config is usable
 * @since 4.8.1
 */
suspend inline fun configsAreUsable(guildId: Snowflake, vararg configs: ConfigOptions): Map<ConfigOptions, Boolean> {
    val results = mutableMapOf<ConfigOptions, Boolean>()
    configs.forEach {
        val result = configIsUsable(guildId, it)
        results[it] = result
    }

    return results
}
