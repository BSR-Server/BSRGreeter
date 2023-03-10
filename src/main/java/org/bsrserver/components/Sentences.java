package org.bsrserver.components;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.bsrserver.config.Config;

public class Sentences {
    private final ArrayList<String> sentences = new ArrayList<>();

    public Sentences() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(this::updateSentences, 0, 5, TimeUnit.MINUTES);
    }

    private void updateSentences() {
        sentences.clear();
        try {
            // request
            URL url = new URL(Config.getInstance().getSentencesUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());
            connection.disconnect();

            // parse
            NodeList nodes = document.getFirstChild().getChildNodes();
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
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public String getRandomSentence() {
        if (!sentences.isEmpty()) {
            return sentences.get((int) (Math.random() * sentences.size()));
        } else {
            return "";
        }
    }
}
