package net.isucon;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

class SurnameIndex {
    private static Surname[] surnames;

    public static void init() throws IOException {
        if (surnames != null) {
            return;
        }
        final List<Surname> list = new ArrayList<>();
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("surname.csv")) {
            final KeyValueReader kvr = new KeyValueReader(is);
            while (kvr.next()) {
                final String yomi = kvr.get().key();
                final String name = kvr.get().value();
                list.add(new Surname(normKana(yomi), name, Normalizer.normalize(name, Normalizer.Form.NFKC)));
            }
            surnames = list.toArray(new Surname[list.size()]);
        }
    }

    public static Name[] searchName(String query) {
        if (surnames == null) {
            throw new IllegalStateException("Not initialized yet.");
        }
        final String q = normKana(query);
        final List<Name> ret = new ArrayList<>();
        for (Surname n : surnames) {
            if (n.yomi.startsWith(q) || n.normName.startsWith(q)) {
                ret.add(new Name(n.yomi, n.name));
            }
        }
        return ret.toArray(new Name[ret.size()]);
    }

    private static String normKana(String str) {
        final StringBuffer sb = new StringBuffer(Normalizer.normalize(str, Normalizer.Form.NFKC));
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c >= 'ぁ' && c <= 'ん') {
                sb.setCharAt(i, (char)(c - 'ぁ' + 'ァ'));
            }
        }
        return sb.toString();
    }

    private static final class Surname {
        private final String yomi;
        private final String name;
        private final String normName;

        public Surname(String yomi, String name, String normName) {
            this.yomi = yomi;
            this.name = name;
            this.normName = normName;
        }
    }
}
