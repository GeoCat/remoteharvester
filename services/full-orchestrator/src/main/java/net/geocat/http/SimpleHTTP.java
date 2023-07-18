package net.geocat.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import sun.misc.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

@Component
@Scope("prototype")
public class SimpleHTTP {


    Logger logger = LoggerFactory.getLogger(SimpleHTTP.class);
    int TIMEOUT_MS = 20 * 1000;


    public String sendJSON(String verb, String location, String json) throws Exception {
        if (json == null)
            json = "";
        URL url = new URL(location);
        if (!url.getProtocol().equalsIgnoreCase("http") && (!url.getProtocol().equalsIgnoreCase("https")))
            throw new SecurityException("Security violation - url should be HTTP or HTTPS");

        if (!verb.equals("POST") && !verb.equals("GET"))
            throw new SecurityException("verb should be 'POST' or 'GET'");

        logger.debug("      * " + verb + " to " + location + " with body " + json.replace("\n", ""));

        boolean isHTTPs = url.getProtocol().equalsIgnoreCase("HTTPS");

        byte[] body_bytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] response_bytes;


        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;

        http.setConnectTimeout(TIMEOUT_MS);
        http.setReadTimeout(TIMEOUT_MS);
        http.setRequestMethod(verb);
        http.setDoOutput(true);
        http.setDoInput(true);
        if (verb.equals("POST")) {
            http.setFixedLengthStreamingMode(body_bytes.length);
            // http.setRequestProperty("Content-Type", "application/xml");
        }
        http.setRequestProperty("Content-Type", "application/xml");
        http.setRequestProperty("Accept", "application/xml");


        String response;

        http.connect();

        http.connect();
        try {
            // send body
            if (body_bytes.length > 0) {
                try (OutputStream os = http.getOutputStream()) {
                    os.write(body_bytes);
                }
            }
            // get response
            try (InputStream is = http.getInputStream()) {
                response_bytes = IOUtils.readAllBytes(is);
                response = new String(response_bytes, StandardCharsets.UTF_8);
            }
        } finally {
            http.disconnect();
        }

        return response;
    }
}
