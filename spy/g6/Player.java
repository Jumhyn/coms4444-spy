package spy.g6;

import java.util.*;
import java.util.List;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class Player implements spy.sim.Player {
    
    private ArrayList<ArrayList<Record>> observations;
    private int id;
    private Point loc;
    private HashMap<Integer,ArrayList<Record>> recordsToldBy;
    private HashMap<Point,ArrayList<Record>> pointsToldBy;
    private List<Point> waterCells;
    private List<Point> path;
    private Boolean pathFound;
    private Boolean packageFound;
    private Boolean targetFound;
    private PathFinder pathFinder;
    private Point packageLoc;
    private Point targetLoc;
    private Boolean movingToPackage;
    
    private static final int EVANS_CONVENTION = 15;
    
    // Keeping track of when was a specific player last seen by us.
    private int[] lastPlayerSeen;
    
    // Ongoing tasks for movements
    private LinkedList<MovementTask> tasks;
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.observations = new ArrayList<ArrayList<Record>>(100);
        this.recordsToldBy = new HashMap<>();
        this.pointsToldBy = new HashMap<>();
        this.waterCells = waterCells;
        this.path = new ArrayList<Point>();
        this.pathFound = false;
        this.packageFound = false;
        this.targetFound = false;
        this.movingToPackage = false;
        pathFinder = new PathFinder(waterCells);
        
        for (int i = 0; i < 100; i++){
        	ArrayList<Record> row = new ArrayList<Record>(100);
            for (int j = 0; j < 100; j++){
                row.add(null);
            }
            observations.add(row);
        }
        
        lastPlayerSeen = new int[n];
        tasks = new LinkedList<MovementTask>();
        tasks.add(new BasicMovement());
        
        // System.out.println(this.records);
    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        int time = Simulator.getElapsedT();
      
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            
            if (status.getPT() == 1){
                // System.out.println("Found Package");
                this.packageFound = true;
                this.packageLoc = new Point(p);
                System.out.println("Found Package");
                System.out.println(this.packageLoc);
            }
            else if (status.getPT() == 2){
                // System.out.println("Found Target");
                this.targetFound = true;
                this.targetLoc = new Point(p);
                System.out.println("Found Target");
                System.out.println(this.targetLoc);
            }

            if (this.packageFound && this.targetFound && !this.movingToPackage){
                tasks.addFirst(new GoToPackageTask(pathFinder, this.loc, this.packageLoc));
                this.movingToPackage = true;
            }


            Record record = new Record(p, status.getC(), status.getPT(), new ArrayList<Observation>());
            record.getObservations().add(new Observation(this.id, time));
            observations.get(p.x).set(p.y, record);
            pathFinder.updateMap(p.x, p.y, status.getC() == 1);
            
            for (Integer player: status.getPresentSoldiers()) {
            	if (lastPlayerSeen[player] + EVANS_CONVENTION < time) {
            	    if(player != this.id){
                        for(MovementTask task: tasks){
                            if(task instanceof MeetPlayer){
                                tasks.remove(task);
                            }
                        }
//                        tasks.addFirst(new MeetPlayer(player, this.id, this.loc, p, waterCells));
                    }
            		//tasks.addFirst(new MeetPlayer(player));
            		lastPlayerSeen[player] = time;
            	}
            }
            /*
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
                
                // Find nearby players

            }*/

        }
        // System.out.println(records);
    }
    
    public List<Record> sendRecords(int id)
    {
        ArrayList<Record> toSend = new ArrayList<Record>();
        for (ArrayList<Record> row : observations)
        	for (Record record : row)
        		if (record != null){
        			toSend.add(record);
        		}
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> records){
        ArrayList<Record> receivedRecs = new ArrayList<Record>();

        for (Record record : records){
            //only add the record if not null
            if (record != null){
                receivedRecs.add(record);
                Point p = record.getLoc();
                //keep track of all the points
                ArrayList<Record> list = pointsToldBy.getOrDefault(p, new ArrayList<Record>());
                list.add(record);
                pointsToldBy.putIfAbsent(p, list);
                pathFinder.updateMap(p.x, p.y, record.getC() == 1);
                if (record.getPT() == 1){
                    // System.out.println("Found Package");
                    this.packageFound = true;
                    this.packageLoc = new Point(p);
                    System.out.println("Found Package");
                    System.out.println(this.packageLoc);
                }
                else if (record.getPT() == 2){
                    // System.out.println("Found Target");
                    this.targetFound = true;
                    this.targetLoc = new Point(p);
                    System.out.println("Found Target");
                    System.out.println(this.targetLoc);
                }
            }
        }

        //keep track of records told by a specific player
        if (recordsToldBy.containsKey(id)){
            // concatenate receivedRecs
            recordsToldBy.get(id).addAll(receivedRecs);
        }else{
            recordsToldBy.put(id,receivedRecs);

        }

        lastPlayerSeen[id] = Simulator.getElapsedT();
    }
    
    public List<Point> proposePath()
    {

        // targetFound =true;
        // packageFound = true;
        System.out.println("proposePath");
        if (this.targetFound && this.packageFound){
            pathFound =true;
            if (pathFound){
                System.out.println("Looking for path");
                // Point s = new Point(0,0);
                // Point t = new Point(99,99);
                path = pathFinder.startSearch(this.packageLoc,this.targetLoc,true);

                if (path != null && path.size() >= 1){
                    return path;
                }
                else{
                    return null;
                }
            } 
            else{
                //find shortest path
                //bfs with all weights as 1
                //PathFinder.search()
                return null;
            }
        }

        else{
            return null;
        }
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            // return entry.getKey();
            return toReturn;
        }
        return null;
    }
    
    // Recieves the results (in the event that no path succeeds).
    public void receiveResults(HashMap<Integer, Integer> results){

        //analyze why we did not win
        //was a the spys doing or a bug in someones code
        for (Map.Entry<Integer, Integer> result : results.entrySet())
        {

            ArrayList<Integer> resultKeys = new ArrayList<Integer>();
            resultKeys.add(result.getKey());
            // result.getKey()

        }

        
    }
    
    public Point getMove(){
    	while (tasks.peek().isCompleted()) {
    		tasks.removeFirst();
    	}
    	
        Point delta = tasks.peek().nextMove();
        loc = new Point(delta.x + loc.x, delta.y + loc.y);
        return delta;
    }
    
    public class BasicMovement extends MovementTask {
    	private double moveFactor = 1.0D;
    	private double exploreFactor = 1.0D;
    	private double verifyFactor = 0.0D;
    	private double proximityFactor = 0.00D; // TODO
    	
    	private Point lastMove = new Point(0, 0);
    	
    	@Override
    	public boolean isCompleted() {
    		return false;
    	}
    	
    	protected double getScore(int deltax, int deltay) {
    		if (waterCells.contains(new Point(loc.x + deltax, loc.y + deltay)))
    			return -1.0D;
            //return negative score for location outside the map
            if (loc.x + deltax < 0 || loc.x + deltax > 99 || loc.y + deltay < 0 || loc.y + deltay > 99 )
                return -1.0D;
    		if (deltax == 0 && deltay == 0)
    			return -1.0D;

    		HashSet<Point> points = new HashSet<Point>();
    		if (deltax != 0) {
    			points.add(new Point(loc.x + 3 * deltax, loc.y - 1));
    			points.add(new Point(loc.x + 3 * deltax, loc.y + 1));
    			points.add(new Point(loc.x + 4 * deltax, loc.y + deltay));
    			points.add(new Point(loc.x + 3 * deltax, loc.y - 2 + deltay));
    			points.add(new Point(loc.x + 3 * deltax, loc.y + 2 + deltay));
    		}
    		else {
    			points.add(new Point(loc.x + 3, loc.y + deltay));
    			points.add(new Point(loc.x - 3, loc.y + deltay));
    		}
    		
    		if (deltay != 0) {
    			points.add(new Point(loc.x - 1, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + 1, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + deltax, loc.y + 4 * deltay));
    			points.add(new Point(loc.x - 2 + deltax, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + 2 + deltax, loc.y + 3 * deltay));
    		}
    		else {
    			points.add(new Point(loc.x + deltax, loc.y + 3));
    			points.add(new Point(loc.x + deltax, loc.y - 3));
    		}
    		
    		int exploreCount = 0, verifyCount = 0;
    		for (Point p : points) {
    			try {
    				if (!waterCells.contains(p) && observations.get(p.x).get(p.y) == null) {
    					if (pointsToldBy.containsKey(new Point(p.x, p.y)))
    						++verifyCount;
    					else
    						++exploreCount;
    				}
                    else{
                        // --exploreCount;
                    }
    			}
    			catch(Exception e) {
    				continue;
    			}
    		}
    		
    		int cost = (Math.abs(deltax) + Math.abs(deltay) == 2) ? 3 : 2;
    		if (observations.get(loc.x).get(loc.y).getC() != 1 &&
    				observations.get(loc.x + deltax).get(loc.y + deltay).getC() == 1)
    			cost *= 2;
    		return (exploreFactor * exploreCount + verifyFactor * verifyCount) / cost / moveFactor;
            // return (exploreFactor * exploreCount ) / cost;
    	}
    	
    	@Override
    	public Point nextMove(){
    		if (moves != null && !moves.isEmpty()) {
				Point p = new Point(moves.getFirst().x - loc.x, moves.getFirst().y - loc.y);
				moves.removeFirst();
				return p;
			}
    		
    		int bestMoveX = 0, bestMoveY = 0;
    		double bestScore = -1.0D;
			for (int deltax = -1; deltax <= 1; ++deltax)
				for (int deltay = -1; deltay <= 1; ++deltay) {
					double localScore = getScore(deltax, deltay);
					/*// TODO Temporary hack for not turning back
					if ((deltax + lastMove.x == 0) && (deltay + lastMove.y == 0)){
						localScore -= 0.5D;
					}*/
					if (localScore > bestScore) {
						bestMoveX = deltax;
						bestMoveY = deltay;
						bestScore = localScore;
					}
				}
			if (bestScore <= 0.0D) {
				moves = pathFinder.explore(loc);
				moves.removeFirst();
				Point p = new Point(moves.getFirst().x - loc.x, moves.getFirst().y - loc.y);
				moves.removeFirst();
				return p;
			}
			//lastMove = new Point(bestMoveX, bestMoveY);
			return new Point(bestMoveX, bestMoveY);
    	}
    }

}
