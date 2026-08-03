package rotaforge

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import rotaforge.io.InputLoader

class InputLoaderSuite extends munit.FunSuite {

  private def write(dir: java.nio.file.Path, name: String, content: String): Unit = {
    Files.createDirectories(dir)
    Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8)
  }

  private val staffCsv =
    "id,name,skill\nN1,Ana Silva,R.N.\nN2,Boris Chen,R.N.\n"
  private val daysCsv =
    "date\n2026-05-04\n2026-05-05\n2026-05-06\n"

  private val shiftsCsv =
    "id,name,start,end,skill,count\nD,Day Shift,07:00,19:00,R.N.,1\nN,Night Shift,21:00,09:00,R.N.,1\n"

  private val rulesCsv =
    "key,value\nmaxShiftsPerPerson,9\nmaxConsecutiveDays,4\nminRestHours,10\nhardPenalty,500000\n"

  private val preferencesCsv =
    "staff,date,shift,weight\nN1,2026-05-04,D,5\nN2,2026-05-05,N,0\n"

  test("loads a full roster from CSV files") {
    val dir = Files.createTempDirectory("rotaforge-input")
    write(dir, "staff.csv", staffCsv)
    write(dir, "days.csv", daysCsv)
    write(dir, "shifts.csv", shiftsCsv)
    write(dir, "rules.csv", rulesCsv)
    write(dir, "preferences.csv", preferencesCsv)

    val roster = InputLoader.load(dir)
    assertEquals(roster.staff.size, 2)
    assertEquals(roster.days.size, 3)
    assertEquals(roster.shifts.size, 2)
    assertEquals(roster.preferences.size, 2)
    assertEquals(roster.days.head, LocalDate.of(2026, 5, 4))
    assertEquals(roster.rules.maxShiftsPerPerson, 9)
    assertEquals(roster.rules.minRestHours, 10)
    assertEquals(roster.shifts.head.headcount, 1)
  }

  test("a night shift that crosses midnight keeps its full length") {
    val dir = Files.createTempDirectory("rotaforge-night")
    write(dir, "staff.csv", staffCsv)
    write(dir, "days.csv", daysCsv)
    write(dir, "shifts.csv", shiftsCsv)

    val roster = InputLoader.load(dir)
    val night = roster.shifts.find(_.id == "N").get
    assertEquals(night.startMinute, 21 * 60)
    assertEquals(night.endMinute, 21 * 60 + 12 * 60)
  }

  test("rules fall back to defaults when rules.csv is missing") {
    val dir = Files.createTempDirectory("rotaforge-norules")
    write(dir, "staff.csv", staffCsv)
    write(dir, "days.csv", daysCsv)
    write(dir, "shifts.csv", shiftsCsv)

    val roster = InputLoader.load(dir)
    assertEquals(roster.rules, rotaforge.model.Rules.defaults)
  }

  test("preferences are rejected when they refer to a day outside the roster") {
    val dir = Files.createTempDirectory("rotaforge-badpref")
    write(dir, "staff.csv", staffCsv)
    write(dir, "days.csv", daysCsv)
    write(dir, "shifts.csv", shiftsCsv)
    write(dir, "preferences.csv", "staff,date,shift,weight\nN1,2030-01-01,D,5\n")

    val error = intercept[IllegalArgumentException] {
      InputLoader.load(dir)
    }
    assert(error.getMessage.contains("outside the roster days"))
  }

  test("missing staff.csv fails with a clear message") {
    val dir = Files.createTempDirectory("rotaforge-nostaff")
    val error = intercept[Exception] {
      InputLoader.load(dir)
    }
    assert(error.getMessage.contains("staff.csv"))
  }

  test("a semicolon skill list loads as several skills") {
    val dir = Files.createTempDirectory("rotaforge-multiskill")
    write(dir, "staff.csv", "id,name,skill\nN1,Ana Silva,R.N.\nN2,Bo Lin,R.N.;L.P.N.\n")
    write(dir, "days.csv", daysCsv)
    write(dir, "shifts.csv", shiftsCsv)

    val index = new rotaforge.core.RosterIndex(InputLoader.load(dir))
    assertEquals(index.staffSkills(index.staffIdx("N2")), Set("R.N.", "L.P.N."))
    assertEquals(index.staffSkills(index.staffIdx("N1")), Set("R.N."))
  }
}
