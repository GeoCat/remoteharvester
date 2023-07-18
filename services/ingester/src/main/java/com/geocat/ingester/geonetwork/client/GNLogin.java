package com.geocat.ingester.geonetwork.client;

import com.geocat.ingester.exception.GeoNetworkClientException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
//@Slf4j(topic = "com.geocat.ingester.geonetwork.client")
public class GNLogin {
    Logger log = LoggerFactory.getLogger(GNLogin.class);

    @Value("${geonetwork.baseUrl}")
    protected String baseUrl;

    @Value("${geonetwork.username}")
    protected String username;

    @Value("${geonetwork.password}")
    protected String password;


    protected String getLoginService() {
        return baseUrl + "/signin";
    }

    protected void setupRequestHttpEntity(HttpPost request, HttpClientContext clientContext,
                                          String username, String password, GNConnection connection) throws UnsupportedEncodingException {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("username", username));
        nvps.add(new BasicNameValuePair("password", password));

        Cookie csrfTokenCookie = getCsrfTokenCookie(username, password, clientContext);
        if (csrfTokenCookie != null) {
            String csrf = csrfTokenCookie.getValue();
            nvps.add(new BasicNameValuePair("_csrf", csrf));

            request.addHeader("X-XSRF-TOKEN", csrf);
            connection.setCrsfTokenCookie(csrfTokenCookie);
        }

        request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    }


    private Cookie getCsrfTokenCookie(String username, String password, HttpClientContext clientContext) {
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();

        CookieStore cookieStore = new BasicCookieStore();

        //HttpClientContext clientContext = HttpClientContext.create();
        clientContext.setCookieStore(cookieStore);

        HttpPost httpost = new HttpPost(baseUrl + "/info?type=me");

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(username, password);

        final URI uri = httpost.getURI();
        HttpHost hh = new HttpHost(
                uri.getHost(),
                uri.getPort(),
                uri.getScheme());
        credentialsProvider.setCredentials(new AuthScope(hh), credentials);

        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(hh, basicAuth);


        // Add AuthCache to the execution context
        clientContext.setCredentialsProvider(credentialsProvider);
        clientContext.setAuthCache(authCache);


        CloseableHttpResponse response = null;
        Cookie csrfTokenCookie = null;

        try {
            response = closeableHttpClient.execute(httpost, clientContext);

            for (Cookie cookie : clientContext.getCookieStore().getCookies()) {
                if (cookie.getName().equalsIgnoreCase("XSRF-TOKEN")) {
                    csrfTokenCookie = clientContext.getCookieStore().getCookies().get(0);
                    break;
                }
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(closeableHttpClient);
        }

        return csrfTokenCookie;
    }

    public void login(GNConnection connection) throws GeoNetworkClientException {

        CloseableHttpClient closeableHttpClient = connection.getCloseableHttpClient();
        Cookie jsessionidCookie = null;

        String gnLoginUrl = getLoginService();

        if (!gnLoginUrl.isEmpty()) {
            CookieStore cookieStore = new BasicCookieStore();

            HttpClientContext clientContext = HttpClientContext.create();
            clientContext.setCookieStore(cookieStore);

            if (closeableHttpClient == null) {
                closeableHttpClient = HttpClients.createDefault();
            }

            HttpPost httpost = new HttpPost(gnLoginUrl);
            CloseableHttpResponse response = null;
            try {
                setupRequestHttpEntity(httpost, clientContext, username, password, connection);

                response = closeableHttpClient.execute(httpost, clientContext);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                httpost.releaseConnection();

                jsessionidCookie = clientContext.getCookieStore().getCookies().get(0);
                log.debug("Authentication session cookie: " + jsessionidCookie.getName()
                        + " = " + jsessionidCookie.getValue()); // should print JSESSIONID =
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                httpost.releaseConnection();

                jsessionidCookie = clientContext.getCookieStore().getCookies().get(0);
                log.debug("Authentication session cookie: " + jsessionidCookie.getName()
                        + " = " + jsessionidCookie.getValue()); // should print JSESSIONID =
            } else {
                httpost.releaseConnection();
                log.warn("User " + username + " not able to login to Geonetwork");
                log.warn("response code from login url is " + response.getStatusLine().getStatusCode());
            }
        } else {
        }

        if (jsessionidCookie != null) {
            connection.setJsessionidCookie(jsessionidCookie);
        } else {
            throw new GeoNetworkClientException("Error login in Geonetwork");
        }
    }
}
