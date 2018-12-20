package spy.g7;

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
import spy.g7.Maze;


public class Player implements spy.sim.Player {

    private ArrayList<ArrayList<Record>> records;
    private int id;
    private int player_num;
    private Set<Integer> trust;
    private Point loc;
    private HashMap<Point, List<Record>> trustRecords;
    private HashMap<Point, List<Record>> tempRecords;
    private Point package_loc;
    private int overallStrategy = 0;
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
    private boolean communicating = false;
    private boolean sentRecord = false;
    private boolean receiveRecord = false;
    // 0 means the player explores diagonally, 1 means they explore horizontal
    private int explorationStrategy = 1;
    private int explorationDirection;
    private int offsetStatus = 0;
    private int step = 1;
    private int sendstep = 1;
    private Point tmpTar;
    private List<Point> tmpPath;
    private Maze maze;
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
        maze = new Maze();
        maze.init(waterCells);
        trust  = new HashSet<>();
        trustRecords = new HashMap<>();
        tempRecords = new HashMap<>();
        this.waterCells = waterCells;
        this.observed = new ArrayList<Point>();
        this.notobserved = new ArrayList<Point>();
        this.mudCells = new ArrayList<Point>();
        trust.add(id);
        this.id = id;
        double randNum = Math.random();
        if (randNum < 0.5) {
            this.explorationStrategy = 0;
        }
        else {
            this.explorationStrategy = 1;
        }
        tmpTar = startingPos;
        Random random = new Random();
        explorationDirection = random.nextInt(4)+4;
        //  System.out.println("Exploration direction: ");
        // System.out.println(explorationDirection);
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
        Collections.shuffle(notobserved);
        tmpTar = notobserved.get(0);
        tmpPath = new ArrayList<>();
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
        boolean isMuddy = true;
        if(trustRecords.containsKey(from) && trustRecords.containsKey(to)){
            isMuddy = trustRecords.get(from).get(0).getC() == 1 || trustRecords.get(to).get(0).getC() == 1;
        }

