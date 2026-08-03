package rotaforge.model

import java.time.LocalDate

/** A person who works shifts in the roster. */
final case class Staff(id: String, name: String, skill: String)

/** The definition of one shift type that repeats each day. */
final case class ShiftDef(
    id: String,
    name: String,
    startMinute: Int,
    endMinute: Int,
    skill: String,
    headcount: Int
)

/**
 * One preference row for a staff member on one day.
 *
 * A weight of zero blocks the staff member from that shift (a hard rule).
 * A weight from 1 to 5 records how much the staff member wants that shift.
 */
final case class Preference(staff: String, day: LocalDate, shift: String, weight: Int)

/** The numeric rule settings that shape the schedule. */
final case class Rules(
    maxShiftsPerPerson: Int,
    maxConsecutiveDays: Int,
    minRestHours: Int,
    hardPenalty: Int
)

object Rules {
  val defaults: Rules =
    Rules(
      maxShiftsPerPerson = 8,
      maxConsecutiveDays = 5,
      minRestHours = 12,
      hardPenalty = 1000000
    )
}

/** A single assignment of a staff member to a shift on a day. */
final case class Assignment(staff: String, day: LocalDate, shift: String)

/** All the input data that defines one rostering problem. */
final case class Roster(
    days: Vector[LocalDate],
    staff: Vector[Staff],
    shifts: Vector[ShiftDef],
    preferences: Vector[Preference],
    rules: Rules
)
