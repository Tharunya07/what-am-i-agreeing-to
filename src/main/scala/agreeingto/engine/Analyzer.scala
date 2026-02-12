package agreeingto.engine

import agreeingto.rules.Ruleset
import agreeingto.rules.Ruleset.Rule
import agreeingto.segment.Segmenter

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.matching.Regex

object Analyzer:
  private val categoryWeights: Map[ClauseCategory, Double] = Map(
    ClauseCategory.AutoRenewal -> 1.0,
    ClauseCategory.Cancellation -> 1.0,
    ClauseCategory.FeesPenalties -> 1.8,
    ClauseCategory.DataSharing -> 1.1,
    ClauseCategory.ArbitrationLiability -> 1.9
  )

  private val docTypeSignals: List[(DocType, List[Regex])] = List(
    DocType.PrivacyPolicy -> List(
      "(?i)\\bprivacy\\b".r,
      "(?i)\\bpersonal\\s+data\\b".r,
      "(?i)\\bcookies?\\b".r,
      "(?i)\\bgdpr\\b".r,
      "(?i)\\bccpa\\b".r,
      "(?i)\\bthird\\s+party\\b".r
    ),
    DocType.TermsAgreement -> List(
      "(?i)\\bterms\\b".r,
      "(?i)\\bagreement\\b".r,
      "(?i)\\bliability\\b".r,
      "(?i)\\barbitration\\b".r,
      "(?i)\\bindemnif(y|ication)\\b".r,
      "(?i)\\bgoverning\\s+law\\b".r
    ),
    DocType.EmailRequest -> List(
      "(?i)\\bplease\\b".r,
      "(?i)\\breply\\b".r,
      "(?i)\\bconfirm\\b".r,
      "(?i)\\bby\\s+(?:\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?|[A-Za-z]+\\s+\\d{1,2})\\b".r,
      "(?i)\\bapprove\\b".r,
      "(?i)\\baction\\s+required\\b".r
    )
  )

  def analyze(text: String): AnalysisResult =
    val sents = Segmenter.sentences(text)
    val sentenceById = sents.map(s => s.id -> s).toMap

    val categoryScores = mutable.Map.empty[ClauseCategory, Int].withDefaultValue(0)
    val sentenceTotalWeight = mutable.Map.empty[Int, Int].withDefaultValue(0)
    val sentenceCategoryMatchCount = mutable.Map.empty[(ClauseCategory, Int), Int].withDefaultValue(0)
    val sentenceCategoryWeight = mutable.Map.empty[(ClauseCategory, Int), Int].withDefaultValue(0)
    val categoryTopRule = mutable.Map.empty[ClauseCategory, Rule]

    sents.foreach { sentence =>
      Ruleset.all.foreach { rule =>
        val matchedPatterns = rule.patterns.count(_.findFirstIn(sentence.text).nonEmpty)
        if matchedPatterns > 0 then
          val key = (rule.category, sentence.id)
          categoryScores.update(rule.category, categoryScores(rule.category) + rule.weight)
          sentenceTotalWeight.update(sentence.id, sentenceTotalWeight(sentence.id) + rule.weight)
          sentenceCategoryMatchCount.update(key, sentenceCategoryMatchCount(key) + matchedPatterns)
          sentenceCategoryWeight.update(key, sentenceCategoryWeight(key) + rule.weight)

          categoryTopRule.get(rule.category) match
            case Some(existing) if existing.weight >= rule.weight => ()
            case _ => categoryTopRule.update(rule.category, rule)
      }
    }

    val cappedCategoryScores = categoryScores.view.mapValues(v => math.min(100, v)).toMap
    val findings = cappedCategoryScores.toList
      .filter(_._2 > 0)
      .sortBy((_, score) => -score)
      .map { (category, score) =>
        val evidence = sents
          .filter(s => sentenceCategoryMatchCount((category, s.id)) > 0)
          .sortBy { s =>
            (
              -sentenceCategoryMatchCount((category, s.id)),
              -sentenceCategoryWeight((category, s.id)),
              -s.text.length,
              s.start
            )
          }
          .take(2)

        val explanation = categoryTopRule
          .get(category)
          .map(_.explanation)
          .getOrElse("This category contains potentially important obligations.")

        ClauseFinding(
          category = category,
          score = score,
          evidence = evidence,
          explanation = explanation
        )
      }

    val (docType, docTypeConfidence) = classifyDocType(text)

    val weightedSum = cappedCategoryScores.toList.map { (category, score) =>
      val weight = categoryWeights.getOrElse(category, 1.0)
      score * weight
    }.sum
    val maxWeighted = categoryWeights.values.sum * 100.0
    val baseRiskScore = math.round((weightedSum / maxWeighted) * 100).toInt

    val autoRenewalScore = cappedCategoryScores.getOrElse(ClauseCategory.AutoRenewal, 0)
    val feesPenaltiesScore = cappedCategoryScores.getOrElse(ClauseCategory.FeesPenalties, 0)
    val arbitrationLiabilityScore = cappedCategoryScores.getOrElse(ClauseCategory.ArbitrationLiability, 0)

    val autoRenewalFeesBonus =
      if autoRenewalScore > 40 && feesPenaltiesScore > 40 then 10 else 0

    val arbitrationBoostMultiplier =
      if arbitrationLiabilityScore > 30 then 1.08 else 1.0

    val riskScore = math
      .round((baseRiskScore + autoRenewalFeesBonus) * arbitrationBoostMultiplier)
      .toInt
      .max(0)
      .min(100)

    val riskLevel =
      if riskScore < 25 then RiskLevel.Low
      else if riskScore < 60 then RiskLevel.Medium
      else RiskLevel.High

    val topCategories = findings.take(2)
    val riskWhy =
      if topCategories.isEmpty then "No major risky clauses were detected by the current rules."
      else
        topCategories
          .map(f => s"${formatCategory(f.category)} (${f.score})")
          .mkString("Highest-impact areas: ", ", ", ".")

    val readCarefully = sentenceTotalWeight.toList
      .filter(_._2 > 0)
      .sortBy { (sentenceId, totalWeight) =>
        val length = sentenceById.get(sentenceId).map(_.text.length).getOrElse(0)
        (-totalWeight, -length, sentenceById(sentenceId).start)
      }
      .take(3)
      .flatMap((id, _) => sentenceById.get(id))

    val nextAction = nextActionFor(
      findings.headOption.map(_.category)
    )

    AnalysisResult(
      docType = docType,
      docTypeConfidence = docTypeConfidence,
      risk = riskLevel,
      riskScore = riskScore,
      riskWhy = riskWhy,
      findings = findings,
      readCarefully = readCarefully,
      nextAction = nextAction
    )

  private def classifyDocType(text: String): (DocType, Int) =
    val scores = docTypeSignals.map { (docType, patterns) =>
      val raw = patterns.map(p => p.findAllMatchIn(text).length).sum * 18
      docType -> raw.min(100)
    }
    val ordered = scores.sortBy((_, score) => -score)
    val (topType, topScore) = ordered.head
    val secondScore = ordered.drop(1).headOption.map(_._2).getOrElse(0)
    val gap = topScore - secondScore

    if topScore <= 0 then (DocType.Unknown, 0)
    else
      val confidence = (45 + (topScore / 2) + (gap * 2)).max(0).min(100)
      (topType, confidence)

  private def nextActionFor(category: Option[ClauseCategory]): String =
    category match
      case Some(ClauseCategory.AutoRenewal) =>
        "Set a calendar reminder for the renewal deadline and confirm the exact cancellation window."
      case Some(ClauseCategory.Cancellation) =>
        "Find the exact cancellation steps and keep a record of notice, timing, and any refund conditions."
      case Some(ClauseCategory.FeesPenalties) =>
        "List all fees and penalty triggers in writing before accepting, including billing and price-change terms."
      case Some(ClauseCategory.DataSharing) =>
        "Review data-sharing and tracking clauses, then adjust privacy settings before agreeing."
      case Some(ClauseCategory.ArbitrationLiability) =>
        "Review arbitration and liability language carefully, and escalate legal review before accepting if needed."
      case None =>
        "No strong red flag category was found; still review the full text before accepting."

  private def formatCategory(category: ClauseCategory): String =
    category match
      case ClauseCategory.AutoRenewal => "Auto renewal"
      case ClauseCategory.Cancellation => "Cancellation"
      case ClauseCategory.FeesPenalties => "Fees and penalties"
      case ClauseCategory.DataSharing => "Data sharing"
      case ClauseCategory.ArbitrationLiability => "Arbitration and liability"
