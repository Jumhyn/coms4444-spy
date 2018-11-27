package spy.g8;

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

import java.lang.Math;
import java.util.Comparator;

public class Player implements spy.sim.Player {
    
    private ArrayList<ArrayList<Record>> records;
    private int id;
    public Point loc;
    private List<Point> waterCells;
    private List<Point> observed;
    private List<Point> notobserved;
    private Point destination;
    private Point move;

    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.loc = startingPos;
        this.destination = startingPos;
        this.records = new ArrayList<ArrayList<Record>>();
        this.waterCells = waterCells;
        this.observed = new ArrayList<Point>();
        this.notobserved = new ArrayList<Point>();
        this.move = new Point(0,0);


        //System.out.println("watersize:"+waterCells.size());
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
        System.out.println("INIT"+records.get(loc.x).get(loc.y));
        //System.out.println("CHECK" + notobserved.size());
    }

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
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
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
        
    }
    
    public List<Point> proposePath()
    {
        return null;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            //return entry.getKey();
        }
        return null;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        
    }
    
    private List<Point> getSurrounding(Point location) {
        //return list of points of surrounding
        List<Point> neighbors = new ArrayList<Point>();
        for (int x=-3;x<=3;x++) {
            if (x == -3 || x == 3) {
                Point temp = new Point(location.x+x, location.y+0);
                if (temp.x >= 0 && temp.x <= 100 && temp.y >= 0 && temp.y <= 100) {
                    neighbors.add(temp);
                }
            } else if (x == -2 || x == 2 || x == -1 || x == 1) {
                for(int y=-2;y<=2;y++) {
                    Point temp = new Point(location.x+x, location.y+y);
                    if (temp.x >= 0 && temp.x <= 100 && temp.y >= 0 && temp.y <= 100) {
                        neighbors.add(temp);
                    }
                }
            } else { //x == 0
                for(int y=-3;y<=3;y++) {
                    Point temp = new Point(location.x+x, location.y+y);
                    if (temp.x >= 0 && temp.x <= 100 && temp.y >= 0 && temp.y <= 100) {
                        neighbors.add(temp);
                    }
                }
            }
        }
        return neighbors;
    }

    public Point getMove()
    {
        //System.out.println("GETMOVE"+records.get(loc.x).get(loc.y));

        // DO observation stuff here
        //System.out.println("CURR LOC:"+loc.x + " " + loc.y);
        List<Point> neighbors = getSurrounding(loc);
        for(Point n:neighbors) {
            // check if any neighbors contain mud, pacakge, target, or any players
            // if other player detected, move towards that player

            if(notobserved.contains(n)) {
                int i = notobserved.indexOf(n);
                notobserved.remove(i);
                observed.add(n);
            }
        }


        //System.out.println("curr n_obs size: "+notobserved.size());
        if(notobserved.size() > 0) {
            Collections.sort(notobserved, pointComparator);
            destination = notobserved.get(0);
            //System.out.println("DEST:"+destination.x + " " + destination.y);
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

        //System.out.println("NEW LOC:"+loc.x + " " + loc.y);
        return move;

    }
}
