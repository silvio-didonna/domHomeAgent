
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import internet.ThingSpeak;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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

public class RoomAgent extends Agent {

    private float temperature;
    private int lumens;
    private boolean motion;
    private boolean flame;
    private boolean laser;

    private boolean tempOrLumen = true;

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
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new GetCurrentTemperature(this, 5000));
        addBehaviour(new GetCurrentLumen(this, 5000));
        addBehaviour(new GetCurrentFireStatus(this, 5000));
        addBehaviour(new GetCurrentMotionStatus(this, 5000));
        addBehaviour(new GetCurrentLaserStatus(this, 5000));
        
        addBehaviour(new RoomService());

        addBehaviour(new sendToThingSpeak(this, 16000));
    }

    public class RoomService extends OneShotBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = 3522316552296891322L;

        public void action() {
            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            addBehaviour(new AchieveREResponder(myAgent, template) {

                /**
                 *
                 */
                private static final long serialVersionUID = 288442867080805012L;

                protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
                    //System.out.println("Agent "+getLocalName()+": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
                    if (request.getContent().equalsIgnoreCase("temperatura") || request.getContent().equalsIgnoreCase("lumen") || request.getContent().equalsIgnoreCase("fuoco") || request.getContent().equalsIgnoreCase("movimento") || request.getContent().equalsIgnoreCase("laser")) {
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

                    switch (request.getContent()) {
                        case ("temperatura"):
                            inform.setContent(String.valueOf(temperature));
                            break;
                        case ("lumen"):
                            inform.setContent(String.valueOf(lumens));
                            break;
                        case ("fuoco"):
                            inform.setContent(String.valueOf(flame));
                            break;
                        case ("movimento"):
                            inform.setContent(String.valueOf(motion));
                            break;
                        case ("laser"):
                            inform.setContent(String.valueOf(laser));
                            break;
                    }

                    return inform;

                }
            });
        }
    }

    private class GetCurrentTemperature extends TickerBehaviour {

        public GetCurrentTemperature(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        /**
         *
         */
        private static final long serialVersionUID = -7843279741024063018L;

        @Override
        protected void onTick() {

            //ricerca agenti termometro
            String roomName = myAgent.getLocalName(); // nome agente stanza
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdRoom = new ServiceDescription();
            sdRoom.setName(roomName + "-thermometer"); // ad es: salone-thermometer
            template.addServices(sdRoom);
            AID[] thermometerAgents = null; // da modificare----------------------null
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found the following thermometer agents:");
                thermometerAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    thermometerAgents[i] = result[i].getName();
                    //System.out.println(thermometerAgents[i].getName());

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            ACLMessage requestTemperatureMessage = new ACLMessage(ACLMessage.REQUEST);

            requestTemperatureMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestTemperatureMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestTemperatureMessage.setContent("temperatura");
            //requestTemperatureMessage.addReceiver(new AID("Termometro",AID.ISLOCALNAME));
            requestTemperatureMessage.addReceiver(thermometerAgents[0]);

            addBehaviour(new AchieveREInitiator(myAgent, requestTemperatureMessage) {
                /**
                 *
                 */
                private static final long serialVersionUID = 4875568271828534008L;

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        temperature = new Float(messageContenut);
                        //System.out.println("Room-Temp::::" + messageContenut);
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

                protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }

    }

    private class GetCurrentLumen extends TickerBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = -5306499643103460629L;

        public GetCurrentLumen(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {

            //ricerca agenti sensore luce
            String roomName = myAgent.getLocalName(); // nome agente stanza
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdRoom = new ServiceDescription();
            sdRoom.setName(roomName + "-light-sensor"); // ad es: salone-light-sensor
            template.addServices(sdRoom);
            AID[] lightSensorAgents = null; // da modificare----------------------null
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found the following light sensor agents:");
                lightSensorAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    lightSensorAgents[i] = result[i].getName();
                    //System.out.println(lightSensorAgents[i].getName());

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            ACLMessage requestTemperatureMessage = new ACLMessage(ACLMessage.REQUEST);

            requestTemperatureMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestTemperatureMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestTemperatureMessage.setContent("lumen");
            //requestTemperatureMessage.addReceiver(new AID("Sensore-Luci",AID.ISLOCALNAME));
            requestTemperatureMessage.addReceiver(lightSensorAgents[0]);

            addBehaviour(new AchieveREInitiator(myAgent, requestTemperatureMessage) {
                /**
                 *
                 */
                private static final long serialVersionUID = 4875568271828534008L;

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    //System.out.println("Agentehall::::"+messageContenut);
                    if (messageContenut != null) {
                        lumens = Integer.parseInt(messageContenut);
                        //System.out.println("Room-Lumen::::" + messageContenut);
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

                protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }

    }

    private class GetCurrentFireStatus extends TickerBehaviour {

        public GetCurrentFireStatus(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {

            //ricerca agenti termometro
            String roomName = myAgent.getLocalName(); // nome agente stanza
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdRoom = new ServiceDescription();
            sdRoom.setName(roomName + "-flame"); // ad es: salone-flame
            template.addServices(sdRoom);
            AID[] fireSensorAgents = null; // da modificare----------------------null
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found the following fire sensor agents:");
                fireSensorAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    fireSensorAgents[i] = result[i].getName();
                    //System.out.println(fireSensorAgents[i].getName());

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            ACLMessage requestFireStatusMessage = new ACLMessage(ACLMessage.REQUEST);

            requestFireStatusMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestFireStatusMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestFireStatusMessage.setContent("fuoco");
            //requestTemperatureMessage.addReceiver(new AID("Termometro",AID.ISLOCALNAME));
            requestFireStatusMessage.addReceiver(fireSensorAgents[0]);

            addBehaviour(new AchieveREInitiator(myAgent, requestFireStatusMessage) {

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        flame = Boolean.valueOf(inform.getContent());
                        //System.out.println("Room-Flame::::" + messageContenut);
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

                protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }

    }
    
    
    private class GetCurrentMotionStatus extends TickerBehaviour {

        public GetCurrentMotionStatus(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {

            //ricerca agenti termometro
            String roomName = myAgent.getLocalName(); // nome agente stanza
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdRoom = new ServiceDescription();
            sdRoom.setName(roomName + "-motion"); // ad es: salone-motion
            template.addServices(sdRoom);
            AID[] motionSensorAgents = null; // da modificare----------------------null
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found the following fire sensor agents:");
                motionSensorAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    motionSensorAgents[i] = result[i].getName();
                    //System.out.println(fireSensorAgents[i].getName());

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            ACLMessage requestMotionStatusMessage = new ACLMessage(ACLMessage.REQUEST);

            requestMotionStatusMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestMotionStatusMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestMotionStatusMessage.setContent("movimento");
            //requestTemperatureMessage.addReceiver(new AID("Termometro",AID.ISLOCALNAME));
            requestMotionStatusMessage.addReceiver(motionSensorAgents[0]);

            addBehaviour(new AchieveREInitiator(myAgent, requestMotionStatusMessage) {

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        motion = Boolean.valueOf(inform.getContent());
                        //System.out.println("Room-Motion::::" + messageContenut);
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

                protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }

    }
    
    private class GetCurrentLaserStatus extends TickerBehaviour {

        public GetCurrentLaserStatus(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {

            //ricerca agenti termometro
            String roomName = myAgent.getLocalName(); // nome agente stanza
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sdRoom = new ServiceDescription();
            sdRoom.setName(roomName + "-laser"); // ad es: salone-laser
            template.addServices(sdRoom);
            AID[] laserSensorAgents = null; // da modificare----------------------null
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found the following laser sensor agents:");
                laserSensorAgents = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    laserSensorAgents[i] = result[i].getName();
                    //System.out.println(laserSensorAgents[i].getName());

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            ACLMessage requestLaserStatusMessage = new ACLMessage(ACLMessage.REQUEST);

            requestLaserStatusMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            requestLaserStatusMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            requestLaserStatusMessage.setContent("laser");
            //requestTemperatureMessage.addReceiver(new AID("Termometro",AID.ISLOCALNAME));
            requestLaserStatusMessage.addReceiver(laserSensorAgents[0]);

            addBehaviour(new AchieveREInitiator(myAgent, requestLaserStatusMessage) {

                protected void handleInform(ACLMessage inform) {
                    String messageContenut = inform.getContent();
                    if (messageContenut != null) {
                        laser = Boolean.valueOf(inform.getContent());
                        //System.out.println("Room-Laser::::" + messageContenut);
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

                protected void handleAllResultNotifications(Vector notifications) {
                    //if (notifications.size() < nResponders) {
                    // Some responder didn't reply within the specified timeout
                    //System.out.println("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
                    //}
                }
            });

        }

    }

    private class sendToThingSpeak extends TickerBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = -8713839390230869708L;

        public sendToThingSpeak(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {
            if (tempOrLumen) {
                //invia temperatura

                try {
                    ThingSpeak.getHTML("https://api.thingspeak.com/update?api_key=OW7HWDZ4UTP04RT6&field1=" + String.valueOf(temperature));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                //invia lumen

                try {
                    ThingSpeak.getHTML("https://api.thingspeak.com/update?api_key=OW7HWDZ4UTP04RT6&field2=" + String.valueOf(lumens));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            tempOrLumen = !tempOrLumen;

        }

    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("RoomAgent " + getAID().getName() + " terminating.");
    }

}
