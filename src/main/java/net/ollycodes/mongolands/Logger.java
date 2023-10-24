package net.ollycodes.mongolands;

public class Logger {
    public final MongoLands plugin;
    public Logger(MongoLands plugin) { this.plugin = plugin; }

    public void info(String text) {
        plugin.getLogger().log(java.util.logging.Level.INFO, "\u001B[34m" + text + "\u001B[0m");
    }

    public void success(String text) {
        plugin.getLogger().log(java.util.logging.Level.INFO, "\u001B[32m" + text + "\u001B[0m");
    }

    public void warn(String text) {
        plugin.getLogger().log(java.util.logging.Level.WARNING, "\u001B[33m" + text + "\u001B[0m");
    }

    public void error(String text) {
        plugin.getLogger().log(java.util.logging.Level.SEVERE, "\u001B[31m" + text + "\u001B[0m");
    }
}
