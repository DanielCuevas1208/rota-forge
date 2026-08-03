package rotaforge

import java.time.LocalDate
import rotaforge.core.Grid
import rotaforge.core.RosterIndex
import rotaforge.model.Roster
import rotaforge.model.Rules
import rotaforge.model.ShiftDef
import rotaforge.model.Staff

/** Small reusable fixtures for the unit tests. */
object TestRosters {

  val days: Vector[LocalDate] = Vector(
    LocalDate.of(2026, 5, 4),
    LocalDate.of(2026, 5, 5),
    LocalDate.of(2026, 5, 9)
  )

  val day: Map[LocalDate, Int] = days.zipWithIndex.toMap

  val staff: Vector[Staff] = Vector(
    Staff("A", "Ana", "R.N."),
    Staff("B", "Boris", "R.N."),
    Staff("C", "Clara", "L.P.N."),
    Staff("D", "Daniel", "R.N.")
  )

  val shifts: Vector[ShiftDef] = Vector(
    ShiftDef("D", "Day Shift", 7 * 60, 19 * 60, "R.N.", 1),
    ShiftDef("N", "Night Shift", 21 * 60, 21 * 60 + 12 * 60, "R.N.", 1)
  )

  val rules: Rules = Rules(
    maxShiftsPerPerson = 2,
    maxConsecutiveDays = 2,
    minRestHours = 12,
    hardPenalty = 1000
  )

  /** Build a roster from the shared fixture, adding preferences. */
  def roster(
      preferences: Vector[rotaforge.model.Preference] = Vector.empty,
      rulesOverride: Rules = rules,
      extraStaff: Vector[Staff] = Vector.empty,
      extraShifts: Vector[ShiftDef] = Vector.empty
  ): Roster =
    Roster(days, staff ++ extraStaff, shifts ++ extraShifts, preferences, rulesOverride)

  def index(roster: Roster): RosterIndex = new RosterIndex(roster)

  /** Build a grid where each staff member has one shift per day. */
  def grid(roster: Roster, plan: Map[String, Map[LocalDate, String]]): Grid = {
    val idx = index(roster)
    val g = Grid.empty(idx)
    plan.foreach { case (staffId, byDay) =>
      val st = idx.staffIdx(staffId)
      byDay.foreach { case (day, shiftId) =>
        val dayIndex = idx.dayIdx(day)
        val s = idx.shiftIdx(shiftId)
        g.duty(st)(dayIndex) = s
      }
    }
    g
  }
}
