package com.urlcustomdiscs;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

// fork: the disc bot does all downloading/converting/pack building (ADR 0006). This only talks to it.
// Call from an async task: requests block until the bot finishes.
public final class BotClient {
    private final String baseUrl;
    private final String secret;

    public BotClient(String baseUrl, String secret) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.secret = secret;
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty() && !secret.isEmpty();
    }

    /** POSTs json to path and hands each JSON line of the (streamed) reply to onEvent. */
    public void post(String path, JSONObject json, Consumer<JSONObject> onEvent) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(5000);
        c.setReadTimeout(15 * 60 * 1000); // a long download streams progress, so this only trips if the bot hangs
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + secret);
        try (OutputStream out = c.getOutputStream()) {
            out.write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        if (code == 401) throw new IOException("the disc bot rejected botSecret (check config.yml vs DISC_API_SECRET)");
        if (code != 200) throw new IOException("the disc bot answered HTTP " + code + " for " + path);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (!line.isBlank()) onEvent.accept(new JSONObject(line));
            }
        }
    }
}
