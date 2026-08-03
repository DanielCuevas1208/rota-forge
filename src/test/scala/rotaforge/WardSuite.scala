package rotaforge

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import rotaforge.core.Annealer
import rotaforge.core.Grid
import rotaforge.core.InitialRoster
import rotaforge.core.RosterIndex
import rotaforge.io.InputLoader
import rotaforge.score.Scorer
import scala.util.Random

class WardSuite extends munit.FunSuite {

  private def wardDir: Path = {
    val resource: URL = getClass.getClassLoader.getResource("ward")
    require(resource != null, "ward fixtures not found on the test classpath")
    Paths.get(resource.toURI)
  }

  private def setup(): (RosterIndex, Scorer) = {
    val roster = InputLoader.load(wardDir)
    val index = new RosterIndex(roster)
    index.requireValid()
    (index, new Scorer(index))
  }

  private def solve(index: RosterIndex, scorer: Scorer, seed: Long, iterations: Int): Grid = {
    val initial = InitialRoster.build(index)
    val rng = new Random(seed)
    new Annealer(index, scorer).run(initial, iterations, 200.0, 0.5, rng)
  }

  private def shiftIsFilledBy(index: RosterIndex, grid: Grid, shiftIdx: Int): Vector[Int] = {
    val staff = Vector.newBuilder[Int]
    var day = 0
    while (day < index.nDays) {
      var st = 0
      while (st < index.nStaff) {
        if (grid.duty(st)(day) == shiftIdx) staff += st
        st += 1
      }
      day += 1
    }
    staff.result()
  }

  test("the ward example loads multi-skilled staff") {
    val (index, _) = setup()
    assertEquals(index.staffSkills(index.staffIdx("W5")), Set("R.N.", "C.A."))
    assertEquals(index.staffSkills(index.staffIdx("W6")), Set("R.N.", "C.A."))
  }

  test("the greedy initial roster satisfies every hard rule") {
    val (index, scorer) = setup()
    val grid = InitialRoster.build(index)
    val breakdown = scorer.score(grid)
    assertEquals(breakdown.hardViolations, 0)
    assertEquals(breakdown.byKey("coverage").get.violations, 0)
  }

  test("every care shift is filled by staff with the care skill") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 11L, iterations = 40000)
    assertEquals(scorer.score(grid).hardViolations, 0)

    val careIdx = index.shiftIdx("D")
    val staff = shiftIsFilledBy(index, grid, careIdx)
    assert(staff.nonEmpty, "the care shift must have assignments")
    staff.foreach { st =>
      assert(index.staffSkills(st).contains("C.A."), s"staff ${index.staffByIndex(st)} lacks the care skill")
    }
  }

  test("every clinical shift is filled by staff with the nursing skill") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 11L, iterations = 40000)
    assertEquals(scorer.score(grid).hardViolations, 0)

    val clinicalIdx = index.shiftIdx("E")
    val staff = shiftIsFilledBy(index, grid, clinicalIdx)
    assert(staff.nonEmpty, "the clinical shift must have assignments")
    staff.foreach { st =>
      assert(index.staffSkills(st).contains("R.N."), s"staff ${index.staffByIndex(st)} lacks the nursing skill")
    }
  }

  test("the multi-skilled staff cover both roles") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 11L, iterations = 40000)

    val dual = Set(index.staffIdx("W5"), index.staffIdx("W6"))
    val careAssigned = shiftIsFilledBy(index, grid, index.shiftIdx("D")).toSet
    val clinicalAssigned = shiftIsFilledBy(index, grid, index.shiftIdx("E")).toSet
    assert(careAssigned.exists(dual.contains), "a multi-skilled member should help on care shifts")
    assert(clinicalAssigned.exists(dual.contains), "a multi-skilled member should help on clinical shifts")
  }

  test("the result is deterministic for a fixed seed") {
    val (index, scorer) = setup()
    val first = solve(index, scorer, seed = 3L, iterations = 20000)
    val second = solve(index, scorer, seed = 3L, iterations = 20000)
    assertEquals(scorer.score(first).total, scorer.score(second).total)
  }
}
