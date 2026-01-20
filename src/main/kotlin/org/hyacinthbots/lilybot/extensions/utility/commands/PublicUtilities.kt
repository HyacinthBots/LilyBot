package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.utils.dm
import dev.kordex.core.utils.getTopRole
import dev.kordex.core.utils.hasPermission
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.requiredConfigs
import kotlin.time.Clock

/**
 * This class contains a few utility commands that can be used by the public in guilds, or that are often seen by the
 * public.
 *
 * @since 3.1.0
 */
class PublicUtilities : Extension() {
	override val name = "public-utilities"

	override suspend fun setup() {
		/**
		 * Ping Command.
		 * @author IMS212
		 * @since 2.0
		 */
		publicSlashCommand {
			name = Translations.Utility.PublicUtilities.Ping.name
			description = Translations.Utility.PublicUtilities.Ping.description

			action {
				val averagePing = this@PublicUtilities.kord.gateway.averagePing

				respond {
					embed {
						color = DISCORD_YELLOW
						title = Translations.Utility.PublicUtilities.Ping.title.translate()

						timestamp = Clock.System.now()

						field {
							name = Translations.Utility.PublicUtilities.Ping.pingValue.translate()
							value = "**$averagePing**"
							inline = true
						}
					}
				}
			}
		}

		/**
		 * Nickname request command
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand {
			name = Translations.Utility.PublicUtilities.Nickname.name
			description = Translations.Utility.PublicUtilities.Nickname.description

			ephemeralSubCommand(::NickRequestArgs) {
				name = Translations.Utility.PublicUtilities.Nickname.Request.name
				description = Translations.Utility.PublicUtilities.Nickname.Request.description

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.UTILITY_LOG)
				}

				action {
					val config = UtilityConfigCollection().getConfig(guildFor(event)!!.id)!!
					val utilityLog = guild?.getChannelOfOrNull<GuildMessageChannel>(config.utilityLogChannel!!)

					val requester = user.asUserOrNull()
					val requesterAsMember = requester?.asMemberOrNull(guild!!.id)
					val self = this@PublicUtilities.kord.getSelf().asMemberOrNull(guild!!.id)

					val translations = Translations.Utility.PublicUtilities.Nickname.Request
					val embedTranslations = Translations.Utility.PublicUtilities.Nickname.Request.LogEmbed

					if (requesterAsMember?.getTopRole()?.getPosition() != null &&
						self?.getTopRole()?.getPosition() == null
					) {
						respond {
							content = translations.lilyNoRolePublic.translate()
						}
						return@action
					} else if ((requesterAsMember?.getTopRole()?.getPosition() ?: 0) >
						(self?.getTopRole()?.getPosition() ?: 0)
					) {
						respond {
							content = translations.highestRolePublic.translate()
						}
						return@action
					}

					if (requesterAsMember?.hasPermission(Permission.ChangeNickname) == true) {
						requesterAsMember.edit { nickname = arguments.newNick }
						respond {
							content = translations.hasPermission.translate()
						}
						return@action
					}

					// Declare the embed outside the action to allow us to reference it inside the action
					var actionLogEmbed: Message? = null

					respond { content = translations.sent.translate() }

					try {
						actionLogEmbed =
							utilityLog?.createMessage {
								embed {
									color = DISCORD_YELLOW
									title = translations.embedTitle.translate()
									timestamp = Clock.System.now()

									field {
										name = Translations.Utility.PublicUtilities.Nickname.userField.translate()
										value =
											"${requester?.mention}\n${requester?.asUserOrNull()?.username}\n${requester?.id}"
										inline = false
									}

									field {
										name = translations.embedCurrentNick.translate()
										value = "`${requesterAsMember?.nickname}`"
										inline = false
									}

									field {
										name = translations.embedRequestedNick.translate()
										value = "`${arguments.newNick}`"
										inline = false
									}
								}
								components {
									ephemeralButton(row = 0) {
										label = Translations.Utility.PublicUtilities.Nickname.Request.Button.accept
										style = ButtonStyle.Success

										action button@{
											if (requesterAsMember?.getTopRole()?.getPosition() != null &&
												self?.getTopRole()?.getPosition() == null
											) {
												respond {
													content = translations.lilyNoRolePrivate.translate()
												}
												return@button
											} else if ((requesterAsMember?.getTopRole()?.getPosition() ?: 0) >
												(self?.getTopRole()?.getPosition() ?: 0)
											) {
												respond {
													content = translations.highestRolePrivate.translate()
												}
												return@button
											}

											requesterAsMember?.edit { nickname = arguments.newNick }

											requester?.dm {
												embed {
													title = translations.dmAcceptTitle.translate(
														guild!!.asGuildOrNull()?.name
													)
													description = translations.dmAcceptDescription.translate(
														requesterAsMember?.nickname,
														arguments.newNick
													)
													color = DISCORD_GREEN
												}
											}

											actionLogEmbed!!.edit {
												components { removeAll() }

												embed {
													color = DISCORD_GREEN
													title = embedTranslations.acceptTitle.translate()

													field {
														name =
															Translations.Utility.PublicUtilities.Nickname.userField.translate()
														value =
															"${requester?.mention}\n${requester?.asUserOrNull()?.username}\n" +
																"${requester?.id}"
														inline = false
													}

													// these two fields should be the same and exist as a sanity check
													field {
														name = embedTranslations.previousNick.translate()
														value = "`${requesterAsMember?.nickname}`"
														inline = false
													}

													field {
														name = embedTranslations.acceptedNick.translate()
														value = "`${arguments.newNick}`"
														inline = false
													}

													footer {
														text = embedTranslations.acceptedBy.translate(
															user.asUserOrNull()?.username
														)
														icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
													}

													timestamp = Clock.System.now()
												}
											}
										}
									}

									ephemeralButton(row = 0) {
										label = Translations.Utility.PublicUtilities.Nickname.Request.Button.deny
										style = ButtonStyle.Danger

										action {
											requester?.dm {
												embed {
													title = translations.dmDenyTitle.translate()
													description = translations.dmDenyDescription.translate(
														arguments.newNick
													)
												}
											}

											actionLogEmbed!!.edit {
												components { removeAll() }
												embed {
													title = embedTranslations.denyTitle.translate()

													field {
														name =
															Translations.Utility.PublicUtilities.Nickname.userField.translate()
														value = "${requester?.mention}\n" +
															"${requester?.asUserOrNull()?.username}\n${requester?.id}"
														inline = false
													}

													field {
														name = embedTranslations.currentNick.translate()
														value = "`${requesterAsMember?.nickname}`"
														inline = false
													}

													field {
														name = embedTranslations.rejectedNick.translate()
														value = "`${arguments.newNick}`"
														inline = false
													}

													footer {
														text = embedTranslations.deniedBy.translate(
															user.asUserOrNull()?.username
														)
														icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
													}

													timestamp = Clock.System.now()
													color = DISCORD_RED
												}
											}
										}
									}
								}
							}
					} catch (_: KtorRequestException) {
						// Avoid hard failing on permission error, since the public won't know what it means
						respond {
							content = Translations.Utility.PublicUtilities.Nickname.failToSend.translate()
						}
						return@action
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Utility.PublicUtilities.Nickname.Clear.name
				description = Translations.Utility.PublicUtilities.Nickname.Clear.description

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.UTILITY_LOG)
				}

				action {
					val config = UtilityConfigCollection().getConfig(guild!!.id)!!
					val utilityLog = guild?.getChannelOfOrNull<GuildMessageChannel>(config.utilityLogChannel!!)
					val translations = Translations.Utility.PublicUtilities.Nickname.Clear

					// Check the user has a nickname to clear, avoiding errors and useless action-log notifications
					if (user.fetchMember(guild!!.id).nickname == null) {
						respond {
							content = translations.nothingToClear.translate()
						}
						return@action
					}

					respond { content = translations.cleared.translate() }

					try {
						utilityLog?.createEmbed {
							val embedTranslations = Translations.Utility.PublicUtilities.Nickname.Clear.LogEmbed
							title = embedTranslations.title.translate()
							color = DISCORD_YELLOW
							timestamp = Clock.System.now()

							field {
								name = Translations.Utility.PublicUtilities.Nickname.userField.translate()
								value = "${user.mention}\n${user.asUserOrNull()?.username}\n${user.id}"
								inline = false
							}

							field {
								name = embedTranslations.newNickTitle.translate()
								value = embedTranslations.newNickValue.translate(
									user.asMemberOrNull(guild!!.id)?.nickname
								)
								inline = false
							}
						}
					} catch (_: KtorRequestException) {
						// Avoid hard failing on permission error, since the public won't know what it means
						respond {
							content = Translations.Utility.PublicUtilities.Nickname.failToSend.translate()
						}
						return@action
					}
					user.asMemberOrNull(guild!!.id)?.edit { nickname = null }
				}
			}
		}
	}

	inner class NickRequestArgs : Arguments() {
		/** The new nickname that the command user requested. */
		val newNick by string {
			name = Translations.Utility.PublicUtilities.Nickname.Request.Args.NewNick.name
			description = Translations.Utility.PublicUtilities.Nickname.Request.Args.NewNick.description

			minLength = 1
			maxLength = 32
		}
	}
}
