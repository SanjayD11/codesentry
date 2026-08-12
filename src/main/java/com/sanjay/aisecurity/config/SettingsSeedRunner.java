package com.sanjay.aisecurity.config;

import com.sanjay.aisecurity.service.ApplicationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds default platform settings into the database on startup
 * if they have not been configured yet.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsSeedRunner implements ApplicationRunner {

    private final ApplicationSettingsService settingsService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding default application settings...");
        settingsService.seedDefaultSettings();
    }
}
