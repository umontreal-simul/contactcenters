package umontreal.iro.lecuyer.xmlconfig;

import com.sun.javadoc.*;

import java.io.*;
import umontreal.iro.lecuyer.tcode.DocToLaTeX;
import umontreal.iro.lecuyer.tcode.FileUtil;
import umontreal.iro.lecuyer.tcode.DocUtil;
import umontreal.iro.lecuyer.tcode.Escaping;
import umontreal.iro.lecuyer.xmlconfig.textaglets.PMTexTaglet;

/**
 * This doclet can be used to document parameter object intended to be used with
 * {@link ParamReader}. It requires the package TCode to format the
 * documentation. Assuming \texttt{contactcenters.jar} and \texttt{tcode.jar}
 * are on the \texttt{CLASSPATH}, it must be used with the Javadoc tool as
 * follows:
 * 
 * \noindent\texttt{javadoc -doclet
 * umontreal.iro.lecuyer.config.XMLConfigDoclet} \texttt{-d} \emph{outputdir}
 * \emph{packagesandclasses}
 * 
 * \noindent where \emph{outputdir} represents the location where the
 * documentation files will be written into. For each selected class with a
 * \texttt{xmlconfig.title} or \texttt{xmlconfig.element} tag in its
 * documentation comment, a \LaTeX{} file of the form
 * \texttt{package/classxmldoc.tex} is created, where \texttt{package} is the
 * package name with \texttt{.} replaced by \texttt{/}. Each \LaTeX{} file
 * contains the title of the class as returned by {@link #getTitle}, its
 * documentation and the documentation of every field or method having a
 * \texttt{xmlconfig.element} or \texttt{xmlconfig.title} tag.
 * 
 * The doclet supports some special tags to identify XML elements and attributes
 * in a parameter file. \begin{description} \item[\texttt{\@xmlconfig.element}
 * \emph{tagname}] Gives the name of a documented XML element.
 * \item[\texttt{\@xmlconfig.required}] Indicates that the documented XML
 * element is required in the parameter file.
 * \item[\texttt{\@xmlconfig.attributedesc} \emph{attrname} \emph{desc}]
 * Documents an attribute with name \emph{attrname} and description \emph{desc}.
 * \item[\texttt{\@xmlconfig.title} \emph{title}] Gives a title to an
 * documentation block. \item[\texttt{\@xmlconfig.attribute}
 * \emph{attributename} Gives the name of a documented XML attribute.
 * \end{description} When processing a documentation block to be written into a
 * \LaTeX{} file, the \texttt{\@linkplain} inline tag has a special purpose.
 * Instead of writing a link to a class name, it copies the contents of the
 * corresponding documentation block.
 */
public class XMLConfigDoclet {
   private static RootDoc root;
   private static DocToLaTeX dtl;
   private static File docDir;

   public static LanguageVersion languageVersion() {
      return LanguageVersion.JAVA_1_5;
   }
   
   public static boolean start (RootDoc doc) {
      final String[][] options = doc.options ();
      for (final String[] option : options)
         if (option[0].equals ("-d"))
            docDir = new File (option[1]);
      if (docDir == null)
         docDir = new File (".");
      else
         docDir.mkdirs ();

      root = doc;
      dtl = new Converter (root);
      try {
         dtl.setBlockTags (false);
         dtl.setIndentOffset (3);
         dtl.setAutoHyphen (true);
         for (final PackageDoc pdoc : root.specifiedPackages ())
            // Use PackageDoc's allClasses instead of DocRoot.classes
            // to use the filtering.
            // The doclet will be sensitive to
            // -private, -protected and -public Javadoc options.
            if (!generateTexFiles (pdoc.allClasses (true)))
               return true;
         // Unfortunately, a class could be processed twice
         // if someone mistakenly specifies a package
         // and classes in that package.
         if (!generateTexFiles (root.specifiedClasses ()))
            return false;
      }
      finally {
         root = null;
         dtl = null;
      }
      return true;
   }

   public static int optionLength (String option) {
      if (option.equals ("-d"))
         return 2;
      return 0;
   }

   /**
    * For each given documented class with XMLConfig tags, generates a LaTeX
    * document containing the documentation. The document is designed to be
    * included into a master LaTeX file using the tcode package.
    */
   private static boolean generateTexFiles (ClassDoc... classDocs) {
      try {
         for (final ClassDoc cdoc : classDocs) {
            if (!xmlConfigDoc (cdoc))
               continue;
            // A portable way to get the path of the target
            // LaTeX document file.
            // In the fully qualified class name, we replace
            // dots with the system-dependent separator
            // and append the .tex extension.
            final String pname = cdoc.containingPackage ().name ();
            final String ppath = FileUtil.packageToPath (pname);
            final File pdir = new File (docDir, ppath);
            pdir.mkdirs ();
            final File texFile = new File (pdir, cdoc.name () + "xmldoc.tex");

            root.printNotice ("Generating " + texFile.getPath () + "...");
            final OutputStreamWriter out = new OutputStreamWriter (
                  new FileOutputStream (texFile), "iso-8859-1");
            writeContents (cdoc, out);
            out.close ();
         }
      }
      catch (final IOException ioe) {
         ioe.printStackTrace ();
         return false;
      }
      return true;
   }

