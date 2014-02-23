package service

import akka.actor.{ActorSystem, Actor, Props}
import akka.event.Logging
import spray.can.Http
import spray.http.{HttpResponse, HttpRequest}
import scala.reflect.io.{NoAbstractFile, VirtualFile, AbstractFile, VirtualDirectory}
import scala.tools.nsc.{Global, Settings}
import java.net.URLClassLoader
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.io.VirtualFile
import scala.concurrent.Await
import java.io._
import spray.http.HttpRequest
import scala.Some
import spray.http.HttpResponse

object Main{
  implicit val system = ActorSystem()
  def main(args: Array[String]): Unit = {
    import akka.io.IO
    import spray.can.Http
    implicit val timeout = akka.util.Timeout(1000)
    import akka.pattern.ask
    val actor = system.actorOf(Props[MyActor])
    val res = IO(Http) ? Http.Bind(actor, interface = "localhost", port = 8080)
    import concurrent.duration._
    Await.result(res, 1 second)
  }
}
class MyActor extends Actor {
  def receive = {
    case Http.Connected(_, _) => sender ! Http.Register(self)
    case HttpRequest(method, uri, headers, entity, protocol) =>


      lazy val settings = new Settings

      val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
      val entries = loader.getURLs map(_.getPath)
      val vd = new VirtualDirectory("(memory)", None)
      settings.outputDirs.setSingleOutput(vd)
      settings.classpath.value = ClassPath.join(entries: _*)

      val compiler = new Global(settings, new ConsoleReporter(settings)){
        override lazy val plugins = List[Plugin](new scala.scalajs.compiler.ScalaJSPlugin(this))
      }

      val singleFile = new VirtualFile("(memory)")

    val output = singleFile.output
      output.write(entity.data.toByteArray)
      output.close()

      val run = new compiler.Run()
      run.compileFiles(List(singleFile))

      println("--------------------VD " + vd.iterator.size)
      vd.iterator.foreach(println)

      sender ! HttpResponse(entity="hello" + entity.data.asString)
    case x => println("UNKNOWN " + x)
  }
}
