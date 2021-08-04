package me.dancedog.rewardclaim.fetch;

import me.dancedog.rewardclaim.Mod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by DanceDog / Ben on 3/29/20 @ 8:38 AM
 */
public class Request {

    private static final Map<String, String> DEFAULT_HEADERS = new ConcurrentHashMap<>();

    static {
        DEFAULT_HEADERS.put("Accept", "*/*");
        DEFAULT_HEADERS.put("Content-Length", "0");
        DEFAULT_HEADERS
                .put("User-Agent", "RewardClaim" + "/" + Mod.VERSION);
    }

    private final URL url;
    private final Method method;
    private final List<String> cookies;

    public Request(URL url, Method method, @Nullable List<String> cookies) {
        this.url = url;
        this.method = method;
        this.cookies = cookies;
    }

    public Response execute() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setRequestMethod(this.method.name());
        connection.setConnectTimeout(10000);

        // Headers
        connection.setRequestProperty("Host", url.getHost());
        for (Entry<String, String> header : DEFAULT_HEADERS.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        if (this.cookies != null) {
            connection.setRequestProperty("Cookie", String.join("; ", this.cookies));
        }

        // Response
        int statusCode = connection.getResponseCode();
        List<String> responseCookies = new ArrayList<>();

        Map<String, List<String>> headerFields = connection.getHeaderFields();

        if (headerFields.containsKey("set-cookie")) {
            responseCookies.addAll(headerFields.get("set-cookie"));
        }

        if (headerFields.containsKey("Set-Cookie")) {
            responseCookies.addAll(headerFields.get("Set-Cookie"));
        }

        if (!(statusCode >= 200 && statusCode < 300)) {
            return new Response(statusCode, responseCookies, connection.getErrorStream());
        } else {
            return new Response(statusCode, responseCookies, connection.getInputStream());
        }
    }

    public enum Method {
        GET, POST
    }
}
