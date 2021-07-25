package umontreal.iro.lecuyer.xmlconfig.taglets;

import java.util.Map;
import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.Tag;

public class AttributeDescTaglet extends BaseTaglet {
   public static void register (Map<String, Taglet> map) {
      final Taglet t = new AttributeDescTaglet ();
      final Taglet tst = map.get (t.getName ());
      if (tst != null)
         map.remove (t.getName ());
      map.put (t.getName (), t);
   }

   public String toString (Tag tag) {
      final String[] s = splitAttribute (tag.text ());
      return "<P><B>Attribute: </B><DL><DT><CODE>" + s[0] + "</CODE></DT>" + "<DD>" + s[1]
            + "</DD></DL></P>\n";
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return null;
      final StringBuffer sb = new StringBuffer ("<B>Attributes: </B><DL>\n");
      for (final Tag element : tags) {
         final String[] s = splitAttribute (element.text ());
         sb.append ("<DT><CODE>").append (s[0]).append ("</CODE></DT><DD>")
               .append (s[1]).append ("</DD>\n");
      }
      sb.append ("</DL><BR/>\n");
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
