package spy.g5;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {
	
	public static final String PATH = "spy/g5/map.txt";
	
	protected List<Point> waterCells;
	protected List<Point> muddyCells;
	protected Point packageCell;
	protected Point targetCell;
	
	public MapGenerator() {
		
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
    public List<Point> startingLocations(List<Point> waterCells){
        ArrayList<Point> startingLocations = new ArrayList<Point>();
        for(int i=0;i<7;i++){
            Point p = new Point(i, 0);
            startingLocations.add(p);
            Point q = new Point(99-i,99);
            startingLocations.add(q);
            Point r = new Point(0,99-i);
            startingLocations.add(r);
            Point s = new Point(99-i,0);
            startingLocations.add(s);
        }
        Point p = new Point(1,1);
        startingLocations.add(p);
        Point q = new Point(98,98);
        startingLocations.add(q);
        return startingLocations;
    }
}