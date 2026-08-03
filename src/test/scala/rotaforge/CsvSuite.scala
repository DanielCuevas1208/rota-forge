package rotaforge

import rotaforge.io.Csv

class CsvSuite extends munit.FunSuite {

  test("parses simple comma rows") {
    val rows = Csv.parse("a,b,c\n1,2,3\n")
    assertEquals(rows, Vector(Vector("a", "b", "c"), Vector("1", "2", "3")))
  }

  test("keeps a quoted field with an embedded comma") {
    val rows = Csv.parse("N1,\"Silva, Ana\",R.N.\n")
    assertEquals(rows, Vector(Vector("N1", "Silva, Ana", "R.N.")))
  }

  test("unwraps doubled quotes inside a quoted field") {
    val rows = Csv.parse("\"He said \"\"hi\"\"\",x\n")
    assertEquals(rows, Vector(Vector("He said \"hi\"", "x")))
  }

  test("handles CRLF line endings") {
    val rows = Csv.parse("a,b\r\n1,2\r\n")
    assertEquals(rows, Vector(Vector("a", "b"), Vector("1", "2")))
  }

  test("skips blank lines") {
    val rows = Csv.parse("a,b\n\n1,2\n\n")
    assertEquals(rows, Vector(Vector("a", "b"), Vector("1", "2")))
  }

  test("preserves empty trailing fields") {
    val rows = Csv.parse("a,,c\n")
    assertEquals(rows, Vector(Vector("a", "", "c")))
  }
}
