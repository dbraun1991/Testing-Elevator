package com.keyservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EchoService {

    private static final Logger log = LoggerFactory.getLogger(EchoService.class);

    public String echo(String msg) {
        log.info("[ECHO] msg='{}'", msg);
        return msg;
    }
}
