package rotaforge.io

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import rotaforge.model.Preference
import rotaforge.model.Roster
import rotaforge.model.Rules
import rotaforge.model.ShiftDef
import rotaforge.model.Staff

/** Reads the input CSV files and builds a [[Roster]]. */
object InputLoader {

  private def fail(message: String): Nothing =
    throw new IllegalArgumentException(message)

  /** Load a roster from the CSV files inside the given directory. */
  def load(dir: Path): Roster = {
    val staff = readStaff(dir.resolve("staff.csv"))
    val days = readDays(dir.resolve("days.csv"))
    val shifts = readShifts(dir.resolve("shifts.csv"))
    val rules = readRules(dir.resolve("rules.csv"))
    val preferences = readPreferences(dir.resolve("preferences.csv"), days)
    Roster(days, staff, shifts, preferences, rules)
  }

  private def readStaff(path: Path): Vector[Staff] = {
    val rows = Csv.read(path, hasHeader = true)
    rows.map { r =>
      if (r.size < 3) fail(s"$path: each staff row needs id, name, skill")
      Staff(id = r(0).trim, name = r(1).trim, skill = r(2).trim)
    }
  }

  private def readDays(path: Path): Vector[LocalDate] = {
    val rows = Csv.read(path, hasHeader = true)
    rows.map { r =>
      if (r.isEmpty || r(0).trim.isEmpty) fail(s"$path: a day row is empty")
      LocalDate.parse(r(0).trim)
    }
  }

  private def parseTime(value: String, path: Path): Int = {
    val parts = value.split(":")
    if (parts.length != 2) fail(s"$path: invalid time '$value'")
    val hours = parts(0).toInt
    val minutes = parts(1).toInt
    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59)
      fail(s"$path: invalid time '$value'")
    hours * 60 + minutes
  }

  private def readShifts(path: Path): Vector[ShiftDef] = {
    val rows = Csv.read(path, hasHeader = true)
    rows.map { r =>
      if (r.size < 6) fail(s"$path: each shift row needs 6 columns")
      val start = parseTime(r(2).trim, path)
      val rawEnd = parseTime(r(3).trim, path)
      val end =
        if (rawEnd > start) rawEnd
        else if (rawEnd == start) start
        else rawEnd + 24 * 60
      val headcount = r(5).trim.toInt
      if (headcount < 0) fail(s"$path: headcount must be zero or more")
      ShiftDef(
        id = r(0).trim,
        name = r(1).trim,
        startMinute = start,
        endMinute = end,
        skill = r(4).trim,
        headcount = headcount
      )
    }
  }

  private def readRules(path: Path): Rules = {
    if (!Files.exists(path)) return Rules.defaults
    val values = Csv
      .read(path, hasHeader = true)
      .collect {
        case r if r.size >= 2 && r(0).trim.nonEmpty =>
          r(0).trim -> r(1).trim
      }
      .toMap
    def intValue(key: String, default: Int): Int =
      values.get(key).map(_.toInt).getOrElse(default)
    Rules(
      maxShiftsPerPerson = intValue("maxShiftsPerPerson", Rules.defaults.maxShiftsPerPerson),
      maxConsecutiveDays = intValue("maxConsecutiveDays", Rules.defaults.maxConsecutiveDays),
      minRestHours = intValue("minRestHours", Rules.defaults.minRestHours),
      hardPenalty = intValue("hardPenalty", Rules.defaults.hardPenalty)
    )
  }

  private def readPreferences(path: Path, days: Vector[LocalDate]): Vector[Preference] = {
    if (!Files.exists(path)) return Vector.empty
    val validDays = days.toSet
    Csv.read(path, hasHeader = true).map { r =>
      if (r.size < 4) fail(s"$path: each preference row needs 4 columns")
      val day = LocalDate.parse(r(1).trim)
      if (!validDays.contains(day))
        fail(s"$path: preference refers to '$day' outside the roster days")
      val weight = r(3).trim.toInt
      if (weight < 0) fail(s"$path: preference weight must not be negative")
      Preference(staff = r(0).trim, day = day, shift = r(2).trim, weight = weight)
    }
  }
}
