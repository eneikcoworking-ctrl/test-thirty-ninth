package com.eneik.generated.messaging;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class InternalMessageQueue {

    private final BlockingQueue<IncomingMessageEvent> queue = new LinkedBlockingQueue<>();

    public void publish(IncomingMessageEvent event) {
        if (event == null) {
            return;
        }
        queue.offer(event);
    }

    public IncomingMessageEvent poll() {
        return queue.poll();
    }

    public IncomingMessageEvent take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
