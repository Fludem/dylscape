package org.rsmod.api.db.migration

import jakarta.inject.Inject
import org.flywaydb.core.Flyway
import org.rsmod.api.db.DatabaseConfig

public class FlywayMigration @Inject constructor(private val config: DatabaseConfig) {
    public fun migrate() {
        val flyway =
            Flyway.configure()
                .dataSource(config.url, config.user, config.password)
                .locations("classpath:db/migration", "classpath:plugin/**/migration")
                // Plugin migrations are numbered from 100 up while core `db/migration` stays
                // low, so once any plugin migration has run, every new core migration is
                // "out of order" and default validation refuses to boot. Allow it: version
                // order across independently-versioned modules carries no meaning here.
                .outOfOrder(true)
                .load()
        flyway.migrate()
    }
}
