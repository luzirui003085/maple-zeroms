
package server.maps;

import client.MapleCharacter;
import handling.world.World;
import java.awt.Point;
import server.Randomizer;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;

/**
 *
 * @author zjj
 */
public class AramiaFireWorks {

    public final static int KEG_ID = 4_031_875, 

    /**
     *
     */
    SUN_ID = 4_001_246, 

    /**
     *
     */
    DEC_ID = 4_001_473;
    public final static int MAX_KEGS = 10_000, 

    /**
     *
     */
    MAX_SUN = 14_000, 

    /**
     *
     */
    MAX_DEC = 18_000;
    private short kegs = 0;
    private short sunshines = MAX_SUN / 6; //start at 1/6 then go from that
    private short decorations = MAX_DEC / 6;
    private static final AramiaFireWorks instance = new AramiaFireWorks();
    private static final int[] arrayMob = {9_400_708};
    private static final int[] arrayX = {-115};
    private static final int[] arrayY = {154};
    private static final int[] array_X = {720, 180, 630, 270, 360, 540, 450, 142,
        142, 218, 772, 810, 848, 232, 308, 142};
    private static final int[] array_Y = {1_234, 1_234, 1_174, 1_234, 1_174, 1_174, 1_174, 1_260, 1_234, 1_234, 1_234, 1_234, 1_234, 1_114, 1_114, 1_140};
    private static final int flake_Y = 149;

    /**
     *
     * @return
     */
    public static final AramiaFireWorks getInstance() {
        return instance;
    }

    /**
     *
     * @param c
     * @param kegs
     */
    public final void giveKegs(final MapleCharacter c, final int kegs) {
        this.kegs += kegs;
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            broadcastEvent(c);
        }
    }

    private final void broadcastServer(final MapleCharacter c, final int itemid) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, itemid, "<頻道 " + c.getClient().getChannel() + "> " + "弓箭手村邱比特公園即將開始發射煙火!"/*c.getMap().getMapName() + " : The amount of {" + MapleItemInformationProvider.getInstance().getName(itemid) + "} has reached the limit!"*/).getBytes());
    }

    /**
     *
     * @return
     */
    public final short getKegsPercentage() {
        return (short) ((kegs / MAX_KEGS) * 10_000);
    }

    private final void broadcastEvent(final MapleCharacter c) {
//        broadcastServer(c, KEG_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startEvent(c.getClient().getChannelServer().getMapFactory().getMap(209_080_000));
            }
        }, 10_000);
    }

    private final void startEvent(final MapleMap map) {
        map.startMapEffect("雪人大大出現啦", 5_120_000);

        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                spawnMonster(map);
            }
        }, 5_000);
    }

    private final void spawnMonster(final MapleMap map) {
        Point pos;

        for (int i = 0; i < arrayMob.length; i++) {
            pos = new Point(arrayX[i], arrayY[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob[i]), pos);
        }
    }

    /**
     *
     * @param c
     * @param kegs
     */
    public final void giveSuns(final MapleCharacter c, final int kegs) {
        this.sunshines += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555_000_000);
        final MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_SUN / 6); gogo > 0; gogo -= (MAX_SUN / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.sunshines >= (MAX_SUN / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.sunshines >= (MAX_SUN / 6)) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.sunshines >= MAX_SUN) {
            this.sunshines = 0;
            broadcastSun(c);
        }
    }

    /**
     *
     * @return
     */
    public final short getSunsPercentage() {
        return (short) ((sunshines / MAX_SUN) * 10_000);
    }

    private final void broadcastSun(final MapleCharacter c) {
        broadcastServer(c, SUN_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startSun(c.getClient().getChannelServer().getMapFactory().getMap(970_010_000));
            }
        }, 10_000);
    }

    private final void startSun(final MapleMap map) {
        map.startMapEffect("The tree is bursting with sunshine!", 5_121_010);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnItem(map);
                }
            }, 5_000 + (i * 10_000));
        }
    }

    private final void spawnItem(final MapleMap map) {
        Point pos;

        for (int i = 0; i < Randomizer.nextInt(5) + 10; i++) {
            pos = new Point(array_X[i], array_Y[i]);
            map.spawnAutoDrop(Randomizer.nextInt(3) == 1 ? 3_010_025 : 4_001_246, pos);
        }
    }

    /**
     *
     * @param c
     * @param kegs
     */
    public final void giveDecs(final MapleCharacter c, final int kegs) {
        this.decorations += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555_000_000);
        final MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_DEC / 6); gogo > 0; gogo -= (MAX_DEC / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.decorations >= (MAX_DEC / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.decorations >= MAX_DEC / 6) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.decorations >= MAX_DEC) {
            this.decorations = 0;
            broadcastDec(c);
        }
    }

    /**
     *
     * @return
     */
    public final short getDecsPercentage() {
        return (short) ((decorations / MAX_DEC) * 10_000);
    }

    private final void broadcastDec(final MapleCharacter c) {
        broadcastServer(c, DEC_ID);
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startDec(c.getClient().getChannelServer().getMapFactory().getMap(555_000_000));
            }
        }, 10_000); //no msg
    }

    private final void startDec(final MapleMap map) {
        map.startMapEffect("The tree is bursting with snow!", 5_120_000);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnDec(map);
                }
            }, 5_000 + (i * 10_000));
        }
    }

    private final void spawnDec(final MapleMap map) {
        Point pos;

        for (int i = 0; i < Randomizer.nextInt(10) + 40; i++) {
            pos = new Point(Randomizer.nextInt(800) - 400, flake_Y);
            map.spawnAutoDrop(Randomizer.nextInt(15) == 1 ? 2_060_006 : 2_060_006, pos);
        }
    }
}
