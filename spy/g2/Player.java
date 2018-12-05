package spy.g2;

import java.util.*;
import spy.sim.*;


public class Player implements spy.sim.Player {
    private class Pair implements Comparable<Pair>{
        public Point pt;
        public int dist;

        public Pair(Point p, int t){
            pt = p;
            dist = t;
        }

        @Override
        public int compareTo(Pair other) {
            return new Integer(dist).compareTo(other.dist);
        }
    }

    private ArrayList<ArrayList<Record>> records;
    private int id;
    private Point loc;
    private List<Point> water;
    private List<Point> path;
    private Boolean path_found;

    private Boolean package_found;
    private Boolean target_found;

    private Point package_loc;
    private Point target_loc;
    
    private boolean[][] viewed;
    private int[][] graph;

    private HashMap<Integer, Integer> Met;
    private List<Point> todo;


    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();        
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            for (int j = 0; j < 100; j++)
            {
                row.add(null);
            }
            this.records.add(row);
        }

        this.graph = new int[100][100];
        this.viewed = new boolean[100][100];
        for (int i=0; i<99; i++){
            for (int j=0; j<99; j++){
                viewed[i][j] = false;
                graph[i][j] = -1;
            }
        }

        this.loc = startingPos;
        this.water = waterCells;

        for (Point p: waterCells){
            viewed[p.x][p.y] = true; 
            graph[p.x][p.y] = -2;
        }

        this.package_found = false;
        this.package_loc = null;

        this.target_found = false;
        this.target_loc = null;

        this.path_found = false;
        this.path = null;

        this.todo = new ArrayList<Point>();
        this.Met = new HashMap<Integer,Integer>();
        for (int i=0; i<n; i++){
            Met.put(i,0);
        }
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
            
            viewed[p.x][p.y] = true;
            
            if (status.getC() == 0){
                graph[p.x][p.y] = 0;
            }
            
            else if (status.getC() == 1){
                graph[p.x][p.y] = -1;
            }
            
            if (status.getPT() == 1){
                System.out.printf("%d Package Found\n",id);
                this.package_found = true;
                this.package_loc = p;
                graph[p.x][p.y] = 1;
            }
            else if (status.getPT() == 2) {

                System.out.printf("%d Target Found\n",id);
                this.target_found = true;
                this.target_loc = p; 
                graph[p.x][p.y] = 1;
            }
            if (!path_found){
                if (status.getPresentSoldiers().size() != 0){
                    for (Integer i: status.getPresentSoldiers()){
                        if (i == this.id){
                            continue;
                        }
                        if (Met.get(i)>0){
                            continue;
                        }
                        if (i > this.id){
                            // wait for five turns for it to get here
                            todo.add(0,new Point(0,0));
                            todo.add(0,new Point(0,0));
                            todo.add(0,new Point(0,0));
                            todo.add(0,new Point(0,0));
                            todo.add(0,new Point(0,0));
                        }
                        else{
                            System.out.printf("%d moving toward %d",this.id, i);
                            move_toward(p);
                            break;
                        }
                    }
                }
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
        }
    }
    
    public List<Record> sendRecords(int id)
    {
        if (Met.get(id) > 0){
            return new ArrayList<Record>();
        }
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
        Met.put(id,30);
        return toSend;
    }

    public void receiveRecords(int id, List<Record> rec)
    {
        /*
        Iterator itr = todo.iterator(); 
        while (itr.hasNext()) 
        { 
            Point p = (Point)itr.next(); 
            if (p.x == 0 && p.y == 0){
                itr.remove();
            }
        } 
        */
        for (Record r : rec){
            Point p = r.getLoc();

            if (viewed[p.x][p.y]){
                continue;
            }
            records.get(r.getLoc().x).set(r.getLoc().y, r);
            viewed[p.x][p.y] = true;

            if (r.getC() == 0){
                graph[p.x][p.y] = 0;
            }
            
            if (r.getC() == 1){
                graph[p.x][p.y] = -1;
            }
            if (r.getPT() == 1){
                this.package_found = true;
                this.package_loc = r.getLoc();
                graph[p.x][p.y] = 1;
            }
            else if (r.getPT() == 2) {
                this.target_found = true;
                this.target_loc = r.getLoc();
                graph[p.x][p.y] = 2;
            }
        }
    }
    
    public List<Point> proposePath(){
        return path;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths){
        List<Integer> votes = new ArrayList<Integer>();
        for (Integer p: paths.keySet()){
            votes.add(p);
            return votes;
        }
        return votes;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
    }
    
    public Point getMove(){
        double max_reward = 0;
        Point best_move = new Point(-1000,-1000);
                
        //System.out.printf("id: %d Current loc: %d %d todo: %d\n", this.id, loc.x, loc.y, todo.size());

        /*
        int ct = 0;
        for (int i=0; i<100; i++){
            for (int j=0; j<100; j++){
                if (viewed[i][j]==false){
                    ct++;
                }
            }
        }
        System.out.printf("remaining cells: %d \n", ct);
        
        */
        
        if (todo.size() > 0){
            return move_to(todo.remove(todo.size()-1));
        }

        if (package_found && target_found){
            if (path_found && loc.x == package_loc.x && loc.y == package_loc.y){
                System.out.printf("%d is waiting \n",id);
                return new Point(0,0);
            }
            else if (dfs(package_loc, target_loc) != null){
                path = dfs(package_loc,target_loc);
                path_found = true;
                move_toward(package_loc);
                System.out.printf("%d moving towards package at %d %d\n",id,package_loc.x,package_loc.y);
                for (Point p: todo){
                    System.out.printf("%d %d\n",p.x,p.y);
                }
                System.out.println("-----------------------");
            }
            else{
                explore();
            }
        }
        else{
            explore();
        }
        if (todo.size()>0){
            return move_to(todo.remove(todo.size()-1));
        }
        else{
            return new Point(0,0);
        }

    }

    // add all neighboring points to a list
    // return : a List contains all the neighbors of Point p, including itself
    public List<Point> neighbors(Point p){
        List<Point> neighbors = new ArrayList<Point>();
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                int dx = p.x+i-1;
                int dy = p.y+j-1;
                if (i==1 && j==1){
                    continue;
                }
                if (dx < 0  || dy < 0 || dx > 99 || dy > 99 || graph[dx][dy] == -2){
                    continue;
                }
                neighbors.add(new Point(dx,dy));
            }
        }
        return neighbors;
    }
    
    public int exploration_reward(Point p){
        int reward = 0;
        if (graph[p.x][p.y] == -2){
            return reward;
        }
        for (int i=0; i<100; ++i){
            for (int j=0; j<100; ++j){
                if (Math.abs(i-p.x) < 3 && Math.abs(j-p.y) < 3){
                    if (viewed[i][j]){
                        continue;
                    }
                    reward ++;
                }
            }
        }
        return reward;
    }

    public void explore(){
        Map<Point, Point> par = new HashMap<Point, Point>();
        int[][] visited = new int[100][100];
        PriorityQueue<Pair> pQueue = new PriorityQueue<Pair>(); 
        double max_reward = 0;
        boolean flag = true;
        Point dest = new Point(-100,-100);

        //+--------------------------------------------------------------------------+
        //| find the cell with max exploration / dist                                |
        //+--------------------------------------------------------------------------+
        pQueue.add(new Pair(loc,0));
        while(pQueue.size() > 0 && flag){
            Pair pa = pQueue.poll();
            Point p = pa.pt;
            int dist = pa.dist;

            visited[p.x][p.y] = 1;
            for (Point n: neighbors(p)){
                if (visited[n.x][n.y]==1 || graph[n.x][n.y] == -2){
                    continue;
                }
                if ((double)exploration_reward(n)/cost(p,n) > max_reward){
                    dest = n;
                    flag = false;
                    max_reward = (double)exploration_reward(n)/cost(p,n);
                }
                pQueue.add(new Pair(n, dist+cost(p,n)));
                par.put(n,p);
                visited[n.x][n.y] = 1;
            }
        }
        Point cur = dest;
        while (par.containsKey(cur)){
            todo.add(new Point(cur.x,cur.y));
            cur = par.get(cur);
        }
    }

    public Point move_to(Point dest){
        return new Point(dest.x-loc.x, dest.y-loc.y);
    }

    public void move_toward(Point dest){
        Map<Point, Point> par = new HashMap<Point, Point>();
        int[][] visited = new int[100][100];
        PriorityQueue<Pair> pQueue = new PriorityQueue<Pair>(); 
        pQueue.add(new Pair(loc,0));
        while(pQueue.size() > 0){
            Pair pa = pQueue.poll();
            Point p = pa.pt;
            int dist = pa.dist;
            /*
            System.out.printf("%d %d\n",p.x, p.y);
            for (int i=0; i<100;++i){
                for (int j=0; j<100;++j){
                    if (visited[i][j]==1)
                        System.out.printf("%d %d\n",i,j);
                }
            }
            System.out.printf("=============================\n");
            */
            visited[p.x][p.y] = 1;
            for (Point n: neighbors(p)){
                if (visited[n.x][n.y] == 1 || graph[n.x][n.y] == -2){
                    continue;
                }
                if (n.x == dest.x && n.y == dest.y){
                    par.put(n,p);
                    
                    Point cur = n;
                    while (par.containsKey(cur)){
                        todo.add(new Point(cur.x,cur.y));
                        cur = par.get(cur);
                    }                
                }
                pQueue.add(new Pair(n, dist+cost(p,n)));
                par.put(n,p);
                visited[n.x][n.y] = 1;
            }
        }
    }

    public boolean move_possible(Point dest){
        List<Point> t = new ArrayList<Point>();
        int [][] visited = new int[100][100];
        t.add(loc);
        while(t.size() > 0){
            Point p = t.remove(t.size()-1);
            visited[p.x][p.y] = 1;
            for (Point n: neighbors(p)){
                if (n.x == dest.x && n.y==dest.y){
                    return true;
                }
                if (visited[n.x][n.y] == 0){
                    if (graph[n.x][n.y] > 0){
                        t.add(n);
                    }
                    visited[n.x][n.y] = 1;
                }
            }
        }
        return false;
    }

    public List<Point> find_path(Point start, Point dest){
        List<Point> path = new ArrayList<Point>();
        Map<Point, Point> par = new HashMap<Point, Point>();
        int[][] visited = new int[100][100];
        PriorityQueue<Pair> pQueue = new PriorityQueue<Pair>(); 

        pQueue.add(new Pair(start,0));
        Point cur = this.loc;
        boolean path_found = true;
        while(pQueue.size() > 0 && path_found){
            Pair pa = pQueue.poll();
            Point p = pa.pt;
            int dist = pa.dist;
            visited[p.x][p.y] = 1;
            /*
            System.out.printf("%d %d\n",p.x, p.y);
            for (int i=0; i<100; i++){
                for (int h=0; h<100; h++){
                    if (visited[i][h]==1){
                        System.out.printf("%d %d\n",i,h);
                    }
                }
            }
            System.out.printf("=================================\n");
            */
            for (Point n: neighbors(p)){
                if (graph[n.x][n.y] < 0 || visited[n.x][n.y] == 1){
                    continue;
                }
                if (n.x == dest.x && n.y == dest.y){
                    cur = n;
                    path_found = false;
                }
                pQueue.add(new Pair(n, dist+cost(p,n)));
                par.put(n,p);
                visited[n.x][n.y] = 1;
            }
        }
        //System.out.printf("From %d %d to %d %d \n", package_loc.x, package_loc.y, target_loc.x, target_loc.y);
        while (par.containsKey(cur)){
            path.add(new Point(cur.x, cur.y));
            cur = par.get(cur);
        }
        path.add(cur);
        Collections.reverse(path);
        
        return path;
    }

    public int cost(Point p, Point n){
        if (Math.abs(n.x-p.x)+Math.abs(n.y-p.y)==1){
            if (graph[n.x][n.y]==1){
                return 2;
            }
            else{
                return 4;
            }
        }
        else if (Math.abs(n.x-p.x)+Math.abs(n.y-p.y)==2){
            if (graph[n.x][n.y]==1){
                return 3;
            }
            else{
                return 6;
            }   
        }
        return 10;    
    }

    public int dist(Point a, Point b){
        int dx = Math.abs(a.x-b.x);
        int dy = Math.abs(a.y-b.y);
        int dst = 0;
        while (dx > 0 && dy > 0){
            dx = dx -1;
            dy = dy -1;
            dst += 3;
        }
        dst += 2*(dx+dy);
        return dst;
    }

    private List<Point> dfs(Point s, Point t) {
        // List<List<Integer>> record = new ArrayList<List<Integer>>();
        // for (int i = 0; i < 100; i++) {
        //     List<Integer> list = new ArrayList<Integer>(Collections.nCopies(100, 0));
        //     record.add(list);
        // }
        Map<Point, Integer> dist = new HashMap<Point, Integer>();
        PriorityQueue<Pair> queue = new PriorityQueue<Pair>();
        Map<Point, Point> prev = new HashMap<Point, Point>();
        boolean[][] v = new boolean[100][100];
        List<Point> proposal = new ArrayList<Point>();

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                Point p = new Point(i, j);
                dist.put(p, Integer.MAX_VALUE);
                // prev.put(p, null);
            }
        }

        dist.put(s, 0);
        queue.add(new Pair(s, 0));
        Pair pair = queue.poll();
        Point next = pair.pt;
        v[next.x][next.y] = true;
        // System.out.println("preprocessing done, starting point: x = " + next.x + ", y = " + next.y);
        while (!next.equals(t)) {
            // System.out.println("next: x = " + next.x + ", y = " + next.y);
            for (int i = next.x - 1; i < next.x + 2; i++) {
                for (int j = next.y - 1; j < next.y + 2; j++) {
                    Point candidate = new Point(i, j);
                    // System.out.println("candidate: x = " + i + ", y = " + j);
                    if (i < 0 || i > 99 || j < 0 || j > 99 || v[i][j] || water.contains(candidate) || records.get(i).get(j) == null || records.get(i).get(j).getC() == 1 /*|| record.get(i).get(j) != 0*/) {
                        continue;
                    }
                    // double distance = Math.abs(next.x - i) + Math.abs(next.y - j);
                    int distance = dist(next, candidate);
                    // System.out.println("valid candidate: x = " + i + ", y = " + j + ", distance: " + distance);
                    if (dist.get(next) + distance < dist.get(candidate)) {
                        dist.put(candidate, dist.get(next) + distance);
                        prev.put(candidate, next);
                        queue.add(new Pair(candidate, dist.get(next) + distance));
                    }
                }
            }

            if (queue.size() == 0) {
                break;
            }

            pair = queue.poll();
            next = pair.pt;
            v[next.x][next.y] = true;
        }

        System.out.println("while loop ended");

        if (!prev.containsKey(t)) {
            return null;
        }

        Point p = t;
        proposal.add(p);
        System.out.println("shortest path, point: x = " + p.x + ", y = " + p.y);
        while (prev.containsKey(p)) {
            p = prev.get(p);
            proposal.add(p);
            System.out.println("shortest path, point: x = " + p.x + ", y = " + p.y);
        }

        if (proposal.get(0).equals(target_loc)) {
            Collections.reverse(proposal);
        }

        if (proposal.size() == 0) {
            return null;
        }

        return proposal;
    }
}
