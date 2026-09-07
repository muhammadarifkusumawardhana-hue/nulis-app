package com.personalnotes.app.presentation.editor

import android.content.Context
import android.os.Environment
import com.personalnotes.app.domain.model.Note
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter

class NoteExporter(private val context: Context) {

    private val exportDir: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "PersonalNotes"
            )
            dir.mkdirs()
            return dir
        }

    fun exportToHtml(note: Note): String {
        val fileName = "${sanitizeFileName(note.title)}.html"
        val file = File(exportDir, fileName)

        val htmlContent = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang='id'>")
            appendLine("<head>")
            appendLine("<meta charset='UTF-8'>")
            appendLine("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            appendLine("<title>${note.title.escapeHtml()}</title>")
            appendLine("<style>")
            appendLine("""
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                       max-width: 800px; margin: 0 auto; padding: 40px 20px; line-height: 1.6;
                       color: #333; }
                h1, h2, h3, h4, h5, h6 { color: #1a1a2e; }
                code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
                pre { background: #f4f4f4; padding: 16px; border-radius: 6px; overflow-x: auto; }
                blockquote { border-left: 4px solid #6200EE; margin: 0; padding-left: 16px; color: #666; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px 12px; }
                th { background: #f4f4f4; }
                .meta { color: #888; font-size: 0.85em; margin-bottom: 24px; }
                .tags { margin-top: 8px; }
                .tag { display: inline-block; background: #e8d5ff; color: #6200EE; padding: 2px 8px;
                       border-radius: 12px; font-size: 0.8em; margin-right: 4px; }
            """.trimIndent())
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<h1>${note.title.escapeHtml()}</h1>")
            appendLine("<div class='meta'>")
            appendLine("<div>Dibuat: ${note.createdAt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))}</div>")
            appendLine("<div>Diperbarui: ${note.updatedAt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))}</div>")
            if (note.tags.isNotEmpty()) {
                appendLine("<div class='tags'>${note.tags.joinToString("") { "<span class='tag'>#$it</span>" }}</div>")
            }
            appendLine("</div>")
            appendLine("<hr>")
            appendLine(markdownToHtml(note.content))
            appendLine("</body>")
            appendLine("</html>")
        }

        FileWriter(file).use { it.write(htmlContent) }
        return file.absolutePath
    }

    fun exportToPdf(note: Note): String {
        // Simple PDF using iText - convert via HTML approach
        val htmlPath = exportToHtml(note)
        val pdfFileName = "${sanitizeFileName(note.title)}.pdf"
        val pdfFile = File(exportDir, pdfFileName)

        // For simplicity, create a basic PDF with text content
        // In production you can use iText HtmlConverter
        try {
            com.itextpdf.kernel.pdf.PdfWriter(pdfFile).use { writer ->
                com.itextpdf.kernel.pdf.PdfDocument(writer).use { pdf ->
                    com.itextpdf.layout.Document(pdf).use { document ->
                        val font = com.itextpdf.kernel.font.PdfFontFactory.createFont()

                        // Title
                        document.add(
                            com.itextpdf.layout.element.Paragraph(note.title)
                                .setFont(font)
                                .setFontSize(20f)
                                .setBold()
                        )

                        // Meta
                        document.add(
                            com.itextpdf.layout.element.Paragraph(
                                "Diperbarui: ${note.updatedAt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}"
                            ).setFont(font).setFontSize(10f)
                                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                        )

                        if (note.tags.isNotEmpty()) {
                            document.add(
                                com.itextpdf.layout.element.Paragraph("Tag: ${note.tags.joinToString(", ") { "#$it" }}")
                                    .setFont(font).setFontSize(10f)
                                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                            )
                        }

                        document.add(com.itextpdf.layout.element.Paragraph("\n"))

                        // Content (plain text, markdown stripped for basic PDF)
                        val plainContent = note.content
                            .replace(Regex("#+\\s"), "")
                            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                            .replace(Regex("\\*(.*?)\\*"), "$1")
                            .replace(Regex("`(.*?)`"), "$1")

                        document.add(
                            com.itextpdf.layout.element.Paragraph(plainContent)
                                .setFont(font).setFontSize(12f)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: return HTML path
            return htmlPath
        }

        return pdfFile.absolutePath
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")
            .trim()
            .replace(" ", "_")
            .take(50)
            .ifBlank { "catatan_${System.currentTimeMillis()}" }

    private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun markdownToHtml(markdown: String): String {
        // Basic markdown to HTML conversion
        var html = markdown
        html = html.replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
        html = html.replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
        html = html.replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
        html = html.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        html = html.replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
        html = html.replace(Regex("`(.+?)`"), "<code>$1</code>")
        html = html.replace(Regex("^> (.+)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")
        html = html.replace(Regex("^- (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
        html = html.replace(Regex("\\[\\[(.+?)\\]\\]"), "<a href='#'>$1</a>")
        html = html.replace("\n\n", "</p><p>")
        return "<p>$html</p>"
    }
}
