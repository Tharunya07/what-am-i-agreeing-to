package agreeingto

import com.raquo.laminar.api.L.{*, given}
import agreeingto.engine.{AnalysisResult, Analyzer, ClauseCategory, Sentence}
import agreeingto.ui.Samples
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

@main def main(): Unit = {
  val inputText = Var("")
  val lastAnalysis = Var(Option.empty[AnalysisResult])
  val copyFeedback = Var(Option.empty[(String, Boolean)])

  def scrollToAnalysis(): Unit =
    dom.window.setTimeout(
      () =>
        dom.document.getElementById("analysis-section") match
          case elem: dom.HTMLElement => elem.scrollIntoView()
          case _ => (),
      30
    )

  def runAnalysis(text: String): Unit =
    val trimmed = text.trim
    if trimmed.isEmpty then lastAnalysis.set(None)
    else
      lastAnalysis.set(Some(Analyzer.analyze(text)))
      scrollToAnalysis()

  def setSample(text: String): Unit =
    inputText.set(text)
    runAnalysis(text)

  def resetCopyFeedbackLater(): Unit =
    dom.window.setTimeout(() => copyFeedback.set(None), 1800)

  def buildCopySummary(analysis: AnalysisResult): String =
    val topAreas = analysis.findings.take(2).map(f => prettyCategory(f.category))
    val topAreasLine =
      if topAreas.isEmpty then "None identified"
      else topAreas.mkString(", ")

    val findingsBlock =
      if analysis.findings.isEmpty then "- No findings from current rules."
      else
        analysis.findings
          .map { finding =>
            val evidenceLines = finding.evidence.take(2).map(s => s"  • ${s.text}")
            (List(s"- ${prettyCategory(finding.category)}: ${finding.explanation}") ++ evidenceLines).mkString("\n")
          }
          .mkString("\n\n")

    val readCarefullyBlock =
      if analysis.readCarefully.isEmpty then "1) No specific sentences flagged."
      else
        analysis.readCarefully
          .take(3)
          .zipWithIndex
          .map((s, i) => s"${i + 1}) ${s.text}")
          .mkString("\n")

    s"""What Am I Agreeing To? (local analysis)
       |
       |Doc type: ${friendlyDocType(analysis)} (${docTypeConfidenceLabel(analysis)})
       |Risk: ${analysis.risk.toString} (${analysis.riskScore}/100)
       |Top areas: $topAreasLine
       |
       |Findings:
       |$findingsBlock
       |
       |Read carefully:
       |$readCarefullyBlock
       |
       |Next action: ${analysis.nextAction}
       |
       |Note: Not legal advice. Runs locally, nothing uploaded.
       |""".stripMargin

  def copyResults(analysis: AnalysisResult): Unit =
    val text = buildCopySummary(analysis)
    dom.window.navigator.clipboard
      .writeText(text)
      .toFuture
      .onComplete {
        case Success(_) =>
          copyFeedback.set(Some("Copied", false))
          resetCopyFeedbackLater()
        case Failure(_) =>
          copyFeedback.set(Some("Could not copy to clipboard", true))
          resetCopyFeedbackLater()
      }

  renderOnDomContentLoaded(
    container = dom.document.querySelector("#app"),
    rootNode = div(
      className := "container",
      div(
        className := "header",
        h1("What Am I Agreeing To?"),
        p("Paste policy, terms, or request text to get a fast first-pass review."),
        p("All analysis runs locally in your browser. Nothing is uploaded.")
      ),
      detailsTag(
        className := "card",
        summaryTag(
          cursor := "pointer",
          fontWeight := "600",
          "How this works"
        ),
        ul(
          marginTop := "8px",
          li("Rule-based scan for common clauses (auto-renewal, fees, data sharing, arbitration)."),
          li("Shows the exact sentences that triggered each finding."),
          li("Risk score is a simple weighted heuristic, not legal advice."),
          li("Runs entirely in your browser, your text is not stored or sent anywhere."),
          li("Best results: paste the relevant section (billing, cancellation, disputes).")
        )
      ),
      div(
        className := "toolbar",
        button(
          className := "secondary",
          "Try subscription sample",
          onClick --> Observer[dom.MouseEvent](_ => setSample(Samples.subscriptionSample))
        ),
        button(
          className := "secondary",
          "Try privacy sample",
          onClick --> Observer[dom.MouseEvent](_ => setSample(Samples.privacySample))
        ),
        button(
          className := "secondary",
          "Try email sample",
          onClick --> Observer[dom.MouseEvent](_ => setSample(Samples.emailSample))
        ),
        button(
          className := "ghost",
          "Clear",
          onClick --> Observer[dom.MouseEvent] { _ =>
            inputText.set("")
            lastAnalysis.set(None)
            copyFeedback.set(None)
          }
        )
      ),
      textArea(
        className := "input",
        rows := 10,
        width := "100%",
        placeholder := "Paste text here...",
        value <-- inputText.signal,
        onInput.mapToValue --> inputText
      ),
      div(
        className := "muted",
        child.text <-- inputText.signal.map(t => s"Characters: ${t.length}")
      ),
      div(
        className := "muted",
        "Tip: paste the 'Cancellation' section if you want a clearer next step."
      ),
      div(
        className := "toolbar",
        button(
          className := "primary",
          "Analyze",
          disabled <-- inputText.signal.map(_.trim.isEmpty),
          onClick.mapTo(inputText.now()) --> Observer[String](runAnalysis)
        ),
        button(
          className := "secondary",
          "Copy results",
          disabled <-- lastAnalysis.signal.map(_.isEmpty),
          onClick --> Observer[dom.MouseEvent] { _ =>
            lastAnalysis.now().foreach(copyResults)
          }
        ),
        child.maybe <-- copyFeedback.signal.map {
          case Some((message, isError)) =>
            Some(
              span(
                className := (if isError then "muted copyStatus error" else "muted copyStatus"),
                message
              )
            )
          case None => None
        }
      ),
      child.maybe <-- lastAnalysis.signal.map { result =>
        result.map { analysis =>
          div(
            idAttr := "analysis-section",
            className := "card",
            h2("Analysis"),
            p(
              b("Doc Type: "),
              s"${friendlyDocType(analysis)} (${docTypeConfidenceLabel(analysis)})"
            ),
            if isLowConfidenceDocType(analysis) then
              p(
                className := "muted",
                "Tip: paste the billing, cancellation, or disputes section for clearer results."
              )
            else span(),
            p(
              b("Risk: "), span(className := s"badge ${riskClass(analysis)}", analysis.risk.toString),
              s" (${analysis.riskScore}/100)"
            ),
            p(
              analysis.riskWhy
            ),
            h3(className := "sectionTitle", "Findings"),
            if analysis.findings.nonEmpty then
              ul(
                analysis.findings.map { finding =>
                  li(
                    b(s"${prettyCategory(finding.category)} (${finding.score}/100): "),
                    finding.explanation,
                    if finding.evidence.nonEmpty then
                      ul(
                        finding.evidence.take(2).map(s => li(s.text))
                      )
                    else span()
                  )
                }
              )
            else p("No findings from current rules."),
            h3(className := "sectionTitle", "Read Carefully"),
            if analysis.readCarefully.nonEmpty then
              ol(
                analysis.readCarefully.take(3).map(s => li(s.text))
              )
            else p("No specific sentences flagged."),
            div(
              className := "card",
              h3(className := "sectionTitle", "Next Action"),
              p(analysis.nextAction)
            ),
            div(
              className := "card",
              h3(className := "sectionTitle", "Original Text (Highlighted)"),
              p(
                className := "muted",
                "Highlighted lines are the top 'Read carefully' sentences."
              ),
              div(
                className := "highlightedText",
                child <-- inputText.signal.map { text =>
                  span(highlightedFragments(text, analysis.readCarefully.take(3)))
                }
              )
            )
          )
        }
      },
      footerTag(
        className := "footer muted",
        "Not legal advice. Runs locally in your browser."
      )
    )
  )
}

