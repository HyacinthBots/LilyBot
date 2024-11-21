package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.commands.converters.impl.int
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicSlashCommand
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.github
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHException
import org.kohsuke.github.GHFileNotFoundException
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHLabel
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHUser
import org.kohsuke.github.PagedIterator
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * This class contains the GitHub commands that allow users to look up Issues, Repositories and Users on GitHub through
 * commands in Discord.
 *
 * @since 2.0
 */
class Github : Extension() {
	override val name = "github"

	override suspend fun setup() {
		/**
		 * GitHub Commands
		 * @author NoComment1105, chalkyjeans (old java version author)
		 * @since 2.0
		 */
		publicSlashCommand {
			name = Translations.Utility.Github.name
			description = Translations.Utility.Github.description

			publicSubCommand(::IssueArgs) {
				name = Translations.Utility.Github.Issue.name
				description = Translations.Utility.Github.Issue.description

				action {
					val translations = Translations.Utility.Github.Issue
					val repository = arguments.repository ?: GithubCollection().getDefaultRepo(guild!!.id)
					if (repository == null) {
						respond {
							content = Translations.Utility.Github.noDefault.translate()
						}
						return@action
					}
					// Clarify the input is formatted correctly, inform the user if not.
					if (!repository.contains("/")) {
						respond {
							embed {
								title = Translations.Utility.Github.badFormatEmbedTitle.translate()
								description = Translations.Utility.Github.badFormatEmbedDesc.translate()
							}
						}
						return@action
					}

					var issue: GHIssue?

					try {
						issue = if (repository.contains("http", true)) {
							github.getRepository(
								"${repository.split("/")[3]}/" +
									repository.split("/")[4]
							)?.getIssue(arguments.issue)
						} else {
							try {
								github.getRepository(repository)?.getIssue(arguments.issue)
							} catch (_: GHFileNotFoundException) {
								respond {
									embed {
										title = translations.unableToFind.translate()
									}
								}
								return@action
							}
						}
					} catch (_: IOException) {
						val iterator: PagedIterator<GHIssue>? = github.searchIssues()
							?.q("${arguments.issue} repo:$repository")
							?.order(GHDirection.DESC)
							?.list()
							?._iterator(1)

						// Run a quick check on the iterator, in case the repository owner org/user doesn't exist
						try {
							iterator!!.hasNext()
						} catch (_: GHException) {
							respond {
								embed {
									title = Translations.Utility.Github.unableToRepo.translate()
								}
							}
							return@action
						}

						if (iterator.hasNext()) {
							issue = iterator.next()
						} else {
							respond {
								embed {
									title = translations.unableToFind.translate()
								}
							}
							return@action
						}

						val num = issue!!.number
						try {
							issue = github.getRepository(repository)?.getIssue(num)
						} catch (_: GHFileNotFoundException) {
							respond {
								embed {
									title = translations.unableToFind.translate()
								}
							}
							return@action
						}
					}

					respond {
						embed {
							val open = issue?.state == GHIssueState.OPEN
							var merged = false
							var draft = false

							if (issue!!.isPullRequest) {
								title = issue.title
								url = issue.htmlUrl.toString()
								description = translations.embedDescPr.translateNamed(
									"number" to issue.number, "repo" to issue.repository.fullName
								)

								try {
									val pull: GHPullRequest = issue.repository.getPullRequest(issue.number)
									merged = pull.isMerged
									draft = pull.isDraft
								} catch (ioException: IOException) {
									ioException.printStackTrace()
									title = translations.errorTitle.translate()
									description = translations.errorDesc.translate()
									color = DISCORD_RED
									return@respond
								}
							} else {
								title = issue.title
								url = issue.htmlUrl.toString()
								description = translations.embedDescIs.translateNamed(
									"number" to issue.number, "repo" to issue.repository.fullName
								)
							}

							field {
								// Grab the first 400 characters of the body, if it's not null
								if (issue.body != null) {
									value = if (issue.body.length > 400) {
										issue.body.substring(0, 399) + "..."
									} else if (issue.body.isNotEmpty() && issue.body.length <= 399) {
										issue.body
									} else {
										translations.noDesc.translate()
									}
								}
							}

							// Use colours similar to that of GitHub's issue status colours
							if (merged) {
								color = dev.kord.common.Color(111, 66, 193)
								field {
									name = translations.statusField.translate()
									value = translations.merged.translate()
									inline = false
								}
							} else if (!open) {
								color = dev.kord.common.Color(203, 36, 49)
								field {
									name = translations.statusField.translate()
									value = translations.closed.translate()
									inline = false
								}
							} else if (draft) {
								color = dev.kord.common.Color(255, 255, 255)
								field {
									name = translations.statusField.translate()
									value = translations.draft.translate()
									inline = false
								}
							} else {
								color = dev.kord.common.Color(44, 185, 78)
								field {
									name = translations.statusField.translate()
									value = translations.open.translate()
									inline = false
								}
							}

							try {
								val author: GHUser = issue.user
								if (author.name != null) {
									field {
										name = translations.authorField.translate()
										value =
											"[" + author.login + " (" + author.name + ")](" +
												"https://github.com/" + author.login + ")"
										inline = false
									}
								} else {
									field {
										name = translations.authorField.translate()
										value = "[" + author.login + "](https://github.com/" + author.login + ")"
										inline = false
									}
								}
							} catch (_: IOException) {
								field {
									name = translations.authorField.translate()
									value = translations.unknownAuthor.translate()
									inline = false
								}
							}

							try {
								field {
									name = translations.openedField.translate()
									value = issue.createdAt.toString()
									inline = false
								}

								val labels = mutableListOf<CharSequence>()

								for (label: GHLabel in issue.labels) {
									labels.add(label.name)
								}

								if (labels.isNotEmpty()) {
									field {
										name = translations.labelsField.translate()
										value = labels.joinToString(", ")
										inline = false
									}
								}
							} catch (ioException: IOException) {
								ioException.printStackTrace()
							}
						}
					}
				}
			}

			publicSubCommand(::RepoArgs) {
				name = Translations.Utility.Github.Repo.name
				description = Translations.Utility.Github.Repo.description

				action {
					val translations = Translations.Utility.Github.Repo
					val repository = arguments.repository ?: GithubCollection().getDefaultRepo(guild!!.id)
					if (repository == null) {
						respond {
							content = Translations.Utility.Github.unableToRepo.translate()
						}
						return@action
					}

					if (!repository.contains("/")) {
						respond {
							embed {
								title = Translations.Utility.Github.badFormatEmbedTitle.translate()
								description = Translations.Utility.Github.badFormatEmbedDesc.translate()
							}
						}
						return@action
					}

					var repo: GHRepository?

					try {
						repo = if (repository.contains("http", true)) {
							github.getRepository(
								"${repository.split("/")[3]}/" +
									repository.split("/")[4]
							)
						} else {
							github.getRepository(repository)
						}
					} catch (_: IOException) {
						respond {
							embed {
								title = Translations.Utility.Github.unableToRepo.translate()
							}
						}
						repo = null
						return@action
					}

					respond {
						embed {
							try {
								title = translations.embedTitle.translate(repo?.fullName)
								url = repo?.htmlUrl.toString()
								description = repo?.description

								if (repo!!.license != null) {
									field {
										name = translations.licenceField.translate()
										value = repo.license.name
										inline = false
									}
								}

								field {
									name = translations.openIssues.translate()
									value = repo.openIssueCount.toString()
									inline = false
								}
								field {
									name = translations.forks.translate()
									value = repo.forksCount.toString()
									inline = false
								}
								field {
									name = translations.stars.translate()
									value = repo.stargazersCount.toString()
									inline = false
								}
								field {
									name = translations.size.translate()
									value = bytesToFriendly(repo.size)
									inline = false
								}
								if (repo.language != null) {
									field {
										name = translations.language.translate()
										value = repo.language.toString()
										inline = false
									}
								}
							} catch (ioException: IOException) {
								ioException.printStackTrace()
							}
						}
					}
				}
			}

			publicSubCommand(::UserArgs) {
				name = Translations.Utility.Github.User.name
				description = Translations.Utility.Github.User.description

				action {
					val translations = Translations.Utility.Github.User
					val ghUser: GHUser?

					try {
						ghUser = if (arguments.username.contains("http", true)) {
							github.getUser(arguments.username.split("/")[3])
						} else {
							github.getUser(arguments.username)
						}
					} catch (_: IOException) {
						respond {
							embed {
								title = translations.invalidName.translate()
							}
						}
						return@action
					}

					try {
						val isOrg: Boolean = ghUser?.type.equals("Organization")

						if (!isOrg) {
							respond {
								embed {
									title = translations.embedTitle.translate(ghUser?.login)
									url = "https://github.com/" + ghUser?.login
									description = ghUser?.bio

									field {
										name = translations.repositories.translate()
										value = ghUser?.publicRepoCount.toString()
										inline = false
									}

									field {
										name = translations.followers.translate()
										value = ghUser?.followersCount.toString()
										inline = false
									}

									field {
										name = translations.following.translate()
										value = ghUser?.followingCount.toString()
										inline = false
									}

									if (ghUser!!.company != null) {
										field {
											name = translations.company.translate()
											value = ghUser.company
											inline = false
										}
									}

									if (!ghUser.blog.equals("")) {
										field {
											name = translations.website.translate()
											value = ghUser.blog
											inline = false
										}
									}

									if (ghUser.twitterUsername != null) {
										field {
											name = translations.twitter.translate()
											value =
												"[@" + ghUser.twitterUsername + "](" +
													"https://twitter.com/" + ghUser.twitterUsername + ")"
											inline = false
										}
									}
									footer {
										text = ghUser.login
										icon = ghUser.avatarUrl
									}
									timestamp = Clock.System.now()
								}
							}
						} else {
							val org: GHOrganization? = github.getOrganization(ghUser?.login)

							respond {
								embed {
									title = translations.embedTitle.translate(org?.login)
									url = "https://github.com/" + org?.login

									field {
										name = translations.publicMembers.translate()
										value = org!!.listMembers().toArray().size.toString()
										inline = false
									}
									field {
										name = translations.repositories.translate()
										value = org?.publicRepoCount.toString()
										inline = false
									}

									footer {
										text = org?.login.toString()
										icon = org?.avatarUrl
									}

									timestamp = Clock.System.now()
								}
							}
						}
					} catch (ioException: IOException) {
						ioException.printStackTrace()
					}
				}
			}

			ephemeralSubCommand(::DefaultArgs) {
				name = Translations.Utility.Github.DefaultRepo.name
				description = Translations.Utility.Github.DefaultRepo.description

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ModerateMembers)
				}

				action {
					if (!arguments.defaultRepo.contains("/")) {
						respond {
							embed {
								title = Translations.Utility.Github.badFormatEmbedTitle.translate()
								description = Translations.Utility.Github.badFormatEmbedDesc.translate()
							}
						}
						return@action
					}
					try {
						if (arguments.defaultRepo.contains("http", true)) {
							val urlParts = arguments.defaultRepo.split("/")
							if (urlParts.size <= 4) {
								respond { content = Translations.Utility.Github.DefaultRepo.invalidUrl.translate() }
								return@action
							}
							github.getRepository("${urlParts[3]}/${urlParts[4]}")
						} else {
							github.getRepository(arguments.defaultRepo)
						}
					} catch (_: IOException) {
						respond {
							content = Translations.Utility.Github.unableToRepo.translate()
						}
						return@action
					}

					GithubCollection().setDefaultRepo(guild!!.id, arguments.defaultRepo)
					respond { content = Translations.Utility.Github.DefaultRepo.response.translate() }
				}
			}

