package com.lzf.stackwatcher.agent.core;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.stackwatcher.agent.*;
import com.lzf.stackwatcher.agent.Service;
import com.lzf.stackwatcher.common.*;
import com.lzf.stackwatcher.zookeeper.ZooKeeper;
import com.lzf.stackwatcher.zookeeper.ZooKeeperConnector;
import org.apache.log4j.Logger;

/**
 * @author 李子帆
 * @time 2018年11月20日 下午2:20:23
 */

public class StandardAgent extends ContainerBase<Void> implements Agent {
	
	private static final Logger log = Logger.getLogger(StandardAgent.class);

	private Map<String, Service<?>> serviceMap = new ConcurrentHashMap<>();

	private ConfigManager configManager = new DefaultConfigManager("ConfigManager");

	private ZooKeeper zooKeeper;
	
	public StandardAgent() {
		setName("Agent");
		addLifecycleEventListener(LifecycleLoggerListener.INSTANCE);


		configManager.registerConfig(new GlobalConfig(this));
		configManager.registerConfig(new ZooKeeperConfig(this));
	}
	
	@Override
	protected void initInternal() {
		 zooKeeper = new ZooKeeperConnector(this);

		 DomainManagerService dms = new StandardDomainManagerService(this);
		 serviceMap.put(dms.serviceName(), dms);

		 RedisService rs = new StandardRedisService(this);
		 serviceMap.put(rs.serviceName(), rs);

		 TransferService ts = new StandardTransferService(this);
		 serviceMap.put(ts.serviceName(), ts);

		for(Map.Entry<String, Service<?>> entry : serviceMap.entrySet()) {
			entry.getValue().init();
		}
	}
	
	@Override
	protected void startInternal() {
		for(Map.Entry<String, Service<?>> entry : serviceMap.entrySet()) {
			entry.getValue().start();
		}

		// 设置ZNode
		DomainManagerService dms = getService(DomainManagerService.DEFAULT_SERVICE_NAME, DomainManagerService.class);
		MonitorService.Config cfg = getConfig(MonitorService.DEFAULT_CONFIG_NAME, MonitorService.Config.class);

		JSONObject obj = new JSONObject();
		obj.put("host", dms.getHostName());

		JSONArray insArr = new JSONArray();
		insArr.add(dms.getAllInstanceUUID());
		obj.put("instances", insArr);

		obj.put("instance-monitor-enable", cfg.enable());
		if(cfg.enable()) {
			JSONObject ins = new JSONObject();
			ins.put("cpu", cfg.insVCPUMonitorRate());
			ins.put("network", cfg.insNetworkIOMonitorRate());
			ins.put("disk-io", cfg.insDiskIOMonitorRate());
			ins.put("disk-capacity", cfg.insDiskCapacityMonitorRate());
			obj.put("instance-monitor-rate", ins);
		}

		JSONObject nova = new JSONObject();
		nova.put("cpu", cfg.novaCPUMonitorRate());
		nova.put("memory", cfg.novaRAMMonitorRate());
		nova.put("network", cfg.novaNetworkIOMonitorRate());
		nova.put("disk-io", cfg.novaDiskIOMonitorRate());
		nova.put("disk-capacity", cfg.novaDiskCapacityMonitorRate());
		obj.put("nova-monitor-rate", nova);

		obj.put("instance-agent-port", cfg.insAgentRecivePort());

		try {
			zooKeeper.createTemporaryNodeRecursive("/stackwatcher/agent/" + dms.getHostName(), obj.toJSONString().getBytes(Charset.forName("UTF-8")));
		} catch (Exception e) {
			log.error("Can not create ZNode at path /stackwatcher/agent/" + dms.getHostName());
		}
	}
	
	@Override
	protected void stopInternal() {
		for(Map.Entry<String, Service<?>> entry : serviceMap.entrySet()) {
			entry.getValue().stop();
		}
	}
	
	@Override
	protected void restartInternal() {
		for(Map.Entry<String, Service<?>> entry : serviceMap.entrySet()) {
			entry.getValue().restart();
		}
	}
	
	@Override
	public Service<?> getService(String serviceName) {
		return serviceMap.get(serviceName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Service<?>> T getService(String serviceName, Class<T> serviceClass) {
		return (T) serviceMap.get(serviceName);
	}

	
	@Override
	public void addService(Service<?> service) {
		serviceMap.put(service.serviceName(), service);
	}

	@Override
	public ZooKeeper getZooKeeper() {
		return zooKeeper;
	}

	@Override
	public InputStream loadResource(String path) throws Exception {
		return configManager.loadResource(path);
	}

	@Override
	public void registerConfig(Config config) {
		configManager.registerConfig(config);
	}

	@Override
	public void updateConfig(Config config) {
		configManager.updateConfig(config);
	}

	@Override
	public void removeConfig(Config config) {
		configManager.removeConfig(config);
	}

	@Override
	public boolean saveConfig(Config config) {
		return configManager.saveConfig(config);
	}

	@Override
	public Config getConfig(String name) {
		return configManager.getConfig(name);
	}

	@Override
	public <C extends Config> C getConfig(String name, Class<C> requireType) {
		return configManager.getConfig(name, requireType);
	}

	@Override
	public void registerConfigEventListener(ConfigEventListener listener) {
		configManager.registerConfigEventListener(listener);
	}

	@Override
	public void removeConfigEventListener(ConfigEventListener listener) {
		configManager.removeConfigEventListener(listener);
	}
}
