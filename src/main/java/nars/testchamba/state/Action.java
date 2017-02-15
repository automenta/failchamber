package nars.testchamba.state;

import nars.testchamba.View;
import nars.testchamba.agent.GridAgent;

/**
 * Defines an action that may or may not be allowed by the game engine.
 * A corresponding Effect will be returned to the agent's buffer
 */
abstract public class Action {


    int expiresAt = -1; //allows an agent to set a time limit on the action


    public Effect process(View p, GridAgent a) {
        return null;
    }

    //generates a string that can be inserted into a NARS judgment
    abstract public String toParamString();


    public static class Forward extends Action {
        public final int steps;

        public Forward(int steps) {
            this.steps = steps;
        }

        @Override
        public String toParamString() {
            return "n" + steps;
        }


        /**
         * rounds to the nearest cardinal direction and moves. steps can be postive or negative
         */
        @Override
        public Effect process(View p, GridAgent a) {
            int tx = (int) Math.round(a.x());
            int ty = (int) Math.round(a.y());
            double angle = a.angle();

            int heading = (int)Math.round((angle / (Math.PI*2))*4);

            //boolean allowDiagonal = false;
            switch (heading) {
                case 2:
                    tx -= steps;
                    break;
                case 0:
                    tx += steps;
                    break;
                case 1:
                    ty += steps;
                    break;
                case 3:
                    ty -= steps;
                    break;
                default:
//                    if (allowDiagonal) {
//                        switch (heading) {
//                            case Hauto.UPLEFT:
//                                tx -= steps;
//                                ty += steps;
//                                break;
//                            case Hauto.UPRIGHT:
//                                tx += steps;
//                                ty += steps;
//                                break;
//                            case Hauto.DOWNLEFT:
//                                tx -= steps;
//                                ty -= steps;
//                                break;
//                            //case DOWNRIGHT: x+=steps; y+=steps;  break;
//                        }
//                    }
                    throw new UnsupportedOperationException();
            }

            Effect result;
            String reason = p.whyNonTraversible(a, tx, ty, tx, ty);
            if (reason == null) {
                result = new Effect(this, true, p.time, "Moved");
            } else {
                result = new Effect(this, false, p.time, reason);
            }

            Effect e = result;
            if (e.success) {
                a.pos(tx, ty);
            }
            return e;
        }

        //public void forward(int angle, int steps, boolean allowDiagonal) {    }

    }

    public static class Turn extends Action {
        public final float angle;

        public Turn(float degrees) {
            this.angle = (float) Math.toRadians(degrees);
        }

        @Override
        public String toParamString() {
            return "n" + angle;
        }

        @Override
        public Effect process(View p, GridAgent a) {
            a.angle(angle);
            return new Effect(this, true, p.getTime());
        }


    }

    public static class Pickup extends Action {
        public final Object o;

        public Pickup(Object o) {
            this.o = o;
        }

        @Override
        public String toParamString() {
            return o.getClass().getSimpleName();
        }
    }

    public static class Drop extends Action {
        public final Object o;

        public Drop(Object o) {
            this.o = o;
        }

        @Override
        public String toParamString() {
            return o.getClass().getSimpleName();
        }
    }

    public static class Door extends Action {
        public final int x, y;
        public final boolean open;

        public Door(int x, int y, boolean open) {
            this.x = x;
            this.y = y;
            this.open = open;
        }

        @Override
        public String toParamString() {
            return String.valueOf(open) + ", n" + x + ", n" + y;
        }
    }


}
