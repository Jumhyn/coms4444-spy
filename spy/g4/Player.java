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
    private int numPlayers;
    private int totalTime;

    private boolean isSpy;
    private List<Point> waterCells;

    private int spy = -1; // player who we think is the spy
    private HashMap<Integer, HashSet<Point>> possibleSpies; // each players mapped to suspicion count
    //private HashMap<Integer, Integer> suspicionScore;
    private HashMap<Point, Record> trueRecords;

    private HashMap<Point, CellStatus> previousStatuses;
    private Queue<Point> pathToPackage;
    private boolean packageKnown = false;
    private boolean targetKnown = false;
    private boolean pathKnown = false;
    private HashMap<String, Point> possibleMoves;
    private HashMap<String, ArrayList<Record>> radialInfo;
    private HashMap<String, Double> hValues;

    private HashMap<String, ArrayList<Point>> observableOffsets;

    private boolean moveToSoldier = false;
    private boolean stayPut = false;
    private int stayPutCounts = 0;
    private HashMap<Integer, Point> nearbySoldiers;
    private String prevDir = "";

    private ArrayList<ArrayList<Boolean>> observedCells;
    private boolean recordsReceived;

    private int timeForRandomMove = 0;
    private List<Point> last12ObsLocs;
    private int obsLocCount = 0;
    private List<Integer> lastPlayersComm;
    private int commsLength;
    private double timeToComm = 0;
    private boolean justTriedToComm = false;
    private Point newPoint;

    private boolean isOpposite(String dir1, String dir2) {
        if (dir1 == "" || dir2 == "") return false;
        else if (dir1 == "w" && dir2 == "e") return true;
        else if (dir1 == "e" && dir2 == "w") return true;
        else if (dir1 == "n" && dir2 == "s") return true;
        else if (dir1 == "s" && dir2 == "n") return true;
        else if (dir1 == "ne" && dir2 == "sw") return true;
        else if (dir1 == "sw" && dir2 == "ne") return true;
        else if (dir1 == "nw" && dir2 == "se") return true;
        else if (dir1 == "se" && dir2 == "nw") return true;
        return false;
    }

    private boolean isStatSoldier(Point ourLoc, Point theirLoc) {
        // # Determine whether to move to another soldier or not #
        // if there is another present soldier with a larger y-coord then move to them
        // if y-coord is the same but their x-coord is smaller then move to them
        // otherwise stay put
        if (theirLoc.y > ourLoc.y) return true;
        if ((theirLoc.y == ourLoc.y) && (theirLoc.x < ourLoc.x)) return true;
        return false;
    }

    private boolean isLoop(List<Point> obsLocs) {

        // keep track of last 12 moves and if there are at least 3 moves and return true if there at least 3 points that have been visited more than 3 times

        HashMap<Point, Integer> locCount = new HashMap<Point, Integer>();
        
        for (Point p : obsLocs) {
            if (!locCount.keySet().contains(p)) locCount.put(p, 1);
            else locCount.replace(p, locCount.get(p));
        }
        int c = 0;
        for (Point p : locCount.keySet()) {
            if (locCount.get(p) > 2) c += 1;
        }
        if (c > 2) return true;
        else return false;

    }
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.numPlayers = n;
        this.totalTime = t;
        this.timeToComm = 0.2*t;
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

        System.out.println("WATER!!! " + waterCells.size());

        pathToPackage = new LinkedList<Point>();
        trueRecords = new HashMap<Point, Record>();
        possibleSpies = new HashMap<Integer, HashSet<Point>>();
        //suspicionScore = new HashMap<Integer, Integer>();
        previousStatuses = new HashMap<Point, CellStatus>();

        observableOffsets = new HashMap<String, ArrayList<Point>>();
        observableOffsets.put("s", new ArrayList(Arrays.asList(new Point(0, -1), new Point(0, -2), new Point(0, -3))));
        observableOffsets.put("n", new ArrayList(Arrays.asList(new Point(0, 1), new Point(0, 2), new Point(0, 3))));
        observableOffsets.put("w", new ArrayList(Arrays.asList(new Point(-1, 0), new Point(-2, 0), new Point(-3, 0))));
        observableOffsets.put("e", new ArrayList(Arrays.asList(new Point(1, 0), new Point(2, 0), new Point(3, 0))));
        observableOffsets.put("sw", new ArrayList(Arrays.asList(new Point(-1, -1), new Point(-2, -2), new Point(-2, -1), new Point(-1, -2))));
        observableOffsets.put("se", new ArrayList(Arrays.asList(new Point(1, -1), new Point(2, -2), new Point(1, -2), new Point(2, -1))));
        observableOffsets.put("ne", new ArrayList(Arrays.asList(new Point(1, 1), new Point(2, 2), new Point(2, 1), new Point(1, 2))));
        observableOffsets.put("nw", new ArrayList(Arrays.asList(new Point(-1, 1), new Point(-2, 2), new Point(-2, 1), new Point(-1, 2))));
        
        possibleMoves = new HashMap<String, Point>();
        radialInfo = new HashMap<String, ArrayList<Record>>();
        newPoint = new Point(0, 0);
        nearbySoldiers = new HashMap<Integer, Point>();

    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {

        previousStatuses = statuses;

        this.loc = loc;
        obsLocCount += 1;

        if (obsLocCount % 12 == 0) {
            last12ObsLocs = new ArrayList<Point>();
        }
        last12ObsLocs.add(this.loc);

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            //update observedCells
            observedCells.get(p.x).set(p.y, true);
            CellStatus status = entry.getValue();
            if (status.getPT() == 1) {packageKnown = true; System.out.println(this.id + " knows where PACKAGE is!!");}
            else if (status.getPT() == 2) {targetKnown = true; System.out.println(this.id + " knows where TARGET is!!");}
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
        }
        /*for (int i=0; i<records.size(); i++) {
            int j=0;
            for (Record r : records.get(i)) {
                System.out.println(j + ": " + r);
                j++;
            }
        }*/
    }
    
    public List<Record> sendRecords(int id)
    {
        System.out.println("> " + this.id + " is SENDING records <");
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
        System.out.println("> " + this.id + " is RECEIVING records <");

        recordsReceived = true;

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

        if (lastPlayersComm.size() < commsLength) {
            lastPlayersComm.add(id);
        } else if (lastPlayersComm.size() > 0) {
            lastPlayersComm.remove(0);
            lastPlayersComm.add(id);
        }

        // If we are in the chain of observations
        // If we are first in the chain: compare information against our own obswerved records
        // If not first in chain: compare sequence of observations with the corresponding sequence of observations in records
        
    }

    public List<Point> calculatePath() {

        List<Point> finalPath = new ArrayList<Point>();

        // if there is a complete path, set pathKnown to true

        if (!targetKnown || !packageKnown) {
            pathKnown = false;
            return null;
        }

        //private HashMap<Point, Record> trueRecords;
        // find the package and target position first
        Point tar = null;
        Point pac = null;

        for (Point key : trueRecords.keySet()) {
            int pt = trueRecords.get(key).getPT();
            if (pt == 1) { /* package location */
                pac = key;
            }
            if (pt == 2) { /* target location */
                tar = key;
            }
            if (tar != null && pac != null) {
                break;
            }
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

            /* goal test */
            if (current.equals(tar)) {
                goal_reached = true;
            }
            /* add to visited */
            List<Point> path = new ArrayList<Point>();
            if (parent == null) {
                path.add(current);
            } else {
                path = visited.get(parent);
                path.add(current);
            }
            visited.put(current, path);


            /* if goal test successful */
            if (goal_reached = true) {
                break;
            }
            /* adds all children that's not visited to queue
             * only adds children that are normal condition */
            int x = current.x;
            int y = current.y;
            //List<Point> children = new ArrayList<Point>();
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    Point child = new Point(x + i, y + j);
                    /* check condition */
                    if (trueRecords.get(child).getC() != 0) {
                        continue;
                    }
                    /* check visited */
                    Boolean visited_child = false;
                    for (Point p : visited.keySet()) {
                        if (p.equals(child)) {
                            visited_child = true;
                        }
                    }
                    if (visited_child = true) {
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
            pathToPackage = calculatePath(loc, pac);
            finalPath = visited.get(tar);
        }


        return finalPath;
    }

    private Queue<Point> calculatePath(Point loc, Point pac) {

        Queue<Point> finalPath = new LinkedList<Point>();

        /* perform BFS */
        /* visited contains the point and the path that took to get to that point */
        HashMap<Point, List<Point>> visited = new HashMap<Point, List<Point>>();
        /* keeps track of parent / children pairs */
        List<List<Point>> queue = new ArrayList<List<Point>>();
        Boolean goal_reached = false;


        List<Point> tempList = new ArrayList<Point>();
        tempList.add(loc);
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

            /* goal test */
            if (current.equals(pac)) {
                goal_reached = true;
            }
            /* add to visited */
            List<Point> path = new ArrayList<Point>();
            if (parent == null) {
                path.add(current);
            } else {
                path = visited.get(parent);
                path.add(current);
            }
            visited.put(current, path);


            /* if goal test successful */
            if (goal_reached = true) {
                break;
            }
            /* adds all children that's not visited to queue
             * only adds children that are normal condition */
            int x = current.x;
            int y = current.y;
            //List<Point> children = new ArrayList<Point>();
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    Point child = new Point(x + i, y + j);
                    /* check condition */
                    if (trueRecords.get(child).getC() != 0) {
                        continue;
                    }
                    /* check visited */
                    Boolean visited_child = false;
                    for (Point p : visited.keySet()) {
                        if (p.equals(child)) {
                            visited_child = true;
                        }
                    }
                    if (visited_child = true) {
                        continue;
                    }
                    tempList = new ArrayList<Point>();
                    tempList.add(child);
                    tempList.add(current);
                    queue.add(tempList);
                }
            }
        }
        // should there be a way to reach the target? 
        if (goal_reached == false) {
            finalPath = null;
        } else {
            for (Point p : visited.get(pac)) {
                finalPath.add(p);
            }
        }
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
                    System.out.println(record.getPT());
                    i++;
                    return false;
                }
            } else if (i == f) {
                // target location 
                if (record.getPT() != 2) {
                    System.out.println(record.getPT());
                    return false;
                }
            } else {
                // ordinary cell 
                i++;
                if (record.getPT() != 0) {
                    System.out.println(record.getPT());
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

        moveToSoldier = false;
        stayPut = false;

        possibleMoves.clear();
        radialInfo.clear();
        nearbySoldiers.clear();

        if (pathKnown) {
            System.out.println("movement for KNOWN PATH");
        } else if (packageKnown || targetKnown) {
            System.out.println("movement for KNOWN PACKAGE or TARGET");
        } else {
            System.out.println("movement for EXPLORATION");

            boolean canMove = false;
            // Get all possible movement directions
            for (String dir : observableOffsets.keySet()) {
                ArrayList<Point> dirMoves = new ArrayList<Point>();
                for (Point offset : observableOffsets.get(dir)) {
                    newPoint = new Point(this.loc.x + offset.x, this.loc.y + offset.y);
                    dirMoves.add(newPoint);    
                    if (!waterCells.contains(newPoint) && (-1 < newPoint.x) && (newPoint.x < 100) && (-1 < newPoint.y) && (newPoint.y < 100)) {
                        // it's not a water cell and it's not off the map
                        if ((Math.abs(offset.x) < 2) && (Math.abs(offset.y) < 2)) {
                            // point we can move to
                            canMove = true;
                            // is there a soldier here?
                            CellStatus cs = previousStatuses.get(p);
                            if ((cs.getPresentSoldiers().size() > 0) && (!p.equals(this.loc))) {
                                // there is a soldier present
                                for (int soldID : cs.getPresentSoldiers()) {
                                    if (!lastPlayersComm.contains(soldID)) {
                                        nearbySoldiers.put(soldID, p);
                                    }
                                }
                            }
                        }
                    }
                }
                if (canMove) radialInfo.put(dir, dirMoves);
            }
            // Determine if there is a soldier 1 block away from us
            dddd


        }
    }
}
