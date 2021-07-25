package umontreal.iro.lecuyer.xmlconfig.textaglets;

import java.util.Map;
import com.sun.javadoc.Tag;
import umontreal.iro.lecuyer.tcode.TexTaglet;

public class AttributeDescTexTaglet extends BaseTexTaglet {
   public static void register (Map<String, TexTaglet> map) {
      final TexTaglet taglet = new AttributeDescTexTaglet ();
      final TexTaglet t = map.get (taglet.getName ());
      if (t != null)
         map.remove (taglet.getName ());
      map.put (taglet.getName (), taglet);
   }

   public String toString (Tag tag) {
      final String[] s = splitAttribute (tag.text ());
      return "\\par\n\\textbf{Attribute: }\n\\begin{description}\n\\item["
            + s[0] + "] " + s[1] + "\n\\end{description}\n";
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return null;
      final StringBuffer sb = new StringBuffer (
            "\\par\n\\textbf{Attributes: }\n\\begin{description}\n");
      for (final Tag element : tags) {
         final String[] s = splitAttribute (element.text ());
         sb.append ("\\item[").append (s[0]).append ("] ").append (s[1])
               .append ("\n");
      }
      sb.append ("\\end{description}\n");
      return sb.toString ();
   }

   String[] splitAttribute (String txt) {
      int spcIdx;
      for (spcIdx = 0; spcIdx < txt.length ()
            && !Character.isWhitespace (txt.charAt (spcIdx)); spcIdx++)
         ;
      if (spcIdx == txt.length ())
         return new String[] { txt, "" };
      else
         return new String[] { txt.substring (0, spcIdx),
               txt.substring (spcIdx) };
   }

   public String getName () {
      return "xmlconfig.attributedesc";
   }
}
