package spy.g5;

import java.util.List;
import java.util.ArrayList;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class MazeSolver
{
	private static int SIZE = 100;
	private int [][] maze;
	
	private Point start;
	private Point end;
	public List<Point> path = null;
	
    public MazeSolver(Point start, Point end, ArrayList<ArrayList<Record>> map)
    {
    	this.start = start;
    	this.end = end;
    	
    	maze = new int[100][100];
    	for (int row = 0; row < SIZE; row++)
    	{
    		for (int col = 0; col < SIZE; col++)
        	{
        		Record r = map.get(row).get(col);
        		// If it is NOT Mudd, give weight 1
        		if(r.getC() == 0)
        		{
        			maze[row][col] = 1;
        		}
        		// If it is muddy, put infinite weight
        		else
        		{
        			maze[row][col] = Integer.MAX_VALUE;
        		}
        	}
    	}
    }
    
    // Build Methods to traverse it
    public void solve()
    {
    	path = new ArrayList<Point>();
    	path.add(start);
    	
    }
}
