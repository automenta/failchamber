package com.codegame.codeseries.notreal2d.listener;

import com.codeforces.commons.geometry.Point2D;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.08.2015
 */
public class PositionListenerAdapter implements PositionListener {
    @Override
    public boolean beforeChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
        return true;
    }

    @Override
    public void afterChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
        // No operations.
    }
}
