package rotaforge.model

/**
 * Parses and compares skill lists.
 *
 * A skill column can hold one skill or several. A semicolon separates
 * the skills. For example, `R.N.;C.A.` means the staff member can fill
 * either a nurse role or a care assistant role. A shift with the
 * wildcard `*` accepts any staff member.
 */
object Skills {

  /** Split a raw skill column into a set of skills. */
  def parse(raw: String): Set[String] =
    raw.split(";").iterator.map(_.trim).filter(_.nonEmpty).toSet

  /**
   * True when the staff member can fill the shift.
   *
   * The shift is fillable when it takes anyone (the `*` wildcard) or
   * when the staff member holds at least one of its required skills.
   */
  def matches(shiftSkills: Set[String], staffSkills: Set[String]): Boolean =
    shiftSkills.contains("*") || shiftSkills.exists(staffSkills.contains)
}
