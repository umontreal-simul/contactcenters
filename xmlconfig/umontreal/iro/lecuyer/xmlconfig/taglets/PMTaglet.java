package umontreal.iro.lecuyer.xmlconfig.taglets;

import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

public class PMTaglet extends BaseTaglet {
   public static void register (Map<String, Taglet> map) {
      final Taglet t = new PMTaglet();
      final Taglet tst = map.get (t.getName ());
      if (tst != null)
         map.remove (t.getName ());
      map.put (t.getName (), t);
   }

   public String getName () {
      return "xmlconfig.pm";
   }

   public String toString (Tag tag) {
      String pmName = tag.holder ().name ();
      PerformanceMeasureType pm;
      try {
         pm = PerformanceMeasureType.valueOf (pmName);
      }
      catch (IllegalArgumentException iae) {
         return "";
      }
      StringBuilder sb = new StringBuilder ("<DL>");
      sb.append ("<DT>Row type</DT><DD>").append (pm.getRowType ().name ()).append ("</DD>\n");
      sb.append ("<DT>Column type</DT><DD>").append (pm.getColumnType ().name ()).append ("</DD>\n");
      sb.append ("<DT>Estimation type</DT><DD>").append (pm.getEstimationType ().name ()).append ("</DD>\n");
      sb.append ("</DL>\n");
      return sb.toString ();
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return "";
      return toString (tags[0]);
   }
}
