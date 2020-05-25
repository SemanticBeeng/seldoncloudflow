package com.lightbend.seldon.executors.tensor.lb
import io.grpc._
import io.grpc.internal._

import java.net._

// Implementation is based on
// https://github.com/saturnism/grpc-by-example-java/blob/master/kubernetes-lb-example/echo-client-lb-api/src/main/java/com/example/grpc/client/KubernetesNameResolverProvider.java
//

object KubernetesNameResolverProvider {
  val SCHEME = "kubernetes"
}

class KubernetesNameResolverProvider extends NameResolverProvider {

  import KubernetesNameResolverProvider._

  override protected def isAvailable = true

  override protected def priority = 5

  // URI here is in the form kubernetes:///{namespace}/{service}/{port}
  override def newNameResolver(targetUri: URI, params: Attributes): NameResolver =

    targetUri.getScheme == SCHEME match {
      case true ⇒
        val parts = targetUri.getPath().split("/")
        parts.size match {
          case s if s == 4 ⇒
            try {
              new KubernetesNameResolver(parts(1), parts(2), parts(3).toInt, GrpcUtil.TIMER_SERVICE)
            } catch {
              case t: Throwable ⇒ throw new IllegalArgumentException("Unable to parse port number", t)
            }
          case _ ⇒ throw new IllegalArgumentException("URL Must be formatted like kubernetes:///{namespace}/{service}/{port}")
        }
      case _ ⇒ throw new IllegalArgumentException("URL Must be formatted like kubernetes:///{namespace}/{service}/{port}")
    }

  override def getDefaultScheme: String = KubernetesNameResolverProvider.SCHEME
}
