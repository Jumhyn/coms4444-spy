package spy.g6;

import java.util.LinkedList;
import java.util.List;

import spy.sim.Point;

public class VerifyPathTask extends MovementTask {
	// protected LinkedList<Point> moves;
	List<Point> path;
	PathFinder pathFinder;
	public int id;

	public VerifyPathTask(PathFinder pathFinder, List<Point> path, int id) {
		super();
		// LinkedList<Point> path = pathFinder.startSearch(loc, packageLoc, false);
		moves = new LinkedList<Point>();
		Point prev = null;
		this.path = path;
		this.id = id;
		this.pathFinder = pathFinder;
		for (Point p: path) {
			if (prev != null) {
				//System.err.print(new Point(p.x - prev.x, p.y - prev.y));
				moves.addLast(new Point(p.x - prev.x, p.y - prev.y));
			}
			prev = p;
		}
	}
	
	// public GoToPackageTask(LinkedList<Point> moves) {
	// 	this.moves = moves;
	// }
	
	// @Override
	// public boolean isCompleted() {
	//     return false;
	// }

	
	@Override
	public boolean isCompleted() {
		if(moves.isEmpty()){
			return true;
		}
		
		if (path.size() >= 4) {
			Point p = path.get(3);
			if (pathFinder.map[p.x][p.y] == 2) {
				return true;
			}
		}
		
		if (path.size() >= 3) {
			Point p = path.get(2);
			if (pathFinder.map[p.x][p.y] == 2) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Point nextMove(){
		//if (moves.isEmpty())
		//	return new Point(0, 0);
	 	return moves.removeFirst();
	}
}
