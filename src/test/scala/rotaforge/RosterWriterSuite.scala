package rotaforge

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import rotaforge.core.InitialRoster
import rotaforge.core.RosterIndex
import rotaforge.io.Csv
import rotaforge.io.InputLoader
import rotaforge.io.RosterWriter

class RosterWriterSuite extends munit.FunSuite {

  test("the roster CSV round-trips the assignments") {
    val dir = Paths.get(getClass.getClassLoader.getResource("clinic").toURI)
    val roster = InputLoader.load(dir)
    val index = new RosterIndex(roster)
    val grid = InitialRoster.build(index)

    val out = Files.createTempDirectory("rotaforge-out")
    val path = RosterWriter.write(out, grid)

    val text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    val rows = Csv.parse(text)
    assertEquals(rows.head, Vector("date", "shift", "shiftName", "staff", "staffName"))
    assertEquals(rows.size - 1, grid.assignments.size)

    val uniqueStaff = rows.drop(1).map(r => r(0) -> r(3)).toSet
    assertEquals(uniqueStaff.size, grid.assignments.size, "no duplicate assignment rows")
  }

  test("the CSV rows are sorted by date, then shift, then staff") {
    val dir = Paths.get(getClass.getClassLoader.getResource("clinic").toURI)
    val roster = InputLoader.load(dir)
    val index = new RosterIndex(roster)
    val grid = InitialRoster.build(index)

    val out = Files.createTempDirectory("rotaforge-out")
    val path = RosterWriter.write(out, grid)
    val rows = Csv.parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).drop(1)
    val keys = rows.map(r => (r(0), r(1), r(3)))
    assertEquals(keys, keys.sorted)
  }
}
