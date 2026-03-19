package com.keyservice.controller;

import com.keyservice.service.EchoService;
import com.keyservice.service.KeyService;
import com.keyservice.service.UuidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KeyController {

    private final EchoService echoService;
    private final UuidService uuidService;
    private final KeyService keyService;

    public KeyController(EchoService echoService,
                         UuidService uuidService,
                         KeyService keyService) {
        this.echoService = echoService;
        this.uuidService = uuidService;
        this.keyService  = keyService;
    }

    @GetMapping("/echo")
    public ResponseEntity<String> echo(@RequestParam String msg) {
        return ResponseEntity.ok(echoService.echo("Hello " + msg));
    }

    @GetMapping("/uuid")
    public ResponseEntity<String> uuid() {
        return ResponseEntity.ok(uuidService.generateAndPersist());
    }

    @GetMapping("/key")
    public ResponseEntity<?> key(@RequestParam int size) {
        try {
            return ResponseEntity.ok(keyService.generate(size));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}