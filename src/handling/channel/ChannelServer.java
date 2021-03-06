package handling.channel;

import client.MapleCharacter;
import client.MapleClient;
import database.DatabaseConnection;
import handling.ByteArrayMaplePacket;
import handling.MaplePacket;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.login.LoginServer;
import handling.mina.MapleCodecFactory;
import handling.world.CheaterData;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.EventScriptManager;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.ServerProperties;
import server.events.MapleCoconut;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.events.MapleFitness;
import server.events.MapleOla;
import server.events.MapleOxQuiz;
import server.events.MapleSnowball;
import server.life.PlayerNPC;
import server.maps.MapleMapFactory;
import server.shops.HiredMerchant;
import tools.CollectionUtil;
import tools.ConcurrentEnumMap;
import tools.MaplePacketCreator;

/**
 *
 * @author zjj
 */
public class ChannelServer implements Serializable {

    /**
     *
     */
    public static long serverStartTime;
    private int expRate, mesoRate, dropRate, cashRate, BossdropRate = 1;
    private int doubleExp = 1;
    private int doubleMeso = 1;
    private int doubleDrop = 1;
    private short port = 7_574;
    private static final short DEFAULT_PORT = 7_574;
    private int channel, running_MerchantID = 0, flags = 0;
    private String serverMessage, key, ip, serverName;
    private boolean shutdown = false, finishedShutdown = false, MegaphoneMuteState = false, adminOnly = false;
    private PlayerStorage players;
    private MapleServerHandler serverHandler;
    private IoAcceptor acceptor;
    private final MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private static final Logger log = LoggerFactory.getLogger(ChannelServer.class);
    private static final Map<Integer, ChannelServer> instances = new HashMap<>();
    private final Map<MapleSquadType, MapleSquad> mapleSquads = new ConcurrentEnumMap<>(MapleSquadType.class);
    private final Map<Integer, HiredMerchant> merchants = new HashMap<>();
    private final Map<Integer, PlayerNPC> playerNPCs = new HashMap<>();
    private final ReentrantReadWriteLock merchLock = new ReentrantReadWriteLock(); //merchant
    private final ReentrantReadWriteLock squadLock = new ReentrantReadWriteLock(); //squad
    private int eventmap = -1;
    private final Map<MapleEventType, MapleEvent> events = new EnumMap<>(MapleEventType.class);
    private boolean debugMode = false;
    private int instanceId = 0;

//    private ChannelServer(final String key, final int channel) {
//        this.key = key;
//        this.channel = channel;
//        mapFactory = new MapleMapFactory();
//        mapFactory.setChannel(channel);
//    }
    private ChannelServer(final int channel) {
        this.channel = channel;
        this.mapFactory = new MapleMapFactory(channel);
        /*
         * this.channel = channel; mapFactory = new MapleMapFactory();
         * mapFactory.setChannel(channel);
         */
    }

    /**
     *
     * @return
     */
    public static Set<Integer> getAllInstance() {
        return new HashSet<>(instances.keySet());
    }

    /**
     *
     */
    public final void loadEvents() {
        if (!events.isEmpty()) {
            return;
        }
        events.put(MapleEventType.打椰子比赛, new MapleCoconut(channel, MapleEventType.打椰子比赛.mapids));
        events.put(MapleEventType.打瓶盖比赛, new MapleCoconut(channel, MapleEventType.打瓶盖比赛.mapids));
        events.put(MapleEventType.向高地, new MapleFitness(channel, MapleEventType.向高地.mapids));
        events.put(MapleEventType.上楼上楼, new MapleOla(channel, MapleEventType.上楼上楼.mapids));
        events.put(MapleEventType.快速0X猜题, new MapleOxQuiz(channel, MapleEventType.快速0X猜题.mapids));
        events.put(MapleEventType.雪球赛, new MapleSnowball(channel, MapleEventType.雪球赛.mapids));
    }

