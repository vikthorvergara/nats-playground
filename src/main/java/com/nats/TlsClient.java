package com.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class TlsClient {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        Options options = new Options.Builder()
                .server(natsUrl)
                .sslContext(sslContext)
                .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected to NATS with TLS");
            System.out.println("Server: " + nc.getConnectedUrl());

            String subject = "secure.message";

            for (int i = 1; i <= 5; i++) {
                String message = "Encrypted message #" + i;
                nc.publish(subject, message.getBytes());
                System.out.println("Published (encrypted): " + message);
                Thread.sleep(500);
            }

            nc.flush(java.time.Duration.ofSeconds(5));
            System.out.println("\nAll messages sent over encrypted TLS connection");
        }
    }
}
