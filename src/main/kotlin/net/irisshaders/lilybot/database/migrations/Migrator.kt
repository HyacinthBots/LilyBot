/*
 * This source code form has been adapted from QuiltMC's Discord Bot, Cozy (https://github.com/QuiltMC/cozy-discord).
 *
 * As a result of this, the following license applies to this source code:
	 * This Source Code Form is subject to the terms of the Mozilla Public
	 * License, v. 2.0. If a copy of the MPL was not distributed with this
	 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.irisshaders.lilybot.database.migrations

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import mu.KotlinLogging
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.collections.MetaCollection
import net.irisshaders.lilybot.database.entities.MetaData
import net.irisshaders.lilybot.database.migrations.config.configV1
import net.irisshaders.lilybot.database.migrations.main.mainV1
import org.koin.core.component.inject

object Migrator : KordExKoinComponent {
	private val logger = KotlinLogging.logger("Migrator Logger")

	val db: Database by inject()
	val metaCollection: MetaCollection by inject()

	suspend fun migrate() {
		var meta = metaCollection.get()

		if (meta == null) {
			meta = MetaData(0, 0)

			metaCollection.set(meta)
		}

		var currentMainVersion = meta.mainVersion
		var currentConfigVersion = meta.configVersion

		@Suppress("TooGenericExceptionCaught")
		while (true) {
			val nextMainVersion = currentMainVersion + 1
			val nextConfigVersion = currentConfigVersion + 1

			try {
				@Suppress("UseIfInsteadOfWhen")
				when (nextMainVersion) {
					1 -> ::mainV1
					else -> break
				}(db.mainDatabase)

				logger.info { "Migrated main database to version $nextMainVersion." }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate main database to version $nextMainVersion." }

				throw t
			}

			currentMainVersion = nextMainVersion

			try {
				@Suppress("UseIfInsteadOfWhen")
				when (nextConfigVersion) {
					1 -> ::configV1
					else -> break
				}(db.configDatabase)

				logger.info { "Migrated config database to version $nextConfigVersion" }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate config database to version $nextConfigVersion." }

				throw t
			}

			currentConfigVersion = nextConfigVersion
		}

		if (currentMainVersion != meta.mainVersion || currentConfigVersion != meta.configVersion) {
			meta = meta.copy(mainVersion = currentMainVersion, configVersion = currentConfigVersion)

			metaCollection.set(meta)

			logger.info { "Finished database migrations." }
		}
	}
}
