package spy.g7;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {
    public List<Point> waterCells()
    {
        ArrayList<Point> water = new ArrayList<Point>();
        
        // squares of water with up to 4 random opening points on each square
        for (int x = 0; x < 100; x++)
        {
            if (x%10 == 0) 
            {
                if (x==0) continue;
                int beginPoint = x;
                int endPoint = 100 - x;
                Random rand = new Random();

                // ensure the randomInt generated is within range
                int seed = Math.abs(endPoint-beginPoint);
                
                int min;
                if (endPoint > beginPoint) {
                    min = beginPoint;
                } else {
                    min = endPoint;
                }

                int randomInt = rand.nextInt(seed+1) + min;
                if (randomInt % 5 == 0) {
                    randomInt = rand.nextInt(seed+1) + min;
                }
                for(int y = beginPoint; y <= endPoint; y++) {
                    if (y == randomInt) continue;
                    water.add(new Point(x, y));  
                    water.add(new Point(y, x));
                    water.add(new Point(100-x, y));
                    water.add(new Point(y, 100-x));
                }
            }
        }
        return water;
    }
    public List<Point> muddyCells()
    {
        ArrayList<Point> mud = new ArrayList<Point>();
        
        for (int x = 0; x < 100; x++)
        {
            if (x%5 == 0) 
            {
                if (x==0) continue;
                if (x%10==0) continue;
                int beginPoint = x;
                int endPoint = 100 - x;
                Random rand = new Random();

                // ensure the randomInt generated is within range
                int seed = Math.abs(endPoint-beginPoint);
                
                int min;
                if (endPoint > beginPoint) {
                    min = beginPoint;
                } else {
                    min = endPoint;
                }

                int randomInt = rand.nextInt(seed+1) + min;
                if (randomInt % 5 == 0) {
                    randomInt = rand.nextInt(seed+1) + min;
                }
                for(int y = beginPoint; y <= endPoint; y++) {
                    if (y == randomInt) {
                        continue;
                    }
                    mud.add(new Point(x, y));  
                    mud.add(new Point(y, x));
                    mud.add(new Point(100-x, y));
                    mud.add(new Point(y, 100-x));
                }
            }
        }
        
        return mud;
    }
    public Point packageLocation()
    {
        ArrayList<Point> points = new ArrayList();
        points.add(new Point(0,0));
        points.add(new Point(0,99));
        points.add(new Point(99,0));
        points.add(new Point(99,99));
        Collections.shuffle(points); 
        return points.get(0);
    }
    public Point targetLocation()
    {
        return new Point(50, 50);
    }
    public List<Point> startingLocations(List<Point> waterCells)
    {
        ArrayList<Point> startingLocations = new ArrayList<Point>();
        Random rand = new Random();
        for (int i = 0; i < 30; i++)
        {
            Point p = new Point(rand.nextInt(100), rand.nextInt(100));
            while (waterCells.contains(p))
            {
                p = new Point(rand.nextInt(100), rand.nextInt(100));
            }
            startingLocations.add(p);
        }
        return startingLocations;
    }
}
