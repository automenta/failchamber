// sdurant12
// 11/14/2012

package nars.testchamba.particle;

public class Block {

    public int light;
    public int type;

    public static final int AIR = 0x0000;

    public static final int GRYSTONE = 0x0001;

    public static final int[][] GRYSTONEINDENT_TEX
            = {{0xff444444, 0xff444444, 0xff444444, 0xff444444, 0xff444444, 0xff444444, 0xff444444, 0xff444444},
            {0xff444444, 0xff303030, 0xff333333, 0xff333333, 0xff333333, 0xff333333, 0xff343434, 0xff444444},
            {0xff444444, 0xff333333, 0xff444444, 0xff474747, 0xff434343, 0xff444444, 0xff494949, 0xff474747},
            {0xff444444, 0xff333333, 0xff404040, 0xff444444, 0xff484848, 0xff484848, 0xff4a4b4b, 0xff434343},
            {0xff444444, 0xff333333, 0xff444444, 0xff484848, 0xff444444, 0xff404040, 0xff484a4a, 0xff414141},
            {0xff444444, 0xff333333, 0xff404040, 0xff444444, 0xff444444, 0xff444444, 0xff4a4a4a, 0xff444444},
            {0xff444444, 0xff373737, 0xff474747, 0xff464842, 0xff4b4a4c, 0xff4c4c4c, 0xff4e4e4e, 0xff404444},
            {0xff434343, 0xff444444, 0xff404040, 0xff444444, 0xff404040, 0xff474747, 0xff444444, 0xff444444},
    };

    public static final int[][] normalIndentMap // 0xleftrightupdown
            = {{0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333},
            {0x33333333, 0x00880088, 0x11110fdd, 0x11110fdd, 0x11110fdd, 0x11110fdd, 0x88000088, 0x33333333},
            {0x33333333, 0x0fdd1111, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0xdd0f1111, 0x33333333},
            {0x33333333, 0x0fdd1111, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0xdd0f1111, 0x33333333},
            {0x33333333, 0x0fdd1111, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0xdd0f1111, 0x33333333},
            {0x33333333, 0x0fdd1111, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0xdd0f1111, 0x33333333},
            {0x33333333, 0x00888800, 0x1111dd0f, 0x1111dd0f, 0x1111dd0f, 0x1111dd0f, 0x88008800, 0x33333333},
            {0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333, 0x33333333},
    };

    public void setBlock(float xAcc, float yAcc, int light, int type) {
        this.light = light;
        this.type = type;
    }

}
