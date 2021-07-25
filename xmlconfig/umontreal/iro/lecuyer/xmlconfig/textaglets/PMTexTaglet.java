package umontreal.iro.lecuyer.xmlconfig.textaglets;

import java.util.Map;

import umontreal.iro.lecuyer.contactcenters.app.PerformanceMeasureType;
import umontreal.iro.lecuyer.tcode.TexTaglet;

import com.sun.javadoc.Tag;

public class PMTexTaglet extends BaseTexTaglet {
   public static void register (Map<String, TexTaglet> map) {
      final TexTaglet taglet = new PMTexTaglet ();
      final TexTaglet t = map.get (taglet.getName ());
      if (t != null)
         map.remove (taglet.getName ());
      map.put (taglet.getName (), taglet);
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
      StringBuilder sb = new StringBuilder ("\\begin{description}\n");
      sb.append ("\\item[Row type] ").append (pm.getRowType ().name ()).append ("\n");
      sb.append ("\\item[Column type] ").append (pm.getColumnType ().name ()).append ("\n");
      sb.append ("\\item[Estimation type] ").append (pm.getEstimationType ().name ()).append ("\n");
      sb.append ("\\end{description}\n");
      return sb.toString ();
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return "";
      return toString (tags[0]);
   }

}
