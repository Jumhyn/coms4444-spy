package spy.g6;

import java.util.LinkedList;

import spy.sim.Point;

public class FindPath extends MovementTask{

	LinkedList<Point> points;
	PathFinder pathFinder;
	Point end;
	
	// Find a non-muddy path from start (current position) to the end.
	public FindPath(PathFinder pathFinder, Point start, Point end) {
		super();
		this.pathFinder = pathFinder;
		this.end = end;
		createNewPath(start);
	}
	
	@Override
	public boolean isCompleted() {
		return points.get(0).equals(end);
	}
	
	public void createNewPath(Point start) {
		points = pathFinder.startSearch(start, end, true);
		moves = new LinkedList<Point>();
		Point prev = null;
		for (Point p: points) {
			if (prev != null) {
				//System.err.print(new Point(p.x - prev.x, p.y - prev.y));
				moves.addLast(new Point(p.x - prev.x, p.y - prev.y));
			}
			prev = p;
		}
	}
	
	public Point nextMove(){
		if (points.size() >= 4) {
			Point p = points.get(3);
			if (pathFinder.map[p.x][p.y] == 2) {
				createNewPath(points.get(0));
			}
		}
		
		if (points.size() >= 3) {
			Point p = points.get(2);
			if (pathFinder.map[p.x][p.y] == 2) {
				createNewPath(points.get(0));
			}
		}

		points.removeFirst();
	 	return moves.removeFirst();
	}
	
	
}
