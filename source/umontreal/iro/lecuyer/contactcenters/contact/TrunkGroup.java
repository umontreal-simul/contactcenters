package umontreal.iro.lecuyer.contactcenters.contact;

import umontreal.iro.lecuyer.contactcenters.Initializable;
import umontreal.iro.lecuyer.contactcenters.Named;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Simulator;

/**
 * Represents a group of trunks, i.e., phone lines or more
 * generally communication channels, in a contact
 * center.  After a contact is constructed, it can be assigned
 * a trunk group using {@link Contact#setTrunkGroup}.
 * When the contact enters the router,
 * a line is allocated.  The contact is blocked if
 * a line is not available.
 */
public class TrunkGroup implements Initializable, Named {
   private String name = "";
   private int capacity;
   private int lines = 0;

   private Accumulate statCapacity;
   private Accumulate statLines;
   private boolean collect = false;

   /**
    * Constructs a new trunk group with capacity
    * \texttt{capacity}.  The capacity corresponds to the maximum number
    * of allocated lines at any simulation time.
    @param capacity the total number of lines in the trunk group.
    @exception IllegalArgumentException if the capacity is negative.
    */
   public TrunkGroup (int capacity) {
      if (capacity < 0)
         throw new IllegalArgumentException
            ("Capacity must not be negative.");
      this.capacity = capacity;
   }

   /**
    * Returns the current capacity of this trunk group.
    @return the current capacity.
    */
   public int getCapacity() {
      return capacity;
   }

   /**
    * Changes the capacity to \texttt{capacity}.
    * If the given capacity is negative or smaller than
    * the current number of allocated lines,
    * an {@link IllegalArgumentException} is thrown.
    @param capacity the new capacity.
    @exception IllegalArgumentException if capacity is too small or negative.
    */
   public void setCapacity (int capacity) {
      if (capacity < 0)
         throw new IllegalArgumentException
            ("Capacity must not be negative.");
      if (capacity < lines)
         throw new IllegalArgumentException
            ("The new capacity is too small.");
      this.capacity = capacity;
      if (collect)
         statCapacity.update (capacity);
   }

   /**
    * Returns the current number of allocated lines.
    @return the current number of lines.
    */
   public int lines() {
      return lines;
   }

   /**
    * Resets this trunk group, releasing all allocated
    * lines.  If statistical collecting is enabled, this also
    * calls {@link #initStat}.
    */
   public void init() {
      lines = 0;
      if (collect)
         initStat();
   }

   /**
    * Initializes the two statistical collectors for
    * the number of lines and the capacity.
    * If statistical collecting is disabled, this throws
    * an {@link IllegalStateException}.
    @exception IllegalStateException if statistical collecting
    is disabled.
    */
   public void initStat() {
      if (!collect)
         throw new IllegalStateException
            ("Statistical collecting is disabled");
      statCapacity.init (capacity);
      statLines.init (lines);
   }

   /**
    * Indicates that the contact \texttt{contact}
    * enters the system and takes one line from
    * this trunk group.  If all lines are busy,
    * this returns \texttt{false}.  Otherwise, this returns
    * \texttt{true}.
    @param contact the contact allocating the line.
    @return the success indicator.
    */
   public boolean take (Contact contact) {
      /*
       * If needed, it would be possible to make a subclass
       * keeping all the contacts having allocated a line.
       */
      if (lines >= capacity)
         return false;
      ++lines;
      if (collect)
         statLines.update (lines);
      return true;
   }

   /**
    * Releases the trunk line allocated by the
    * contact \texttt{contact}.
    @param contact the contact releasing the line.
    */
   public void release (Contact contact) {
      if (lines == 0)
         throw new IllegalStateException
            ("No line allocated");
      --lines;
      if (collect)
         statLines.update (lines);
   }

   /**
    * Determines if this trunk group is collecting
    * statistics about the number of allocated lines
    * and its capacity.  By default, statistical collecting
    * is turned OFF.
    @return the statistical collecting indicator.
    */
   public boolean isStatCollecting() {
      return collect;
   }

   /**
    * Sets the statistical collecting to \texttt{b}.  If \texttt{b} is \texttt{true},
    * the collecting is turned ON.  Otherwise, it is turned OFF.
    @param b the statistical collecting indicator.
    */
   public void setStatCollecting (boolean b) {
      if (b)
         setStatCollecting (Simulator.getDefaultSimulator ());
      else
         collect = false;
   }

   /**
    * Enables statistical collecting, but associates
    * the given simulator to the internal
    * accumulates.
    * @param sim the simulator associated to
    * the internal accumulates.
    */
   public void setStatCollecting (Simulator sim) {
      if (sim == null)
         throw new NullPointerException();
      collect = true;
      if (statLines == null) {
         statLines = new Accumulate (sim, getLinesProbeName ());
         statCapacity = new Accumulate (sim, getCapacityProbeName ());
      }
      else {
         statLines.setSimulator (sim);
         statCapacity.setSimulator (sim);
      }
      initStat ();
   }

   /**
    * Enables statistical collecting, and attach
    * the simulator \texttt{sim} to the internal
    * accumulates.
    * The given simulator is used to determine
    * the simulation time when the values of the
    * probes are updated.
    * @param sim the given simulator.
    */
   public void setStatCollectiong (Simulator sim) {
      collect = true;
      if (statLines == null) {
         statLines = new Accumulate (sim, getLinesProbeName());
         statCapacity = new Accumulate (sim, getCapacityProbeName());
      }
      else {
         statLines.setSimulator (sim);
         statCapacity.setSimulator (sim);
         initStat();
      }
   }

   /**
    * Returns the statistical collector for the capacity of this trunk group
    * through simulation time.  The returned value is non-\texttt{null}
    * only if {@link #setStatCollecting} was called with \texttt{true}.
    @return the statistical collector for the capacity of this trunk group.
    */
   public Accumulate getStatCapacity() {
      return statCapacity;
   }

   /**
    * Returns the statistical collector for the number of allocated
    * lines through simulation time.  The returned value is non-\texttt{null}
    * only if {@link #setStatCollecting} was called with \texttt{true}.
    @return the statistical collector for the number of allocated lines.
    */
   public Accumulate getStatLines() {
      return statLines;
   }

   public String getName() {
      return name;
   }

   public void setName (String name) {
      if (name == null)
         throw new NullPointerException ("The given name must not be null");
      this.name = name;
      if (statLines != null) {
         statLines.setName (getLinesProbeName());
         statCapacity.setName (getCapacityProbeName());
      }
   }

   private String getLinesProbeName() {
      final String n = getName();
      if (n.length() > 0)
         return "Allocated lines for trunk group " + n + ")";
      else
         return "Allocated lines";
   }

   private String getCapacityProbeName() {
      final String n = getName();
      if (n.length() > 0)
         return "Capacity for trunk group " + n + ")";
      else
         return "Trunk group capacity";
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder (getClass().getSimpleName());
      sb.append ('[');
      if (getName().length() > 0)
         sb.append ("name: ").append (getName()).append (", ");
      sb.append ("used lines: ").append (lines).append (", ");
      sb.append ("capacity: ").append (capacity);
      sb.append (']');
      return sb.toString();
   }
}