    /**
     *
     */
    public final void run_startup_configurations() {
        setChannel(this.channel); //instances.put
        try {
            expRate = Integer.parseInt(ServerProperties.getProperty("ZeroMS.Exp"));
            mesoRate = Integer.parseInt(ServerProperties.getProperty("ZeroMS.Meso"));
            dropRate = Integer.parseInt(ServerProperties.getProperty("ZeroMS.Drop"));
            BossdropRate = Integer.parseInt(ServerProperties.getProperty("ZeroMS.BDrop"));
            cashRate = Integer.parseInt(ServerProperties.getProperty("ZeroMS.Cash"));
            serverMessage = ServerProperties.getProperty("ZeroMS.ServerMessage");
            serverName = ServerProperties.getProperty("ZeroMS.ServerName");
            flags = Integer.parseInt(ServerProperties.getProperty("ZeroMS.WFlags", "0"));
            adminOnly = Boolean.parseBoolean(ServerProperties.getProperty("ZeroMS.Admin", "false"));
            eventSM = new EventScriptManager(this, ServerProperties.getProperty("ZeroMS.Events").split(","));
            port = Short.parseShort(ServerProperties.getProperty("ZeroMS.Port" + this.channel, String.valueOf(DEFAULT_PORT + this.channel)));
            //他不会去 启动 tms.Port  而是启动的 DEFAULT_PORT的yto
            //   port = Short.parseShort(ServerProperties.getProperty("tms.Port" + channel));
            // port = Integer.parseInt(this.props.getProperty("net.sf.cherry.channel.net.port"));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ip = ServerProperties.getProperty("ZeroMS.IP") + ":" + port;//绑定ip

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        players = new PlayerStorage(channel);
        loadEvents();
        try {
            acceptor.setHandler(new MapleServerHandler(channel, false));
            acceptor.bind(new InetSocketAddress(port));
            ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);

            System.out.println("频道 " + this.channel + ": 启动端口 " + port + ": 服务器ip " + ip + "");
            eventSM.init();
        } catch (IOException e) {
            System.out.println("Binding to port " + port + " failed (ch: " + getChannel() + ")" + e);
        }
    }

    /**
     *
     * @param threadToNotify
     */
    public final void shutdown(Object threadToNotify) {
        if (finishedShutdown) {
            return;
        }
        broadcastPacket(MaplePacketCreator.serverNotice(0, "这个频道正在关闭中."));
        // dc all clients by hand so we get sessionClosed...
        shutdown = true;

        System.out.println("Channel " + channel + ", Saving hired merchants...");

        System.out.println("Channel " + channel + ", Saving characters...");

        // getPlayerStorage().disconnectAll();
        System.out.println("Channel " + channel + ", Unbinding...");

        //temporary while we dont have !addchannel
        instances.remove(channel);
        LoginServer.removeChannel(channel);
        setFinishShutdown();
//        if (threadToNotify != null) {
//            synchronized (threadToNotify) {
//                threadToNotify.notify();
//            }
//        }
    }

    /**
     *
     * @return
     */
    public final boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    /**
     *
     * @return
     */
    public final MapleMapFactory getMapFactory() {
        return mapFactory;
    }

//    public static final ChannelServer newInstance(final String key, final int channel) {
//        return new ChannelServer(key, channel);
//    }

    /**
     *
     * @param channel
     * @return
     */
    public static final ChannelServer newInstance(final int channel) {
        return new ChannelServer(channel);
    }

    /**
     *
     * @param channel
     * @return
     */
    public static final ChannelServer getInstance(final int channel) {
        return instances.get(channel);
    }

    /**
     *
     * @param chr
     */
    public final void addPlayer(final MapleCharacter chr) {
        getPlayerStorage().registerPlayer(chr);
        chr.getClient().getSession().write(MaplePacketCreator.serverMessage(serverMessage));
    }

    /**
     *
     * @return
     */
    public final PlayerStorage getPlayerStorage() {
        if (players == null) { //wth
            players = new PlayerStorage(channel); //wthhhh
        }
        return players;
    }

    /**
     *
     * @param chr
     */
    public final void removePlayer(final MapleCharacter chr) {
        getPlayerStorage().deregisterPlayer(chr);

    }

    /**
     *
     * @param idz
     * @param namez
     */
    public final void removePlayer(final int idz, final String namez) {
        getPlayerStorage().deregisterPlayer(idz, namez);

    }

    /**
     *
     * @return
     */
    public final String getServerMessage() {
        return serverMessage;
    }

