/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.operator;

import com.github.jcustenborder.kafka.connect.client.KafkaConnectClient;
import com.github.jcustenborder.kafka.connect.client.KafkaConnectException;
import com.github.jcustenborder.kafka.connect.client.model.ConnectorInfo;
import com.github.jcustenborder.kafka.connect.client.model.ConnectorStatus;
import com.github.jcustenborder.kafka.connect.client.model.ServerInfo;
import com.github.jcustenborder.kafka.connect.client.model.TaskStatus;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

class ConnectOperatorService extends AbstractScheduledService {
  final ConnectOperatorConfig config;
  private static final Logger log = LoggerFactory.getLogger(ConnectOperatorService.class);
  KafkaConnectClient client;

  public ConnectOperatorService(Properties properties) {
    this.config = new ConnectOperatorConfig(properties);

  }

  @Override
  protected void startUp() throws Exception {
    log.info("startUp() - Creating client.");
    KafkaConnectClient.Settings.Builder builder = KafkaConnectClient.builder()
        .host(this.config.host)
        .port(this.config.port);
    this.client = builder.createClient();
  }

  @Override
  protected void shutDown() throws Exception {
    this.client.close();
  }


  @Override
  protected void runOneIteration() throws Exception {
    try {
      log.info("Checking if server is available.");
      ServerInfo serverInfo = this.client.serverInfo();
      log.info("Server responded with commit='{}' kafkaClusterId='{}' version='{}'",
          serverInfo.commit(),
          serverInfo.kafkaClusterId(),
          serverInfo.version()
      );

      for (final String connectorName : this.config.connectorConfigurations.keySet()) {
        final Map<String, String> config = this.config.connectorConfigurations.get(connectorName);
        try {
          processConnector(connectorName, config);
        } catch (Exception ex) {
          log.error("Exception thrown while processing connector '{}'", connectorName, ex);
        }
      }
    } catch (Exception ex) {
      log.error("Exception thrown", ex);
    }
  }

  static final com.github.jcustenborder.kafka.connect.client.model.State RUNNING = com.github.jcustenborder.kafka.connect.client.model.State.Running;
  static final com.github.jcustenborder.kafka.connect.client.model.State FAILED = com.github.jcustenborder.kafka.connect.client.model.State.Failed;


  private void processConnector(String connectorName, Map<String, String> config) throws IOException {
    log.info("processConnector() - connectorName = '{}'", connectorName);
    ConnectorStatus status;
    try {
      status = this.client.status(connectorName);
    } catch (KafkaConnectException exception) {
      if (404 == exception.errorCode()) {
        log.info("Connector '{}' does not exist", connectorName, exception);
        status = null;
      } else {
        throw exception;
      }
    }

    if (null == status) {
      ConnectorInfo connectorInfo = this.client.createOrUpdate(connectorName, config);
      log.info("Created Connector '{}'\n'{}", connectorName, connectorInfo);
    } else {
      log.info("Connector '{}' status {}", connectorName, status);
      if (RUNNING != status.connector().state()) {
        log.info("Restarting connector '{}'", connectorName);
        this.client.restart(connectorName);
      }
      for (TaskStatus taskStatus : status.tasks()) {
        log.info("Connector '{}' task status {}", connectorName, taskStatus);
        if (FAILED == taskStatus.state()) {
          log.warn("Task Failed. Connector '{}' task '{}'\n{}", connectorName, taskStatus.id(), taskStatus.trace());
        }
        if (RUNNING != taskStatus.state()) {
          log.info("Restarting task. Connector '{}' task '{}'", connectorName, taskStatus.id());
          this.client.restart(connectorName, taskStatus.id());
        }
      }
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(
        this.config.initialDelay,
        this.config.period
    );
  }
}
