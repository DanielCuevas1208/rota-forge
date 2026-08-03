package rotaforge

import rotaforge.model.Preference
import rotaforge.score.Scorer

class ScorerSuite extends munit.FunSuite {

  private val d0 = TestRosters.days(0)
  private val d1 = TestRosters.days(1)
  private val d2 = TestRosters.days(2)

  private def score(grid: rotaforge.core.Grid, roster: rotaforge.model.Roster): rotaforge.score.ScoreBreakdown = {
    new Scorer(TestRosters.index(roster)).score(grid)
  }

  test("coverage counts missing and extra staff") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("coverage").get.violations, 5)
  }

  test("skill mismatch is a hard violation") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(roster, Map("C" -> Map(d0 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("skill-match").get.violations, 1)
    assert(result.hardViolations > 0)
  }

  private def dualRoster: rotaforge.model.Roster =
    TestRosters.roster(
      extraStaff = Vector(
        rotaforge.model.Staff("E", "Elena", "R.N.;L.P.N."),
        rotaforge.model.Staff("F", "Feng", "L.P.N.")
      ),
      extraShifts = Vector(
        rotaforge.model.ShiftDef("A", "Care Assist", 8 * 60, 20 * 60, "L.P.N.", 1)
      )
    )

  test("a multi-skilled staff member fills shifts that need either skill") {
    val roster = dualRoster
    val grid = TestRosters.grid(
      roster,
      Map(
        "A" -> Map(d0 -> "D"),
        "E" -> Map(d1 -> "D", d2 -> "A")
      )
    )
    val result = score(grid, roster)
    assertEquals(result.byKey("skill-match").get.violations, 0)
  }

  test("a staff member without the skill still violates on a multi-skill shift") {
    val roster = dualRoster
    val grid = TestRosters.grid(roster, Map("F" -> Map(d0 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("skill-match").get.violations, 1)
  }

  test("a zero-weight preference blocks an assignment") {
    val roster = TestRosters.roster(Vector(Preference("A", d0, "D", 0)))
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("availability").get.violations, 1)
  }

  test("a positive preference rewards the exact shift") {
    val roster = TestRosters.roster(Vector(Preference("A", d0, "D", 5)))
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("preference").get.violations, 0)
  }

  test("an unmet preference loses its weight") {
    val roster = TestRosters.roster(Vector(Preference("A", d0, "D", 5)))
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "N")))
    val result = score(grid, roster)
    assertEquals(result.byKey("preference").get.violations, 5)
  }

  test("max shifts counts the shifts above the limit") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(
      roster,
      Map("A" -> Map(d0 -> "D", d1 -> "D", d2 -> "D"))
    )
    val result = score(grid, roster)
    assertEquals(result.byKey("max-shifts").get.violations, 1)
  }

  test("a night shift before a day shift violates rest") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "N", d1 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("rest").get.violations, 1)
  }

  test("rest is fine when a day of rest separates two shifts") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "N", d2 -> "D")))
    val result = score(grid, roster)
    assertEquals(result.byKey("rest").get.violations, 0)
  }

  test("consecutive days counts the days above the limit") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(
      roster,
      Map("A" -> Map(d0 -> "D", d1 -> "D", d2 -> "D"))
    )
    val result = score(grid, roster)
    assertEquals(result.byKey("consecutive-days").get.violations, 1)
  }

  test("workload fairness measures the spread around the average") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(
      roster,
      Map(
        "A" -> Map(d0 -> "D", d1 -> "D", d2 -> "D"),
        "B" -> Map(d0 -> "N")
      )
    )
    val result = score(grid, roster)
    assertEquals(result.byKey("fairness").get.violations, 4)
  }

  test("weekend balance measures the spread on weekend days") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(
      roster,
      Map(
        "A" -> Map(d2 -> "D"),
        "B" -> Map(d2 -> "N")
      )
    )
    val result = score(grid, roster)
    assertEquals(result.byKey("weekend").get.violations, 2)
  }

  test("an empty plan reports every hard slot as missing") {
    val roster = TestRosters.roster()
    val grid = TestRosters.grid(roster, Map.empty)
    val result = score(grid, roster)
    assertEquals(result.byKey("coverage").get.violations, 6)
    assert(result.hardViolations > 0)
  }

  test("total sums hard and soft penalties") {
    val roster = TestRosters.roster(Vector(Preference("A", d0, "D", 3)))
    val grid = TestRosters.grid(roster, Map("A" -> Map(d0 -> "N")))
    val result = score(grid, roster)
    assertEquals(result.byKey("coverage").get.violations, 5)
    val expected = result.results.map(_.penalty).sum
    assertEquals(result.total, expected)
    assert(result.total >= result.softPenalty)
  }
}
