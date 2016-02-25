package temperature;

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

public class TemperatureAgent extends Agent {

	/**
	 *
	 */
	private static final long serialVersionUID = 138736042772986486L;
	private AID[] serverAgents;
	Map <String,CurrentStatusInRoom> currentStatuses = new HashMap<String,CurrentStatusInRoom>();
	String responseToSorter = "";
	Boolean boilerOn = false;
	Boolean autoTemperature = false;
	int temperatureLevel = 20;

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
		addBehaviour(new TemperatureService());

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

			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();

				ACLMessage requestFanToggle = new ACLMessage(ACLMessage.REQUEST);

				requestFanToggle.setContent(""); // per far funzionare l'IF dopo
				if (autoTemperature) { //se e' attiva la gestione automatica
					if (!currentStatuses.get(roomName).getFanStatus()) {
						if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo((float)temperatureLevel) > 0))) {

							requestFanToggle.setContent("true"); //accendi
						}
					} else if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo((float)temperatureLevel) < 0))) {

						requestFanToggle.setContent("false"); //spegni
					}
				} 
				else 
				{ //se non è attiva la gestione automatica
					if (!currentStatuses.get(roomName).getFanStatus()) 
					{
						if (currentStatuses.get(roomName).getFanOn()) 
						{

						requestFanToggle.setContent("true");

						}
					} else if (!currentStatuses.get(roomName).getFanOn()) 
					{
						requestFanToggle.setContent("false");
					}
				}

				

				if (requestFanToggle.getContent().equalsIgnoreCase("true") || requestFanToggle.getContent().equalsIgnoreCase("false")) {

					//ricerca agenti ventilatore
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sdRoom = new ServiceDescription();
					sdRoom.setName(roomName + "-fan"); // ad es: salone-fan
					template.addServices(sdRoom);
					AID[] fanAgents = null; // da modificare----------------------null
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						fanAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							fanAgents[i] = result[i].getName();

						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					requestFanToggle.addReceiver(fanAgents[0]); // da modificare----------------------
					
					requestFanToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestFanToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

					addBehaviour(new AchieveREInitiator(myAgent, requestFanToggle) {

						/**
						 *
						 */
						private static final long serialVersionUID = 1457890379172110455L;

						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setFanStatus(Boolean.valueOf(inform.getContent()));
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

			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				
				ACLMessage requestWindowToggle = new ACLMessage(ACLMessage.REQUEST);

				requestWindowToggle.setContent(""); // per far funzionare l'IF dopo
				if (autoTemperature) //se e' attiva la gestione automatica
				{ 
					if (!currentStatuses.get(roomName).getWindowStatus()) 
					{
						if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo((float)temperatureLevel) > 0))) 
						{
							System.out.println("CURRTEMP " + currentStatuses.get(roomName).getCurrentTemperature());
							System.out.println("TEMPERATURELEVEL " + (float)temperatureLevel);
							requestWindowToggle.setContent("true"); //accendi
						}

					}
					else if ((currentStatuses.get(roomName).getCurrentTemperature() != null) && ((currentStatuses.get(roomName).getCurrentTemperature().compareTo((float)temperatureLevel) < 0))) 
					{
						requestWindowToggle.setContent("false"); //spegni
					}
				}
				else //se non è attiva la gestione automatica
				{
					if (!currentStatuses.get(roomName).getWindowStatus()) {
						if (currentStatuses.get(roomName).getWindowOpen()) {

							requestWindowToggle.setContent("true");

						}
					} else if (!currentStatuses.get(roomName).getWindowOpen()) {

						requestWindowToggle.setContent("false");
					}
				}

				if (requestWindowToggle.getContent().equalsIgnoreCase("true") || requestWindowToggle.getContent().equalsIgnoreCase("false")) {

					//ricerca agenti finestra
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sdRoom = new ServiceDescription();
					sdRoom.setName(roomName + "-window"); // ad es: salone-window
					template.addServices(sdRoom);
					AID[] windowAgents = null; 
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						windowAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							windowAgents[i] = result[i].getName();

						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					requestWindowToggle.addReceiver(windowAgents[0]); // da modificare----------------------

					
					requestWindowToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// We want to receive a reply in 10 secs
					requestWindowToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

					addBehaviour(new AchieveREInitiator(myAgent, requestWindowToggle) {

						/**
						 *
						 */
						private static final long serialVersionUID = 1457890379172110455L;

						protected void handleInform(ACLMessage inform) {
							System.out.println("Agent " + inform.getSender().getName() + " send" + inform.getContent());
							currentStatuses.get(roomName).setWindowStatus(Boolean.valueOf(inform.getContent())); //DA MODIFICARE
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

			Float averageTemp = new Float(0);
			Set<String> rooms = currentStatuses.keySet();
			Iterator <String> roomIterator = rooms.iterator();
			while(roomIterator.hasNext()) {
				String roomName = roomIterator.next();
				averageTemp += currentStatuses.get(roomName).getCurrentTemperature();
			}
			averageTemp = averageTemp/currentStatuses.size();

			ACLMessage requestBoilerToggle = new ACLMessage(ACLMessage.REQUEST);

			requestBoilerToggle.setContent(""); // per far funzionare l'IF dopo
			if (autoTemperature) //se e' attiva la gestione automatica
			{ 
				if (!boilerOn) 
				{
					if (averageTemp.compareTo((float)temperatureLevel) < 0) 
					{
						requestBoilerToggle.setContent("true"); //accendi
					}
				
				} 
				else if (averageTemp.compareTo((float)temperatureLevel) > 0) 
				{
					requestBoilerToggle.setContent("false"); //spegni
				}
			}
			else //se non e' attiva la gestione automatica
			{
				
				if (boilerOn) 
				{
					requestBoilerToggle.setContent("false");
				}
				
			}

			if (requestBoilerToggle.getContent().equalsIgnoreCase("true") || requestBoilerToggle.getContent().equalsIgnoreCase("false")) {

				//ricerca agenti boiler
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sdRoom = new ServiceDescription();
				sdRoom.setName("boiler"); 
				template.addServices(sdRoom);
				AID[] boilerAgents = null; // da modificare----------------------null
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					boilerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						boilerAgents[i] = result[i].getName();

					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
				
				requestBoilerToggle.addReceiver(boilerAgents[0]); // da modificare----------------------
				
				requestBoilerToggle.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				// We want to receive a reply in 10 secs
				requestBoilerToggle.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

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
	
	private class TemperatureService extends OneShotBehaviour {


		
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
			case "fan":
				Boolean fanOn = !(currentStatuses.get(room).getFanStatus());
				currentStatuses.get(room).setFanOn(fanOn);
				responseToSorter = fanOn.toString();
				break;
			case "autoTemp":
				autoTemperature = !(autoTemperature);
				responseToSorter = autoTemperature.toString();
				break;
			case "temp":
				temperatureLevel = Integer.parseInt(value.trim());
				responseToSorter = Integer.toString(temperatureLevel);
				break;
			case "window":
				Boolean windowOpen = !(currentStatuses.get(room).getWindowStatus());
				currentStatuses.get(room).setWindowOpen(windowOpen);
				responseToSorter = windowOpen.toString();
				break;
			case "currentTemp":
				currentStatuses.get(room).getCurrentTemperature();
				responseToSorter = currentStatuses.get(room).getCurrentTemperature().toString();
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

        private Float currentTemperature;
        private AID roomAgent;
        private Boolean fanOn; 
        private Boolean fanStatus;
        private Boolean windowOpen;
        private Boolean windowStatus;
        

        public CurrentStatusInRoom() {
        	setCurrentTemperature(null);
            setRoomAgent(null);
            setFanOn(false);
            setFanStatus(false);
            setWindowOpen(false);
            setWindowStatus(false);
        }

        public CurrentStatusInRoom(AID roomAgent) {
            setCurrentTemperature(null);
            setRoomAgent(roomAgent);
            setFanOn(false);
            setFanStatus(false);
            setWindowOpen(false);
            setWindowStatus(false);
        }

        public Float getCurrentTemperature() {
            return currentTemperature;
        }

        public void setCurrentTemperature(Float currentTemperature) {
            this.currentTemperature = currentTemperature;
        }
        
        /*public Boolean getAutoTemperature() {
            return autoTemperature;
        }

        public void setAutoTemperature(Boolean autoTemperature) {
            this.autoTemperature = autoTemperature;
        }
        
        public int getTemperatureLevel()
        {
        	return temperatureLevel;
        }
        
        public void setTemperatureLevel(int temperatureLevel)
        {
        	this.temperatureLevel = temperatureLevel;
        }*/

        public AID getRoomAgent() {
            return roomAgent;
        }

        public void setRoomAgent(AID roomAgent) {
            this.roomAgent = roomAgent;
        }

        public Boolean getFanOn() {
            return fanOn;
        }

        public void setFanOn(Boolean fanOn) {
            this.fanOn = fanOn;
        }
        
        public Boolean getFanStatus()
        {
        	return fanStatus;
        }
        
        public void setFanStatus(Boolean fanStatus)
        {
        	this.fanStatus = fanStatus;
        }
        
        public Boolean getWindowOpen()
        {
        	return windowOpen;
        }
        
        public void setWindowOpen(Boolean windowOpen)
        {
        	this.windowOpen = windowOpen;
        }
        
        public Boolean getWindowStatus()
        {
        	return windowStatus;
        }
        
        public void setWindowStatus(Boolean windowStatus)
        {
        	this.windowStatus = windowStatus;
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