			ephemeralSubCommand {
				name = Translations.Utility.Github.RemoveDefaultRepo.name
				description = Translations.Utility.Github.RemoveDefaultRepo.description

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ModerateMembers)
				}

				action {
					if (GithubCollection().getDefaultRepo(guild!!.id) == null) {
						respond { content = Translations.Utility.Github.RemoveDefaultRepo.noDefault.translate() }
						return@action
					}

					GithubCollection().removeDefaultRepo(guild!!.id)
					respond { content = Translations.Utility.Github.RemoveDefaultRepo.response.translate() }
				}
			}
		}
	}

	/**
	 * Convert an input of [bytes] - which is what GitHub sends repository sizes in - and convert it to more friendly
	 * values such as KB, MB and such.
	 *
	 * @param bytes The repository size provided by GitHub
	 * @return The friendly repository size
	 * @since 2.0
	 * @author NoComment1105, chalkyjeans (java version author)
	 */
	private fun bytesToFriendly(bytes: Int): String {
		val byteValue = 1024.0
		val measure = arrayOf("B", "KB", "MB", "GB", "TB")

		val i = if (bytes == 0) {
			0.0
		} else {
			floor(ln(bytes.toDouble()) / ln(byteValue))
		}

		val df = DecimalFormat("#.##") // Set the formatted response to 2 decimal places

		return df.format(bytes / byteValue.pow(i)) + " " + measure[(i + 1).toInt()]
	}

	inner class IssueArgs : Arguments() {
		/** The issue number being searched for. */
		val issue by int {
			name = Translations.Utility.Github.Issue.Arguments.Issue.name
			description = Translations.Utility.Github.Issue.Arguments.Issue.description
		}

		/** The repository being searched for, must contain a `/`. */
		val repository by optionalString {
			name = Translations.Utility.Github.Issue.Arguments.Repo.name
			description = Translations.Utility.Github.Issue.Arguments.Repo.description
		}
	}

	inner class RepoArgs : Arguments() {
		/** The repository being searched for, must contain a `/`. */
		val repository by optionalString {
			name = Translations.Utility.Github.Issue.Arguments.Repo.name
			description = Translations.Utility.Github.Issue.Arguments.Repo.description
		}
	}

	inner class UserArgs : Arguments() {
		/** The name of the User/Organisation being searched for. */
		val username by string {
			name = Translations.Utility.Github.User.Arguments.Username.name
			description = Translations.Utility.Github.User.Arguments.Username.description
		}
	}

	inner class DefaultArgs : Arguments() {
		/** The default repo for the GitHub commands. */
		val defaultRepo by string {
			name = Translations.Utility.Github.DefaultRepo.name
			description = Translations.Utility.Github.DefaultRepo.description
		}
	}
}
