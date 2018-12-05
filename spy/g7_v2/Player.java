package spy.g7_v2;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;
import java.util.Comparator;


public class Player implements spy.sim.Player {

    private ArrayList<ArrayList<Record>> records;
    private int id;
    private int player_num;
    private Set<Integer> trust;
    private Point loc;
    private HashMap<Point, List<Record>> trustRecords;
    private HashMap<Point, List<Record>> tempRecords;
    private Point package_loc;
    private Point target_loc;
    private boolean package_found;
    private boolean target_found;
    private List<Point> waterCells;
    private List<Point> mudCells;
    private List<Point> observed;
    private List<Point> notobserved;
    private Point destination;
    private Point move;
    private PriorityQueue<Vote> lastResult;
    private boolean nearbySoldiers;
    private ArrayList<Integer> soldierIds;
    private boolean stayPut;
    private int stayPutTime = 0;
    private Point soldierToMoveTo;

    class PathTime{
        Integer id;
        Integer time;
        PathTime(int id, int time){
            this.id = id;
            this.time = time;
        }
        public Integer getId() {
            return id;
        }
        public Integer getTime() {
            return time;
        }
    }
    class Vote{
        Integer id;
        Integer number;
        Vote(int id, int number){
            this.id = id;
            this.number = number;
        }
        public Integer getId() {
            return id;
        }
        public Integer getNumber() {
            return number;
        }
    }
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        lastResult = null;
        trust  = new HashSet<>();
        trustRecords = new HashMap<>();
        tempRecords = new HashMap<>();
        this.waterCells = waterCells;
        this.observed = new ArrayList<Point>();
        this.notobserved = new ArrayList<Point>();
        this.mudCells = new ArrayList<Point>();
        trust.add(id);
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();

        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            for (int j = 0; j < 100; j++)
            {
                Point cell = new Point(i,j);
                if (!waterCells.contains(cell)) {
                    notobserved.add(new Point(i,j));
                }
                row.add(null);
            }
            this.records.add(row);
        }
        
        this.package_found = false;
        this.target_found = false;
        this.nearbySoldiers = false;
        this.soldierIds = new ArrayList<Integer>();
        player_num = n;
    }


    private boolean pointsAreAdjacent(Point p1, Point p2)
    {
        return Math.abs(p1.x - p2.x) <= 1 && Math.abs(p1.y - p2.y) <= 1;
    }

    private int moveTime(Point from, Point to)
    {
        if (from.equals(to))
        {
            return 1;
        }
        int base = 2;
        if (from.x != to.x && from.y != to.y)
        {
            base = 3;
        }
        boolean isMuddy = trustRecords.get(from).get(0).getC() == 1 || trustRecords.get(to).get(0).getC() == 1;
        return isMuddy ? base * 2 : base;
    }

    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            Record record = records.get(p.x).get(p.y);
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            // this adds the observation into the record
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
            observed.add(p);
            // keep track of muddy cells
            if (status.getC() == 1) {
                if (!mudCells.contains(p)) {
                    this.mudCells.add(p);    
                }
            }
            // keep track of package
            if (status.getPT() == 1) {
                this.package_loc = loc;
                this.package_found = true;
            }
            // keep track of target
            else if (status.getPT() == 2) {
                this.target_loc = loc;
                this.target_found = true;
            }

            // if we notice soldiers, store the location of the soldier with the smallest id
            if (status.getPresentSoldiers().size() > 0) {
                this.soldierIds = status.getPresentSoldiers();
                Point min_point = p;
                int smallest_id = Integer.MAX_VALUE;
                for (int soldierId : soldierIds) {
                    if (soldierId == this.id) {
                        // System.out.println("in if statement");
                        continue;
                    }
                    else if (soldierId < smallest_id) {
                        smallest_id = soldierId;
                        min_point = p;
                    }
                    this.soldierToMoveTo = min_point;
                    this.nearbySoldiers = true;
                }
            }

            List<Record> rl = new ArrayList<>();
            rl.add(record);
            trustRecords.put(p, rl);
        }

    }

    private int calculateTime(List<Point> path){
        int totalTime = 0;
        for (int i = 1; i < path.size(); i++)
        {
            Point from = path.get(i - 1);
            Point to = path.get(i);
            if (!pointsAreAdjacent(from, to))
                return Integer.MAX_VALUE;
            if (!trustRecords.containsKey(from)||!trustRecords.containsKey(to))
                return Integer.MAX_VALUE;
            totalTime += moveTime(from, to) * 5;
        }
        return totalTime;
    }


    public List<Record> sendRecords(int id)
    {
        List<Record> send = new ArrayList<Record>();
        for(List<Record> l :  trustRecords.values()){
            for(Record r:l){
                if(r.getObservations().get(r.getObservations().size()-1).getID()!=this.id)
                    r.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
                else {
                    r.getObservations().remove(r.getObservations().size()-1);
                    r.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
                }
            }
            send.addAll(l);
        }
        // after sending all records, reset nearbySoldiers
        this.nearbySoldiers = false;
        return send;
    }

    public void receiveRecords(int id, List<Record> records)
    {
        for(Record r: records){
            if(!trustRecords.containsKey(r.getLoc())){
                List<Record> rl = new ArrayList<>();
                rl.add(r);
                trustRecords.put(r.getLoc(), rl);
            }else {
                List<Record> rl = trustRecords.get(r.getLoc());
                rl.add(r);
            }
        }
//        for(Record r:records){
//            List<Record> l = new ArrayList<>();
//            l.add(r);
//            tempRecords.put(r.getLoc() ,l);
//        }
//        for (Map.Entry<Point, List<Record>> entry : tempRecords.entrySet()){
//            List<Record> rl= entry.getValue();
//            for(int i =0; i<rl.size()-1;i++){
//                Set<Integer> playerInI = new HashSet<>();
//                List<Observation> obInI = rl.get(i).getObservations();
//                for(Observation o: obInI)
//                    playerInI.add(o.getID());
//                for(int j=i; j<rl.size();j++){
//                    if(rl.get(i).getC() != rl.get(j).getC() || rl.get(i).getPT() != rl.get(j).getPT()){
//                        Set<Integer> playerInJ = new HashSet<>();
//                        List<Observation> obInJ = rl.get(j).getObservations();
//                        for(Observation o: obInJ)
//                            playerInJ.add(o.getID());
//                        playerInJ.addAll(playerInI);   //If there are conflicts then, spys will not be out of the two sets
//                        Set<Integer> untrust= new HashSet();
//                        for(int k = 0; k<player_num;k++)
//                            untrust.add(k);
//                        untrust.removeAll(playerInJ); //remove all players might be spys
//                        trust.addAll(untrust);
//                    }
//                }
//            }
//        }
//        for(Record r:records){
//            List<Observation> obs= r.getObservations();
//            boolean flag = true;
//            for(Observation ob: obs){
//                if(!trust.contains(ob.getID())) {
//                    flag = false;
//                    break;
//                }
//            }
//            if(flag){
//                List<Record> temp = trustRecords.get(r.getLoc());
//                temp.add(r);
//                trustRecords.put(r.getLoc(), temp);
//            }
//        }

    }

    public List<Point> proposePath()
    {
        if(!package_found||!target_found)  return null;
        List<List<Point>> result = new ArrayList<>();
        List<Point> path = new ArrayList<>();
        path.add(package_loc);
        List<Point> visited = new ArrayList<>();
        visited.add(package_loc);
        backtrack(result, path, visited, package_loc, target_loc);
        if(result.size()==0) return null;
        Collections.sort(result, (x, y) -> calculateTime(x)-calculateTime(y));
        return result.get(0);

    }

    private void backtrack(List<List<Point>> result, List<Point> path, List<Point> visited, Point current, Point target){
        if(current.equals(target)){
            result.add(path);
            return;
        }
        PriorityQueue<Point> pq = new PriorityQueue<>((x, y)->(moveTime(current, x)-moveTime(current, y)));
        Point temp = new Point(current.x+1,current.y);
        if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x-1,current.y);
        if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x,current.y+1);
        if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x,current.y-1);
        if(trustRecords.containsKey(temp)) pq.add(temp);
        while(pq.size()!=0){
            temp = pq.poll();
            if(temp.x < 100 && temp.x > 0 && temp.y > 0 && temp.y < 100 && ! waterCells.contains(temp) && !visited.contains(temp)){
                visited.add(temp);
                path.add(temp);
                backtrack(result, path, visited, temp, target);
                path.remove(path.size()-1);
            }
        }
    }

    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        PriorityQueue<PathTime> heap = new PriorityQueue<PathTime>((x,y)->(x.getTime()-y.getTime()));
        if(lastResult!=null){
            return new ArrayList<Integer>(lastResult.poll().getId());
        }else{
            for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
            {
                if(calculateTime(entry.getValue())!=Integer.MAX_VALUE)
                    heap.add(new PathTime(entry.getKey(),calculateTime(entry.getValue())));
            }
            if(heap.size()!=0){
                return new ArrayList<Integer>(heap.poll().getId());
            }
        }
        return null;
    }

    public void receiveResults(HashMap<Integer, Integer> results)
    {
        lastResult = new PriorityQueue<Vote>((x, y) -> y.getNumber() - x.getNumber());
        for (Map.Entry<Integer, Integer> entry : results.entrySet()) {
            Vote v= new Vote(entry.getKey(), entry.getValue());
            lastResult.add(v);
        }

    }
    /*
    * ADAPTED FROM G8 CODE
    */
    private double distance(Point p1, Point p2) 
    {
        return Math.sqrt(Math.pow(p1.x-p2.x,2)+Math.pow(p1.y-p2.y,2));
    }
    public Comparator<Point> pointComparator = new Comparator<Point>() 
    {   
            public int compare(Point p1, Point p2)  
            {
                double d1 = distance(loc, p1);
                double d2 = distance(loc, p2);
                if (d1 < d2) 
                {
                    return -1;
                } 
                else if (d1 > d2) 
                {
                    return 1;
                } 
                else 
                {
                    return 0;
                }
            }
    };

    private List<Point> getSurrounding(Point location) {
        //return surrounding area
        List<Point> neighbors = new ArrayList<Point>();
        for (int x=-1;x<=1;x++) {
            for(int y=-1;y<=1;y++) {
                Point temp = new Point(location.x+x, location.y+y);
                if (temp.x >= 0 && temp.x <= 100 && temp.y >= 0 && temp.y <= 100) {
                    neighbors.add(temp);
                }
            }
        }
        return neighbors;
    }
    
    public Point getMove()
    {

        // if we observe soldiers nearby
        if (nearbySoldiers) {
            System.out.println("there are nearby soldiers!");
            int smallest_id = Integer.MAX_VALUE;
            for (int otherSoldierId : soldierIds) {
                System.out.println("soldiers seen: " + otherSoldierId);
                if (otherSoldierId < smallest_id) {
                    smallest_id = otherSoldierId;
                }
                if (this.id < smallest_id) {
                    stayPut = true;
                }
                //move to soldier with smallest id if one cell away, else keep moving
                else {
                    System.out.println("id of soldier moving: " + smallest_id);
                    System.out.println("soldierToMoveTo: " + soldierToMoveTo.x +","+soldierToMoveTo.y);
                    if (distance(loc, soldierToMoveTo) < 2) {
                        int x = soldierToMoveTo.x - loc.x;
                        int y = soldierToMoveTo.y - loc.y;
                        return new Point(x, y);
                    }
                    else {
                        nearbySoldiers = false;
                    }
                }
            }
        }

        if (stayPut) {
            System.out.println(this.id + " is stopping for a while");
            // don't move for 4 time steps.
            // 4 because people need time to move to us (1 time step), also need to 
            // make sure they move away from sight before resetting 
            if (stayPutTime < 4) {
                stayPutTime += 1;
                return new Point(0, 0);
            }
            //when done exchanging info, reset
            stayPut = false;
            stayPutTime = 0;
            soldierToMoveTo = null;
            nearbySoldiers = false;
        }


        List<Point> path= proposePath();
        // System.out.println("printing current location: " + loc.x + "," + loc.y);      
        List<Point> neighbors = getSurrounding(loc);
        for(Point n:neighbors) {
            // System.out.println("neighbors: " + n.x + "," + n.y);
            if(notobserved.contains(n)) {
                int i = notobserved.indexOf(n);
                notobserved.remove(i);
                observed.add(n);
            }
        }

        // System.out.println("notobserved");
        // for (Point n: notobserved) {
        //     System.out.println(n.x + "," + n.y);
        // }
        // System.out.println("notobserved size: " + notobserved.size());

        if(notobserved.size() > 0) {
            Collections.sort(notobserved, pointComparator);
            destination = notobserved.get(0);
            // System.out.println("DEST:"+destination.x + " " + destination.y);
            if(destination.x > loc.x) { 
                move = new Point(1,0);
            } else if (destination.x < loc.x) {
                move = new Point(-1,0);
            } else {
                if (destination.y > loc.y) {
                    move = new Point(0,1);
                } else {
                    move = new Point(0,-1);
                } 
            }
        }
        else {
            Random rand = new Random();
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            move = new Point(x, y);
        }

        loc = new Point(loc.x + move.x, loc.y + move.y);
        if (waterCells.contains(loc)) {
            Random rand = new Random();
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            move = new Point(x, y);
            loc = new Point(loc.x + move.x, loc.y + move.y);
        }

        if (package_found && target_found) {
            // move towards package
            if (package_loc.x > loc.x && package_loc.y > loc.y) {
                move = new Point(1,1);
            } else if (package_loc.x < loc.x && package_loc.y < loc.y) {
                move = new Point(-1,-1);
            } else if (package_loc.x > loc.x && package_loc.y < loc.y) {
                move = new Point(1, -1);
            } else if (package_loc.x < loc.x && package_loc.y > loc.y) {
                move = new Point(-1, 1);
            } else if (package_loc.x > loc.x) {
                move = new Point(1,0);
            } else if (package_loc.y > loc.y) {
                move = new Point(1,0);
            }
            loc = new Point(loc.x + move.x, loc.y + move.y);
        }

        else if (package_found || target_found) {
            // System.out.println("something found");
            // only move to non-muddy cells
            while (mudCells.contains(loc)) {
                Random rand = new Random();
                int x = rand.nextInt(2) * 2 - 1;
                int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
                move = new Point(x, y);
                loc = new Point(loc.x + move.x, loc.y + move.y);
            }
        } 
        
        //System.out.println("NEW LOC:"+loc.x + " " + loc.y);
        return move;
    }
    /*

    public Point getMove() {
        // if not waterCells
        // System.out.println("printing recordss from records");
        // for (ArrayList<Record> r: this.records) {
        //     for (Record each : r) {
        //         System.out.println(each);
        //     }
        // }

        // System.out.println("printing records from trustRecords");
        // for (Point p : trustRecords.keySet() ) {
        //     System.out.println(p.x + "," + p.y);
        //     System.out.println(trustRecords.get(p));
        // }

        System.out.println("printing current location: " + loc.x + "," + loc.y);
        Random rand = new Random();
        int x = rand.nextInt(2) * 2 - 1;
        int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
        Point location = new Point(loc.x + x, loc.y + y);
        System.out.println("printing new location: " + location.x + "," + location.y);
        System.out.println("testing trust records: " + trustRecords.get(location));

        // if target or package found, only move to clear spaces
        
        
        // we want to move to a point if it's not already in trusted records
        // while (trustRecords.get(location) != null) {
        //     System.out.println("in condition loop");
        //     if (trustRecords.get(location.x + 3) == null) {
        //         location = new Point(loc.x + 1, loc.y);
        //         return location;
        //     }
        //     else if (trustRecords.get(location.y + 3) == null) {
        //         location = new Point(loc.x, loc.y + 1);
        //         return location;
        //     }
        //     else break;
        // }
        return location;

    }
    */
    

}
