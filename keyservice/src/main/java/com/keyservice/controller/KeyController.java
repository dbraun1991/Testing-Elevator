package com.keyservice.controller;

import com.keyservice.service.EchoService;
import com.keyservice.service.KeyService;
import com.keyservice.service.UuidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the three service endpoints.
 *
 * <ul>
 *   <li>{@code GET /echo?msg=} — low-latency echo, used as a load-test baseline</li>
 *   <li>{@code GET /uuid}      — UUID generation with PostgreSQL persistence</li>
 *   <li>{@code GET /key?size=} — RSA key generation with measured duration</li>
 * </ul>
 *
 * <p>Invalid input is handled at the controller boundary: an
 * {@link IllegalArgumentException} from the service layer is mapped to HTTP 400.</p>
 */
@RestController
public class KeyController {

    private final EchoService echoService;
    private final UuidService uuidService;
    private final KeyService keyService;

    /**
     * Creates the controller via constructor injection.
     *
     * @param echoService service for the {@code /echo} endpoint
     * @param uuidService service for the {@code /uuid} endpoint
     * @param keyService  service for the {@code /key} endpoint
     */
    public KeyController(EchoService echoService,
                         UuidService uuidService,
                         KeyService keyService) {
        this.echoService = echoService;
        this.uuidService = uuidService;
        this.keyService  = keyService;
    }

    /**
     * Echoes the given message back to the caller, prefixed with "Hello ".
     *
     * @param msg the message to echo
     * @return HTTP 200 with the echoed message body
     */
    @GetMapping("/echo")
    public ResponseEntity<String> echo(@RequestParam String msg) {
        return ResponseEntity.ok(echoService.echo("Hello " + msg));
    }

    /**
     * Generates a random UUID, persists it to the database, and returns it.
     *
     * @return HTTP 200 with the UUID string; HTTP 500 if the database is unreachable
     */
    @GetMapping("/uuid")
    public ResponseEntity<String> uuid() {
        return ResponseEntity.ok(uuidService.generateAndPersist());
    }

    /**
     * Generates an RSA key pair of the requested size and returns a fingerprint
     * together with the generation duration.
     *
     * @param size the RSA key size in bits (512, 1024, 2048, or 4096)
     * @return HTTP 200 with a {@link com.keyservice.service.KeyService.KeyResult} JSON body,
     *         or HTTP 400 with an error message if {@code size} is not an allowed value
     */
    @GetMapping("/key")
    public ResponseEntity<?> key(@RequestParam int size) {
        try {
            return ResponseEntity.ok(keyService.generate(size));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}