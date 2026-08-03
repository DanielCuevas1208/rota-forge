package rotaforge

import java.nio.file.Paths
import rotaforge.core.InitialRoster
import rotaforge.core.RosterIndex
import rotaforge.io.InputLoader
import rotaforge.report.HtmlReport
import rotaforge.report.ReportMeta
import rotaforge.score.Scorer

class HtmlReportSuite extends munit.FunSuite {

  private def rendered: String = {
    val dir = Paths.get(getClass.getClassLoader.getResource("clinic").toURI)
    val roster = InputLoader.load(dir)
    val index = new RosterIndex(roster)
    val grid = InitialRoster.build(index)
    val score = new Scorer(index).score(grid)
    val meta = ReportMeta("Maple Clinic", dir.toString, seed = 42L, iterations = 100, startTemp = 200.0, endTemp = 0.5)
    HtmlReport.render(grid, score, meta)
  }

  test("the report names every constraint") {
    val html = rendered
    val names = Vector(
      "Shift coverage",
      "Skill match",
      "Availability",
      "Max shifts per person",
      "Rest between shifts",
      "Max consecutive days",
      "Workload fairness",
      "Staff preferences",
      "Weekend balance"
    )
    names.foreach { name =>
      assert(html.contains(name), s"report must mention '$name'")
    }
  }

  test("the report marks every hard rule as passing") {
    val html = rendered
    val passCount = "Pass".r.findAllIn(html).length
    assert(passCount >= 4, s"expected at least 4 passing hard rules, saw $passCount")
    assert(!html.contains(">Fail<"), "no hard rule may fail")
  }

  test("the report shows the roster grid and its headings") {
    val html = rendered
    assert(html.contains("Roster grid"))
    assert(html.contains("Staff"))
    assert(html.contains("weekend"))
    assert(html.contains("Ana Silva"))
    assert(html.contains("Ian O'Reilly"))
  }

  test("the report is a complete HTML document") {
    val html = rendered
    assert(html.startsWith("<!doctype html>"))
    assert(html.contains("</html>"))
    assert(html.contains("<style>"))
  }

  test("the report shows the score summary") {
    val html = rendered
    assert(html.contains("Hard violations"))
    assert(html.contains("Total score"))
    assert(html.contains("Soft penalty"))
  }
}
