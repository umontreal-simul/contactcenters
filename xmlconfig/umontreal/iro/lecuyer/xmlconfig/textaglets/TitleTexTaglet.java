package umontreal.iro.lecuyer.xmlconfig.textaglets;

import java.util.Map;
import com.sun.javadoc.Tag;
import umontreal.iro.lecuyer.tcode.TexTaglet;

public class TitleTexTaglet extends BaseTexTaglet {
   public static void register (Map<String, TexTaglet> map) {
      final TexTaglet taglet = new TitleTexTaglet ();
      final TexTaglet t = map.get (taglet.getName ());
      if (t != null)
         map.remove (taglet.getName ());
      map.put (taglet.getName (), taglet);
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
