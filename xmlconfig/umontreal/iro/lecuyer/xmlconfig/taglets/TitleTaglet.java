package umontreal.iro.lecuyer.xmlconfig.taglets;

import java.util.Map;
import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.Tag;

public class TitleTaglet extends BaseTaglet {
   public static void register (Map<String, Taglet> map) {
      final Taglet t = new TitleTaglet ();
      final Taglet tst = map.get (t.getName ());
      if (tst != null)
         map.remove (t.getName ());
      map.put (t.getName (), t);
   }

   public String toString (Tag tag) {
      return "";
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return null;
      return toString (tags[0]);
   }

   public String getName () {
      return "xmlconfig.title";
   }
}
