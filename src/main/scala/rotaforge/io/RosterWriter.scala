package rotaforge.io

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import rotaforge.core.Grid

/** Writes the final roster as a CSV file. */
object RosterWriter {

  private val header = "date,shift,shiftName,staff,staffName"

  /** Write the roster CSV next to the given output directory. */
  def write(dir: Path, grid: Grid): Path = {
    Files.createDirectories(dir)
    val rows = grid.assignments.sortBy(a => (a.day.toString, a.shift, a.staff))
    val lines = header +: rows.map { a =>
      val s = grid.index.shiftIdx(a.shift)
      val st = grid.index.staffIdx(a.staff)
      List(
        a.day.toString,
        a.shift,
        grid.index.shiftName(s),
        a.staff,
        grid.index.staffName(st)
      ).mkString(",")
    }
    val path = dir.resolve("roster.csv")
    Files.writeString(path, lines.mkString("", "\n", "\n"), StandardCharsets.UTF_8)
    path
  }
}
