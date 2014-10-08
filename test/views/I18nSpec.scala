package views

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.scalatest.{ Matchers, FlatSpec }

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class I18nSpec extends FlatSpec with Matchers {

  case class Violation(path: Path, lineNum: Int, content: String)

  class MultibyteCharDetector extends SimpleFileVisitor[Path] {
    val violations = ArrayBuffer.empty[Violation]

    val pathMatcher = FileSystems.getDefault.getPathMatcher("glob:**/*.scala*") // Scala files and .scala.html templates

    override def visitFile(file: Path, attrs: BasicFileAttributes) = {
      if (pathMatcher.matches(file)) {
        Source.fromFile(file.toFile).getLines().zipWithIndex.foreach {
          case (line, lineNum) =>
            if (line.getBytes(StandardCharsets.UTF_8).length != line.size) {
              violations += Violation(file, lineNum, line)
            }
        }
      }
      FileVisitResult.CONTINUE
    }
  }

  "app directory" should "contain no multibyte characters" in {
    val detector = new MultibyteCharDetector
    Files.walkFileTree(Paths.get("app"), detector)

    if (detector.violations.nonEmpty) {
      fail(s"Found ${detector.violations.size} lines containing multibyte chars:\n\n${detector.violations.mkString("\n")}")
    }
  }

}
