package nars.failchamber;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.DoublePair;
import com.codeforces.commons.pair.LongPair;
import com.codeforces.commons.process.ThreadUtil;
import nars.failchamber.util.Video;
import notreal.Body;
import notreal.Defaults;
import notreal.NamedEntry;
import notreal.index.SpatialIndex;
import notreal.index.CellSpaceBodyList;
import notreal.index.SimpleBodyList;
import notreal.collision.*;
import notreal.form.LinearGeom;
import nars.failchamber.state.Hauto;
import nars.failchamber.state.ParticleSystem;
import nars.failchamber.state.Spatial;
import notreal.form.Shape;
import notreal.listener.CollisionAware;
import notreal.listener.CollisionListener;
import notreal.provider.MomentumTransferFactorProvider;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.codeforces.commons.math.Math.*;

/**
 * Created by me on 2/15/17.
 */
public class Space {

    static {
        Video.init(); //hack for processing's bad parsing of JDK version
    }

    private static final Logger logger = LoggerFactory.getLogger(Space.class);
    @SuppressWarnings("ConstantConditions")
    private static final CollisionInfo NULL_COLLISION_INFO = new CollisionInfo(null, null, null, null, 0.0D, 0.0D);
    /**
     * The only supported value is 2.
     */
    private static final int PARALLEL_THREAD_COUNT = 2;

    public Hauto cells;

    public ParticleSystem particles;

    private final int iterationCountPerStep;
    private final int stepCountPerTimeUnit;
    private final double updateFactor;
    private final double epsilon;
    private final double squaredEpsilon;

    private final SpatialIndex bodyList;

    private final MomentumTransferFactorProvider momentumTransferFactorProvider;

    @Nullable
    private final ExecutorService parallelTaskExecutor;

    private final Map<String, ColliderEntry> colliderEntryByName = new LinkedHashMap<>();
    private final SortedSet<ColliderEntry> colliderEntries = new TreeSet<>(ColliderEntry.comparator);

    @Deprecated private final Map<String, CollisionListenerEntry> collisionListenerEntryByName = new LinkedHashMap<>();
    @Deprecated private final SortedSet<CollisionListenerEntry> collisionListenerEntries = new TreeSet<>(CollisionListenerEntry.comparator);

    private final Collider[] lineColliders;
    double boundsX, boundsY;
    private Body[] bodies = new Body[0];


    public Space() {
        this(Defaults.ITERATION_COUNT_PER_STEP);
    }

    public Space(int iterationCountPerStep) {
        this(iterationCountPerStep, Defaults.STEP_COUNT_PER_TIME_UNIT);
    }

    public Space(int iterationCountPerStep, int stepCountPerTimeUnit) {
        this(iterationCountPerStep, stepCountPerTimeUnit, Defaults.EPSILON);
    }

