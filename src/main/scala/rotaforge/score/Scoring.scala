package rotaforge.score

/** Whether a constraint is a hard rule or a soft preference. */
enum ConstraintKind:
  case Hard, Soft

/** The score of one constraint for one schedule. */
final case class ConstraintResult(
    key: String,
    name: String,
    kind: ConstraintKind,
    weight: Int,
    violations: Int,
    penalty: Int
)

/** The full score of a schedule, broken down per constraint. */
final case class ScoreBreakdown(results: Vector[ConstraintResult]) {

  def byKey(key: String): Option[ConstraintResult] = results.find(_.key == key)

  def hardViolations: Int =
    results.iterator.filter(_.kind == ConstraintKind.Hard).map(_.violations).sum

  def softPenalty: Int =
    results.iterator.filter(_.kind == ConstraintKind.Soft).map(_.penalty).sum

  def total: Int = results.iterator.map(_.penalty).sum

  def hardResults: Vector[ConstraintResult] =
    results.filter(_.kind == ConstraintKind.Hard)

  def softResults: Vector[ConstraintResult] =
    results.filter(_.kind == ConstraintKind.Soft)
}
