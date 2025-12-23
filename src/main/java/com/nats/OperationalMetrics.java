package com.nats;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OperationalMetrics {
    public static void main(String[] args) throws Exception {
        String monitorUrl = System.getenv().getOrDefault("NATS_MONITOR_URL", "http://localhost:8222/varz");

        URL url = new URL(monitorUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String json = response.toString();

        System.out.println("NATS Server Metrics:");
        System.out.println("  Memory: " + extractValue(json, "mem") + " bytes");
        System.out.println("  Uptime: " + extractValue(json, "uptime"));
        System.out.println("  Connections: " + extractValue(json, "connections"));
        System.out.println("  Messages In: " + extractValue(json, "in_msgs"));
        System.out.println("  Messages Out: " + extractValue(json, "out_msgs"));
        System.out.println("  Bytes In: " + extractValue(json, "in_bytes"));
        System.out.println("  Bytes Out: " + extractValue(json, "out_bytes"));
        System.out.println("  CPU: " + extractValue(json, "cpu") + "%");
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "N/A";

        start += search.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != '"') {
            end++;
        }

        return json.substring(start, end);
    }
}
