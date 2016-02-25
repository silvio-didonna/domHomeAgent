package light;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

public class LightningAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 697611633768237195L;
	private AID[] serverAgents;
	Map <String,CurrentStatusInRoom> currentStatuses = new HashMap<String,CurrentStatusInRoom>();
	String responseToSorter = "";

	protected void setup() {

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("lightning-manager");
		sd.setName("JADE-lightning");
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
			serverAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				serverAgents[i] = result[i].getName();
				CurrentStatusInRoom currentStatusInRoom = new CurrentStatusInRoom(serverAgents[i]);
				currentStatuses.put(serverAgents[i].getLocalName(), currentStatusInRoom);

			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new RequestCurrentLumen(this, 5000));
		addBehaviour(new SetLight(this, 6000));
		addBehaviour(new SetShutter(this, 6000));
		addBehaviour(new LightningService());
	}

	private class RequestCurrentLumen extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = -4219050278752369718L;
		private int nResponders;

		public RequestCurrentLumen(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			ACLMessage requestLumenMessage = new ACLMessage(ACLMessage.REQUEST);

			for (int i = 0; i < serverAgents.length; ++i) {
				requestLumenMessage.addReceiver(serverAgents[i]);
			}

			requestLumenMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// We want to receive a reply in 10 secs
			requestLumenMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			requestLumenMessage.setContent("lumen");

			addBehaviour(new AchieveREInitiator(myAgent, requestLumenMessage) {
				/**
				 *
				 */
				private static final long serialVersionUID = 4875568271828534008L;

				protected void handleInform(ACLMessage inform) {
					String messageContenut = inform.getContent();
					if (messageContenut != null) {
						try {
							int lumen = Integer.parseInt(messageContenut);

							currentStatuses.get(inform.getSender().getLocalName()).setCurrentLumen(lumen);

							System.out.println("AgenteGestore-Luce-HASH::::" + currentStatuses.get(inform.getSender().getLocalName()).getCurrentLumen());

						} catch (NumberFormatException e) {
							System.out.println("AgenteGestore-Luce::::errore");
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

	private class SetLight extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = -8454124241057674169L;
		private int nResponders;

		public SetLight(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				
				ACLMessage requestLightToggle = new ACLMessage(ACLMessage.REQUEST);
				requestLightToggle.setContent(""); // per far funzionare l'IF dopo
				if (currentStatuses.get(roomName).getAutoLight()) { //se e' attiva la gestione automatica
					if (!currentStatuses.get(roomName).getlightStatus()) {//se e' spenta
						if (currentStatuses.get(roomName).getCurrentLumen() < (currentStatuses.get(roomName).getLumenLevel())*100) {

							requestLightToggle.setContent("true");

						}
						//se e' accesa
					} else if (currentStatuses.get(roomName).getCurrentLumen() > (currentStatuses.get(roomName).getLumenLevel())*100) {

						requestLightToggle.setContent("false");
					}
				}
				else { //se non e' attiva la gestione automatica
					if (!currentStatuses.get(roomName).getlightStatus()) {
						if (currentStatuses.get(roomName).getLightOn()) {

							requestLightToggle.setContent("true");

						}
					} else if (!currentStatuses.get(roomName).getLightOn()) {

						requestLightToggle.setContent("false");
					}

				}


				if (requestLightToggle.getContent().equalsIgnoreCase("true") || requestLightToggle.getContent().equalsIgnoreCase("false")) {

					//ricerca agenti luce
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sdRoom = new ServiceDescription();
					sdRoom.setName(roomName + "-light"); // ad es: salone-light
					template.addServices(sdRoom);
					AID[] lightAgents=null; 
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						lightAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							lightAgents[i] = result[i].getName();

						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					requestLightToggle.addReceiver(lightAgents[0]); // da modificare----------------------
					
					requestLightToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestLightToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

					addBehaviour(new AchieveREInitiator(myAgent, requestLightToggle) {

						/**
						 *
						 */
						private static final long serialVersionUID = 1457890379172110455L;

						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setlightStatus(Boolean.valueOf(inform.getContent())); // CONTROLLARE!!!!!!!!!!!!
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

	private class SetShutter extends TickerBehaviour {

		private int nResponders;

		public SetShutter(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {


			int hour = Integer.parseInt((new SimpleDateFormat ("HH").format(new Date())).trim());
			int sunrise = 6;
			int sunset = 18;

			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();

				ACLMessage requestShutterToggle = new ACLMessage(ACLMessage.REQUEST);
				requestShutterToggle.setContent(""); // per far funzionare l'IF dopo
				if (currentStatuses.get(roomName).getAutoLight()) { //se e' attiva la gestione automatica
					if (!currentStatuses.get(roomName).getShutterStatus()) {
						if ((currentStatuses.get(roomName).getCurrentLumen() < (currentStatuses.get(roomName).getLumenLevel())*100) && (hour>=sunrise || hour <=sunset)) { // DA MODIFICARE!!!!

							requestShutterToggle.setContent("true");

						}
					} else if ((currentStatuses.get(roomName).getCurrentLumen() > (currentStatuses.get(roomName).getLumenLevel())*100)) { // DA MODIFICARE!!!!

						requestShutterToggle.setContent("false");
					}
				}
				else { //se non e' attiva la gestione automatica
					if (!currentStatuses.get(roomName).getShutterStatus()) {
						if (currentStatuses.get(roomName).getShutterOpen()) {

							requestShutterToggle.setContent("true");

						}
					} else if (!currentStatuses.get(roomName).getShutterOpen()) {

						requestShutterToggle.setContent("false");
					}

				}

				if (requestShutterToggle.getContent().equalsIgnoreCase("true") || requestShutterToggle.getContent().equalsIgnoreCase("false")) {

					//ricerca agenti luce
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sdRoom = new ServiceDescription();
					sdRoom.setName(roomName + "-shutter"); // ad es: salone-light
					template.addServices(sdRoom);
					AID[] shutterAgents=null; 
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						shutterAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							shutterAgents[i] = result[i].getName();

						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					requestShutterToggle.addReceiver(shutterAgents[0]); // da modificare----------------------
					
					requestShutterToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestShutterToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

					addBehaviour(new AchieveREInitiator(myAgent, requestShutterToggle) {


						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setShutterStatus(Boolean.valueOf(inform.getContent()));
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

	private class LightningService extends OneShotBehaviour {

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
			case "light":
				Boolean lightOn = !(currentStatuses.get(room).getlightStatus());
				currentStatuses.get(room).setLightOn(lightOn);
				responseToSorter = lightOn.toString();
				break;
			case "shutter":
				Boolean shutterOpen = !(currentStatuses.get(room).getShutterStatus());
				currentStatuses.get(room).setShutterOpen(shutterOpen);
				responseToSorter = shutterOpen.toString();
				break;
			case "autoLightning":
				Boolean autoLight = !(currentStatuses.get(room).getAutoLight());
				currentStatuses.get(room).setAutoLight(autoLight);
				responseToSorter = autoLight.toString();
				break;
			case "lightning":
				int valueInt = Integer.parseInt(value.trim());
				currentStatuses.get(room).setLumenLevel(valueInt);
				responseToSorter = Integer.toString(valueInt);
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
					// We agree to perform the action.

					makeAction(request.getContent());
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					return agree;
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

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("LightningAgent " + getAID().getName() + " terminating.");
	}

	private class CurrentStatusInRoom {

		private int currentLumen;
		private int lumenLevel;
		private AID roomAgent;
		private Boolean lightStatus;
		private Boolean autoLight;
		private Boolean lightOn;
		private Boolean shutterOpen;
		private Boolean shutterStatus;

		public CurrentStatusInRoom() {
			currentLumen = 0;
			roomAgent = null;
			lightStatus = false;
			shutterOpen = false;
			lumenLevel = 1;
			autoLight = false;
			lightOn = false;
			shutterStatus = false;
		}

		public CurrentStatusInRoom(AID roomAgent) {
			setCurrentLumen(0);
			setroomAgent(roomAgent);
			lightStatus = false;
			shutterOpen = false;
			lumenLevel = 1;
			autoLight = false;
			lightOn = false;
			shutterStatus = false;
		}

		public int getCurrentLumen() {
			return currentLumen;
		}

		public void setCurrentLumen(int currentLumen) {
			this.currentLumen = currentLumen;
		}

		public AID getroomAgent() {
			return roomAgent;
		}

		public void setroomAgent(AID roomAgent) {
			this.roomAgent = roomAgent;
		}

		public Boolean getlightStatus() {
			return lightStatus;
		}

		public void setlightStatus(Boolean lightStatus) {
			this.lightStatus = lightStatus;
		}

		public Boolean getShutterOpen() {
			return shutterOpen;
		}

		public void setShutterOpen(Boolean shutterOpen) {
			this.shutterOpen = shutterOpen;
		}

		public Boolean getShutterStatus() {
			return shutterStatus;
		}

		public void setShutterStatus(Boolean shutterStatus) {
			this.shutterStatus = shutterStatus;
		}

		public Boolean getLightOn() {
			return lightOn;
		}

		public void setLightOn(Boolean lightOn) {
			this.lightOn = lightOn;
		}

		public int getLumenLevel() {
			return lumenLevel;
		}

		public void setLumenLevel(int lumenLevel) {
			this.lumenLevel = lumenLevel;
		}

		public Boolean getAutoLight() {
			return autoLight;
		}

		public void setAutoLight(Boolean autoLight) {
			this.autoLight = autoLight;
		}
	}
}
