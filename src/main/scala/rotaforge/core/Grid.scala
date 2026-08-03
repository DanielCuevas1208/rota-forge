package rotaforge.core

import rotaforge.model.Assignment

/**
 * One candidate schedule.
 *
 * The duty table stores the shift index for each staff member and day.
 * The value -1 means that the staff member rests that day. The structure
 * guarantees that a staff member works at most one shift per day.
 */
final class Grid(val index: RosterIndex, val duty: Array[Array[Int]]) {

  /** Count the shifts that each staff member works. */
  def workload: Array[Int] = {
    val counts = Array.fill(index.nStaff)(0)
    var st = 0
    while (st < index.nStaff) {
      var day = 0
      while (day < index.nDays) {
        if (duty(st)(day) != -1) counts(st) += 1
        day += 1
      }
      st += 1
    }
    counts
  }

  /** The assignments as a flat list, in a stable order. */
  def assignments: Vector[Assignment] = {
    val out = Vector.newBuilder[Assignment]
    var day = 0
    while (day < index.nDays) {
      var st = 0
      while (st < index.nStaff) {
        val s = duty(st)(day)
        if (s != -1)
          out += Assignment(
            staff = index.staffByIndex(st),
            day = index.daysByIndex(day),
            shift = index.shiftsByIndex(s).id
          )
        st += 1
      }
      day += 1
    }
    out.result()
  }

  def copyOf(): Grid = new Grid(index, duty.map(_.clone()))
}

object Grid {

  def empty(index: RosterIndex): Grid = {
    val duty = Array.fill(index.nStaff)(Array.fill(index.nDays)(-1))
    new Grid(index, duty)
  }

  /** Build a grid from an explicit assignment list. */
  def fromAssignments(index: RosterIndex, assignments: Vector[Assignment]): Grid = {
    val grid = empty(index)
    assignments.foreach { a =>
      val st = index.staffIdx.getOrElse(
        a.staff,
        throw new IllegalArgumentException(s"unknown staff '${a.staff}'")
      )
      val day = index.dayIdx.getOrElse(
        a.day,
        throw new IllegalArgumentException(s"unknown day '${a.day}'")
      )
      val s = index.shiftIdx.getOrElse(
        a.shift,
        throw new IllegalArgumentException(s"unknown shift '${a.shift}'")
      )
      require(grid.duty(st)(day) == -1, s"staff '${a.staff}' works twice on ${a.day}")
      grid.duty(st)(day) = s
    }
    grid
  }
}
