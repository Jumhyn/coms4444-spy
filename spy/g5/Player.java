package spy.g5;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

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

	// Build a Records Table for each individual Player
	// We can then use Set Theory as suggested by Bohan to find the spy!
	// Or find who has been tricked by the spy!
	// Truth table technically is in here as well.
	private HashMap<Integer, ArrayList<ArrayList<Record>>> records = new HashMap<Integer, ArrayList<ArrayList<Record>>>();	
	private ArrayList<ArrayList<Record>> explore_map;
	
	private valid validate;// Use a new class to seperate validation methods
	
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
    private boolean movingLeft;
	
	// Anyone accused of spy (includes buggy players, go in this black list!
	private ArrayList<Integer> spies = new ArrayList<Integer>();
	
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
		this.movingLeft = false;
		stay = false;
		if(isSpy)
		{
			SPY_ID = id;
		}
		this.isSpy = isSpy;
		movePosition = new Point(0,0);
		moveToPlayer = false;
		
		// Initialize Maps
		for (int k = 0; k < n; k++)
		{
			ArrayList<ArrayList<Record>> record = new ArrayList<ArrayList<Record>>();
			for (int i = 0; i < SIZE; i++)
			{
				ArrayList<Record> row = new ArrayList<Record>();
				for (int j = 0; j < SIZE; j++)
				{
					row.add(null);
				}
				record.add(row);
			}
			records.put(k, record); 
		}

	        this.explore_map = new ArrayList<ArrayList<Record>>();
		for (int i = 0; i < SIZE; i++)
		    {
			ArrayList<Record> row = new ArrayList<Record>();
			for (int j = 0; j < SIZE; j++)
			    {
				row.add(null);
			    }
			explore_map.add(row);
		    }


		truth_table = records.get(this.id);

		// It is critical for truth_table to know water tiles
		// to catch spies saying a water tile is a mud/clear tile!
		for (int i = 0; i < waterCells.size(); i++)
		{
			Point p = waterCells.get(i);
			// c = 2 is water, pt = 0, regular
			truth_table.get(p.x).set(p.y, new Record(p, 2, 0, new ArrayList<Observation>()));
			explore_map.get(p.x).set(p.y, new Record(p, 2, 0, new ArrayList<Observation>()));
		}
		
		validate = new valid(truth_table); //validate is now a pointer to truth_table, is this intended?
	        // i dont think we need this?? records.put(this.id, truth_table);
	}

    public boolean checkLoc(List<Integer> players, Point soilder, Point us)
    {
    	Integer player = players.get(0);
    	for(Integer player_test:players){
    		if(player_test!=this.id){
    			player = player_test;
    			break;
    		}
    	}

		if(player>this.id)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

    public boolean checkLoc(Point soilder, Point us)
    {
        if(soilder.y > us.y)
        {
        	return true;
       	}
        else if (soilder.y == us.y)
        {
            if(soilder.x < us.x)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else 
        {
            return false;
        }
    }

    public Point playerDetectedMove()
    {
    	playerDetect = false;
		Point ret = new Point(0,0);
        if(stay)
        {
            stay = false;
            ret = null;
        }
        else if (moveToPlayer)
        {
            // System.out.println("Move Here");
            // System.out.println(movePosition.x);
            // System.out.println(movePosition.y);
            ret = movePosition;
        }
        return ret;
    }

	// This is our observation, so we know this is factual.
    // Like do I really need to check if Package mismatched? Or Condition?
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
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
            if((Math.abs(p.x-current.x)>1)||(Math.abs(p.y-current.y)>1)){}
            else{
                for(Integer player:players)
	            {
	                if(justMet.contains(player)||player==this.id)
	                {
	                    flag *= 1;
	                }
	                else
	                {
		            	// System.out.println(player);
	                    flag *= 0;
	                    justMet.add(player);
	                    meetTime.add(25);

	                }
	            }
	        }
            if((players != null)&&(flag==0))
            {
                if(((p.x==current.x)&&(p.y==current.y))||(Math.abs(p.x-current.x)>1)||(Math.abs(p.y-current.y)>1))
                {
                	
                }
                else
                {
                    playerDetect = true;
                    if(checkLoc(players,p,current))
                    {
                        stay = true;
                        moveToPlayer = false;
                        movePosition.x = p.x - current.x;
                        movePosition.y = p.y - current.y;
                    }
                    else
                    {
                        stay = false;
                        moveToPlayer = true;
                        movePosition.x = p.x - current.x;
                        movePosition.y = p.y - current.y;
                      //   System.out.println("Where to go");
                     	// System.out.println(movePosition.x);
                     	// System.out.println(movePosition.y);                           
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
                explore_map.get(p.x).set(p.y, record2);
                
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
            // Update Validate and Records
            // since validate is carrying a pointer, I dont think we need this either? validate.update_truth(truth_table);
            // ^^^ records.put(this.id, truth_table);
        }
    }
	
	public List<Record> sendRecords(int id)
	{
		// justMet.add(id);
		// meetTime.add(25);
		//System.out.println("Communicate");
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
		// All spies are ignored
		// Note a buggy player is a spy in terms of function
		if(spies.contains(id))
		{
			return;
		}
		else
		{
			for(int i = 0; i < recs.size(); i++)
			{
				Record r = recs.get(i);
				if(is_lying(r) == 1){
				    if (r.getObservations().size() == 1) //dont want to accuse people as spies for passing on info people told them
					{
					    spies.add(id);
					    return;
					}
				    else{
					continue;
				    }
				}
				else
				{
					Point p = r.getLoc();
					r.getObservations().add(new Observation (this.id, Simulator.getElapsedT()));
					
					// Add this record to its matchin group!
					// Update regradless if null or not
					ArrayList<ArrayList<Record>> record = records.get(r.getObservations().get(0).getID());
					record.get(p.x).set(p.y, new Record(r));
					
					if(truth_table.get(p.x).get(p.y) == null)
					{
						if(r.getPT() == 1 && package_loc == null)
						{
							possible_package = p;
						}
						if(r.getPT() == 2 && target_loc == null)
						{
							possible_target = p;
						}
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
		if(target_loc == null || package_loc == null)
		{
			return null;
		}
		if (proposing_rounds > 6){
		    MazeSolver explore = new MazeSolver(current, target_loc, truth_table);
		    explore.bushwhack();
		    go_to = explore.path;
		    proposing_rounds = 0;
	    }	
		
		// Use other people's data as well!
		ArrayList<ArrayList<Record>> grand_table = new ArrayList<ArrayList<Record>>();
		for (int i = 0; i < SIZE; i++)
		{
			ArrayList<Record> row = new ArrayList<Record>();
			for (int j = 0; j < SIZE; j++)
			{
				row.add(null);
			}
			grand_table.add(row);
		}
		
		// Start with appending all stuff from truth table!
		for (int i = 0; i < SIZE;i++)
		{
			ArrayList<Record> row = truth_table.get(i);
			for(Record r : row)
			{
				if(r != null)
				{
					Point p = r.getLoc();
					grand_table.get(p.x).set(p.y, new Record(r));	
				}
			}
		}
		
		for(int i = 0; i < n_players;i++)
		{
			// No need to check Truth table?
			if(i == this.id)
			{
				continue;
			}
			
			// Ignore spy data
			if(spies.contains(i))
			{
				continue;
			}
			else
			{
				ArrayList<ArrayList<Record>> g_table = records.get(i);			
				// Do a Pairwise comparison with our truth table THIS CAN ACCUSE VALID PLAYERS OF BEING SPIES IF THE SPY LIES ABOUT HISTORY WITH THEM AS THE DISCOVERER
			/*	if(validate.find_contradiction(g_table))
				{
				    System.out.println("found lie in players table");
				    System.out.println(i);
				    //spies.add(i);
				}
				else
				{				
					// If it passes put everything in record to grand table! 
					for(int a = 0; a < SIZE; a++)
					{
						ArrayList<Record> row = g_table.get(a);
						for(int b = 0; b < SIZE; b++)
						{
							Record rec = row.get(b);
							grand_table.get(a).set(b, new Record(rec));
						}
					}
				 } */
					
			}
		}

		
		MazeSolver total = new MazeSolver(package_loc, target_loc, grand_table);
		total.solve();
		
		MazeSolver solution = new MazeSolver(package_loc, target_loc, truth_table);
		solution.solve();
		
		//	System.out.print("proposing path: ");
		if(solution.path != null)
		{
			//  for(Point p : solution.path){
			//	System.out.printf("(%d,%d), ", p.x, p.y);

			//		    }
			// System.out.println();
		}

		// give wrong direction somehow...
		if(isSpy && solution.path != null)
		{
			// Try this...
		    // solution.path.add(solution.path.get(solution.path.size() - 2));
		    solution.path.remove(solution.path.size() - 2);
		    return solution.path;
		}
		else
		{
			// IMPORTANT FOR REPORT TO SEE IF WE CATCH THE SPY
			//System.out.println(this.id + " G5 accuses the following as spies: " + Arrays.toString(spies.toArray()));
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

		HashMap<Integer, Integer> sizes = new HashMap<Integer, Integer>();
		
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
				if(validate.is_valid_path(proposed_path) != 0 && !isSpy)
				{
				    sizes.put(i, proposed_path.size());
				}

				else
				    {
					if(validate.is_valid_path(proposed_path) == 0 && isSpy){
					    sizes.put(i, proposed_path.size());
					}
					spies.add(i);
				    }
				if(validate.is_valid_path(proposed_path) == -1) {
				    valid_paths.add(proposed_path);
				}
			
			}
		}
		if(valid_paths.size() > 0)
		{
			possible_path = valid_paths.get(0);
		} 
		else 
		{
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

		if(voting_rounds > 1){
		    int minLen = 10000;
		    int bestPath = this.id;
		    for (Integer i : sizes.keySet()){
			if (sizes.get(i) < minLen){
			    minLen = sizes.get(i);
			    bestPath = i;
			}
		    }

		    vote_for.add(bestPath);
		} else {
		    for(Integer i : sizes.keySet()){
			vote_for.add(i);
		    }
		}
		return vote_for;
	}

	// Recieves the results (in the event that no path succeeds).
	public void receiveResults(HashMap<Integer, Integer> results)
	{
		voting_rounds++;
		if(voting_rounds > 3)
		{
			go_to = possible_path;
			voting_rounds = 0;
		}

		for(int i = 0; i < n_players; i++)
		{
			Integer num_votes = results.get(i);
			if(num_votes != null)
			{
			    //				System.out.println("G"+i+ " got " + num_votes + " votes");
			}
		}
	}


	// How much to shift to next location...
	public Point getMove()
	{
		stuck_for--;
		for (int i=justMet.size()-1;i>=0;i--)
		{
			// System.out.println("Meet Time");
			// System.out.println(meetTime.get(i));
			int a = meetTime.get(i);
			a = a - 1 ;
			if(a==0)
			{
				meetTime.remove(i);
				justMet.remove(i);
			}
			else
			{
				meetTime.set(i, a);
			}
		}

		if(playerDetect)
		{
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

		int x = 0;
		int y = 0;

		if(target_loc != null && package_loc != null)
		{
			MazeSolver moveToPackage = new MazeSolver(current, package_loc, truth_table);
			moveToPackage.bushwhack();

			MazeSolver finalPath = new MazeSolver(package_loc, target_loc, truth_table);
			finalPath.solve();

			if(moveToPackage.path != null && finalPath != null)
			{
				go_to = moveToPackage.path;
				return new Point(0, 0);
			}
		}

		if(target_loc == null && possible_target != null){

		    MazeSolver exploreTarget = new MazeSolver(current, possible_target, truth_table);
		    
		    exploreTarget.bushwhack();
		    possible_target = null;
		    if(exploreTarget.path != null){
			go_to = exploreTarget.path;
			
			//System.out.println("exploring target");
			return new Point(0,0);
		    }
		    
		}
		
		if(package_loc == null && possible_package != null){
		    MazeSolver exploreTarget = new MazeSolver(current, possible_package, truth_table);
		    exploreTarget.bushwhack();
		    possible_package = null;
		    if(exploreTarget.path != null){
			go_to = exploreTarget.path;
			
			//System.out.println("exploring target");
			return new Point(0,0);
		    }
		    
		} 


		//System.out.println("Target FOUND");

		if( !movingLeft ){
		    int possible_y = current.y;
		    int possible_x = current.x;
		    while (possible_y+1 < SIZE && explore_map.get(current.x).get(possible_y).getC() == 0) 
			{
			    possible_y++;
			    if (explore_map.get(current.x).get(possible_y) == null)
				{
				    return new Point(0, 1);
				}
			}
		    while (possible_x+1 < SIZE && explore_map.get(possible_x).get(current.y).getC() == 0)
			{
			    possible_x++;
			    if (explore_map.get(possible_x).get(current.y) == null)
				{
				    return new Point(1, 0);
				}
			}
		    possible_y = current.y;
		    possible_x = current.x;
		    while (possible_y-1 >= 0 && explore_map.get(current.x).get(possible_y).getC() == 0)
			{
			    possible_y--;
			    if (explore_map.get(current.x).get(possible_y) == null)
				{
				    return new Point(0, -1);
				}
			}
		}
		int possible_x = current.x;
		while (possible_x-1 >= 0 && explore_map.get(possible_x).get(current.y).getC() == 0)
		    {
			possible_x--;
			if (explore_map.get(possible_x).get(current.y) == null)
			    {
				movingLeft = true;
				return new Point(-1, 0);
			    }
		    }
		movingLeft = false;
		
		    /*		int count = 0;
		List<Point> new_path = new ArrayList<Point>();
		while (possible_x-1 >= 0 && explore_map.get(possible_x).get(current.y).getC() == 0)
		{
			possible_x--;
			count ++;
			new_path.add(new Point(possible_x, current.y));
			if (explore_map.get(possible_x).get(current.y) == null)
			{
			    while ( count < 7 && possible_x-1 >= 0 && (explore_map.get(possible_x-1).get(current.y) == null || explore_map.get(possible_x-1).get(current.y).getC() == 0)){
				possible_x--;
				count ++;
				new_path.add(new Point(possible_x, current.y));
				
			    }
			    go_to = new ArrayList<Point>();
			    int count2 = 0;
			    while(count2 < 7 && count2 < new_path.size()){
			    
				go_to.add(new_path.get(count2));
				count2 ++;
				    
			    }
			    return new Point(0,0);
			}
		}
		    */
		
		MazeSolver sweeper = new MazeSolver(current, current, explore_map);
		List<Point> spath = sweeper.sweep(current, explore_map);
		if (spath != null) 
		{
		    spath.remove(spath.size()-1);
			go_to = spath;
			return new Point(0, 0);
		}
		List<Point> epath = sweeper.explore(current, explore_map);
		if (epath != null) 
		{
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
}
