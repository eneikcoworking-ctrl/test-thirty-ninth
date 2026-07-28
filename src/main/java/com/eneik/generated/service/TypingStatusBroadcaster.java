package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TypingStatusBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(TypingStatusBroadcaster.class);

    private final List<String> broadcastLog = Collections.synchronizedList(new ArrayList<>());

    public void broadcastTyping(Long accountId, String recipient) {
        String event = "Account " + accountId + " typing to " + recipient;
        log.info("BROADCAST: {}", event);
        broadcastLog.add(event);
    }

    public List<String> getBroadcastLog() {
        return new ArrayList<>(broadcastLog);
    }

    public void clearLog() {
        broadcastLog.clear();
    }
}
