package temperature;

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

public class ThermometerAgent extends Agent {

    private Float currentTemperature;
    /**
     *
     */
    private static final long serialVersionUID = -8355663085154105053L;

    protected void setup() {
        Object[] inRoom = this.getArguments();
        String roomName = inRoom[0].toString();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("thermometer-manager");
        sd.setName(roomName + "-thermometer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new GetCurrentTemperatureFIPA(this, 3000));
        addBehaviour(new TempService());
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("ThermometerAgent " + getAID().getName() + " terminating.");
    }

    private class TempService extends OneShotBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = 6894891571072136375L;

        @Override
        public void action() {

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            addBehaviour(new AchieveREResponder(myAgent, template) {

                /**
                 *
                 */
                private static final long serialVersionUID = -694158247143718355L;

                protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
                    if (request.getContent().equalsIgnoreCase("temperatura")) {
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

                    inform.setContent(currentTemperature.toString());

                    return inform;

                }
            });

        }

    }

    private class GetCurrentTemperatureFIPA extends TickerBehaviour {

        public GetCurrentTemperatureFIPA(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        /**
         *
         */
        private static final long serialVersionUID = -7683060797864759807L;

        @Override
        public void onTick() {

            ACLMessage requestTemperatureMessage = new ACLMessage(ACLMessage.REQUEST);

            requestTemperatureMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestTemperatureMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestTemperatureMessage.setContent("therm1\n");
            requestTemperatureMessage.addReceiver(new AID("Gestore-Seriale", AID.ISLOCALNAME));

            addBehaviour(new AchieveREInitiator(myAgent, requestTemperatureMessage) {

                /**
                 *
                 */
                private static final long serialVersionUID = -5261992017174744491L;

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        try {
                            currentTemperature = Float.parseFloat(messageContenut);
                        } catch (NumberFormatException e) {
                            System.out.println("AgenteTermometro::::errore");
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
