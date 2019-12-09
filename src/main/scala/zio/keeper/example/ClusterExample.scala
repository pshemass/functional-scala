package zio.keeper.example

import zio._
import zio.keeper.membership._
import zio.keeper.transport._
import zio.keeper.discovery._
import zio.keeper._
import zio.duration._
import zio.clock._
import zio.logging.slf4j._
import zio.logging._
import zio.macros.delegate._
import zio.nio._
import zio.system._

object ClusterExample extends zio.ManagedApp {

  val withSlf4j = enrichWith[Logging[String]](
    new Slf4jLogger.Live {

      override def formatMessage(msg: String): ZIO[Any, Nothing, String] =
        ZIO.succeed(msg)
    }
  )

  val withTcpTransport = ZIO.environment[zio.ZEnv with Logging[String]] @@
    tcp.withTcpTransport(10.seconds, 10.seconds)

  val withDiscovery =
    enrichWithM[Discovery](
      ZIO
        .foreach(Set(55557, 55558, 55559))(
          port => InetAddress.localHost.flatMap(addr => SocketAddress.inetSocketAddress(addr, port))
        )
        .orDie
        .map(addrs => Discovery.staticList(addrs.toSet))
    )

  def withK8DnsDiscovery =
    enrichWithM[Discovery](
      InetAddress
        .byName("echo-srv.zio-keeper.svc.cluster.local")
        .mapError(ex => ServiceDiscoveryError(ex.getMessage))
        .flatMap(serviceAddress => K8DnsDiscovery.k8DnsDiscovery(serviceAddress, 30.seconds, 55558))
    )

  def membership(port: Int) =
    ZManaged.environment[zio.ZEnv with Logging[String] with Transport with Discovery] @@
      SWIM.withSWIM(port)

  def myEnvironment(port: Int) =
    (ZIO.environment[zio.ZEnv] @@ withSlf4j >>>
      withTcpTransport @@ withK8DnsDiscovery).toManaged_ >>>
      membership(port)

  override def run(args: List[String]) =
    ((ZIO
      .fromOption(args.headOption)
      .flatMap(port => ZIO.effectTotal(Integer.parseInt(port)))
      .toManaged_
      .flatMap(myEnvironment) >>> program)).fold(
      _ => 1,
      _ => 0
    )

  def program =
    for {
      local <- localMember.toManaged_
      _     <- broadcast(Chunk.fromArray(local.nodeId.toString.getBytes)).ignore.toManaged_
      _ <- receive
            .foreach(
              message =>
                logger.info("receive message: " + new String(message.payload.toArray) + " from " + message.sender)
                  *> send(message.payload, message.sender).ignore
                  *> sleep(5.seconds)
            )
            .toManaged_
    } yield ()

}
