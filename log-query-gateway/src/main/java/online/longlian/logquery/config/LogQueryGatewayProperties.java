package online.longlian.logquery.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "log-query-gateway")
public class LogQueryGatewayProperties {

    private final VictoriaLogs victoriaLogs = new VictoriaLogs();
    private final Query query = new Query();
    private final Auth auth = new Auth();

    public VictoriaLogs getVictoriaLogs() {
        return victoriaLogs;
    }

    public Query getQuery() {
        return query;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class VictoriaLogs {
        private String queryUrl = "http://localhost:9428/select/logsql/query";
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(10);

        public String getQueryUrl() {
            return queryUrl;
        }

        public void setQueryUrl(String queryUrl) {
            this.queryUrl = queryUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Query {
        private Duration defaultRange = Duration.ofMinutes(15);
        private Duration maxRange = Duration.ofHours(24);
        private int maxLimit = 200;
        private int maxConcurrent = 8;

        public Duration getDefaultRange() {
            return defaultRange;
        }

        public void setDefaultRange(Duration defaultRange) {
            this.defaultRange = defaultRange;
        }

        public Duration getMaxRange() {
            return maxRange;
        }

        public void setMaxRange(Duration maxRange) {
            this.maxRange = maxRange;
        }

        public int getMaxLimit() {
            return maxLimit;
        }

        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }
    }

    public static class Auth {
        private boolean required = true;
        private String token = "";

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
