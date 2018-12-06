package spy.sim;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.MapGenerator;
import spy.sim.CellStatus;
import spy.sim.HTTPServer;

public class Simulator {
    
    private static class InternalCellStatus implements CellStatus {
        
        private int c;
        private int pt;
        public ArrayList<Integer> presentSoldiers;
        
        public InternalCellStatus(int c, int pt, ArrayList<Integer> presentSoldiers)
        {
            this.c = c;
            this.pt = pt;
            this.presentSoldiers = presentSoldiers;
        }
        
        public InternalCellStatus(InternalCellStatus other)
        {
            this.c = other.c;
            this.pt = other.pt;
            this.presentSoldiers = new ArrayList<Integer>(other.presentSoldiers);
        }
        
        public int getC()
        {
            return this.c;
        }
        
        public int getPT()
        {
            return pt;
        }
        
        public ArrayList<Integer> getPresentSoldiers()
        {
            return presentSoldiers;
        }
    }
    
    private static final String root = "spy";
    private static final String statics_root = "statics";

    private static boolean gui = false;

    private static double fps = 1;
    private static int n_runs = 1;
    private static int num_players;
    private static int t;
    private static int elapsedT;
    private static ArrayList<String> playerNames;
    private static String mapGenName;
    private static int seed = -1;
    private static Random rand;
    
    private static ArrayList<PlayerWrapper> players;
    private static MapGenerator mapGen;
    
    private static ArrayList<ArrayList<InternalCellStatus>> map;
    
    private static ArrayList<Point> playerDestinations;
    private static ArrayList<Point> playerLocations;
    private static ArrayList<Integer> playerRemainingTravelTimes;

    private static ArrayList<Point> observableOffsets;
    
    private static List<Point> waterCells;
    private static List<Point> muddyCells;
    private static Point targetLocation;
    private static Point packageLocation;
    
    private static List<Point> finalPath;
    private static boolean victory;

    private static int spyID = -1;
    private static boolean noSpy = false;
    
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        parseArgs(args);
        num_players = playerNames.size();
        ArrayList<Integer> shuffle = new ArrayList<Integer>();
        for (int i = 0; i < num_players; i++)
        {
            shuffle.add(i);
        }
        Collections.shuffle(shuffle);

        ArrayList<String> playerNamesClone = new ArrayList<String>(playerNames);
        for (int i = 0; i < num_players; i++)
        {
            playerNames.set(i, playerNamesClone.get(shuffle.get(i)));
        }

        for (int i = 0; i < playerNames.size(); i++)
        {
            System.out.println("Player " + i + ": " + playerNames.get(i));
            Log.record("Player " + i + ": " + playerNames.get(i));
        }
        
        if (seed == -1)
        {
            rand = new Random();
        } else
        {
            rand = new Random(seed);
        }
        
        if (noSpy == false) {
            if (spyID == -1) {
                spyID = rand.nextInt(num_players);
            }
            spyID = shuffle.get(spyID);
            System.out.println("Player " + spyID + " is spy");
            Log.record("Player " + spyID + " is spy");
        } else
        {
            Log.record("NO SPY!");
            spyID = -1;
        }
        
        observableOffsets = new ArrayList<Point>();
        observableOffsets.add(new Point(0, -3));
        observableOffsets.add(new Point(-2, -2));
        observableOffsets.add(new Point(-1, -2));
        observableOffsets.add(new Point(0, -2));
        observableOffsets.add(new Point(1, -2));
        observableOffsets.add(new Point(2, -2));
        observableOffsets.add(new Point(-2, -1));
        observableOffsets.add(new Point(-1, -1));
        observableOffsets.add(new Point(0, -1));
        observableOffsets.add(new Point(1, -1));
        observableOffsets.add(new Point(2, -1));
        observableOffsets.add(new Point(-3, 0));
        observableOffsets.add(new Point(-2, 0));
        observableOffsets.add(new Point(-1, 0));
        observableOffsets.add(new Point(0, 0));
        observableOffsets.add(new Point(1, 0));
        observableOffsets.add(new Point(2, 0));
        observableOffsets.add(new Point(3, 0));
        observableOffsets.add(new Point(-2, 1));
        observableOffsets.add(new Point(-1, 1));
        observableOffsets.add(new Point(0, 1));
        observableOffsets.add(new Point(1, 1));
        observableOffsets.add(new Point(2, 1));
        observableOffsets.add(new Point(-2, 2));
        observableOffsets.add(new Point(-1, 2));
        observableOffsets.add(new Point(0, 2));
        observableOffsets.add(new Point(1, 2));
        observableOffsets.add(new Point(2, 2));
        observableOffsets.add(new Point(0, 3));
        
