package com.englishtown.vertx.zookeeper.impl;

import com.englishtown.vertx.zookeeper.ZooKeeperClient;
import com.englishtown.vertx.zookeeper.ZooKeeperConfigurator;
import com.englishtown.vertx.zookeeper.ZooKeeperOperation;
import io.vertx.core.*;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorWatcher;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import static org.apache.curator.framework.CuratorFrameworkFactory.builder;

/**
 */
public class DefaultZooKeeperClient implements ZooKeeperClient {

    private final Vertx vertx;
    private final List<Handler<AsyncResult<Void>>> onReadyCallbacks = new ArrayList<>();

    private CuratorFramework framework;
    private AsyncResult<Void> initResult;

    private static final Logger logger = LoggerFactory.getLogger(DefaultZooKeeperClient.class);

    @Inject
    public DefaultZooKeeperClient(Vertx vertx, ZooKeeperConfigurator configurator) {
        this.vertx = vertx;

        configurator.onReady(result -> {
            if (result.failed()) {
                runOnReadyCallbacks(result);
                return;
            }
            init(configurator);
        });
    }

    private DefaultZooKeeperClient(Vertx vertx, CuratorFramework framework, AsyncResult<Void> initResult) {
        this.vertx = vertx;
        this.framework = framework;
        this.initResult = initResult;
    }

    private void init(ZooKeeperConfigurator configurator) {

        Builder builder = builder().retryPolicy(configurator.getRetryPolicy());

        ZooKeeperConfigurator.AuthPolicy auth = configurator.getAuthPolicy();
        if (auth != null) {
            builder.authorization(auth.geScheme(), auth.getAuth().getBytes());
        }

        EnsembleProvider ensembleProvider = configurator.getEnsembleProvider();
        if (ensembleProvider != null) {
            builder.ensembleProvider(ensembleProvider);
        } else {
            builder.connectString(configurator.getConnectionString());
        }

        framework = builder.build();
        framework.start();

        runOnReadyCallbacks(Future.succeededFuture(null));

    }

    private void runOnReadyCallbacks(AsyncResult<Void> result) {
        initResult = result;
        onReadyCallbacks.forEach(handler -> handler.handle(result));
        onReadyCallbacks.clear();
    }

    @Override
    public CuratorFramework getCuratorFramework() {
        return framework;
    }

    @Override
    public void execute(ZooKeeperOperation operation, Handler<AsyncResult<CuratorEvent>> handler) {
        try {
            operation.execute(this, wrapHandler(handler));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    @Override
    public ZooKeeperClient usingNamespace(String namespace) {
        if (!initialized() || framework == null) {
            throw new IllegalStateException("Cannot call usingNamespace() until after onReady() successfully completes");
        }
        return new DefaultZooKeeperClient(vertx, framework.usingNamespace(namespace), initResult);
    }

    @Override
    public boolean initialized() {
        return initResult != null;
    }

    @Override
    public void onReady(Handler<AsyncResult<Void>> callback) {
        if (initResult != null) {
            callback.handle(initResult);
        } else {
            onReadyCallbacks.add(callback);
        }
    }

    @Override
    public CuratorWatcher wrapWatcher(CuratorWatcher watcher) {
        Context context = vertx.getOrCreateContext();

        return event -> {
            context.runOnContext(aVoid -> {
                try {
                    watcher.process(event);
                } catch (Exception e) {
                    logger.warn("CuratorWatcher threw an exception", e);
                }
            });
        };
    }

    private Handler<AsyncResult<CuratorEvent>> wrapHandler(Handler<AsyncResult<CuratorEvent>> toWrap) {
        Context context = vertx.getOrCreateContext();

        if (context == null) {
            logger.warn("Current vertx context is null, are you running on the correct thread?");
            return toWrap;
        }

        return (result) -> {
            context.runOnContext(aVoid -> {
                toWrap.handle(result);
            });
        };
    }

    @Override
    public void close() {
        getCuratorFramework().close();
    }
}
