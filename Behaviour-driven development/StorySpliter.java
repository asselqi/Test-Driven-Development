package Solution;



public class StorySpliter {
    private String regex;//regex = "\n"

    public StorySpliter(String regex) {
        this.regex = regex;
    }

    public String[] splitFunction(String story) {
        String[] sentences = story.split(regex);
        return sentences;
    }
}
