package notreal.index;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.IntPair;
import notreal.Body;
import notreal.listener.PositionListenerAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.codeforces.commons.math.Math.floor;
import static com.codeforces.commons.math.Math.sqr;

/**
 *
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@NotThreadSafe
public class CellSpaceBodyList implements SpatialIndex {
    private static final int MIN_FAST_X = -1000;
    private static final int MAX_FAST_X = 1000;
    private static final int MIN_FAST_Y = -1000;
    private static final int MAX_FAST_Y = 1000;

    private static final int MAX_FAST_BODY_ID = 9999;

    private final Map<Long, Body> bodyById = new ConcurrentHashMap<>();
    //TODO use long instead of IntPair
    private final Map<IntPair, Body[]> bodiesByCell = new ConcurrentHashMap<>();

    private final Set<Body> cellExceedingBodies = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Body[] fastBodies = new Body[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellXByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellYByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellLeftTopByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellRightBottomByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];

    private final Body[][][] bodiesByCellXY = new Body[MAX_FAST_X - MIN_FAST_X + 1][MAX_FAST_Y - MIN_FAST_Y + 1][];

    private double cellSize;
    private final double maxCellSize;

    public CellSpaceBodyList(double initialCellSize, double maxCellSize) {
        this.cellSize = initialCellSize;
        this.maxCellSize = maxCellSize;
    }

    @Override
    public boolean addBody(@NotNull Body body) {

        long id = body.id;
        if (bodyById.putIfAbsent(id, body)!=null)
            return false;

//        if (contains(id)) {
//            throw new IllegalStateException(body + " is already added.");
//        }

        double radius = body.geom().radius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize && diameter <= maxCellSize) {
            cellSize = diameter;
            rebuildIndexes();
        }

        addBodyToIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            fastBodies[fastId] = body;

            body.state().registerPositionListener(new PositionListenerAdapter() {
                private final Lock listenerLock = new ReentrantLock();

                @Override
                public void afterChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
                    if (diameter > cellSize) {
                        return;
                    }

                    Point2D cellLeftTop = fastCellLeftTopByBodyId[fastId];
                    Point2D cellRightBottom = fastCellRightBottomByBodyId[fastId];

                    Point2D position = body.pos();

                    if (position.getX() >= cellLeftTop.getX() && position.getY() >= cellLeftTop.getY()
                            && position.getX() < cellRightBottom.getX() && position.getY() < cellRightBottom.getY()) {
                        return;
                    }

                    int oldCellX = getCellX(oldPosition.getX());
                    int oldCellY = getCellY(oldPosition.getY());

                    int newCellX = getCellX(newPosition.getX());
                    int newCellY = getCellY(newPosition.getY());

                    listenerLock.lock();
                    try {
                        removeBodyFromIndexes(body, oldCellX, oldCellY);
                        addBodyToIndexes(body, newCellX, newCellY);
                    } finally {
                        listenerLock.unlock();
                    }
                }
            }, getClass().getSimpleName() + "Listener");
        } else {
            body.state().registerPositionListener(new PositionListenerAdapter() {
                private final Lock listenerLock = new ReentrantLock();

                @Override
                public void afterChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
                    if (diameter > cellSize) {
                        return;
                    }

                    int oldCellX = getCellX(oldPosition.getX());
                    int oldCellY = getCellY(oldPosition.getY());

                    int newCellX = getCellX(newPosition.getX());
                    int newCellY = getCellY(newPosition.getY());

                    if (oldCellX == newCellX && oldCellY == newCellY) {
                        return;
                    }

                    listenerLock.lock();
                    try {
                        removeBodyFromIndexes(body, oldCellX, oldCellY);
                        addBodyToIndexes(body, newCellX, newCellY);
                    } finally {
                        listenerLock.unlock();
                    }
                }
            }, getClass().getSimpleName() + "Listener");
        }

        return true;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean removeBody(@NotNull Body body) {
        return removeBody(body.id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean removeBody(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            //throw new IllegalStateException("Can't find Body {id=" + id + "}.");
            return false;
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }

        return true;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean contains(@NotNull Body body) {
        return contains(body.id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean contains(long id) {
        return id >= 0L && id <= MAX_FAST_BODY_ID ? fastBodies[(int) id] != null : bodyById.containsKey(id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public Body getBody(long id) {
        return id >= 0L && id <= MAX_FAST_BODY_ID ? fastBodies[(int) id] : bodyById.get(id);
    }

    @Override
    public Collection<Body> getBodies() {
        return bodyById.values();
    }

    @Override
    public void forEach(Consumer<Body> each) {
        bodyById.values().forEach(each);
    }


    /**
     * May not find all potential intersections for bodies whose size exceeds cell size.
     */
    @SuppressWarnings("OverlyLongMethod")
    @Override
    public List<Body> getPotentialIntersections(@NotNull Body body) {
        long id = body.id;

//        if (!contains(id)) {
//            throw new IllegalStateException("Can't find " + body + '.');
//        }

        List<Body> potentialIntersections = new ArrayList<>();

        if (!cellExceedingBodies.isEmpty()) {
            for (Body otherBody : cellExceedingBodies) {
                addPotentialIntersection(body, otherBody, potentialIntersections);
            }
        }

        int cellX;
        int cellY;

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            cellX = fastCellXByBodyId[fastId];
            cellY = fastCellYByBodyId[fastId];
        } else {
            cellX = getCellX(body.x());
            cellY = getCellY(body.y());
        }

        if (body.isStatic()) {
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY - 1), potentialIntersections);
            addPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY + 1), potentialIntersections);
        } else {
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY - 1), potentialIntersections);
            addPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY + 1), potentialIntersections);
        }

        return Collections.unmodifiableList(potentialIntersections);
    }

    private static void addPotentialIntersections(
            @NotNull Body body, @Nullable Body[] bodies, @NotNull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (Body body1 : bodies) {
            addPotentialIntersection(body, body1, potentialIntersections);
        }
    }

    private static void addPotentialIntersection(
            @NotNull Body body, @NotNull Body otherBody, @NotNull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (body.isStatic() && otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.geom().radius() + body.geom().radius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void addPotentialIntersectionsStatic(
            @NotNull Body body, @Nullable Body[] bodies, @NotNull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (Body body1 : bodies) {
            addPotentialIntersectionStatic(body, body1, potentialIntersections);
        }
    }

    private static void addPotentialIntersectionStatic(
            @NotNull Body body, @NotNull Body otherBody, @NotNull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.geom().radius() + body.geom().radius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void addPotentialIntersectionsNotStatic(
            @NotNull Body body, @Nullable Body[] bodies, @NotNull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (Body body1 : bodies) {
            addPotentialIntersectionNotStatic(body, body1, potentialIntersections);
        }
    }

    private static void addPotentialIntersectionNotStatic(
            @NotNull Body body, @NotNull Body otherBody, @NotNull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (sqr(otherBody.geom().radius() + body.geom().radius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void fastAddPotentialIntersectionsStatic(
            @NotNull Body body, @Nullable Body[] bodies, @NotNull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (Body body1 : bodies) {
            fastAddPotentialIntersectionStatic(body, body1, potentialIntersections);
        }
    }

    private static void fastAddPotentialIntersectionStatic(
            @NotNull Body body, @NotNull Body otherBody, @NotNull List<Body> potentialIntersections) {
        if (otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.geom().radius() + body.geom().radius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void fastAddPotentialIntersectionsNotStatic(
            @NotNull Body body, @Nullable Body[] bodies, @NotNull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (Body body1 : bodies) {
            fastAddPotentialIntersectionNotStatic(body, body1, potentialIntersections);
        }
    }

    private static void fastAddPotentialIntersectionNotStatic(
            @NotNull Body body, @NotNull Body otherBody, @NotNull List<Body> potentialIntersections) {
        if (sqr(otherBody.geom().radius() + body.geom().radius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    final AtomicBoolean busyRebuild = new AtomicBoolean(false);

    private void rebuildIndexes() {
        if (busyRebuild.compareAndSet(false, true)) {

            for (Body[][] x : bodiesByCellXY)
                for (Body[] y : x)
                    Arrays.fill(y, null);

            bodiesByCell.clear();
            cellExceedingBodies.clear();

            bodyById.values().forEach(this::addBodyToIndexes);
        }
    }

    private void addBodyToIndexes(@NotNull Body body) {
        double radius = body.geom().radius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize) {
            if (!cellExceedingBodies.add(body)) {
                throw new IllegalStateException("Can't add Body {id=" + body.id + "} to index.");
            }
        } else {
            addBodyToIndexes(body, getCellX(body.x()), getCellY(body.y()));
        }
    }

    private void addBodyToIndexes(@NotNull Body body, int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            Body[] cellBodies = bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = cellBodies;
        } else {
            IntPair cell = new IntPair(cellX, cellY);
            Body[] cellBodies = bodiesByCell.get(cell);
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCell.put(cell, cellBodies);
        }

        long id = body.id;

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            fastCellXByBodyId[fastId] = cellX;
            fastCellYByBodyId[fastId] = cellY;
            fastCellLeftTopByBodyId[fastId] = new Point2D(cellX * cellSize, cellY * cellSize);
            fastCellRightBottomByBodyId[fastId] = new Point2D((cellX + 1) * cellSize, (cellY + 1) * cellSize);
        }
    }

    private void removeBodyFromIndexes(@NotNull Body body) {
        double radius = body.geom().radius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize) {
            if (!cellExceedingBodies.remove(body)) {
                //throw new IllegalStateException("Can't remove Body {id=" + body.id + "} from index.");
            }
        } else {
            removeBodyFromIndexes(body, getCellX(body.x()), getCellY(body.y()));
        }
    }

    private void removeBodyFromIndexes(@NotNull Body body, int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            Body[] cellBodies = bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
            if (cellBodies!=null) {
                cellBodies = removeBodyFromCell(cellBodies, body);
                bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = cellBodies;
            }
        } else {
            IntPair cell = new IntPair(cellX, cellY);
            Body[] cellBodies = bodiesByCell.get(cell);
            cellBodies = removeBodyFromCell(cellBodies, body);

            if (cellBodies == null) {
                bodiesByCell.remove(cell);
            } else {
                bodiesByCell.put(cell, cellBodies);
            }
        }
    }

    @NotNull
    private static Body[] addBodyToCell(@Nullable Body[] cellBodies, @NotNull Body body) {
        if (cellBodies == null) {
            return new Body[]{body};
        }

        int bodyIndex = ArrayUtils.indexOf(cellBodies, body);
        if (bodyIndex != ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalStateException("Can't add Body {id=" + body.id + "} to index.");
        }

        int bodyCount = cellBodies.length;
        Body[] newCellBodies = new Body[bodyCount + 1];
        System.arraycopy(cellBodies, 0, newCellBodies, 0, bodyCount);
        newCellBodies[bodyCount] = body;
        return newCellBodies;
    }

    @Nullable
    private static Body[] removeBodyFromCell(@NotNull Body[] cellBodies, @NotNull Body body) {
        int bodyIndex = ArrayUtils.indexOf(cellBodies, body);
        if (bodyIndex == ArrayUtils.INDEX_NOT_FOUND) {
            //throw new IllegalStateException("Can't remove Body {id=" + body.id + "} from index.");
            return null;
        }

        int bodyCount = cellBodies.length;
        if (bodyCount == 1) {
            return null;
        }

        Body[] newCellBodies = new Body[bodyCount - 1];
        System.arraycopy(cellBodies, 0, newCellBodies, 0, bodyIndex);
        System.arraycopy(cellBodies, bodyIndex + 1, newCellBodies, bodyIndex, bodyCount - bodyIndex - 1);
        return newCellBodies;
    }

    @Nullable
    private Body[] getCellBodies(int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            return bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
        } else {
            return bodiesByCell.get(new IntPair(cellX, cellY));
        }
    }

    private int getCellX(double x) {
        return NumberUtil.toInt(floor(x / cellSize));
    }

    private int getCellY(double y) {
        return NumberUtil.toInt(floor(y / cellSize));
    }

}
