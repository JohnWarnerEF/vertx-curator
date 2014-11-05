package com.englishtown.vertx.zookeeper.promises.impl;

import com.englishtown.promises.Done;
import com.englishtown.promises.When;
import com.englishtown.promises.WhenFactory;
import com.englishtown.vertx.zookeeper.ZooKeeperClient;
import com.englishtown.vertx.zookeeper.ZooKeeperOperation;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultWhenZooKeeperClientTest {

    @Mock
    ZooKeeperClient client;

    @Mock
    CuratorFramework framework;

    @Mock
    private AsyncResult<Void> asyncResultOnReady;

    @Captor
    private ArgumentCaptor<Handler<AsyncResult<Void>>> handlerCaptorOnReady;

    @Mock
    ZooKeeperOperation operation;

    @Mock
    private AsyncResult<CuratorEvent> asyncResultExecute;

    @Captor
    private ArgumentCaptor<Handler<AsyncResult<CuratorEvent>>> handlerCaptorExecute;

    @Mock
    CuratorEvent curatorEvent;

    private Done<Void> doneOnReady = new Done<>();

    private Done<CuratorEvent> doneExecute = new Done<>();

    @Test
    public void testGetCuratorFramework() throws Exception{

        When when = WhenFactory.createSync();

        when(client.getCuratorFramework()).thenReturn(framework);

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        assertEquals(framework, target.getCuratorFramework());
    }

    @Test
    public void testInitialize() throws Exception{

        When when = WhenFactory.createSync();

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        target.initialized();

        verify(client).initialized();
    }

    @Test
    public void testOnReady_Success() throws Exception{

        When when = WhenFactory.createSync();

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        target.onReady().then(doneOnReady.onFulfilled, doneOnReady.onRejected);

        verify(client).onReady(handlerCaptorOnReady.capture());
        when(asyncResultOnReady.succeeded()).thenReturn(true);
        handlerCaptorOnReady.getValue().handle(asyncResultOnReady);

        doneOnReady.assertFulfilled();
    }

    @Test
    public void testOnReady_Failed() throws Exception{

        When when = WhenFactory.createSync();

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        target.onReady().then(doneOnReady.onFulfilled, doneOnReady.onRejected);

        verify(client).onReady(handlerCaptorOnReady.capture());
        when(asyncResultOnReady.succeeded()).thenReturn(false);
        handlerCaptorOnReady.getValue().handle(asyncResultOnReady);

        doneOnReady.assertRejected();
    }

    @Test
    public void testExecute_Success() throws Exception{

        When when = WhenFactory.createSync();

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        target.execute(operation).then(doneExecute.onFulfilled, doneExecute.onRejected);

        verify(client).execute(any(ZooKeeperOperation.class), handlerCaptorExecute.capture());
        when(asyncResultExecute.succeeded()).thenReturn(true);
        when(asyncResultExecute.result()).thenReturn(curatorEvent);
        handlerCaptorExecute.getValue().handle(asyncResultExecute);

        CuratorEvent event = doneExecute.getValue();
        assertEquals(curatorEvent, event);

        doneExecute.assertFulfilled();
    }

    @Test
    public void testOnExecuteFailed() throws Exception{

        When when = WhenFactory.createSync();

        DefaultWhenZooKeeperClient target = new DefaultWhenZooKeeperClient(client, when);

        target.execute(operation).then(doneExecute.onFulfilled, doneExecute.onRejected);

        verify(client).execute(any(ZooKeeperOperation.class), handlerCaptorExecute.capture());
        when(asyncResultExecute.succeeded()).thenReturn(false);
        handlerCaptorExecute.getValue().handle(asyncResultExecute);

        doneExecute.assertRejected();
    }
}
