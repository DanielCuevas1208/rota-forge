package rotaforge.report

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import rotaforge.core.Grid
import rotaforge.score.ConstraintKind
import rotaforge.score.ScoreBreakdown

/** Metadata shown in the report header. */
final case class ReportMeta(
    instanceName: String,
    inputDir: String,
    seed: Long,
    iterations: Int,
    startTemp: Double,
    endTemp: Double
)

/** Renders the roster as a printable, self-contained HTML report. */
object HtmlReport {

  private def escape(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

  private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
  private val timeFormat = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")

  /** Write the report file next to the given output directory. */
  def write(dir: Path, grid: Grid, score: ScoreBreakdown, meta: ReportMeta): Path = {
    Files.createDirectories(dir)
    val path = dir.resolve("report.html")
    Files.writeString(path, render(grid, score, meta), StandardCharsets.UTF_8)
    path
  }

  /** Render the report as a full HTML document. */
  def render(grid: Grid, score: ScoreBreakdown, meta: ReportMeta): String = {
    val index = grid.index
    val dayLabels = index.daysByIndex.map { d =>
      escape(d.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy")))
    }

    val constraintRows = score.results.map { r =>
      val badge =
        if (r.kind == ConstraintKind.Hard) {
          if (r.violations == 0) "pass" else "fail"
        } else {
          "soft"
        }
      val badgeText =
        if (r.kind == ConstraintKind.Hard) {
          if (r.violations == 0) "Pass" else "Fail"
        } else {
          "Soft"
        }
      s"""<tr>
         |<td>${escape(r.name)}</td>
         |<td><span class="badge $badge">$badgeText</span></td>
         |<td class="num">${r.weight}</td>
         |<td class="num">${r.violations}</td>
         |<td class="num">${r.penalty}</td>
         |</tr>""".stripMargin
    }.mkString("\n")

    val staffRows = index.staffByIndex.zipWithIndex.map { case (id, st) =>
      val cells = (0 until index.nDays).map { day =>
        val s = grid.duty(st)(day)
        if (s == -1)
          """<td class="cell rest">&ndash;</td>"""
        else {
          val shift = index.shiftsByIndex(s)
          val title = escape(shift.name)
          s"""<td class="cell shift" title="$title">${escape(shift.id)}</td>"""
        }
      }.mkString("\n")
      s"""<tr>
         |<td class="staff">${escape(index.staffName(st))}<span class="staff-id">${escape(id)}</span></td>
         |$cells
         |</tr>""".stripMargin
    }.mkString("\n")

    val dayHeaders = (0 until index.nDays).map { day =>
      val cls = if (index.isWeekend(day)) "dayhdr weekend" else "dayhdr"
      s"""<th class="$cls">${dayLabels(day)}</th>"""
    }.mkString("\n")

    val shiftLegend = index.shiftsByIndex.map { s =>
      s"""<span class="legend-item"><span class="shift-badge shift-${escape(s.id)}">${escape(s.id)}</span> ${escape(s.name)}</span>"""
    }.mkString("\n")

    s"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Rota Forge &mdash; ${escape(meta.instanceName)}</title>
<style>
  :root {
    --teal-700: #0f766e;
    --teal-600: #0d9488;
    --ink: #1f2937;
    --muted: #6b7280;
    --line: #e5e7eb;
    --paper: #ffffff;
    --bg: #f8fafc;
    --pass: #dcfce7;
    --pass-text: #166534;
    --fail: #fee2e2;
    --fail-text: #991b1b;
    --soft: #e0f2fe;
    --soft-text: #075985;
    --day: #fef3c7;
    --day-text: #92400e;
    --evening: #e0e7ff;
    --evening-text: #3730a3;
    --night: #e2e8f0;
    --night-text: #334155;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: "Segoe UI", Arial, Helvetica, sans-serif;
    color: var(--ink);
    background: var(--bg);
    line-height: 1.45;
  }
  .hero {
    background: linear-gradient(135deg, var(--teal-700), var(--teal-600));
    color: #fff;
    padding: 28px 36px;
  }
  .hero h1 { margin: 0 0 4px; font-size: 28px; letter-spacing: 0.3px; }
  .hero .subtitle { margin: 0; font-size: 15px; opacity: 0.92; }
  .hero .meta { margin: 14px 0 0; padding: 0; list-style: none; display: flex; flex-wrap: wrap; gap: 8px 26px; font-size: 12.5px; opacity: 0.95; }
  main { padding: 24px 36px 48px; }
  section { margin-bottom: 34px; }
  h2 { font-size: 17px; margin: 0 0 12px; color: var(--teal-700); border-bottom: 2px solid var(--line); padding-bottom: 6px; }
  .summary { display: flex; flex-wrap: wrap; gap: 16px; }
  .card {
    flex: 1 1 180px; background: var(--paper); border: 1px solid var(--line);
    border-radius: 10px; padding: 14px 18px; box-shadow: 0 1px 2px rgba(0,0,0,0.04);
  }
  .card .label { font-size: 12px; text-transform: uppercase; letter-spacing: 0.6px; color: var(--muted); }
  .card .value { font-size: 26px; font-weight: 600; margin-top: 4px; }
  .card .value.ok { color: var(--pass-text); }
  .card .value.bad { color: var(--fail-text); }
  table { border-collapse: collapse; width: 100%; background: var(--paper); }
  table.bordered { border: 1px solid var(--line); border-radius: 10px; overflow: hidden; }
  th, td { border: 1px solid var(--line); padding: 8px 10px; text-align: left; }
  th { background: #f1f5f9; font-size: 13px; }
  td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
  .badge {
    display: inline-block; padding: 2px 9px; border-radius: 999px;
    font-size: 12px; font-weight: 600;
  }
  .badge.pass { background: var(--pass); color: var(--pass-text); }
  .badge.fail { background: var(--fail); color: var(--fail-text); }
  .badge.soft { background: var(--soft); color: var(--soft-text); }
  .roster-grid th { position: sticky; top: 0; }
  .roster-grid .dayhdr { font-weight: 600; font-size: 12px; text-align: center; }
  .roster-grid .dayhdr.weekend { background: var(--soft); }
  .roster-grid td.cell { text-align: center; font-size: 13px; font-weight: 600; }
  .roster-grid td.staff { font-size: 13px; }
  .staff-id { display: block; font-size: 11px; color: var(--muted); font-weight: 400; }
  .cell.rest { color: #cbd5e1; }
  .shift-badge {
    display: inline-block; min-width: 26px; padding: 2px 7px; border-radius: 6px;
    font-size: 12px; font-weight: 700; text-align: center;
  }
  .shift-D { background: var(--day); color: var(--day-text); }
  .shift-E { background: var(--evening); color: var(--evening-text); }
  .shift-N { background: var(--night); color: var(--night-text); }
  .legend { display: flex; flex-wrap: wrap; gap: 10px 22px; margin-top: 10px; font-size: 13px; }
  .legend-item { display: inline-flex; align-items: center; gap: 7px; }
  footer { padding: 14px 36px 30px; font-size: 12px; color: var(--muted); border-top: 1px solid var(--line); }
  @media print {
    body { background: #fff; }
    .hero { padding: 18px 8px; }
    main { padding: 18px 8px; }
    section { margin-bottom: 18px; }
    .roster-grid { page-break-inside: auto; }
    .roster-grid tr { page-break-inside: avoid; }
    footer { padding: 10px 8px; }
  }
</style>
</head>
<body>
<header class="hero">
  <h1>${escape(meta.instanceName)}</h1>
  <p class="subtitle">Staff roster report</p>
  <ul class="meta">
    <li>Period: ${escape(index.daysByIndex.head.format(dateFormat))} to ${escape(index.daysByIndex.last.format(dateFormat))} (${index.nDays} days)</li>
    <li>Staff: ${index.nStaff}</li>
    <li>Shifts: ${index.nShifts}</li>
    <li>Assignments: ${grid.assignments.size}</li>
    <li>Seed: ${meta.seed}</li>
    <li>Iterations: ${meta.iterations}</li>
    <li>Generated: ${escape(LocalDateTime.now().format(timeFormat))}</li>
  </ul>
</header>
<main>
  <section class="summary">
    <div class="card"><div class="label">Total score</div><div class="value">${score.total}</div></div>
    <div class="card"><div class="label">Hard violations</div><div class="value ${if (score.hardViolations == 0) "ok" else "bad"}">${score.hardViolations}</div></div>
    <div class="card"><div class="label">Soft penalty</div><div class="value">${score.softPenalty}</div></div>
    <div class="card"><div class="label">Staff on duty</div><div class="value">${grid.workload.filter(_ > 0).size}</div></div>
  </section>

  <section>
    <h2>Constraint scores</h2>
    <table class="bordered">
      <thead>
        <tr>
          <th>Constraint</th>
          <th>Type</th>
          <th class="num">Weight</th>
          <th class="num">Violations</th>
          <th class="num">Penalty</th>
        </tr>
      </thead>
      <tbody>
$constraintRows
      </tbody>
    </table>
  </section>

  <section>
    <h2>Roster grid</h2>
    <table class="bordered roster-grid">
      <thead>
        <tr>
          <th class="dayhdr">Staff</th>
$dayHeaders
        </tr>
      </thead>
      <tbody>
$staffRows
      </tbody>
    </table>
    <div class="legend">
      <span class="legend-item">&ndash; Rest day</span>
$shiftLegend
    </div>
  </section>
</main>
<footer>
  Generated by Rota Forge 0.1.0 from ${escape(meta.inputDir)}.
  Hard rules always hold. A lower score is better.
</footer>
</body>
</html>
"""
  }
}
