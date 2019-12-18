package Solution;

import Provided.StoryTestException;
import java.util.ArrayList;
import java.util.List;

public class StoryTestExceptionImpl extends StoryTestException {
    private String failed_then = "";
    private List<String> expected = new ArrayList<>();
    private List<String> actual = new ArrayList<>();
    private int num_of_failed_then;

    @Override
    public String getSentance() {
        return failed_then;
    }

    public void setFailed_then(String str) {
        failed_then = str;
    }

    public void setExpected(List<String> expected_temp) {
        expected = new ArrayList<String>(expected_temp);
    }

    @Override
    public List<String> getStoryExpected() {
        return expected;
    }

    public void setActual(List<String> actual_temp) { actual = new ArrayList<String>(actual_temp); }

    @Override
    public List<String> getTestResult() {
        return actual;
    }
    public void setFailNum(int num) {
        num_of_failed_then = num;
    }

    @Override
    public int getNumFail() {
        return num_of_failed_then;
    }
}
