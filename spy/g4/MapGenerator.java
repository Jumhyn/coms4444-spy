package spy.g4;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {
	
	//public static final String PATH = "spy/g4/map.txt";
	public static String PATH;
	
	protected List<Point> waterCells;
	protected List<Point> muddyCells;
	protected Point packageCell;
	protected Point targetCell;

	public MapGenerator() {

		String path_p = "spy/g4/";
		ArrayList<String> paths = new ArrayList<String>(Arrays.asList("map1.txt", "map2.txt", "map3.txt"));
		Random rand = new Random();
		int p = rand.nextInt(3);
		PATH = path_p + paths.get(p);
		//PATH = "spy/g4/smallmap.txt";
		//System.out.println("map path: " + PATH);

		
		waterCells = new ArrayList<Point>();
		muddyCells = new ArrayList<Point>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(PATH));
			String line = br.readLine();
			int i = 0;
			while (line != null) {
				for (int j = 0; j < line.length(); ++j) {
					switch (line.charAt(j)) {
						case 'n': break;
						case 'm': muddyCells.add(new Point(j, i)); break;
						case 'w': waterCells.add(new Point(j, i)); break;
						case 'p': packageCell = new Point(j, i); break;
						case 't': targetCell = new Point(j, i); break;
						default : throw new IOException("Invalid map token");
					}
				}
				++i;
				line = br.readLine();
			}
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public List<Point> waterCells(){
        return waterCells;
    }
    
    public List<Point> muddyCells(){
        return muddyCells;
    }
    
    public Point packageLocation(){
        return packageCell;
    }
    
    public Point targetLocation(){
        return targetCell;
    }
    
    public List<Point> startingLocations(List<Point> waterCells)
    {
        ArrayList<Point> startingLocations = new ArrayList<Point>();
        Random rand = new Random();
        for (int i = 0; i < 30; i++)
        {
            Point p = new Point(15 + rand.nextInt(70), 10 + rand.nextInt(80));
            while (waterCells.contains(p))
            {
                p = new Point(15 + rand.nextInt(70), 10 + rand.nextInt(80));
            }
            startingLocations.add(p);
        }
        return startingLocations;
        /*for (int i = 0; i < 10; i++)
        {
            Point p = new Point(rand.nextInt(10), rand.nextInt(10));
            while (waterCells.contains(p))
            {
                p = new Point(rand.nextInt(10), rand.nextInt(10));
            }
            startingLocations.add(p);
        }
        return startingLocations;*/
    }
}