def prettyCategory(category: ClauseCategory): String =
  category match
    case ClauseCategory.AutoRenewal => "Auto renewal"
    case ClauseCategory.Cancellation => "Cancellation"
    case ClauseCategory.FeesPenalties => "Fees and penalties"
    case ClauseCategory.DataSharing => "Data sharing"
    case ClauseCategory.ArbitrationLiability => "Arbitration and liability"

def friendlyDocType(analysis: AnalysisResult): String =
  if isLowConfidenceDocType(analysis) then "General legal text"
  else
    analysis.docType match
      case agreeingto.engine.DocType.PrivacyPolicy => "Privacy policy"
      case agreeingto.engine.DocType.TermsAgreement => "Terms/Agreement"
      case agreeingto.engine.DocType.EmailRequest => "Email request"
      case agreeingto.engine.DocType.Unknown => "General legal text"

def docTypeConfidenceLabel(analysis: AnalysisResult): String =
  if isLowConfidenceDocType(analysis) then s"Low confidence (${analysis.docTypeConfidence}%)"
  else s"${analysis.docTypeConfidence}%"

def isLowConfidenceDocType(analysis: AnalysisResult): Boolean =
  analysis.docType == agreeingto.engine.DocType.Unknown || analysis.docTypeConfidence < 55

def riskClass(analysis: AnalysisResult): String =
  analysis.risk.toString.toLowerCase

def highlightedFragments(text: String, highlights: List[Sentence]): List[Modifier[HtmlElement]] =
  val sorted = highlights
    .filter(s => s.start >= 0 && s.end <= text.length && s.start < s.end)
    .sortBy(_.start)

  val out = scala.collection.mutable.ListBuffer.empty[Modifier[HtmlElement]]
  var cursor = 0

  sorted.foreach { s =>
    if s.start > cursor then out += text.slice(cursor, s.start)
    out += mark(text.slice(s.start, s.end))
    cursor = s.end
  }

  if cursor < text.length then out += text.drop(cursor)
  out.toList
