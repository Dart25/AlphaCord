package me.dartn.alphacord;

public class CharReplacement {
    private final String from;
    private final String to;

    public CharReplacement(char from, String to) {
        this.from = Character.toString(from);
        this.to = to;
    }

    public CharReplacement(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String process(String str) {
        return str.replace(from, to);
    }

    public static CharReplacement rankPrefix(char icon, char to){
        return new CharReplacement("<" + icon + ">", "<" + to + ">");
    }
}
