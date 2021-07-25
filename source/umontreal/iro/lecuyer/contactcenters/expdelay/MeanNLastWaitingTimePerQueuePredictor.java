package umontreal.iro.lecuyer.contactcenters.expdelay;

import java.util.ArrayList;

import umontreal.iro.lecuyer.contactcenters.contact.Contact;
import umontreal.iro.lecuyer.contactcenters.expdelay.MeanNLastWaitingTimePredictor;
import umontreal.iro.lecuyer.contactcenters.queue.DequeueEvent;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueue;
import umontreal.iro.lecuyer.contactcenters.queue.WaitingQueueListener;
import umontreal.iro.lecuyer.contactcenters.router.Router;

public class MeanNLastWaitingTimePerQueuePredictor extends MeanNLastWaitingTimePredictor{
	 private final QueueListener ql = new QueueListener();

	 private double waitingTimePredictor[];
	   private ArrayList<Double> list[];
	 //  private Router router;
	   private int []windowSizeVQ; //ajouter
	   private double  []waitingTimeDefaultVQ; //ajouter
	//   private final ArrayList<Double> list= new ArrayList<Double>(); //ajouter
	   private boolean collectingAbandonment = false;
	   private boolean collectingService = true;


	    public void setWindowSizeVQ(int []taille){
		  	  windowSizeVQ=taille;
		    }
	    //Ajout de la methode qui modifie la taille de la fenetre

	    public void setWaitingTimeDefaultVQ(double attente[]){
	    	waitingTimeDefaultVQ=attente;
		    }


	  //modifier
	   public double getWaitingTime (Contact contact, WaitingQueue queue) {
		      double somme=0;
		      final int q = queue.getId ();
		     if (list!=null)
		     {  for(Double d:list[q])
			   somme+=d;
		       if(waitingTimePredictor!=null)
		       { waitingTimePredictor[q]=somme/windowSizeVQ[q];
		          return waitingTimePredictor[q];
		        }
			  }
		     init (contact.getRouter());
		     return 0;

	   }
	  /* public double getWaitingTime (Contact contact) {
		   int q=contact.getTypeId();
		   return waitingTimePredictor[q];
	   }*/

	   //Ajout de la methdoe update pour mettre a jour le Arraylist des N dernier temps de service

	   public double updateWaitingTime(DequeueEvent ev) {
		   int q=ev.getWaitingQueue().getId();
		   if(list[q].size()>0)
		   { list[q].remove(0);
		   double d=(Double)(ev.getEffectiveQueueTime ());
		  // double d=(Double)(ev.getContact().getTotalServiceTime());
		   list[q].add(d);
		   }
		   return getWaitingTime(ev.getContact(),ev.getWaitingQueue());

	   }

  // modifier pour initialiser le WaitingTime
	   public void init (Router router ) {
		 //  Router router=super.getRouter();
		  // System.out.println("ok1");
		  if(router!=null)
		 { // System.out.println("ok2");
			  int Q=router.getNumWaitingQueues();
		   //System.out.println("ok2");
		    this.waitingTimePredictor = new double [Q];
		    for(int i=0;i<Q;i++)
		    	waitingTimePredictor[i]=waitingTimeDefaultVQ[i];
		    // windowSizeVQ=new int[Q];
		     //waitingTimeDefaultVQ=new double[Q];
		     list=new ArrayList [Q];
		     for(int q=0;q<Q;q++)
		      {   list[q]=new ArrayList<Double>();
		          for(int i=0;i<windowSizeVQ[q]-1;i++)
	    	         list[q].add( waitingTimeDefaultVQ[q]);
		       }
		   }

	   }
	   public void init(){}

	   public void setRouter (Router newRouter) {
		     Router router=super.getRouter();
		      if (router != null && newRouter != router) {
		         final int nq = router.getNumWaitingQueues ();
		        // if( (waitingTimePredictor==null) || (waitingTimeDefaultVQ==null))//ajout
		        // { waitingTimePredictor= new double[nq];
		      //     waitingTimeDefaultVQ=new double[nq];
		      //   }
		         for (int q = 0; q < nq; q++) {
		            final WaitingQueue queue = router.getWaitingQueue (q);
		            if (queue != null)
		               queue.removeWaitingQueueListener (ql);
		         }
		         waitingTimePredictor= new double[nq];
		         System.out.println("ok1111");
		         for(int i=0;i<nq;i++)
		            waitingTimePredictor[i]= waitingTimeDefaultVQ[i];
		      }
		      if (newRouter != null && router != newRouter) {
		         final int nq = newRouter.getNumWaitingQueues ();

		        // if( (waitingTimePredictor==null) || (waitingTimeDefaultVQ==null)) //ajout
		       //  { waitingTimePredictor= new double[nq];
		         //  waitingTimeDefaultVQ=new double[nq];
		        // }

		         for (int q = 0; q < nq; q++) {
		            final WaitingQueue queue = newRouter.getWaitingQueue (q);
		            if (queue != null)
		               queue.addWaitingQueueListener (ql);
		         }
		         if(router==null)
		        	 waitingTimePredictor=null;
		         else
		         {   waitingTimePredictor= new double[nq];
		            // System.out.println("ok22222");
		        	 for(int i=0;i<nq;i++)
		               waitingTimePredictor[i]= waitingTimeDefaultVQ[i];
		         }
		      }
		      router = newRouter;
		   }

	   protected void dequeued (DequeueEvent ev) {
	      if (ev.getEffectiveDequeueType () == Router.DEQUEUETYPE_BEGINSERVICE) {
	         if (collectingService)
	           { if(waitingTimePredictor!=null)
	        	 waitingTimePredictor [ev.getWaitingQueue().getId()]= updateWaitingTime(ev);    //modifier


	         }
	      }
	      else if (ev.getEffectiveDequeueType () != Router.DEQUEUETYPE_TRANSFER)
	         if (collectingAbandonment)
	         {   if(waitingTimePredictor!=null)
	        	    waitingTimePredictor[ev.getWaitingQueue().getId()] =  updateWaitingTime(ev);   //modifier

	         }

	   }

	   //modifier
	 private class QueueListener implements WaitingQueueListener {
	      public void dequeued (DequeueEvent ev) {
	    	  MeanNLastWaitingTimePerQueuePredictor.this.dequeued (ev);
	      }

	      public void enqueued (DequeueEvent ev) {
	      }

	      public void init (WaitingQueue queue) {
	      }
	   }
}
