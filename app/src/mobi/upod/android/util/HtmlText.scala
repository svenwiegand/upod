package mobi.upod.android.util

object HtmlText {
  private val htmlStartPattern = raw"<\s*html\s*>".r


  implicit class HtmlText(val text: String) extends AnyVal {

    private def containsHtmlTag: Boolean = htmlStartPattern.findFirstIn(text).isDefined

    private def isHtml: Boolean = {
      text.contains("</") ||
      text.contains("<a") ||
      text.contains("<i>") ||
      text.contains("<b>") ||
      text.contains("<strong") ||
      text.contains("<ul") ||
      text.contains("<ol") ||
      text.contains("<h1") ||
      text.contains("<h2") ||
      text.contains("<h3") ||
      text.contains("<h4") ||
      text.contains("<p>")
    }

    private def convertPlainTextToHtml: String = {
      def indentLines(str: String): String = {
        if (str.contains("<br/> "))
          indentLines(str.replaceAll("<br/>( *) ", "<br/>$1&nbsp;"))
        else
          str
      }

      def linkifyUrls(str: String): String = {
        str.replaceAll(
          "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
          """<a href="$1">$1</a>""")
      }

      val converted = text.
        replace("\r\n", "<br/>").
        replace("\r", "<br/>").
        replace("\n", "<br/>").
        replace("\t", "&nbsp;&nbsp;")
      linkifyUrls(indentLines(converted))
    }

    def html: String = if (isHtml) text else convertPlainTextToHtml

    def htmlWithStyle(cssAssets: String*)(bodyStyle: (String, String)*): String = {
      if (containsHtmlTag)
        text
      else {
        def cssReference(cssAsset: String) = s"""<link rel="stylesheet" type="text/css" href="file:///android_asset/$cssAsset" />"""
        val cssReferences = cssAssets.map(cssReference(_)).mkString("\n", "\n", "\n")
        val bodyCss = bodyStyle.map(style => s"${style._1}:${style._2}").mkString(";")
        s"""<html><head>$cssReferences</head><body style="$bodyCss">$html</body></html>"""
      }
    }

    def withBottomHtml(bottomHtml: String): String = {
      if (html.contains("</body>"))
        html.replace("</body>", s"$bottomHtml</body>")
      else if (html.contains("</html>"))
        html.replace("</html>", s"$bottomHtml</html>")
      else
        html + bottomHtml
    }
  }
}
