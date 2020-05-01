package com.github.jcustenborder.kafka.connect.operator;

import com.github.jcustenborder.docker.junit5.Compose;
import com.github.jcustenborder.docker.junit5.Port;
import com.google.common.util.concurrent.ServiceManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Properties;

@Disabled
@Compose(dockerComposePath = "src/test/resources/docker-compose.yml", clusterHealthCheck = ConnectClusterHealthCheck.class)
public class ConnectOperatorServiceIT {
  private static final Logger log = LoggerFactory.getLogger(ConnectOperatorServiceIT.class);

  @Test
  public void foo(@Port(container = "connect", internalPort = 8083) InetSocketAddress address) throws Exception {
    Properties properties = new Properties();
    properties.put(ConnectOperatorConfig.HOST_CONFIG, address.getHostString());
    properties.put(ConnectOperatorConfig.PORT_CONFIG, Integer.toString(address.getPort()));
    properties.put(ConnectOperatorConfig.DIRECTORY_CONF, "src/test/resources/json");
    properties.put(ConnectOperatorConfig.INITIAL_DELAY_CONF, "1");
    properties.put(ConnectOperatorConfig.PERIOD_CONF, "1");

    ConnectOperatorService service = new ConnectOperatorService(properties);
    ServiceManager serviceManager = new ServiceManager(Collections.singletonList(service));
    serviceManager.startAsync();
    service.awaitRunning();




    Thread.sleep(1000 * 10);
  }

}
