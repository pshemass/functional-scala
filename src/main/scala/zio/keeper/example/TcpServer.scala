package zio.keeper.example

import zio._
import zio.duration._
import zio.keeper.transport._
import zio.macros.delegate._
import zio.logging.Logging
import zio.logging.slf4j._
import zio.console._
import zio.nio._
import zio.clock._

object TcpServer extends zio.App {

  val withSlf4j = enrichWith[Logging[String]](
    new Slf4jLogger.Live {

      override def formatMessage(msg: String): ZIO[Any, Nothing, String] =
        ZIO.succeed(msg)
    }
  )

  val withTcpTransport = ZIO.environment[zio.ZEnv with Logging[String]] @@
    tcp.withTcpTransport(10.seconds, 10.seconds)

  val dependencies =
    ZIO.environment[zio.ZEnv] @@ withSlf4j >>> withTcpTransport

  override def run(args: List[String]) =
    (dependencies >>> program)
      .fold(
        _ => 1,
        _ => 0
      )

  def program =
    for {
      localHost <- InetAddress.localHost.orDie
      publicAddress <- SocketAddress
                        .inetSocketAddress(localHost, 8010)
                        .orDie
      logging <- ZIO.environment[Logging[String]]
      handler = (channel: ChannelOut) => {
        for {
          data <- channel.read
          _    <- logger.info("receive message: " + new String(data.toArray))
          _    <- channel.send(data)
        } yield ()
      }.forever
        .catchAll(ex => logger.error("error: " + ex.msg))
        .provide(logging)
      _ <- logger.info("public address: " + publicAddress.toString())
      _ <- bind(publicAddress)(handler).useForever
    } yield ()

}

object TcpClient extends zio.App {

  val withSlf4j = enrichWith[Logging[String]](
    new Slf4jLogger.Live {

      override def formatMessage(msg: String): ZIO[Any, Nothing, String] =
        ZIO.succeed(msg)
    }
  )

  val withTcpTransport = ZIO.environment[zio.ZEnv with Logging[String]] @@
    tcp.withTcpTransport(10.seconds, 10.seconds)

  val dependencies =
    ZIO.environment[zio.ZEnv] @@ withSlf4j >>> withTcpTransport

  override def run(args: List[String]) =
    dependencies >>>
      program.fold(
        _ => 1,
        _ => 0
      )

  def program =
    for {
      localHost <- InetAddress.localHost.orDie
      publicAddress <- SocketAddress
                        .inetSocketAddress(localHost, 8010)
                        .orDie
      _ <- logger.info("connect to address: " + publicAddress.toString())
      _ <- connect(publicAddress).use(
            connection =>
              currentDateTime
                .map(date => "message from client " + date)
                .flatMap(msg => connection.send(Chunk.fromArray(msg.getBytes)))
                .repeat(Schedule.recurs(100))
          )
    } yield ()
}
