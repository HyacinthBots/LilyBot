package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondPublic
import dev.kord.rest.builder.message.create.embed
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.github

import org.kohsuke.github.GHIssue
import org.kohsuke.github.PagedIterator
import org.kohsuke.github.GHDirection
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHUser
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHLabel
import org.kohsuke.github.GHOrganization

import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow


class Github : Extension() {
    override val name = "github"

    override suspend fun setup() {
        /**
         * GitHub Issue Command
         * @author NoComment1105
         * Java version author: chalkyjeans
         */
        publicSlashCommand {
            name = "github"
            description = "The parent command for all /github commands"

            publicSubCommand(::IssueArgs) {
                name = "issue"
                description = "Look up an issue on a specific repository"

                action {
                    val issueNumberArg = arguments.issue
                    val issueRepoArg = arguments.repository
                    var issue: GHIssue?

                    try {
                        issue = github?.getRepository(issueRepoArg)?.getIssue(issueNumberArg)
                    } catch (e: NumberFormatException) {
                        val iterator: PagedIterator<GHIssue>? = github?.searchIssues()
                            ?.q("$issueNumberArg repo: $issueRepoArg")
                            ?.order(GHDirection.DESC)
                            ?.list()
                            ?._iterator(1)

                        if (iterator!!.hasNext()) {
                            issue = iterator.next()
                        } else {
                            throw Exception("Could not find specified issue in repo `$issueRepoArg`!")
                        }

                        val num: Int = issue.number
                        issue = github?.getRepository(issueRepoArg)?.getIssue(num)
                    }
                    respondPublic {
                        embed {

                            val open: Boolean = issue?.state == GHIssueState.OPEN
                            var merged = false
                            var draft = false

                            if (issue!!.isPullRequest) {
                                title =
                                    "Information for Pull Request #" + issue.number + " in " + issue.repository.fullName

                                try {
                                    val pull: GHPullRequest = issue.repository.getPullRequest(issue.number)
                                    merged = pull.isMerged
                                    draft = pull.isDraft
                                } catch (ioException: IOException) {
                                    ioException.printStackTrace()
                                    title = "Error!"
                                    description = "Error occurred initializing Pull Request. How did this happen?"
                                    return@embed
                                }
                            } else {
                                title = "Information for issue #" + issue.number + " in " + issue.repository.fullName
                            }

                            field {
                                name = issue.htmlUrl.toString()

                                if (issue.body != null) {
                                    value = if (issue.body.length > 400) {
                                        issue.body.substring(0, 399) + "..."
                                    } else {
                                        issue.body
                                    }
                                }
                            }

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
                                            "[" + author.login + " (" + author.name + ")](https://github.com/" + author.login + ")"
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
                            footer {
                                text = "Requested by: " + user.asUser().tag
                                icon = user.asUser().avatar?.url
                            }
                            timestamp = Clock.System.now()
                        }
                    }
                }
            }

            publicSubCommand(::RepoArgs) {
                name = "repo"
                description = "Search GitHub for a specific repository"

                action {
                    val repoArg = arguments.repository

                    if (!repoArg.contains("/")) {
                        respond {
                            embed {
                                title = "Make sure your input is formatted like this:"
                                description = "Format: `User/Repo` or `Org/Repo` \n For example: `IrisShaders/Iris"
                                color = DISCORD_RED
                                return@embed
                            }
                        }
                    }

                    var repo: GHRepository?

                    try {
                        repo = github!!.getRepository(repoArg)
                    } catch (ioException: IOException) {
                        respond {
                            embed {
                                title = "Invalid repository name. Make sure this repository exists!"
                                color = DISCORD_RED
                                return@action
                            }
                        }
                        repo = null
                    }

                    respond {
                        embed {
                            try {
                                title = "GitHub Repository Info for " + repo?.fullName
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
                                field {
                                    name = repo.htmlUrl.toString()
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
                description = "Search github for a User/Organisation"

                action {
                    var ghUser: GHUser? = null

                    try {
                        ghUser = github?.getUser(arguments.username)
                    } catch (exception: Exception) {
                        respond {
                            embed {
                                color = DISCORD_RED
                                title = "Invalid Username. Make sure this user exists"
                                return@action
                            }
                        }
                    }

                    try {
                        val isOrg: Boolean = ghUser?.type.equals("Organization")

                        if (!isOrg) {
                            respond {
                                embed {
                                    title = "GitHub profile for " + ghUser?.login
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
                                                "[@" + ghUser.twitterUsername + "](https://twitter.com/" + ghUser.twitterUsername + ")"
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
                            val org: GHOrganization? = github?.getOrganization(ghUser?.login)

                            respond {
                                embed {
                                    title = "GitHub profile for " + ghUser?.login
                                    description = "https://github.com/" + ghUser?.login

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
        }
    }

    private fun bytesToFriendly(bytes: Int): String {
        val k = 1024
        val measure = arrayOf("B", "KB", "MB", "GB", "TB")

        val i: Double = if (bytes == 0) {
            0.0
        } else {
            floor(ln(bytes.toDouble()) / ln(k.toDouble()))
        }

        val df = DecimalFormat("#.##")

        return df.format(bytes / k.toDouble().pow(i)) + " " + measure[(i + 1).toInt()]
    }

    inner class IssueArgs : Arguments() {
        val repository by string("repository", "The GitHub repository you would like to search")
        val issue by int("issue-number", "The issue number you would like to search for")
    }

    inner class RepoArgs : Arguments() {
        val repository by string("repository", "The github repository you would like to search for")
    }

    inner class UserArgs : Arguments() {
        val username by string("username", "The name of the User/Organisation you wish to search for")
    }
}