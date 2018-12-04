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
	// we can't trust others records of the map.
    private ArrayList<ArrayList<Record>> records;
    public boolean playerDetect;
    // Parse Init
    private int id;
    public ArrayList<Integer> justMet = new ArrayList<Integer>();
    public ArrayList<Integer> meetTime = new ArrayList<Integer>();
    private Point current;
    private boolean isSpy;
    private List<Point> waterCells;
    private int time;
    private final static int SIZE = 100;
    private int n_players;
    public boolean stay;
    // Spy functions
    private int SPY_ID = -1;
    public Point movePosition;
    public boolean moveToPlayer;
    // Keep Location of Target and Package
    private Point target_loc;
    private Point package_loc;
    private Point possible_package;
    private Point possible_target;

    private int stuck_for;
    private int voting_rounds;
    private int proposing_rounds;
    private List<Point> possible_path;
    
    // Movement functions
    private boolean sweep_complete = false;
    private List<Point> go_to = new ArrayList<Point>();
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.n_players = n;// n = number of players
        this.id = id;// id is our Player id (G5)
        this.time = t; // time out argument
        this.current = startingPos;// Current Position
        this.waterCells = waterCells; //Water, Can't pass them 
	this.voting_rounds = 0;
	this.stuck_for = 0;
        stay = false;
        if(isSpy)
        {
        	SPY_ID = id;
        }
        this.isSpy = isSpy;
        movePosition = new Point(0,0);
        moveToPlayer = false;
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
        	records.get(p.x).set(p.y, new Record(p, 2, 0, observations));
	}
    }
    
    public boolean checkLoc(List<Integer> players, Point soilder, Point us){
    	Integer player = players.get(0);
		if(player>this.id){
			return false;
		}
		else if (player==this.id){
			return checkLoc(soilder,us);
		}
		else{
			return true;
		}
	}


    public boolean checkLoc(Point soilder, Point us){
        if(soilder.y>us.y) return true;
        else if (soilder.y==us.y){
            if(soilder.x<us.x){
                return true;
            }
            else{
                return false;
            }
        }
        else {
            return false;
        }
    }

    public Point playerDetectedMove(){
		Point ret = new Point(0,0);
        if(stay){
            stay = false;
            ret = null;
        }
        else if (moveToPlayer){
            ret = movePosition;
        }
        return ret;
    }

    // This is our observation, so we know this is factual.
    // Like do I really need to check if Package mismatched? Or Condition?
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
    	System.out.println("YO");
    	// Update current solider location
        current = loc;
        playerDetect = false;
        stay = false;
        moveToPlayer = false;
        //System.out.println("Updaing loc");
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            int flag = 1;
            CellStatus status = entry.getValue();
            List<Integer> players = status.getPresentSoldiers();
            for(Integer player:players){
                if(justMet.contains(player)){
                    flag = 1;
                }
                else{
                    flag = 0;
                    break;
                }
            }

            if((players != null)&&(flag==0))
            {
                if(((p.x==current.x)&&(p.y==current.y))||(Math.abs(p.x-current.x)>1)||(Math.abs(p.y-current.y)>1)){
                }
                else{
                    playerDetect = true;
                    if(checkLoc(players,p,current)){
                        stay = true;
                        moveToPlayer = false;
                        movePosition.x = p.x - current.x;
                        movePosition.y = p.y - current.y;
                    }
                    else{
                        stay = false;
                        moveToPlayer = true;
                        movePosition.x = p.x - current.x;
                        movePosition.y = p.y - current.y;
                    }
                }
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
                package_loc = p;
		possible_package = null;
            }
            // Target found
            else if(type == 2)
            {
            	target_loc = p;
		possible_target = null;
            }
            // 0 just means not special tile...
            
            // Check our entry. Row: x, Column: y at our truth only table
            Record record = truth_table.get(p.x).get(p.y);
            if (record == null)
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                truth_table.get(p.x).set(p.y, record);
                Record record2 = new Record(p, status.getC(), status.getPT(), observations);
		records.get(p.x).set(p.y, record2);
            }
            else
            {
            	// In a truth table, only we add our oberservation ourselves? 
            	// As a spy we may need to start lying here?
            	if(isSpy)
            	{
            		record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
            	}
            	else
            	{
            		record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));		
            	}
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
        justMet.add(id);
        meetTime.add(25);
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
    public void receiveRecords(int id, List<Record> recs)
    {
    	// Who seriously is trying to send me a null pointer?
    	if(recs == null)
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
    		for(int i = 0; i < recs.size(); i++)
    		{
		    Record r = recs.get(i);
		    Point p = r.getLoc();
		    r.getObservations().add(new Observation (this.id, Simulator.getElapsedT()));
		    
		    if(is_lying(r) == 1)
    			{
			    // BLOCK EVERYTHING!
			    // SPY IS FOUND!
			    SPY_ID = id;
    			}
		    
		    if(truth_table.get(p.x).get(p.y) == null){
		       
			this.records.get(p.x).set(p.y, r);
			if(r.getPT() == 1 && package_loc == null){
			    possible_package = p;
			}
			if(r.getPT() == 2 && target_loc == null){
			    possible_target = p;
			}
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
    		// Lie detected about Muddy/Not Muddy/Water!
    		if(truth.getC() != unknown.getC())
    		{
    			return 1;
    		}
    		// Lie detected about Locaion of Package/Target
    		if(truth.getPT() != unknown.getPT())
    		{
    			return 1;
    		}
    		// No lie detected
    		return 0;
    	}
    }
    
    // Gets a proposed path from a player at the package
    public List<Point> proposePath()
    {
	proposing_rounds++;

	if(target_loc == null || package_loc == null){
	    return null;
	}
	/*	if (proposing_rounds > 6){
	    MazeSolver explore = new MazeSolver(current, target_loc, truth_table);
	    explore.bushwhack();
	    go_to = explore.path;
	    proposing_rounds = 0;
	    }	*/
		MazeSolver solution = new MazeSolver(package_loc, target_loc, truth_table);
		solution.solve();
		//	System.out.print("proposing path: ");
		if(solution.path != null){
		    //  for(Point p : solution.path){
		    //	System.out.printf("(%d,%d), ", p.x, p.y);
			
		    //		    }
		    // System.out.println();
		}

       
    		// give wrong direction somehow...
	if(isSpy){
  		return solution.path;
    	}
    	else
    	{
    		return solution.path;
    	}
    }
    
    // Gives a map from player ID to a path that player proposed. 
    // Returns the IDs which the player supports
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
    	// Initialize List of Player paths I will vote for
    	ArrayList<Integer> vote_for = new ArrayList<Integer>();
	List<List<Point>> valid_paths = new ArrayList<List<Point>>();
	List<Integer> valid_players = new ArrayList<Integer>();
	
	for(int i = 0; i < n_players;i++)
    	{
    		List<Point> proposed_path = paths.get(i);
    		if(proposed_path == null)
    		{
    			continue;
    		}
    		else
    		{
    			// Analyze the Path. For now, just compare with our truth table. 
    			// If a lie is found, Do NOT vote. Otherwise, vote for it!
		   	if(is_valid_path(proposed_path))
    			{
			    if(vote_for.size() == 0){
				vote_for.add(i);
			    }
			    
			    //valid_players.add(i);
			}
	    
    			else
			    {

    				valid_paths.add(proposed_path);

				// Who else but the spy would give me a bad path?
    				SPY_ID = i;
    			}
    		}
    	}
	if(valid_paths.size() > 0){
	    possible_path = valid_paths.get(0);
	} else {
	    possible_path = new ArrayList<Point>();
	    possible_path.add(new Point(0,0));
	}
	/*
	int p = valid_players.get(0);
       	for(int j=0; j<valid_paths.size(); j++){
	    List<Point> path = valid_paths.get(j);
	    if (path.size() < possible_path.size()){ //not necessarily the shortest path but an ok estimate
		p = valid_players.get(j);
		possible_path = path;
	    }
	}
	vote_for.add(p);
	*/
	return vote_for;
    }
    
    private boolean is_valid_path(List<Point> path)
    {
    	// Obviously empty path is bad!
    	if(path.isEmpty())
    	{
    		return false;
    	}
    	
    	for (int i = 0; i < path.size(); i++)
    	{
         	Point test = path.get(i);
        	Record verify = truth_table.get(test.x).get(test.y);
        	if(verify == null)
        	{
        		// Sadly you can only just continue...
        		return false;
        	}
        	
    		if (i == 0)
    		{
    	    	// Not Location of Package!
    	    	if(verify.getPT() != 1)
    	    	{
    	    		return false;
    	    	}
    		}
    		else if(i == path.size() - 1)
    		{
    	    	// Not Location of Target!
    	    	if(verify.getPT() != 2)
    	    	{
    	    		return false;
    	    	}
    		}
    
        	// Are any of these tiles muddy?
        	// Conidtion 1 -> Muddy and 2 -> Water!
            if(verify.getC() != 0)
        	{
        		return false;
        	}
    	}
    	// Well the path looks good according to the truth table
    	return true;
    }
    
    
    // Recieves the results (in the event that no path succeeds).
    public void receiveResults(HashMap<Integer, Integer> results)
    {
	voting_rounds++;
	if(voting_rounds > 3){
	    go_to = possible_path;
	    voting_rounds = 0;
	}
	
	for(int i = 0; i < n_players; i++)
    	{
    		Integer num_votes = results.get(i);
    		if(num_votes != null)
		    {
			//		System.out.println("G"+i+ " got " + num_votes + " votes");
    		}
    	}
    }
    
	
    // How much to shift to next location...
	public Point getMove()
	{
	stuck_for--;
        for (int i=justMet.size()-1;i>=0;i--){
            int a = meetTime.get(i);
            a = a - 1 ;
            if(a==0){
                meetTime.remove(i);
                justMet.remove(i);
            }
            else{
                meetTime.set(i,a);
            }
        }

        if(playerDetect){
            Point bla = new Point(0,0);
            bla = playerDetectedMove();
            return bla;
        }
        

		// You have a pre-determined path from BFS
		// Just find your next move step!
		if(!go_to.isEmpty())
		{
			Point next_step = go_to.remove(0);
			// Given current, find how to get to next!
			return new Point(next_step.x - current.x, next_step.y - current.y);
		}
		
		ArrayList<Point> moves = all_valid_moves(current);
		// TODO: Combine a sweeping algorithm to select the correct move for player to use
		// Please note this method doesn't consider muddy tiles!
		// Also, it only checks adjacent tiles! So you can be walking into a swamp!
		
		int x = 0;
		int y = 0;

		// If we know location of both target and package, we must go to target ASAP!
		// TODO: modify this movement function so that it successfully navigates around water instead of getting stuck
		if(target_loc != null && package_loc != null)
		{
		    // System.out.printf("Package at %d, %d, target at %d, %d\n", package_loc.x, package_loc.y, target_loc.x, target_loc.y);
		    //			if(target_loc.x > current.x)
		    //	{
		    //		--x;
		    //		return new Point(x, y);
		    //	}
		    //	else if(target_loc.x == current.x)
		    //	{
		    //			if(target_loc.y > current.y)
		    //		{
		    //			--y;
		    //			return new Point(x, y);
		    //		}
		    //		else if(target_loc.y == current.y)
		    //		{
		    //			// At location! DONT MOVE
		    //			return new Point(x, y);
		    //		}
		    //		else
		    //		{
		    //			++y;
		    //			return new Point(x, y);
		    //		}    			
		    //	}
		    //	else
		    //	{
		    //		++x;
		    //		return new Point(x, y);
		    //	}
		    MazeSolver moveToPackage = new MazeSolver(current, package_loc, truth_table);
		    //		    System.out.printf("trying to move to %d, %d from %d, %d if we have a final path", package_loc.x, package_loc.y, current.x, current.y);
		    moveToPackage.bushwhack();

		    MazeSolver finalPath = new MazeSolver(package_loc, target_loc, truth_table);
		    finalPath.solve();
		    
		    if(moveToPackage.path != null && finalPath != null){
			go_to = moveToPackage.path;
			//	System.out.println("set path to package");
			return new Point(0, 0);
		    }
		    //System.out.println("no package path found\n");
		}

		/*    if(target_loc == null && possible_target != null){
			
			    MazeSolver exploreTarget = new MazeSolver(current, possible_target, records);
			    exploreTarget.bushwhack();
			    possible_target = null;
			    if(exploreTarget.path != null){
				go_to = exploreTarget.path;
				
				//System.out.println("exploring target");
				return new Point(0,0);
			    }
			
		    }
		    
		    if(package_loc == null && possible_package != null){
			    MazeSolver exploreTarget = new MazeSolver(current, possible_package, records);
			    exploreTarget.bushwhack();
			    possible_package = null;
			    if(exploreTarget.path != null){
				go_to = exploreTarget.path;
				
				//System.out.println("exploring target");
				return new Point(0,0);
			    }
			
			    } */

		    
		    //System.out.println("Target FOUND");
		    int possible_y = current.y;
		    int possible_x = current.x;
		    while (possible_y+1 < SIZE && truth_table.get(current.x).get(possible_y).getC() == 0) {
			possible_y++;
			if (truth_table.get(current.x).get(possible_y) == null)
			    {
				return new Point(0, 1);
			    }
		    }
		    while (possible_x+1 < SIZE && truth_table.get(possible_x).get(current.y).getC() == 0)
			{
			    possible_x++;
			    if (truth_table.get(possible_x).get(current.y) == null)
				{
				    return new Point(1, 0);
				}
			}
		    possible_y = current.y;
		    possible_x = current.x;
		    while (possible_y-1 >= 0 && truth_table.get(current.x).get(possible_y).getC() == 0)
			{
			    possible_y--;
			    if (truth_table.get(current.x).get(possible_y) == null)
				{
				    return new Point(0, -1);
				}
			}
		    possible_y = current.y;
		    possible_x = current.x;
		    while (possible_x-1 >= 0 && truth_table.get(possible_x).get(current.y).getC() == 0)
			{
			    possible_x--;
			    if (truth_table.get(possible_x).get(current.y) == null)
				{
				    List<Point> new_path = new ArrayList<Point>();
				    if(truth_table.get(current.x-1).get(current.y).getC() == 0){				    
					if(truth_table.get(current.x-2).get(current.y).getC() == 0){
					    new_path.add(new Point(current.x-2,current.y));

					    if(truth_table.get(current.x-3).get(current.y).getC() == 0){
						new_path.add(new Point(current.x-3,current.y));
						if(truth_table.get(current.x-4).get(current.y) == null || truth_table.get(current.x-4).get(current.y).getC() == 0) {
						    new_path.add(new Point(current.x-4,current.y));

						}
					    }
					}
				    }

				    if (new_path.size() > 0){
					go_to = new_path;
					//System.out.print("stepping left");
				    }
				    
				    return new Point(-1, 0);
				}
			}
		    MazeSolver sweeper = new MazeSolver(current, current, truth_table);
		    List<Point> spath = sweeper.sweep(current, truth_table);
		    if (spath != null) {
			go_to = spath;
			return new Point(0, 0);
		    }
		    List<Point> epath = sweeper.explore(current, truth_table);
		    if (epath != null) {
			//System.out.println("epath\n\n\n\n\n\n\n");
			go_to = epath;
			return new Point(0, 0);
		    }
		    
		
		    /*		    //System.out.printf("stuck at %d, %d\n", current.x, current.y);
		stuck_for+=2;
		if(stuck_for > 6){
		    stuck_for = 0;
		    if(target_loc != null){
			MazeSolver helpme = new MazeSolver(current, target_loc, truth_table);
			helpme.bushwhack();
			go_to = helpme.path;
		    } /*else if(target_loc != null){
			MazeSolver helpme = new MazeSolver(current, target_loc, truth_table);
			helpme.bushwhack();
			go_to = helpme.path;
			}*/
		    
		
	       return new Point(0,0);
	}
	
	private ArrayList<Point> all_valid_moves(Point current)
	{
		ArrayList<Point> valid = new ArrayList<Point>();
		/*
		 * All moves are: X is current
		 * (-1,  1) , (0, 1)	, (1, 1)
		 * (-1,  0) , X			, (1, 0)
		 * (-1, -1) , (0. -1)	, (1, -1)
		 * 
		 * The for loops should force the absolute value constraint easily.
		 */
		
		for (int x = -1; x < 2; x++)
		{
			for(int y = -1; y < 2; y++)
			{
				if(valid_step(current, x, y))
				{
					valid.add(new Point(x, y));
				}
			}
		}
		// Probably should remove the point 0, 0?
		valid.remove(new Point(0, 0));
		return valid;
	}
    
    private boolean valid_step(Point loc, int x, int y)
    {
        if (loc.x + x <= SIZE - 1 && loc.x + x >= 0 && loc.y + y <= SIZE - 1 && loc.y + y >= 0)
        {
            // Check if in water using the truth_table
        	Record r = truth_table.get(loc.x + x).get(loc.y + y);
        	// Can't be water here since no entry exists!
        	if(r == null)
        	{
        		return true;
        	}
        	else
        	{
        		// It is a water tile! Not Valid!
        		if(r.getC() == 2)
        		{
        			return false;
        		}
        		else
        		{
        			return true;
        		}
        	}
        }
        else
        {
            return false;
        }
    }
}
