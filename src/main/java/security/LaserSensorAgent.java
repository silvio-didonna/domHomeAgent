package security;

import java.util.Date;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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

public class LaserSensorAgent extends Agent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8151165026940842914L;
	Boolean laserStatus;
    int currentLumen;

    protected void setup() {
        laserStatus = false;
        Object[] inRoom = this.getArguments();
        String roomName = inRoom[0].toString();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("laser-sensor-manager");
        sd.setName(roomName + "-laser");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CheckLaserStatus(this, 3000));
        addBehaviour(new LaserService());
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("LaserSensorAgent " + getAID().getName() + " terminating.");
    }

    private class LaserService extends OneShotBehaviour {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7194676557443506386L;

		@Override
        public void action() {

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            addBehaviour(new AchieveREResponder(myAgent, template) {

                /**
				 * 
				 */
				private static final long serialVersionUID = 1942183116484356529L;

				protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
                    if (request.getContent().equalsIgnoreCase("laser")) {
                        // We agree to perform the action.
                        ACLMessage agree = request.createReply();
                        agree.setPerformative(ACLMessage.AGREE);
                        return agree;
                    } else {
                        // We refuse to perform the action
                        throw new RefuseException("Message content not supported");
                    }
                }

                protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                    ACLMessage inform = request.createReply();
                    inform.setPerformative(ACLMessage.INFORM);

                    inform.setContent(laserStatus.toString());

                    return inform;

                }
            });

        }

    }

    private class CheckLaserStatus extends TickerBehaviour {

        /**
		 * 
		 */
		private static final long serialVersionUID = -5886813500382258596L;

		public CheckLaserStatus(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onTick() {

            ACLMessage requestLaserMessage = new ACLMessage(ACLMessage.REQUEST);

            requestLaserMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestLaserMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestLaserMessage.setContent("laser1\n");
            requestLaserMessage.addReceiver(new AID("Gestore-Seriale", AID.ISLOCALNAME));

            addBehaviour(new AchieveREInitiator(myAgent, requestLaserMessage) {

                /**
				 * 
				 */
				private static final long serialVersionUID = -5437904919062032600L;

				protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        messageContenut = messageContenut.trim();
                        try {
                            currentLumen = Integer.parseInt(messageContenut);
                            if (currentLumen < 800)
                                laserStatus = true;
                            else
                                laserStatus = false;
                        } catch (NumberFormatException e) {
                            System.out.println("Agente LaserSensor::::errore");
                        }
                    }
                }

                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
                }

                protected void handleFailure(ACLMessage failure) {
                    if (failure.getSender().equals(myAgent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver
                        // does not exist
                        System.out.println("Responder does not exist");
                    } else {
                        System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                    }
                }

                @SuppressWarnings("rawtypes")
				protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }
    }

}
