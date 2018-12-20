package spy.g7;
import java.util.*;
import spy.sim.Point;

class Maze{
    int maze[][];
    private int row = 100;
    private int col = 100;
    Stack<Point> stack;
    boolean p[][] = null;
    public Maze(){
        maze = new int[102][102];
        stack = new Stack<Point>();
        p = new boolean[102][102];
    }


    public void init(List<Point> waterCells){

        for(int i = 0; i < row; ++i) {
            for(int j = 0; j < col; ++j) {
//                temp = scanner.nextInt();

                p[i][j] = false;
            }
        }
        for(Point p:waterCells){
            maze[p.x][p.y] = 1;
        }

    }


    public List<Point> findPath(Point package_loc, Point target_loc){
        int temp[][] = new int[row + 2][col + 2];
        //System.out.println(row+" "+col);
        for(int i = 0; i < row + 2; ++i) {
            for(int j = 0; j < col + 2; ++j) {
                temp[0][j] = 1;
                temp[row + 1][j] = 1;
                temp[i][0] = temp[i][col + 1] = 1;
            }
        }
        for(int i = 0; i < row; ++i) {
            for(int j = 0; j < col; ++j) {
                temp[i + 1][j + 1] = maze[i][j];
            }
        }
        int i = package_loc.x+1;
        int j = package_loc.y+1;
        p[i][j] = true;
        stack.push(new Point(i, j));
        while (!stack.empty() && (!(i == (target_loc.x+1) && (j == target_loc.y+1)))) {
            int flag = -1;
      //      System.out.print(stack.peek());
            if ((temp[i][j + 1] == 0) && (p[i][j + 1] == false)) {
                if(flag == -1) flag =1;
            }
            if ((temp[i + 1][j] == 0) && (p[i + 1][j] == false)) {

                if(flag == -1) flag = 2;
                if(flag == 1){
                    if(Math.pow(i+1-target_loc.x,2)+Math.pow(j-target_loc.y,2)<Math.pow(i-target_loc.x,2)+Math.pow(j+1-target_loc.y,2))
                        flag = 2;
                }
            }
            if ((temp[i][j - 1] == 0) && (p[i][j - 1] == false)) {

                if(flag== -1) flag = 3;
                if(flag == 1){
                    if(Math.pow(i-target_loc.x,2)+Math.pow(j-1-target_loc.y,2)<Math.pow(i-target_loc.x,2)+Math.pow(j+1-target_loc.y,2))
                        flag = 3;
                }
                if(flag == 2){
                    if(Math.pow(i-target_loc.x,2)+Math.pow(j-1-target_loc.y,2)<Math.pow(i+1-target_loc.x,2)+Math.pow(j-target_loc.y,2))
                        flag = 3;
                }
            }
            if ((temp[i - 1][j] == 0) && (p[i - 1][j] == false)) {

                if(flag==- 1) flag =4;
                if(flag == 1){
                    if(Math.pow(i-1-target_loc.x,2)+Math.pow(j-target_loc.y,2)<Math.pow(i-target_loc.x,2)+Math.pow(j+1-target_loc.y,2))
                        flag = 4;
                }
                if(flag == 2){
                    if(Math.pow(i-1-target_loc.x,2)+Math.pow(j-target_loc.y,2)<Math.pow(i+1-target_loc.x,2)+Math.pow(j-target_loc.y,2))
                        flag = 4;
                }
                if(flag == 3){
                    if(Math.pow(i-1-target_loc.x,2)+Math.pow(j-target_loc.y,2)<Math.pow(i-target_loc.x,2)+Math.pow(j-1-target_loc.y,2))
                        flag = 4;
                }
            }
            if(flag ==-1 ){
                stack.pop();
                if(stack.empty()){
                    break;
                }
                i = stack.peek().x;
                j = stack.peek().y;
            }
            if(flag==1){

                p[i][j + 1] = true;
                stack.push(new Point(i, j + 1));
                j++;
            }
            if(flag==2){
                p[i + 1][j] = true;
                stack.push(new Point(i + 1, j));
                i++;
            }
            if(flag==3){
                p[i][j - 1] = true;
                stack.push(new Point(i, j - 1));
                j--;
            }
            if(flag==4){
                p[i - 1][j] = true;
                stack.push(new Point(i - 1, j));
                i--;
            }

        }

        Stack<Point> newPos = new Stack<Point>();
        if (stack.empty()) {
            System.out.println("no path");
        } else {

            while (!stack.empty()) {
                Point pos = stack.pop();
                newPos.push(pos);
            }
        }
        List<Point> path = new ArrayList<Point>();
        while(!newPos.empty()){
            Point p = newPos.pop();
            path.add(new Point(p.x-1, p.y-1));
        }
        return path;





    }


}

