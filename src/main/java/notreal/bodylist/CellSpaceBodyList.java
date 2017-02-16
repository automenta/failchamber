package notreal.bodylist;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.IntPair;
import notreal.Body;
import notreal.listener.PositionListenerAdapter;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.codeforces.commons.math.Math.floor;
import static com.codeforces.commons.math.Math.sqr;

/**
 * I BROKE IT -seh
 *
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@NotThreadSafe
public class CellSpaceBodyList implements BodyList {
    private static final int MIN_FAST_X = -1000;
    private static final int MAX_FAST_X = 1000;
    private static final int MIN_FAST_Y = -1000;
    private static final int MAX_FAST_Y = 1000;

    private static final int MAX_FAST_BODY_ID = 9999;

    private final Map<Long, Body> bodyById = new ConcurrentHashMap<>();

    private final Body[] fastBodies = new Body[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellXByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellYByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellLeftTopByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellRightBottomByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];

    private final Body[][][] bodiesByCellXY = new Body[MAX_FAST_X - MIN_FAST_X + 1][MAX_FAST_Y - MIN_FAST_Y + 1][];

    //TODO use long instead of IntPair
    private final Map<IntPair, Body[]> bodiesByCell = new HashMap<>();

    private final Set<Body> cellExceedingBodies = new HashSet<>();

    private double cellSize;
    private final double maxCellSize;

    public CellSpaceBodyList(double initialCellSize, double maxCellSize) {
        this.cellSize = initialCellSize;
        this.maxCellSize = maxCellSize;
    }

    @Override
    public void addBody(@NotNull Body body) {

        long id = body.id;

//        if (contains(id)) {
//            throw new IllegalStateException(body + " is already added.");
//        }

        double radius = body.geom().radius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize && diameter <= maxCellSize) {
            cellSize = diameter;
            rebuildIndexes();
        }

        bodyById.put(id, body);
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
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBody(@NotNull Body body) {
        removeBody(body.id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBody(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            throw new IllegalStateException("Can't find Body {id=" + id + "}.");
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBodyQuietly(@Nullable Body body) {
        if (body == null) {
            return;
        }

        long id = body.id;

        if (bodyById.remove(id) == null) {
            return;
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBodyQuietly(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            return;
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
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

        if (!contains(id)) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

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

    private void rebuildIndexes() {
        for (int cellX = MIN_FAST_X; cellX <= MAX_FAST_X; ++cellX) {
            for (int cellY = MIN_FAST_Y; cellY <= MAX_FAST_Y; ++cellY) {
                bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = null;
            }
        }

        bodiesByCell.clear();
        cellExceedingBodies.clear();

        bodyById.values().forEach(this::addBodyToIndexes);
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
                throw new IllegalStateException("Can't remove Body {id=" + body.id + "} from index.");
            }
        } else {
            removeBodyFromIndexes(body, getCellX(body.x()), getCellY(body.y()));
        }
    }

    private void removeBodyFromIndexes(@NotNull Body body, int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            Body[] cellBodies = bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
            cellBodies = removeBodyFromCell(cellBodies, body);
            bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = cellBodies;
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
            throw new IllegalStateException("Can't remove Body {id=" + body.id + "} from index.");
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

    private static final class UnmodifiableCollectionWrapperList<E> implements List<E> {
        private final Collection<E> collection;

        private UnmodifiableCollectionWrapperList(Collection<E> collection) {
            this.collection = collection;
        }

        @Override
        public int size() {
            return collection.size();
        }

        @Override
        public boolean isEmpty() {
            return collection.isEmpty();
        }

        @Override
        public boolean contains(Object object) {
            return collection.contains(object);
        }

        @NotNull
        @Override
        public Iterator<E> iterator() {
            Iterator<E> iterator = collection.iterator();

            return new MyUnmodifiableIterator<>(iterator);
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return collection.toArray();
        }

        @SuppressWarnings("SuspiciousToArrayCall")
        @NotNull
        @Override
        public <T> T[] toArray(@NotNull T[] array) {
            return collection.toArray(array);
        }

        //@Contract("_ -> fail")
        @Override
        public boolean add(E element) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_ -> fail")
        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> collection) {
            return this.collection.containsAll(collection);
        }

        //@Contract("_ -> fail")
        @Override
        public boolean addAll(@NotNull Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_, _ -> fail")
        @Override
        public boolean addAll(int index, @NotNull Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_ -> fail")
        @Override
        public boolean removeAll(@NotNull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_ -> fail")
        @Override
        public boolean retainAll(@NotNull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        //@Contract(" -> fail")
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E get(int index) {
            if (collection instanceof List) {
                return ((List<E>) collection).get(index);
            }

            if (index < 0 || index >= collection.size()) {
                throw new IndexOutOfBoundsException("Illegal index: " + index + ", size: " + collection.size() + '.');
            }

            Iterator<E> iterator = collection.iterator();

            for (int i = 0; i < index; ++i) {
                iterator.next();
            }

            return iterator.next();
        }

        //@Contract("_, _ -> fail")
        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_, _ -> fail")
        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        //@Contract("_ -> fail")
        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            Iterator<E> iterator = collection.iterator();
            int index = 0;

            if (o == null) {
                while (iterator.hasNext()) {
                    if (iterator.next() == null) {
                        return index;
                    }
                    ++index;
                }
            } else {
                while (iterator.hasNext()) {
                    if (o.equals(iterator.next())) {
                        return index;
                    }
                    ++index;
                }
            }

            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if (collection instanceof List) {
                return ((List) collection).lastIndexOf(o);
            }

            Iterator<E> iterator = collection.iterator();
            int index = 0;
            int lastIndex = -1;

            if (o == null) {
                while (iterator.hasNext()) {
                    if (iterator.next() == null) {
                        lastIndex = index;
                    }
                    ++index;
                }
            } else {
                while (iterator.hasNext()) {
                    if (o.equals(iterator.next())) {
                        lastIndex = index;
                    }
                    ++index;
                }
            }

            return lastIndex;
        }

        @NotNull
        @Override
        public ListIterator<E> listIterator() {
            return collection instanceof List
                    ? Collections.unmodifiableList((List<E>) collection).listIterator()
                    : Collections.unmodifiableList(new ArrayList<>(collection)).listIterator();
        }

        @NotNull
        @Override
        public ListIterator<E> listIterator(int index) {
            return collection instanceof List
                    ? Collections.unmodifiableList((List<E>) collection).listIterator(index)
                    : Collections.unmodifiableList(new ArrayList<>(collection)).listIterator(index);
        }

        @NotNull
        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return collection instanceof List
                    ? Collections.unmodifiableList(((List<E>) collection).subList(fromIndex, toIndex))
                    : Collections.unmodifiableList(new ArrayList<>(collection)).subList(fromIndex, toIndex);
        }

        private static class MyUnmodifiableIterator<E> extends UnmodifiableIterator<E> {
            private final Iterator<E> iterator;

            public MyUnmodifiableIterator(Iterator<E> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next();
            }
        }
    }
}
