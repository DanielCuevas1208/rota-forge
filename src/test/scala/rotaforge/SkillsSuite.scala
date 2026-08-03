package rotaforge

import rotaforge.model.Skills

class SkillsSuite extends munit.FunSuite {

  test("parse keeps a single skill") {
    assertEquals(Skills.parse("R.N."), Set("R.N."))
  }

  test("parse splits a semicolon list") {
    assertEquals(Skills.parse("R.N.;C.A."), Set("R.N.", "C.A."))
  }

  test("parse trims surrounding spaces") {
    assertEquals(Skills.parse(" R.N. ; C.A. "), Set("R.N.", "C.A."))
  }

  test("parse ignores empty entries") {
    assertEquals(Skills.parse("R.N.;;C.A."), Set("R.N.", "C.A."))
  }

  test("matches accepts the exact skill") {
    assert(Skills.matches(Set("R.N."), Set("R.N.")))
  }

  test("matches accepts when the staff hold one of several required skills") {
    assert(Skills.matches(Set("R.N.", "C.A."), Set("C.A.")))
  }

  test("matches rejects a staff member without the skill") {
    assert(!Skills.matches(Set("R.N."), Set("C.A.")))
  }

  test("matches rejects a staff member with no matching skill") {
    assert(!Skills.matches(Set("R.N.", "C.A."), Set("P.T.")))
  }

  test("the wildcard shift accepts any staff member") {
    assert(Skills.matches(Set("*"), Set("R.N.")))
    assert(Skills.matches(Set("*"), Set("C.A.")))
  }
}
