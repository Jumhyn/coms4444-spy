package spy.sim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import spy.sim.CellStatus;
import spy.sim.Record;
import spy.sim.Point;
import spy.sim.Player;

public class PlayerWrapper {
    private Timer timer;
    private Player player;
    private String name;
    private int id;

    public PlayerWrapper(Player player, String name) {
        this.player = player;
        this.name = name;
        this.timer = new Timer();
    }

    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        Log.record("Initializing player " + id + ": " + this.name);
        
        this.id = id;
        try {
            if (!timer.isAlive()) timer.start();
            
            timer.call_start(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    player.init(n, id, t, startingPos, waterCells, isSpy);
                    return null;
                }
            });
            
            timer.call_wait(2 * 60 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on init");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        Log.record("Player " + this.id + " observing from " + loc);
        
        try {
            timer.call_start(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    player.observe(loc, statuses);
                    return null;
                }
            });
            
            timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on observe");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    public List<Record> sendRecords(int id)
    {
        Log.record("Player " + this.id + " sending records to " + id);
        
        List<Record> records = null;
        try {
            timer.call_start(new Callable<List<Record>>() {
                @Override
                public List<Record> call() throws Exception {
                    return player.sendRecords(id);
                }
            });
            
            records = timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on sendRecords");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
        return records;
    }
    
    public void receiveRecords(int id, List<Record> records)
    {
        Log.record("Player " + this.id + " receiving from " + id);
        
        try {
            timer.call_start(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    player.receiveRecords(id, records);
                    return null;
                }
            });
            
            timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on receiveRecords");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    public List<Point> proposePath()
    {
        Log.record("Player " + id + " proposing path");
        
        List<Point> path = null;
        try {
            timer.call_start(new Callable<List<Point>>() {
                @Override
                public List<Point> call() throws Exception {
                    return player.proposePath();
                }
            });
            
            path = timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on proposePath");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
        
        if (path != null)
        {
            Log.record("\tPath proposed: " + path);
        }
        else
        {
            Log.record("\tNo path proposed");
        }
        
        return path;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        Log.record("Player " + id + " sending votes");
        
        List<Integer> votes = null;
        
        try {
            timer.call_start(new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return player.getVotes(paths);
                }
            });
            
            votes = timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on getVotes");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
        return votes;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        try {
            timer.call_start(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    player.receiveResults(results);
                    return null;
                }
            });
            
            timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on receiveRecords");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
    }
    
    public Point getMove()
    {
        Log.record("Player " + id + " sending move");
        
        Point p = null;
        try {
            timer.call_start(new Callable<Point>() {
                @Override
                public Point call() throws Exception {
                    return player.getMove();
                }
            });
            
            p = timer.call_wait(1 * 1000);
        }
        catch (TimeoutException ex)
        {
            System.out.println("Player " + this.id + " timed out on getMove");
        }
        catch (Exception ex)
        {
            System.out.println("Player " + this.id + " threw exception: " + ex);
            ex.printStackTrace();
        }
        
        if (p != null)
        {
            Log.record("\tMove: " + p);
        }
        else
        {
            Log.record("\tNo move");
        }
        
        return p;
    }

    public String getName() {
        return name;
    }
}
