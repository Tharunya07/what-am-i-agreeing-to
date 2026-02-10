package agreeingto.segment

import agreeingto.engine.Sentence

import scala.collection.mutable.ListBuffer

object Segmenter:
  def sentences(text: String): List[Sentence] =
    val out = ListBuffer.empty[Sentence]
    var id = 1
    var start = 0
    var i = 0

    def flush(endExclusive: Int): Unit =
      if endExclusive <= start then ()
      else
        val raw = text.substring(start, endExclusive)
        val leading = raw.indexWhere(ch => !ch.isWhitespace)
        if leading >= 0 then
          val trailing = raw.lastIndexWhere(ch => !ch.isWhitespace) + 1
          val sentenceStart = start + leading
          val sentenceEnd = start + trailing
          out += Sentence(
            id = id,
            text = text.substring(sentenceStart, sentenceEnd),
            start = sentenceStart,
            end = sentenceEnd
          )
          id += 1

    while i < text.length do
      val ch = text.charAt(i)
      if ch == '\r' then
        flush(i)
        if i + 1 < text.length && text.charAt(i + 1) == '\n' then i += 1
        start = i + 1
      else if ch == '\n' then
        flush(i)
        start = i + 1
      else if ch == '.' || ch == '?' || ch == '!' then
        flush(i + 1)
        start = i + 1
      i += 1

    flush(text.length)
    out.toList
