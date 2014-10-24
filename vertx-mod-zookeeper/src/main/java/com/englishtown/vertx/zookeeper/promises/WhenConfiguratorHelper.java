package com.englishtown.vertx.zookeeper.promises;

import com.englishtown.promises.Promise;
import com.englishtown.vertx.zookeeper.ConfigElement;
import org.apache.curator.framework.api.CuratorWatcher;

/**
 * Promises version of {@link com.englishtown.vertx.zookeeper.ConfiguratorHelper}
 */
public interface WhenConfiguratorHelper {

    Promise<ConfigElement> getConfigElement(String path);

    Promise<ConfigElement> getConfigElement(String path, CuratorWatcher watcher);

    Promise<ConfigElement> getConfigElementChildren(String path);

    Promise<ConfigElement> getConfigElementChildren(String path, CuratorWatcher watcher);

}
