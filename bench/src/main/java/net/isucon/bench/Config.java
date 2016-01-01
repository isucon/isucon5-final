package net.isucon.bench;

public class Config {
    // TODO: variable GET/POST timeouts per requests
    public static final long GET_TIMEOUT = 30 * 1000;
    public static final long POST_TIMEOUT = 30 * 1000;

    public static final String DEFAULT_USER_AGENT = "Isucon bench";

    public String scheme;
    public String host;
    public int port;
    public String agent;

    public Config() {
        this.scheme = "http";
        this.host = null;
        this.port = 0;
        this.agent = DEFAULT_USER_AGENT;
    }

    public String uri(String path) {
        if (port == 0) {
            return uriDefaultPort(path);
        } else {
            return String.format("%s://%s:%d%s", scheme, host, port, path);
        }
    }

    public String uriDefaultPort(String path) {
        return String.format("%s://%s%s", scheme, host, path);
    }
}
