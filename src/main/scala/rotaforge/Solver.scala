package rotaforge

import java.nio.file.Path
import rotaforge.core.Annealer
import rotaforge.core.Grid
import rotaforge.core.InitialRoster
import rotaforge.core.RosterIndex
import rotaforge.io.InputLoader
import rotaforge.io.RosterWriter
import rotaforge.model.Roster
import rotaforge.report.HtmlReport
import rotaforge.report.ReportMeta
import rotaforge.score.ScoreBreakdown
import rotaforge.score.Scorer
import scala.util.Random

/** The settings that control one solver run. */
final case class SolverConfig(
    seed: Long = 42L,
    iterations: Int = 200000,
    startTemp: Double = 200.0,
    endTemp: Double = 0.5
)

/** The outcome of one solver run. */
final case class SolveResult(
    roster: Roster,
    index: RosterIndex,
    grid: Grid,
    score: ScoreBreakdown,
    initialScore: ScoreBreakdown,
    rosterCsv: Path,
    reportHtml: Path
)

/** Runs the full pipeline: load, build, anneal, and write results. */
object Solver {

  def solve(inputDir: Path, outputDir: Path, config: SolverConfig): SolveResult = {
    val roster = InputLoader.load(inputDir)
    val index = new RosterIndex(roster)
    index.requireValid()

    val scorer = new Scorer(index)
    val initial = InitialRoster.build(index)
    val initialScore = scorer.score(initial)

    val rng = new Random(config.seed)
    val annealer = new Annealer(index, scorer)
    val finalGrid = annealer.run(initial, config.iterations, config.startTemp, config.endTemp, rng)
    val finalScore = scorer.score(finalGrid)

    val rosterCsv = RosterWriter.write(outputDir, finalGrid)
    val meta = ReportMeta(
      instanceName = instanceName(inputDir),
      inputDir = inputDir.toString,
      seed = config.seed,
      iterations = config.iterations,
      startTemp = config.startTemp,
      endTemp = config.endTemp
    )
    val reportHtml = HtmlReport.write(outputDir, finalGrid, finalScore, meta)

    SolveResult(
      roster = roster,
      index = index,
      grid = finalGrid,
      score = finalScore,
      initialScore = initialScore,
      rosterCsv = rosterCsv,
      reportHtml = reportHtml
    )
  }

  private def instanceName(inputDir: Path): String = {
    val name = Option(inputDir.getFileName).map(_.toString).getOrElse("")
    name.replaceAll("[_-]", " ").split(" ").filter(_.nonEmpty).map { w =>
      w.head.toUpper.toString + w.drop(1)
    }.mkString(" ")
  }
}
