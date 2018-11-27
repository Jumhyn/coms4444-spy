package spy.g2;

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

public class Player implements spy.sim.Player {

    private ArrayList<ArrayList<Record>> records;
    private ArrayList<ArrayList<Record>> map;
    private ArrayList<ArrayList<Boolean>> visited;
    private int id;
    private Point loc;
    private List<Point> water;
    private boolean package_found;
    private Point package_loc;
    private Point target_loc;
    private List<Point> route;
    private boolean readyForRoute;
    private boolean routeFound;
    private Point start;
    private Point destination;
    private List<Point> Friend;
    private HashMap<Integer, Integer> Met;


    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        // this.map = new ArrayList<ArrayList<Record>>();
        this.visited = new ArrayList<ArrayList<Boolean>>();
        this.route = new ArrayList<Point>();
        
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            ArrayList<Record> r = new ArrayList<Record>();
            ArrayList<Boolean> v = new ArrayList<Boolean>();
            for (int j = 0; j < 100; j++)
            {
                row.add(null);
                r.add(null);
                v.add(false);
            }
            this.records.add(row);
            // this.map.add(r);
            this.visited.add(v);
        }
        this.loc = startingPos;
        this.water = waterCells;
        this.package_found = false;
        this.package_loc = null;
        // this.package_loc = new Point(0, 0);
        this.target_loc = null;
        this.readyForRoute = false;
        this.routeFound = false;
        this.start = null;
        this.destination = null;
        this.Met = new HashMap<Integer,Integer>();
        for (int i=0; i<n; i++){
            Met.put(i,0);
        }

    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        this.Friend = new ArrayList<Point>();
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
            if (status.getPT() == 1){
                this.package_found = true;
                this.package_loc = p;
                if (start != null && this.target_loc != null && start.equals(this.target_loc)) {
                    destination = this.package_loc;
                }
            }
            else if (status.getPT() == 2) {
                // System.out.println("target found");
                this.target_loc = p;
                if (start != null && this.package_loc != null && start.equals(this.package_loc)) {
                    // System.out.println("destination assigned");
                    destination = this.target_loc;
                    // System.out.println("destination: x = " + destination.x + ", y = " + destination.y);
                }
            }

            if (status.getPresentSoldiers().size() != 0){
                for (Integer i: status.getPresentSoldiers()){
                    if (i== this.id){
                        continue;
                    }
                    if (Met.get(i) < 15){
                        Friend.add(p);
                        break;
                    }
                }
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
            // map.get(p.x).set(p.y, new Record(p, status.getC(), status.getPT(), new ArrayList<Observation>()));
        }
    }
    
    public List<Record> sendRecords(int id)
    {
        ArrayList<Record> toSend = new ArrayList<Record>();
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
        Met.put(id,15);
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> rec)
    {
        for (Record r : rec){
            if (records.get(r.getLoc().x).get(r.getLoc().y) == null){
                records.get(r.getLoc().x).set(r.getLoc().y, r);
            }
        }
    }
    
    public List<Point> proposePath()
    {
        if (!routeFound) {
            return null;
        }
        
        if (route.get(0) == package_loc) {
            return route;
        }

        Collections.reverse(route);
        return route;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        List<Integer> votes = new ArrayList<Integer>();
        for (Integer p: paths.keySet()){
            votes.add(p);
        }
        return votes;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
    }
    
    public Point getMove()
    {
        double max_reward = 0;
        Point best_move = this.loc;
        Map<Point, Integer> possible_move = new HashMap<Point, Integer>();
        Point loc = this.loc;
        for (Integer i: Met.keySet()){
            if (Met.get(i)>0){
                Met.put(i, Met.get(i)-1);
            }
        }
        System.out.printf("Current loc: %d %d \n", loc.x, loc.y);
        if (routeFound) {
            System.out.println("route found");
            if (!loc.equals(package_loc)) {
                // System.out.println("moving towards package location");
                return move_toward(package_loc);
            }
            else {
                // stay at package loc
                return new Point(0,0);
            }
        }

        if (Friend.size() != 0){
            System.out.println("Moving towards friend");
            Point p = Friend.remove(0);

            System.out.printf("at %d %d \n", p.x, p.y);
            return move_toward(p);
        }

        if (!readyForRoute && (loc.equals(package_loc) || loc.equals(target_loc))) {
            System.out.println("package or target found");
            readyForRoute = true;
            if (loc.equals(package_loc)) {
                start = package_loc;
                destination = target_loc;
            }
            else {
                start = target_loc;
                destination = package_loc;
            }
        }

        if (readyForRoute) {
            System.out.println("moving towards destination");
            System.out.println("loc x = " + this.loc.x + ", y = " + this.loc.y);
            Point next = findValidPathOneMove(loc);
            // System.out.println("x = " + next.x + ", y = " + next.y);
            // target found
            if (next == null) {
                return move_toward(package_loc);
            }

            // this.loc = next;
            //System.out.println("x = " + next.x + ", y = " + next.y);
            // System.out.println("x = " + this.loc.x + ", y = " + this.loc.y);
            return next;
        }

        if (package_loc != null && !loc.equals(package_loc)){
            System.out.println("moving toward package");
            // return move_toward(package_loc);
            Point next = move_toward(package_loc);
            // System.out.println("x = " + next.x + ", y = " + next.y);
            return next;

        }

        if (target_loc != null && !loc.equals(target_loc)) {
            return move_toward(target_loc);
        }
        map = records;

        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                int dx = loc.x+i-1;
                int dy = loc.y+j-1;
                if (i==1 && j==1){
                    continue;
                }
                if (map.get(dx).get(dy).getC() == 1){
                    possible_move.put(new Point(i-1,j-1),4);
                }
                if(this.water.contains(new Point(dx,dy))){
                    continue;
                }
                else{
                    if (Math.abs(i)+Math.abs(j)>1){
                        if (map.get(dx).get(dy).getC() == 1){
                            possible_move.put(new Point(i-1,j-1),6);
                        }
                        else{
                            possible_move.put(new Point(i-1,j-1),3);
                        }
                    }
                    else{

                        if (map.get(dx).get(dy).getC() == 1){
                            possible_move.put(new Point(i-1,j-1),4);
                        }
                        else{
                            possible_move.put(new Point(i-1,j-1),2);
                        }
                    }
                }
            }
        }
        System.out.println("exploring");
        for (Point p: possible_move.keySet()){
            Point nex = new Point(loc.x+p.x,loc.y+p.y);
            if (exploration_reward(nex)/possible_move.get(p)>max_reward){
                max_reward = exploration_reward(nex)/possible_move.get(p);
                best_move = p;
            }
        }
        if (max_reward == 0){
            best_move = find_unknown(loc);
        }
        return best_move;
    }

    public int exploration_reward(Point p){
        int reward = 0;
        for (int i=0; i<map.size();++i){
            for (int j=0; j<map.get(i).size();++j){
                if (Math.abs(i-p.x) < 3 && Math.abs(j-p.y) < 3){
                    if (map.get(i).get(j)==null){
                        reward ++;
                    }
                }
            }
        }
        return reward;
    }

    public Point find_unknown(Point loc){
        int minimum = 200;
        int tx = 0;
        int ty = 0;
        for (int i=0; i<map.size();++i){
            for (int j=0; j<map.get(i).size();++j){
                if (map.get(i).get(j)==null){
                    if (Math.abs(i-loc.x)+Math.abs(j-loc.y)< minimum){
                        minimum = Math.abs(i-loc.x)+Math.abs(j-loc.y);
                        tx = i;
                        ty = j;
                    }
                }
            }
        }
        return move_toward(new Point(tx, ty));
    }


    public Point move_toward(Point p){
        int dx = 0;
        int dy = 0;
        if (p.x > this.loc.x){
            dx = 1;
        }
        else if (p.x == this.loc.x){
            dx = 0;
        }
        else{
            dx = -1;
        }
        if (p.y > this.loc.y){
            dy = 1;
        }
        else if (p.y == this.loc.y){
            dy = 0;
        }
        else{
            dy = -1;
        }
        return new Point (dx, dy);
    }

    // private void findValidPath(Point start) {
    //     Point curr = start;
    //     Point destination = null;
    //     if (curr == package_loc) {
    //         destination = target_loc;
    //     }
    //     else {
    //         destination = package_loc;
    //     }
    //     route.add(curr);
    //     visited.get(curr.x).set(curr.y, true);
    //     while (target_loc == null || curr != destination) {
    //         Point next = getNextPos(curr);
    //         if (next != null) {
    //             route.add(next);
    //             visited.get(next.x).set(next.y, true);
    //             curr = next;
    //         }
    //         else {
    //             route.remove(this.route.size() - 1);
    //             curr = route.get(this.route.size() - 1);
    //         }
    //     }
    // }

    private Point findValidPathOneMove(Point p) {
        // System.out.println("find valid path begins");
        // System.out.println("p: x = " + p.x + ", y = " + p.y);
        if (destination != null && p.equals(destination)) {
            routeFound = true;
            // System.out.println("destination is not null or current point equals to destination");
            return null;
        }

        route.add(p);
        visited.get(p.x).set(p.y, true);
        Point next = getNextPos(p);
        if (next != null) {
            return new Point(next.x - p.x, next.y - p.y);
            // return next;
        }

        route.remove(route.size() - 1);
        // return route.remove(route.size() - 1);
        Point prev = route.remove(route.size() - 1);
        return new Point(prev.x - p.x, prev.y - p.y);
    }

    private Point getNextPos(Point curr) {
        // if not visited, not muddy, then go, otherwise return null
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Point next = new Point(curr.x + i, curr.y + j);
                if (next.x < 0 || next.y < 0 || next.x > 99 || next.y > 99) {
                    continue;
                }
                if (visited.get(next.x).get(next.y)) {
                    continue;
                }

                if (records.get(next.x).get(next.y).getC() == 1) {
                    continue;
                }

                if (water.contains(next)) {
                    continue;
                }

                return next;
            }
        }

        return null;
    }
}
