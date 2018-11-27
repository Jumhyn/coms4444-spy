package spy.g5;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class Player implements spy.sim.Player 
{
	// This Matrix has a 100% Truthful Map because
	// I assume we can trust ourselves right?
	// There is no point to use Record because I don't care what other players say!
	private ArrayList<ArrayList<Record>> truth_table;
	
	// This Matrix has a <= 100% Turthful map because
	// we can't trust others.
    private ArrayList<ArrayList<Record>> records;
    
    // Parse Init
    private int id;
    private Point current;
    private boolean isSpy;
    private List<Point> waterCells;
    private int time;
    private final static int SIZE = 100;
    
    // Spy functions
    private boolean spy_detected;
    private int SPY_ID = -1;
    
    
    // Keep Location of Target and Package
    private Point target_loc;
    private Point package_loc;
    
    // Exploration function
    private boolean sweep_complete = false;

    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        // n = number of players
        this.id = id;// id is our Player id (G5)
        this.time = t; // time out argument
        this.current = startingPos;// Current Position
        this.waterCells = waterCells; //Water, Can't pass them 
        
        if(isSpy)
        {
        	SPY_ID = id;
        }
        this.isSpy = isSpy;//Am I spying?
        
        // Initialize Maps
        this.records = new ArrayList<ArrayList<Record>>();
        this.truth_table = new ArrayList<ArrayList<Record>>();
        
        for (int i = 0; i < SIZE; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            ArrayList<Record> true_row = new ArrayList<Record>();
            for (int j = 0; j < SIZE; j++)
            {
                row.add(null);
                true_row.add(null);
            }
            this.records.add(row);
            this.truth_table.add(true_row);
        }
        
    	// It is critical for truth_table to know water tiles
    	// to catch spies saying a water tile is a mud/clear tile!
        for (int i = 0; i < waterCells.size(); i++)
        {
        	Point p = waterCells.get(i);
        	// Technically speaking everyone knows at t = 0 water.
        	ArrayList<Observation> observations = new ArrayList<Observation>();
        	for(int j = 0; j < n; j++)
        	{
        		observations.add(new Observation(i, 0));
        	}
        	// c = 2 is water, pt = 0, regular
        	truth_table.get(p.x).set(p.y, new Record(p, 2, 0, observations));
        }
        
    }
    
    // This is our observation, so we know this is factual.
    // Like do I really need to check if Package mismatched? Or Condition?
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
    	// Update current solider location
        current = loc;
	//System.out.println("Updaing loc");

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            List<Integer> players = status.getPresentSoldiers();
            if(players != null)
            {

            }
            
            int condition = status.getC();
            int type = status.getPT();
            if(condition == 0)
            {
            	//Not Mud!
            }
            else if(condition == 1)
            {
            	//Mud!
            }
            
            // Package Found!
            if(type == 1)
            {
                package_loc = loc;
            }
            // Target found
            else if(type == 2)
            {
            	target_loc = loc;
            }
            // 0 just means not special tile...
            
            // Check our entry. Row: x, Column: y at our truth only table
            Record record = truth_table.get(p.x).get(p.y);
            if (record == null)
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            else
            {
            	// In a truth table, only we add our oberservation ourselves? 
            	// As a spy we may need to start lying here?
            	record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
            }
        }
    }
    
    private boolean liar_found()
    {
    	// Comare our truth map with our record table...
    	for(int row = 0; row < SIZE; row++)
    	{
    		 List<Record> truth_row = truth_table.get(row);
             List<Record> current_row = records.get(row);
    	     for (int col = 0; col < SIZE; col++)
             {
    	    	 Record truth = truth_row.get(col);
    	    	 Record challenge = current_row.get(col);
    	    	 if(truth == null)
    	    	 {
    	    		 // Well fuck and unknown unknown?
    	    		 // We can put this as unverifiable?
    	    		 if(challenge != null)
    	    		 {
    	    			 continue;
    	    		 }
    	    	 }
    	    	 // Nothing to test the lie against, so eh w/e
    	    	 if(challenge == null)
    	    	 {
    	    		 continue;
    	    	 }
    	    	 // Somone lied about the Mud/Not Mud! The horror!
    	    	 if(truth.getC() != challenge.getC())
    	    	 {
    	    		 return false;
    	    	 }
    	    	 // Someone lied about package/target location! Oh the humanity!
    	    	 // If there is a bug, we can check if it is swapped by someone?
    	    	 if(truth.getPT() != challenge.getPT())
    	    	 {
    	    		return false; 
    	    	 }
             }
    	}
    	return true;
    }
    
    public List<Record> sendRecords(int id)
    {
        ArrayList<Record> toSend = new ArrayList<Record>();
    	if(isSpy)
    	{
    		// We should lie mwahahaha
    		
    	}
    	else
    	{
    		if(id == SPY_ID)
    		{
    			// Deny Spy information!
    			return toSend;
    		}
            for (ArrayList<Record> row : truth_table)
            {
                for (Record record : row)
                {
                    if (record != null)
                    {
                        toSend.add(record);
                    }
                }
            }
    	}
        return toSend;
    }
    
    // Place this in our Record Table, can contain lies!
    // Another idea is if we are sure one ID is a soy, we can just ignore...
    public void receiveRecords(int id, List<Record> records)
    {
    	// Who seriously is trying to send me a null pointer?
    	if(records == null)
    	{
    		return;
    	}
    	if(id == SPY_ID)
    	{
    		// Yeah fuck this I am not trusting a spy...
    		// But should I listen to one and keep the data?
    	}
    	else
    	{
    		for(int i = 0; i < records.size(); i++)
    		{
    			Record r = records.get(i);
                Point p = r.getLoc();
                r.getObservations().add(new Observation (id, Simulator.getElapsedT()));
                records.get(p.x).set(p.y, r);
    			if(is_lying(r) == 1)
    			{
    				// BLOCK EVERYYTHING!
    				// SPY IS FOUND!
    				SPY_ID = id;
    			}

    		}
    		
    		// Append to current observations? 
    		// Check contradicting claims?
    		
    		// For now, just copy it to records?
    	}
    }
    
    private int is_lying(Record unknown)
    {
    	Record truth = truth_table.get(unknown.getLoc().x).get(unknown.getLoc().y);
    	if(truth == null)
    	{
    		// Nothing can be done to check, argh!
    		return -1;
    	}
    	else
    	{
    		// Lie detected
    		if(truth.getC() != unknown.getC())
    		{
    			return 1;
    		}
    		// Lie detected
    		if(truth.getPT() != unknown.getPT())
    		{
    			return 1;
    		}
    		// No lie detected
    		return 0;
    	}
    }
    
    public List<Point> proposePath()
    {
    	if(isSpy)
    	{
    		return null;
    	}
    	else
    	{
    		MazeSolver solution = new MazeSolver(package_loc, target_loc, truth_table);
    		solution.solve();
    		return solution.path;
    	}
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            return new ArrayList<Integer>(entry.getKey());
        }
        return null;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        
    }
    
    // How much to shift to next location...
    public Point getMove()
    {
    	int x = 0;
    	int y = 0;
    	
    	// If we know location of both target and package, we must go to target ASAP!
	//TODO: modify this movement function so that it successfully navigates around water instead of getting stuck
    	if(target_loc != null && package_loc != null)
    	{
    		if(target_loc.x > current.x)
    		{
    			--x;
    			return new Point(x, y);
    		}
    		else if(target_loc.x == current.x)
    		{
    	  		if(target_loc.y > current.y)
        		{
        			--y;
        			return new Point(x, y);
        		}
        		else if(target_loc.y == current.y)
        		{
           			// At location! DONT MOVE
        			return new Point(x, y);
        		}
        		else
        		{
        			++y;
        			return new Point(x, y);
        		}    			
    		}
    		else
    		{
    			++x;
    			return new Point(x, y);
    		}
    	}
	else {
	    //System.out.printf("At point %d, %d\n", current.x, current.y);
	    //TODO: modify this to navigate over diagonal bridges
	    if( package_loc != null || target_loc != null){
		//System.out.println("Target FOUND");
		int possible_y = current.y;
		int possible_x = current.x;
		while (possible_y+1 < SIZE && records.get(current.x).get(possible_y).getC() != 2 && records.get(current.x).get(possible_y).getC() != 1){
		    possible_y++;
		    if (records.get(current.x).get(possible_y) == null) {
			return new Point(0, 1);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_x+1 < SIZE && records.get(possible_x).get(current.y).getC() != 2 && records.get(possible_x).get(current.y).getC() != 1){
		    possible_x++;
		    if (records.get(possible_x).get(current.y) == null) {
			return new Point(1, 0);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_y-1 >= 0 && records.get(current.x).get(possible_y).getC() != 2 && records.get(current.x).get(possible_y).getC() != 1){
		    possible_y--;
		    if (records.get(current.x).get(possible_y) == null) {
			return new Point(0, -1);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_x-1 >= 0 && records.get(possible_x).get(current.y).getC() != 2 && records.get(possible_x).get(current.y).getC() != 1){
		    possible_x--;
		    if (records.get(possible_x).get(current.y) == null) {
			return new Point(-1, 0);
		    }
		}
		if (current.x+1 < SIZE && current.y+1 < SIZE && records.get(current.x+1).get(current.y+1).getC() != 2 && records.get(current.x+1).get(current.y+1).getC() != 1){
		    return new Point(1,1);
		} else if (current.x+1 < SIZE && current.y+1 >= 0 && records.get(current.x+1).get(current.y-1).getC() != 2 && records.get(current.x+1).get(current.y-1).getC() != 1){
		    return new Point(1, -1);
		    
		} else if (current.x-1 >= 0 && current.y+1 < SIZE && records.get(current.x-1).get(current.y+1).getC() != 2 && records.get(current.x-1).get(current.y+1).getC() != 1){
		    return new Point(-1, 1);
		} else if (current.x-1 >= 0 && current.y-1 >= 0 && records.get(current.x-1).get(current.y-1).getC() != 2 && records.get(current.x-1).get(current.y-1).getC() != 1){
		    return new Point(-1, -1);
		} else {
		    return new Point(0, 0);
		}


	    } else { //target has not been found
		//		System.out.println("Target unfound");
		int possible_y = current.y;
		int possible_x = current.x;
		while (possible_y+1 < SIZE && records.get(current.x).get(possible_y).getC() != 2){
		    possible_y++;
		    if (records.get(current.x).get(possible_y) == null) {
      			//System.out.printf("should return %d, %d\n", current.x, current.y+1);
			return new Point(0, 1);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_x+1 < SIZE && records.get(possible_x).get(current.y).getC() != 2){
		    possible_x++;
		    if (records.get(possible_x).get(current.y) == null) {
			///System.out.printf("should return %d, %d\n", current.x+1, current.y);
		        return new Point(1, 0);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_y-1 >= 0 && records.get(current.x).get(possible_y).getC() != 2){
		    possible_y--;
		    if (records.get(current.x).get(possible_y) == null) {
			//System.out.printf("should return %d, %d\n", current.x, current.y-1);
			return new Point(0, -1);
		    }
		}
		possible_y = current.y;
		possible_x = current.x;
		while (possible_x-1 >= 0 && records.get(possible_x).get(current.y).getC() != 2){
		    possible_x--;
		    if (records.get(possible_x).get(current.y) == null) {
			//System.out.printf("should return %d, %d\n", current.x-1, current.y);
			return new Point(-1, 0);
		    }
		}
		//		System.out.println("Can't make simple move");
		if (current.x+1 < SIZE && current.y+1 < SIZE && records.get(current.x+1).get(current.y+1).getC() != 2){
		    return new Point(1, 1);
		} else if (current.x+1 < SIZE && current.y+1 >= 0 && records.get(current.x+1).get(current.y-1).getC() != 2){
		    return new Point(1, -1);
		    
		} else if (current.x-1 >= 0 && current.y+1 < SIZE && records.get(current.x-1).get(current.y+1).getC() != 2){
		    return new Point(-1, 1);
		} else if (current.x-1 >= 0 && current.y-1 >= 0 && records.get(current.x-1).get(current.y-1).getC() != 2){
		    return new Point(-1, -1);
		} else {
		    return new Point(0, 0);
		}
	    }

	}
    	
    	// Lets always sweep left to right?
	//    	if(sweep_complete)
    	//{
    		// If done sweeping, move up!
    	//	return new Point(0, 0);
    	//}
	//	else
    	//{
    	//	return new Point(0, 0);
		//}
    	
    	// Double check I am not walking to water
    	/*
    	if(playerMoveIsValid(p))
    	{
    		if(playerLocationIsValid(p))
    		{
        		return p;
    		}
    		else
    		{
    			// Error
    			return new Point(0, 0);
    		}
    	}
    	else
    	{
    		// ERROR
    		return new Point(0, 0);
    	}
    	*/
    }
    
    private boolean playerMoveIsValid(Point move)
    {
        return Math.abs(move.x) <= 1 && Math.abs(move.y) <= 1;
    }
    
    private boolean playerLocationIsValid(Point loc)
    {
        if (loc.x <= 99 && loc.x >= 0 && loc.y <= 99 && loc.y >= 0)
        {
            // CHeck if in water
        	if(in_water(loc))
        	{
        		return false;
        	}
        	else
        	{
        		return true;
        	}
        }
        else
        {
            return false;
        }
    }
    
    private boolean in_water(Point loc)
    {
    	if(waterCells.contains(loc))
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
}
