package com.codegame.codeseries.notreal2d;

import com.codegame.codeseries.notreal2d.form.CircularForm;
import org.junit.Assert;
import org.junit.Test;

import static com.codeforces.commons.math.Math.max;
import static com.codeforces.commons.math.Math.pow;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
@SuppressWarnings("OverlyLongMethod")
public class WorldTest {
    @Test
    public void testMovement() throws Exception {
        int iterationCountPerStep = Defaults.ITERATION_COUNT_PER_STEP;
        int stepCountPerTimeUnit = 10;

        World world = new World(iterationCountPerStep, stepCountPerTimeUnit);

        Body body = new Body();
        body.form(new CircularForm(1.0D));
        body.mass(1.0D);
        world.add(body);

        body.pos(0.0D, 0.0D);
        body.vel(1.0D, 0.0D);

        for (int i = 1; i <= 1000; ++i) {
            world.next();
            Assert.assertEquals(
                    "Simple movement. Illegal 'x' after step " + i + '.',
                    0.1D * i, body.x(), world.getEpsilon()
            );
            Assert.assertEquals(
                    "Simple movement. Illegal 'y' after step " + i + '.',
                    0.0D, body.y(), world.getEpsilon()
            );
        }

        body.pos(0.0D, 0.0D);
        body.vel(1.0D, 0.0D);
        body.setMovementFrictionFactor(0.1D);

        double expectedPositionX = 0.0D;
        double expectedVelocityX = 1.0D;

        for (int i = 1; i <= 1000; ++i) {
            for (int j = 0; j < stepCountPerTimeUnit; ++j) {
                expectedPositionX += 0.01D * expectedVelocityX;
                expectedVelocityX = max(expectedVelocityX - 0.001D, 0.0D);
            }

            world.next();
            Assert.assertEquals(
                    "Movement with ground friction. Illegal 'x' after step " + i + '.',
                    expectedPositionX, body.x(), world.getEpsilon()
            );
            Assert.assertEquals(
                    "Movement with ground friction. Illegal 'y' after step " + i + '.',
                    0.0D, body.y(), world.getEpsilon()
            );
        }

        body.pos(0.0D, 0.0D);
        body.vel(1.0D, 0.0D);
        body.setMovementFrictionFactor(0.0D);
        body.setMovementAirFrictionFactor(0.1D);

        expectedPositionX = 0.0D;
        expectedVelocityX = 1.0D;

        for (int i = 1; i <= 1000; ++i) {
            for (int j = 0; j < stepCountPerTimeUnit; ++j) {
                expectedPositionX += 0.01D * expectedVelocityX;
                expectedVelocityX *= pow(1.0D - 0.1D, 0.01D);
            }

            world.next();
            Assert.assertEquals(
                    "Movement with air friction. Illegal 'x' after step " + i + '.',
                    expectedPositionX, body.x(), world.getEpsilon()
            );
            Assert.assertEquals(
                    "Movement with air friction. Illegal 'y' after step " + i + '.',
                    0.0D, body.y(), world.getEpsilon()
            );
        }
    }

    @Test
    public void testMomentumTransferFactor() throws Exception {
        int iterationCountPerStep = Defaults.ITERATION_COUNT_PER_STEP;
        int stepCountPerTimeUnit = 1;

        double originalVelocityModule = 0.1D;

        World world = new World(iterationCountPerStep, stepCountPerTimeUnit);

        Body bodyA = new Body();
        bodyA.form(new CircularForm(1.0D));
        bodyA.mass(1.0D);
        bodyA.setMomentumTransferFactor(0.5D);
        world.add(bodyA);

        bodyA.pos(0.0D, 0.0D);
        bodyA.vel(originalVelocityModule, 0.0D);

        Body bodyB = new Body();
        bodyB.form(new CircularForm(1.0D));
        bodyB.mass(1.0D);
        bodyB.setMomentumTransferFactor(0.5D);
        world.add(bodyB);

        bodyB.pos(3.0D, 0.0D);
        bodyB.vel(-originalVelocityModule, 0.0D);

        for (int i = 1; i <= 10; ++i) {
            world.next();
        }

        Assert.assertEquals(
                "Inelastic collision test. Illegal speed module " + bodyA.vel().getLength() + " of body A.",
                originalVelocityModule * bodyA.getMomentumTransferFactor() * bodyB.getMomentumTransferFactor(),
                bodyA.vel().getLength(), Defaults.EPSILON
        );

        Assert.assertEquals(
                "Inelastic collision test. Illegal speed module " + bodyB.vel().getLength() + " of body B.",
                originalVelocityModule * bodyA.getMomentumTransferFactor() * bodyB.getMomentumTransferFactor(),
                bodyB.vel().getLength(), Defaults.EPSILON
        );
    }
}
