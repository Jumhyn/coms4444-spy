package spy.g6;

import java.util.*;

import spy.sim.Point;

public class MeetPlayer extends MovementTask {

    private int targetPlayerID;
    private int myPlayerID;
    private Point myPlayerLoc;
    private Point targetLoc;
    private Set<Point> waterSet;
    private boolean abort = false;
    private int roundCount = 0;

    private static final int EXPIRATION = 10;
    private static final boolean QUINCY_PROTOCOL = true;

    MeetPlayer(int targetPlayerID, int myPlayerID, Point myPlayerLoc, Point targetLoc, List<Point> waterCells) {
        super();
        this.targetPlayerID = targetPlayerID;
        this.myPlayerID = myPlayerID;
        this.myPlayerLoc = myPlayerLoc;
        this.waterSet = new HashSet<>();
        waterSet.addAll(waterCells);
        this.moves = new LinkedList<>();
        this.targetLoc = targetLoc;
        if(targetPlayerID != myPlayerID){
            updateMoves();
        }
        System.out.println("meetplayer");
    }

    private boolean inMap(Point p) {
        return p.x >= 0 && p.x < 100 && p.y >= 0 && p.y < 100;
    }

    private boolean inMap(int x, int y) {
        return x >= 0 && x < 100 && y >= 0 && y < 100;
    }

    private List<Point> withinObservationRadius(Point center) {
        ArrayList<Point> ret = new ArrayList<>();
        Point[] candidates = {
                new Point(center.x, center.y),
                new Point(center.x - 1, center.y),
                new Point(center.x - 2, center.y),
                new Point(center.x - 1, center.y + 1),
                new Point(center.x - 1, center.y - 1),
                new Point(center.x, center.y + 1),
                new Point(center.x, center.y + 2),
                new Point(center.x, center.y - 1),
                new Point(center.x, center.y - 2),
                new Point(center.x + 1, center.y),
                new Point(center.x + 1, center.y + 1),
                new Point(center.x + 1, center.y - 1),
                new Point(center.x + 2, center.y),
        };
        for (Point c : candidates) {
            if (inMap(c)) {
                ret.add(c);
            }
        }
        return ret;
    }

    private List<Point> intersectionOfPoints(List<Point> A, List<Point> B) {
//        ArrayList<Point> T = new ArrayList<>();
//        Collections.copy(T, A);
        A.retainAll(B);
        return A;
    }

    private List<Point> filterToAvailablePoints(List<Point> cells) {
        List<Point> ret = new ArrayList<>();
        for (Point p : cells) {
            if (!waterSet.contains(p)) {
                ret.add(p);
            }
        }
        return ret;
    }

    private List<Point> getMoves(Point cur, List<Point> availableCells) {
        ArrayList<Point> ret = new ArrayList<>();
        Point[] candidates = {
                new Point(cur.x - 1, cur.y),
                new Point(cur.x - 1, cur.y + 1),
                new Point(cur.x - 1, cur.y - 1),
                new Point(cur.x, cur.y + 1),
                new Point(cur.x, cur.y - 1),
                new Point(cur.x + 1, cur.y),
                new Point(cur.x + 1, cur.y + 1),
                new Point(cur.x + 1, cur.y + 1)
        };
        for (Point c : candidates) {
            if (inMap(c) && availableCells.contains(c)) {
                ret.add(c);
            }
        }
        return ret;
    }

    //from myLoc to targetLoc
    private List<Point> findPath(List<Point> availableCells) {
        //bfs
        Queue<Point> q = new LinkedList<>();
        Map<Point, Point> parent = new HashMap<>();
        List<Point> ret = new ArrayList<>();
        q.add(myPlayerLoc);
        parent.put(myPlayerLoc, myPlayerLoc);
        while (!q.isEmpty()) {
            Point cur = q.poll();
            if (cur.equals(targetLoc)) {
                break;
            }
            List<Point> availableMoves = getMoves(cur, availableCells);
            if (availableMoves == null || availableMoves.isEmpty()) return null;
            for (Point p : availableMoves) {
                q.add(p);
                parent.put(p, cur);
            }
        }
        Point itr = new Point(targetLoc.x, targetLoc.y);
        Stack<Point> stk = new Stack<>();
        while (!itr.equals(myPlayerLoc)) {
            stk.push(itr);
            itr = parent.get(itr);
        }
        stk.push(myPlayerLoc);
        while (!stk.isEmpty()) {
            ret.add(stk.pop());
        }
        return ret;
    }

    private List<Point> deltaFromPath(List<Point> path) {
        ArrayList<Point> ret = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            Point cur = path.get(i);
            Point prv = path.get(i - 1);
            ret.add(new Point(cur.x - prv.x, cur.y - prv.y));
        }
        return ret;
    }

    @Override
    public boolean isCompleted() {
        // TODO
        if (moves.isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public Point nextMove() {
        if (moves.isEmpty()) {
           return null;
        } else return super.nextMove();
    }

    private boolean shouldMeet() {
        return targetLoc != null;
    }

    private boolean shouldWait() {
        if (!QUINCY_PROTOCOL) return false;
        if (roundCount > EXPIRATION) return false;
        if (myPlayerID < targetPlayerID) {
            moves.clear();
            moves.add(new Point(0, 0));
            System.out.println(myPlayerID + " wait " + targetPlayerID);
            roundCount++;
            return true;
        }
        return false;
    }

    private boolean updateMoves() {
        if(targetLoc.equals(myPlayerLoc)) {
            moves.clear();
            return false;
        }
        if (abort) return false;
        if (!shouldMeet()) return false;
        if (shouldWait()) return true;
        else {
            List<Point> myObs = withinObservationRadius(myPlayerLoc);
            if (myObs == null) return false;
            List<Point> targetObs = withinObservationRadius(targetLoc);
            List<Point> intersectionCells = intersectionOfPoints(myObs, targetObs);
            List<Point> availableCells = filterToAvailablePoints(intersectionCells);
            List<Point> path = findPath(availableCells);
            if (path == null) return false;
            moves.clear();
            moves.addAll(deltaFromPath(path));
        }
        return true;
    }
}
