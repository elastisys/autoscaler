package com.elastisys.autoscaler.server.testutils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.ws.rs.client.Client;

import org.glassfish.jersey.filter.LoggingFilter;

import com.elastisys.scale.commons.net.ssl.KeyStoreType;
import com.elastisys.scale.commons.rest.client.RestClients;

/**
 * Utility class for Jersey/REST unit tests. Adds a {@link LoggingFilter} to the
 * {@link Client}s produced by the {@link RestClient} factory class.
 */
public class RestTestUtils {

    public static Client basicAuthClient(String userName, String password) {
        Client client = RestClients.httpsBasicAuth(userName, password);
        client.register(new LoggingFilter());
        return client;
    }

    public static Client noAuthClient() {
        Client client = RestClients.httpsNoAuth();
        client.register(new LoggingFilter());
        return client;
    }

    public static Client certAuthClient(String keyStorePath, String keyStorePassword, KeyStoreType keystoreType)
            throws RuntimeException {
        try (InputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            KeyStore keystore = KeyStore.getInstance(keystoreType.name());
            keystore.load(keyStoreStream, keyStorePassword.toCharArray());
            Client client = RestClients.httpsCertAuth(keystore, keyStorePassword);
            client.register(new LoggingFilter());
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