   private static boolean xmlConfigDoc (Doc doc) {
      for (final Tag tag : doc.tags ())
         if (tag.name ().equals ("@xmlconfig.title")
               || tag.name ().equals ("@xmlconfig.element")
               || tag.name ().equals ("@xmlconfig.attribute"))
            return true;
      return false;
   }

   /**
    * Write the contents of a LaTeX class-specific document.
    */
   private static void writeContents (ClassDoc cdoc, Writer out)
         throws IOException {
      final String title = getTitle (cdoc);
      out.write ("\\defxmlconfigclass{" + title + "}\n");
      final String label = Escaping.getLabelKey (cdoc, root);
      if (label == null)
         out.write ("\n");
      else
         out.write ("\\label{" + label + "}\n");
      final ClassDoc sc = cdoc.superclass ();
      if (sc != null && xmlConfigDoc (sc)) {
         final String scLabel = Escaping.getLabelKey (sc, root);
         out.write ("Inherits elements and attributes from " + getTitle (sc));
         if (scLabel != null)
            out.write (" (see section~\\ref{" + scLabel + "}).");
         out.write ("\n\n");
      }
      final ClassDoc cc = cdoc.containingClass ();
      if (cc != null && xmlConfigDoc (cc)) {
         final String ccLabel = Escaping.getLabelKey (cc, root);
         out.write ("Nested element for " + getTitle (cc));
         if (ccLabel != null)
            out.write (" (see section~\\ref{" + ccLabel + "}).");
         out.write ("\n\n");
      }
      out.write (dtl.toLaTeX (cdoc));
      out.write (writeAttributes (cdoc));
      final StringBuilder sb = new StringBuilder ();
      sb.append (writeMembers (cdoc.enumConstants ()));
      sb.append (writeMembers (cdoc.fields ()));
      sb.append (writeMembers (cdoc.methods ()));
      if (sb.length () > 0) {
         out.write ("\n\\bigskip\\hrule\\bigskip\n\n");
         out.write (sb.toString ());
      }
   }

   private static String writeMembers (Doc[] docs) {
      final StringBuilder sb = new StringBuilder ();
      boolean first = true;
      for (final Doc doc : docs) {
         if (!xmlConfigDoc (doc))
            continue;
         if (first) {
            first = false;
            sb.append ("\\begin{xmlelements}\n");
         }
         sb.append ("\\item[").append (getTitle (doc)).append ("]");
         final String key = Escaping.getLabelKey (doc, root);
         if (key != null)
            sb.append ("\\label{").append (key).append ("}");
         sb.append ("\n");
         sb.append (dtl.toLaTeX (doc)).append ('\n');
         sb.append (writeAttributes (doc));
      }
      if (!first)
         sb.append ("\\end{xmlelements}\n");
      return sb.toString ();
   }

   private static String writeAttributes (Doc doc) {
      final StringBuilder sb = new StringBuilder ();
      final Tag[] tags = doc.tags ("@xmlconfig.attributedesc");
      if (tags.length > 0) {
         sb.append ("\\paragraph{Supported attribute").append (
               tags.length > 1 ? "s" : "").append (".}\n");
         sb.append ("\\begin{xmlattributes}\n");
      }
      for (final Tag tag : tags) {
         final String txt = tag.text ().trim ();
         int spcIdx;
         for (spcIdx = 0; spcIdx < txt.length ()
               && !Character.isWhitespace (txt.charAt (spcIdx)); spcIdx++)
            ;
         if (spcIdx == txt.length ())
            sb.append ("\\item[").append (txt).append ("]\n");
         else
            sb.append ("\\item[").append (txt.substring (0, spcIdx)).append (
                  "] ").append (txt.substring (spcIdx)).append ("\n");
      }
      if (tags.length > 0)
         sb.append ("\\end{xmlattributes}\n");
      return sb.toString ();
   }

   private static String getTagText (Doc doc, String tagName) {
      final Tag[] tags = doc.tags (tagName);
      if (tags.length > 1)
         root
               .printWarning (doc.position (), tagName
                     + " must appear only once");
      if (tags.length > 0) {
         String n = tags[0].text ();
         if (n == null || n.length () == 0) {
            String name = tags[0].holder ().name (); 
            return "\\texttt{" + Escaping.quoteLatex (name) + "}";
         }
         return n;
      }
      return null;
   }

