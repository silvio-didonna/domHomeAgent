import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;



public class RoomAgent extends Agent {
	private float temperature;
	private float lumens;
	private boolean door;
	private boolean motion;
	private boolean flame;

	/**
	 * 
	 */
	private static final long serialVersionUID = -7504622688999058316L;
	
	protected void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("room-manager");
		sd.setName("JADE-room");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch(FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new AskCurrentTemperature(this,5000));
		addBehaviour(new GetCurrentTemperature());
		
		addBehaviour(new AskCurrentLumen(this, 5000));
		addBehaviour(new GetCurrentLumen());
	}
	
	private class AskCurrentTemperature extends TickerBehaviour {

		public AskCurrentTemperature(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = 9072626078728707911L;

		@Override
		public void onTick() {


			AID msgReceiver= new AID("Termometro",AID.ISLOCALNAME);

			ACLMessage serialAnswer = new ACLMessage(ACLMessage.REQUEST);
			serialAnswer.addReceiver(msgReceiver);
			//serialAnswer.setContent("therm1");
			myAgent.send(serialAnswer);


			//temperature = Float.parseFloat(currTemp);
			//System.out.println(currTemp);
		}
	}
	
	private class GetCurrentTemperature extends CyclicBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7188335783111581107L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			//System.out.println("Server behaviour 1 wait a message.");
			ACLMessage msg = myAgent.receive(mt);
			if (msg!=null) {

				String messageContenut=msg.getContent();
				System.out.println("Room::::"+messageContenut);

			}
			else {
				block();
			}
			
		}
	
	}
	
	private class AskCurrentLumen extends TickerBehaviour {

		public AskCurrentLumen(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = -4558099421600874487L;

		@Override
		public void onTick() {


			AID msgReceiver= new AID("Sensore-Luci",AID.ISLOCALNAME);

			ACLMessage serialAnswer = new ACLMessage(ACLMessage.REQUEST);
			serialAnswer.addReceiver(msgReceiver);
			//serialAnswer.setContent("therm1");
			myAgent.send(serialAnswer);


			//temperature = Float.parseFloat(currTemp);
			//System.out.println(currTemp);
		}
	}
	
	private class GetCurrentLumen extends CyclicBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3165583707741329448L;

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			//System.out.println("Server behaviour 1 wait a message.");
			ACLMessage msg = myAgent.receive(mt);
			if (msg!=null) {

				String messageContenut=msg.getContent();
				System.out.println("Room::::"+messageContenut);

			}
			else {
				block();
			}
			
		}
	
	}
	
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("RoomAgent "+getAID().getName()+" terminating.");
	}

}