        return isMuddy ? base * 2 : base;
    }

    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();            // keep track of muddy cells
            if (status.getC() == 1) {
                if (!mudCells.contains(p)) {
                    this.mudCells.add(new Point(p.x,p.y));
                }
            }
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

            // keep track of package
            if (status.getPT() == 1) {
                this.package_loc = entry.getKey();
                this.package_found = true;
            }
            // keep track of target
            else if (status.getPT() == 2) {
                this.target_loc = entry.getKey();
                this.target_found = true;
            }

            // if we notice soldiers, store the location of the soldier with the smallest id
            if (!communicating) {
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
                        this.nearbySoldiers = true;
                        this.soldierToMoveTo = min_point;
                    }
                }
            }


            List<Record> rl = new ArrayList<>();
            rl.add(record);
            if(!trustRecords.containsKey(p)){
                trustRecords.put(p, rl);
            }else{
                if(!trustRecords.get(p).contains(record)){
                    rl = trustRecords.get(p);
                    rl.add(record);
                    trustRecords.put(p, rl);
                }
            }

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
        //  System.out.println(id + " is sending records");
        List<Record> send = new ArrayList<Record>();
        if(communicating) return send;
        communicating = true;

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
        //   System.out.println(id + " is receiving records");
        if(!receiveRecord){
            for(Record r: records){
                if(!trustRecords.containsKey(r.getLoc())){
                    List<Record> rl = new ArrayList<>();
                    rl.add(r);
                    trustRecords.put(r.getLoc(), rl);
                }else {
                    List<Record> rl = trustRecords.get(r.getLoc());
                    if(trustRecords.get(r.getLoc()).size() < 100 && r.getObservations().size()<20 && !trustRecords.get(r.getLoc()).contains(r))
                        rl.add(r);
                }
                receiveRecord = true;
            }

        }else{
            step = step+1;
            if(step%60 == 0) receiveRecord = false;
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
        System.out.println("propose now");
//        List<List<Point>> result = new ArrayList<>();
//        List<Point> path = new ArrayList<>();
//        path.add(package_loc);
//        List<Point> visited = new ArrayList<>();
//        visited.add(package_loc);
//        backtrack(result, path, visited, package_loc, target_loc);
//        if(result.size()==0) return null;
//        Collections.sort(result, (x, y) -> calculateTime(x)-calculateTime(y));
//        System.out.println(result.get(0).size());
        maze = new Maze();
        List<Point> unvalid = new ArrayList<>();
        for(Point p: waterCells) unvalid.add(new Point(p.x,p.y));
        for (Point p: mudCells) unvalid.add(new Point(p.x,p.y));
        maze.init(unvalid);
//        int[][] m=maze.maze;
//        for(int i =0;i<m.length;i++){
//            for(int j =0 ;j<m.length;j++)
//             System.out.print(m[i][j]);
//            System.out.println("");
//        }
        List<Point> path = maze.findPath(package_loc,target_loc);
        //  path.add(0,package_loc);
        System.out.println(package_loc);
//        path.add(target_loc);
        return path;

    }

    private void backtrack(List<List<Point>> result, List<Point> path, List<Point> visited, Point current, Point target){
        System.out.println(visited.size()+" "+result.size()+" "+path.size());

        if(current.equals(target)){
            result.add(new ArrayList<>(path));
            return;
        }
        if(path.size()>1000) return;
        List<Point> pq = new ArrayList<>();
        Point temp = new Point(current.x+1,current.y);
        pq.add(temp);
//     if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x+1,current.y+1);
        pq.add(temp);
        temp = new Point(current.x+1,current.y-1);
        pq.add(temp);
        temp = new Point(current.x-1,current.y);
        pq.add(temp);
        temp = new Point(current.x-1,current.y+1);
        pq.add(temp);
        temp = new Point(current.x-1,current.y-1);
        pq.add(temp);

//     if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x,current.y+1);
        pq.add(temp);
//     if(trustRecords.containsKey(temp)) pq.add(temp);
        temp = new Point(current.x,current.y-1);
        pq.add(temp);
        Collections.sort(pq,(m,n)->Math.abs(m.x-target.x)+Math.abs(m.y-target.y)-Math.abs(n.x-target.x)+Math.abs(n.y-target.y));
//     if(trustRecords.containsKey(temp)) pq.add(temp);
        for(int i=0;i<pq.size();i++){
            temp = pq.get(i);
//            for(Point n: pq){
//                if(Math.abs(n.x-target.x)+Math.abs(n.y-target.y)<Math.abs(temp.x-target.x)+Math.abs(temp.y-target.y))
//                    temp = n;
//            }
//            pq.remove(temp);
            //  System.out.println(temp);
            if(temp.x < 100 && temp.x > 0 && temp.y > 0 && temp.y < 100 && ! waterCells.contains(temp) && !visited.contains(temp)){
                visited.add(temp);
                path.add(temp);
                backtrack(result, path, visited, temp, target);
                // System.out.println(path);
                if(result.size()>0)  return;
                //  visited.remove(temp);
                path.remove(path.size()-1);

            }
        }
    }

    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        List<Integer> vot = new ArrayList<Integer>();
        // System.out.println(paths);
        PriorityQueue<PathTime> heap = new PriorityQueue<PathTime>((x,y)->(x.getTime()-y.getTime()));
        if(lastResult!=null && lastResult.size()>0){
            //   System.out.println(lastResult);
            vot =  new ArrayList<Integer>();
            vot.add(lastResult.poll().getId());
            return vot;
        }else{
            for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
            {
                if(calculateTime(entry.getValue())!=Integer.MAX_VALUE)
                    heap.add(new PathTime(entry.getKey(),calculateTime(entry.getValue())));
            }
            if(heap.size()!=0){
                vot =  new ArrayList<Integer>();
                vot.add(heap.poll().getId());
                return vot;
            }
        }

        vot.add(id);
        //System.out.println(vot);
        return vot;
    }

    public void receiveResults(HashMap<Integer, Integer> results)
    {
        //System.out.println(results);
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

//        System.out.println(mudCells);
        // if we observe soldiers nearby
//        if (nearbySoldiers) {
//            System.out.println("there are nearby soldiers!");
//            int smallest_id = Integer.MAX_VALUE;
//            for (int otherSoldierId : soldierIds) {
//                System.out.println("soldiers seen: " + otherSoldierId + " by " + this.id);
//                if (otherSoldierId < smallest_id) {
//                    smallest_id = otherSoldierId;
//                }
//                if (this.id < smallest_id) {
//                    stayPut = true;
//                }
//                //move to soldier with smallest id if one cell away, else keep moving
//                else {
//                    System.out.println("id of soldier moving: " + smallest_id);
//                    System.out.println("soldierToMoveTo: " + soldierToMoveTo.x +","+soldierToMoveTo.y);
//                    if (distance(loc, soldierToMoveTo) < 2) {
//                        int x = soldierToMoveTo.x - loc.x;
//                        int y = soldierToMoveTo.y - loc.y;
//                        loc = new Point(soldierToMoveTo.x , soldierToMoveTo.y);
//                        return new Point(x, y);
//                    }
//                    else {
//                        nearbySoldiers = false;
//                    }
//                }
//            }
//        }
//
//        if (stayPut) {
//            System.out.println(this.id + " is stopping for a while");
//            // don't move for 6 time steps
//            // because people need time to move to us, also need to
//            // make sure they move away from sight before resetting
//            if (stayPutTime < 6) {
//                stayPutTime += 1;
//                return new Point(0, 0);
//            }
//            //when done exchanging info, reset
//            stayPut = false;
//            stayPutTime = 0;
//            soldierToMoveTo = null;
//            nearbySoldiers = false;
//            if(sendstep%60 == 0) communicating = false;
//        }


        //  List<Point> path= proposePath();

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


        System.out.println(trustRecords.size()+"  "+id);
        if(tmpPath.size()==0){

            if(notobserved.size()>0){
                //System.out.println(notobserved.size());
                Point min = new Point(loc.x, loc.y);
                for(Point n: notobserved){
                    if(Math.abs(min.x-loc.x)+Math.abs(min.y-loc.y)<Math.abs(n.x-loc.x)+Math.abs(n.y-loc.y))
                        min = n;
                }
                tmpTar = min;
            }


            if (package_found && target_found) {
                tmpTar = package_loc;
                //  receiveRecord = true;
                if (loc.x == package_loc.x && loc.y == package_loc.y)
                    return new Point(0, 0);
            }
//            List<List<Point>> result = new ArrayList<>();
//            List<Point> path = new ArrayList<>();
//            path.add(loc);
//            List<Point> visited = new ArrayList<>();
//            visited.add(loc);
//            System.out.println(loc+"  "+tmpTar);
//            backtrack(result, path, visited, loc, tmpTar);
//            Collections.sort(result, (x, y) -> calculateTime(x)-calculateTime(y));
            maze = new Maze();
            maze.init(waterCells);
            //   System.out.println(loc+" "+tmpTar+" "+waterCells);
            tmpPath = maze.findPath(loc, tmpTar);

            //      System.out.println(tmpPath);
        }
        move = new Point(tmpPath.get(0).x- loc.x, tmpPath.get(0).y- loc.y);
        System.out.println(move);
        loc = tmpPath.get(0);
        tmpPath.remove(0);
        return move;

    }



