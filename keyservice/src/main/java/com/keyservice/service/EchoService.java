package com.keyservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service backing the {@code GET /echo} endpoint.
 *
 * <p>Intentionally lightweight — used in load tests to establish a low-latency
 * baseline that stays fast even when RSA generation saturates the thread pool.</p>
 */
@Service
public class EchoService {

    private static final Logger log = LoggerFactory.getLogger(EchoService.class);

    /**
     * Logs and returns the given message unchanged.
     *
     * @param msg the message to echo; may be any non-null string
     * @return the same {@code msg} value that was passed in
     */
    public String echo(String msg) {
        log.info("[ECHO] msg='{}'", msg);
        return msg;
    }
}
