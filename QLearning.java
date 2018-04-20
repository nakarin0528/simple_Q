import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Random;

import jdk.nashorn.internal.ir.ReturnNode;

public class QLearning {
    public static final double EPSILON = 0.3;
    public static final double ALPHA = 0.1;
    public static final double GAMMA = 0.9;
    public static final int GOAL_REWARD = 100;
    public static final int HIT_WALL_PENALTY = 5;
    public static final int ONE_STEP_PENALTY = 1;
    public static final int LEARNING_TIMES = 1000000;
    public static final int INIT_Q_MAX = 30;

    public static final int MAZE[][] = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 1, 1, 1, 1, 1, 0},
        {0, 1, 1, 1, 0, 1, 1, 0, 1, 0},
        {0, 0, 0, 1, 0, 1, 0, 0, 1, 0},
        {0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
        {0, 1, 0, 1, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 1, 1, 1, 0},
        {0, 1, 0, 0, 0, 1, 0, 0, 1, 0},
        {0, 1, 1, 1, 1, 1, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    };

    public double q[][][];

    private int posX;
    private int posY;
    private int observedPosX;
    private int observedPosY;

	public static final int LEFT  = 0;
	public static final int UP    = 1;
	public static final int RIGHT = 2;
	public static final int DOWN  = 3;

    private int stepNum;

    private ArrayList<String> route;

    QLearning() {
        q = new double[10][10][4];

        posX = 1;
        posY = 1;
        observedPosX = 1;
        observedPosY = 1;

        route = new ArrayList<String>();
    }

    public static void main(String[] args) {
        int minStepNum = Integer.MAX_VALUE;

        QLearning learn = new QLearning();
        learn.initQ();

        for (int i=0; i<LEARNING_TIMES; i++) {
            learn.initAgent();
            learn.route = new ArrayList<String>();

            boolean isGoal = false;
            while (!isGoal) {
                learn.stepNum++;

                int action = learn.selectByEGreedy();
                int reward = learn.reward(action);
                reward -= ONE_STEP_PENALTY;
                learn.updeteQ(reward, action);
                learn.updateState();

                isGoal = (learn.posX == 8 && learn.posY == 8);
            }

            if (learn.stepNum < minStepNum) {
                minStepNum = learn.stepNum;
            }
            // shortest path
            if (learn.stepNum == 22) {
                minStepNum = learn.stepNum;

                learn.printQ();
                for (int j=0; j<learn.route.size(); j++) {
                    System.out.println(learn.route.get(j));
                }
                System.out.println("LEARNING TIMES：" + i + "　STEP FOR GOAL:" + learn.stepNum + "　SHORTEST STEP:" + minStepNum);
                break;
            }
            System.out.println("LEARNING_TIMES：" + i + "　STEP FOR GOAL:" + learn.stepNum + "　SHORTEST STEP:" + minStepNum);
        }
    }
    
    public void initQ() {
        // 0~INIT_Q_MAX
        Random rand = new Random();
        for (int x=0; x<10; x++) {
            for (int y=0; y<10; y++) {
                for (int a=0; a<4; a++) {
                    int randNum = rand.nextInt(INIT_Q_MAX+1);
                    q[x][y][a] = randNum;
                }
            }
        }
    }

    public void initAgent() {
        posX = 1;
        posY = 1;
        stepNum = 0;
    }

    public int selectByEGreedy() {
        int selectedA = 0;
        Random rand = new Random();
        int randNum = rand.nextInt(100+1);

        // epsilon probability
        if (randNum <= EPSILON*100.0) {
            for (int a=0; a<4; a++) {
                boolean isLearger = q[posX][posY][selectedA] < q[posX][posY][a];
                if (isLearger) {
                    selectedA = a;
                }
            }
        } else {
            selectedA = rand.nextInt(4);
        }

        return selectedA;
    }

    public int reward(int action) {
        int reward = 0;

        observedPosX = posX;
        observedPosY = posY;
        boolean canMove;

        switch (action) {

            case LEFT:
                canMove = (MAZE[posY][posX-1] == 1);
                if (canMove) {
                    observedPosX--;
                } else {
                    reward -= HIT_WALL_PENALTY;
                }
                route.add("←" + "[" + String.valueOf(observedPosX) + "][" + String.valueOf(observedPosY) + "]");
                break;
            case UP:
                canMove = (MAZE[posY-1][posX] == 1);
                if (canMove) {
                    observedPosY--;
                } else {
                    reward -= HIT_WALL_PENALTY;
                }
                route.add("↑" + "[" + String.valueOf(observedPosX) + "][" + String.valueOf(observedPosY) + "]");
                break;
            case RIGHT:
                canMove = (MAZE[posY][posX + 1] == 1);
                if (canMove) {
                    observedPosX++;
                } else {
                    reward -= HIT_WALL_PENALTY;
                }
                route.add("→" + "[" + String.valueOf(observedPosX) + "][" + String.valueOf(observedPosY) + "]");
                break;
            case DOWN:
                canMove = (MAZE[posY+1][posX] == 1);
                if (canMove) {
                    observedPosY++;
                } else {
                    reward -= HIT_WALL_PENALTY;
                }
                route.add("↓" + "[" + String.valueOf(observedPosX) + "][" + String.valueOf(observedPosY) + "]");
                break;
        }

        boolean isGoal = (observedPosX==8 && observedPosY==8);
        if (isGoal) {
            reward = GOAL_REWARD;
        }
        
        return reward;
    }

    public void updeteQ(int reward, int action) {
        int maxA = 0;
        for (int i=0; i<4; i++) {
            boolean isLearger = (q[observedPosX][observedPosY][maxA] < q[observedPosX][observedPosY][action]);
            if (isLearger) {
                maxA = i;
            }
        }

        q[posX][posY][action] = (1.0 - ALPHA) * q[posX][posY][action] + ALPHA * (reward + GAMMA *q[observedPosX][observedPosY][maxA]);
    }

    public void updateState() {
        posX = observedPosX;
        posY = observedPosY;
    }

    public void printQ() {
        for (int x=0; x<10; x++) {
            for (int y=0; y<10; y++) {
                for (int a=0; a<4; a++) {
                    System.out.println("x:" + x + " y:" + y + " a:" + a + " Q:" + q[x][y][a]);
                }
            }
        }
    }
}