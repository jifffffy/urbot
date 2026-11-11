package com.embabel.urbot;

import org.drivine.autoconfigure.EnableDrivine;
import org.drivine.autoconfigure.EnableDrivinePropertiesConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static org.springframework.context.annotation.FilterType.REGEX;

/**
 * Test boot class that mirrors {@link UrbotApplication} but excludes security packages.
 * Uses individual annotations instead of {@code @SpringBootApplication} to avoid a
 * double component scan that would re-discover the excluded classes.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableDrivine
@EnableDrivinePropertiesConfig
@ComponentScan(
        basePackages = "com.embabel.urbot",
        excludeFilters = {
                @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = UrbotApplication.class),
                @ComponentScan.Filter(type = REGEX, pattern = "com\\.embabel\\.urbot\\.security\\..*")
        }
)
class TestUrbotApplication {
}
