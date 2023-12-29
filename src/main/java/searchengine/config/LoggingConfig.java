package searchengine.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.context.annotation.Configuration;
import searchengine.Application;

@Configuration
public class LoggingConfig {

    public static final Logger LOGGER = LogManager.getLogger(Application.class);
    public static final Marker MARKER = MarkerManager.getMarker("ApplicationInfo");
}
