package net.isucon;

class Name {
    private final String name;
    private final String yomi;

    public Name(String name, String yomi) {
        this.name = name;
        this.yomi = yomi;
    }

    public String getName() {
        return name;
    }

    public String getYomi() {
        return yomi;
    }
}
