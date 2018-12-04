package spy.g1;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Random;

import spy.g1.Edge;

//import javafx.scene.shape.MoveTo;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class Player implements spy.sim.Player {

    private ArrayList<ArrayList<Record>> records;
    //private ArrayList<ArrayList<Record>> map;
    //private HashMap<Point,Boolean> visited;
    private int id;
    private Point loc;
    private HashMap<String,Point> water = new HashMap<String,Point>();
    //private HashSet existingEdges = new HashSet();
    private Dijkstra djk = new Dijkstra();

    private Point packageLocation;
    private Point targetLocation;
    private int moveMode;
    private boolean findPackage, findTarget;
    private List<Point> ourPath;
    private Queue<Vertex> moves = new LinkedList<>();


    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        // Hashmap of water cells for more efficient check
        for (Point w : waterCells){
          int x = w.x;
          int y = w.y;
          String p = Integer.toString(x) + "," + Integer.toString(y);
          water.put(p, w);
          // System.out.print(water.containsKey(p));
          // System.out.println(p);
        }

        // Construct Dijkstra graph of land cells
        this.id = id;
        this.records = new ArrayList<ArrayList<Record>>();
        this.ourPath = new ArrayList<Point>();
        //this.map = new ArrayList<ArrayList<Record>>();
        //this.visited = new HashMap<Point,Boolean>();
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();

            for (int j = 0; j < 100; j++)
            {
            	//int[] coords = {i,j};
                String name = Integer.toString(i) + "," + Integer.toString(j);
                Vertex newVertex = new Vertex(name,i,j);
                //System.out.print(water.contains(newVertex));
                if(!water.containsKey(name)){
                  djk.addVertex(newVertex);
                  // System.out.println(newVertex);
                }
                row.add(null);
                //visited.put(new Point(i,j),false);
            }
            this.records.add(row);
        }
        for (Vertex source : djk.getVertices()){
            // construct edge weights -- assume muddy
            setIncomingEdges(source, true);
        }

        // doesn't know package location or target location at beginning
        this.findPackage = false;
        this.findTarget = false;
        this.moveMode = 0;
        // moveMode = 0, initial exploration
        // moveMode = 1, saw package or target
        // moveMode = 2, reached package or target -- looking for other one
        // moveMode = 3, saw the other one -- trying to reach target
        // moveMode = 4, saw the other one -- go to package to propose path
        // moveMode = 5, done -- just stay put
    }

    private void setIncomingEdges(Vertex source, boolean isMuddy) {
        int x = source.x;
        int y = source.y;
        int[][] adjacent = {
            {x-1,y-1},
            {x-1,y},
            {x-1,y+1},
            {x,y+1},
            {x+1,y+1},
            {x+1,y},
            {x+1,y-1},
            {x,y-1}
        };

        for (int k = 0; k < adjacent.length; ++k) {
            int i = adjacent[k][0], j = adjacent[k][1];
            String name = Integer.toString(i) + "," + Integer.toString(j);

            if(i>=0 && i<=99 && j>=0 && j<=99 && !water.containsKey(name)) {
                Vertex target = djk.getVertex(name);
                Vertex[] key = {target, source};
                double weight = (k%2==0) ? 3 : 2;
                if (isMuddy) {
                  if (moveMode<2 || moveMode>3) {weight *= 2;}
                  if (moveMode>=2 && moveMode<=3) {weight = Double.POSITIVE_INFINITY;}
                }
                djk.setEdge(target.name, source.name, weight);
                //existingEdges.add(key);
            }
        }
    }

    // updates the state of the player based on surroundings
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        // update location
        this.loc = loc;


        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();

            // record the data learned
            Record record = records.get(p.x).get(p.y);
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            //map.get(p.x).set(p.y, new Record(p, status.getC(), status.getPT(), new ArrayList<Observation>()));
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));

            update(record);

          //   // check tile status
          //   if(record.getPT() != 0) {
          //       switch(moveMode) {
          //           case 0:
          //               moveMode = 1; // reach the first special tile
          //               break;
          //           case 2:
          //               if(findPackage && record.getPT()==2) {
          //                   moveMode = 3;
          //                   // found package first and just discorvered target
          //                   // move to target
          //               } else if(findTarget && record.getPT()==1) {
          //                   moveMode = 4;
          //                   // found target first and just discovered package
          //                   // just go to package and we're done
          //               }
          //               break;
          //           default:
          //               break;
          //       }

          //       if(record.getPT()==1) {
          //           this.findPackage = true;
          //           this.packageLocation = p;
          //       } else {
          //           this.findTarget = true;
          //           this.targetLocation = p;
          //       }
          //   }

          //   // update the graph to reflect new information
          //   String name = Integer.toString(p.x) + "," + Integer.toString(p.y);
          //   if(!water.containsKey(name)) {
          //   	Vertex v = djk.getVertex(name);
          //   	v.explored = true;
		        // setIncomingEdges(v, record.getC()==1);
          //   }

          //   // check on location
          //   boolean atPackage = this.loc.equals(packageLocation);
          //   boolean atTarget = this.loc.equals(targetLocation);
          //   if(atPackage || atTarget) {
          //       switch(moveMode) {
          //           case 1:
          //               moveMode = 2;
          //               // update graph so all muddy edges are infinite
          //               for (Vertex source : djk.getVertices()){
          //                   Record r = records.get(source.x).get(source.y);
          //                   if(r!=null && r.getC()==1) {
          //                       setIncomingEdges(source, true);
          //                   }
          //               }
          //               break;
          //           case 3:
          //               if(atTarget) {moveMode = 4;}
          //               break;
          //           case 4:
          //               if(atPackage) {moveMode = 5;}
          //               break;
          //           default:
          //               break;
          //       }
          //   }
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
        return toSend;
    }

    public void receiveRecords(int id, List<Record> records)
    {
        // for(int i=0;i<records.size();i++){
        //     Record newRecord = records.get(i);
        //     Point curPoint = newRecord.getLoc();
        //     Record preRecord = this.records.get(curPoint.x).get(curPoint.y);

        //     if(newRecord.getPT()==1){
        //         packageLocation = curPoint;
        //         findPackage = true;
        //     }
        //     else if(newRecord.getPT()==2){
        //         targetLocation = curPoint;
        //         findTarget = true;
        //     }
        //     else{
        //         if(preRecord == null){
        //             this.records.get(curPoint.x).set(curPoint.y,newRecord);
        //         }
        //         else{
        //             newRecord.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
        //         }
        //     }
        // }

        for(Record rec: records) {
        	// record the data learned
        	Point p = rec.getLoc();
            Record record = this.records.get(p.x).get(p.y);
            if (record == null || record.getC() != rec.getC() || record.getPT() != rec.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(rec);
                this.records.get(p.x).set(p.y, record);
            }
            //map.get(p.x).set(p.y, new Record(p, status.getC(), status.getPT(), new ArrayList<Observation>()));
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));

            update(rec);
        }
    }

    private void update(Record record) {
    	Point p = record.getLoc();

    	// check tile status
        if(record.getPT() != 0) {
            switch(moveMode) {
                case 0:
                    moveMode = 1; // reach the first special tile
                    break;
                case 2:
                    if(findPackage && record.getPT()==2) {
                        moveMode = 3;
                        // found package first and just discorvered target
                        // move to target
                    } else if(findTarget && record.getPT()==1) {
                        moveMode = 4;
                        // found target first and just discovered package
                        // just go to package and we're done
                    }
                    break;
                default:
                    break;
            }

            if(record.getPT()==1) {
                this.findPackage = true;
                this.packageLocation = p;
            } else {
                this.findTarget = true;
                this.targetLocation = p;
            }
        }

        // update the graph to reflect new information
        String name = Integer.toString(p.x) + "," + Integer.toString(p.y);
        if(!water.containsKey(name)) {
        	Vertex v = djk.getVertex(name);
        	v.explored = true;
	        setIncomingEdges(v, record.getC()==1);
        }

        // check on location
        boolean atPackage = this.loc.equals(packageLocation);
        boolean atTarget = this.loc.equals(targetLocation);
        if(atPackage || atTarget) {
            switch(moveMode) {
                case 1:
                    moveMode = 2;
                    // update graph so all muddy edges are infinite
                    for (Vertex source : djk.getVertices()){
                        Record r = records.get(source.x).get(source.y);
                        if(r!=null && r.getC()==1) {
                            setIncomingEdges(source, true);
                        }
                    }
                    break;
                case 3:
                    if(atTarget) {moveMode = 4;}
                    break;
                case 4:
                    if(atPackage) {moveMode = 5;}
                    break;
                default:
                    break;
            }
        }
    }

    public List<Point> proposePath()
    {
        String packageLoc;
        String targetLoc;
        packageLoc = Integer.toString(packageLocation.x) + "," + Integer.toString(packageLocation.y);
        targetLoc = Integer.toString(targetLocation.x) + "," + Integer.toString(targetLocation.y);
        List<Edge> validPath = djk.getDijkstraPath(packageLoc,targetLoc);
        for(int i=0;i<validPath.size();i++){
            Vertex nextVertex = validPath.get(i).target;
            Point nextPoint = new Point(nextVertex.x,nextVertex.y);
            System.out.println("step"+i+nextPoint);
            this.ourPath.add(nextPoint);
        }
        if(ourPath.size()>1){
            return ourPath;
        }
        return null;
    }

    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            return toReturn;
        }
        return null;
    }
    public void receiveResults(HashMap<Integer, Integer> results)
    {

    }

    // runs algorithms to decide which move to make based on the current state
    public Point getMove()
    {
        //System.err.println(moveMode);

        // moveMode = 0, initial exploration
        // moveMode = 1, saw package or target
        // moveMode = 2, reached package or target -- looking for other one
        // moveMode = 3, saw the other one -- trying to reach target
        // moveMode = 4, saw the other one -- go to package to propose path
        // moveMode = 5, done -- just stay put
        if(this.moves.isEmpty()){
          List<Edge> curPath;
          String source = Integer.toString(loc.x) + "," + Integer.toString(loc.y);
          String target;
          switch(moveMode) {
              case 0:
                  curPath = djk.getShortestPathToUnexplored(source);
                  break;

              case 1:
                  if(findPackage) {
                      target = Integer.toString(packageLocation.x) + "," + Integer.toString(packageLocation.y);
                  } else {
                      target = Integer.toString(targetLocation.x) + "," + Integer.toString(targetLocation.y);
                  }
                  curPath = djk.getDijkstraPath(source, target);
                  break;

              case 2:
                  curPath = djk.getShortestPathToUnexplored(source);
                  break;

              case 3:
                  target = Integer.toString(targetLocation.x) + "," + Integer.toString(targetLocation.y);
                  curPath = djk.getDijkstraPath(source, target);
                  break;

              case 4:
                  target = Integer.toString(packageLocation.x) + "," + Integer.toString(packageLocation.y);
                  curPath = djk.getDijkstraPath(source, target);
                  break;

              default:
                  return new Point(0,0);
          }
          for(Edge e : curPath){
            Vertex next = e.target;
            moves.add(next);
          }
        }
        Vertex nextMove = moves.poll();

        return new Point(nextMove.x - loc.x, nextMove.y - loc.y);

      // Point currentLoc = this.loc;
      // Point nextLoc = currentLoc;
      // visited.put(currentLoc, true);
      // int currentx = 0;
      // int currenty = 0;
      // int packageX = 0;
      // int packageY = 0;
      // int targetX = 0;
      // int targetY = 0;
      // String packageLoc = "";
      // String targetLoc = "";

      // targetLoc = "targetX,targetY";
      // System.out.println("targetLoc:"+targetLoc);

      // if ((findPackage == true)&&(findTarget == true)){
      //   packageX = packageLocation.x;
      //   packageY = packageLocation.y;
      //   targetX = targetLocation.x;
      //   targetY = targetLocation.y;
      //   packageLoc = "packageX,packageY";
      //   targetLoc = "targetX,targetY";
      //   // String targetLoc = {targetX,targetY};

      //   List<Edge> path = djk.getDijkstraPath(packageLoc, targetLoc);
      //   if(!path.isEmpty()){
      //     return move(packageLocation);
      //   }
      // }

      // if (findPackage == true || findTarget == true){
      //   // know packageLocation from communication
      //   if ((findPackage == true) && (this.moveMode == 1)){
      //     // go to package location
      //     return move(packageLocation);
      //   }
      //   // know packageLocation from observation, start from observation
      //   else if ((findPackage == true) && (this.moveMode == 2)){
      //     // System.out.println("222222222");
      //     nextLoc = findNextAvoid(this.loc);
      //     visited.put(new Point(nextLoc.x+currentLoc.x,nextLoc.y+currentLoc.y), true);
      //     return nextLoc;
      //   }
      //   // know targetLocation from communication
      //   else if ((findPackage != true) && (this.moveMode == 3)){
      //       // go to target location
      //       return move(targetLocation);
      //   }
      //   // know targetLocation from observation
      //   else{
      //     nextLoc = findNextAvoid(this.loc);
      //     visited.put(new Point(nextLoc.x+currentLoc.x,nextLoc.y+currentLoc.y), true);
      //     return nextLoc;
      //   }
      // }
      // // if didn't know either target location nor package location
      // nextLoc = findNext(currentLoc);
      // visited.put(new Point(nextLoc.x+currentLoc.x,nextLoc.y+currentLoc.y), true);
      // // System.out.println("nextLoc"+nextLoc);
      // return nextLoc;
    }

    // public Point move(Point target)
    // {
    //   int dx = 0;
    //   int dy = 0;
    //   Point start = this.loc;
    //   if (target.x > start.x){
    //       dx = 1;
    //   }
    //   else if (target.x == start.x){
    //       dx = 0;
    //   }
    //   else{
    //       dx = -1;
    //   }
    //   if (target.y > start.y){
    //       dy = 1;
    //   }
    //   else if (target.y == start.y){
    //       dy = 0;
    //   }
    //   else{
    //       dy = -1;
    //   }
    //   return new Point (dx, dy);
    // }

  //   public Point findNext(Point start)
  //   {
  //     Record recordNext;
  //     Point toReturn = start;
  //     Map<Point, Integer> possible_move = new HashMap<Point, Integer>();
  //     for(int i = 0;i<3;i++)
  //     {
  //       int dx = this.loc.x+i-1;
  //       if (dx > 99 || dx < 0){
  //         continue;
  //       }
  //       for(int j = 0;j<3;j++){
  //         int dy = this.loc.y+j-1;
  //         if (i==1 && j==1){
  //             continue;
  //         }
  //         if(dy>99||dy<0){
  //           continue;
  //         }
  //         if(this.water.contains(new Point(dx,dy))){
  //           continue;
  //         }

  //         else{
  //           if (visited.get(new Point(dx, dy))){
  //             if (Math.abs(i)+Math.abs(j)>1){
  //               if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),0);
  //               }
  //               else{
  //                   possible_move.put(new Point(i-1,j-1),2);
  //               }
  //           }
  //             else{
  //               if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),0);
  //               }
  //               else{
  //                   possible_move.put(new Point(i-1,j-1),2);
  //               }
  //             }
  //           }
  //           else{
  //             if (Math.abs(i)+Math.abs(j)>1){
  //               if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),1);
  //               }
  //               else{
  //                   possible_move.put(new Point(i-1,j-1),4);
  //               }
  //           }
  //           else{
  //               if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),1);
  //               }
  //               else{
  //                   possible_move.put(new Point(i-1,j-1),5);
  //               }
  //             }
  //           }

  //           }
  //         }
  //     }
  //     double max_reward = 0;
  //     for (Point p: possible_move.keySet()){
  //       Point next = new Point(start.x+p.x,start.y+p.y);
  //       if (possible_move.get(p)>max_reward){
  //           max_reward = possible_move.get(p);
  //           toReturn = p;
  //       }
  //       if (max_reward == 0){
  //           toReturn = find_unknown(start);
  //       }
  //     }
  //     return toReturn;
  //   }

  //   public Point findNextAvoid(Point start)
  //   {
  //     Record recordNext;
  //     Point toReturn = start;
  //     Map<Point, Integer> possible_move = new HashMap<Point, Integer>();
  //     for(int i = 0;i<3;i++)
  //     {
  //       for(int j = 0;j<3;j++){
  //         int dx = this.loc.x+i-1;
  //         int dy = this.loc.y+j-1;
  //         if (i==1 && j==1){
  //             continue;
  //         }
  //         if(dx>100||dx<0||dy>100||dy<0){
  //           continue;
  //         }
  //         // next step is muddy cell
  //         // Point(i-1,j-1) is next step
  //         if(this.water.contains(new Point(dx,dy))){
  //           continue;
  //         }
  //         else{
  //             if (visited.get(new Point(dx,dy)) == true){
  //               if (Math.abs(i)+Math.abs(j)>1){
  //                 // System.out.println("trueeeeeeeeee");
  //                 if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),-2);
  //                 }
  //                 else{
  //                   possible_move.put(new Point(i-1,j-1),0);
  //                   }
  //               }

  //                 else{
  //                   if (Math.abs(i)+Math.abs(j)>1){
  //                     if (map.get(dx).get(dy).getC() == 1){
  //                       possible_move.put(new Point(i-1,j-1),-1);
  //                     }
  //                     else {
  //                       possible_move.put(new Point(i-1,j-1),0);
  //                     }
  //                   }
  //                   else{
  //                     if (map.get(dx).get(dy).getC() == 1){
  //                       possible_move.put(new Point(i-1,j-1),-1);
  //                     }
  //                     else{
  //                       possible_move.put(new Point(i-1,j-1),3);
  //                     }
  //                   }
  //               }
  //           }
  //           else{
  //               if (map.get(dx).get(dy).getC() == 1){
  //                   possible_move.put(new Point(i-1,j-1),-1);
  //               }
  //               else{
  //                   possible_move.put(new Point(i-1,j-1),5);
  //               }
  //             }
  //           }
  //         }
  //     }
  //     double max_reward = 0;
  //     for (Point p: possible_move.keySet()){
  //       Point next = new Point(start.x+p.x,start.y+p.y);
  //       if (possible_move.get(p)>max_reward){
  //           max_reward = possible_move.get(p);
  //           toReturn = p;
  //       }
  //       if (max_reward == 0){
  //           toReturn = find_unknown(start);
  //       }
  //     }
  //     return toReturn;
  //   }

  // public Point find_unknown(Point loc){
  //     int minimum = 200;
  //     int tx = 0;
  //     int ty = 0;
  //     for (int i=0; i<map.size();++i){
  //         for (int j=0; j<map.get(i).size();++j){
  //             if (map.get(i).get(j)==null){
  //                 if (Math.abs(i-loc.x)+Math.abs(j-loc.y)< minimum){
  //                     minimum = Math.abs(i-loc.x)+Math.abs(j-loc.y);
  //                     tx = i;
  //                     ty = j;
  //                 }
  //             }
  //         }
  //     }
  //     return move(new Point(tx, ty));
  // }

}
