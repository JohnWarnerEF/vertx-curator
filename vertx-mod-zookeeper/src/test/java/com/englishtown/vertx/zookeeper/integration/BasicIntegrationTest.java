package com.englishtown.vertx.zookeeper.integration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.junit.Test;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.VertxAssert;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class BasicIntegrationTest extends AbstractIntegrationTest {

    private CuratorFramework curatorFramework;

    @Override
    protected JsonObject createZooKeeperConfig() {
        JsonObject json = super.createZooKeeperConfig();

        return json
                .putObject("auth", new JsonObject()
                        .putString("scheme", "digest")
                        .putString("username", "test_user")
                        .putString("password", "test_user_password"))
                .putArray("path-prefixes", new JsonArray()
                        .addString("/test/env/dev/application")
                        .addString("/test/env/dev")
                        .addString("/test/global"));
    }

    @Override
    protected void setup() throws Exception {
        super.setup();
        curatorFramework = zookeeperClient.getCuratorFramework();

        List<ACL> acls = new ArrayList<>();
        acls.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));

        curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/test/global/cassandra/seeds", "10.0.0.1,10.0.0.2".getBytes());
        curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/test/env/dev/cassandra/seeds", "192.168.0.1,192.168.0.2".getBytes());
        curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).withACL(acls).forPath("/test/env/dev/application/cassandra/seeds", "0.0.0.0".getBytes());
    }

    @Override
    public void stop() {
//        curatorFramework.close();
    }

    @Test
    public void testReadingTheApplicationConfigValue() throws Exception {

        // First time we try and get the seeds variable, it should return 0.0.0.0
        configuratorHelper.getConfigElement("/cassandra/seeds").then(
                element -> {
                    VertxAssert.assertNotNull(element);
                    VertxAssert.assertEquals("0.0.0.0", element.asString());

                    // Assuming that is true then wipe out the application one and try again
                    try {
                        curatorFramework.delete().forPath("/test/env/dev/application/cassandra/seeds");
                        return configuratorHelper.getConfigElement("/cassandra/seeds");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        ).then(
                element -> {
                    VertxAssert.assertNotNull(element);
                    VertxAssert.assertEquals("192.168.0.1,192.168.0.2", element.asString());

                    // Now wipe out the environment znode and go again.
                    try {
                        curatorFramework.delete().forPath("/test/env/dev/cassandra/seeds");
                        return configuratorHelper.getConfigElement("/cassandra/seeds");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        ).then(
                element -> {
                    VertxAssert.assertNotNull(element);
                    VertxAssert.assertEquals("10.0.0.1,10.0.0.2", element.asString());

                    VertxAssert.testComplete();

                    return null;
                }
        ).otherwise(
                t -> {
                    VertxAssert.handleThrowable(t);
                    VertxAssert.fail();

                    return null;
                }
        );
    }

}
