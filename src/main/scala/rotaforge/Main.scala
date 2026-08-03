package rotaforge

import java.nio.file.Path
import java.nio.file.Paths
import rotaforge.score.ConstraintKind

/** Command line entry point for Rota Forge. */
object Main {

  private val version = "0.1.0"

  private val usage: String =
    s"""Rota Forge $version - staff rostering solver
       |
       |Usage: rota-forge <inputDir> [outputDir] [options]
       |
       |  inputDir    directory that holds staff.csv, days.csv, shifts.csv,
       |              preferences.csv, and optional rules.csv
       |  outputDir   directory for roster.csv and report.html (default: out)
       |
       |Options:
       |  --seed <n>         random seed (default: 42)
       |  --iterations <n>   annealing iterations (default: 200000)
       |  --startTemp <t>    starting temperature (default: 200)
       |  --endTemp <t>      final temperature (default: 0.5)
       |  --help             show this help
       |""".stripMargin

  def main(args: Array[String]): Unit = {
    if (args.isEmpty || args.contains("--help") || args.contains("-h")) {
      println(usage)
      sys.exit(if (args.isEmpty) 2 else 0)
    }

    val result = parseArgs(args.toVector)
    result match {
      case Left(message) =>
        System.err.println(s"error: $message")
        System.err.println(usage)
        sys.exit(2)
      case Right(parsed) =>
        run(parsed)
    }
  }

  private final case class Parsed(
      inputDir: Path,
      outputDir: Path,
      config: SolverConfig
  )

  private def parseArgs(args: Vector[String]): Either[String, Parsed] = {
    val positional = scala.collection.mutable.ArrayBuffer[String]()
    var seed = 42L
    var iterations = 200000
    var startTemp = 200.0
    var endTemp = 0.5

    def nextValue(i: Int, name: String): Either[String, String] =
      if (i + 1 >= args.length) Left(s"missing value for $name")
      else Right(args(i + 1))

    var i = 0
    while (i < args.length) {
      args(i) match {
        case "--seed" =>
          nextValue(i, "--seed") match {
            case Left(m) => return Left(m)
            case Right(v) =>
              seed = v.toLong
              i += 2
          }
        case "--iterations" =>
          nextValue(i, "--iterations") match {
            case Left(m) => return Left(m)
            case Right(v) =>
              iterations = v.toInt
              i += 2
          }
        case "--startTemp" =>
          nextValue(i, "--startTemp") match {
            case Left(m) => return Left(m)
            case Right(v) =>
              startTemp = v.toDouble
              i += 2
          }
        case "--endTemp" =>
          nextValue(i, "--endTemp") match {
            case Left(m) => return Left(m)
            case Right(v) =>
              endTemp = v.toDouble
              i += 2
          }
        case opt if opt.startsWith("--") =>
          return Left(s"unknown option '$opt'")
        case value =>
          positional += value
          i += 1
      }
    }

    if (positional.isEmpty) return Left("no input directory given")
    if (positional.size > 2) return Left("too many positional arguments")
    val inputDir = Paths.get(positional(0)).toAbsolutePath.normalize()
    if (!java.nio.file.Files.isDirectory(inputDir))
      return Left(s"input directory not found: $inputDir")
    val outputDir = positional
      .lift(1)
      .map(p => Paths.get(p).toAbsolutePath.normalize())
      .getOrElse(Paths.get("out").toAbsolutePath.normalize())

    Right(Parsed(inputDir, outputDir, SolverConfig(seed, iterations, startTemp, endTemp)))
  }

  private def run(parsed: Parsed): Unit = {
    println(s"Rota Forge $version")
    println(s"Input:   ${parsed.inputDir}")
    println(s"Output:  ${parsed.outputDir}")
    println(s"Seed:    ${parsed.config.seed}")
    println(s"Iterations: ${parsed.config.iterations}")
    println()

    val result = Solver.solve(parsed.inputDir, parsed.outputDir, parsed.config)

    println("Roster")
    println(s"  Staff:     ${result.roster.staff.size}")
    println(s"  Days:      ${result.roster.days.size} (${result.roster.days.head} to ${result.roster.days.last})")
    println(s"  Shifts:    ${result.roster.shifts.map(_.id).mkString(", ")}")
    println(s"  Assignments: ${result.grid.assignments.size}")
    println()

    println("Score")
    println(s"  Hard violations: ${result.score.hardViolations}")
    println(s"  Total score:     ${result.score.total}  (initial ${result.initialScore.total})")
    println()

    println("Constraint results")
    val width = result.score.results.map(_.name.length).max
    result.score.results.foreach { r =>
      val kind = if (r.kind == ConstraintKind.Hard) "HARD" else "soft"
      val name = r.name.padTo(width, ' ')
      println(
        s"  $name $kind  weight ${r.weight}  violations ${r.violations}  penalty ${r.penalty}"
      )
    }
    println()
    println(s"Wrote ${result.rosterCsv}")
    println(s"Wrote ${result.reportHtml}")
  }
}
