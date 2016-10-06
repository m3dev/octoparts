import sbt.Keys._
import sbt.Tests
import scoverage.ScoverageSbtPlugin._
import scoverage.ScoverageKeys._

object Scoverage {

  val settings = Seq(
    coverageHighlighting := true,
    coverageExcludedFiles := Seq(".*(classes|src)_managed.*", ".*twirl/.*", ".*ReverseRoutes.*", ".*routes.*").mkString(";")
  )
}
