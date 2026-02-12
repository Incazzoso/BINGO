import java.util.Random;

public class BingoTicket {
    private int[][] slots;
    private final static int ROWS = 3;
    private final static int COLUMNS = 9;
    private final static int NUMBERS_PER_ROW = 5;
    private Random rand = new Random();
    private boolean cinquinaFatta = false;

    public BingoTicket() {
        slots = new int[ROWS][COLUMNS];
        generate();
    }

    private void generate() {
        for (int i = 0; i < ROWS; i++) {
            int numbersPlaced = 0;
            while (numbersPlaced < NUMBERS_PER_ROW) {
                int col = rand.nextInt(COLUMNS);
                if (slots[i][col] == 0) {
                    int rangeMin = (col * 10) + 1;
                    int rangeMax = (col == 8) ? 91 : (col + 1) * 10;
                    int number = rand.nextInt(rangeMin, rangeMax);
                    if (!isDuplicateInColumn(col, number)) {
                        slots[i][col] = number;
                        numbersPlaced++;
                    }
                }
            }
        }
        sortColumns();
    }

    private boolean isDuplicateInColumn(int col, int number) {
        for (int i = 0; i < ROWS; i++) {
            if (slots[i][col] == number) return true;
        }
        return false;
    }

    private void sortColumns() {
        for (int j = 0; j < COLUMNS; j++) {
            for (int i = 0; i < ROWS - 1; i++) {
                for (int k = i + 1; k < ROWS; k++) {
                    if (slots[i][j] != 0 && slots[k][j] != 0 && slots[i][j] > slots[k][j]) {
                        int temp = slots[i][j];
                        slots[i][j] = slots[k][j];
                        slots[k][j] = temp;
                    }
                }
            }
        }
    }

    public void eliminateNumber(int number) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                if (slots[i][j] == number) {
                    slots[i][j] = -1;
                    return;
                }
            }
        }
    }

    public boolean checkCinquina() {
        if (cinquinaFatta) return false;
        for (int i = 0; i < ROWS; i++) {
            int count = 0;
            for (int j = 0; j < COLUMNS; j++) {
                if (slots[i][j] == -1) count++;
            }
            if (count == NUMBERS_PER_ROW) {
                cinquinaFatta = true;
                return true;
            }
        }
        return false;
    }

    public boolean checkBingo() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                if (slots[i][j] > 0) return false;
            }
        }
        return true;
    }

    public int getSlotValue(int row, int col) { return slots[row][col]; }
}