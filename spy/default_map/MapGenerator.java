package spy.default_map;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {
    public List<Point> waterCells()
    {
        ArrayList<Point> water = new ArrayList<Point>();
        
        for (int i = 0; i < 100; i++)
        {
            if (i == 49 || i == 50 || i == 51) continue;
            int x = 99 - i;
            water.add(new Point(x, i));
            if (x > 0) water.add(new Point(x - 1, i));
            if (x > 1) water.add(new Point(x - 2, i));
            if (x > 2) water.add(new Point(x - 3, i));
        }
        
        return water;
    }
    public List<Point> muddyCells()
    {
        ArrayList<Point> mud = new ArrayList<Point>();
        
        for (int i = 0; i < 100; i++)
        {
            if (i == 50) continue;
            int x = 99 - i;
            if (i == 49 || i == 50 || i == 51)
            {
                mud.add(new Point(x, i));
                mud.add(new Point(x - 1, i));
                mud.add(new Point(x - 2, i));
                mud.add(new Point(x - 3, i));
            }
            if (x <= 97) mud.add(new Point(x + 2, i));
            if (x <= 98) mud.add(new Point(x + 1, i));
            if (x >= 4) mud.add(new Point(x - 4, i));
            if (x >= 5) mud.add(new Point(x - 5, i));
        }
        
        return mud;
    }
    public Point packageLocation()
    {
        return new Point(0, 0);
    }
    public Point targetLocation()
    {
        return new Point(99, 99);
    }
    public List<Point> startingLocations(List<Point> waterCells)
    {
        ArrayList<Point> startingLocations = new ArrayList<Point>();
        Random rand = new Random(50);
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
