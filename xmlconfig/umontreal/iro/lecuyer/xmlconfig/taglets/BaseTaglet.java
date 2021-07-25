package umontreal.iro.lecuyer.xmlconfig.taglets;

import com.sun.tools.doclets.Taglet;

public abstract class BaseTaglet implements Taglet {
   public boolean isInlineTag () {
      return false;
   }

   public boolean inType () {
      return true;
   }

   public boolean inPackage () {
      return false;
   }

   public boolean inOverview () {
      return false;
   }

   public boolean inMethod () {
      return true;
   }

   public boolean inField () {
      return true;
   }

   public boolean inConstructor () {
      return false;
   }
}
