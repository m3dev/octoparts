import sbt.Keys._
import sbt.Tests
import scoverage.ScoverageSbtPlugin._
import ScoverageKeys._

object Scoverage {

  val settings = Seq(
    coverageHighlighting := true,
    coverageExcludedFiles := ".*(classes|src)_managed.*"
  )
}