//        } else if (tmpTar.x > loc.x && tmpTar.y > loc.y) {
//            move = new Point(1,1);
//        } else if (tmpTar.x < loc.x && tmpTar.y < loc.y) {
//            move = new Point(-1,-1);
//        } else if (tmpTar.x > loc.x && tmpTar.y < loc.y) {
//            move = new Point(1, -1);
//        } else if (tmpTar.x < loc.x && tmpTar.y > loc.y) {
//            move = new Point(-1, 1);
//        } else if (tmpTar.x > loc.x) {
//            move = new Point(1,0);
//        } else if (tmpTar.y > loc.y) {
//            move = new Point(0,1);
//        }
//        Random rand = new Random();
//        if((loc.x == 45  && loc.y ==50) || (loc.x == 45 && loc.y ==50)) offsetStatus  = 0;
//        List<Point> ll =new ArrayList<>();
//        ll.add(new Point(45, 50));
//        ll.add(new Point(45, 50));
    //System.out.println(rand.nextInt(2));
    //      if(offsetStatus == 1) tmpTar = ll.get(rand.nextInt(2));
//        PriorityQueue<Point> pq = new PriorityQueue<>((m, n)->((Math.abs(tmpTar.x-loc.x-m.x)+Math.abs(tmpTar.y-loc.y-m.y))-(Math.abs(tmpTar.x-loc.x-n.x)+Math.abs(tmpTar.y-loc.y-n.y))));
//
//        List<Point> l =new ArrayList<>();
//        l.add(new Point(1,1));
//        l.add(new Point(0,-1));
//        l.add(new Point(-1,0));
//        l.add(new Point(-1,-1));
//        l.add(new Point(1,-1));
//        l.add(new Point(-1,1));
//        l.add(new Point(1,0));
//        l.add(new Point(0,1));
//        int count = 8;
//        for(Point p:l){
//            if(!waterCells.contains(new Point(loc.x + p.x, loc.y + p.y)) || loc.x+p.x>100 || loc.x+p.x<0||loc.y+p.y>100 || loc.y+p.y<0){
//                pq.add(p);
//                count--;
//            }
//        }
//        if(count >= 5) offsetStatus = 1;
//        Point move = pq.poll();
//        loc = new Point(loc.x + move.x, loc.y + move.y);

    //    }
    // System.out.println("notobserved");
    // for (Point n: notobserved) {
    //     System.out.println(n.x + "," + n.y);
    // }
    // System.out.println("notobserved size: " + notobserved.size());

