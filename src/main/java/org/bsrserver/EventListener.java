package org.bsrserver;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EventListener {
    private final ProxyServer proxyServer;
    private final HashMap<String, ServerInfo> serverInfoHashMap;

    private String getOpenDays(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        ServerInfo serverInfo = serverInfoHashMap.get(serverName);
        String namedName = serverName;
        int daysBetween = 0;
        if (serverInfo != null) {
            namedName = serverInfo.namedName() != null ? serverInfo.namedName() : namedName;
            daysBetween = (int) ChronoUnit.DAYS.between(serverInfo.foundationTime().toLocalDate(), LocalDate.now());
        }
        return "这是 " + namedName + " 开服的第 " + daysBetween + " 天\n\n";
    }

    private String getSentence() {
        try {
            // request
            URL url = new URL("https://www.bsrserver.org:8443/static?prefix=pages/gifs/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
            connection.disconnect();

            // parse
            NodeList nodes = document.getFirstChild().getChildNodes();
            ArrayList<String> sentences = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals("Contents")) {
                    String text = node.getFirstChild().getTextContent();
                    text = text
                            .replaceAll("^pages/gifs/", "")
                            .replaceAll(".jpg$", "")
                            .replaceAll(".png$", "");
                    sentences.add(text);
                }
            }

            // return
            return sentences.get((int) (Math.random() * sentences.size()));
        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    private Component getServerList(RegisteredServer server) {
        ArrayList<Component> components = new ArrayList<>();

        // for each server
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            String serverName = registeredServer.getServerInfo().getName();
            Component serverNameComponent;

            // this server or other server
            if (serverName.equals(server.getServerInfo().getName())) {
                serverNameComponent = Component.text("[§l" + serverName + "§r]")
                        .hoverEvent(HoverEvent.showText(Component.text("当前服务器")));
            } else {
                serverNameComponent = Component.text("[§a" + serverName + "§r]")
                        .clickEvent(ClickEvent.runCommand("/server " + serverName))
                        .hoverEvent(HoverEvent.showText(Component.text("点击加入服务器 §b" + serverName)));
            }
            components.add(serverNameComponent);
        }

        return Component.join(JoinConfiguration.separator(Component.text(" ")), components);
    }

    public EventListener(Main main) {
        this.proxyServer = main.getProxyServer();
        this.serverInfoHashMap = main.getServerInfoHashMap();
    }

    @Subscribe
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        new Thread(() -> {
            Component message = Component.text("-".repeat(40) + "\n")
                    .append(Component.text("§e§l" + event.getPlayer().getUsername()))
                    .append(Component.text("§r, 欢迎回到 §bBSR 服务器§r！\n"))
                    .append(Component.text(getOpenDays(event.getServer())))
                    .append(Component.text("[§a一言§r] " + getSentence() + "\n\n"))
                    .append(getServerList(event.getServer()))
                    .append(Component.text("\n"))
                    .append(Component.text("-".repeat(40)));

            // send to player
            event.getPlayer().sendMessage(message);
        }).start();
    }
}