    /**
     *
     * @param newMessage
     */
    public final void setServerMessage(final String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    /**
     *
     * @param data
     */
    public final void broadcastPacket(final MaplePacket data) {
        getPlayerStorage().broadcastPacket(data);
    }

    /**
     *
     * @param data
     */
    public final void broadcastSmegaPacket(final MaplePacket data) {
        getPlayerStorage().broadcastSmegaPacket(data);
    }

    /**
     *
     * @param data
     */
    public final void broadcastGMPacket(final MaplePacket data) {
        getPlayerStorage().broadcastGMPacket(data);
    }

    /**
     *
     * @return
     */
    public final int getExpRate() {
        return expRate * doubleExp;
    }

    /**
     *
     * @param expRate
     */
    public final void setExpRate(final int expRate) {
        this.expRate = expRate;
    }

    /**
     *
     * @return
     */
    public final int getCashRate() {
        return cashRate;
    }

    /**
     *
     * @param cashRate
     */
    public final void setCashRate(final int cashRate) {
        this.cashRate = cashRate;
    }

    /**
     *
     * @return
     */
    public final int getChannel() {
        return channel;
    }

    /**
     *
     * @param channel
     */
    public final void setChannel(final int channel) {
        instances.put(channel, this);
        LoginServer.addChannel(channel);
    }

    /**
     *
     * @return
     */
    public static final Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    /**
     *
     * @return
     */
    public final String getSocket() {
        return ip;
    }

    /**
     *
     * @return
     */
    public final String getIP() {
        return ip;
    }

    /**
     *
     * @return
     */
    public String getIPA() {
        return ip;
    }

    /**
     *
     * @return
     */
    public final boolean isShutdown() {
        return shutdown;
    }

    /**
     *
     * @return
     */
    public final int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    /**
     *
     * @return
     */
    public final EventScriptManager getEventSM() {
        return eventSM;
    }

    /**
     *
     */
    public final void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerProperties.getProperty("ZeroMS.Events").split(","));
        eventSM.init();
    }

    /**
     *
     * @return
     */
    public final int getBossDropRate() {
        return BossdropRate;
    }

    /**
     *
     * @param dropRate
     */
    public final void setBossDropRate(final int dropRate) {
        this.BossdropRate = dropRate;
    }

    /**
     *
     * @return
     */
    public final int getMesoRate() {
        return mesoRate * doubleMeso;
    }

    /**
     *
     * @param mesoRate
     */
    public final void setMesoRate(final int mesoRate) {
        this.mesoRate = mesoRate;
    }

    /**
     *
     * @return
     */
    public final int getDropRate() {
        return dropRate * doubleDrop;
    }

    /**
     *
     * @param dropRate
     */
    public final void setDropRate(final int dropRate) {
        this.dropRate = dropRate;
    }

    /**
     *
     * @return
     */
    public int getDoubleExp() {
        if ((this.doubleExp < 0) || (this.doubleExp > 2)) {
            return 1;
        }
        return this.doubleExp;
    }

    /**
     *
     * @param doubleExp
     */
    public void setDoubleExp(int doubleExp) {
        if ((doubleExp < 0) || (doubleExp > 2)) {
            this.doubleExp = 1;
        } else {
            this.doubleExp = doubleExp;
        }
    }

    /**
     *
     * @return
     */
    public int getDoubleMeso() {
        if ((this.doubleMeso < 0) || (this.doubleMeso > 2)) {
            return 1;
        }
        return this.doubleMeso;
    }

    /**
     *
     * @param doubleMeso
     */
    public void setDoubleMeso(int doubleMeso) {
        if ((doubleMeso < 0) || (doubleMeso > 2)) {
            this.doubleMeso = 1;
        } else {
            this.doubleMeso = doubleMeso;
        }
    }

    /**
     *
     * @return
     */
    public int getDoubleDrop() {
        if ((this.doubleDrop < 0) || (this.doubleDrop > 2)) {
            return 1;
        }
        return this.doubleDrop;
    }

    /**
     *
     * @param doubleDrop
     */
    public void setDoubleDrop(int doubleDrop) {
        if ((doubleDrop < 0) || (doubleDrop > 2)) {
            this.doubleDrop = 1;
        } else {
            this.doubleDrop = doubleDrop;
        }
    }

    /*
     * public static final void startChannel_Main() { serverStartTime =
     * System.currentTimeMillis();
     *
     * for (int i = 0; i <
     * Integer.parseInt(ServerProperties.getProperty("tms.Count", "0")); i++) {
     * //newInstance(ServerConstants.Channel_Key[i], i +
     * 1).run_startup_configurations(); newInstance(i +
     * 1).run_startup_configurations(); } }
     */

    /**
     *
     */


    public static void startChannel_Main() {
        serverStartTime = System.currentTimeMillis();
        int ch = Integer.parseInt(ServerProperties.getProperty("ZeroMS.Count", "0"));
        if (ch > 10) {
            ch = 10;
        }
        for (int i = 0; i < ch; i++) {
            newInstance(i + 1).run_startup_configurations();
        }
    }

    /**
     *
     * @param channel
     */
    public static final void startChannel(final int channel) {
        serverStartTime = System.currentTimeMillis();
        for (int i = 0; i < Integer.parseInt(ServerProperties.getProperty("ZeroMS.Count", "0")); i++) {
            if (channel == i + 1) {

                //newInstance(ServerConstants.Channel_Key[i], i + 1).run_startup_configurations();
                newInstance(i + 1).run_startup_configurations();
                break;
            }
        }
    }

    /**
     *
     * @return
     */
    public Map<MapleSquadType, MapleSquad> getAllSquads() {
        return Collections.unmodifiableMap(mapleSquads);
    }

    /**
     *
     * @param type
     * @return
     */
    public final MapleSquad getMapleSquad(final String type) {
        return getMapleSquad(MapleSquadType.valueOf(type.toLowerCase()));
    }

    /**
     *
     * @param type
     * @return
     */
    public final MapleSquad getMapleSquad(final MapleSquadType type) {
        return mapleSquads.get(type);
    }

    /**
     *
     * @param squad
     * @param type
     * @return
     */
    public final boolean addMapleSquad(final MapleSquad squad, final String type) {
        final MapleSquadType types = MapleSquadType.valueOf(type.toLowerCase());
        if (types != null && !mapleSquads.containsKey(types)) {
            mapleSquads.put(types, squad);
            squad.scheduleRemoval();
            return true;
        }
        return false;
    }

    /**
     *
     * @param squad
     * @param type
     * @return
     */
    public boolean removeMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (type != null && mapleSquads.containsKey(type)) {
            if (mapleSquads.get(type) == squad) {
                mapleSquads.remove(type);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param types
     * @return
     */
    public final boolean removeMapleSquad(final MapleSquadType types) {
        if (types != null && mapleSquads.containsKey(types)) {
            mapleSquads.remove(types);
            return true;
        }
        return false;
    }

    /**
     *
     * @return
     */
    public int closeAllMerchant() {
        int ret = 0;
        merchLock.writeLock().lock();
        try {
            Iterator merchants_ = this.merchants.entrySet().iterator();
            while (merchants_.hasNext()) {
                HiredMerchant hm = (HiredMerchant) ((Map.Entry) merchants_.next()).getValue();
                hm.closeShop(true, false);
                hm.getMap().removeMapObject(hm);
                merchants_.remove();
                ret++;
            }
        } finally {
            merchLock.writeLock().unlock();
        }
        return ret;
    }

    /**
     *
     * @param hMerchant
     * @return
     */
    public final int addMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();

        int runningmer = 0;
        try {
            runningmer = running_MerchantID;
            merchants.put(running_MerchantID, hMerchant);
            running_MerchantID++;
        } finally {
            merchLock.writeLock().unlock();
        }
        return runningmer;
    }

    /**
     *
     * @param hMerchant
     */
    public final void removeMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();

        try {
            merchants.remove(hMerchant.getStoreId());
        } finally {
            merchLock.writeLock().unlock();
        }
    }

    /**
     *
     * @param accid
     * @return
     */
    public final boolean containsMerchant(final int accid) {
        boolean contains = false;

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                if (((HiredMerchant) itr.next()).getOwnerAccId() == accid) {
                    contains = true;
                    break;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return contains;
    }

    /**
     *
     * @param itemSearch
     * @return
     */
    public final List<HiredMerchant> searchMerchant(final int itemSearch) {
        final List<HiredMerchant> list = new LinkedList<>();
        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.searchItem(itemSearch).size() > 0) {
                    list.add(hm);
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return list;
    }

    /**
     *
     */
    public final void toggleMegaphoneMuteState() {
        this.MegaphoneMuteState = !this.MegaphoneMuteState;
    }

    /**
     *
     * @return
     */
    public final boolean getMegaphoneMuteState() {
        return MegaphoneMuteState;
    }

    /**
     *
     * @return
     */
    public int getEvent() {
        return eventmap;
    }

    /**
     *
     * @param ze
     */
    public final void setEvent(final int ze) {
        this.eventmap = ze;
    }

    /**
     *
     * @param t
     * @return
     */
    public MapleEvent getEvent(final MapleEventType t) {
        return events.get(t);
    }

    /**
     *
     * @return
     */
    public final Collection<PlayerNPC> getAllPlayerNPC() {
        return playerNPCs.values();
    }

    /**
     *
     * @param id
     * @return
     */
    public final PlayerNPC getPlayerNPC(final int id) {
        return playerNPCs.get(id);
    }

    /**
     *
     * @param npc
     */
    public final void addPlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.containsKey(npc.getId())) {
            removePlayerNPC(npc);
        }
        playerNPCs.put(npc.getId(), npc);
        getMapFactory().getMap(npc.getMapId()).addMapObject(npc);
    }

    /**
     *
     * @param npc
     */
    public final void removePlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.containsKey(npc.getId())) {
            playerNPCs.remove(npc.getId());
            getMapFactory().getMap(npc.getMapId()).removeMapObject(npc);
        }
    }

    /**
     *
     * @return
     */
    public final String getServerName() {
        return serverName;
    }

    /**
     *
     * @param sn
     */
    public final void setServerName(final String sn) {
        this.serverName = sn;
    }

    /**
     *
     * @return
     */
    public final int getPort() {
        return port;
    }

    /**
     *
     * @return
     */
    public static final Set<Integer> getChannelServer() {
        return new HashSet<>(instances.keySet());
    }

    /**
     *
     */
    public final void setShutdown() {
        this.shutdown = true;
        System.out.println("频道 " + channel + " 已开始关闭.");
    }

    /**
     *
     */
    public final void setFinishShutdown() {
        this.finishedShutdown = true;
        System.out.println("频道 " + channel + " 已关闭完成.");
    }

    /**
     *
     * @return
     */
    public final boolean isAdminOnly() {
        return adminOnly;
    }

    /**
     *
     * @return
     */
    public final static int getChannelCount() {
        return instances.size();
    }

    /**
     *
     * @return
     */
    public final MapleServerHandler getServerHandler() {
        return serverHandler;
    }

    /**
     *
     * @return
     */
    public final int getTempFlag() {
        return flags;
    }

    /**
     *
     * @return
     */
    public static Map<Integer, Integer> getChannelLoad() {
        Map<Integer, Integer> ret = new HashMap<>();
        for (ChannelServer cs : instances.values()) {
            ret.put(cs.getChannel(), cs.getConnectedClients());
        }
        return ret;
    }

    /**
     *
     * @return
     */
    public int getConnectedClients() {
        return getPlayerStorage().getConnectedClients();
    }

    /**
     *
     * @return
     */
    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = getPlayerStorage().getCheaters();

        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 20);
    }

    /**
     *
     * @param message
     */
    public void broadcastMessage(byte[] message) {
        broadcastPacket(new ByteArrayMaplePacket(message));
    }

    /**
     *
     * @param message
     */
    public void broadcastMessage(MaplePacket message) {
        broadcastPacket(message);
    }

    /**
     *
     * @param message
     */
    public void broadcastSmega(byte[] message) {
        broadcastSmegaPacket(new ByteArrayMaplePacket(message));
    }

    /**
     *
     * @param message
     */
    public void broadcastGMMessage(byte[] message) {
        broadcastGMPacket(new ByteArrayMaplePacket(message));
    }

    /**
     *
     */
    public void saveAll() {
        int ppl = 0;
        for (MapleCharacter chr : this.players.getAllCharactersThreadSafe()) {
            if (chr != null) {
                ppl++;
                chr.saveToDB(false, false);
            }
        }
        System.out.println("[自动存档] 已经将频道 " + this.channel + " 的 " + ppl + " 个玩家保存到数据中.");
    }

    /**
     *
     * @param dy
     */
    public void AutoNx(int dy) {
        mapFactory.getMap(910_000_000).AutoNxmht(dy);//泡点地图
    }

    /**
     *
     * @param dy
     */
    public void AutoTime(int dy) {
        try {
            for (ChannelServer chan : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                    if (chr == null) {
                        continue;
                    }
                    chr.gainGamePoints(1);
                    if (chr.getGamePoints() < 5) {
                        chr.resetGamePointsPD();
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     *
     * @return
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     *
     */
    public void addInstanceId() {
        instanceId++;
    }

    /**
     *
     */
    public void shutdown() {

        if (this.finishedShutdown) {
            return;
        }
        broadcastPacket(MaplePacketCreator.serverNotice(0, "游戏即将关闭维护..."));

        this.shutdown = true;
        System.out.println("频道 " + this.channel + " 正在清理活动脚本...");

        this.eventSM.cancel();

        System.out.println("频道 " + this.channel + " 正在保存所有角色数据...");

        // getPlayerStorage().disconnectAll();
        System.out.println("频道 " + this.channel + " 解除绑定端口...");
        acceptor.unbind(new InetSocketAddress(port));
        instances.remove(this.channel);
        setFinishShutdown();
    }

    //-------------------------------------0620新加 清除bosslog全部数据 功能2 清除bosslog制定数据

    /**
     *
     */
    public void setQKBossLog() {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("truncate table bosslog");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException Ex) {
            log.error("Error while insert bosslog.", Ex);
        }
    }

    /**
     *
     * @param id
     * @param bossid
     */
    public void setQLBossLog(String id, String bossid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("delete from bosslog where characterid = ? and bossid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setString(1, id);
            ps.setString(2, bossid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException Ex) {
            log.error("Error while insert bosslog.", Ex);
        }
    }

    /*  public void AutoSaveAll() {
          for (ChannelServer chan : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                if (chr != null){
                   // chr.saveToDB(true);
                     Calendar c = Calendar.getInstance();
                     int hour = c.get(11);
                     int minute = c.get(12);
                     if(hour==0&&(minute<1)){
                     
                     }
                }
            }
        }
         try {
             //ChannelServer.this.getWorldInterface().broadcastGMMessage(null, MaplePacketCreator.serverNotice(6, "[系统信息] 已经启用存档系统").getBytes());
             Calendar c = Calendar.getInstance();
             int hour = c.get(11);
             int minute = c.get(12);
             if(hour==0&&(minute<1)){
                 try{
                     Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement("DELETE FROM bosslog where 1=1 ");
                     ps.executeUpdate();
                     ps.close();
                     PreparedStatement ps1 = con.prepareStatement("UPDATE accounts_info SET gamePoints = 0 ");
                     ps1.executeUpdate();
                     ps1.close();
                  World.Broadcast.broadcastGMMessage( MaplePacketCreator.serverNotice(6, "[重置事件]当前时间凌晨12点,系统已经重置所有玩家的在积累时间和重置任务").getBytes());
                 }catch (SQLException ex) {
                        log.error("重置在线时间失败.", ex);
                     }
             }
           }
           catch (Throwable t)
           {
               log.error("存档失败.", t);
           }
  }
     */

    /**
     *
     * @param Name
     * @return
     */

    public static boolean forceRemovePlayerByCharName(String Name) {
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            Collection<MapleCharacter> chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter c : chrs) {
                if (c.getName().equalsIgnoreCase(Name)) {
                    try {
                        if (c.getMap() != null) {
                            c.getMap().removePlayer(c);
                        }
                        if (c.getClient() != null) {
                            c.getClient().disconnect(true, false, false);
                            c.getClient().getSession().close();
                        }

                    } catch (Exception ex) {
                    }
                    chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
                    if (chrs.contains(c)) {
                        ch.removePlayer(c);
                        return true;
                    }

                }
            }
        }
        return false;
    }

    /**
     *
     * @param c
     * @param accid
     */
    public static void forceRemovePlayerByAccId(MapleClient c, int accid) {
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            Collection<MapleCharacter> chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter chr : chrs) {
                if (chr.getAccountID() == accid) {
                    try {
                        if (chr.getClient() != null) {
                            if (chr.getClient() != c) {
                                chr.getClient().disconnect(true, false, false);
                            }
                        }
                    } catch (Exception ex) {
                    }
                    chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
                    if (chr.getClient() != c) {
                        if (chrs.contains(chr)) {
                            ch.removePlayer(chr);
                        }
                        if (chr.getMap() != null) {
                            chr.getMap().removePlayer(chr);
                        }
                    }
                }
            }
        }
        try {
            Collection<MapleCharacter> chrs = CashShopServer.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter chr : chrs) {
                if (chr.getAccountID() == accid) {
                    try {
                        if (chr.getClient() != null) {
                            if (chr.getClient() != c) {
                                chr.getClient().disconnect(true, false, false);
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    /**
     *
     * @param accid
     */
    public static void forceRemovePlayerByAccId(int accid) {
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            Collection<MapleCharacter> chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
            for (MapleCharacter c : chrs) {
                if (c.getAccountID() == accid) {
                    try {
                        if (c.getClient() != null) {
                            c.getClient().disconnect(true, false, false);
                        }
                    } catch (Exception ex) {
                    }
                    chrs = ch.getPlayerStorage().getAllCharactersThreadSafe();
                    if (chrs.contains(c)) {
                        ch.removePlayer(c);
                    }
                    if (c.getMap() != null) {
                        c.getMap().removePlayer(c);
                    }
                }
            }
        }

    }

}
