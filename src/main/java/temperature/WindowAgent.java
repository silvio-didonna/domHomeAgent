package temperature;

import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;

public class WindowAgent extends Agent {

    /**
     *
     */
    private static final long serialVersionUID = -886414830436718922L;
    Boolean windowStatus = true; // per non far spegnere il ventilatore dopo il primo ciclo

    protected void setup() {

        Object[] inRoom = this.getArguments();
        String roomName = inRoom[0].toString();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("window-manager");
        sd.setName(roomName + "-window"); // ad es: salone-light
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new toggleWindowFIPA());

    }

    private class toggleWindowFIPA extends OneShotBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = 2294861505144201563L;

        public void action() {

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            AchieveREResponder arer = new AchieveREResponder(myAgent, template) {

                /**
                 *
                 */
                private static final long serialVersionUID = -2315567321875139483L;

                protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {

                    ACLMessage agree = request.createReply();
                    agree.setPerformative(ACLMessage.AGREE);

                    return agree;

                }

            };

            arer.registerPrepareResultNotification(new SendToSerialAgent(myAgent, null));
            addBehaviour(arer);
        }
    }

    private class SendToSerialAgent extends AchieveREInitiator {

        /**
		 * 
		 */
		private static final long serialVersionUID = -6413735650983519988L;

		public SendToSerialAgent(Agent a, ACLMessage msg) {
            super(a, msg);
            // TODO Auto-generated constructor stub
        }

        public SendToSerialAgent(Agent a, ACLMessage msg, DataStore store) {
            super(a, msg, store);
            // TODO Auto-generated constructor stub
        }

		// Since we don't know what message to send to the responder
        // when we construct this AchieveREInitiator, we redefine this 
        // method to build the request on the fly
        @SuppressWarnings({ "rawtypes", "unchecked" })
		protected Vector prepareRequests(ACLMessage request) {
            // Retrieve the incoming request from the DataStore
            String incomingRequestKey = (String) ((AchieveREResponder) parent).REQUEST_KEY;
            ACLMessage incomingRequest = (ACLMessage) getDataStore().get(incomingRequestKey);
			// Prepare the request to forward to the responder
            ACLMessage outgoingRequest = new ACLMessage(ACLMessage.REQUEST);
            outgoingRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            outgoingRequest.setContent("window1\n");
            outgoingRequest.addReceiver(new AID("Gestore-Seriale", AID.ISLOCALNAME));
            outgoingRequest.setReplyByDate(incomingRequest.getReplyByDate());
            Vector v = new Vector(1);
            v.addElement(outgoingRequest);
            return v;
        }

        protected void handleInform(ACLMessage inform) {
            String messageContenut = inform.getContent();
            messageContenut = messageContenut.trim();
            windowStatus = Boolean.valueOf(messageContenut);
            System.out.println("AgenteFinestra::::" + windowStatus);
            storeNotification(ACLMessage.INFORM, windowStatus.toString());

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

        @SuppressWarnings("rawtypes")
		protected void handleAllResultNotifications(Vector notifications) {
            if (notifications.size() == 0) {
                // Timeout
                storeNotification(ACLMessage.FAILURE, null);
            }
        }

        private void storeNotification(int performative, String message) {
            if (performative == ACLMessage.INFORM) {
                System.out.println("Agent " + getLocalName() + ": brokerage successful");
            } else {
                System.out.println("Agent " + getLocalName() + ": brokerage failed");
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
        System.out.println("WindowAgent " + getAID().getName() + " terminating.");
    }
}
