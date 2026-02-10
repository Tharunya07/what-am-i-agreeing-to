package agreeingto.rules

import agreeingto.engine.ClauseCategory

import scala.util.matching.Regex

object Ruleset:
  case class Rule(
      category: ClauseCategory,
      weight: Int,
      patterns: List[Regex],
      explanation: String
  )

  val all: List[Rule] = List(
    Rule(
      category = ClauseCategory.AutoRenewal,
      weight = 22,
      patterns = List(
        "(?i)\\bauto[-\\s]?renew\\b".r,
        "(?i)\\brenews?\\s+automatically\\b".r,
        "(?i)\\brecurring\\b".r
      ),
      explanation = "Auto-renewing language may continue charges unless cancellation is done in time."
    ),
    Rule(
      category = ClauseCategory.AutoRenewal,
      weight = 18,
      patterns = List(
        "(?i)\\bsubscription\\s+renews?\\b".r,
        "(?i)\\buntil\\s+you\\s+cancel\\b".r
      ),
      explanation = "Subscription renewal terms can lock you into ongoing billing."
    ),
    Rule(
      category = ClauseCategory.Cancellation,
      weight = 16,
      patterns = List(
        "(?i)\\bcancel\\b".r,
        "(?i)\\bcancellation\\b".r,
        "(?i)\\b(?:terminate|termination)\\b".r
      ),
      explanation = "Cancellation or termination conditions may restrict when and how you can exit."
    ),
    Rule(
      category = ClauseCategory.Cancellation,
      weight = 14,
      patterns = List(
        "(?i)\\bnotice\\b".r,
        "(?i)\\brefund\\b".r,
        "(?i)\\bnon[-\\s]?refundable\\b".r
      ),
      explanation = "Refund and notice terms can create cost or timing constraints."
    ),
    Rule(
      category = ClauseCategory.FeesPenalties,
      weight = 20,
      patterns = List(
        "(?i)\\b(?:fee|fees)\\b".r,
        "(?i)\\bcharges?\\b".r,
        "(?i)\\bpenalty\\b".r
      ),
      explanation = "Fees and penalties can add unexpected cost obligations."
    ),
    Rule(
      category = ClauseCategory.FeesPenalties,
      weight = 18,
      patterns = List(
        "(?i)\\blate\\s+fee\\b".r,
        "(?i)\\bbilling\\b".r,
        "(?i)\\bprice\\s+changes?\\b".r,
        "(?i)\\bmay\\s+charge\\b".r
      ),
      explanation = "Billing and charge-change clauses can increase your financial exposure."
    ),
    Rule(
      category = ClauseCategory.DataSharing,
      weight = 18,
      patterns = List(
        "(?i)\\bthird\\s+party\\b".r,
        "(?i)\\bshare\\s+your\\s+data\\b".r,
        "(?i)\\bpartners?\\b".r
      ),
      explanation = "Data-sharing terms indicate your information may be shared beyond the primary service."
    ),
    Rule(
      category = ClauseCategory.DataSharing,
      weight = 15,
      patterns = List(
        "(?i)\\baffiliates?\\b".r,
        "(?i)\\bcookies?\\b".r,
        "(?i)\\btracking\\b".r
      ),
      explanation = "Tracking and affiliate clauses may broaden collection and use of personal data."
    ),
    Rule(
      category = ClauseCategory.ArbitrationLiability,
      weight = 24,
      patterns = List(
        "(?i)\\barbitration\\b".r,
        "(?i)\\bclass\\s+action\\s+waiver\\b".r
      ),
      explanation = "Arbitration and class-action waiver terms can limit legal remedies."
    ),
    Rule(
      category = ClauseCategory.ArbitrationLiability,
      weight = 22,
      patterns = List(
        "(?i)\\bliability\\b".r,
        "(?i)\\blimitation\\s+of\\s+liability\\b".r,
        "(?i)\\bindemnif(y|ication)\\b".r,
        "(?i)\\bhold\\s+harmless\\b".r
      ),
      explanation = "Liability and indemnity clauses can shift legal and financial risk to you."
    )
  )

  def rulesFor(category: ClauseCategory): List[Rule] =
    all.filter(_.category == category)
