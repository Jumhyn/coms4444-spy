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

    private ArrayList<ArrayList<Boolean>> visitedCells;

    public boolean isOpposite(String dir1, String dir2) {
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
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        this.visitedCells = new ArrayList<ArrayList<Boolean>>();
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
            this.visitedCells.add(row2);
        }

        //System.out.println("vc.get(0).get(0) = " + visitedCells.get(0).get(0));

        this.isSpy = isSpy;
        this.waterCells = waterCells;
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
  
    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {

        previousStatuses = statuses;

        this.loc = loc;

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            //update visitedCells
            visitedCells.get(p.x).set(p.y, true);
            CellStatus status = entry.getValue();
            if (status.getPT() == 1) packageKnown = true;
            else if (status.getPT() == 2) targetKnown = true;
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
        ArrayList<Record> toSend = new ArrayList<Record>();
        /*if ( (possibleSpies.size() > 0) && ((possibleSpies.get(id).size() > 30) || (id == spy))) {
            return toSend;
        } else {
            if (!isSpy) {
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
            } else {
                int m = 0;
                for (ArrayList<Record> row : records) {
                    for (Record record : row) {
                        if (record != null) {
                            if (m%3 == 0) {
                                //System.out.println("WE WANT TO LIE");
                                Record newRec = new Record(record.getLoc(), (record.getC()+1)%2, (record.getPT()+1)%3, record.getObservations());
                                toSend.add(newRec);
                            } else {
                                toSend.add(record);
                            }
                        }
                        m += 1;
                    }
                }
            }
            return toSend;
        }*/
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
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> records)
    {
        /*if (id != spy) {
            // Compare received records against those that we have in our trueRecords list
            // If there is a conflict
            for (Record recR : records) {
                Point p = recR.getLoc();
                Record ourRecord = this.trueRecords.get(p);
                if (ourRecord != null) {
                    if (!((ourRecord.getC() == recR.getC()) && (ourRecord.getPT() == recR.getPT()))) {
                        //System.out.println("player " + id + " LIED!");
                        if (!(possibleSpies.containsKey(id))) {
                            HashSet<Point> clashPs = new HashSet<Point>();
                            clashPs.add(p);
                            possibleSpies.put(id, clashPs);
                        } else {
                            if (!(possibleSpies.get(id).contains(p))) {
                                possibleSpies.get(id).add(p);
                            }
                        }
                        //System.out.println("p" + id + " suspicion score = " + possibleSpies.get(id).size());
                        if (recR.getObservations().size() < 2) {
                            spy = id;
                        }
                        
                    } else {
                        //System.out.println("player " + id + " told the TRUTH!");
                    }
                }
            }
        }*/

        // Assuming no spies
        for (Record recR : records) {

            if (recR != null) {
                
                if (recR.getPT() == 1) packageKnown = true;
                else if (recR.getPT() == 2) targetKnown = true;

                Point p = recR.getLoc();
                Record ourRecord = this.records.get(p.x).get(p.y);

                if (ourRecord == null) {
                    ourRecord = new Record(p, recR.getC(), recR.getPT(), recR.getObservations());
                    this.records.get(p.x).set(p.y, ourRecord);
                }
            }
        }
        

        // If we are in the chain of observations
        // If we are first in the chain: compare information against our own obswerved records
        // If not first in chain: compare sequence of observations with the corresponding sequence of observations in records
        
    }

    public List<Point> calculatePath() {
        
        // ##########################
        // ### Quincy's code here ###
        // ##########################

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
        stayPutCounts = 0;

        if (pathKnown && pathToPackage != null) {

            // ### MOVE TO PACKAGE ###

            // ##########################
            // ### Ashley's code here ###
            // ##########################

            return pathToPackage.remove(); // change this

            // probably will need to calculate the shortest path from current location to target first--store as a list of points
            // then each time getMove is called, iterate through the list of points and remove each one you visit until soldier has reached last point in list which should be the package location

        } else if (packageKnown || targetKnown) {

            // ### KEEP EXPLORING (currently might time out) ###

            // ##########################
            // ### Shandu's code here ###
            // ##########################

            //System.out.println("cellStatus from previous observation:");
            for (Point p : previousStatuses.keySet()) {
                CellStatus cs = previousStatuses.get(p);
                //System.out.println(p + ": " + cs.getC() + ", " + cs.getPT() + ", " + cs.getPresentSoldiers());
            }

            Random rand = new Random();
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            return new Point(x, y);

        } else {

            // ### KEEP EXPLORING ###

            // get a list of all possible moves to make
            // keep track of all information about cells within observable radius to take into account for moves
            possibleMoves = new HashMap<String, Point>();
            radialInfo = new HashMap<String, ArrayList<Record>>();

            for (String dir : observableOffsets.keySet()) {
                ArrayList<Point> offsetL = observableOffsets.get(dir);
                ArrayList<Record> toAdd = new ArrayList<Record>();
                for (Point offset : offsetL) {
                    //System.out.println(this.loc.x + offset.x);
                    //System.out.println(this.loc.y + offset.y);
                    if ((-1 < (this.loc.x + offset.x)) && ((this.loc.x + offset.x) < 100) && (-1 < (this.loc.y + offset.y)) && ((this.loc.y + offset.y) < 100) && (records.get(this.loc.x + offset.x).get(this.loc.y + offset.y) != null)) {toAdd.add(records.get(this.loc.x + offset.x).get(this.loc.y + offset.y));}
                    if ((Math.abs(offset.x) < 2) && (Math.abs(offset.y) < 2)) {
                        possibleMoves.put(dir, new Point(offset.x, offset.y));
                    }
                }
                radialInfo.put(dir, toAdd);
            }
            
            //System.out.println("len of possMoves = " + possibleMoves.size());
            //int radLen = 0;
            //for (ArrayList<Record> f : radialInfo.values()) {
            //    for (Record g : f) {
            //        radLen += 1;
            //    }
            //}
            //System.out.println("len of radialInfo = " + radLen);
            
            String dirToMoveIn = "";
            nearbySoldiers = new HashMap<Integer, Point>();
            for (Point p : previousStatuses.keySet()) {
                CellStatus cs = previousStatuses.get(p);
                if ((cs.getPresentSoldiers().size() > 0) && (!p.equals(this.loc))) {
                    //System.out.println("there are " + cs.getPresentSoldiers().size() + "soldiers nearby!!!");
                    // there are soldiers present nearby
                    // check if they are se (s first)--stayPut=true
                    // check if they are nw (n first)--moveToSoldier=true
                    for (int soldID : cs.getPresentSoldiers()) {
                        nearbySoldiers.put(soldID, p);
                    }
                    for (String dir : radialInfo.keySet()) {
                        if (radialInfo.get(dir).contains(p) && (dir.equals("n") || dir.equals("nw") || dir.equals("w"))) {
                            moveToSoldier = true;
                            dirToMoveIn = dir;
                        } else {
                            stayPut = true;
                        }
                    }

                }

                //System.out.println(p + ": " + cs.getC() + ", " + cs.getPT() + ", " + cs.getPresentSoldiers());
            }

            if (moveToSoldier) {
                //System.out.println("move to soldier!!!");
                // # Move towards soldier #
                if (!waterCells.contains(possibleMoves.get(dirToMoveIn))) {
                    return possibleMoves.get(dirToMoveIn);
                } else {
                    if (dirToMoveIn.length() == 2) {
                        if (!waterCells.contains(possibleMoves.get(dirToMoveIn.charAt(0)))) return possibleMoves.get(dirToMoveIn.charAt(0));
                        if (!waterCells.contains(possibleMoves.get(dirToMoveIn.charAt(1)))) return possibleMoves.get(dirToMoveIn.charAt(1));
                    } else {
                        if (dirToMoveIn.equals("w") || dirToMoveIn.equals("e")) {if (!waterCells.contains(possibleMoves.get("n"+dirToMoveIn))) return possibleMoves.get("n"+dirToMoveIn);}
                        if (dirToMoveIn.equals("n") || dirToMoveIn.equals("s")) {if (!waterCells.contains(possibleMoves.get(dirToMoveIn+"e"))) return possibleMoves.get(dirToMoveIn+"w");}
                    }
                }
            }
            if (stayPut) {
                //System.out.println("stay put!!!");
                // # Stay put for 2 timesteps to enable exchange of info #
                if (stayPutCounts < 2) {
                    stayPutCounts += 1;
                    return new Point(0, 0);
                }
                stayPutCounts = 0;
            } 

            if (stayPutCounts == 0) {

                // # Explore randomly #
                //System.out.println("Explore randomly!!!");
                double c1 = 40, c2 = -30, c3 = -10, c4 = 20;
                
                hValues = new HashMap<String, Double>();
                double maxHVal = Double.NEGATIVE_INFINITY;
                String maxDir = "";
                for (String dir : radialInfo.keySet()) {
                    double num_muddy = 0;
                    double num_water = 0;
                    double num_clear = 0; // MAYBE when exploring once package and target are found we can give less weight to directions in which there is no package/target
                    double num_unknown = 0;
                    ArrayList<Record> dirRecords = radialInfo.get(dir);
                    double numRecords = dirRecords.size();
                    for (Record dR : dirRecords) {
                        if (dR == null) {
                            if (waterCells.contains(dR.getLoc())) num_water += 1;
                            else num_unknown += 1;
                        } else {
                            if (dR.getC() == 0) num_clear += 1;
                            else if (dR.getC() == 1) num_muddy += 1;
                        }
                    }
                    double hVal = c1*(num_clear/numRecords) + c2*(num_water/numRecords) + c3*(num_muddy/numRecords) + c4*(num_unknown/numRecords);
                    hValues.put(dir, hVal);    
                }
                boolean dirFound = false;
                boolean cancel = false;
                while (!dirFound) {
                    maxHVal = Double.NEGATIVE_INFINITY;
                    maxDir = "";
                    for (String d : hValues.keySet()) {
                        if (hValues.get(d) > maxHVal) {
                            maxHVal = hValues.get(d);
                            maxDir = d;
                        }
                    }
                    if (!maxDir.equals("") && (possibleMoves.get(maxDir) != null)) {
                        //System.out.println("not empty direction and in possible moves!");
                        //System.out.println("Best direction: " + maxDir);
                        Point move = new Point(this.loc.x + possibleMoves.get(maxDir).x, this.loc.y + possibleMoves.get(maxDir).y);
                        //System.out.println("CHECK: " + move);
                        if ((!waterCells.contains(possibleMoves.get(maxDir))) && (!isOpposite(prevDir, maxDir))) {dirFound = true; prevDir = maxDir;}
                        else {hValues.remove(maxDir);}
                    }
                    if (hValues.size() == 0) {
                        dirFound = true;
                        cancel = true;
                    }
                }
                if (!maxDir.equals("") && (possibleMoves.get(maxDir) != null) && (!cancel)) {
                    //System.out.println("not empty direction and in possible moves!");
                    //System.out.println("Best direction: " + maxDir + " cur_loc = " + this.loc + " >>> " + possibleMoves.get(maxDir));
                    return possibleMoves.get(maxDir);
                }

                
            }
            //System.out.println("random");
            Random rand = new Random();
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            return new Point(x, y);

        }
        // ***NOTE*** currently both exploration blocks will do the same thing (the else-if and else statements)
    }
}