        mapGen = loadMapGenerator(mapGenName);
        
        waterCells = mapGen.waterCells();
        muddyCells = mapGen.muddyCells();
        packageLocation = mapGen.packageLocation();
        targetLocation = mapGen.targetLocation();
        List<Point> startingLocations = mapGen.startingLocations(waterCells);
        
        map = new ArrayList<ArrayList<InternalCellStatus>>(100);
        for (int i = 0; i < 100; i++)
        {
            map.add(new ArrayList<InternalCellStatus>(100));
            for (int j = 0; j < 100; j++)
            {
                Point location = new Point(i, j);
                boolean isWater = waterCells.contains(location);
                boolean isMuddy = muddyCells.contains(location);
                boolean isPackage = location.equals(packageLocation);
                boolean isTarget = location.equals(targetLocation);
                InternalCellStatus status = new InternalCellStatus((isWater ? 2 : (isMuddy ? 1 : 0)), (isTarget ? 2 : (isPackage ? 1 : 0)), new ArrayList<Integer>());
                map.get(i).add(status);
            }
        }
        
        playerDestinations = new ArrayList<Point>();
        playerRemainingTravelTimes = new ArrayList<Integer>();
        playerLocations = new ArrayList<Point>();
        for (int i = 0; i < num_players; i++)
        {
            playerDestinations.add(null);
            playerRemainingTravelTimes.add(0);
            Point startingLoc = startingLocations.get(i);
            playerLocations.add(startingLoc);
            map.get(startingLoc.x).get(startingLoc.y).presentSoldiers.add(i);
        }
        
        players = new ArrayList<PlayerWrapper>();
        try {
            for (String name : playerNames)
            {
                PlayerWrapper player = loadPlayerWrapper(name);
                players.add(player);
            }
        } catch (Exception ex) {
            System.out.println("Unable to load player. " + ex.getMessage());
            System.exit(0);
        }
        
        finalPath = null;

        HTTPServer server = null;
        if (gui) {
            server = new HTTPServer();
            Log.record("Hosting HTTP Server on " + server.addr());
            if (!Desktop.isDesktopSupported())
                Log.record("Desktop operations not supported");
            else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Log.record("Desktop browse operation not supported");
            else {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            
            gui(server, state(fps));
        }
        
        for (int i = 0; i < num_players; i++)
        {
            players.get(i).init(num_players, i, t, playerLocations.get(i), waterCells, i == spyID);
        }
        
        victory = false;
        elapsedT = 0;
        while (elapsedT < t)
        {
            handleMovement();
            handleObservations();
            handleCommunication();
            int result = handlePathVoting();
            if (result == 1) victory = true;
            if (result != 0) break;
            getMoves();
            elapsedT++;
            if (gui)
            {
                gui(server, state(fps));
            }
        }
        
        if (victory)
        {
            System.out.println("\nEND: SUCCESS! Score: " + elapsedT);
            Log.record("\nEND: SUCCESS! Score: " + elapsedT);
        }
        else if (elapsedT >= t)
        {
            System.out.println("\nEND: RAN OUT OF TIME! Score: " + t);
            Log.record("\nEND: RAN OUT OF TIME! Score: " + t);
        }
        else
        {
            System.out.println("\nEND: INVALID PATH! Score: " + 2 * t);
            Log.record("\nEND: INVALID PATH! Score: " + 2 * t);
        }

        if (gui)
        {
            gui(server, state(fps));
            while (true)
            {

            }
        }
    }
    
    private static void handleMovement()
    {
        for (int i = 0; i < num_players; i++)
        {
            Point dest = playerDestinations.get(i);
            if (dest != null)
            {
                int timeLeft = playerRemainingTravelTimes.get(i);
                timeLeft--;
                if (timeLeft == 0)
                {
                    playerDestinations.set(i, null);
                    Point loc = playerLocations.get(i);
                    System.out.println("Player " + i + " from: " + loc.toString() + " to: " + dest.toString());
                    map.get(loc.x).get(loc.y).presentSoldiers.remove((Integer) i);
                    map.get(dest.x).get(dest.y).presentSoldiers.add(i);
                    playerLocations.set(i, dest);
                }
                playerRemainingTravelTimes.set(i, timeLeft);
            }
        }
    }
    
