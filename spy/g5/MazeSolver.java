package spy.g5;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class MazeSolver
{
	private static int SIZE = 100;
	private ArrayList<ArrayList<Record>> maze;

	private Point start;
	private Point end;
	public ArrayList<Point> path = null;
	private List<List<Point>> parents;
	private List<List<Integer>> offsets;

	//returns path to nearest unexplored square within mud
	public List<Point> sweep(Point s, ArrayList<ArrayList<Record>> map) 
	{
		List<List<Point>> prev = new ArrayList<List<Point>>();
		for(int i = 0; i < SIZE; i++)
		{
			List<Point> row = new ArrayList<Point>();
			for (int j = 0; j < SIZE; j++)
			{
				row.add(null);
			}
			prev.add(row);
		}

		Point c = null;
		List<Point> explore = new ArrayList<Point>();
		explore.add(s);

		while (c == null || map.get(c.x).get(c.y) != null)
		{
			if(explore.size() == 0) 
			{
				return null; //no path is known
			}
			else 
			{
				c = explore.get(0);
				for( List<Point> pre : prev){
					//	    System.out.println(pre);
				}
				explore.remove(0);   
			}

			for(List<Integer> offset: offsets)
			{
				// if cell is in grid, not mud or water and parent is not set
				Point adjacent = new Point(c.x+offset.get(0), c.y+offset.get(1));
				if(adjacent.x >=0 && adjacent.x < 100 && adjacent.y >=0 && adjacent.y < 100) 
				{
					if(prev.get(adjacent.x).get(adjacent.y) == null)
					{
						if (map.get(adjacent.x).get(adjacent.y) == null || map.get(adjacent.x).get(adjacent.y).getC() == 0)
						{

							// set parent as current
							// append cell to explore list (unless cell == end, then add to front of list)
							prev.get(adjacent.x).set(adjacent.y, new Point( c.x, c.y ));
							explore.add(adjacent);
						}
					}
				}
			}
		}
		ArrayList<Point> p = new ArrayList<Point>();
		while(c.x != s.x || c.y != s.y) 
		{
			p.add(c);
			c = prev.get(c.x).get(c.y);
		}
		p.add(s);
		Collections.reverse(p);	
		return p;
	}

	//returns path to nearest unexplored square
	public List<Point> explore(Point s, ArrayList<ArrayList<Record>> map)
	{
		List<List<Point>> prev = new ArrayList<List<Point>>();
		
		for(int i=0; i<100; i++)
		{

			List<Point> row = new ArrayList<Point>();
			for (int j=0; j<100; j++)
			{
				row.add(null);
			}
			prev.add(row);
		}

		Point current = null;
		List<Point> explore = new ArrayList<Point>();
		explore.add(s);

		while (current == null || map.get(current.x).get(current.y) != null)
		{
			if(explore.size() == 0) 
			{
				return null; //no path is known
			}
			else 
			{
				current = explore.get(0);
				explore.remove(explore.get(0));   
			}

			for(List<Integer> offset: offsets)
			{
				// if cell is in grid, not mud or water and parent is not set
				Point adjacent = new Point(current.x+offset.get(0), current.y+offset.get(1));
				if(adjacent.x >=0 && adjacent.x < 100 && adjacent.y >=0 && adjacent.y < 100) 
				{
					if(prev.get(adjacent.x).get(adjacent.y) == null)
					{
						if (map.get(adjacent.x).get(adjacent.y) == null || map.get(adjacent.x).get(adjacent.y).getC() != 2)
						{
							// set parent as current
							// append cell to explore list (unless cell == end, then add to front of list)
							prev.get(adjacent.x).set(adjacent.y, new Point( current.x, current.y ));
							explore.add(adjacent);
						}
					}
				}
			}
		}
		ArrayList<Point> p = new ArrayList<Point>();
		while(current.x != s.x || current.y != s.y) 
		{
			p.add(current);
			current = prev.get(current.x).get(current.y);
		}
		p.add(s);
		Collections.reverse(p);	
		return p;
	}


	public MazeSolver(Point start, Point end, ArrayList<ArrayList<Record>> map)
	{
		this.start = start;
		this.end = end;
		maze = map;

		parents = new ArrayList<List<Point>>();
		offsets = new ArrayList<List<Integer>>();
		offsets.add(Arrays.asList(1,0));
		offsets.add(Arrays.asList(0,1));
		offsets.add(Arrays.asList(-1,0));
		offsets.add(Arrays.asList(0,-1));
		offsets.add(Arrays.asList(1,1));
		offsets.add(Arrays.asList(1,-1));
		offsets.add(Arrays.asList(-1,1));
		offsets.add(Arrays.asList(-1,-1));

		for(int i=0; i < SIZE; i++)
		{
			List<Point> row = new ArrayList<Point>();
			for (int j = 0; j < SIZE; j++)
			{
				row.add(null);
			}
			parents.add(row);
		}
	}


	// Build Methods to traverse it
	public void solve()
	{

		Point current = null;
		List<Point> explore = new ArrayList<Point>();
		explore.add(start);

		while (current == null || current.x != this.end.x || current.y != this.end.y)
		{
			if(explore.size() == 0) 
			{
				return; //no path is known
			}
			else 
			{
				current = explore.get(0);
				explore.remove(0);   
			}

			for(List<Integer> offset: offsets)
			{
				// if cell is in grid, not mud or water and parent is not set
				Point adjacent = new Point(current.x+offset.get(0), current.y+offset.get(1));
				if(adjacent.x >=0 && adjacent.x < 100 && adjacent.y >=0 && adjacent.y < 100) 
				{
					if(parents.get(adjacent.x).get(adjacent.y) == null && maze.get(adjacent.x).get(adjacent.y) != null)
					{
						if (maze.get(adjacent.x).get(adjacent.y).getC() == 0)
						{
							// set parent as current
							// append cell to explore list (unless cell == end, then add to front of list)
							parents.get(adjacent.x).set(adjacent.y, new Point( current.x, current.y ));
							explore.add(adjacent);
						}
					}
				}
			}
		}
		path = new ArrayList<Point>();
		while(current.x != start.x || current.y != start.y) 
		{
			path.add(current);
			current = parents.get(current.x).get(current.y);
		}
		path.add(start);
		Collections.reverse(path);
	}

	public void bushwhack()
	{
		Point current = null;
		List<Point> explore = new ArrayList<Point>();
		explore.add(start);

		while (current == null || current.x != this.end.x || current.y != this.end.y)
		{
			if(explore.size() == 0) 
			{
				return; //no path is known
			}
			else 
			{
				current = explore.get(0);
				explore.remove(0);   
			}

			for(List<Integer> offset: offsets)
			{
				// if cell is in grid, not mud or water and parent is not set
				Point adjacent = new Point(current.x+offset.get(0), current.y+offset.get(1));
				if(adjacent.x >=0 && adjacent.x < 100 && adjacent.y >=0 && adjacent.y < 100) 
				{
					if(parents.get(adjacent.x).get(adjacent.y) == null)
					{
						if (maze.get(adjacent.x).get(adjacent.y) == null || maze.get(adjacent.x).get(adjacent.y).getC() != 2)
						{
							// set parent as current
							// append cell to explore list (unless cell == end, then add to front of list)
							parents.get(adjacent.x).set(adjacent.y, new Point( current.x, current.y ));
							explore.add(adjacent);
						}
					}
				}
			}
		}
		path = new ArrayList<Point>();
		while(current.x != start.x || current.y != start.y) 
		{
			path.add(current);
			current = parents.get(current.x).get(current.y);
		}
		path.add(start);
		Collections.reverse(path);
	}
}