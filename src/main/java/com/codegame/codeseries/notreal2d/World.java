package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.LongPair;
import com.codeforces.commons.process.ThreadUtil;
import com.codegame.codeseries.notreal2d.bodylist.BodyList;
import com.codegame.codeseries.notreal2d.bodylist.SimpleBodyList;
import com.codegame.codeseries.notreal2d.collision.*;
import com.codegame.codeseries.notreal2d.listener.CollisionListener;
import com.codegame.codeseries.notreal2d.provider.MomentumTransferFactorProvider;
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
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@SuppressWarnings("WeakerAccess")
public class World {
    private static final Logger logger = LoggerFactory.getLogger(World.class);

    @SuppressWarnings("ConstantConditions")
    private static final CollisionInfo NULL_COLLISION_INFO = new CollisionInfo(null, null, null, null, 0.0D, 0.0D);

    /**
     * The only supported value is 2.
     */
    private static final int PARALLEL_THREAD_COUNT = 2;

    private final int iterationCountPerStep;
    private final int stepCountPerTimeUnit;
    private final double updateFactor;

    private final double epsilon;
    private final double squaredEpsilon;

    private final BodyList bodyList;
    private final MomentumTransferFactorProvider momentumTransferFactorProvider;

    @Nullable
    private final ExecutorService parallelTaskExecutor;

    private final Map<String, ColliderEntry> colliderEntryByName = new HashMap<>();
    private final SortedSet<ColliderEntry> colliderEntries = new TreeSet<>(ColliderEntry.comparator);

    private final Map<String, CollisionListenerEntry> collisionListenerEntryByName = new HashMap<>();
    private final SortedSet<CollisionListenerEntry> collisionListenerEntries = new TreeSet<>(CollisionListenerEntry.comparator);

    public World() {
        this(Defaults.ITERATION_COUNT_PER_STEP);
    }

