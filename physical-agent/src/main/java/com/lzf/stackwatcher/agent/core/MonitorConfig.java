package com.lzf.stackwatcher.agent.core;

import com.lzf.stackwatcher.agent.MonitorService;
import com.lzf.stackwatcher.common.AbstractConfig;
import com.lzf.stackwatcher.common.ConfigInitializationException;
import com.lzf.stackwatcher.common.ConfigManager;
import com.lzf.stackwatcher.zookeeper.ZooKeeper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;
/**
 * 监控服务配置项
 * 包含Libvirtd主机名/IP(默认localhost)、端口号(默认6379)、密码(默认为空)、默认数据库编号(默认为0)
 */
public class MonitorConfig extends AbstractConfig implements MonitorService.Config {

    private static final String CONFIG_PATH = "classpath://config.properties";

    private final ZooKeeper zooKeeper;

    private boolean enable = true;

    private int insVCPURate = 10;
    private int insRAMRate = 10;
    private int insNetIORate = 60;
    private int insDiskIORate = 60;
    private int insDiskCapRate = 600;

    private int novaCPURate = 15;
    private int novaRAMRate = 15;
    private int novaNetIORate = 60;
    private int novaDiskIORate = 60;
    private int novaDiskCapRate = 600;

    private int storagePoolRate = 60;
    private boolean storageVol = false;

    private int insAgentRecivePort = 25001;

    MonitorConfig(ConfigManager configManager, ZooKeeper zooKeeper) {
        super(configManager, MonitorService.DEFAULT_CONFIG_NAME);
        this.zooKeeper = zooKeeper;
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        GlobalConfig cfg = configManager.getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if(!cfg.isRemoteConfig()) {
            try (InputStream is = configManager.loadResource(CONFIG_PATH)) {
                Properties p = new Properties();
                p.load(is);
                readConfig(p);
            } catch (Exception e) {
                throw new ConfigInitializationException(e);
            }
        } else {
            try {
                byte[] b = zooKeeper.readNode(cfg.getZNodePath());
                InputStream bis = new ByteArrayInputStream(b);
                Properties p = new Properties();
                p.load(bis);
                readConfig(p);
            } catch (UnknownHostException e) {
                throw new Error(e);
            } catch (Exception e) {
                throw new ConfigInitializationException(e);
            }
        }
    }

    private void readConfig(Properties p) {
        enable = Boolean.valueOf(p.getProperty("monitor.instance"));
        if(!enable)
            return;

        insVCPURate = Integer.valueOf(p.getProperty("monitor.instance.cpu"));
        insRAMRate = Integer.valueOf(p.getProperty("monitor.instance.memory"));
        insNetIORate = Integer.valueOf(p.getProperty("monitor.instance.netio"));
        insDiskIORate = Integer.valueOf(p.getProperty("monitor.instance.diskio"));
        insDiskCapRate = Integer.valueOf(p.getProperty("monitor.instance.diskcap"));

        novaCPURate = Integer.valueOf(p.getProperty("monitor.physical.cpu"));
        novaRAMRate = Integer.valueOf(p.getProperty("monitor.physical.memory"));
        novaNetIORate = Integer.valueOf(p.getProperty("monitor.physical.netio"));
        novaDiskIORate = Integer.valueOf(p.getProperty("monitor.physical.diskio"));
        novaDiskCapRate = Integer.valueOf(p.getProperty("monitor.physical.diskcap"));

        storagePoolRate = Integer.valueOf(p.getProperty("monitor.storagepool"));
        storageVol = Boolean.valueOf(p.getProperty("monitor.storagevol.enable"));

        insAgentRecivePort = Integer.valueOf(p.getProperty("instance.agent.port"));
    }

    @Override public boolean enable() { return enable; }
    @Override public int insVCPUMonitorRate() { return insVCPURate; }
    @Override public int insRAMMonitorRate() { return insRAMRate; }
    @Override public int insNetworkIOMonitorRate() { return insNetIORate; }
    @Override public int insDiskIOMonitorRate() { return insDiskIORate; }
    @Override public int insDiskCapacityMonitorRate() { return insDiskCapRate; }
    @Override public int novaCPUMonitorRate() { return novaCPURate; }
    @Override public int novaRAMMonitorRate() { return novaRAMRate; }
    @Override public int novaNetworkIOMonitorRate() { return novaNetIORate; }
    @Override public int novaDiskIOMonitorRate() { return novaDiskIORate; }
    @Override public int novaDiskCapacityMonitorRate() {return novaDiskCapRate; }
    @Override public int storagePoolRate() { return storagePoolRate; }
    @Override public boolean enableStorageVolMonitor() { return storageVol; }
    @Override public int insAgentRecivePort() { return insAgentRecivePort; }

    @Override public String toString() {
        return "MointorConfig [insVCPURate=" + insVCPURate + ", insRAMRate=" + insRAMRate + ", insNetIORate="
                + insNetIORate + ", insDiskIORate=" + insDiskIORate + ", insDiskCapRate=" + insDiskCapRate
                + ", novaCPURate=" + novaCPURate + ", novaRAMRate=" + novaRAMRate + ", novaNetIORate="
                + novaNetIORate + ", novaDiskIORate=" + novaDiskIORate + ", novaDiskCapRate=" + novaDiskCapRate
                + "]";
    }
}
