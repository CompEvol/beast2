package beast.app.beauti2.util;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.text.BreakIterator;
import java.util.Locale;

/**
 * @author Walter Xie
 */
public class TextUtil {

    // auto wrap string by given a JComp and the length limit

    public static String wrapText(String someText, JComponent jComp, int lenLimit) {
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.getDefault());
        iterator.setText(someText);

        int start = iterator.first();
        int end = iterator.next();

        FontMetrics fm = jComp.getFontMetrics(jComp.getFont());
        String s = "<html>";
        int len = 0;
        while (end != BreakIterator.DONE) {
            String word = someText.substring(start, end);
            if (len + fm.stringWidth(word) > lenLimit) {
                s += "<br> ";
                len = fm.stringWidth(word);
            } else {
                len += fm.stringWidth(word);
            }

            s += word;
            start = end;
            end = iterator.next();
        }
        s += "</html>";
        return s;
    }

    public static JScrollPane createHTMLScrollPane(String text, Dimension dimension) {
        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(jEditorPane);

        HTMLEditorKit kit = new HTMLEditorKit();
        jEditorPane.setEditorKit(kit);
        // create a document, set it on the jeditorpane, then add the html
        Document doc = kit.createDefaultDocument();
        jEditorPane.setDocument(doc);
        jEditorPane.setText(text);
        jEditorPane.setPreferredSize(dimension); // to make html auto wrap

        return scrollPane;
    }
}
