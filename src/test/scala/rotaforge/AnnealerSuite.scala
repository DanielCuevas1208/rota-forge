package rotaforge

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import rotaforge.core.Annealer
import rotaforge.core.InitialRoster
import rotaforge.core.RosterIndex
import rotaforge.io.InputLoader
import rotaforge.score.Scorer
import scala.util.Random

class AnnealerSuite extends munit.FunSuite {

  private def clinicDir: Path = {
    val resource: URL = getClass.getClassLoader.getResource("clinic")
    require(resource != null, "clinic fixtures not found on the test classpath")
    Paths.get(resource.toURI)
  }

  private def setup(): (RosterIndex, Scorer) = {
    val roster = InputLoader.load(clinicDir)
    val index = new RosterIndex(roster)
    index.requireValid()
    (index, new Scorer(index))
  }

  private def solve(index: RosterIndex, scorer: Scorer, seed: Long, iterations: Int): rotaforge.core.Grid = {
    val initial = InitialRoster.build(index)
    val rng = new Random(seed)
    new Annealer(index, scorer).run(initial, iterations, 200.0, 0.5, rng)
  }

  test("the greedy initial roster satisfies every hard rule") {
    val (index, scorer) = setup()
    val initial = InitialRoster.build(index)
    val breakdown = scorer.score(initial)
    assertEquals(breakdown.hardViolations, 0)
    assertEquals(breakdown.byKey("coverage").get.violations, 0)
  }

  test("the clinic example needs the expected number of assignments") {
    val (index, _) = setup()
    val grid = InitialRoster.build(index)
    assertEquals(grid.assignments.size, 14 * 3 * 2)
  }

  test("annealing keeps zero hard violations") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 42L, iterations = 40000)
    val breakdown = scorer.score(grid)
    assertEquals(breakdown.hardViolations, 0)
  }

  test("annealing improves the soft score") {
    val (index, scorer) = setup()
    val initial = InitialRoster.build(index)
    val grid = solve(index, scorer, seed = 42L, iterations = 40000)
    assert(scorer.score(grid).total <= scorer.score(initial).total)
  }

  test("the final score stays in a stable range") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 42L, iterations = 40000)
    val total = scorer.score(grid).total
    assert(total >= 45, s"score below the stable range, got $total")
    assert(total <= 70, s"score above the stable range, got $total")
  }

  test("the result matches the recorded golden value") {
    val (index, scorer) = setup()
    val grid = solve(index, scorer, seed = 42L, iterations = 40000)
    assertEquals(scorer.score(grid).total, 58)
  }

  test("the result is deterministic for a fixed seed") {
    val (index, scorer) = setup()
    val first = solve(index, scorer, seed = 7L, iterations = 30000)
    val second = solve(index, scorer, seed = 7L, iterations = 30000)
    assertEquals(scorer.score(first).total, scorer.score(second).total)
  }
}
