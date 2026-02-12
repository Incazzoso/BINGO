import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BingoNumberGenerator {
    private List<Integer> numbers;
    private int currentIndex;
    public static final int MAX = 90;

    public BingoNumberGenerator() {
        numbers = new ArrayList<>();
        for (int i = 1; i <= MAX; i++) {
            numbers.add(i);
        }
        reset();
    }

    public void reset() {
        Collections.shuffle(numbers);
        currentIndex = 0;
    }

    public int nextInt() {
        if (currentIndex >= MAX) {
            return 0;
        }
        return numbers.get(currentIndex++);
    }
}