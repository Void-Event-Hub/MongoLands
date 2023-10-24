package net.ollycodes.mongolands;

import javax.annotation.Nonnull;

public class Config {
    public static void handleConfig(@Nonnull MongoLands plugin) {
        plugin.config.addDefault("mongo_connection_string", "mongodb+srv://<username>:<password>@<domain>/?retryWrites=true&w=majority");
        plugin.config.addDefault("mongo_database", "mongo_lands");
        plugin.config.addDefault("lands_db_path", "plugins/Lands/Data/database.db");
        plugin.config.addDefault("sync_interval", "600");
        plugin.config.options().copyDefaults(true);
        plugin.saveConfig();
    }
}