    public Space(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, new SimpleBodyList() );
    }

    public Space(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, SpatialIndex bodyList) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, bodyList, null);
    }

    public Space(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, SpatialIndex bodyList,
                 @Nullable MomentumTransferFactorProvider momentumTransferFactorProvider) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, bodyList, momentumTransferFactorProvider, false);
    }

    public Space(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, SpatialIndex bodyList,
                 @Nullable MomentumTransferFactorProvider momentumTransferFactorProvider, boolean multithreaded) {
        if (iterationCountPerStep < 1) {
            throw new IllegalArgumentException("Argument 'iterationCountPerStep' is zero or negative.");
        }

        if (stepCountPerTimeUnit < 1) {
            throw new IllegalArgumentException("Argument 'stepCountPerTimeUnit' is zero or negative.");
        }

        if (Double.isNaN(epsilon) || Double.isInfinite(epsilon) || epsilon < 1.0E-100D || epsilon > 1.0D) {
            throw new IllegalArgumentException("Argument 'epsilon' should be between 1.0E-100 and 1.0.");
        }

        if (bodyList == null) {
            throw new IllegalArgumentException("Argument 'bodyList' is null.");
        }

        this.stepCountPerTimeUnit = stepCountPerTimeUnit;
        this.iterationCountPerStep = iterationCountPerStep;
        this.updateFactor = 1.0D / (stepCountPerTimeUnit * iterationCountPerStep);
        this.epsilon = epsilon;
        this.squaredEpsilon = epsilon * epsilon;
        this.bodyList = bodyList;
        this.momentumTransferFactorProvider = momentumTransferFactorProvider;

        this.parallelTaskExecutor = multithreaded ? new ThreadPoolExecutor(
                0, Space.PARALLEL_THREAD_COUNT - 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new Space.MyThreadFactory()
        ) : null;

        registerCollider(new ArcAndArcCollider(epsilon));
        registerCollider(new ArcAndCircleCollider(epsilon));
        registerCollider(new CircleAndCircleCollider(epsilon));
        lineColliders = registerColliders(
                new LineAndArcCollider(epsilon),
                new LineAndCircleCollider(epsilon),
                new LineAndLineCollider(epsilon),
                new LineAndRectangleCollider(epsilon));
        registerCollider(new RectangleAndArcCollider(epsilon));
        registerCollider(new RectangleAndCircleCollider(epsilon));
        registerCollider(new RectangleAndRectangleCollider(epsilon));
    }

    public Space(Hauto cells) {
        this(5, 60, 1E-4,
            new CellSpaceBodyList(8, 8)
            //new SimpleBodyList()
        );

        this.cells = cells;
        this.particles = new ParticleSystem(cells);

        boundsX = cells.w;
        boundsY = cells.h;

        add(LinearGeom.line(0, 0, boundsX, 0).statik());
        add(LinearGeom.line(0, boundsY, boundsX, boundsY).statik());
        add(LinearGeom.line(0, 0, 0, boundsY).statik());
        add(LinearGeom.line(boundsX, 0, boundsX, boundsY).statik());




//        Body body = new Body();
//        body.form(new CircularForm(1.0D));
//        body.mass(1.0D);
//        body.force(5, 5);
//        add(body);
//
//        body.state().registerPositionListener(new PositionListener() {
//
//            @Override
//            public boolean beforeChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
//                System.out.println(newPosition);
//                return true;
//            }
//
//            @Override
//            public void afterChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
//
//            }
//        }, "x");
    }

    private static void awaitParallelTask(@NotNull Future<?> parallelTask) {
        try {
            parallelTask.get(5L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            parallelTask.cancel(true);
            logger.error("Thread has been interrupted while executing parallel task.", e);
            throw new RuntimeException("Thread has been interrupted while executing parallel task.", e);
        } catch (ExecutionException e) {
            parallelTask.cancel(true);
            logger.error("Thread has failed while executing parallel task.", e);
            throw new RuntimeException("Thread has failed while executing parallel task.", e);
        } catch (TimeoutException e) {
            parallelTask.cancel(true);
            logger.error("Thread has timed out while executing parallel task.", e);
            throw new RuntimeException("Thread has timed out while executing parallel task.", e);
        }
    }

    private static void beforeStep(@NotNull Body[] bodies, int leftIndex, int rightIndex) {
        for (int bodyIndex = leftIndex; bodyIndex < rightIndex; ++bodyIndex) {
            Body body = bodies[bodyIndex];
            if (body == null)
                continue;

//            if (!contains(body)) {
//                continue;
//            }

            body.normalizeAngle();
            body.saveBeforeStepState();
        }
    }

    private static void afterStep(@NotNull Body[] bodies, int leftIndex, int rightIndex) {
        for (int bodyIndex = leftIndex; bodyIndex < rightIndex; ++bodyIndex) {
            Body body = bodies[bodyIndex];
//            if (!contains(body)) {
//                continue;
//            }

            body.force(0.0D, 0.0D);
            body.torque(0.0D);
        }
    }

//    private static void logCollision(CollisionInfo collisionInfo) {
//        if (collisionInfo.getDepth() >= collisionInfo.getBodyA().geom().radius() * 0.25D
//                || collisionInfo.getDepth() >= collisionInfo.getBodyB().geom().radius() * 0.25D) {
////            if (logger.isEnabledFor(Level.WARN)) {
////                logger.warn("Resolving collision (big depth) " + collisionInfo + '.');
////            }
//        } else {
////            if (logger.isDebugEnabled()) {
////                logger.debug("Resolving collision " + collisionInfo + '.');
////            }
//        }
//    }

    @NotNull
    private static Vector3D toVector3DZ(double z) {
        return new Vector3D(0.0D, 0.0D, z);
    }

    @NotNull
    private static Vector3D toVector3D(@NotNull Vector2D vector) {
        return new Vector3D(vector.getX(), vector.getY(), 0.0D);
    }

    @NotNull
    private static Vector3D toVector3D(@NotNull Point2D point1, @NotNull Point2D point2) {
        return Space.toVector3D(new Vector2D(point1, point2));
    }




    public void update(View view, double dt) {


        cells.update();

        forEach(b -> {
            if (b instanceof Spatial) {
                Spatial g = (Spatial) b;
                g.update(view, dt);
            }
        });

        next();

    }

    public static float[] ff(DoublePair pos) {
        return new float[] { (float) pos.getFirst(), (float) pos.getSecond()};
    }


    //public void forEach(Consumer<Spatial> s)
    public void draw(View v) {
        long rt = System.nanoTime();


        forEach(o -> {
            if (o instanceof Spatial) {
                ((Spatial) o).draw(v, rt);
            }
        });
    }

    public String what(int x, int y) {
        //TODO convert space to grid coordinates correctly

//        for (Body o : getAll()) {
//            if (o instanceof Spatial) {
//                Spatial s = (Spatial) o;
//                if (s.x == x && s.y == y && s.doorname != null && !s.doorname.isEmpty())
//                    return s.doorname;
//            }
//        }
        return "";

    }

    public Point2D whereSpawns() {
        return new Point2D(Math.random()*boundsX, Math.random()*boundsY);
    }

    public int getIterationCountPerStep() {
        return iterationCountPerStep;
    }

    public int getStepCountPerTimeUnit() {
        return stepCountPerTimeUnit;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void add(@NotNull Body body) {
        if (bodyList.addBody(body)) {
            if (body instanceof Spatial) {
                ((Spatial) body).start(this);
            }
        }
    }

    public void remove(@NotNull Body body) {
        if (bodyList.removeBody(body)) {
            if (body instanceof Spatial) {
                ((Spatial)body).stop(this);
            }
        }
    }

    public void remove(long id) {
        bodyList.removeBody(id);
    }

    public boolean contains(@NotNull Body body) {
        return bodyList.contains(body);
    }

    public boolean contains(long id) {
        return bodyList.contains(id);
    }

    public Body get(long id) {
        return bodyList.getBody(id);
    }

    public Collection<Body> all() {
        return bodyList.getBodies();
    }

    public void forEach(Consumer<Body> each) {
        bodyList.forEach(each);
    }

    @NotNull
    public List<CollisionInfo> getCollisionInfos(@NotNull Point2D a, Point2D b, @Nullable Body excludeFromCollisionTest) {

        double epsilon = 0.01;
        LineAndLineCollider llc = new LineAndLineCollider(epsilon);
        LineAndCircleCollider lcc = new LineAndCircleCollider(epsilon);
        LineAndRectangleCollider lrc = new LineAndRectangleCollider(epsilon);

        Point2D mid = new Point2D(0.5 * (a.getX() + b.getX() ),  0.5 * (a.getY() + b.getY()) );

        List<Body> pot = getPotentialIntersections(mid, a.getDistanceTo(b), excludeFromCollisionTest);
        if (pot.isEmpty())
            return Collections.emptyList();

        List<CollisionInfo> r = new ArrayList<>();

        Body x = LinearGeom.line(a.getX(), a.getY(), b.getX(), b.getY());
        for (Body y : pot) {
            CollisionInfo ci = null;

            Shape shape = y.geom().shape;
            switch (shape) {
                case LINE:
                    ci = llc.collide(x, y);
                    break;
                case CIRCLE:
                    ci = lcc.collide(x, y);
                    break;
                case RECTANGLE:
                    ci = lrc.collide(x, y);
                    break;
                default:
                    throw new UnsupportedOperationException("TODO");
            }
            if (ci!=null)
                r.add(ci);
        }

        return r;
    }

    /** broaphase */
    public List<Body> getPotentialIntersections(Point2D center, double maxDist, @Nullable Body exclude) {

        List<Body> potentialIntersections = new ArrayList<>();

        for (Body otherBody : all()) {
            if (exclude!=null && otherBody.equals(exclude))
                continue;

            if (sqr(otherBody.geom().radius() + maxDist)
                    < otherBody.getSquaredDistanceTo(center)) {
                continue;
            }

            potentialIntersections.add(otherBody);
        }

//        if (!exists) {
//            //throw new IllegalStateException("Can't find " + body + '.');
//            return Collections.emptyList();
//        }

        return (potentialIntersections);
    }

    @NotNull
    public List<CollisionInfo> getCollisionInfos(@NotNull Body body, @Nullable Body excludeFromCollisionTest) {
//        if (!bodyList.contains(body)) {
//            return Collections.emptyList();
//        }

        List<Body> potentialIntersections = bodyList.getPotentialIntersections(body);

        if (excludeFromCollisionTest!=null)
            potentialIntersections.remove(excludeFromCollisionTest);

        int intersectionCount = potentialIntersections.size();

        if (intersectionCount == 0) {
            return Collections.emptyList();
        }

        List<CollisionInfo> collisionInfos = new ArrayList<>();

        boolean bs = body.isStatic();

        for (ColliderEntry colliderEntry : colliderEntries) {

            for (int intersectionIndex = 0; intersectionIndex < intersectionCount; ++intersectionIndex) {
                Body otherBody = potentialIntersections.get(intersectionIndex);
                if (bs && otherBody.isStatic()) {
                    throw new IllegalArgumentException("Static body pairs are unexpected at this time.");
                }

                if (colliderEntry.collider.matches(body, otherBody)) {
                    CollisionInfo collisionInfo = colliderEntry.collider.collide(body, otherBody);
                    if (collisionInfo != null) {
                        collisionInfos.add(collisionInfo);
                    }
                    break;
                }
            }
        }

        return (collisionInfos);
    }

    @SuppressWarnings("ForLoopWithMissingComponent")
    public void next() {
        Collection<Body> bodyCollection = all();
        int bodyCount = bodyCollection.size();
        if (bodies.length != bodyCount)
            bodies = new Body[bodyCount];
        bodyCollection.toArray(bodies);

        if (bodyCount < 1000 || parallelTaskExecutor == null) {
            beforeStep(bodies, 0, bodyCount);

            for (int i = iterationCountPerStep; --i >= 0; ) {
                beforeIteration(bodies, 0, bodyCount);
                processIteration(bodies);
            }

            afterStep(bodies, 0, bodyCount);
        } else {
            int middleIndex = bodyCount / PARALLEL_THREAD_COUNT;

            Future<?> parallelTask = parallelTaskExecutor.submit(() -> beforeStep(bodies, 0, middleIndex));
            beforeStep(bodies, middleIndex, bodyCount);
            awaitParallelTask(parallelTask);

            for (int i = iterationCountPerStep; --i >= 0; ) {
                parallelTask = parallelTaskExecutor.submit(() -> beforeIteration(bodies, 0, middleIndex));
                beforeIteration(bodies, middleIndex, bodyCount);
                awaitParallelTask(parallelTask);

                processIteration(bodies);
            }

            parallelTask = parallelTaskExecutor.submit(() -> afterStep(bodies, 0, middleIndex));
            afterStep(bodies, middleIndex, bodyCount);
            awaitParallelTask(parallelTask);
        }
    }

    private void beforeIteration(@NotNull Body[] bodies, int leftIndex, int rightIndex) {
        for (int bodyIndex = leftIndex; bodyIndex < rightIndex; ++bodyIndex) {
            Body body = bodies[bodyIndex];
            if (body == null)
                continue;

//            if (!contains(body)) {
//                continue;
//            }

            body.saveBeforeIterationState();
            updateState(body);
            body.normalizeAngle();
        }
    }

    private void processIteration(@NotNull Body[] bodies) {
        Map<LongPair, CollisionInfo> collisionInfoByBodyIdsPair = new HashMap<>();

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            Body body = bodies[bodyIndex];
            if (body == null || body.isStatic() /*|| !contains(body)*/) {
                continue;
            }

            bodyList.getPotentialIntersections(body).forEach(otherBody -> collide(body, otherBody, collisionInfoByBodyIdsPair));
        }
    }

    private void collide(@NotNull Body body, @NotNull Body otherBody,
                         @NotNull Map<LongPair, CollisionInfo> collisionInfoByBodyIdsPair) {
        Body bodyA;
        Body bodyB;

        if (body.id > otherBody.id) {
            bodyA = otherBody;
            bodyB = body;
        } else {
            bodyA = body;
            bodyB = otherBody;
        }

        boolean ok = true;
        if (bodyA instanceof CollisionAware) {
            ok &= ((CollisionAware)bodyA).collide(bodyB, this, bodyA);
        }
        if (bodyB instanceof CollisionAware) {
            ok &= ((CollisionAware)bodyB).collide(bodyA, this, bodyB);
        }
        if (!ok) {
            return;
        }

        LongPair bodyIdsPair = new LongPair(bodyA.id, bodyB.id);

        CollisionInfo collisionInfo = collisionInfoByBodyIdsPair.get(bodyIdsPair);
        if (collisionInfo != null) {
            return;
        }

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            if (!collisionListenerEntry.listener.collide(bodyA, this, bodyB)) {
                collisionInfoByBodyIdsPair.put(bodyIdsPair, NULL_COLLISION_INFO);
                return;
            }

//            if (!contains(bodyA) || !contains(bodyB)) {
//                return;
//            }
        }

        for (ColliderEntry colliderEntry : colliderEntries) {
            if (colliderEntry.collider.matches(bodyA, bodyB)) {
                collisionInfo = colliderEntry.collider.collide(bodyA, bodyB);
                break;
            }
        }

        if (collisionInfo == null) {
            collisionInfoByBodyIdsPair.put(bodyIdsPair, NULL_COLLISION_INFO);
        } else {
            collisionInfoByBodyIdsPair.put(bodyIdsPair, collisionInfo);
            resolveCollision(collisionInfo);
        }
    }

    private void resolveCollision(@NotNull CollisionInfo collisionInfo) {
        Body bodyA = collisionInfo.getBodyA();
        Body bodyB = collisionInfo.getBodyB();

        if (!contains(bodyA) || !contains(bodyB)) {
            return;
        }

        if (bodyA.isStatic() && bodyB.isStatic()) {
            throw new IllegalArgumentException("Both " + bodyA + " and " + bodyB + " are static.");
        }

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            if (!collisionListenerEntry.listener.beforeCollision(collisionInfo)) {
                return;
            }
        }

        //logCollision(collisionInfo);

        Vector3D collisionNormalB = toVector3D(collisionInfo.getNormalB());

        Vector3D vectorAC = toVector3D(bodyA.centerOfMass(), collisionInfo.getPoint());
        Vector3D vectorBC = toVector3D(bodyB.centerOfMass(), collisionInfo.getPoint());

        Vector3D angularVelocityPartAC = toVector3DZ(bodyA.angVel()).crossProduct(vectorAC);
        Vector3D angularVelocityPartBC = toVector3DZ(bodyB.angVel()).crossProduct(vectorBC);

        Vector3D velocityAC = toVector3D(bodyA.vel()).add(angularVelocityPartAC);
        Vector3D velocityBC = toVector3D(bodyB.vel()).add(angularVelocityPartBC);

        Vector3D relativeVelocityC = velocityAC.subtract(velocityBC);
        double normalRelativeVelocityLengthC = -relativeVelocityC.dotProduct(collisionNormalB);

        if (normalRelativeVelocityLengthC > -epsilon) {
            resolveImpact(bodyA, bodyB, collisionNormalB, vectorAC, vectorBC, relativeVelocityC);
            resolveSurfaceFriction(bodyA, bodyB, collisionNormalB, vectorAC, vectorBC, relativeVelocityC);
        }

        if (collisionInfo.getDepth() >= epsilon) {
            pushBackBodies(bodyA, bodyB, collisionInfo);
        }

        bodyA.normalizeAngle();
        bodyB.normalizeAngle();

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            collisionListenerEntry.listener.afterCollision(collisionInfo);
        }
    }

    @SuppressWarnings("Duplicates")
    private void resolveImpact(
            @NotNull Body bodyA, @NotNull Body bodyB, @NotNull Vector3D collisionNormalB,
            @NotNull Vector3D vectorAC, @NotNull Vector3D vectorBC, @NotNull Vector3D relativeVelocityC) {
        Double momentumTransferFactor;

        if (momentumTransferFactorProvider == null
                || (momentumTransferFactor = momentumTransferFactorProvider.getFactor(bodyA, bodyB)) == null) {
            momentumTransferFactor = bodyA.getMomentumTransferFactor() * bodyB.getMomentumTransferFactor();
        }

        Vector3D denominatorPartA = vectorAC.crossProduct(collisionNormalB)
                .scalarMultiply(bodyA.getInvertedAngularMass()).crossProduct(vectorAC);
        Vector3D denominatorPartB = vectorBC.crossProduct(collisionNormalB)
                .scalarMultiply(bodyB.getInvertedAngularMass()).crossProduct(vectorBC);

        double denominator = bodyA.getInvertedMass() + bodyB.getInvertedMass()
                + collisionNormalB.dotProduct(denominatorPartA.add(denominatorPartB));

        double impulseChange = -1.0D * (1.0D + momentumTransferFactor) * relativeVelocityC.dotProduct(collisionNormalB)
                / denominator;

        if (abs(impulseChange) < epsilon) {
            return;
        }

        if (!bodyA.isStatic()) {
            Vector3D velocityChangeA = collisionNormalB.scalarMultiply(impulseChange * bodyA.getInvertedMass());
            Vector3D newVelocityA = toVector3D(bodyA.vel()).add(velocityChangeA);
            bodyA.vel(newVelocityA.getX(), newVelocityA.getY());

            Vector3D angularVelocityChangeA = vectorAC.crossProduct(collisionNormalB.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyA.getInvertedAngularMass());
            Vector3D newAngularVelocityA = toVector3DZ(bodyA.angVel()).add(angularVelocityChangeA);
            bodyA.angVel(newAngularVelocityA.getZ());
        }

        if (!bodyB.isStatic()) {
            Vector3D velocityChangeB = collisionNormalB.scalarMultiply(impulseChange * bodyB.getInvertedMass());
            Vector3D newVelocityB = toVector3D(bodyB.vel()).subtract(velocityChangeB);
            bodyB.vel(newVelocityB.getX(), newVelocityB.getY());

            Vector3D angularVelocityChangeB = vectorBC.crossProduct(collisionNormalB.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyB.getInvertedAngularMass());
            Vector3D newAngularVelocityB = toVector3DZ(bodyB.angVel()).subtract(angularVelocityChangeB);
            bodyB.angVel(newAngularVelocityB.getZ());
        }
    }

    @SuppressWarnings("Duplicates")
    private void resolveSurfaceFriction(
            @NotNull Body bodyA, @NotNull Body bodyB, @NotNull Vector3D collisionNormalB,
            @NotNull Vector3D vectorAC, @NotNull Vector3D vectorBC, @NotNull Vector3D relativeVelocityC) {
        Vector3D tangent = relativeVelocityC
                .subtract(collisionNormalB.scalarMultiply(relativeVelocityC.dotProduct(collisionNormalB)));

        if (tangent.getNormSq() < squaredEpsilon) {
            return;
        }

        tangent = tangent.normalize();

        double surfaceFriction = sqrt(bodyA.getSurfaceFrictionFactor() * bodyB.getSurfaceFrictionFactor())
                * SQRT_2 * abs(relativeVelocityC.dotProduct(collisionNormalB)) / relativeVelocityC.getNorm();

        if (surfaceFriction < epsilon) {
            return;
        }

        Vector3D denominatorPartA = vectorAC.crossProduct(tangent)
                .scalarMultiply(bodyA.getInvertedAngularMass()).crossProduct(vectorAC);
        Vector3D denominatorPartB = vectorBC.crossProduct(tangent)
                .scalarMultiply(bodyB.getInvertedAngularMass()).crossProduct(vectorBC);

        double denominator = bodyA.getInvertedMass() + bodyB.getInvertedMass()
                + tangent.dotProduct(denominatorPartA.add(denominatorPartB));

        double impulseChange = -1.0D * surfaceFriction * relativeVelocityC.dotProduct(tangent)
                / denominator;

        if (abs(impulseChange) < epsilon) {
            return;
        }

        if (!bodyA.isStatic()) {
            Vector3D velocityChangeA = tangent.scalarMultiply(impulseChange * bodyA.getInvertedMass());
            Vector3D newVelocityA = toVector3D(bodyA.vel()).add(velocityChangeA);
            bodyA.vel(newVelocityA.getX(), newVelocityA.getY());

            Vector3D angularVelocityChangeA = vectorAC.crossProduct(tangent.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyA.getInvertedAngularMass());
            Vector3D newAngularVelocityA = toVector3DZ(bodyA.angVel()).add(angularVelocityChangeA);
            bodyA.angVel(newAngularVelocityA.getZ());
        }

        if (!bodyB.isStatic()) {
            Vector3D velocityChangeB = tangent.scalarMultiply(impulseChange * bodyB.getInvertedMass());
            Vector3D newVelocityB = toVector3D(bodyB.vel()).subtract(velocityChangeB);
            bodyB.vel(newVelocityB.getX(), newVelocityB.getY());

            Vector3D angularVelocityChangeB = vectorBC.crossProduct(tangent.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyB.getInvertedAngularMass());
            Vector3D newAngularVelocityB = toVector3DZ(bodyB.angVel()).subtract(angularVelocityChangeB);
            bodyB.angVel(newAngularVelocityB.getZ());
        }
    }

    private void updateState(@NotNull Body body) {
        updatePosition(body);
        updateAngle(body);
    }

    private void updatePosition(@NotNull Body body) {
        if (body.vel().getSquaredLength() > 0.0D) {
            body.pos().add(body.vel().copy().multiply(updateFactor));
        }

        if (body.force().getSquaredLength() > 0.0D) {
            body.vel().add(body.force().copy().multiply(body.getInvertedMass()).multiply(updateFactor));
        }

        if (body.getMovementAirFrictionFactor() >= 1.0D) {
            body.vel(body.getMedianVelocity().copy());
        } else if (body.getMovementAirFrictionFactor() > 0.0D) {
            body.applyMovementAirFriction(updateFactor);

            if (body.vel().nearlyEquals(body.getMedianVelocity(), epsilon)) {
                body.vel(body.getMedianVelocity().copy());
            }
        }

        body.vel().subtract(body.getMedianVelocity());
        body.applyFriction(updateFactor);
        body.vel().add(body.getMedianVelocity());
    }

    private void updateAngle(@NotNull Body body) {
        body.angle(body.angle() + body.angVel() * updateFactor);
        body.angVel(
                body.angVel() + body.torque() * body.getInvertedAngularMass() * updateFactor
        );

        if (body.getRotationAirFrictionFactor() >= 1.0D) {
            body.angVel(body.getMedianAngularVelocity());
        } else if (body.getRotationAirFrictionFactor() > 0.0D) {
            body.applyRotationAirFriction(updateFactor);

            if (NumberUtil.nearlyEquals(body.angVel(), body.getMedianAngularVelocity(), epsilon)) {
                body.angVel(body.getMedianAngularVelocity());
            }
        }

        double angularVelocity = body.angVel() - body.getMedianAngularVelocity();

        if (abs(angularVelocity) > 0.0D) {
            double rotationFrictionFactor = body.getRotationFrictionFactor() * updateFactor;

            if (rotationFrictionFactor >= abs(angularVelocity)) {
                body.angVel(body.getMedianAngularVelocity());
            } else if (rotationFrictionFactor > 0.0D) {
                if (angularVelocity > 0.0D) {
                    body.angVel(angularVelocity - rotationFrictionFactor + body.getMedianAngularVelocity());
                } else {
                    body.angVel(angularVelocity + rotationFrictionFactor + body.getMedianAngularVelocity());
                }
            }
        }
    }

    private void pushBackBodies(@NotNull Body bodyA, @NotNull Body bodyB, @NotNull CollisionInfo collisionInfo) {
        if (bodyA.isStatic()) {
            bodyB.pos().subtract(collisionInfo.getNormalB().multiply(collisionInfo.getDepth() + epsilon));
        } else if (bodyB.isStatic()) {
            bodyA.pos().add(collisionInfo.getNormalB().multiply(collisionInfo.getDepth() + epsilon));
        } else {
            Vector2D normalOffset = collisionInfo.getNormalB().multiply(0.5D * (collisionInfo.getDepth() + epsilon));
            bodyA.pos().add(normalOffset);
            bodyB.pos().subtract(normalOffset);
        }
    }

    public void registerCollider(@NotNull Collider collider, @NotNull String name, double priority) {
        NamedEntry.validateName(name);

        if (colliderEntryByName.containsKey(name)) {
            throw new IllegalArgumentException("Collider '" + name + "' is already registered.");
        }

        ColliderEntry colliderEntry = new ColliderEntry(name, priority, collider);
        colliderEntryByName.put(name, colliderEntry);
        colliderEntries.add(colliderEntry);
    }

    public void registerCollider(@NotNull Collider collider, @NotNull String name) {
        registerCollider(collider, name, 0.0D);
    }

    private void registerCollider(@NotNull Collider collider) {
        registerCollider(collider, collider.getClass().getSimpleName());
    }

    private Collider[] registerColliders(@NotNull Collider... c) {
        for (Collider x : c)
            registerCollider(x);
        return c;
    }

    public void unregisterCollider(@NotNull String name) {
        NamedEntry.validateName(name);

        ColliderEntry colliderEntry = colliderEntryByName.remove(name);
        if (colliderEntry == null) {
            throw new IllegalArgumentException("Collider '" + name + "' is not registered.");
        }

        colliderEntries.remove(colliderEntry);
    }

    public boolean hasCollider(@NotNull String name) {
        NamedEntry.validateName(name);
        return colliderEntryByName.containsKey(name);
    }

    public void registerCollisionListener(@NotNull CollisionListener listener, @NotNull String name, double priority) {
        NamedEntry.validateName(name);

        if (collisionListenerEntryByName.containsKey(name)) {
            throw new IllegalArgumentException("Listener '" + name + "' is already registered.");
        }

        CollisionListenerEntry collisionListenerEntry = new CollisionListenerEntry(name, priority, listener);
        collisionListenerEntryByName.put(name, collisionListenerEntry);
        collisionListenerEntries.add(collisionListenerEntry);
    }

    public void registerCollisionListener(@NotNull CollisionListener listener, @NotNull String name) {
        registerCollisionListener(listener, name, 0.0D);
    }

    private @NotNull CollisionListener registerCollisionListener(@NotNull CollisionListener listener) {
        registerCollisionListener(listener, listener.getClass().getSimpleName());
        return listener;
    }

    public void unregisterCollisionListener(@NotNull String name) {
        NamedEntry.validateName(name);

        CollisionListenerEntry collisionListenerEntry = collisionListenerEntryByName.remove(name);
        if (collisionListenerEntry == null) {
            throw new IllegalArgumentException("Listener '" + name + "' is not registered.");
        }

        collisionListenerEntries.remove(collisionListenerEntry);
    }

    public boolean hasCollisionListener(@NotNull String name) {
        NamedEntry.validateName(name);
        return collisionListenerEntryByName.containsKey(name);
    }

    public List<CollisionInfo> collisions(Point2D source, double angle, double maxDistance, @Nullable Body excludeFromCollisionTest) {
        Point2D target = source.copy();
        target.add( maxDistance * Math.cos(angle), maxDistance * Math.sin(angle) );
        return getCollisionInfos(source, target, excludeFromCollisionTest);
    }

    @SuppressWarnings("PublicField")
    private static final class ColliderEntry extends NamedEntry {
        private static final Comparator<Space.ColliderEntry> comparator = (colliderEntryA, colliderEntryB) -> {
            int comparisonResult = Double.compare(colliderEntryB.priority, colliderEntryA.priority);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return colliderEntryA.name.compareTo(colliderEntryB.name);
        };

        public final double priority;
        public final Collider collider;

        private ColliderEntry(String name, double priority, Collider collider) {
            super(name);

            this.priority = priority;
            this.collider = collider;
        }
    }

    @SuppressWarnings("PublicField")
    private static final class CollisionListenerEntry extends NamedEntry {
        private static final Comparator<CollisionListenerEntry> comparator = (listenerEntryA, listenerEntryB) -> {
            int comparisonResult = Double.compare(listenerEntryB.priority, listenerEntryA.priority);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return listenerEntryA.name.compareTo(listenerEntryB.name);
        };

        public final double priority;
        public final CollisionListener listener;

        private CollisionListenerEntry(String name, double priority, CollisionListener listener) {
            super(name);

            this.priority = priority;
            this.listener = listener;
        }
    }

    private static class MyThreadFactory implements ThreadFactory {
        private final AtomicInteger threadIndex = new AtomicInteger();

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            return ThreadUtil.newThread(
                    "World#ParallelExecutionThread-" + threadIndex.incrementAndGet(), runnable,
                    (t, e) -> logger.error("Can't complete parallel task in thread '" + t + "'.", e),
                    true
            );
        }
    }

//    public void operate(String arg, String opnamer) {
//        opname = opnamer;
//        Hauto cells = this.space.cells;
//        goal = arg;
//        for (int i = 0; i < cells.w; i++) {
//            for (int j = 0; j < cells.h; j++) {
//                if (cells.read[i][j].name.equals(arg)) {
//                    if (opname.equals("go-to"))
//                        this.target.set(i, j);
//                }
//            }
//        }
//        //if("pick".equals(opname)) {
//        for (Body gridi : this.space.getAll()) {
//            if (gridi instanceof Spatial && gridi.getName().equals(goal)) { //Key && ((Key)gridi).doorname.equals(goal)) {
//                Spatial gridu = (Spatial) gridi;
//                if (opname.equals("go-to"))
//                    this.target.set((float)gridu.x(), (float)gridu.y());
//            }
//        }
//        //}
//    }
}
