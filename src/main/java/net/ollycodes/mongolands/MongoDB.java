package net.ollycodes.mongolands;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class MongoDB {

    public final MongoLands plugin;
    public MongoDB(MongoLands plugin) { this.plugin = plugin; }

    public MongoDatabase database;
    public MongoClient mongoClient;

    public MongoCollection<Document> containers_world;
    public MongoCollection<Document> declarations;
    public MongoCollection<Document> lands;
    public MongoCollection<Document> nations;
    public MongoCollection<Document> players;
    public MongoCollection<Document> pluginCollection;
    public MongoCollection<Document> relations;
    public MongoCollection<Document> wars;
    public MongoCollection<Document> worlds;
    public String[] collectionNames = {
        "containers_world", "declarations", "lands", "nations", "players", "plugin", "relations", "wars", "worlds"
    };

    public MongoDatabase getDatabase() {
        if (database != null) {
            return database;
        }

        String connectionString = plugin.getConfig().getString("mongo_connection_string");
        String databaseName = plugin.getConfig().getString("mongo_database");
        assert connectionString != null;
        assert databaseName != null;
        try {
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(databaseName);
            for (String collectionName : collectionNames) {
                if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                    database.createCollection(collectionName);
                    plugin.logger.info("Created collection " + collectionName);
                }
            }
            containers_world = plugin.mongo.database.getCollection("containers_world");
            declarations = plugin.mongo.database.getCollection("declarations");
            lands = plugin.mongo.database.getCollection("lands");
            nations = plugin.mongo.database.getCollection("nations");
            players = plugin.mongo.database.getCollection("players");
            pluginCollection = plugin.mongo.database.getCollection("plugin");
            relations = plugin.mongo.database.getCollection("relations");
            wars = plugin.mongo.database.getCollection("wars");
            worlds = plugin.mongo.database.getCollection("worlds");

            plugin.logger.info("Lands: " + lands.countDocuments());
        } catch (Exception e) {
            plugin.logger.error("Error connecting to MongoDB: " + e.getMessage());
            throw new RuntimeException("Error connecting to MongoDB: " + e.getMessage());
        }
        return database;
    }

    public boolean testMongo() {
        try {
            getDatabase();
            plugin.logger.success("Connected to MongoDB");
            return true;
        } catch (Exception e) {
            plugin.logger.error("Error connecting to MongoDB: " + e.getMessage());
            return false;
        }
    }
}
