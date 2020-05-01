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

import com.github.jcustenborder.DockerProperties;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) throws Exception {
    DockerProperties dockerProperties = DockerProperties.builder().patterns("^CONNECT_(.+)$").build();

    ConnectOperatorService service = new ConnectOperatorService(dockerProperties.toProperties());
    final ServiceManager serviceManager = new ServiceManager(Collections.singletonList(service));
    serviceManager.startAsync();

    Runtime.getRuntime().addShutdownHook(new Thread(serviceManager::stopAsync));
    serviceManager.awaitStopped();
  }
}
