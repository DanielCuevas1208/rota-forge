package rotaforge.io

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.mutable.ArrayBuffer

/**
 * A small CSV reader that handles the common cases of RFC 4180.
 *
 * It supports quoted fields, embedded commas, doubled quotes inside
 * quotes, and both LF and CRLF line endings.
 */
object Csv {

  /** Split CSV text into rows of fields. Blank lines are ignored. */
  def parse(text: String): Vector[Vector[String]] = {
    val rows = ArrayBuffer[Vector[String]]()
    val row = ArrayBuffer[String]()
    val field = new StringBuilder
    var inQuotes = false
    var i = 0
    val n = text.length

    def endField(): Unit = {
      row += field.result()
      field.clear()
    }

    def endRow(): Unit = {
      endField()
      val fields = row.toVector
      if (fields.exists(_.trim.nonEmpty)) rows += fields
      row.clear()
    }

    while (i < n) {
      val c = text.charAt(i)
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < n && text.charAt(i + 1) == '"') {
            field += '"'
            i += 2
          } else {
            inQuotes = false
            i += 1
          }
        } else {
          field += c
          i += 1
        }
      } else {
        c match {
          case '"' =>
            inQuotes = true
            i += 1
          case ',' =>
            endField()
            i += 1
          case '\r' =>
            if (i + 1 < n && text.charAt(i + 1) == '\n') i += 2 else i += 1
            endRow()
          case '\n' =>
            endRow()
            i += 1
          case _ =>
            field += c
            i += 1
        }
      }
    }

    if (field.nonEmpty || row.nonEmpty) endRow()
    rows.toVector
  }

  /**
   * Read a CSV file and return its data rows.
   * The header row, when present, is removed.
   */
  def read(path: Path, hasHeader: Boolean): Vector[Vector[String]] = {
    val text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    val data = parse(text)
    if (hasHeader) data.drop(1) else data
  }
}
