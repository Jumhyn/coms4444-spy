package spy.g3;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {
    public List<Point> waterCells()
    {
        ArrayList<Point> water = new ArrayList<Point>();
        
        return water;
    }
    public List<Point> muddyCells()
    {
        ArrayList<Point> mud = new ArrayList<Point>();

        Integer z = 1;

        Integer x = 48;
        Integer y = 50;

        mud.add(new Point(x, y));

        Integer dx = 0;
        Integer dy = -1;

        Integer wall_length = 2;

        while ((x > 0) && (x < 99) && (y > 0) && (y < 99))
        {
            for (int i = 1; i <= wall_length; i++)
            {
                x += dx;
                y += dy;

                if (z % 100 != 0)
                {
                    mud.add(new Point(x, y));
                }

                z += 1;
            }
            
            if (dy == -1)
            {
                dx = 1;
                dy = 0;
            }
            else if (dx == 1)
            {
                dx = 0;
                dy = 1;
            }
            else if (dy == 1)
            {
                dx = -1;
                dy = 0;
            }
            else if (dx == -1)
            {
                dx = 0;
                dy = -1;
            }

            wall_length += 1;
        }

        return mud;
    }
    public Point packageLocation()
    {
        return new Point(0, 99);
    }
    public Point targetLocation()
    {
        return new Point(50, 49);
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
