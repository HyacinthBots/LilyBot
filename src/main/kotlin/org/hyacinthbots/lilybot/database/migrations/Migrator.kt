@file:Suppress("NoWildcardImports", "WildcardImport")

/*
 * This source code form has been adapted from QuiltMC's Discord Bot, Cozy (https://github.com/QuiltMC/cozy-discord).
 *
 * As a result of this, the following license applies to this source code:
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.hyacinthbots.lilybot.database.migrations

import dev.kordex.core.koin.KordExKoinComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.collections.ConfigMetaCollection
import org.hyacinthbots.lilybot.database.collections.MainMetaCollection
import org.hyacinthbots.lilybot.database.entities.ConfigMetaData
import org.hyacinthbots.lilybot.database.entities.MainMetaData
import org.hyacinthbots.lilybot.database.migrations.config.*
import org.hyacinthbots.lilybot.database.migrations.main.*
import org.koin.core.component.inject

object Migrator : KordExKoinComponent {
	private val logger = KotlinLogging.logger("Migrator Logger")

	val db: Database by inject()
	private val mainMetaCollection: MainMetaCollection by inject()
	private val configMetaCollection: ConfigMetaCollection by inject()

	suspend fun migrateMain() {
		logger.info { "Starting main database migration" }

		var meta = mainMetaCollection.get()

		if (meta == null) {
			meta = MainMetaData(0)

			mainMetaCollection.set(meta)
		}

		var currentVersion = meta.version

		logger.info { "Current main database version: v$currentVersion" }

		while (true) {
			val nextVersion = currentVersion + 1

			@Suppress("TooGenericExceptionCaught")
			try {
				when (nextVersion) {
					1 -> ::mainV1
					2 -> ::mainV2
					3 -> ::mainV3
					4 -> ::mainV4
					5 -> ::mainV5
					6 -> ::mainV6
					7 -> ::mainV7
					8 -> ::mainV8
					9 -> ::mainV9
					10 -> ::mainV10
					11 -> ::mainV11
					12 -> ::mainV12
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

			mainMetaCollection.update(meta)

			logger.info { "Finished main database migrations." }
		}
	}

	suspend fun migrateConfig() {
		logger.info { "Starting config database migration" }

		var meta = configMetaCollection.get()

		if (meta == null) {
			meta = ConfigMetaData(0)

			configMetaCollection.set(meta)
		}

		var currentVersion = meta.version

		logger.info { "Current config database version: v$currentVersion" }

		while (true) {
			val nextVersion = currentVersion + 1

			@Suppress("TooGenericExceptionCaught")
			try {
				when (nextVersion) {
					1 -> ::configV1
					2 -> ::configV2
					3 -> ::configV3
					4 -> ::configV4
					5 -> ::configV5
					6 -> ::configV6
					7 -> ::configV7
					8 -> ::configV8
					else -> break
				}(db.configDatabase)

				logger.info { "Migrated config database to version $nextVersion" }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate config database to version $nextVersion." }

				throw t
			}

			currentVersion = nextVersion
		}

		if (currentVersion != meta.version) {
			meta = meta.copy(version = currentVersion)

			configMetaCollection.update(meta)

			logger.info { "Finished config database migrations." }
		}
	}
}
