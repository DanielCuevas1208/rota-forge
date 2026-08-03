package rotaforge.score

import rotaforge.core.Grid
import rotaforge.core.RosterIndex
import rotaforge.model.Preference
import rotaforge.model.Rules

/**
 * Scores a schedule against hard and soft constraints.
 *
 * Each constraint produces a result with a violation count and a penalty.
 * The annealing loop drives the total penalty down.
 */
final class Scorer(index: RosterIndex) {

  private val rules: Rules = index.roster.rules
  private val nStaff = index.nStaff
  private val nDays = index.nDays
  private val nShifts = index.nShifts

  private val blocked: Set[(Int, Int, Int)] =
    index.roster.preferences.collect {
      case Preference(staff, day, shift, weight) if weight == 0 =>
        (index.staffIdx(staff), index.dayIdx(day), index.shiftIdx(shift))
    }.toSet

  /** Score one schedule and return the full breakdown. */
  def score(grid: Grid): ScoreBreakdown = {
    val duty = grid.duty
    ScoreBreakdown(
      Vector(
        coverage(duty),
        skillMatch(duty),
        availability(duty),
        maxShifts(duty),
        restBetweenShifts(duty),
        consecutiveDays(duty),
        workloadFairness(duty),
        preference(duty),
        weekendFairness(duty)
      )
    )
  }

  private def coverage(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    var day = 0
    while (day < nDays) {
      var s = 0
      while (s < nShifts) {
        var count = 0
        var st = 0
        while (st < nStaff) {
          if (duty(st)(day) == s) count += 1
          st += 1
        }
        if (count != index.headcount(s))
          violations += math.abs(count - index.headcount(s))
        s += 1
      }
      day += 1
    }
    hardResult("coverage", "Shift coverage", rules.hardPenalty, violations)
  }

  private def skillMatch(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    var day = 0
    while (day < nDays) {
      var st = 0
      while (st < nStaff) {
        val s = duty(st)(day)
        if (s != -1) {
          val required = index.shiftSkill(s)
          if (required != "*" && required != index.staffSkill(st))
            violations += 1
        }
        st += 1
      }
      day += 1
    }
    hardResult("skill-match", "Skill match", rules.hardPenalty, violations)
  }

  private def availability(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    var day = 0
    while (day < nDays) {
      var st = 0
      while (st < nStaff) {
        val s = duty(st)(day)
        if (s != -1 && blocked.contains((st, day, s)))
          violations += 1
        st += 1
      }
      day += 1
    }
    hardResult("availability", "Availability", rules.hardPenalty, violations)
  }

  private def maxShifts(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    var st = 0
    while (st < nStaff) {
      var count = 0
      var day = 0
      while (day < nDays) {
        if (duty(st)(day) != -1) count += 1
        day += 1
      }
      if (count > rules.maxShiftsPerPerson)
        violations += count - rules.maxShiftsPerPerson
      st += 1
    }
    hardResult("max-shifts", "Max shifts per person", rules.hardPenalty, violations)
  }

  private def restBetweenShifts(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    val minimum = rules.minRestHours * 60
    var st = 0
    while (st < nStaff) {
      var prevDay = -1
      var prevEnd = 0
      var day = 0
      while (day < nDays) {
        val s = duty(st)(day)
        if (s != -1) {
          if (prevDay != -1 && day == prevDay + 1) {
            val gap = index.startMinute(s) + 24 * 60 - prevEnd
            if (gap < minimum) violations += 1
          }
          prevDay = day
          prevEnd = index.endMinute(s)
        }
        day += 1
      }
      st += 1
    }
    softResult("rest", "Rest between shifts", 5, violations)
  }

  private def consecutiveDays(duty: Array[Array[Int]]): ConstraintResult = {
    var violations = 0
    var st = 0
    while (st < nStaff) {
      var run = 0
      var day = 0
      while (day < nDays) {
        if (duty(st)(day) != -1) {
          run += 1
          if (run > rules.maxConsecutiveDays) violations += 1
        } else {
          run = 0
        }
        day += 1
      }
      st += 1
    }
    softResult("consecutive-days", "Max consecutive days", 4, violations)
  }

  private def workloadFairness(duty: Array[Array[Int]]): ConstraintResult = {
    if (nStaff == 0) return softResult("fairness", "Workload fairness", 3, 0)
    val counts = Array.fill(nStaff)(0)
    var st = 0
    while (st < nStaff) {
      var day = 0
      while (day < nDays) {
        if (duty(st)(day) != -1) counts(st) += 1
        day += 1
      }
      st += 1
    }
    val total = counts.sum
    val target = Math.round(total.toDouble / nStaff).toInt
    val violations = counts.map(c => math.abs(c - target)).sum
    softResult("fairness", "Workload fairness", 3, violations)
  }

  private def preference(duty: Array[Array[Int]]): ConstraintResult = {
    var lost = 0
    index.roster.preferences.foreach { p =>
      if (p.weight > 0) {
        val st = index.staffIdx(p.staff)
        val day = index.dayIdx(p.day)
        val s = index.shiftIdx(p.shift)
        if (duty(st)(day) != s) lost += p.weight
      }
    }
    softResult("preference", "Staff preferences", 1, lost)
  }

  private def weekendFairness(duty: Array[Array[Int]]): ConstraintResult = {
    if (nStaff == 0) return softResult("weekend", "Weekend balance", 3, 0)
    val counts = Array.fill(nStaff)(0)
    var st = 0
    while (st < nStaff) {
      var day = 0
      while (day < nDays) {
        if (index.isWeekend(day) && duty(st)(day) != -1) counts(st) += 1
        day += 1
      }
      st += 1
    }
    val total = counts.sum
    val target = Math.round(total.toDouble / nStaff).toInt
    val violations = counts.map(c => math.abs(c - target)).sum
    softResult("weekend", "Weekend balance", 3, violations)
  }

  private def hardResult(key: String, name: String, weight: Int, violations: Int): ConstraintResult =
    ConstraintResult(key, name, ConstraintKind.Hard, weight, violations, violations * weight)

  private def softResult(key: String, name: String, weight: Int, violations: Int): ConstraintResult =
    ConstraintResult(key, name, ConstraintKind.Soft, weight, violations, violations * weight)
}
