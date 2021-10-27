/**
 * This code was taken, with permission, from <https://github.com/QuiltServerTools/axolotl>
 * This code is Licensed under the MIT license (<https://mit-license.org/>) unlike the rest of hte project
 * @author Tom_The_Geek
 */
package net.irisshaders.lilybot.tags

import mu.KotlinLogging
import net.irisshaders.lilybot.utils.REPO_URL
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

class TagRepo(private val repoDir: Path) {
    private val tagParser = TagParser()

    private val git: Git by lazy {
        if (Files.exists(repoDir)) {
            logger.debug { "Opening git repo: $repoDir..." }

            Git.open(repoDir.toFile())
        } else {
            logger.debug { "Repo $repoDir does not exist, cloning from $REPO_URL..." }

            Git.cloneRepository()
                .setURI(REPO_URL)
                // Make sure to set this to the correct branch, or remove once merged into main
                .setBranch("kord-experiments")
                .setDirectory(repoDir.toFile())
                .call()
        }
    }

    fun init() {
        updateTags()

        tagParser.loadTags(repoDir.resolve("tags"))
    }

    fun reload() {
        updateTags()

        tagParser.reloadTags(repoDir.resolve("tags"))
    }

    private fun updateTags() {
        logger.debug { "Ensuring tag repo is up-to-date..." }

        this.git.pull()
            .setRemote("origin")
            .call()
    }

    operator fun get(name: String): Tag? = this.tagParser.getTag(name)
}
