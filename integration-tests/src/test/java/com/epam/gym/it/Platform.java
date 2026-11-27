package com.epam.gym.it;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * The whole platform from compose.yml, run with the Docker Compose CLI for the test run:
 * <ul>
 *     <li>images are built from the current sources;</li>
 *     <li>every secret is generated for the run and host ports are free ones picked at random;</li>
 *     <li>its own project name plus compose.it.yml (own container, network and volume names) keep it apart from a
 *     development stack, which is never touched;</li>
 *     <li>{@code up --wait} returns only when every container's health check passes.</li>
 * </ul>
 */
public final class Platform {

    public static final String GYM_CRM = "gym-crm";
    public static final String WORKLOAD = "trainer-workload-service";
    public static final String BROKER = "activemq";

    private static final String PROJECT = "gym-crm-it";
    private static final String STARTUP_TIMEOUT_SECONDS = "600";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<String, String> ENV = new HashMap<>();
    private static boolean started;
    private static String serviceSecret;
    private static int gymCrmPort;
    private static int workloadPort;
    private static int eurekaPort;

    private Platform() {
    }

    static synchronized void start() {
        if (started) {
            return;
        }
        serviceSecret = randomBase64Key();
        gymCrmPort = freePort();
        workloadPort = freePort();
        eurekaPort = freePort();

        ENV.put("DB_NAME", "gym_crm");
        ENV.put("DB_USERNAME", "gym_it");
        ENV.put("DB_PASSWORD", randomHex());
        ENV.put("JWT_SECRET", randomBase64Key());
        ENV.put("SERVICE_JWT_SECRET", serviceSecret);
        ENV.put("ACTIVEMQ_USER", "gym_it");
        ENV.put("ACTIVEMQ_PASSWORD", randomHex());
        ENV.put("MONGO_USERNAME", "workload_it");
        ENV.put("MONGO_PASSWORD", randomHex());
        ENV.put("APP_PORT", String.valueOf(gymCrmPort));
        ENV.put("WORKLOAD_PORT", String.valueOf(workloadPort));
        ENV.put("EUREKA_PORT", String.valueOf(eurekaPort));
        ENV.put("ACTIVEMQ_CONSOLE_PORT", String.valueOf(freePort()));

        // Leftovers of an interrupted earlier run would clash with the fixed container names
        compose("down", "--volumes", "--remove-orphans");
        compose("up", "--detach", "--build", "--wait", "--wait-timeout", STARTUP_TIMEOUT_SECONDS);
        started = true;
    }

    static synchronized void stop() {
        if (started) {
            compose("down", "--volumes", "--remove-orphans");
            started = false;
        }
    }

    public static String gymCrmUrl() {
        return "http://localhost:" + gymCrmPort;
    }

    public static String workloadUrl() {
        return "http://localhost:" + workloadPort;
    }

    public static String eurekaUrl() {
        return "http://localhost:" + eurekaPort;
    }

    /**
     * A service token like the ones Gym CRM signs, for reading the Trainer Workload API.
     */
    public static String serviceToken() {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder()
                .issuer("gym-crm")
                .subject("gym-crm")
                .audience().add("trainer-workload-service").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(5))))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(serviceSecret)), Jwts.SIG.HS256)
                .compact();
    }

    public static String logsOf(String service) {
        return compose("logs", "--no-color", service);
    }

    public static void stopService(String service) {
        compose("stop", service);
    }

    /**
     * Starts a stopped service again and waits until its health check passes.
     */
    public static void startService(String service) {
        compose("up", "--detach", "--no-deps", "--wait", "--wait-timeout", "180", service);
    }

    private static String compose(String... args) {
        List<String> command = new ArrayList<>(List.of("docker", "compose", "--project-name", PROJECT,
                "--file", "../compose.yml", "--file", "compose.it.yml"));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).directory(new File(".")).redirectErrorStream(true);
        builder.environment().putAll(ENV);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IllegalStateException("docker compose " + String.join(" ", args) + " failed:\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String randomBase64Key() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private static String randomHex() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
