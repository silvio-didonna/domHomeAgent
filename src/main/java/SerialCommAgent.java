
import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;

import jade.core.Agent;
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
import jade.proto.AchieveREResponder;

public class SerialCommAgent extends Agent {

    SerialPort serialPort;
    String port;
    int baud;

    /**
     *
     */
    private static final long serialVersionUID = 138736042772986486L;

    protected void setup() {
        //port="/dev/ttyACM0";
        //port="/dev/ttyUSB1";
        //port="COM3";
        //baud=115200;

        Object[] portAndBaud = this.getArguments(); //ottiene la porta seriale
        port = portAndBaud[0].toString();
        baud = Integer.parseInt(portAndBaud[1].toString());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("serialcomm-manager");
        sd.setName("JADE-serialcomm");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        serialPort = SerialPort.getCommPort(port);
        serialPort.setComPortParameters(this.baud, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

        System.out.println("Porta: " + serialPort.getDescriptivePortName() + " baud: " + baud);

        // Apre porta seriale
        serialPort.openPort();

        // Questa istruzione e' necessaria perche' Arduino si riavvia dopo aver aperto la seriale
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        addBehaviour(new SendSerialServiceBehaviourFIPA());
    }

    private class SendSerialServiceBehaviourFIPA extends OneShotBehaviour {

        /**
         *
         */
        private static final long serialVersionUID = 8837634265096484413L;

        @Override
        public void action() {

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            addBehaviour(new AchieveREResponder(myAgent, template) {
                /**
                 *
                 */
                private static final long serialVersionUID = -3503422695676488699L;
                String msgRecv = "";

                protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
                    if (!request.getContent().isEmpty()) {
                        // We agree to perform the action.
                        ACLMessage agree = request.createReply();
                        agree.setPerformative(ACLMessage.AGREE);

                        msgRecv = sendRecSerial(request.getContent()); //richiesta ad arduino

                        return agree;
                    } else {
                        // We refuse to perform the action
                        throw new RefuseException("Message content void");
                    }
                }

                protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                    ACLMessage inform = request.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    inform.setContent(msgRecv);

                    return inform;

                }
            });

        } 

        String sendRecSerial(String msgContent) {
            try {
                serialPort.getOutputStream().write((msgContent).getBytes());
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            String msgRecv = "";

            byte[] readBuffer = null;
            try {
                while (serialPort.bytesAvailable() == 0) {
                    Thread.sleep(20);
                }

                Thread.sleep(50);
                readBuffer = new byte[serialPort.bytesAvailable()];
                serialPort.getInputStream().read(readBuffer);
                msgRecv = new String(readBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return msgRecv;
        }

    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        serialPort.closePort();
        System.out.println("SerialCommAgent " + getAID().getName() + " terminating.");
    }
}
