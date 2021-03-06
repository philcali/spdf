package com.github.philcali

import xsbti.{ AppMain, AppConfiguration }

import org.xhtmlrenderer.pdf.ITextRenderer
import org.xhtmlrenderer.pdf.ITextUserAgent
import org.xhtmlrenderer.pdf.ITextOutputDevice

import java.io.{
  File,
  FileInputStream,
  FileOutputStream => FOS
}

import java.net.URL
import java.net.URI

import util.control.Exception.allCatch

import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import xml.parsing.NoBindingFactoryAdapter
import org.xml.sax.InputSource

object Main {
  val Output = """\s*-o (.*\.pdf)\s*""".r

  val Input = """\s*-i\s*""".r

  def printHelp() {
    println("""
  spdf v0.1.0, Copyright Philip Cali

  sdpf -h [file|folder|url...] -i [-o out.pdf]

  -h   Prints this help
  -i   Piped input for objects (files, folders, urls)
  -o   Explicitly filename output

  ex:
    find book | grep "html$" | spdf cover.html -i > book.pdf
    spdf < flyer.html > flyer.pdf
    curl www.google.com | spdf > out.pdf
    cat flyer.html | spdf -o flyer.pdf
""")
  }

  def main(args: Array[String]) {
    run(args)
  }

  def run(args: Array[String]): Int = {
    val full = args.mkString(" ")
    val out = Output.findFirstMatchIn(full).map(_.group(1))

    if (args.contains("-h")) {
      printHelp()
    } else {
      val extra = if (args.contains("-i")) parseExtra() else Nil
      val cleaned = Input.replaceAllIn(Output.replaceAllIn(full, ""), "")
      val inputs = cleaned.split(" ") ++ extra
      process(if (inputs.isEmpty) None else Some(inputs), out)
    }
    0
  }

  def parseExtra() = {
    val reg = """\s+"""

    // Efficiently builds List from stdin
    def pump(ls: List[String]): List[String] = Console.readLine match {
      case line: String => pump((line.trim.split(reg) :\ ls)(_ :: _))
      case _ => ls.reverse
    }

    pump(Nil)
  }

  def isValid(src: Any) = allCatch.opt(src match {
    case url: URL => url.openConnection().getInputStream().close(); true
    case file: File => file.exists()
  }).getOrElse(false)

  def identify(str: String) = str match {
    case url if url.contains("://") => new URL(url)
    case file => new File(file)
  }

  def stream(src: Any) = src match {
    case url: URL => url.openStream()
    case file: File => new FileInputStream(file)
    case _ => throw new Exception("Unexpected source")
  }

  def toURL(src: Any) = src match {
    case url: URL => url.toString
    case file: File => file.toURI.toURL.toString
  }

  def buildDocument(is: java.io.InputStream) = {
      val adapter = new NoBindingFactoryAdapter
      val factory = new SAXFactoryImpl
      adapter.loadXML(new InputSource(is), factory.newSAXParser)
  }

  def flatten(src: Any): List[_] = src match {
    case dir: File if dir.isDirectory =>
      dir.listFiles.filter(_.isFile).sortWith(_.getName < _.getName).toList
    case other => List(other)
  }

  def paginate(src: Any, renderer: ITextRenderer) {
    val is = stream(src)

    try {
      val url = toURL(src)

      resetResourceLoader(renderer, url)
      renderer.setDocumentFromString(buildDocument(is).toString, url)
      renderer.layout()
      renderer.writeNextDocument()
    } finally {
      is.close()
    }
  }

  def resetResourceLoader(renderer: ITextRenderer, uri: String) {
    val callback = new ResourceLoader(renderer.getOutputDevice, uri)
    callback.setSharedContext(renderer.getSharedContext)
    renderer.getSharedContext.setUserAgentCallback(callback)
  }

  def process(in: Option[Array[String]], out: Option[String]) = {
    val os = out.map(new File(_)).map(new FOS(_)).getOrElse(Console.out)

    try {
      val renderer = new ITextRenderer

      val ids = in.map(_.map(identify).flatMap(flatten).filter(isValid)).filter(!_.isEmpty)

      val initial = ids.map(_.head).map(stream).getOrElse(System.in)
      val url = ids.map(_.head).map(toURL).getOrElse(toURL(new File(".")))

      resetResourceLoader(renderer, url)
      renderer.setDocumentFromString(buildDocument(initial).toString, url)
      renderer.layout()
      renderer.createPDF(os, false)

      initial.close()

      // Write additional pages
      ids.map(_.tail.foreach(paginate(_, renderer)))

      renderer.finishPDF()

    } catch {
      case cast: ClassCastException =>
        val issue = "http://code.google.com/p/flying-saucer/issues/detail?id=144"
        println(
"""[ERROR]: Flying saucer did not like this grid layout.
  Please vote on this issue: %s""" format issue
        )
    } finally {
      os.close()
    }
  }
}

class ResourceLoader(output: ITextOutputDevice, base: String) extends ITextUserAgent(output) {
  override protected def resolveAndOpenStream(uri: String) = {
    if (base.startsWith("file:") && !(new File(new URI(uri)).exists)) {
      val str = base.split("/").dropRight(1).mkString("/").replace("file:", "")
      val uriStr = uri.replace("file:", "")

      val newFile = new File(str, uriStr)

      super.resolveAndOpenStream(newFile.toURI.toURL.toString)
    } else {
      super.resolveAndOpenStream(uri)
    }
  }
}

class Script extends AppMain {
  def run(conf: AppConfiguration) = Exit(Main.run(conf.arguments))
}

case class Exit(val code: Int) extends xsbti.Exit
