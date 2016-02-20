package temperature;

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
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

public class TemperatureAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 138736042772986486L;
	private AID[] serverAgents;
	//List<CurrentTemperatureInRoom> currentTemperatures = new LinkedList<CurrentTemperatureInRoom>();
	Map <String,CurrentStatusInRoom> currentStatuses = new HashMap<String,CurrentStatusInRoom>();
	Boolean boilerOn = false;

	protected void setup() {

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("temperature-manager");
		sd.setName("JADE-temperature");
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

		addBehaviour(new RequestCurrentTemperature(this, 5000));
		addBehaviour(new SetFan(this, 6000));
		addBehaviour(new SetWindow(this, 6000));
		addBehaviour(new SetBoiler(this, 6000));

	}

	private class RequestCurrentTemperature extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = -4219050278752369718L;
		private int nResponders;

		public RequestCurrentTemperature(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			ACLMessage requestTemperatureMessage = new ACLMessage(ACLMessage.REQUEST);

			for (int i = 0; i < serverAgents.length; ++i) {
				requestTemperatureMessage.addReceiver(serverAgents[i]);
			}

			requestTemperatureMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// We want to receive a reply in 10 secs
			requestTemperatureMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			requestTemperatureMessage.setContent("temperatura");

			addBehaviour(new AchieveREInitiator(myAgent, requestTemperatureMessage) {
				/**
				 *
				 */
				private static final long serialVersionUID = 4875568271828534008L;

				protected void handleInform(ACLMessage inform) {
					String messageContenut = inform.getContent();
					System.out.println("AgenteGestore-Temperatura::::" + messageContenut);
					if (messageContenut != null) {
						try {
							Float temp = new Float(messageContenut);

							currentStatuses.get(inform.getSender().getLocalName()).setCurrentTemperature(temp);

						} catch (NumberFormatException e) {
							System.out.println("AgenteGestore-Temperatura::::errore");
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

	private class SetFan extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = -8454124241057674169L;
		private int nResponders;

		public SetFan(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			Float tempMaxValue = new Float(20);
			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				//for (CurrentTemperatureInRoom currentTemperatureInRoom : currentTemperatures) { // per ogni stanza

				//ricerca agenti ventilatore
				//String roomName = currentTemperatureInRoom.getroomAgent().getLocalName(); // nome agente stanza
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sdRoom = new ServiceDescription();
				sdRoom.setName(roomName + "-fan"); // ad es: salone-fan
				template.addServices(sdRoom);
				AID[] fanAgents = null; // da modificare----------------------null
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					//System.out.println("Found the following fan agents:");
					fanAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						fanAgents[i] = result[i].getName();
						//System.out.println(fanAgents[i].getName());

					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				//AID msgReceiver= new AID("Ventilatore",AID.ISLOCALNAME);
				ACLMessage requestFanToggle = new ACLMessage(ACLMessage.REQUEST);
				//requestFanToggle.addReceiver(msgReceiver);
				requestFanToggle.addReceiver(fanAgents[0]); // da modificare----------------------

				//System.out.println("setFan:::: " + currentTemperatureInRoom.getCurrentTemperature());
				requestFanToggle.setContent(""); // per far funzionare l'IF dopo
				if (!currentStatuses.get(roomName).getfanOn()) {
					if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo(tempMaxValue) > 0))) {

						requestFanToggle.setContent("true"); //accendi

					}
				} else if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo(tempMaxValue) < 0))) {

					requestFanToggle.setContent("false"); //spegni
				}

				if (requestFanToggle.getContent().equalsIgnoreCase("true") || requestFanToggle.getContent().equalsIgnoreCase("false")) {

					requestFanToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestFanToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					//requestFanToggle.setContent("dummy-action");

					addBehaviour(new AchieveREInitiator(myAgent, requestFanToggle) {

						/**
						 *
						 */
						private static final long serialVersionUID = 1457890379172110455L;

						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setfanOn(Boolean.valueOf(inform.getContent()));
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

	private class SetWindow extends TickerBehaviour {

		/**
		 *
		 */
		private static final long serialVersionUID = 2793118778911471269L;
		private int nResponders;

		public SetWindow(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			Float tempMaxValue = new Float(20);
			//for (CurrentTemperatureInRoom currentTemperatureInRoom : currentTemperatures) { // per ogni stanza
			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				//ricerca agenti finestra
				//String roomName = currentTemperatureInRoom.getroomAgent().getLocalName(); // nome agente stanza
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sdRoom = new ServiceDescription();
				sdRoom.setName(roomName + "-window"); // ad es: salone-window
				template.addServices(sdRoom);
				AID[] windowAgents = null; // da modificare----------------------null
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					//System.out.println("Found the following window agents:");
					windowAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						windowAgents[i] = result[i].getName();
						//System.out.println(windowAgents[i].getName());

					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}

				//AID msgReceiver = new AID("Finestra", AID.ISLOCALNAME);
				ACLMessage requestWindowToggle = new ACLMessage(ACLMessage.REQUEST);
				//requestWindowToggle.addReceiver(msgReceiver);
				requestWindowToggle.addReceiver(windowAgents[0]); // da modificare----------------------

				//System.out.println("setWindow:::: " + currentTemperatureInRoom.getCurrentTemperature());
				requestWindowToggle.setContent(""); // per far funzionare l'IF dopo
				if (!currentStatuses.get(roomName).getfanOn()) {
					if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo(tempMaxValue) > 0))) {

						requestWindowToggle.setContent("true"); //accendi

					}
				} else if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo(tempMaxValue) < 0))) {

					requestWindowToggle.setContent("false"); //spegni
				}

				if (requestWindowToggle.getContent().equalsIgnoreCase("true") || requestWindowToggle.getContent().equalsIgnoreCase("false")) {

					requestWindowToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestWindowToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					//requestWindowToggle.setContent("dummy-action");

					addBehaviour(new AchieveREInitiator(myAgent, requestWindowToggle) {

						/**
						 *
						 */
						private static final long serialVersionUID = 1457890379172110455L;

						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setfanOn(Boolean.valueOf(inform.getContent())); //DA MODIFICARE
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

	private class SetBoiler extends TickerBehaviour {

		private int nResponders;

		public SetBoiler(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {

			Float tempMinValue = new Float(20);
			Float averageTemp = new Float(0);
			//for (CurrentTemperatureInRoom currentTemperatureInRoom : currentTemperatures) { // per ogni stanza
			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				averageTemp += currentStatuses.get(roomName).getCurrentTemperature();
			}
			averageTemp = averageTemp/currentStatuses.size();
			//System.out.println("setBoiler:::: " + averageTemp + " "+currentTemperatures.size());

			//ricerca agenti boiler
			//String roomName = currentTemperatureInRoom.getroomAgent().getLocalName(); // nome agente stanza
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sdRoom = new ServiceDescription();
			sdRoom.setName("boiler"); 
			template.addServices(sdRoom);
			AID[] boilerAgents = null; // da modificare----------------------null
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				//System.out.println("Found the following window agents:");
				boilerAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					boilerAgents[i] = result[i].getName();
					//System.out.println(windowAgents[i].getName());

				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

			//AID msgReceiver = new AID("Finestra", AID.ISLOCALNAME);
			ACLMessage requestBoilerToggle = new ACLMessage(ACLMessage.REQUEST);
			//requestWindowToggle.addReceiver(msgReceiver);
			requestBoilerToggle.addReceiver(boilerAgents[0]); // da modificare----------------------

			//System.out.println("setWindow:::: " + currentTemperatureInRoom.getCurrentTemperature());
			requestBoilerToggle.setContent(""); // per far funzionare l'IF dopo
			if (!boilerOn) {
				if (averageTemp.compareTo(tempMinValue) < 0) {

					requestBoilerToggle.setContent("true"); //accendi

				}
			} else if (averageTemp.compareTo(tempMinValue) > 0) {

				requestBoilerToggle.setContent("false"); //spegni
			}

			if (requestBoilerToggle.getContent().equalsIgnoreCase("true") || requestBoilerToggle.getContent().equalsIgnoreCase("false")) {

				requestBoilerToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				// We want to receive a reply in 10 secs
				requestBoilerToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
				//requestWindowToggle.setContent("dummy-action");

				addBehaviour(new AchieveREInitiator(myAgent, requestBoilerToggle) {


					protected void handleInform(ACLMessage inform) {
						System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
						boilerOn=Boolean.valueOf(inform.getContent()); //DA MODIFICARE
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

	private class CurrentStatusInRoom {

		private Float currentTemperature;
		private AID roomAgent;
		private Boolean fanOn; // manca window

		public CurrentStatusInRoom() {
			currentTemperature = null;
			roomAgent = null;
			fanOn = false;
		}

		public CurrentStatusInRoom(AID roomAgent) {
			setCurrentTemperature(null);
			setroomAgent(roomAgent);
			fanOn = false;
		}

		public CurrentStatusInRoom(AID roomAgent, Float currentTemperature) {
			setCurrentTemperature(currentTemperature);
			setroomAgent(roomAgent);
			fanOn = false;
		}

		public Float getCurrentTemperature() {
			return currentTemperature;
		}

		public void setCurrentTemperature(Float currentTemperature) {
			this.currentTemperature = currentTemperature;
		}

		public AID getroomAgent() {
			return roomAgent;
		}

		public void setroomAgent(AID roomAgent) {
			this.roomAgent = roomAgent;
		}

		public Boolean getfanOn() {
			return fanOn;
		}

		public void setfanOn(Boolean fanOn) {
			this.fanOn = fanOn;
		}
	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("TemperatureAgent " + getAID().getName() + " terminating.");
	}
}