   /**
    * Returns the title given to the documentation block in the output file. If
    * \texttt{doc} contains a \texttt{\@xmlconfig.element} and
    * \texttt{\@xmlconfig.title} tags, the string \emph{title}\verb! (element
    * \texttt{!\emph{tagname}\verb!})! is returned. If \texttt{doc} contains a
    * \texttt{\@xmlconfig.element} tag only, this returns \verb!Element
    * \texttt{!\emph{tagname}\verb!}!. If \texttt{doc} only contains the
    * \texttt{\@xmlconfig.title} tag, the title is returned. If the
    * \texttt{\@xmlconfig.required} tag is present, the string \texttt{element}
    * is replaced with \texttt{required element}.
    * 
    * @param doc
    *           the documentation block being processed.
    * @return the associated title.
    */
   public static String getTitle (Doc doc) {
      final String xmlTitle = getTagText (doc, "@xmlconfig.title");
      final String xmlElement = getTagText (doc, "@xmlconfig.element");
      final String xmlAttribute = getTagText (doc, "@xmlconfig.attribute");
      final Tag[] tags = doc.tags ("@xmlconfig.required");
      final boolean required = tags.length > 0;

      if (xmlTitle != null && (xmlElement != null || xmlAttribute != null)) {
         final String type = xmlElement != null ? "element" : "attribute";
         final String el = xmlElement != null ? xmlElement : xmlAttribute;
         return xmlTitle + " (" + (required ? "required " : "") + type
               + " \\texttt{" + el + "})";
      }
      else if (xmlTitle != null)
         return xmlTitle;
      else if (xmlElement != null)
         return (required ? "Required element " : "Element ") + "\\texttt{"
               + xmlElement + "}";
      else if (xmlAttribute != null)
         return (required ? "Required attribute " : "Attribute ")
               + "\\texttt{" + xmlAttribute + "}";
      else
         throw new IllegalStateException ();
   }

   public static String getTitleSee (Doc doc, boolean showParent) {
      final String xmlTitle = getTagText (doc, "@xmlconfig.title");
      final String xmlElement = getTagText (doc, "@xmlconfig.element");
      final String xmlAttribute = getTagText (doc, "@xmlconfig.attribute");

      final String title;
      if (xmlTitle != null && (xmlElement != null || xmlAttribute != null)) {
         final String type = xmlElement != null ? "element" : "attribute";
         final String el = xmlElement != null ? xmlElement : xmlAttribute;
         title = xmlTitle + " (" + type + " \\texttt{" + el + "})";
      }
      else if (xmlTitle != null)
         title = xmlTitle;
      else if (xmlElement != null)
         title = "element \\texttt{" + xmlElement + "}";
      else if (xmlAttribute != null)
         title = "attribute \\texttt{" + xmlAttribute + "}";
      else
         throw new IllegalStateException ();
      if (showParent && doc instanceof MemberDoc) {
         final ClassDoc cdoc = ((MemberDoc) doc).containingClass ();
         if (xmlConfigDoc (cdoc))
            return title + " contained in " + getTitleSee (cdoc, false);
      }
      return title;
   }

   static class Converter extends DocToLaTeX {
      Converter (DocErrorReporter rep) {
         super (rep);
      }

      @Override
      public String toLaTeX (Doc doc) {
         final StringBuilder tex = new StringBuilder ();
         for (final Tag tag : doc.inlineTags ())
            if (tag.name ().equals ("@see")
                  || tag.name ().equals ("@link"))
               tex.append (processSeeTag ((SeeTag) tag));
            else if (tag.name ().equals ("@linkplain"))
               tex.append (copyDoc ((SeeTag) tag));
            else
               tex.append (processInlineTag (tag));
         if (isBlockTags ())
            tex.append (processBlockTags (doc));
         Tag[] tags = doc.tags ("xmlconfig.pm"); 
         if (tags.length > 0)
            tex.append (new PMTexTaglet().toString (tags));
         return tex.toString ();
      }

      private String processSeeTag (SeeTag tag) {
         boolean pageNeeded = true;
         Doc doc = tag.referencedMember ();
         if (doc == null) {
            doc = tag.referencedClass ();
            pageNeeded = false;
         }
         if (doc == null)
            doc = tag.referencedPackage ();
         if (doc == null)
            return processInlineTag (tag);
         final boolean refNeeded = !DocUtil.sameClass (tag.holder (), doc);
         if (!xmlConfigDoc (doc))
            return processInlineTag (tag);
         // boolean showParent = !DocUtil.sameClass (doc, tag.holder());
         final String title = getTitleSee (doc, false);
         final String label = Escaping.getLabelKey (doc, root);
         String see;
         if (refNeeded) {
            see = " (";
            if (title.indexOf ("\\texttt") >= 0)
               see += "see ";
            if (pageNeeded)
               see += "p.~\\pageref{" + label + "})";
            else
               see += "section~\\ref{" + label + "})";
         }
         else
            see = "";
         return title + see;
      }

      private String copyDoc (SeeTag tag) {
         Doc doc = tag.referencedMember ();
         if (doc == null)
            doc = tag.referencedClass ();
         if (doc == null)
            doc = tag.referencedPackage ();
         if (doc == null)
            return processInlineTag (tag);
         return toLaTeX (doc);
      }
   }
}