//        if(notobserved.size() > 0) {
//            // if we're currently moving over by 3
//            if (this.offsetStatus > 0 && this.offsetStatus < 3){
//                System.out.println("in the midst of an offset traversal");
//                if (this.explorationDirection == 5 || this.explorationDirection == 8) {
//                    move = new Point(0,1);
//                }
//                else if (this.explorationDirection == 6 || this.explorationDirection == 7) {
//                    move = new Point(0, 1);
//                }
//                this.offsetStatus += 1;
//                return move;
//            }
//            else {
//
//                // we've reached the end of the offset traversal, go in the opposite direction now
//                if (this.offsetStatus == 3) {
//                    if (this.explorationStrategy == 0) {
//                        getOppositeDiagonalDirection();
//                        this.offsetStatus = 0;
//                    }
//                    else {
//                        getOppositeOrthogonalDirection();
//                        this.offsetStatus = 0;
//                    }
//                }
//                // go diagonal
//                if (this.explorationStrategy == 0) {
//                    if (this.explorationDirection == 8) {
//                        move = new Point(-1,-1);
//                    }
//                    else if (this.explorationDirection == 7){
//                        move = new Point(1, -1);
//                    }
//                    else if (this.explorationDirection == 6) {
//                        move = new Point(1, 1);
//                    }
//                    else {
//                        move = new Point(-1, 1);
//                    }
//                    loc = new Point(loc.x + move.x, loc.y + move.y);
//                    // check to see if the place we're going is a water cell
//                    if (this.waterCells.contains(loc)) {
//                        // offset by 3 and keep going
//                        this.offsetStatus = 1;
//                        if (this.explorationDirection == 5 || this.explorationDirection == 8) {
//                            move = new Point(0,-1);
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(0,1);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(1,0);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(-1,0);
//                            }
//                        }
//                        else if (this.explorationDirection == 6 || this.explorationDirection == 7) {
//                            move = new Point(0, 1);
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(0,-1);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(1,0);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(-1,0);
//                            }
//                        }
//                    }
//                    // if we hit the edge, we want to move 3 away and go in the opposite direction
//                    if (loc.x >= 100 || loc.y >= 100 || loc.x == 0 || loc.y == 0) {
//                        this.offsetStatus = 1;
//                        // hit the far right wall
//                        if (loc.x >= 100 && this.explorationDirection == 5) {
//                            // go up
//                            move = new Point(0, -1);
//                        }
//                        else if (loc.x >=100 && this.explorationDirection == 6) {
//                            move = new Point(0, 1);
//                        }
//                        else if (loc.x == 0 && this.explorationDirection == 7) {
//                            move = new Point(0, -1);
//                        }
//                        else if (loc.x == 0 && this.explorationDirection == 8) {
//                            move = new Point(0, 1);
//                        }
//                        else if (loc.y >= 100 && this.explorationDirection == 6) {
//                            move = new Point(-1,0);
//                        }
//                        else if (loc.y >= 100 && this.explorationDirection == 7) {
//                            move = new Point(1,0);
//                        }
//                        else if (loc.y == 0 && this.explorationDirection == 8) {
//                            move = new Point(-1,0);
//                        }
//                        else {
//                            move = new Point(1, 0);
//                        }
//                    }
//                }
//                // if we're going horizontal/vertical
//                else {
//                    System.out.println("Going vertical");
//                    if (this.explorationDirection == 1) {
//                        move = new Point(0,-1);
//                    }
//                    else if (this.explorationDirection == 2){
//                        move = new Point(1, 0);
//                    }
//                    else if (this.explorationDirection == 3) {
//                        move = new Point(0, 1);
//                    }
//                    else {
//                        move = new Point(-1, 0);
//                    }
//                    loc = new Point(loc.x + move.x, loc.y + move.y);
//                    // check to see if the place we're going is a water cell
//                    if (this.waterCells.contains(loc)) {
//                        // offset by 3 and keep going
//                        this.offsetStatus = 1;
//                        if (this.explorationDirection == 2 || this.explorationDirection == 4) {
//                            move = new Point(0,1);
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(0,-1);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(1,0);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(-1,0);
//                            }
//                        }
//                        else if (this.explorationDirection == 1 || this.explorationDirection == 3) {
//                            move = new Point(0, -1);
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(0,1);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(1,0);
//                            }
//                            if (this.waterCells.contains(move)) {
//                                move = new Point(-1,0);
//                            }
//                        }
//                    }
//                    // if we hit the edge, we want to move 3 away and go in the opposite direction
//                    if (loc.x >= 100 || loc.y >= 100 || loc.x == 0 || loc.y == 0) {
//                        this.offsetStatus = 1;
//                        if (this.explorationDirection == 1 || this.explorationDirection == 3) {
//                            move = new Point(0,-1);
//                        }
//                        else if (this.explorationDirection == 2 || this.explorationDirection == 4) {
//                            move = new Point(0, 1);
//                        }
//
//                    }
//
//                }
//                /*Collections.sort(notobserved, pointComparator);
//                  destination = notobserved.get(0);
//                // System.out.println("DEST:"+destination.x + " " + destination.y);
//                if(destination.x > loc.x) {
//                move = new Point(1,0);
//                } else if (destination.x < loc.x) {
//                move = new Point(-1,0);
//            } else {
//                if (destination.y > loc.y) {
//                    move = new Point(0,1);
//                } else {
//                    move = new Point(0,-1);
//                }
//            }*/
//            }



//
//            else if (package_found || target_found) {
//                System.out.println("something found");
//                // only move to non-muddy cells
//                // while (mudCells.contains(loc)) {
//                //     Random rand = new Random();
//                //     int x = rand.nextInt(2) * 2 - 1;
//                //     int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
//                //     move = new Point(x, y);
//                //     loc = new Point(loc.x + move.x, loc.y + move.y);
//                // }
//            }
    // }

    //System.out.println("NEW LOC:"+loc.x + " " + loc.y);

    //}
    private void getOppositeDiagonalDirection() {
        if (this.explorationDirection == 5) {
            this.explorationDirection = 7;
        }
        else if (this.explorationDirection == 6) {
            this.explorationDirection = 8;
        }
        else if (this.explorationDirection == 7) {
            this.explorationDirection = 5;
        }
        else {
            this.explorationDirection = 6;
        }

    }

    private void getOppositeOrthogonalDirection() {
        if (this.explorationDirection == 1) {
            this.explorationDirection = 3;
        }
        else if (this.explorationDirection == 2) {
            this.explorationDirection = 4;
        }
        else if (this.explorationDirection == 3) {
            this.explorationDirection = 1;
        }
        else {
            this.explorationDirection = 2;
        }

    }


}

