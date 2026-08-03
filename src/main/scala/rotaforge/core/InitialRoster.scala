package rotaforge.core

import rotaforge.model.Preference
import rotaforge.model.Rules
import rotaforge.model.Skills

/**
 * Builds a first feasible schedule with a greedy assignment pass.
 *
 * The solver fills every shift slot in day order. It picks the staff
 * members with the lowest workload first so that the work is spread
 * evenly from the start. The pass ignores staff preferences on purpose.
 * It only creates a feasible starting point; simulated annealing then
 * reduces the soft score.
 */
object InitialRoster {

  /** Build a greedy schedule. The result is deterministic. */
  def build(index: RosterIndex): Grid = {
    val grid = Grid.empty(index)
    val duty = grid.duty
    val rules: Rules = index.roster.rules
    val nStaff = index.nStaff
    val nDays = index.nDays
    val nShifts = index.nShifts

    val workload = Array.fill(nStaff)(0)

    val blocked: Set[(Int, Int, Int)] =
      index.roster.preferences.collect {
        case Preference(staff, day, shift, weight) if weight == 0 =>
          (index.staffIdx(staff), index.dayIdx(day), index.shiftIdx(shift))
      }.toSet

    var day = 0
    while (day < nDays) {
      var s = 0
      while (s < nShifts) {
        val required = index.headcount(s)
        var assigned = 0
        while (assigned < required) {
          var bestStaff = -1
          var bestKey = Int.MaxValue
          var st = 0
          while (st < nStaff) {
            val eligible =
              duty(st)(day) == -1 &&
                workload(st) < rules.maxShiftsPerPerson &&
                Skills.matches(index.shiftSkills(s), index.staffSkills(st)) &&
                !blocked.contains((st, day, s))
            if (eligible) {
              val key = workload(st)
              if (key < bestKey) {
                bestKey = key
                bestStaff = st
              }
            }
            st += 1
          }
          if (bestStaff == -1) {
            assigned = required
          } else {
            duty(bestStaff)(day) = s
            workload(bestStaff) += 1
            assigned += 1
          }
        }
        s += 1
      }
      day += 1
    }
    grid
  }
}
