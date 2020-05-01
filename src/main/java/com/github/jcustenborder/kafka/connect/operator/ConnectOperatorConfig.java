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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

class ConnectOperatorConfig extends AbstractConfig {
  private static final Logger log = LoggerFactory.getLogger(ConnectOperatorConfig.class);
  public static final String HOST_CONFIG = "host";
  static final String HOST_DOC = "The host running kafka connect";
  public static final String PORT_CONFIG = "port";
  static final String PORT_DOC = "The port running kafka connect";
  public final static String DIRECTORY_CONF = "directory";
  final static String DIRECTORY_DOC = "directory";
  public final static String INITIAL_DELAY_CONF = "initial.delay.secs";
  public final static String INITIAL_DELAY_DOC = "initial.delay.secs";
  public final static String PERIOD_CONF = "period.secs";
  public final static String PERIOD_DOC = "period.secs";

  public final Duration initialDelay;
  public final Duration period;

  public final int port;
  public final String host;
  public final File directory;
  public final Map<String, Map<String, String>> connectorConfigurations;
  private ObjectMapper objectMapper = new ObjectMapper();

  Duration getDuration(String key) {
    int seconds = getInt(key);
    return Duration.ofSeconds(seconds);
  }

  public ConnectOperatorConfig(Map<?, ?> originals) {
    super(config(), originals);
    this.host = this.getString(HOST_CONFIG);
    this.port = this.getInt(PORT_CONFIG);
    this.directory = getDirectory(DIRECTORY_CONF);
    this.connectorConfigurations = loadConfigurations();
    this.initialDelay = getDuration(INITIAL_DELAY_CONF);
    this.period = getDuration(PERIOD_CONF);
  }

  void assertProperty(Map<String, String> config, File inputFile, String... properties) {
    for (String property : properties) {
      if (!config.containsKey(property)) {
        throw new ConfigException(
            DIRECTORY_CONF,
            inputFile,
            String.format("Configuration must include '%s' property.", property)
        );
      }
    }
  }

  Map.Entry<String, Map<String, String>> loadJsonConfig(File inputFile) {
    log.info("loadJsonConfig() - loading {}", inputFile);
    try {
      Map<String, String> config = this.objectMapper.readValue(inputFile, Map.class);
      assertProperty(config, inputFile, "connector.class", "name");
      String connectorName = config.get("name");
      Map.Entry<String, Map<String, String>> entry = new AbstractMap.SimpleEntry<>(
          connectorName,
          config
      );
      return entry;
    } catch (IOException ex) {
      ConfigException exception = new ConfigException(
          DIRECTORY_CONF,
          inputFile,
          "Could not parse file"
      );
      exception.initCause(ex);
      throw exception;
    }
  }

  private Map<String, Map<String, String>> loadConfigurations() {
    Map<String, Map<String, String>> result = new LinkedHashMap<>();
    File[] configFiles = this.directory.listFiles();
    Stream.of(configFiles)
        .filter(File::isFile)
        .filter(f -> f.getName().endsWith(".json"))
        .map(this::loadJsonConfig)
        .forEach(e -> result.put(e.getKey(), e.getValue()));
    return result;
  }

  public static ConfigDef config() {
    return new ConfigDef()
        .define(HOST_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, HOST_DOC)
        .define(DIRECTORY_CONF, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, new DirectoryValidator(), ConfigDef.Importance.HIGH, DIRECTORY_DOC)
        .define(PORT_CONFIG, ConfigDef.Type.INT, 8083, ConfigDef.Importance.HIGH, PORT_DOC)
        .define(INITIAL_DELAY_CONF, ConfigDef.Type.INT, 10, ConfigDef.Importance.HIGH, INITIAL_DELAY_DOC)
        .define(PERIOD_CONF, ConfigDef.Type.INT, 30, ConfigDef.Importance.HIGH, PERIOD_DOC);
  }

  File getDirectory(String key) {
    String value = getString(key);
    return new File(value);
  }

  static class DirectoryValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(String name, Object value) {
      if (!(value instanceof String)) {
        throw new ConfigException(name, value, "value should be a string.");
      }
      File file = new File(value.toString());
      if (!file.isDirectory()) {
        throw new ConfigException(name, value, "value should be an existing directory.");
      }
    }
  }


}
