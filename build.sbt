name := "spdf"

organization := "com.github.philcali"

version := "0.1.0"

libraryDependencies ++= Seq(
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "org.xhtmlrenderer" % "flying-saucer-pdf" % "9.0.1",
  "com.itextpdf" % "itextpdf" % "5.3.0"
)

libraryDependencies <+= (sbtVersion) { v =>
  v.split('.').toList match {
    case "0" :: "11" :: "3" :: Nil  =>
       "org.scala-sbt" %%
        "launcher-interface" %
          v % "provided"
    case _ =>
      "org.scala-sbt" %
        "launcher-interface" %
          v % "provided"
  }
}
