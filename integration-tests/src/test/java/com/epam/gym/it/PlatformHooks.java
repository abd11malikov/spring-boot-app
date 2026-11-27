package com.epam.gym.it;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

/**
 * Starts the platform before the first scenario and removes it (containers, network, volumes) after the last.
 */
public class PlatformHooks {

    @BeforeAll
    public static void startPlatform() {
        Platform.start();
    }

    @AfterAll
    public static void stopPlatform() {
        Platform.stop();
    }
}
