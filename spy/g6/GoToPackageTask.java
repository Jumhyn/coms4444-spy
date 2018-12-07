package spy.g6;

import java.util.LinkedList;

import spy.sim.Point;

public class GoToPackageTask extends MovementTask {
	// protected LinkedList<Point> moves;
	
	public GoToPackageTask(PathFinder pathFinder, Point loc, Point packageLoc) {
		super();
		LinkedList<Point> points = pathFinder.startSearch(loc, packageLoc, false);
		moves = new LinkedList<Point>();
		Point prev = null;
		for (Point p: points)
			if (prev != null)
				moves.addLast(new Point(p.x - prev.x, p.y - prev.y));
			else
				prev = p;
		
	}
	
	// public GoToPackageTask(LinkedList<Point> moves) {
	// 	this.moves = moves;
	// }
	
	public boolean isCompleted() {
	    return false;
	}
	
	public Point nextMove(){
		if (moves.isEmpty())
			return new Point(0, 0);
	 	return moves.removeFirst();
	}
}
