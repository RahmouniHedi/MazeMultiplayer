package common;

import java.io.Serializable;

// C'est un "Type Complexe" sérialisé pour RMI
public class MazeState implements Serializable {
    private static final long serialVersionUID = 1L;

    private int[][] grid;
    private int size;
    private String difficultyName;
    private long serverStartTime;

    public MazeState(int[][] grid, int size, String difficultyName) {
        this.grid = grid;
        this.size = size;
        this.difficultyName = difficultyName;
        this.serverStartTime = System.currentTimeMillis();
    }

    public int[][] getGrid() { return grid; }
    public int getSize() { return size; }
    public String getDifficultyName() { return difficultyName; }
}