    public World(int iterationCountPerStep) {
        this(iterationCountPerStep, Defaults.STEP_COUNT_PER_TIME_UNIT);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit) {
        this(iterationCountPerStep, stepCountPerTimeUnit, Defaults.EPSILON);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, new SimpleBodyList() );
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, BodyList bodyList) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, bodyList, null);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, BodyList bodyList,
                 @Nullable MomentumTransferFactorProvider momentumTransferFactorProvider) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, bodyList, momentumTransferFactorProvider, false);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, BodyList bodyList,
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
                0, PARALLEL_THREAD_COUNT - 1, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new MyThreadFactory()
        ) : null;

        registerCollider(new ArcAndArcCollider(epsilon));
        registerCollider(new ArcAndCircleCollider(epsilon));
        registerCollider(new CircleAndCircleCollider(epsilon));
        registerCollider(new LineAndArcCollider(epsilon));
        registerCollider(new LineAndCircleCollider(epsilon));
        registerCollider(new LineAndLineCollider(epsilon));
        registerCollider(new LineAndRectangleCollider(epsilon));
        registerCollider(new RectangleAndArcCollider(epsilon));
        registerCollider(new RectangleAndCircleCollider(epsilon));
        registerCollider(new RectangleAndRectangleCollider(epsilon));
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
        if (body.form() == null || body.mass() == 0.0D) {
            throw new IllegalArgumentException("Specify form and mass of 'body' before adding to the world.");
        }

        bodyList.addBody(body);
    }

    public void remove(@NotNull Body body) {
        bodyList.removeBody(body);
    }

    public void remove(long id) {
        bodyList.removeBody(id);
    }

    public void removeBodyQuietly(@Nullable Body body) {
        bodyList.removeBodyQuietly(body);
    }

    public void removeBodyQuietly(long id) {
        bodyList.removeBodyQuietly(id);
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

    public boolean isColliding(@NotNull Body body) {
        return getCollisionInfo(body) != null;
    }

    public Collection<Body> getAll() {
        return bodyList.getBodies();
    }
    public void forEach(Consumer<Body> each) {
        bodyList.forEach(each);
    }

    @Nullable
    public CollisionInfo getCollisionInfo(@NotNull Body body) {
        if (!bodyList.contains(body)) {
            return null;
        }

        List<Body> potentialIntersections = bodyList.getPotentialIntersections(body);
        int intersectionCount = potentialIntersections.size();

        for (int intersectionIndex = 0; intersectionIndex < intersectionCount; ++intersectionIndex) {
            Body otherBody = potentialIntersections.get(intersectionIndex);
            if (body.isStatic() && otherBody.isStatic()) {
                throw new IllegalArgumentException("Static body pairs are unexpected at this time.");
            }

            for (ColliderEntry colliderEntry : colliderEntries) {
                if (colliderEntry.collider.matches(body, otherBody)) {
                    return colliderEntry.collider.collide(body, otherBody);
                }
            }
        }

        return null;
    }

    @NotNull
    public List<CollisionInfo> getCollisionInfos(@NotNull Body body) {
        if (!bodyList.contains(body)) {
            return Collections.emptyList();
        }

        List<Body> potentialIntersections = bodyList.getPotentialIntersections(body);
        int intersectionCount = potentialIntersections.size();

        if (intersectionCount == 0) {
            return Collections.emptyList();
        }

        List<CollisionInfo> collisionInfos = new ArrayList<>();

        for (int intersectionIndex = 0; intersectionIndex < intersectionCount; ++intersectionIndex) {
            Body otherBody = potentialIntersections.get(intersectionIndex);
            if (body.isStatic() && otherBody.isStatic()) {
                throw new IllegalArgumentException("Static body pairs are unexpected at this time.");
            }

            for (ColliderEntry colliderEntry : colliderEntries) {
                if (colliderEntry.collider.matches(body, otherBody)) {
                    CollisionInfo collisionInfo = colliderEntry.collider.collide(body, otherBody);
                    if (collisionInfo != null) {
                        collisionInfos.add(collisionInfo);
                    }
                    break;
                }
            }
        }

        return Collections.unmodifiableList(collisionInfos);
    }

    private Body[] bodies = new Body[0];

    @SuppressWarnings("ForLoopWithMissingComponent")
    public void next() {
        Collection<Body> bodyCollection = getAll();
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
//            if (!contains(body)) {
//                continue;
//            }

            body.normalizeAngle();
            body.saveBeforeStepState();
        }
    }

    private void beforeIteration(@NotNull Body[] bodies, int leftIndex, int rightIndex) {
        for (int bodyIndex = leftIndex; bodyIndex < rightIndex; ++bodyIndex) {
            Body body = bodies[bodyIndex];
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
            if (body.isStatic() /*|| !contains(body)*/) {
                continue;
            }

            for (Body otherBody : bodyList.getPotentialIntersections(body)) {
//                if (!contains(body)) {
//                    break;
//                }

                //if (contains(otherBody)) {
                    collide(body, otherBody, collisionInfoByBodyIdsPair);
                //}
            }
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

        LongPair bodyIdsPair = new LongPair(bodyA.id, bodyB.id);

        CollisionInfo collisionInfo = collisionInfoByBodyIdsPair.get(bodyIdsPair);
        if (collisionInfo != null) {
            return;
        }

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            if (!collisionListenerEntry.listener.beforeStartingCollision(bodyA, bodyB)) {
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
            if (!collisionListenerEntry.listener.beforeResolvingCollision(collisionInfo)) {
                return;
            }


        }

        logCollision(collisionInfo);

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
            collisionListenerEntry.listener.afterResolvingCollision(collisionInfo);
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

    private void registerCollisionListener(@NotNull CollisionListener listener) {
        registerCollisionListener(listener, listener.getClass().getSimpleName());
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

    private static void logCollision(CollisionInfo collisionInfo) {
        if (collisionInfo.getDepth() >= collisionInfo.getBodyA().form().radius() * 0.25D
                || collisionInfo.getDepth() >= collisionInfo.getBodyB().form().radius() * 0.25D) {
//            if (logger.isEnabledFor(Level.WARN)) {
//                logger.warn("Resolving collision (big depth) " + collisionInfo + '.');
//            }
        } else {
//            if (logger.isDebugEnabled()) {
//                logger.debug("Resolving collision " + collisionInfo + '.');
//            }
        }
    }

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
        return toVector3D(new Vector2D(point1, point2));
    }

    @SuppressWarnings("PublicField")
    private static final class ColliderEntry extends NamedEntry {
        private static final Comparator<ColliderEntry> comparator = (colliderEntryA, colliderEntryB) -> {
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
}
