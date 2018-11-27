package spy.g1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import spy.sim.Point;

public class MapGenerator implements spy.sim.MapGenerator {

	public static final String PATH = "spy/g1/maze.txt";

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
						case 'm': muddyCells.add(new Point(i, j)); break;
						case 'w': waterCells.add(new Point(i, j)); break;
						case 'p': packageCell = new Point(i, j); break;
						case 't': targetCell = new Point(i, j); break;
						default : throw new IOException("Invalid map token");
					}
				}
				i++;
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
				try {
					BufferedReader br = new BufferedReader(new FileReader("spy/g1/starts.txt"));
					String line = br.readLine();
					while (line != null) {
						String[] coords = line.split(",");
						int i = Integer.parseInt(coords[0]);
						int j = Integer.parseInt(coords[1]);
						startingLocations.add(new Point(i,j));
						line = br.readLine();
					}
					br.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
        return startingLocations;
    }
}
