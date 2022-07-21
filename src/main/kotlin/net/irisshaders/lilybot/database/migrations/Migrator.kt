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
import net.irisshaders.lilybot.database.collections.ConfigMetaCollection
import net.irisshaders.lilybot.database.collections.MainMetaCollection
import net.irisshaders.lilybot.database.entities.ConfigMetaData
import net.irisshaders.lilybot.database.entities.MainMetaData
import net.irisshaders.lilybot.database.migrations.config.configV1
import net.irisshaders.lilybot.database.migrations.main.mainV1
import org.koin.core.component.inject

object Migrator : KordExKoinComponent {
	private val logger = KotlinLogging.logger("Migrator Logger")

	val db: Database by inject()
	private val mainMetaCollection: MainMetaCollection by inject()
	private val configMetaCollection: ConfigMetaCollection by inject()

	suspend fun migrateMain() {
		var meta = mainMetaCollection.get()

		if (meta == null) {
			meta = MainMetaData(0)

			mainMetaCollection.set(meta)
		}

		var currentVersion = meta.version

		@Suppress("TooGenericExceptionCaught")
		while (true) {
			val nextVersion = currentVersion + 1

			try {
				@Suppress("UseIfInsteadOfWhen")
				when (nextVersion) {
					1 -> ::mainV1
					else -> break
				}(db.mainDatabase)

				logger.info { "Migrated main database to version $nextVersion." }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate main database to version $nextVersion." }

				throw t
			}

			currentVersion = nextVersion
		}

		if (currentVersion != meta.version) {
			meta = meta.copy(version = currentVersion)

			mainMetaCollection.set(meta)

			logger.info { "Finished database migrations." }
		}
	}

	suspend fun migrateConfig() {
		var meta = configMetaCollection.get()

		if (meta == null) {
			meta = ConfigMetaData(0)

			configMetaCollection.set(meta)
		}

		var currentVersion = meta.version

		@Suppress("TooGenericExceptionCaught")
		while (true) {
			val nextVersion = currentVersion + 1

			try {
				@Suppress("UseIfInsteadOfWhen")
				when (nextVersion) {
					1 -> ::configV1
					else -> break
				}(db.mainDatabase, db.configDatabase) // TODO Remove the first param, comment knows whats needed

				logger.info { "Migrated config database to version $nextVersion" }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate config database to version $nextVersion." }

				throw t
			}

			currentVersion = nextVersion
		}
	}
}
