# Introduction
Example

```hcl-terraform
resource "kubernetes_config_map" "connectors" {

  metadata {
    name = "connectors"
    namespace = var.namespace
  }

  data = {
    "confluent-datagen-ratings.json" = jsonencode({
      "name" = "confluent-datagen-ratings",
      "connector.class" = "io.confluent.kafka.connect.datagen.DatagenConnector"
      "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
      "value.converter" = "io.confluent.connect.avro.AvroConverter"
      "value.converter.schema.registry.url" = local.schema_registry_internal_url
      "quickstart" = "ratings"
      "kafka.topic" = "ratings"
      "max.interval" = 2000
      "iterations" = -1
    })
    "mysql-source-customers-raw.json" = jsonencode({
      "name": "mysql-source-customers-raw",
      "connector.class": "io.debezium.connector.mysql.MySqlConnector",
      "key.converter": "org.apache.kafka.connect.storage.StringConverter",
      "value.converter": "io.confluent.connect.avro.AvroConverter",
      "value.converter.schema.registry.url": local.schema_registry_internal_url,
      "database.hostname": "mysql",
      "snapshot.mode": "when_needed",
      "database.port": "3306",
      "database.user": "connect",
      "database.password": var.user_password,
      "database.server.name": "mysql",
      "table.whitelist": "customer.customers",
      "database.history.kafka.bootstrap.servers": local.kafka_internal_bootstrap_url,
      "database.history.kafka.topic": "dbhistory.confab-raw",
      "database.history.producer.security.protocol": "SASL_PLAINTEXT",
      "database.history.consumer.security.protocol": "SASL_PLAINTEXT",
      "database.history.producer.sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"test123\";",
      "database.history.consumer.sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"test123\";",
      "database.history.consumer.sasl.mechanism": "PLAIN",
      "database.history.producer.sasl.mechanism": "PLAIN",
      "include.schema.changes": "true",
      "transforms": "addTopicSuffix",
      "transforms.addTopicSuffix.type":"org.apache.kafka.connect.transforms.RegexRouter",
      "transforms.addTopicSuffix.regex":"(.*)",
      "transforms.addTopicSuffix.replacement":"$1-raw"
    })
    "mysql-source-customers.json" = jsonencode({
      "name" = "mysql-source-customers"
      "tasks.max" = "1"
      "connector.class" = "io.debezium.connector.mysql.MySqlConnector"
      "key.converter" = "org.apache.kafka.connect.storage.StringConverter"
      "value.converter" = "io.confluent.connect.avro.AvroConverter"
      "value.converter.schema.registry.url" = local.schema_registry_internal_url
      "database.hostname" = "mysql"
      "database.port" = "3306"
      "database.user" = "connect"
      "database.password" = var.user_password
      "database.server.name" = "mysql"
      "table.whitelist" = "customer.customers"
      "snapshot.mode" = "when_needed"
      "database.history.kafka.bootstrap.servers" = local.kafka_internal_bootstrap_url
      "database.history.kafka.topic" = "dbhistory.confab"
      "database.history.producer.security.protocol" = "SASL_PLAINTEXT"
      "database.history.consumer.security.protocol" = "SASL_PLAINTEXT"
      "database.history.producer.sasl.jaas.config" = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"test123\";"
      "database.history.consumer.sasl.jaas.config" = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"test123\";"
      "database.history.consumer.sasl.mechanism" = "PLAIN"
      "database.history.producer.sasl.mechanism" = "PLAIN"
      "include.schema.changes" = "true"
      "transforms" = "unwrap"
      "transforms.unwrap.type" = "io.debezium.transforms.UnwrapFromEnvelope"
      "transforms.InsertTopic.type" = "org.apache.kafka.connect.transforms.InsertField$Value"
    })
  }
}

resource "kubernetes_deployment" "connector_operator" {
  metadata {
    name = "connector-operator"
    namespace = var.namespace
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "connector-operator"
      }
    }
    template {
      metadata {
        labels = {
          app = "connector-operator"
        }
      }
      spec {
        volume {
          name = "config-volume"
          config_map {
            name = kubernetes_config_map.connectors.metadata[0].name
          }
        }
        container {
          image = "jcustenborder/connector-operator:0.1-SNAPSHOT"
          name = "console"
          env {
            name = "CONNECT_HOST"
            value = "connectors-0-internal"
          }
          env {
            name = "CONNECT_DIRECTORY"
            value = "/config"
          }
          volume_mount {
            mount_path = "/config"
            name = "config-volume"
          }
        }
      }
    }
  }
}
```