package spy.g3;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue; 

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;
import java.util.AbstractMap;


public class Player implements spy.sim.Player {

    private class Entry implements Comparable<Entry> {
    public Double key;
    public Point p;

    public Entry(Double key, Point pt) {
        this.key = key;
        this.p = pt;
    }

    // getters

    @Override
    public int compareTo(Entry other) {
        return this.key.compareTo(other.key);
    }
}
    
    private ArrayList<ArrayList<Record>> records; // 2-dim list of cells on map (personal records)
    private int id;
    private boolean isSpy;
    private Point loc; // Current location on map
    private Boolean _target;  // whether target has been located
    private Boolean _package; //whether package has been located
    private Point package_Location; // package location
    private Point target_Location; // target location
    private int[][] grid; // status of cells: -2 is water, -1 is muddy, 0 is normal, 1 is target & Package
    private int[][] visited; // whether the cell has been visited
    private int[][] explored; // whether the cell has been explored i.e. neighbors searched, in the current iteration
    private List<Point> proposedPath; // proposed safe path from package to target
    private Boolean found_path = false; // whether a safe proposed path has been found
    private HashMap<Point,Integer> trap_count; // maintains frequency of visiting a location
    private Integer time; // keeps track of elapsed time
    private Point unexplored; // the next unexplored point to visit

    // Handles communicatin protocol
    private HashMap<Point, CellStatus> lastObservation; 
    private Boolean moveToSoldier = false;
    private Boolean stayStill = false;
    private int targetPeer = -1;
    private HashMap <Integer, Point> nearbySoldiers;
    private HashMap <Integer, Integer> notSeenCount; // Stores number of rounds since last encountering a player
    private int idleCount = 0; // Counter to track idle rounds
    private int followCount = 0; // Counter to track rounds following same communication target
    private int maxCount = 4; // Only approach/wait for target peer for maxCount turns before disregarding communication
    private int minGraceTime = 25; // Minimum count needed to allow communication protocol
    private Boolean send_rec;

    // Handle Information Fusion
    // Each cell has a corresponding list of records which respecitve dry/muddy claims
    // To access a list of records of position (x, y)
    // Simply Index into the desired records with dryRecords.get(x).get(y)
    // This returns a list of records for that cell
 	private ArrayList<ArrayList<ArrayList<Record>>> dryRecords;
    private ArrayList<ArrayList<ArrayList<Record>>> mudRecords; 
    private double[][] dryConfidence;
    private double[][] mudConfidence;


    // Handles Spy Detection
    private ArrayList<Integer> suspects;


    // Handles Spy Implementation

    private int sendCount = 0;
    private int recCount = 0;
     
    private ArrayList<Point> wayPoints;
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        // Initialize parameters

        this._package=false;
        this._target = false;
        this.grid = new int[100][100];
        this.visited = new int[100][100];
        this.explored = new int[100][100];
        this.dryConfidence = new double[100][100];
        this.mudConfidence = new double[100][100];
        this.package_Location = new Point(-1,-1);
        this.target_Location = new Point(-1,-1);
        this.proposedPath = new ArrayList<Point>();
        this.trap_count =  new HashMap<Point,Integer>();
        this.time =0;
        this.unexplored = getRandomUnexplored();
        this.isSpy = isSpy;

        this.wayPoints = new ArrayList<Point>();
        this.suspects = new ArrayList<Integer>();

        // Keep reference of recent obeservation (referenced during getMove call)
        this.lastObservation = new HashMap<Point, CellStatus>();
        this.send_rec = false;

        // Communcation Fusion Initialization
        // this.landRecords = new int[100][100];
        for (int i=0; i < 100; i++) 
       	{
       		for (int j=0; j<100; j++)
       		{
       			int temp = 1;
       		}
       	}

        // Initialize notSeenCount to minGraceTime to initially allow communication with any player
        this.notSeenCount = new HashMap<Integer, Integer>();
        for (int i=0 ; i < n; i++) {
            this.notSeenCount.put(i, minGraceTime);
        }

