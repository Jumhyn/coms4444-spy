package spy.g6;

import java.util.LinkedList;

import spy.sim.Point;

public class MovementTask {
	protected LinkedList<Point> moves;
	
	public MovementTask() {
		
	}
	
	public MovementTask(LinkedList<Point> moves) {
		this.moves = moves;
	}
	
	public boolean isCompleted() {
		return moves.isEmpty();
	}
	
	public Point nextMove(){
		return moves.removeFirst();
	}
}
