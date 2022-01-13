package com.lightbend.seldon.configuration

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * Various configuration parameters.
 */
object ModelServingConfiguration {

  val config = ConfigFactory.load()

  // Minio
  val MINIO_URL = try {
    config.getString("minio.miniourl")
  } catch {
    case _: Throwable ⇒ "http://minio-service.kubeflow.svc.cluster.local:9000"
  }
  val MINIO_KEY = try {
    config.getString("minio.miniokey")
  } catch {
    case _: Throwable ⇒ "minio"
  }
  val MINIO_SECRET = try {
    config.getString("minio.miniosecret")
  } catch {
    case _: Throwable ⇒ "minio123"
  }

  // GRPC
  val GRPC_HOST = try {
    config.getString("grpc.host")
  } catch {
    case _: Throwable ⇒ "ambassador.seldon.svc.cluster.local"
  }
  val GRPC_PORT = try {
    config.getInt("grpc.port")
  } catch {
    case _: Throwable ⇒ 80
  }
  val GRPC_TARGET = try {
    config.getString("grpc.target")
  } catch {
    case _: Throwable ⇒ "kubernetes:///seldon/ambassador/8080"
  }

  // HTTP
  val REST_PATH = try {
    config.getString("rest.path")
  } catch {
    case _: Throwable ⇒ "http://ambassador.seldon.svc.cluster.local:80/seldon/seldon/rest-tfserving/v1/models/recommender/:predict"
  }

  val GRPC_FRAUD_MODEL_PATH = try {
    config.getString("grpc.model.path")
  } catch {
    case _: Throwable ⇒ "/mnt/sharedLibs/models/fraud/1"
  }

  // Load
  val DATA_FREQUENCY: FiniteDuration = (try {
    Duration(config.getString("source.frequency"))
  } catch {
    case _: Throwable ⇒ Duration("3 millisecond")
  }).toMillis.millisecond
  val DATA_FILE = try {
    config.getString("source.data")
  } catch {
    case _: Throwable ⇒ "data/fraud/data/creditcard.csv"
  }
}
