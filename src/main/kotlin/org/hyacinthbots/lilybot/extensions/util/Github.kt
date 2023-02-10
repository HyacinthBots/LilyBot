package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
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
			name = "github"
			description = "The parent command for all /github commands"

			publicSubCommand(::IssueArgs) {
				name = "issue"
				description = "Look up an issue on a specific repository"

				action {
					val repository = arguments.repository ?: GithubCollection().getDefaultRepo(guild!!.id)
					if (repository == null) {
						respond {
							content =
								"There is no default repository set. Please specify a repository to search or set" +
										"a default."
						}
						return@action
					}
					// Clarify the input is formatted correctly, inform the user if not.
					if (!repository.contains("/")) {
						sentry.breadcrumb(BreadcrumbType.Error) {
							category = "extensions.util.Github.issue.InputCheck"
							message = "Input missing /"
						}
						respondEphemeral {
							embed {
								title = "Make sure your repository input is formatted like this:"
								description = "Format: `User/Repo` or `Org/Repo` \nFor example: `HyacinthBots/LilyBot`"
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
							} catch (e: GHFileNotFoundException) {
								respondEphemeral {
									embed {
										title = "Unable to find issue number! Make sure this issue exists"
									}
								}
								return@action
							}
						}
					} catch (e: IOException) {
						val iterator: PagedIterator<GHIssue>? = github.searchIssues()
							?.q("${arguments.issue} repo:$repository")
							?.order(GHDirection.DESC)
							?.list()
							?._iterator(1)

						// Run a quick check on the iterator, in case the repository owner org/user doesn't exist
						try {
							iterator!!.hasNext()
						} catch (e: GHException) {
							respondEphemeral {
								embed {
									title = "Unable to access repository, make sure this repository exists!"
								}
							}
							return@action
						}

						if (iterator.hasNext()) {
							issue = iterator.next()
						} else {
							sentry.breadcrumb(BreadcrumbType.Error) {
								category = "extensions.util.Github.issue.getIssue"
								message = "Unable to find issue"
							}

							respondEphemeral {
								embed {
									title = "Invalid issue number. Make sure this issue exists!"
								}
							}
							return@action
						}

						val num = issue!!.number
						try {
							issue = github.getRepository(repository)?.getIssue(num)
						} catch (e: GHFileNotFoundException) {
							respond {
								embed {
									title = "Unable to find issue number! Make sure this issue exists"
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
								description =
									"**Information for Pull request #${issue.number} in ${issue.repository.fullName}**"

								try {
									val pull: GHPullRequest = issue.repository.getPullRequest(issue.number)
									merged = pull.isMerged
									draft = pull.isDraft
								} catch (ioException: IOException) {
									sentry.breadcrumb(BreadcrumbType.Error) {
										category = "extensions.util.Github.issue.CheckPRStatus"
										message = "Error initializing PR wtf"
									}
									ioException.printStackTrace()
									title = "Error!"
									description = "Error occurred initializing Pull Request. How did this happen?"
									color = DISCORD_RED
									return@action
								}
							} else {
								title = issue.title
								url = issue.htmlUrl.toString()
								description =
									"**Information for issue #${issue.number} in ${issue.repository.fullName}**"
							}

							field {
								// Grab the first 400 characters of the body, if it's not null
								if (issue.body != null) {
									value = if (issue.body.length > 400) {
										issue.body.substring(0, 399) + "..."
									} else if (issue.body.isNotEmpty() && issue.body.length <= 399) {
										issue.body
									} else {
										"No description Provided"
									}
								}
							}

							// Use colours similar to that of GitHub's issue status colours
							if (merged) {
								color = dev.kord.common.Color(111, 66, 193)
								field {
									name = "Status:"
									value = "Merged"
									inline = false
								}
							} else if (!open) {
								color = dev.kord.common.Color(203, 36, 49)
								field {
									name = "Status:"
									value = "Closed"
									inline = false
								}
							} else if (draft) {
								color = dev.kord.common.Color(255, 255, 255)
								field {
									name = "Status:"
									value = "Draft"
									inline = false
								}
							} else {
								color = dev.kord.common.Color(44, 185, 78)
								field {
									name = "Status:"
									value = "Open"
									inline = false
								}
							}

							try {
								val author: GHUser = issue.user
								if (author.name != null) {
									field {
										name = "Author:"
										value =
											"[" + author.login + " (" + author.name + ")](" +
													"https://github.com/" + author.login + ")"
										inline = false
									}
								} else {
									field {
										name = "Author:"
										value = "[" + author.login + "](https://github.com/" + author.login + ")"
										inline = false
									}
								}
							} catch (ioException: IOException) {
								field {
									name = "Author:"
									value = "Unknown Author"
									inline = false
								}
							}

							try {
								field {
									name = "Opened on:"
									value = "${issue.createdAt}"
									inline = false
								}

								val labels = mutableListOf<CharSequence>()

								for (label: GHLabel in issue.labels) {
									labels.add(label.name)
								}

								if (labels.size > 0) {
									field {
										name = "Labels:"
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
				name = "repo"
				description = "Search GitHub for a specific repository"

				action {
					val repository = arguments.repository ?: GithubCollection().getDefaultRepo(guild!!.id)
					if (repository == null) {
						respond {
							content =
								"There is no default repository set. Please specify a repository to search or set" +
										"a default."
						}
						return@action
					}
					// Clarify the input is formatted correctly, inform the user if not
					if (!repository.contains("/")) {
						sentry.breadcrumb(BreadcrumbType.Error) {
							category = "extensions.util.Github.repository.InputCheck"
							message = "Input missing /"
						}
						respondEphemeral {
							embed {
								title = "Make sure your input is formatted like this:"
								description = "Format: `User/Repo` or `Org/Repo`\nFor example: `HyacinthBots/LilyBot`"
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
						sentry.breadcrumb(BreadcrumbType.Info) {
							category = "extensions.util.Github.repository.getRepository"
							message = "Repository found"
						}
					} catch (exception: IOException) {
						sentry.breadcrumb(BreadcrumbType.Error) {
							category = "extensions.util.Github.repository.getRepository"
							message = "Repository not found"
						}
						respondEphemeral {
							embed {
								title = "Invalid repository name. Make sure this repository exists"
							}
						}
						repo = null
						return@action
					}

					respond {
						embed {
							try {
								title = "GitHub Repository Info for " + repo?.fullName
								url = repo?.htmlUrl.toString()
								description = repo?.description

								if (repo!!.license != null) {
									field {
										name = "License:"
										value = repo.license.name
										inline = false
									}
								}

								field {
									name = "Open Issues and PRs:"
									value = repo.openIssueCount.toString()
									inline = false
								}
								field {
									name = "Forks:"
									value = repo.forksCount.toString()
									inline = false
								}
								field {
									name = "Stars:"
									value = repo.stargazersCount.toString()
									inline = false
								}
								field {
									name = "Size:"
									value = bytesToFriendly(repo.size)
									inline = false
								}
								if (repo.language != null) {
									field {
										name = "Language:"
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
				name = "user"
				description = "Search GitHub for a User/Organisation"

				action {
					val ghUser: GHUser?

					try {
						ghUser = if (arguments.username.contains("http", true)) {
							github.getUser(arguments.username.split("/")[3])
						} else {
							github.getUser(arguments.username)
						}
						sentry.breadcrumb(BreadcrumbType.Info) {
							category = "extensions.util.Github.user.getUser"
							message = "User found"
						}
					} catch (exception: IOException) {
						sentry.breadcrumb(BreadcrumbType.Error) {
							category = "extensions.util.Github.user.getUser"
							message = "Unable to find user"
						}
						respondEphemeral {
							embed {
								title = "Invalid Username. Make sure this user exists!"
							}
						}
						return@action
					}

					try {
						val isOrg: Boolean = ghUser?.type.equals("Organization")

						if (!isOrg) {
							sentry.breadcrumb(BreadcrumbType.Info) {
								category = "extensions.util.Github.user.isOrg"
								message = "User is not Organisation"
								data["isNotOrg"] = ghUser?.login
							}
							respond {
								embed {
									title = "GitHub profile for " + ghUser?.login
									url = "https://github.com/" + ghUser?.login
									description = ghUser?.bio

									field {
										name = "Followers:"
										value = ghUser?.followersCount.toString()
										inline = false
									}

									field {
										name = "Following:"
										value = ghUser?.followingCount.toString()
										inline = false
									}

									if (ghUser!!.company != null) {
										field {
											name = "Company"
											value = ghUser.company
											inline = false
										}
									}

									if (!ghUser.blog.equals("")) {
										field {
											name = "Website:"
											value = ghUser.blog
											inline = false
										}
									}

									if (ghUser.twitterUsername != null) {
										field {
											name = "Twitter:"
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
							sentry.breadcrumb(BreadcrumbType.Info) {
								category = "extensions.util.Github.user.isOrg"
								message = "User is Organisation"
								data["isOrg"] = ghUser?.login
							}
							val org: GHOrganization? = github.getOrganization(ghUser?.login)

							respond {
								embed {
									title = "GitHub profile for " + ghUser?.login
									url = "https://github.com/" + ghUser?.login

									field {
										name = "Public Members:"
										value = org!!.listMembers().toArray().size.toString()
										inline = false
									}
									field {
										name = "Repositories:"
										value = ghUser?.publicRepoCount.toString()
										inline = false
									}

									footer {
										text = ghUser?.login.toString()
										icon = ghUser?.avatarUrl
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
				name = "default-repo"
				description = "Set the default repo to look up issues in."

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
								title = "Make sure your input is formatted like this:"
								description = "Format: `User/Repo` or `Org/Repo`\nFor example: `HyacinthBots/LilyBot`"
							}
						}
						return@action
					}
					try {
						if (arguments.defaultRepo.contains("http", true)) {
							val urlParts = arguments.defaultRepo.split("/")
							if (urlParts.size <= 4) {
								respond { content = "Invalid repo URL, please try again" }
								return@action
							}
							github.getRepository("${urlParts[3]}/${urlParts[4]}")
						} else {
							github.getRepository(arguments.defaultRepo)
						}
					} catch (e: IOException) {
						respond {
							content = "GitHub repository not found! Please make sure this repository exists"
						}
						return@action
					}

					GithubCollection().setDefaultRepo(guild!!.id, arguments.defaultRepo)
					respond { content = "Default repo set." }
				}
			}

			ephemeralSubCommand {
				name = "remove-default-repo"
				description = "Removes the default repo for this guild"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requireBotPermissions(Permission.ModerateMembers)
				}

				action {
					if (GithubCollection().getDefaultRepo(guild!!.id) == null) {
						respond { content = "There is no default repo for this guild!" }
						return@action
					}

					GithubCollection().removeDefaultRepo(guild!!.id)
					respond {
						content = "Default repo removed"
					}
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
			name = "issue-number"
			description = "The issue number you would like to search for"
		}

		/** The repository being searched for, must contain a `/`. */
		val repository by optionalString {
			name = "repository"
			description = "The GitHub repository you would like to search if no default is set"
		}
	}

	inner class RepoArgs : Arguments() {
		/** The repository being searched for, must contain a `/`. */
		val repository by optionalString {
			name = "repository"
			description = "The GitHub repository you would like to search if no default is set"
		}
	}

	inner class UserArgs : Arguments() {
		/** The name of the User/Organisation being searched for. */
		val username by string {
			name = "username"
			description = "The name of the User/Organisation you wish to search for"
		}
	}

	inner class DefaultArgs : Arguments() {
		/** The default repo for the GitHub commands. */
		val defaultRepo by string {
			name = "default-repo"
			description = "The default repo to look up issues in"
		}
	}
}
