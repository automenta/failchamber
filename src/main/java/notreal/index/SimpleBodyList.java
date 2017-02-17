package notreal.index;

import notreal.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.codeforces.commons.math.Math.sqr;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class SimpleBodyList implements SpatialIndex {

    //TODO just use Map<Long,Body> and this will give faster lookups

    private final Set<Body> bodies = Collections.newSetFromMap(new ConcurrentHashMap());

    @Override
    public boolean addBody(@NotNull Body body) {

        if (bodies.contains(body)) {
            throw new IllegalStateException(body + " is already added.");
        }

        return bodies.add(body);
    }

    @Override
    public boolean removeBody(@NotNull Body body) {
        return bodies.remove(body);
    }

    @Override
    public boolean removeBody(long id) {
        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().id == id) {
                bodyIterator.remove();
                return true;
            }
        }

        //throw new IllegalStateException("Can't find Body {id=" + id + "}.");
        return false;
    }

    @Override
    public boolean contains(@NotNull Body body) {
        return bodies.contains(body);
    }

    @Override
    public boolean contains(long id) {
        for (Body body : bodies) {
            if (body.id == id) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public Body getBody(long id) {
        for (Body body : bodies) {
            if (body.id == id) {
                return body;
            }
        }

        return null;
    }

    @Override
    public Collection<Body> getBodies() {
        return bodies;
    }

    @Override
    public List<Body> getPotentialIntersections(@NotNull Body body) {

        double bRad = body.geom().radius();

        List<Body> potentialIntersections = new ArrayList<>();

        boolean statik = body.isStatic();

        for (Body otherBody : bodies) {
            if (otherBody.equals(body)) {
                continue;
            }

            if (statik && otherBody.isStatic()) {
                continue;
            }

            if (sqr(otherBody.geom().radius() + bRad)
                    < otherBody.getSquaredDistanceTo(body)) {
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

    @Override
    public void forEach(Consumer<Body> each) {
        bodies.forEach(each);
    }
}
