package notreal.provider;

import notreal.Body;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.06.2015
 */
public interface MovementFrictionProvider {
    void applyFriction(Body body, double updateFactor);
}
