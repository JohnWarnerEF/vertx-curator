package com.englishtown.vertx.zookeeper;

import org.apache.curator.RetryPolicy;
import org.vertx.java.core.Handler;

import java.util.List;

/**
 * ZooKeeper configuration
 */
public interface ZooKeeperConfigurator {

    /**
     * Standard zookeeper connection string
     *
     * @return connection string
     */
    String getConnectionString();

    /**
     * The curator retry policy to use
     *
     * @return retry policy
     */
    RetryPolicy getRetryPolicy();

    /**
     * Optional authorization policy (digest, ip, etc.)
     *
     * @return auth policy
     */
    AuthPolicy getAuthPolicy();

    /**
     * Optional path prefixes used when getting data with the {@link com.englishtown.vertx.zookeeper.promises.WhenConfiguratorHelper}
     *
     * @return
     */
    List<String> getPathPrefixes();

    void onReady(Handler<Void> callback);

    public interface AuthPolicy {

        String geScheme();

        String getAuth();

    }
}
