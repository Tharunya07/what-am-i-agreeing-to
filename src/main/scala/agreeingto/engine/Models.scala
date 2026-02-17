package agreeingto.engine

enum DocType:
  case PrivacyPolicy, TermsAgreement, EmailRequest, Unknown

enum RiskLevel:
  case Low, Medium, High

enum ClauseCategory:
  case AutoRenewal, Cancellation, FeesPenalties, DataSharing, ArbitrationLiability

case class Sentence(id: Int, text: String, start: Int, end: Int)

case class ClauseFinding(
    category: ClauseCategory,
    score: Int,
    evidence: List[Sentence],
    explanation: String
)

case class AnalysisResult(
    docType: DocType,
    docTypeConfidence: Int,
    risk: RiskLevel,
    riskScore: Int,
    riskWhy: String,
    findings: List[ClauseFinding],
    readCarefully: List[Sentence],
    nextAction: String
)

case class CategoryScoreDiff(
    category: ClauseCategory,
    oldScore: Int,
    newScore: Int,
    delta: Int
)

case class CompareResult(
    oldAnalysis: AnalysisResult,
    newAnalysis: AnalysisResult,
    deltaScore: Int,
    categoryDiffs: List[CategoryScoreDiff],
    addedCategories: List[ClauseCategory],
    removedCategories: List[ClauseCategory]
)
