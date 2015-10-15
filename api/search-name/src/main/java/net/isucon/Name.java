package net.isucon;

class Name {
    private final String yomi;
    private final String name;

    public Name(String yomi, String name) {
        this.yomi = yomi;
        this.name = name;
    }

    public String getYomi() {
        return yomi;
    }

    public String getName() {
        return name;
    }
}
