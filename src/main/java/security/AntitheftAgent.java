package security;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class AntitheftAgent extends Agent {

	private AID[] serverAgents;
	Map <String,CurrentStatusInRoom> currentStatuses = new HashMap<String,CurrentStatusInRoom>();
	String responseToSorter = "";
	Boolean antitheftSystemOn=false;
	Boolean buzzerStatus=false;

	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("antitheft-manager");
		sd.setName("JADE-antitheft");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		//ricerca agenti
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sdRoom = new ServiceDescription();
		sdRoom.setType("room-manager");
		template.addServices(sdRoom);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			//System.out.println("Found the following seller agents:");
			serverAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				serverAgents[i] = result[i].getName();
				//System.out.println(serverAgents[i].getName());
				CurrentStatusInRoom currentStatusInRoom = new CurrentStatusInRoom(serverAgents[i]);
				currentStatuses.put(serverAgents[i].getLocalName(), currentStatusInRoom);

			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new RequestCurrentMotionStatuses(this, 5000));
		addBehaviour(new RequestCurrentLaserStatuses(this, 5000));
		addBehaviour(new SetBuzzer(this, 5000));
		addBehaviour(new FireSystemService());
	}

	private class RequestCurrentMotionStatuses extends TickerBehaviour {

		private int nResponders;

		public RequestCurrentMotionStatuses(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			ACLMessage requestMotionStatusMessage = new ACLMessage(ACLMessage.REQUEST);

			for (int i = 0; i < serverAgents.length; ++i) {
				requestMotionStatusMessage.addReceiver(serverAgents[i]);
			}

			requestMotionStatusMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// We want to receive a reply in 10 secs
			requestMotionStatusMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			requestMotionStatusMessage.setContent("movimento");

			addBehaviour(new AchieveREInitiator(myAgent, requestMotionStatusMessage) {

				protected void handleInform(ACLMessage inform) {
					//System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
					String messageContenut = inform.getContent();
					System.out.println("Agente Gestore-AntifurtoMotion::::" + messageContenut);
					if (messageContenut != null) {
						try {

							Boolean motionStatus = Boolean.valueOf(inform.getContent());

							currentStatuses.get(inform.getSender().getLocalName()).setCurrentMotionStatus(motionStatus);


						} catch (NumberFormatException e) {
							System.out.println("Agente Gestore-Antifurto::::errore");
						}
					}
				}

				protected void handleRefuse(ACLMessage refuse) {
					System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
					nResponders--;
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
					if (notifications.size() < nResponders) {
						// Some responder didn't reply within the specified timeout
						System.out.println("Timeout expired: missing " + (nResponders - notifications.size()) + " responses");
					}
				}
			});

		}
	}

	private class RequestCurrentLaserStatuses extends TickerBehaviour {

		private int nResponders;

		public RequestCurrentLaserStatuses(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			ACLMessage requestLaserStatusMessage = new ACLMessage(ACLMessage.REQUEST);

			for (int i = 0; i < serverAgents.length; ++i) {
				requestLaserStatusMessage.addReceiver(serverAgents[i]);
			}

			requestLaserStatusMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// We want to receive a reply in 10 secs
			requestLaserStatusMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			requestLaserStatusMessage.setContent("laser");

			addBehaviour(new AchieveREInitiator(myAgent, requestLaserStatusMessage) {

				protected void handleInform(ACLMessage inform) {
					//System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
					String messageContenut = inform.getContent();
					System.out.println("Agente Gestore-AntifurtoLaser::::" + messageContenut);
					if (messageContenut != null) {
						try {

							Boolean laserStatus = Boolean.valueOf(inform.getContent());

							currentStatuses.get(inform.getSender().getLocalName()).setCurrentLaserStatus(laserStatus);

						} catch (NumberFormatException e) {
							System.out.println("Agente Gestore-Antifurto::::errore");
						}
					}
				}

				protected void handleRefuse(ACLMessage refuse) {
					System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
					nResponders--;
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
					if (notifications.size() < nResponders) {
						// Some responder didn't reply within the specified timeout
						System.out.println("Timeout expired: missing " + (nResponders - notifications.size()) + " responses");
					}
				}
			});

		}
	}

	private class SetBuzzer extends TickerBehaviour {
		private int nResponders;

		public SetBuzzer(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();

				ACLMessage requestBuzzerToggle = new ACLMessage(ACLMessage.REQUEST);
				requestBuzzerToggle.setContent(""); // per far funzionare l'IF dopo
				if (antitheftSystemOn) { //se e' attivo l'impianto antifurto
					if (currentStatuses.get(roomName).getCurrentMotionStatus() || currentStatuses.get(roomName).getCurrentLaserStatus()) { //se c'e' un intruso
						if(!buzzerStatus)
							requestBuzzerToggle.setContent("true");
						//se non c'e'
					} else if(buzzerStatus)
						requestBuzzerToggle.setContent("false");
				}
				//se non e' attivo l'impianto antifurto
				else if (buzzerStatus)
					requestBuzzerToggle.setContent("false"); //spegne l'allarme se acceso

				if (requestBuzzerToggle.getContent().equalsIgnoreCase("true") || requestBuzzerToggle.getContent().equalsIgnoreCase("false")) {

					//ricerca agenti buzzer
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sdRoom = new ServiceDescription();
					sdRoom.setName("buzzer");
					template.addServices(sdRoom);
					AID[] buzzerAgents=null; // da modificare----------------------null
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						buzzerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							buzzerAgents[i] = result[i].getName();
							//System.out.println(lightAgents[i].getName());

						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					requestBuzzerToggle.addReceiver(buzzerAgents[0]); // da modificare----------------------
					
					requestBuzzerToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestBuzzerToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					//requestLightToggle.setContent("dummy-action");

					addBehaviour(new AchieveREInitiator(myAgent, requestBuzzerToggle) {


						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							buzzerStatus = Boolean.valueOf(inform.getContent());
						}

						protected void handleAgree(ACLMessage agree) {
							System.out.println("Agent " + agree.getSender().getName() + " agreed");
						}

						protected void handleRefuse(ACLMessage refuse) {
							System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
							nResponders--;
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
							if (notifications.size() < nResponders) {
								// Some responder didn't reply within the specified timeout
								System.out.println("Timeout expired: missing " + (nResponders - notifications.size()) + " responses");
							}
						}
					});
				}
			}
		}
	}

	private class FireSystemService extends OneShotBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1567060780776225027L;


		Boolean makeAction(String messageFromGateway) {
			String type;
			String room;
			String value;


			String msg = messageFromGateway;
			msg = msg.replace("set-", "");
			String[] parts = msg.split("-");
			type = parts[0];
			room = parts[1];
			value = parts[2];

			switch(type) {
			case "security":
				antitheftSystemOn = !antitheftSystemOn;
				responseToSorter = antitheftSystemOn.toString();
				break;
			}


			return true;
		}

		@Override
		public void action() {

			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

			addBehaviour(new AchieveREResponder(myAgent, template) {

				/**
				 * 
				 */
				private static final long serialVersionUID = 6070529588872379895L;

				protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
					//    if (request.getContent().equalsIgnoreCase("lumen") && currentLumen >= 0) {
					// We agree to perform the action.

					makeAction(request.getContent());
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					return agree;
					//    } else {
					// We refuse to perform the action
					//        throw new RefuseException("Message content not supported or corrupted value");
					//    }
				}

				protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {


					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);

					inform.setContent(responseToSorter);

					return inform;

				}
			});

		}

	}

	private class CurrentStatusInRoom {

		private Boolean currentMotionStatus;
		private Boolean currentLaserStatus;
		private AID roomAgent;

		public CurrentStatusInRoom() {
			currentMotionStatus = null;
			currentLaserStatus = null;
			roomAgent = null;
			//fanOn = false;
		}

		public CurrentStatusInRoom(AID roomAgent) {
			setCurrentMotionStatus(null);
			setRoomAgent(roomAgent);
			//fanOn = false;
		}

		public CurrentStatusInRoom(AID roomAgent, Boolean currentMotionStatus) {
			setCurrentMotionStatus(currentMotionStatus);
			setRoomAgent(roomAgent);
			//fanOn = false;
		}

		public Boolean getCurrentMotionStatus() {
			return currentMotionStatus;
		}

		public void setCurrentMotionStatus(Boolean currentMotionStatus) {
			this.currentMotionStatus = currentMotionStatus;
		}

		public Boolean getCurrentLaserStatus() {
			return currentLaserStatus;
		}

		public void setCurrentLaserStatus(Boolean currentLaserStatus) {
			this.currentLaserStatus = currentLaserStatus;
		}

		public AID getRoomAgent() {
			return roomAgent;
		}

		public void setRoomAgent(AID roomAgent) {
			this.roomAgent = roomAgent;
		}

	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("AntitheftAgent " + getAID().getName() + " terminating.");
	}
}
