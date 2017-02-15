package com.codegame.codeseries.notreal2d.form;

import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 01.07.2015
 */
public abstract class ThinGeom extends Geom {
    private final boolean endpointCollisionEnabled;

    protected ThinGeom(@NotNull Shape shape, boolean endpointCollisionEnabled) {
        super(shape);

        this.endpointCollisionEnabled = endpointCollisionEnabled;
    }

    public boolean isEndpointCollisionEnabled() {
        return endpointCollisionEnabled;
    }
}
