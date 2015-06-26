import AssemblyKeys._

assemblySettings

jarName in assembly := "wilson.jar"

mainClass in assembly := Some("com.me2ds.wilson.Wilson")

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case x if Assembly.isConfigFile(x) =>
      MergeStrategy.concat
    case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case PathList("META-INF", xs@_*) =>
      (xs map {
        _.toLowerCase
      }) match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: xs =>
          MergeStrategy.discard
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) | ("spring.factories" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case "spring.tooling" :: xs => MergeStrategy.discard
        case _ => MergeStrategy.deduplicate
      }
    case _ => MergeStrategy.deduplicate
  }
}