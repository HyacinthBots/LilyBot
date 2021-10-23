package net.irisshaders.lilybot.tags

import net.irisshaders.lilybot.utils.REPO_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.irisshaders.lilybot.tags.Tag
import net.irisshaders.lilybot.tags.TagParser
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

class TagRepo(
    private val repoDir: Path,
) {
    private val tagParser = TagParser()

    private val git: Git by lazy {
        if (Files.exists(repoDir)) {
            logger.info { "Opening git repo: $repoDir..." }
            Git.open(repoDir.toFile())
        } else {
            logger.info { "Repo $repoDir does not exist, cloning from $REPO_URL..." }
            Git.cloneRepository()
                .setURI(REPO_URL)
                .setDirectory(repoDir.toFile())
                .call()
        }
    }

    // Run these on the IO dispatcher to avoid blocking the main thread(s)
    suspend fun init() = withContext(Dispatchers.IO) {
        updateTags()
        tagParser.loadTags(repoDir.resolve("tags"))
    }

    suspend fun reload() = withContext(Dispatchers.IO) {
        updateTags()
        tagParser.reloadTags(repoDir.resolve("tags"))
    }

    private fun updateTags() {
        logger.info { "Ensuring tag repo is up-to-date..." }
        this.git.pull()
            .setRemote("origin")
            .call()
    }

    operator fun get(name: String): Tag? = this.tagParser.getTag(name)
}