    private static void handleObservations()
    {
        for (int i = 0; i < num_players; i++)
        {
            HashMap<Point, CellStatus> observations = new HashMap<Point, CellStatus>();
            Point playerLoc = playerLocations.get(i);
            for (Point offset : observableOffsets)
            {
                Point loc = new Point(playerLoc.x + offset.x, playerLoc.y + offset.y);
                if (loc.x > 99 || loc.x < 0 || loc.y > 99 || loc.y < 0)
                {
                    continue;
                }
                
                observations.put(loc, new InternalCellStatus(map.get(loc.x).get(loc.y)));
            }
            players.get(i).observe(playerLoc, observations);
        }
    }
    
    private static void handleCommunication()
    {
        HashMap<Point, List<Record>> communications = new HashMap<Point, List<Record>>();
        for (int i = 0; i < num_players; i++)
        {
            for (int j = 0; j < num_players; j++)
            {
                if (i == j) continue;
                
                Point player1Loc = playerLocations.get(i);
                Point player2Loc = playerLocations.get(j);
                if (!player1Loc.equals(player2Loc)) continue;
                
                List<Record> records = players.get(i).sendRecords(j);
                if (records == null) records = new ArrayList<Record>();
                
                ArrayList<Record> copy = new ArrayList<Record>();
                for (Record r : records)
                {
                    copy.add(new Record(r));
                }
                communications.put(new Point(i, j), copy);
            }
        }
        
        for (Map.Entry<Point, List<Record>> entry : communications.entrySet())
        {
            Point pair = entry.getKey();
            List<Record> records = entry.getValue();
            
            players.get(pair.y).receiveRecords(pair.x, records);
        }
    }
    
    private static int handlePathVoting()
    {
        ArrayList<Integer> playersPresent = map.get(packageLocation.x).get(packageLocation.y).presentSoldiers;
        if (playersPresent.size() < 3)
        {
            return 0;
        }

        HashMap<Integer, List<Point>> proposals = new HashMap<Integer, List<Point>>();
        for (int i : playersPresent)
        {
            List<Point> proposal = players.get(i).proposePath();
            if (proposal != null)
            {
                proposals.put(i, proposal);
            }
        }
        
        HashMap<Integer, Integer> results = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for (int i : playersPresent)
        {
            List<Integer> votes = players.get(i).getVotes(proposals);
            if (votes != null)
            {
                for (int vote : votes)
                {
                    if (proposals.get(vote) != null)
                    {
                        counts.put(vote, (counts.get(vote) == null) ? 1 : counts.get(vote) + 1);
                        results.put(i, vote);
                    }
                }
            }
        }
        
        List<Point> winningPath = null;
        int maxCount = 2;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet())
        {
            if (entry.getValue() > maxCount)
            {
                maxCount = entry.getValue();
                winningPath = proposals.get(entry.getKey());
            }
            else if (entry.getValue() == maxCount)
            {
                winningPath = null;
            }
        }
        
