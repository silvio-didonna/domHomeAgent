
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;

public class domHome {

    static String thermometerName = "Termometro";
    static String fanName = "Ventilatore";
    static String temperatureName = "Gestore-Temperatura";
    static String roomName = "hall";
    static String serialCommName = "Gestore-Seriale";
    static String lightSensorName = "Sensore-Luci";
    static String lightningName = "Gestore-Luci";
    static String lightName = "Luce";
    static String windowName = "Finestra";
    static String flameSensorName = "Sensore-Fiamme";
    static String fireSystemName = "Antincendio";
    static String motionSensorName = "Sensore-Movimento";
    static String antitheftName = "Antifurto";
    static String boilerName = "Boiler";
    static String shutterName = "Persiana";
    static String laserSensorName = "Laser";
    static String garageDoorName = "Garage";
    static String buzzerName = "Buzzer";
    
    static String sorterName = "Sorter";

    public static void main(String args[]) throws Exception {
        if (args.length < 2) {
            System.out.println("Insert port and baud as argument (i.e. domHome COM3 115200)");
            return;
        }

		//---------------------********************************************-----------------------------------
        // http://jade.tilab.com/pipermail/jade-develop/2008q3/012874.html
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();

        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
		//System.out.print("runtime created\n");

        // Create a default profile
        Profile profile = new ProfileImpl(null, 1200, null);
		//System.out.print("profile created\n");

        //System.out.println("Launching a whole in-process platform..."+profile);
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(profile);

        // now set the default Profile to start a container
        ProfileImpl pContainer = new ProfileImpl(null, 1200, null);
        //System.out.println("Launching the agent container ..."+pContainer);

        jade.wrapper.AgentContainer cont = rt.createAgentContainer(pContainer);
		//System.out.println("Launching the agent container after ..."+pContainer);

		//System.out.println("containers created");
        //System.out.println("Launching the rma agent on the main container ...");
        AgentController rma = mainContainer.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
        rma.start();
		//---------------------********************************************-----------------------------------
	
        Object[] portAndBaud = new Object[2];
        portAndBaud[0]=args[0];
        portAndBaud[1]=args[1];
        
        Object[] inRoom = new Object[1];
        inRoom[0]=roomName; // imposto la stanza nella quale sono presenti i sensori e gli attuatori considerati
        
        // Agent creation on local container
        AgentController fanAgent = cont.createNewAgent(fanName, "temperature.FanAgent", inRoom);
        AgentController thermometerAgent = cont.createNewAgent(thermometerName, "temperature.ThermometerAgent", inRoom);
        AgentController serialCommAgent = cont.createNewAgent(serialCommName, "SerialCommAgent", portAndBaud);
        AgentController lightSensorAgent = cont.createNewAgent(lightSensorName, "light.LightSensorAgent", inRoom);
        AgentController roomAgent = cont.createNewAgent(roomName, "RoomAgent", null);
        AgentController temperatureAgent = cont.createNewAgent(temperatureName, "temperature.TemperatureAgent", null);
        AgentController lightningAgent = cont.createNewAgent(lightningName, "light.LightningAgent", null);
        AgentController lightAgent = cont.createNewAgent(lightName, "light.LightAgent", inRoom);
        AgentController windowAgent = cont.createNewAgent(windowName, "temperature.WindowAgent", inRoom);
        AgentController flameSensorAgent = cont.createNewAgent(flameSensorName, "security.FlameSensorAgent", inRoom);
        AgentController fireSystemAgent = cont.createNewAgent(fireSystemName, "security.FireSystemAgent", null);
        AgentController motionSensorAgent = cont.createNewAgent(motionSensorName, "security.MotionSensorAgent", inRoom);
        AgentController laserSensorAgent = cont.createNewAgent(laserSensorName, "security.LaserSensorAgent", inRoom);
        AgentController antitheftAgent = cont.createNewAgent(antitheftName, "security.AntitheftAgent", null);
        AgentController boilerAgent = cont.createNewAgent(boilerName, "temperature.BoilerAgent", null);
        AgentController shutterAgent = cont.createNewAgent(shutterName, "light.ShutterAgent", inRoom);
        AgentController garageDoorAgent = cont.createNewAgent(garageDoorName, "security.GarageDoorAgent", null);
        AgentController buzzerAgent = cont.createNewAgent(buzzerName, "security.BuzzerAgent", null);
        AgentController sorterAgent = cont.createNewAgent(sorterName, "SorterAgent", null);
        /*
         try {
         Thread.sleep(8000);
         } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         }
         */
        //Gestore seriale
        serialCommAgent.start();

        //Agenti base per temperatura
        fanAgent.start();
        windowAgent.start();
        thermometerAgent.start();
        boilerAgent.start();

        //Agenti base per l'illuminazione
        lightAgent.start();
        lightSensorAgent.start();
        shutterAgent.start();
        
        //Agenti base per la sicurezza
        flameSensorAgent.start();
        motionSensorAgent.start();
        laserSensorAgent.start();
        garageDoorAgent.start();

        //Gestore ambiente
        roomAgent.start();

        //Gestori generali
        temperatureAgent.start();
        lightningAgent.start();
        fireSystemAgent.start();
        antitheftAgent.start();
        
        sorterAgent.start();


    }
}
