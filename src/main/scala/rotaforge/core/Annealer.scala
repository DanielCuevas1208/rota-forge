package rotaforge.core

import rotaforge.model.Preference
import rotaforge.score.Scorer
import scala.util.Random

/**
 * Simulated annealing over the roster.
 *
 * The schedule moves through two kinds of changes:
 *
 *   - A shift swap swaps the shift types of two staff members on one day.
 *   - A day swap swaps the working days of two staff members.
 *
 * Both moves keep the shift coverage constant, so the hard rules stay
 * satisfied while the solver reduces the soft score. The search runs
 * several times from the same starting schedule with different random
 * streams, and the solver keeps the best schedule it has seen. The
 * returned schedule therefore always has zero hard-rule violations.
 */
final class Annealer(index: RosterIndex, scorer: Scorer) {

  private val nStaff = index.nStaff
  private val nDays = index.nDays
  private val nShifts = index.nShifts

  private val blocked: Set[(Int, Int, Int)] =
    index.roster.preferences.collect {
      case Preference(staff, day, shift, weight) if weight == 0 =>
        (index.staffIdx(staff), index.dayIdx(day), index.shiftIdx(shift))
    }.toSet

  /** Run annealing and return the best schedule found. */
  def run(
      initial: Grid,
      iterations: Int,
      startTemp: Double,
      endTemp: Double,
      rng: Random
  ): Grid = {
    val baseSeed = rng.nextLong()
    val restarts = math.max(1, iterations / 50000)
    val perRun = math.max(iterations / restarts, 2000)

    var best = initial.copyOf()
    var bestEnergy = scorer.score(best).total
    var r = 0
    while (r < restarts) {
      val runRng = new Random(baseSeed + r * 7919L)
      val working = initial.copyOf()
      val candidate = runOnce(working, perRun, startTemp, endTemp, runRng)
      val candidateEnergy = scorer.score(candidate).total
      if (candidateEnergy < bestEnergy) {
        bestEnergy = candidateEnergy
        best = candidate
      }
      r += 1
    }
    best
  }

  private def runOnce(
      initial: Grid,
      iterations: Int,
      startTemp: Double,
      endTemp: Double,
      rng: Random
  ): Grid = {
    val duty = initial.duty
    var currentEnergy = scorer.score(initial).total
    var best = initial.copyOf()
    var bestEnergy = currentEnergy
    var temperature = startTemp
    val cooling = math.pow(endTemp / math.max(startTemp, 1e-9), 1.0 / math.max(iterations, 1))

    var i = 0
    while (i < iterations) {
      val move = propose(duty, rng)
      if (move != null) {
        move.apply(duty)
        val energy = scorer.score(initial).total
        val delta = energy - currentEnergy
        if (delta <= 0 || rng.nextDouble() < math.exp(-delta / temperature)) {
          currentEnergy = energy
          if (energy < bestEnergy) {
            bestEnergy = energy
            best = initial.copyOf()
          }
        } else {
          move.undo(duty)
        }
      }
      temperature = math.max(endTemp, temperature * cooling)
      i += 1
    }
    polish(best, math.max(iterations / 5, 1000), rng)
  }

  /**
   * A greedy pass that accepts only improving moves.
   * It runs after the temperature loop to reach a local optimum.
   */
  private def polish(start: Grid, steps: Int, rng: Random): Grid = {
    val duty = start.duty
    var energy = scorer.score(start).total
    var step = 0
    while (step < steps) {
      val move = propose(duty, rng)
      if (move != null) {
        move.apply(duty)
        val candidate = scorer.score(start).total
        if (candidate < energy) {
          energy = candidate
        } else {
          move.undo(duty)
        }
      }
      step += 1
    }
    start
  }

  /** A reversible change to the duty table. */
  private sealed trait Move {
    def apply(duty: Array[Array[Int]]): Unit
    def undo(duty: Array[Array[Int]]): Unit
  }

  private final case class ShiftSwap(day: Int, a: Int, b: Int, sa: Int, sb: Int) extends Move {
    def apply(duty: Array[Array[Int]]): Unit = {
      duty(a)(day) = sb
      duty(b)(day) = sa
    }
    def undo(duty: Array[Array[Int]]): Unit = {
      duty(a)(day) = sa
      duty(b)(day) = sb
    }
  }

  private final case class DaySwap(da: Int, db: Int, a: Int, b: Int, sa: Int, sb: Int)
      extends Move {
    def apply(duty: Array[Array[Int]]): Unit = {
      duty(a)(db) = sa
      duty(b)(da) = sb
      duty(a)(da) = -1
      duty(b)(db) = -1
    }
    def undo(duty: Array[Array[Int]]): Unit = {
      duty(a)(da) = sa
      duty(b)(db) = sb
      duty(a)(db) = -1
      duty(b)(da) = -1
    }
  }

  private def canWork(st: Int, day: Int, s: Int): Boolean =
    !blocked.contains((st, day, s))

  /** Pick a random valid move, or return null when none is found. */
  private def propose(duty: Array[Array[Int]], rng: Random): Move =
    if (rng.nextBoolean()) proposeShiftSwap(duty, rng) else proposeDaySwap(duty, rng)

  private def proposeShiftSwap(duty: Array[Array[Int]], rng: Random): Move = {
    val day = rng.nextInt(nDays)
    val s1 = rng.nextInt(nShifts)
    val s2 = rng.nextInt(nShifts)
    if (s1 == s2) return null
    var a = -1
    var b = -1
    var st = 0
    while (st < nStaff && (a == -1 || b == -1)) {
      if (duty(st)(day) == s1 && a == -1) a = st
      else if (duty(st)(day) == s2 && b == -1) b = st
      st += 1
    }
    if (a == -1 || b == -1) return null
    val aSkilled = index.shiftSkill(s2) == "*" || index.staffSkill(a) == index.shiftSkill(s2)
    val bSkilled = index.shiftSkill(s1) == "*" || index.staffSkill(b) == index.shiftSkill(s1)
    if (!aSkilled || !bSkilled) return null
    if (!canWork(a, day, s2) || !canWork(b, day, s1)) return null
    ShiftSwap(day, a, b, s1, s2)
  }

  private def proposeDaySwap(duty: Array[Array[Int]], rng: Random): Move = {
    val da = rng.nextInt(nDays)
    var db = rng.nextInt(nDays)
    var tries = 0
    while (db == da && tries < 8) {
      db = rng.nextInt(nDays)
      tries += 1
    }
    if (db == da) return null
    var a = -1
    var b = -1
    var st = 0
    while (st < nStaff && (a == -1 || b == -1)) {
      if (duty(st)(da) != -1 && a == -1) a = st
      else if (duty(st)(db) != -1 && b == -1) b = st
      st += 1
    }
    if (a == -1 || b == -1) return null
    val sa = duty(a)(da)
    val sb = duty(b)(db)
    if (duty(a)(db) != -1 || duty(b)(da) != -1) return null
    if (!canWork(a, db, sa) || !canWork(b, da, sb)) return null
    DaySwap(da, db, a, b, sa, sb)
  }
}
