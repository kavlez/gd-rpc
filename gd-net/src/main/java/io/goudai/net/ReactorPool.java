package io.goudai.net;

import io.goudai.common.Lifecycle;
import io.goudai.session.AbstractSession;
import io.goudai.session.factory.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by freeman on 2016/1/8.
 */
public class ReactorPool implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(ReactorPool.class);

    private final Reactor[] reactors;
    private final int reactorCount;
    private AtomicInteger selectorIndex = new AtomicInteger();

    public ReactorPool(int reactorCount,SessionFactory sessionFactory) throws IOException {

        if (reactorCount <= 0) {
            reactorCount = 1;
            logger.warn("reactor less than 1,using auto reactors 1");
        }
        this.reactors = new Reactor[reactorCount];
        for (int i = 0; i < this.reactors.length; i++) {
            this.reactors[i] = new Reactor("reactor--" + i,sessionFactory);
        }
        this.reactorCount = reactorCount;
    }

    @Override
    public void startup() throws Exception {
        for (Lifecycle life : this.reactors) {
            life.startup();
        }
    }

    @Override
    public void shutdown() throws Exception {
        for (Lifecycle life : this.reactors) {
            life.shutdown();
        }
    }

    public void register(SocketChannel socketChannel) throws ClosedChannelException {
        this.register(socketChannel,null);
    }

    public void register(SocketChannel socketChannel, AbstractSession session) throws ClosedChannelException {
        this.nextReactor().register(socketChannel, SelectionKey.OP_READ,session);
    }



    private Reactor nextReactor() {
        selectorIndex.compareAndSet(Integer.MAX_VALUE, 0);
        return this.reactors[this.selectorIndex.getAndIncrement() % this.reactorCount];
    }

}
