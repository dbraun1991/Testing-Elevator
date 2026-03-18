package com.keyservice.service;

import org.springframework.stereotype.Service;

@Service
public class EchoService {

    public String echo(String msg) {
        return msg;
    }
}