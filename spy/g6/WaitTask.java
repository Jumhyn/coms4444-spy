package spy.g6;

import spy.sim.Point;

public class WaitTask extends MovementTask {
	// protected LinkedList<Point> moves;
	
	int roundsWait;
	//wait at point and avoid communication
	public WaitTask(int i) {
		super();
		roundsWait = i;
	}

	public boolean isCompleted() {
		return (roundsWait == 0);
		// return false;
	}
	
	@Override
	public Point nextMove(){
		--roundsWait;
		return new Point(0, 0);
	}
}
