package spy.g4;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.lang.Math;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class Player implements spy.sim.Player {
    
    private ArrayList<ArrayList<Record>> records;
    private int id;
    private Point loc;
    public Point sharedLoc;
    private int numPlayers;
    private int totalTime;
    private List<Point> waterCells;

    
    private HashMap<Point, Record> trueRecords;
    private Queue<Point> pathToPackage;

    private HashMap<Point, CellStatus> previousStatuses;
    private HashMap<String, ArrayList<Point>> observableOffsets;
    private ArrayList<ArrayList<Boolean>> observedCells;


    private boolean packageKnown = false;
    private boolean targetKnown = false;
    private boolean pathKnown = false;
    private boolean moveToSoldier = false;
    private boolean stayPut = false;
    //private boolean recordsReceived = false;
    private int stayPutCounts = 0;
    private static final int maxWaitTime = 8;

    private static boolean movingToPackage = false;
    private static boolean waitingAtPackage = false;

    private List<ExploreMovement> moves;
    private Point nearbySoldier;

    private ArrayList<Point> maxScores;
    
    private int lastPlayerComm;
    private static final int timeToComm = 15;

    private Random rand;

    // Spy variables
    private boolean isSpy;
    private int spy = -1; // player who we think is the spy
    private HashMap<Integer, HashSet<Point>> possibleSpies; // each players mapped to suspicion count
    private HashMap<Integer, Integer> suspicionScore;

    private boolean isInMap(Point pt) {
        if ((pt.x > -1) && (100 > pt.x) && (pt.y > -1) && (100 > pt.y)) return true;
        return false;
    }
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.numPlayers = n;
        this.totalTime = t;
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        this.observedCells = new ArrayList<ArrayList<Boolean>>();
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            ArrayList<Boolean> row2 = new ArrayList<Boolean>();
            for (int j = 0; j < 100; j++)
            {
                row.add(null);
                row2.add(false);
            }
            this.records.add(row);
            this.observedCells.add(row2);
        }

        //System.out.println("vc.get(0).get(0) = " + observedCells.get(0).get(0));

        this.isSpy = isSpy;
        this.waterCells = waterCells;

        //System.out.println("WATER!!! " + waterCells.size());

        pathToPackage = new LinkedList<Point>();
        trueRecords = new HashMap<Point, Record>();
        possibleSpies = new HashMap<Integer, HashSet<Point>>();
        //suspicionScore = new HashMap<Integer, Integer>();
        previousStatuses = new HashMap<Point, CellStatus>();
        
        moves = new ArrayList<ExploreMovement>();
        moves.add(new ExploreMovement());

        nearbySoldier = new Point(0, 0);

        //maxScores = new ArrayList<Point>();

        //rand = new Random();

    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {

        previousStatuses = statuses;
        //recordsReceived = false;

        this.loc = loc;
        this.sharedLoc = loc;
        //System.out.println("I am at " + loc);

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            //update observedCells
            observedCells.get(p.x).set(p.y, true);
            CellStatus status = entry.getValue();
            if (status.getPT() == 1) {packageKnown = true; /*System.out.println(this.id + " knows where PACKAGE is!!");*/}
            else if (status.getPT() == 2) {targetKnown = true; /*System.out.println(this.id + " knows where TARGET is!!");*/}
            Record record = records.get(p.x).get(p.y);
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
                trueRecords.put(p, record);
                //System.out.println("observed a record at " + p);
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));

            // determine if there is a soldier to talk to
            if ((Simulator.getElapsedT() > timeToComm) && (Math.abs(this.loc.x - p.x) == 1) && (Math.abs(this.loc.y - p.y) == 1)) {
                if (status.getPresentSoldiers().size() > 0 && (!p.equals(this.loc))) {
                    for (int soldID : status.getPresentSoldiers()) {
                        if (soldID != lastPlayerComm)
                            if (soldID > this.id) stayPut = true;
                            else {moveToSoldier = true; nearbySoldier = p;}
                    }
                }
            }
        }

        //if (!stayPut && !moveToSoldier) moves.add(new ExploreMovement());
        if (!stayPut && !moveToSoldier) {
            moves.clear();
            moves.add(new ExploreMovement());
        }

        // ##########################
        // ### call CALCULATEPATH ###
        // ##########################
        calculatePath();
    }
    
    public List<Record> sendRecords(int id)
    {
        System.out.println("> " + this.id + " SENDS to " + id + " <");
        ArrayList<Record> toSend = new ArrayList<Record>();
        for (ArrayList<Record> recarray : records) {
            for (Record ourRecord : recarray) {
                if (ourRecord != null) {
                    ArrayList<Observation> observations = ourRecord.getObservations();
                    if ((observations.size() > 1) && (observations.get(observations.size() - 1).getID() != this.id)) {
                        observations.add(new Observation(this.id, Simulator.getElapsedT()));
                    }
                    toSend.add(ourRecord);
                }
            }
        }
        System.out.println("length of records to send = " + toSend.size());
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> records)
    {
        System.out.println("> " + this.id + " RECEIVES from " + id + " <");

        //recordsReceived = true;

        // Assuming no spies
        int numRecs = 0;
        for (ArrayList<Record> rL : this.records) {
            for (Record r : rL) {
                if (r!=null) {numRecs += 1;}
            }
        }
        System.out.println("< initial length of records = " + numRecs);
        for (Record recR : records) {

            if (recR != null) {
                
                if (recR.getPT() == 1) {packageKnown = true; System.out.println(this.id + " knows where PACKAGE is!!");}
                else if (recR.getPT() == 2) {targetKnown = true; System.out.println(this.id + " knows where TARGET is!!");}

                Point p = recR.getLoc();
                Record ourRecord = this.records.get(p.x).get(p.y);

                if (ourRecord == null) {
                    ourRecord = new Record(p, recR.getC(), recR.getPT(), recR.getObservations());
                    this.records.get(p.x).set(p.y, ourRecord);
                }
            }
        }
        numRecs = 0;
        for (ArrayList<Record> rL : this.records) {
            for (Record r : rL) {
                if (r!=null) {numRecs += 1;}
            }
        }
        System.out.println(">>>> new length of records = " + numRecs);

        lastPlayerComm = id;

        // If we are in the chain of observations
        // If we are first in the chain: compare information against our own obswerved records
        // If not first in chain: compare sequence of observations with the corresponding sequence of observations in records
        
    }

    private int shorterPath(List<Point> path1, List<Point> path2) {
        int path1_diff = 0;
        int path2_diff = 0;

        if(path1.size() < path2.size()) {
            return 1;
        } else if (path1.size() > path2.size()) {
            return 2;
        } else {

            for(int i = 0; i < path1.size() - 1; i++) {
                Point a = path1.get(i);
                Point b = path1.get(i + 1);
                int diff = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
                path1_diff = path1_diff + diff;
            }

            for(int i = 0; i < path2.size() - 1; i++) {
                Point a = path2.get(i);
                Point b = path2.get(i + 1);
                int diff = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
                path2_diff = path2_diff + diff;
            }
        }
        if (path1_diff < path2_diff) {
            return 1;
        } else if (path1_diff > path2_diff) {
            return 2;
        } else { // same length return whatever
            return 1;
        }


    }

    public List<Point> calculatePath() {
        
        // ##########################
        // ### Quincy's code here ###
        // ##########################

        System.out.println("###########CALCPATH###########################################");

        List<Point> finalPath = new ArrayList<Point>();

        // if there is a complete path, set pathKnown to true

        /*if (!targetKnown || !packageKnown) {
            pathKnown = false;
            System.out.println(targetKnown);
            System.out.println(packageKnown);
            System.out.println("# Package / target unknown #####################################################\n");
            return null;
        }*/

        //private HashMap<Point, Record> trueRecords;
        // find the package and target position first
        Point tar = null;
        Point pac = null;

        for (Point key : trueRecords.keySet()) {
            int pt = trueRecords.get(key).getPT();
            if (pt == 1) { /* package location */
                System.out.println("Found pac");
                pac = key;
            }
            if (pt == 2) { /* target location */
                System.out.println("Found tar");
                tar = key;
            }
            if (tar != null && pac != null) {
                break;
            }
        }

        if(tar == null || pac == null) {
            pathKnown = false;
            //System.out.println(targetKnown);
            //System.out.println(packageKnown);
            System.out.println("# Package / target unknown #####################################################\n");
            return null;
        }

        /* perform BFS */
        /* visited contains the point and the path that took to get to that point */
        HashMap<Point, List<Point>> visited = new HashMap<Point, List<Point>>();
        /* keeps track of parent / children pairs */
        List<List<Point>> queue = new ArrayList<List<Point>>();
        Boolean goal_reached = false;

        List<Point> tempList = new ArrayList<Point>();
        tempList.add(pac);
        tempList.add(null);
        queue.add(tempList);

        while (true) {
            /* dequeue and set to current */
            if (queue.size() == 0 && goal_reached == false) {
                break;
            }
            List<Point> temp = queue.get(0);
            queue.remove(temp);
            Point current = temp.get(0);
            Point parent = temp.get(1);

            //System.out.println(current == null);
            //System.out.println(tar == null);
            /*if (parent != null) {
                System.out.println("parent: (" + parent.x + ", " + parent.y +")");
            } else {
                System.out.println("parent is null");
            }
            System.out.println("current: (" + current.x + ", " + current.y +")");*/
            /* goal test */
            if (current.equals(tar)) {
                //System.out.println("goal reached");
                goal_reached = true;
            }
            /* add to visited */
            List<Point> path = null;
            if (parent == null) {
                path = new ArrayList<Point>();
                path.add(current);
            } else {
                path = new ArrayList<Point>(visited.get(parent));
                path.add(current);
            }
            /*if (visited.get(current) != null && visited.get(current).size() < path.size()) {
                //System.out.println("Short path exist");
            } else {
                visited.put(current, path);
            }*/
            if (visited.get(current) != null && shorterPath(visited.get(current), path) == 1) {
                // don't do anything
            } else {
                visited.put(current, path);
            }

            /*System.out.println("added to visited");
            System.out.println("Path added for (" + current.x + ", " + current.y +")");
            for (Point x : path) {
                System.out.print("(" + x.x + ", " + x.y +"), ");
            }
            System.out.println("End path\n");*/
            /* if goal test successful */
            if (goal_reached == true) {
                System.out.println("goal reached break");
                break;
            }
            /* adds all children that's not visited to queue
             * only adds children that are normal condition */
            int x = current.x;
            int y = current.y;
            //List<Point> children = new ArrayList<Point>();
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    //System.out.println("Doing offset");
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    Point child = new Point(x + i, y + j);
                    /* check condition */
                    if (trueRecords.get(child) == null) {
                        //System.out.println("child is null");
                        continue;
                    }
                    if (trueRecords.get(child).getC() != 0) {
                        continue;
                    }
                    //System.out.println("child is NOT null");
                    /* check visited */
                    Boolean visited_child = false;
                    for (Point p : visited.keySet()) {
                        if (p.equals(child)) {
                            //System.out.println("child visited");
                            visited_child = true;
                        }
                    }
                    if (visited_child == true) {
                        continue;
                    }

                    tempList = new ArrayList<Point>();
                    tempList.add(child);
                    tempList.add(current);
                    queue.add(tempList);
                }
            }
        }
        
        if (goal_reached == false) {
            finalPath = null;
        } else {
            pathKnown = true;
            pathToPackage = calculatePath(loc, pac, 1);
            //Point test = new Point(0, 3);
            //pathToPackage = calculatePath(test, pac, 1);
            finalPath = visited.get(tar);
        }


        if (finalPath != null) {
            for(Point x: finalPath) {
                System.out.print("(" + x.x + ", " + x.y + "),");
            }
        } else {
            System.out.println("finalPath is null");
        }
        System.out.println("###########END###########################################\n");

        return finalPath;
    }

    private Queue<Point> calculatePath(Point loc_arg, Point pac_arg, int muddyOkay) {
        // ##########################
        // ### Quincy's code here ###
        // ##########################

        System.out.println("###########LOC - CALCPATH###########################################");

        Queue<Point> finalPath = new LinkedList<Point>();

        // if there is a complete path, set pathKnown to true

        /*if (!targetKnown || !packageKnown) {
            pathKnown = false;
            System.out.println(targetKnown);
            System.out.println(packageKnown);
            System.out.println("# Package / target unknown #####################################################\n");
            return null;
        }*/

        //private HashMap<Point, Record> trueRecords;
        // find the package and target position first
        Point tar = pac_arg;
        Point pac = loc_arg;

        /*for (Point key : trueRecords.keySet()) {
            int pt = trueRecords.get(key).getPT();
            if (pt == 1) { // package location 
                System.out.println("Found pac");
                pac = key;
            }
            if (pt == 2) { // target location 
                System.out.println("Found tar");
                tar = key;
            }
            if (tar != null && pac != null) {
                break;
            }
        }

        if(tar == null || pac == null) {
            pathKnown = false;
            //System.out.println(targetKnown);
            //System.out.println(packageKnown);
            System.out.println("# Package / target unknown #####################################################\n");
            return null;
        }*/

        /* perform BFS */
        /* visited contains the point and the path that took to get to that point */
        HashMap<Point, List<Point>> visited = new HashMap<Point, List<Point>>();
        /* keeps track of parent / children pairs */
        List<List<Point>> queue = new ArrayList<List<Point>>();
        Boolean goal_reached = false;

        List<Point> tempList = new ArrayList<Point>();
        tempList.add(pac);
        tempList.add(null);
        queue.add(tempList);

        while (true) {
            /* dequeue and set to current */
            if (queue.size() == 0 && goal_reached == false) {
                break;
            }
            List<Point> temp = queue.get(0);
            queue.remove(temp);
            Point current = temp.get(0);
            Point parent = temp.get(1);

            //System.out.println(current == null);
            //System.out.println(tar == null);
            /*if (parent != null) {
                System.out.println("parent: (" + parent.x + ", " + parent.y +")");
            } else {
                System.out.println("parent is null");
            }
            System.out.println("current: (" + current.x + ", " + current.y +")");*/
            /* goal test */
            if (current.equals(tar)) {
                System.out.println("goal reached");
                goal_reached = true;
            }
            /* add to visited */
            List<Point> path = null;
            if (parent == null) {
                path = new ArrayList<Point>();
                path.add(current);
            } else {
                path = new ArrayList<Point>(visited.get(parent));
                path.add(current);
            }
            /*
            if (visited.get(current) != null && visited.get(current).size() < path.size()) {
                //System.out.println("Short path exist");
            } else {
                visited.put(current, path);
            }*/
            if (visited.get(current) != null && shorterPath(visited.get(current), path) == 1) {
                // don't do anything
            } else {
                visited.put(current, path);
            }

            /*System.out.println("added to visited");
            System.out.println("Path added for (" + current.x + ", " + current.y +")");
            for (Point x : path) {
                System.out.print("(" + x.x + ", " + x.y +"), ");
            }
            System.out.println("End path\n");*/
            /* if goal test successful */
            if (goal_reached == true) {
                System.out.println("goal reached break");
                break;
            }
            /* adds all children that's not visited to queue
             * only adds children that are normal condition */
            int x = current.x;
            int y = current.y;
            //List<Point> children = new ArrayList<Point>();
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    //System.out.println("Doing offset");
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    Point child = new Point(x + i, y + j);
                    /* check condition */

                    if (muddyOkay == 0) { // if muddy is not okay
                        if (trueRecords.get(child) == null) {
                            //System.out.println("child is null");
                            continue;
                        }
                        if (trueRecords.get(child).getC() != 0) {
                            continue;
                        }
                    } else {
                        if (child.x < 0 || child.y < 0 || child.x > 99 || child.y > 99) {
                            continue;// point invalid
                        }
                        Boolean inWater = false;
                        for (Point w : waterCells) {
                            if (w.equals(child)) {
                                inWater = true;
                                break;
                            }
                        }
                        if (inWater == true) {
                            continue;
                        }
                    }
                    //System.out.println("child is NOT null");
                    /* check visited */
                    Boolean visited_child = false;
                    for (Point p : visited.keySet()) {
                        if (p.equals(child)) {
                            //System.out.println("child visited");
                            visited_child = true;
                        }
                    }
                    if (visited_child == true) {
                        continue;
                    }

                    tempList = new ArrayList<Point>();
                    tempList.add(child);
                    tempList.add(current);
                    queue.add(tempList);
                }
            }
        }
        
        if (goal_reached == false) {
            finalPath = null;
        } else {
            //pathKnown = true;
            //pathToPackage = calculatePath(loc, pac);
            //finalPath = visited.get(tar);
            for (Point p : visited.get(tar)) {
                finalPath.add(p);
            }
        }


        if (finalPath != null) {
            for(Point x: finalPath) {
                System.out.print("(" + x.x + ", " + x.y + "),");
            }
        } else {
            System.out.println("finalPath is null");
        }
        System.out.println("###########END###########################################\n");

        return finalPath;
    }
    
    public List<Point> proposePath()
    {
        return null;
    }

    // getVotes() gets as input all the proposed paths and a list of corresponding player IDs
    // it returns the list of player IDs who propose verified paths (in agreement with our records of the cells)
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    { 
        // list of players we agree with 
        ArrayList<Integer> toReturn = new ArrayList<Integer>(); 
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        { 
            // if player proposed a valid path
            if (this.isValidPath(entry.getValue())) { 
                toReturn.add(entry.getKey());
            } 
        }
        return toReturn;
    }

    // ** ASSUMES proposed path = [ package location, ... (list of clear cells), target location ]
    // isValidPath() gets as input a proposed path from getVotes()
    // it returns a boolean, true if path is valid  
    private boolean isValidPath(List<Point> proposedPath) {
        int f = proposedPath.size() - 1; 
        int i = 0;
        for (Point point : proposedPath) {
            Record record = records.get(point.x).get(point.y);
            // matching record must exist and cell condition must be clear (0) 
            if (record == null || record.getC() != 0) {
                return false;
            }
            if (i == 0) {
                // package location 
                if (record.getPT() != 1) {
                    //System.out.println(record.getPT());
                    i++;
                    return false;
                }
            } else if (i == f) {
                // target location 
                if (record.getPT() != 2) {
                    //System.out.println(record.getPT());
                    return false;
                }
            } else {
                // ordinary cell 
                i++;
                if (record.getPT() != 0) {
                    //System.out.println(record.getPT());
                    return false; 
                }
            }
        } // end of for loop
        return true; // if all passed 
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        
    }

    public Point getMove() 
    {
        //possibleMoves.clear();
        //radialInfo.clear();
        //nearbySoldiers.clear();

        if (waitingAtPackage == true) {

            return new Point(0, 0);

        } else if (pathKnown) {
            
            System.out.println(this.id + " movement for KNOWN PATH");
            // move to the package
            if ((pathToPackage != null) && (!pathToPackage.isEmpty())) {
                movingToPackage = true;
                return pathToPackage.remove();
            
            } else if (pathToPackage.isEmpty() && movingToPackage == true) {
                waitingAtPackage = true;
                System.out.println("waitingAtPackage");
            }
            //return new Point(0,0);
        
        } else if (packageKnown || targetKnown) {
            
            System.out.println(this.id + " movement for KNOWN PACKAGE or TARGET");
            //calculatePath()
            return moves.get(0).nextMove();
        
        } //else {

        if (stayPut) {
            // stay put for 2 time counts
            System.out.println(this.id + " stay put");
            if ((stayPutCounts < maxWaitTime)) {//&& (!recordsReceived)) {
                ++stayPutCounts;
                return new Point(0,0);
            }
            stayPutCounts = 0;
            stayPut = false;
        }

        if (moveToSoldier) {
            // move in the direction of a soldier
            System.out.println(this.id + " move to soldier");
            System.out.println(nearbySoldier);
            moveToSoldier = false;
            if (nearbySoldier.x != 0 || nearbySoldier.y != 0) return nearbySoldier;
        }

        System.out.println(this.id + " movement for EXPLORATION");

        // Explore randomly
        System.out.println("moves: " + moves.size());
        return moves.get(0).nextMove();




        //} remove the last outer else
    }

    /* Credit to g6 for their implementation of getMove which helped consolidate our methods of calcuating heuristics */
    public class ExploreMovement {
        
        private LinkedList<Point> possibleMoves;

        private double mudCost = 0.5;
        private double clearCost = 0.75;
        private double unobservedCost = 1.0;

        private double numMuddy = 0, numClear = 0, numUnobserved = 0;

        private Point lastMove = new Point(0, 0);

        public ExploreMovement() {
            possibleMoves = new LinkedList<Point>();
        }

        public boolean isCompleted() {
            return possibleMoves.isEmpty();
        }

        public double calcScore(Point offP) {

            Point loc = sharedLoc;

            int newX = loc.x + offP.x;
            int newY = loc.y + offP.y;

            if (waterCells.contains(new Point(newX, newY)))
                // it is a water cell
                return -1;
            if ((newX < 0) || (99 < newX) || (newY < 0) || (99 < newY))
                // it is off of the map
                return -1;
            if (newX == 0 && newY == 0)
                return -1;

            HashSet<Point> points = new HashSet<Point>();
            if (offP.x != 0) {
                points.add(new Point(loc.x + 3 * offP.x, loc.y - 1));
                points.add(new Point(loc.x + 3 * offP.x, loc.y + 1));
                points.add(new Point(loc.x + 4 * offP.x, loc.y + offP.y));
                points.add(new Point(loc.x + 3 * offP.x, loc.y - 2 + offP.y));
                points.add(new Point(loc.x + 3 * offP.x, loc.y + 2 + offP.y));
            }
            else {
                points.add(new Point(loc.x + 3, loc.y + offP.y));
                points.add(new Point(loc.x - 3, loc.y + offP.y));
            }
            
            if (offP.y != 0) {
                points.add(new Point(loc.x - 1, loc.y + 3 * offP.y));
                points.add(new Point(loc.x + 1, loc.y + 3 * offP.y));
                points.add(new Point(loc.x + offP.x, loc.y + 4 * offP.y));
                points.add(new Point(loc.x - 2 + offP.x, loc.y + 3 * offP.y));
                points.add(new Point(loc.x + 2 + offP.x, loc.y + 3 * offP.y));
            }
            else {
                points.add(new Point(loc.x + offP.x, loc.y + 3));
                points.add(new Point(loc.x + offP.x, loc.y - 3));
            }

            for (Point p : points) {

                if (isInMap(p) && !waterCells.contains(p) && !observedCells.get(p.x).get(p.y)) {
                    ++numUnobserved;

                    if ((trueRecords.get(p) != null) && (trueRecords.get(p).getC() == 0)) numClear += 1;
                    else if ((trueRecords.get(p) != null) && (trueRecords.get(p).getC() == 1)) numMuddy += 1;
                }
                //System.out.println(p + " isWater? " + waterCells.contains(p));    
            }

            //System.out.println(clearCost + "*" + numClear + " + " + mudCost + "*" + numMuddy + " + " + unobservedCost + "*" + numUnobserved);
            return clearCost*numClear + mudCost*numMuddy + unobservedCost*numUnobserved;
        } 

        public Point nextMove() {
            
            if (possibleMoves != null && !possibleMoves.isEmpty()) {
                Point p = new Point(possibleMoves.getFirst().x - loc.x, possibleMoves.getFirst().y - loc.y);
                possibleMoves.removeFirst();
                return p;
            }
            
            // update possible moves with new information
            int maxX = 0, maxY = 0;
            double maxScore = -1.0;
            //maxScores.clear();
            
            for (int offX = -1; offX <= 1; ++offX) {
                for (int offY = -1; offY <= 1; ++offY) {
                    double score = calcScore(new Point(offX, offY));
                    if (score > maxScore) {
                        maxX = offX;
                        maxY = offY;
                        maxScore = score;
                    }
                    /*if (score == maxScore) {
                        maxScores.add(new Point(offX, offY));
                        System.out.println(new Point(offX, offY) + " same max of " + score);
                        maxScore = score;
                    } else if (score > maxScore) {
                        maxScores.clear();
                        maxScores.add(new Point(offX, offY));
                        System.out.println(new Point(offX, offY) + " new max of " + score);
                        maxScore = score;
                    }*/
                }
            }

            //System.out.println("maxScores.size() = " + maxScores.size());

            //int iMax = rand.nextInt(maxScores.size());
            //System.out.println("iMax = " + iMax);
            //Point np = maxScores.get(iMax);
            //System.out.println("np: " + np);

            lastMove = new Point(maxX, maxY);
            //lastMove = new Point(np);
            //System.out.println("maxScore = " + maxScore);
            return new Point(maxX, maxY);
            //return new Point(np);
        }
    }
}

// FIX TIE BREAKING STRATEGY FOR BEST PATH (keep track of paths with same best values and randomize selection)
