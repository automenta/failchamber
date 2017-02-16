package com.codegame.codeseries.notreal2d.bodylist;

import com.codegame.codeseries.notreal2d.Body;
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
public class SimpleBodyList implements BodyList {

    //TODO just use Map<Long,Body> and this will give faster lookups

    private final Set<Body> bodies = Collections.newSetFromMap(new ConcurrentHashMap());

    @Override
    public void addBody(@NotNull Body body) {

        if (bodies.contains(body)) {
            throw new IllegalStateException(body + " is already added.");
        }

        bodies.add(body);
    }

    @Override
    public void removeBody(@NotNull Body body) {

        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().equals(body)) {
                bodyIterator.remove();
                return;
            }
        }

        throw new IllegalStateException("Can't find " + body + '.');
    }

    @Override
    public void removeBody(long id) {
        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().id == id) {
                bodyIterator.remove();
                return;
            }
        }

        throw new IllegalStateException("Can't find Body {id=" + id + "}.");
    }

    @Override
    public void removeBodyQuietly(@Nullable Body body) {
        if (body == null) {
            return;
        }

        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().equals(body)) {
                bodyIterator.remove();
                return;
            }
        }
    }

    @Override
    public void removeBodyQuietly(long id) {
        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().id == id) {
                bodyIterator.remove();
                return;
            }
        }
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

        List<Body> potentialIntersections = new ArrayList<>();
        boolean exists = false;

        for (Body otherBody : bodies) {
            if (otherBody.equals(body)) {
                exists = true;
                continue;
            }

            if (body.isStatic() && otherBody.isStatic()) {
                continue;
            }

            if (sqr(otherBody.form().radius() + body.form().radius())
                    < otherBody.getSquaredDistanceTo(body)) {
                continue;
            }

            potentialIntersections.add(otherBody);
        }

        if (!exists) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

        return Collections.unmodifiableList(potentialIntersections);
    }

    @Override
    public void forEach(Consumer<Body> each) {
        bodies.forEach(each);
    }
}
