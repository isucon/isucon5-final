package net.isucon;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

class NameIndex {
    private final NormalizedName[] names;

    public NameIndex(String resource) throws IOException {
        final List<NormalizedName> list = new ArrayList<>();
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
            final KeyValueReader kvr = new KeyValueReader(is);
            while (kvr.next()) {
                final String yomi = kvr.get().key();
                final String name = kvr.get().value();
                list.add(new NormalizedName(normKana(yomi), name, Normalizer.normalize(name, Normalizer.Form.NFKC)));
            }
            names = list.toArray(new NormalizedName[list.size()]);
        }
    }

    public Name[] searchName(String query, int maxNum) {
        final String q = normKana(query);
        final List<Name> ret = new ArrayList<>();
        int i = 0;
        for (NormalizedName n : names) {
            if (n.yomi.startsWith(q) || n.normName.startsWith(q)) {
                ret.add(new Name(n.yomi, n.name));
                i++;
                if (i >= maxNum) {
                    break;
                }
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

    private static final class NormalizedName {
        private final String yomi;
        private final String name;
        private final String normName;

        public NormalizedName(String yomi, String name, String normName) {
            this.yomi = yomi;
            this.name = name;
            this.normName = normName;
        }
    }
}
