import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;

public class SorterAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4019608168429702476L;
	
	String messageFromGateway = "";
	String messageToGateway = "";

	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("sorter");
		sd.setName("Sorter");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new SortGatewayMessages());
	}

	private class SortGatewayMessages extends OneShotBehaviour {


		public void action() {

			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST) );

			AchieveREResponder arer = new AchieveREResponder(myAgent, template) {
				protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					
					messageFromGateway = request.getContent();
					System.out.println(messageFromGateway);
					return agree;

				}

				protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {

					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					inform.setContent(messageToGateway);
					return inform;

				}
			};
			arer.registerPrepareResultNotification(new SendToAgent(myAgent, null));
			addBehaviour(arer);
		}
	}


	private class SendToAgent extends AchieveREInitiator {

		public SendToAgent(Agent a, ACLMessage msg) {
			super(a, msg);
			// TODO Auto-generated constructor stub
		}

		public SendToAgent(Agent a, ACLMessage msg, DataStore store) {
			super(a, msg, store);
			// TODO Auto-generated constructor stub
		}

		String chooseReceiver(String messageFromGateway) {
			String type;
			String room;
			String value;

			String receiver;
			receiver = "";

			String msg = messageFromGateway;
			msg = msg.replace("set-", "");
			String[] parts = msg.split("-");
			type = parts[0];
			room = parts[1];
			value = parts[2];

			System.out.println("parametro1 = " + type);
			System.out.println("parametro2 = " + room);
			System.out.println("parametro3 = " + value);

			switch(type) {
			case "autoTemp":
			case "temp":
			case "window":
			{
				receiver="Gestore-Temperatura";
				break;
			}
			case "security":
			{
				receiver="Antifurto";
				break;
			}
			case "fireSystem":
			{
				receiver="Antincendio";
				break;
			}			
			case "light":
			case "shutter":
			case "autoLightning":
			case "lightning":
			{
				receiver="Gestore-Luci";
				break;
			}

			case "garageDoor":
			{
				receiver="Garage"; // MODIFICARE
				break;
			}			
			}
			return receiver;
		}
/*
		String chooseMessage(String messageFromGateway) {
			String type;
			String room;
			String value;

			String message;
			

			String msg = messageFromGateway;
			msg = msg.replace("set-", "");
			String[] parts = msg.split("-");
			type = parts[0];
			room = parts[1];
			value = parts[2];

			System.out.println("parametro1 = " + type);
			System.out.println("parametro2 = " + room);
			System.out.println("parametro3 = " + value);
			
			message = room;

			switch(room) {
			case "general":
			{
				break;
			}
			case "hall":
			{
				break;
			}
			case "room":
			{
				break;
			}
			case "bathroom":
			{
				break;
			}
			case "kitchen":
			{
				break;
			}
			}


			return "";
		}
		*/


		// Since we don't know what message to send to the responder
		// when we construct this AchieveREInitiator, we redefine this 
		// method to build the request on the fly
		protected Vector prepareRequests(ACLMessage request) {
			// Retrieve the incoming request from the DataStore
			String incomingRequestKey = (String) ((AchieveREResponder) parent).REQUEST_KEY;
			ACLMessage incomingRequest = (ACLMessage) getDataStore().get(incomingRequestKey);
			// Prepare the request to forward to the responder
			//System.out.println("Agent "+getLocalName()+": Forward the request to "+responder.getName());
			ACLMessage outgoingRequest = new ACLMessage(ACLMessage.REQUEST);
			outgoingRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			outgoingRequest.setContent(messageFromGateway);
			outgoingRequest.addReceiver(new AID(chooseReceiver(messageFromGateway),AID.ISLOCALNAME));
			outgoingRequest.setReplyByDate(incomingRequest.getReplyByDate());
			Vector v = new Vector(1);
			v.addElement(outgoingRequest);
			return v;
		}

		protected void handleInform(ACLMessage inform) {
			String messageContenut=inform.getContent();
			messageContenut=messageContenut.trim();
			if (messageContenut!=null)
				messageToGateway = messageContenut;
			
			storeNotification(ACLMessage.INFORM,messageToGateway);

		}

		protected void handleRefuse(ACLMessage refuse) {
			storeNotification(ACLMessage.FAILURE, null);
		}

		protected void handleNotUnderstood(ACLMessage notUnderstood) {
			storeNotification(ACLMessage.FAILURE, null);
		}

		protected void handleFailure(ACLMessage failure) {
			storeNotification(ACLMessage.FAILURE, null);
		}

		protected void handleAllResultNotifications(Vector notifications) {
			if (notifications.size() == 0) {
				// Timeout
				storeNotification(ACLMessage.FAILURE, null);
			}
		}

		private void storeNotification(int performative, String message) {
			if (performative == ACLMessage.INFORM) {
				//System.out.println("Agent "+getLocalName()+": brokerage successful");
			}
			else {
				//System.out.println("Agent "+getLocalName()+": brokerage failed");
			}

			// Retrieve the incoming request from the DataStore
			String incomingRequestkey = (String) ((AchieveREResponder) parent).REQUEST_KEY;
			ACLMessage incomingRequest = (ACLMessage) getDataStore().get(incomingRequestkey);
			// Prepare the notification to the request originator and store it in the DataStore
			ACLMessage notification = incomingRequest.createReply();
			notification.setPerformative(performative);
			notification.setContent(message);
			String notificationkey = (String) ((AchieveREResponder) parent).RESULT_NOTIFICATION_KEY;
			getDataStore().put(notificationkey, notification);
		}
	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("SorterAgent " + getAID().getName() + " terminating.");
	}

}
