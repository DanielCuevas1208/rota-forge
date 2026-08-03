package rotaforge.core

import java.time.LocalDate
import rotaforge.model.Roster
import rotaforge.model.ShiftDef

/**
 * Compact integer indexes over a roster.
 *
 * The solver works with integer indexes because they keep the hot loops
 * allocation-free. Each staff member, day, and shift gets an index.
 */
final class RosterIndex(val roster: Roster) {

  val staffIdx: Map[String, Int] = roster.staff.map(_.id).zipWithIndex.toMap
  val dayIdx: Map[LocalDate, Int] = roster.days.zipWithIndex.toMap
  val shiftIdx: Map[String, Int] = roster.shifts.map(_.id).zipWithIndex.toMap

  val nStaff: Int = roster.staff.size
  val nDays: Int = roster.days.size
  val nShifts: Int = roster.shifts.size

  val staffByIndex: Array[String] = roster.staff.map(_.id).toArray
  val staffName: Array[String] = roster.staff.map(_.name).toArray
  val staffSkill: Array[String] = roster.staff.map(_.skill).toArray

  val daysByIndex: Array[LocalDate] = roster.days.toArray
  val isWeekend: Array[Boolean] =
    roster.days.map(d => d.getDayOfWeek.getValue >= 6).toArray

  val shiftsByIndex: Array[ShiftDef] = roster.shifts.toArray
  val shiftName: Array[String] = roster.shifts.map(_.name).toArray
  val startMinute: Array[Int] = roster.shifts.map(_.startMinute).toArray
  val endMinute: Array[Int] = roster.shifts.map(_.endMinute).toArray
  val shiftSkill: Array[String] = roster.shifts.map(_.skill).toArray
  val headcount: Array[Int] = roster.shifts.map(_.headcount).toArray

  def requireValid(): Unit = {
    require(nStaff > 0, "roster has no staff")
    require(nDays > 0, "roster has no days")
    require(nShifts > 0, "roster has no shifts")
    require(staffIdx.size == nStaff, "duplicate staff id")
    require(dayIdx.size == nDays, "duplicate day")
    require(shiftIdx.size == nShifts, "duplicate shift id")
  }
}
