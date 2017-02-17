package notreal.index;

import notreal.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public interface SpatialIndex {
    boolean addBody(@NotNull Body body);

    boolean removeBody(@NotNull Body body);
    boolean removeBody(long id);

    boolean contains(@NotNull Body body);
    boolean contains(long id);

    Body getBody(long id);
    Collection<Body> getBodies();

    List<Body> getPotentialIntersections(@NotNull Body body);

    void forEach(Consumer<Body> each);
}
