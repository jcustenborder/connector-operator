package com.github.jcustenborder.kafka.connect.operator;

import com.google.common.util.concurrent.ServiceManager;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

@Disabled
public class ConnectOperatorServiceTest {
  private static final Logger log = LoggerFactory.getLogger(ConnectOperatorServiceTest.class);
  MockWebServer mockWebServer;
  int requestCount;
  @BeforeEach
  public void before() throws IOException {
    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();
    this.requestCount = 0;
  }

  @AfterEach
  public void afterEach() throws IOException {
    this.mockWebServer.close();
  }

  void enqueue(MockResponse response) {
    this.requestCount++;
    this.mockWebServer.enqueue(response);
  }
  void enqueueError(int code) {
    MockResponse response = new MockResponse();
    response.setResponseCode(code);
    enqueue(response);
  }


  @Test
  public void test() throws InterruptedException {
    enqueueError(500);
    enqueueError(500);
    MockResponse response = new MockResponse();
    response.setResponseCode(200);
    response.setHeader("Content-Type", "application/json");
    response.setBody("{\n" +
        "  \"version\":\"5.5.0\",\n" +
        "  \"commit\":\"e5741b90cde98052\",\n" +
        "  \"kafka_cluster_id\":\"I4ZmrWqfT2e-upky_4fdPA\"\n" +
        "}");
    enqueue(response);

    Properties properties = new Properties();
    properties.put(ConnectOperatorConfig.HOST_CONFIG, this.mockWebServer.getHostName());
    properties.put(ConnectOperatorConfig.PORT_CONFIG, Integer.toString(this.mockWebServer.getPort()));
    properties.put(ConnectOperatorConfig.DIRECTORY_CONF, "src/test/resources/json");
    properties.put(ConnectOperatorConfig.INITIAL_DELAY_CONF, "1");
    properties.put(ConnectOperatorConfig.PERIOD_CONF, "1");
    ConnectOperatorService service = new ConnectOperatorService(properties);
    ServiceManager serviceManager = new ServiceManager(Collections.singletonList(service));
    serviceManager.startAsync();
    serviceManager.awaitHealthy();

    while (this.mockWebServer.getRequestCount() < this.requestCount) {
      Thread.sleep(1000);
    }
    log.info("Stopping service manager");

    serviceManager.stopAsync();
    serviceManager.awaitStopped();
  }


}
