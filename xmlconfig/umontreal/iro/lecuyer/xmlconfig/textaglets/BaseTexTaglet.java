package umontreal.iro.lecuyer.xmlconfig.textaglets;

import umontreal.iro.lecuyer.tcode.TexTaglet;

public abstract class BaseTexTaglet implements TexTaglet {
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