        // set status of water cells and set unknown cells to muddy
        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                grid[i][j] = -1;
                visited[i][j] = 0;
                dryConfidence[i][j] = 0;
                mudConfidence[i][j] = 0;
            }
        }

        for(int i=0;i<waterCells.size();i++)
        {
            Point tmp = waterCells.get(i);
            visited[tmp.x][tmp.y]= -2;
            grid[tmp.x][tmp.y] = -2;
        }
    
        // create records for sending and bookeeping
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            for (int j = 0; j < 100; j++)
            {
                row.add(null);
            }
        // System.out.println(row);
            this.records.add(row);
        }

        // Initialize dry/mud records 
        this.dryRecords = new ArrayList<ArrayList<ArrayList<Record>>>();
        this.mudRecords = new ArrayList<ArrayList<ArrayList<Record>>>();

        for (int i = 0; i < 100; i++)
        {
            ArrayList<ArrayList<Record>> dryRow = new ArrayList<ArrayList<Record>>();
            ArrayList<ArrayList<Record>> mudRow = new ArrayList<ArrayList<Record>>();
            for (int j = 0; j < 100; j++)
            {
                ArrayList<Record> dryColumn = new ArrayList<Record>();
                ArrayList<Record> mudColumn = new ArrayList<Record>();

                dryRow.add(dryColumn);
                mudRow.add(mudColumn);
            }
            this.dryRecords.add(dryRow);
            this.mudRecords.add(mudRow);
        }
    }
    
    //Observes the vicinity, upfates grid & visited for all visible cells
    // Adds observations to record
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        // Store the current observation for reference in next move command
        lastObservation = statuses;

        this.loc = loc;
        visited[loc.x][loc.y] = 1;
        // System.out.println("Called observe function =========");
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            Record record = records.get(p.x).get(p.y);

            if(status.getC()==0)
                {
                    grid[p.x][p.y] = 0;
                    visited[p.x][p.y] = 1;
                }
            else if(status.getC()==1)
                {
                    grid[p.x][p.y] = -1;
                    visited[p.x][p.y] = 1;
                }

            if(status.getPT()==1)
            {
                grid[p.x][p.y] = 1;
                package_Location.x = p.x;
                package_Location.y = p.y;
                _package =true;
            }
            else if (status.getPT()==2)
            {
                grid[p.x][p.y] = 1;
                target_Location.x = p.x;
                target_Location.y = p.y;
                _target =true;
            }
            // System.out.println(p + " " + status + " " );
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
        }
    }
    
    //Sends records when demanded
    public List<Record> sendRecords(int id)
    {
        this.sendCount++;
        // Mark that player has been been communicated with recently
        this.notSeenCount.put(id, 0);
        // System.out.println("Called sendRecords ======");   
        ArrayList<Record> toSend = new ArrayList<Record>();

        if(loc!=null)
        {
            CellStatus cs = lastObservation.get(loc);
            if(cs.getPresentSoldiers().size() > 1 && this.send_rec)
            {
                for (ArrayList<Record> row : records)
                {
                    for (Record record : row)
                    {
                        if (record != null)
                        {
                            toSend.add(record);
                        }
                    }
                 }

                 unexplored = getRandomUnexplored();
                 this.send_rec = false;

            }
            

        }
        
        return toSend;
    }
    
    // receives records and updates grid &  visited according ro info provided.
    // NOTE:  Right now all information is assumed to be true. We are trusting other players on blind faith.
    // Will have to change in presence of spy
    public void receiveRecords(int id, List<Record> records)
    {
        this.recCount++;
       // Mark that player has been been communicated with recently
        this.notSeenCount.put(id, 0);   

        for(int i=0;i<records.size();i++)
        {
            Record new_record = records.get(i);
            Point p = new_record.getLoc();
            Record curr_record = this.records.get(p.x).get(p.y);

            // Flag when new respective record has been added
            // Use this to detect cases where we had all dry records
            // Then suddenly one record claimed muddy
            // This way we can narrow down who is the spy based on this new record
            boolean dryAdded = false;
            boolean mudAdded = false;

            visited[p.x][p.y] = 1;  // to be changed in case of spy

            if(new_record.getC()==0)
            {
                grid[p.x][p.y] = 0;

                ArrayList<Record> dryClaims = dryRecords.get(p.x).get(p.y);  

                // Add only new records to claim list
                if (!dryClaims.contains(new_record)) {
                	dryClaims.add(new_record);
                	dryAdded = true;
                }
            }
            else if(new_record.getC()==1)
            {
                grid[p.x][p.y] = -1;

                ArrayList<Record> mudClaims = mudRecords.get(p.x).get(p.y);  

                // Add only new records to claim list
                if (!mudClaims.contains(new_record)) {
                	mudClaims.add(new_record);
                	mudAdded = true;
                }
            }

            if(new_record.getPT()==1)
            {
                grid[p.x][p.y] = 1;
                package_Location.x = p.x;
                package_Location.y = p.y;
                _package =true;
            }
            else if (new_record.getPT()==2)
            {
                grid[p.x][p.y] = 2;
                target_Location.x = p.x;
                target_Location.y = p.y;
                _target =true;
            }

            if(curr_record==null)
            {
            curr_record = new Record(new_record);
            this.records.get(p.x).set(p.y, curr_record);

            } 

            else
            curr_record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));


        	// Compute new confidence given this new record
        	// Get confidence by doing mudC
        	// Note: May want to refer to our personal records to tell when a blatant lie is told
        	// Eg. We personally saw (x, y) is dry but a record says muddy
        	ArrayList<Record> dryClaims = dryRecords.get(p.x).get(p.y);
        	ArrayList<Record> mudClaims = mudRecords.get(p.x).get(p.y);

        	// The case where only one record exists with a claim for the cell
        	// IMPORTANT: Never update a cell status if we've personally seen it's status
        	if (dryClaims.size() == 0 && mudClaims.size() == 1) {

        	} else if (dryClaims.size() == 1 && mudClaims.size() == 0) {

        	}

        	// The case where only one record exists for each status (dry or muddy)
        	// IMPORTANT: Never update a cell status if we've personally seen it's status
        	if (dryClaims.size() == 1 && mudClaims.size() == 1) {

        	}

        }

    }
    
    //Proposes the path if on package location
    public List<Point> proposePath()
    {
        //if (proposedPath.size()>1)
        //{
        //    System.out.println(proposedPath);
        //    return proposedPath;
        //}
        if (!this.isSpy)
        {
            if(proposedPath.size()>1)
            {
                return proposedPath;
            }
        }
        else
        {
            List<Point> badPath = new ArrayList<Point>();
            for (int i = 0; i < proposedPath.size(); i++)
            {
                if (i != proposedPath.size() - 2)
                {
                    badPath.add(proposedPath.get(i));
                }
            }
            System.out.println(badPath);
            return badPath;
        }
        return null;
    }
    
    // Vote for proposed paths
    //NOTE: Currently trusting all paths proposed by the players. Assuming the correctness of their implementations
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        ArrayList<Integer> toReturn = new ArrayList<Integer>();

        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            int _id = entry.getKey();
            List<Point> _path = entry.getValue();
            if (isValidPath(_path) && !this.isSpy)
            {
                toReturn.add(_id);
            }
            return toReturn;
        }
        return null;
    }

    public Boolean isValidPath(List<Point> _path)
    {
        int _path_size = _path.size();

        Point prev = _path.get(0);
        Point next;
        Record record;
        int status;
        for (int i = 0; i < _path_size; i++)
        {
            next = _path.get(i);
            record = records.get(next.x).get(next.y);
            status = record.getPT();

            if (i == 0)
            {
                // check first cell is the package location
                if (status != 1)
                {
                    //System.out.println("FAIL1");
                    return false;
                }
            }
            else if (i == _path_size - 1)
            {
                // check last cell is the target location 
                if (status != 2)
                {
                    //System.out.println("FAIL2");
                    return false;
                }
            }
            else
            {
                // check all other cells is clear
                if (status != 0)
                {
                    //System.out.println("FAIL3");
                    return false;
                }
            }

            if ((Math.abs(next.x - prev.x) > 1) || (Math.abs(next.y - prev.y) > 1))
            {
                // check it's actually a path
                //System.out.println("FAIL4");
                return false;
            }    
            prev = next;
        }
        return true;
    }
    
    // No idea what this is for
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        // System.out.println("Called receiveResults Command ======= " + recCount + " and " + sendCount);
    }

    private void setWayPoints()
    {
        Point wp1 = new Point(0,99);
        Point wp2 = new Point(99,99);
        Point wp3 = new Point(99,0);
        Point wp4 = new Point(0,0);
        Point wp5 = new Point(50,50);

        Point wp6 = new Point(50,82);
        Point wp7 = new Point(50,17);
        Point wp8 = new Point(82,50);
        Point wp9 = new Point(17,50);

        wayPoints.add(wp1);
        wayPoints.add(wp2);
        wayPoints.add(wp3);
        wayPoints.add(wp4);
        wayPoints.add(wp5);
        wayPoints.add(wp6);
        wayPoints.add(wp7);
        wayPoints.add(wp8);
    }



    // Gets the nearest unvisited cell. This is done inorder to explore new areas with minimal repetition
    private Point getNearestUnExplored(Point curr)
    {

        double min_dist = Integer.MAX_VALUE;
        Point next_move = new Point(-2000,-2000);

        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                if(grid[i][j]==-2 || visited[i][j]==1) continue;

                double dist_curr = Math.abs(curr.x-i) + Math.abs(curr.y-j) - grid[i][j];

                if(dist_curr<min_dist)
                {
                    min_dist = dist_curr;
                    next_move = new Point(i,j);
                }
            }
        }
        return next_move;

    }

    //Move to a random unvisited cell if trapped. Intended as tie breaker
    private Point getRandomUnexplored()
    {
        Random rand = new Random();
        int n = rand.nextInt(50);

        for(int i=n;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                if(grid[i][j]==-2 || visited[i][j]==1) continue;

                return new Point(i,j);
            }
        }

        return new Point(-1000,-1000);
    }


    //Dijkstra'a shortest path algorithm to find shortest path from loc to destination.
    // if safe set to true it finds the shortest path from normal cells only
    // if sade set to false, it finds rge shortest path overall including muddy cells 
    private Point getNextOnPath(Point loc,Point destination,Boolean safe)
    {
        HashMap<Point, Double> dist = new HashMap<Point, Double>();
        HashMap<Point, Point> parent = new HashMap<Point, Point>();

        Boolean found = false;

        //
    // reset exploration matrix before searching for next exploration site
    //
        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
                explored[i][j] = 0;
        }

        for(int i=0;i<100;i++)
        {
            for(int j=0;j<100;j++)
            {
                dist.put(new Point(i,j),Double.POSITIVE_INFINITY);
            }
        }

        dist.put(loc,0.0);

        PriorityQueue<Entry> q = new PriorityQueue<>();
        Entry s = new Entry(0.0,loc);
        q.add(s);

        
        while(q.peek()!=null && !found)
            {
                Entry tmp = q.poll();
                Point next = tmp.p;
                explored[next.x][next.y]=1;
                for(int i = next.x-1;i<=next.x+1;i++)
                {
                    for(int j = next.y-1;j<=next.y+1;j++)
                    {
                        double diff = Math.abs(next.x-i) + Math.abs(next.y-j);
                        Double val = Double.POSITIVE_INFINITY; 

                        if(i < 0 || i>=100 || j<0 || j>=100 || explored[i][j]==1 || grid[i][j]==-2) continue;
                        if(safe && grid[i][j]<0) continue;

                        if(diff>1)
                            val = tmp.key + 1.5 - 2*grid[i][j];
                        else
                            val = tmp.key + 1 - 2*grid[i][j];

                        Point pt = new Point(i,j);
                        Double distance = dist.get(pt);


                        if(val<distance)
                        {
                            dist.put(pt,val);
                            parent.put(pt,next);
                            Entry new_entry = new Entry(val,pt);
                            q.add(new_entry);
                        }

                        if(destination.x == i && destination.y ==j)
                           {
                            found =true;
                            //System.out.println("location is  " + loc + " destination is " + destination);
                            //System.out.println("found the destination at distance "  + val);
                        }

                             
                    }

                }
            }


            Point next = new Point(destination);
            Point prev = new Point(-1000,-1000);

            if(!found_path)
            proposedPath.clear();

            while(parent.get(next)!=null)
            {
                //System.out.println(next);
                prev = new Point(next.x,next.y);
                if(!found_path)
                proposedPath.add(0,new Point(prev.x,prev.y));
                next = new Point(parent.get(next));
                
            }

            //System.out.println("next move point is "  + prev);
            return prev;


    }

    public String getOrientation(Point me, Point other){
        String orientation = "same point";
        int yDiff = me.y - other.y;
        if (yDiff > 0) {
            orientation = "n";
        } else if (yDiff <0 ){
            orientation = "s";
        } else {
            orientation = "";
        }

        int xDiff = me.x - other.x;
        if (xDiff > 0) {
            orientation = orientation + "e";
        } else if (xDiff < 0) {
            orientation = orientation + "w";
        }

        return orientation;
    }
    //Computes the next move    
    public Point getMove(){
        time++;

        // Increment not seen counts of all peers by 1
        for (int i : notSeenCount.keySet()) {
            this.notSeenCount.put(i, this.notSeenCount.get(i) + 1);
        }

        visited[loc.x][loc.y] = 1; //mark current location as visited
        Point move = new Point(-1000,-1000);


        // Communication protocol, check if soldier is near (HashMap cleared every round)
        // Add soldiers in range to this HashMap
        nearbySoldiers = new HashMap<Integer, Point>();

        // Iterate through recent observation radius points and get nearby peers
        for (Point p: lastObservation.keySet()) {
            CellStatus cs = lastObservation.get(p);
            
            if ((cs.getPresentSoldiers().size() > 0) && (!p.equals(this.loc))) {
                
                // Add all in-range players to nearbySoldiers HashMap
                for (int peerID : cs.getPresentSoldiers()) 
                {
                    // Only consider eligible soldiers (Have not been recently contacted)
                    if (notSeenCount.get(peerID) > minGraceTime) {
                        nearbySoldiers.put(peerID, p);
                    }
                    //System.out.println(this.id + " Spotted soldier: " + peerID + " at location " + p + "=================================");
                }
            }
        }

        // Discern lowest ID player in vicinity
        int minID = 99999;
        for (int peerID : nearbySoldiers.keySet()) {
            if ( peerID < minID ) {
                minID = peerID;
                this.send_rec = true;
            }
        }

        // No soldier in range
        if (minID == 99999) {
            idleCount = 0;
            followCount = 0;
            this.send_rec = false;
        // Soldiers near, use communication protocol
        } else {
            // Detect new targetPeer
            if (targetPeer == -1) {
                targetPeer = minID;
                idleCount = 0; 
                followCount = 0;
            // Detect if targetPeer remained the same as last round
            } else if (targetPeer == minID) {
                followCount++;
                idleCount++;
            // Update to new move/wait target 
            } else {
                targetPeer = minID;
                // Allow idle time for player to approach/be contacted
                idleCount = 0;
                followCount = 0;
            }

            Point posToMove = new Point(0, 0);

            if (targetPeer < this.id) {
                moveToSoldier = true;
                posToMove = nearbySoldiers.get(minID);

                stayStill = false;
            } else {
                stayStill = true;
                moveToSoldier = false;
            }

            // Move towards lowest ID player in range
            if (moveToSoldier && (followCount < maxCount) && !found_path ) {
                return getNextOnPath(this.loc, posToMove, false);
            } else if (moveToSoldier && (followCount >= maxCount) )  {
                followCount = 0;
                targetPeer = -1;
                // May want to add following line if constantly chasing same player
                // (Other player is not following protocol)
                this.notSeenCount.put(targetPeer, 0);
            }

            // Wait to be contacted for maxCount turns 
            if (stayStill && (idleCount < maxCount) && !found_path) {
                return new Point(0, 0);     
            } else if (stayStill && (idleCount >= maxCount) ) {
                idleCount = 0;
                targetPeer = -1;
                // May want to add following line if constantly chasing same player
                // (Other player is not following protocol)
                this.notSeenCount.put(targetPeer, 0);
            }
            
        }

        //
        // If target and package have been located, try to find a safe path between them. If found set found_path to true
        //
        if(_target && _package)
        {
            //wait
            if(!found_path)
            {
                proposedPath.clear();
                
                Point start = package_Location;
                getNextOnPath(start,target_Location,true);

                if(proposedPath.size()>1)

                {
                    Point reach_pt = proposedPath.get(proposedPath.size()-1);
                Point next_pt = proposedPath.get(0);

                proposedPath.add(0,start);

                int diff = Math.abs(Math.abs(start.x - next_pt.x) - Math.abs(start.y - next_pt.y));

                if(reach_pt.x==target_Location.x && reach_pt.y == target_Location.y && diff<=1)
                   { 
                    found_path = true;
                    //System.out.println("--------------------------------package at location:" + package_Location + " target at location " + target_Location + " proposed path is ----------------------------------------");
                        for(int i=0;i<proposedPath.size();i++)
                        {
                            //System.out.println(proposedPath.get(i));
                        }
                   }
            }
                
            }
            //announce shortest path
        }

        //
        // if a safe path has been found, proceed to the package on the shortest path from current location
        //
        if(_target && _package && found_path && (loc.x!=package_Location.x || loc.y!=package_Location.y))
        {
            //go to package
            Point next = getNextOnPath(loc,package_Location,false);
            move = next;
            //System.out.println("location is " + loc + " moving to " + move );
            int x  = move.x - loc.x;
            int y = move.y - loc.y;

            
            return new Point(x,y);
        }
        //
        //If safe path has been found from package to target and you are package location, then wait and announce proposed path.
        //Also vote for appropriate paths
        //
        else if(_target && _package && found_path)
        {
            return new Point(0,0);
        }
        //
        // If you are enroute to the next unexplored cell and haven't reached it then continue along the shortest path to that cell
        //
        if(unexplored.x>=0 && unexplored.y>=0 && visited[unexplored.x][unexplored.y]!=1)
        {
            Point next = getNextOnPath(loc,unexplored,false);
                move = next;
                int x  = move.x - loc.x;
                int y = move.y - loc.y;
                // System.out.println("moving to closest unexplored from " + loc + " moving to " + unexplored + "via "  + move );
                // System.out.println("the cell condition for " + move +   " is  " + grid[move.x][move.y] );

                if(x>=-1 && y>=-1)
                return new Point(x,y);
        }
        //
        //If you have reached the last unexplored cell, then find the next nearest unexplored cell. Set unexplored to next site
        //
        Point next_loc = getNearestUnExplored(loc);
        unexplored = next_loc;

        //
        //get next move for new unexplored site
        //
        
        Point next = getNextOnPath(loc,next_loc,false);

        //
        // maintain a trap count for current location
        //
        if(trap_count.containsKey(next))
        {
            trap_count.put(next,trap_count.get(next)+1);
        }
        else
        {
            trap_count.put(next,0);
        }
        //
        //if you have visited the same site more than 10 times, then probably trapped. Select a random unexplored cell and proceed towards that to break free.
        //
        if(trap_count.get(next)<10)
        {
            move = next;
            int x  = move.x - loc.x;
            int y = move.y - loc.y;
             if(x>=-1 && y>=-1)
                return new Point(x,y);
        }
        else
        {
            unexplored = getRandomUnexplored();
            move = getNextOnPath(loc,unexplored,false);
                int x  = move.x - loc.x;
                int y = move.y - loc.y;

                if(x>=-1 && y>=-1)
                return new Point(x,y);

        }

        //
        //This basically should never happem. It implies that you visited all possible cells and still found no valid path!
        //
        return move;
    }
}
