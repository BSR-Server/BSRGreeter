package org.bsrserver;

import java.sql.*;
import java.nio.file.Path;
import java.util.HashMap;

import org.slf4j.Logger;
import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import org.bsrserver.config.Config;
import org.bsrserver.components.Sentences;
import org.bsrserver.components.ServerInfo;
import org.bsrserver.event.ServerConnectedEventEventListener;

@Plugin(
        id = "bsrgreeter",
        name = "BSR Greeter",
        version = "1.3.2",
        url = "https://www.bsrserver.org:8443",
        description = "A greeter",
        authors = {"Andy Zhang"}
)
public class Main {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final HashMap<String, ServerInfo> serverInfoHashMap = new HashMap<>();
    private final Sentences sentences;

    @Inject
    public Main(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;

        // load config
        Config.getInstance().loadConfig(dataDirectory);

        // init data
        new Thread(this::loadDatabase).start();
        this.sentences = new Sentences();
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        // register command
        proxyServer.getEventManager().register(this, new ServerConnectedEventEventListener(this));
    }

    private void loadDatabase() {
        boolean loadDatabase = false;
        do {
            try {
                // connect
                Class.forName("org.postgresql.Driver");
                Connection connection = DriverManager.getConnection(Config.getInstance().getDatabaseUrl(), Config.getInstance().getDatabaseUser(), Config.getInstance().getDatabasePassword());
                logger.info("Successfully connected to database");

                // select
                String tableName = Config.getInstance().getDatabaseTable();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);
                while (resultSet.next()) {
                    String serverName = resultSet.getString("server_name");
                    ServerInfo serverInfo = new ServerInfo(serverName, resultSet.getString("named_name"), resultSet.getDate("foundation_time").toLocalDate(), resultSet.getInt("priority"));
                    serverInfoHashMap.put(serverName, serverInfo);
                }
                resultSet.close();
                statement.close();
                connection.close();
                loadDatabase = true;
                logger.info("Loaded servers: " + serverInfoHashMap.keySet());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Fail connect to database, retry in 10 seconds...");
                try {
                    Thread.sleep(100000);
                } catch (Exception ignored) {
                }
            }
        } while (!loadDatabase);
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public HashMap<String, ServerInfo> getServerInfoHashMap() {
        return serverInfoHashMap;
    }

    public Sentences getSentences() {
        return sentences;
    }
}
