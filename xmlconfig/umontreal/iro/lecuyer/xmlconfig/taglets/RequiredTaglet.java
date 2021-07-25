package umontreal.iro.lecuyer.xmlconfig.taglets;

import java.util.Map;
import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.Tag;

public class RequiredTaglet extends BaseTaglet {
   public static void register (Map<String, Taglet> map) {
      final Taglet t = new RequiredTaglet ();
      final Taglet tst = map.get (t.getName ());
      if (tst != null)
         map.remove (t.getName ());
      map.put (t.getName (), t);
   }

   public String toString (Tag tag) {
      return "<P><B>Required element</B><BR>";
   }

   public String toString (Tag[] tags) {
      if (tags.length == 0)
         return null;
      return toString (tags[0]);
   }

   public String getName () {
      return "xmlconfig.required";
   }
}
