package com.lightbend.seldon.executors.tensor.lb

import java.net.InetSocketAddress

import io.grpc._
import io.grpc.internal._
import java.util.concurrent._

import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.client._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

// Implementation is based on
// https://github.com/saturnism/grpc-by-example-java/blob/master/kubernetes-lb-example/echo-client-lb-api/src/main/java/com/example/grpc/client/KubernetesNameResolver.java
//
class KubernetesNameResolver(namespace: String, name: String, port: Int,
                             timerServiceResource: SharedResourceHolder.Resource[ScheduledExecutorService]) extends NameResolver {

  private val kubernetesClient = new DefaultKubernetesClient()
  private var listener: NameResolver.Listener = _
  private var refreshing = false
  private var watching = false

  override def getServiceAuthority(): String = kubernetesClient.getMasterUrl().getAuthority();

  override def shutdown(): Unit = kubernetesClient.close();

  override def start(listener: NameResolver.Listener): Unit = {
    this.listener = listener
    refresh
  }

  override def refresh(): Unit = synchronized {
    println("Refreshing endpoints")
    if (refreshing) return
    try {
      refreshing = true
      kubernetesClient.endpoints.inNamespace(namespace).withName(name).get match {
        case endpoints if (endpoints != null) ⇒ // got endpoints
          update(endpoints)
          watch()
        case _ ⇒ // Didn't find anything, retrying
          val timerService = SharedResourceHolder.get(timerServiceResource)
          timerService.schedule(() ⇒ new Callable[Unit] {
            override def call(): Unit = refresh()
          }, 30, TimeUnit.SECONDS)
      }
    } finally
      refreshing = false
    ()
  }

  private def update(endpoints: Endpoints): Unit = {
    endpoints.getSubsets match {
      case subsets if (subsets != null) ⇒
        val addresses = new ListBuffer[EquivalentAddressGroup]
        subsets.asScala.foreach { subset ⇒
          subset.getPorts.asScala.filter(p ⇒ (p != null) && (p.getPort == port)).size match {
            case s if s > 0 ⇒
              subset.getAddresses.asScala.foreach { address ⇒
                addresses += new EquivalentAddressGroup(new InetSocketAddress(address.getIp(), port))
              }
            case _ ⇒ Seq.empty
          }
        }
        println(s"adding servers $addresses")
        listener.onAddresses(addresses.asJava, Attributes.EMPTY)
      case _ ⇒
    }
  }

  private def watch(): Unit = synchronized {

    watching match {
      case false ⇒
        kubernetesClient.endpoints.inNamespace(namespace).withName(name).watch(new Watcher[Endpoints]() {
          override def eventReceived(action: Watcher.Action, endpoints: Endpoints): Unit = {
            action match {
              case Watcher.Action.MODIFIED ⇒
              case Watcher.Action.ADDED ⇒
                println("Endpoints modified")
                update(endpoints)
              case Watcher.Action.DELETED ⇒
                println("Endpoints deleted")
                listener.onAddresses(Seq.empty.asJava, Attributes.EMPTY)
              case Watcher.Action.ERROR ⇒
                println("Endpoints error")
                listener.onError(io.grpc.Status.UNKNOWN)
            }
          }

          override def onClose(e: KubernetesClientException): Unit = {
            watching = false
          }
        })
        watching = true
      case _ ⇒ ()
    }
  }
}