        if (winningPath != null)
        {
            finalPath = winningPath;
            int result = handlePathSelection(winningPath);
            return result;
        }
        else
        {
            for (int i : playersPresent)
            {
                players.get(i).receiveResults(results);
            }
            return 0;
        }
    }
    
    private static void getMoves()
    {
        for (int i = 0; i < num_players; i++)
        {
            if (playerDestinations.get(i) == null)
            {
                Point move = players.get(i).getMove();
                if (move == null || move.equals(new Point(0, 0)))
                {
                    continue;
                }
                if (!playerMoveIsValid(move))
                {
                    continue;
                }
                Point loc = playerLocations.get(i);
                Point dest = new Point(loc.x + move.x, loc.y + move.y);
                if (!playerLocationIsValid(dest))
                {
                    continue;
                }
                
                playerDestinations.set(i, dest);
                playerRemainingTravelTimes.set(i, moveTime(loc, dest));
            }
        }
    }
    
    private static int handlePathSelection(List<Point> path)
    {
        if (!path.get(path.size() - 1).equals(targetLocation))
        {
            return 2;
        }
        path.add(0, packageLocation);
        int totalTime = 0;
        for (int i = 1; i < path.size(); i++)
        {
            Point from = path.get(i - 1);
            Point to = path.get(i);
            if (!pointsAreAdjacent(from, to))
            {
                return 2;
            }
            CellStatus cell = map.get(to.x).get(to.y);
            if (cell.getC() != 0)
            {
                return 2;
            }
            totalTime += moveTime(from, to) * 5;
        }

        elapsedT += totalTime;
        return 1;
    }
    
    private static boolean pointsAreAdjacent(Point p1, Point p2)
    {
        return Math.abs(p1.x - p2.x) <= 1 && Math.abs(p1.y - p2.y) <= 1;
    }
    
    private static Point randomPlayerLocation()
    {
        Point location;
        do {
            location = new Point(rand.nextInt(100), rand.nextInt(100));
        } while (!playerLocationIsValid(location));
        return location;
    }
    
    private static boolean playerLocationIsValid(Point loc)
    {
        if (loc.x <= 99 && loc.x >= 0 && loc.y <= 99 && loc.y >= 0)
        {
            return map.get(loc.x).get(loc.y).getC() != 2;
        }
        else
        {
            return false;
        }
    }
    
    private static boolean playerMoveIsValid(Point move)
    {
        return Math.abs(move.x) <= 1 && Math.abs(move.y) <= 1;
    }
    
    private static int moveTime(Point from, Point to)
    {
        if (from.equals(to))
        {
            return 1;
        }
        
        int base = 2;
        if (from.x != to.x && from.y != to.y)
        {
            base = 3;
        }
        
        boolean isMuddy = map.get(from.x).get(from.y).getC() == 1 || map.get(to.x).get(to.y).getC() == 1;
        
        return isMuddy ? base * 2 : base;
    }
    
    private static double dist(Point a, Point b)
    {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private static PlayerWrapper loadPlayerWrapper(String name) throws Exception {
        Log.record("Loading player " + name);
        Player p = loadPlayer(name);
        if (p == null) {
            Log.record("Cannot load player " + name);
            System.exit(1);
        }

        return new PlayerWrapper(p, name);
    }

    // The state that is sent to the GUI. (JSON)
    private static String state(double fps) {
        String json = "{ \"refresh\":" + (1000.0/fps) + ",\"t\":" + t + ",\"elapsed\":" + elapsedT + ",\"victory\":" + victory + ",\"final_path\":";
        
        if (finalPath == null)
        {
            json += "-1";
        }
        else
        {
            json += "[";
            for (int i = 0; i < finalPath.size(); i++)
            {
                Point p = finalPath.get(i);
                
                json += "{";
                
                json += "\"x\":" + p.x + ",\"y\":" + p.y;
                
                json += "}";
                
                if (i != finalPath.size() - 1)
                {
                    json += ",";
                }
                
            }
            json += "]";
        }
        
        json += ",\"players\":[";
        
        for (int i = 0; i < num_players; i++)
        {
            json += "{";
            
            Point loc = playerLocations.get(i);
            json += "\"name\":\"" + playerNames.get(i) + "\",\"id\":" + i + ",\"x\":" + loc.x + ",\"y\":" + loc.y + ",\"spy\":" + (i == spyID);
            
            json += "}";
            if (i != num_players - 1)
            {
                json += ",";
            }
        }
        
        json += "]";
        
        json += ",\"water\":[";
        
        for (int i = 0; i < waterCells.size(); i++)
        {
            json += "{";
            
            Point loc = waterCells.get(i);
            json += "\"x\":" + loc.x + ",\"y\":" + loc.y;
            
            json += "}";
            if (i != waterCells.size() - 1)
            {
                json += ",";
            }
        }
        
        json += "]";
        
        json += ",\"mud\":[";
        
        for (int i = 0; i < muddyCells.size(); i++)
        {
            json += "{";
            
            Point loc = muddyCells.get(i);
            json += "\"x\":" + loc.x + ",\"y\":" + loc.y;
            
            json += "}";
            if (i != muddyCells.size() - 1)
            {
                json += ",";
            }
        }
        
        json += "]";
        
        json += ",\"package\": { \"x\":" + packageLocation.x + ",\"y\":" + packageLocation.y + "}";
        json += ",\"target\": { \"x\":" + targetLocation.x + ",\"y\":" + targetLocation.y + "}";
        
        json += "}";
        
        return json;
    }

    private static String join(String joins, List<Integer> list) {
        return list.stream().map(Object::toString).collect(Collectors.joining(joins));
    }

    private static void gui(HTTPServer server, String content) {
        if (server == null) return;
        String path = null;
        for (;;) {
            for (;;) {
                try {
                    path = server.request();
                    break;
                } catch (IOException e) {
                    Log.record("HTTP request error " + e.getMessage());
                }
            }
            if (path.equals("data.txt")) {
                try {
                    server.reply(content);
                } catch (IOException e) {
                    Log.record("HTTP dynamic reply error " + e.getMessage());
                }
                return;
            }
            if (path.equals("")) path = "webpage.html";
            else if (!Character.isLetter(path.charAt(0))) {
                Log.record("Potentially malicious HTTP request \"" + path + "\"");
                break;
            }

            File file = new File(statics_root + File.separator + path);
            if (file == null) {
                Log.record("Unknown HTTP request \"" + path + "\"");
            } else {
                try {
                    server.reply(file);
                } catch (IOException e) {
                    Log.record("HTTP static reply error " + e.getMessage());
                }
            }
        }
    }

    private static void parseArgs(String[] args) {
        int i = 0;
        playerNames = new ArrayList<String>();
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].equals("-p") || args[i].equals("--player")) {
                        while (i + 1 < args.length && args[i + 1].charAt(0) != '-') {
                            ++i;
                            playerNames.add(args[i]);
                        }

                        if (playerNames.size() > 30) {
                            throw new IllegalArgumentException("Cannot have more than 30 players.");
                        }
                        if (playerNames.size() < 2) {
                            throw new IllegalArgumentException("Must have at least two players.");
                        }
                    } else if (args[i].equals("-g") || args[i].equals("--gui")) {
                        gui = true;
                    } else if (args[i].equals("-l") || args[i].equals("--logfile")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing logfile name");
                        }
                        Log.setLogFile(args[i]);
                    } else if (args[i].equals("--fps")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing frames per second.");
                        }
                        fps = Double.parseDouble(args[i]);
                    } else if (args[i].equals("-r") || args[i].equals("--runs")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing number of runs.");
                        }
                        n_runs = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                        Log.activate();
                    } else if (args[i].equals("-m")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing field size.");
                        }
                        mapGenName = args[i];
                    } else if (args[i].equals("-t")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit.");
                        }
                        t = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-s") || args[i].equals("--seed")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing random seed.");
                        }
                        seed = Integer.parseInt(args[i]);
                    } else if (args[i].equals("--spy")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing random seed.");
                        }
                        spyID = Integer.parseInt(args[i]);
                    }
                    else if (args[i].equals("--nospy")) {
                        noSpy = true;
                    } else {
                        throw new IllegalArgumentException("Unknown argument '" + args[i] + "'");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument '" + args[i] + "'");
            }
        }

        //Log.record("Players: " + playerName.toString());
        Log.record("GUI " + (gui ? "enabled" : "disabled"));

        if (gui)
            Log.record("FPS: " + fps);
    }

    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<File>();
        Set<File> prev_dirs = new HashSet<File>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<File>();
            for (File dir : prev_dirs)
                for (File file : dir.listFiles())
                    if (!file.canRead()) ;
                    else if (file.isDirectory())
                        next_dirs.add(file);
                    else if (file.getPath().endsWith(extension))
                        files.add(file);
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    public static Player loadPlayer(String name) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
                class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
//            long files = player_files.size();
            Log.record("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "Player.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class raw_class = loader.loadClass(root + "." + name + ".Player");
        return (Player)raw_class.newInstance();
    }
    
    public static MapGenerator loadMapGenerator(String name) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "MapGenerator.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
            class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
            getStandardFileManager(null, null, null);
            //            long files = player_files.size();
            Log.record("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                                  manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "MapGenerator.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class raw_class = loader.loadClass(root + "." + name + ".MapGenerator");
        return (MapGenerator)raw_class.newInstance();
    }

    private static long last_modified(Iterable<File> files) {
        long last_date = 0;
        for (File file : files) {
            long date = file.lastModified();
            if (last_date < date)
                last_date = date;
        }
        return last_date;
    }
    
    public static int getElapsedT()
    {
        return elapsedT;
    }
}
