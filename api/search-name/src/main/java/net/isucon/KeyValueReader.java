package net.isucon;

import java.io.*;

class KeyValueReader {
    private final BufferedReader br;
    private KeyValue ptr;

    public KeyValueReader(InputStream is) throws UnsupportedEncodingException {
        br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    }

    public boolean next() throws IOException {
        final String l = br.readLine();
        if (l == null) {
            return false;
        }
        final String[] kv = l.split(",");
        if (kv.length != 2) {
            throw new IllegalStateException("Invalid input. [" + l + "] must contain at least and only one ','.");
        }
        this.ptr = new KeyValue(kv[0], kv[1]);
        return true;
    }

    public KeyValue get() {
        return this.ptr;
    }

    public static final class KeyValue {
        private final String key;
        private final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String key() {
            return key;
        }

        public String value() {
            return value;
        }
    }
}
