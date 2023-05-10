package searchengine.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jsoup-settings")
public class JsoupConfig {

    private String useragent;

    private String referrer;
}
