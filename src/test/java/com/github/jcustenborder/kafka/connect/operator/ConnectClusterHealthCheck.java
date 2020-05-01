package com.github.jcustenborder.kafka.connect.operator;

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;

import java.util.function.Function;

public class ConnectClusterHealthCheck implements ClusterHealthCheck {
  @Override
  public SuccessOrFailure isClusterHealthy(Cluster cluster) throws InterruptedException {
    Container connectContainer = cluster.container("connect");

    return connectContainer.portIsListeningOnHttpAndCheckStatus2xx(
        8083,
        new Function<DockerPort, String>() {
          @Override
          public String apply(DockerPort dockerPort) {
            return dockerPort.inFormat("http://$HOST:$EXTERNAL_PORT/");
          }
        }
    );
  }
}
