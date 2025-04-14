package org.firstinspires.ftc.teamcode.utils.rr.visualizer;

import com.acmerobotics.dashboard.DashboardCore;
import com.acmerobotics.dashboard.RobotStatus;
import com.acmerobotics.dashboard.SendFun;
import com.acmerobotics.dashboard.SocketHandler;
import com.acmerobotics.dashboard.config.ValueProvider;
import com.acmerobotics.dashboard.message.Message;
import com.acmerobotics.dashboard.message.redux.InitOpMode;
import com.acmerobotics.dashboard.message.redux.ReceiveOpModeList;
import com.acmerobotics.dashboard.message.redux.ReceiveRobotStatus;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import java.io.IOException;
import java.util.stream.Collectors;

public class DashboardInstance {
    public static void main(String[] args) throws InterruptedException {
        instance.start();
    }
    private static DashboardInstance instance = new DashboardInstance();

    final String DEFAULT_OP_MODE_NAME = "$Stop$Robot$";
    TestOpModeManager opModeManager = new TestOpModeManager();

    private TelemetryPacket currentPacket;

    DashboardCore core = new DashboardCore();

    private NanoWSD server = new NanoWSD(8000) {
        @Override
        protected NanoWSD.WebSocket openWebSocket(NanoHTTPD.IHTTPSession handshake) {
            return new DashWebSocket(handshake);
        }
    };

    private class DashWebSocket extends NanoWSD.WebSocket implements SendFun {
        final SocketHandler sh = core.newSocket(this);

        public DashWebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        public void send(Message message) {
            try {
                String messageStr = DashboardCore.GSON.toJson(message);
                send(messageStr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onOpen() {
            sh.onOpen();

            opModeManager.setSendFun(this);
            send(new ReceiveOpModeList(
                    opModeManager
                            .getTestOpModes()
                            .stream().map(TestOpMode::getName)
                            .collect(Collectors.toList())
            ));
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason,
                               boolean initiatedByRemote) {
            sh.onClose();

            opModeManager.clearSendFun();
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            String payload = message.getTextPayload();
            Message msg = DashboardCore.GSON.fromJson(payload, Message.class);

            if (sh.onMessage(msg)) {
                return;
            }

            switch (msg.getType()) {
                case GET_ROBOT_STATUS: {
                    String opModeName;
                    RobotStatus.OpModeStatus opModeStatus;
                    if (opModeManager.getActiveOpMode() == null) {
                        opModeName = DEFAULT_OP_MODE_NAME;
                        opModeStatus = RobotStatus.OpModeStatus.STOPPED;
                    } else {
                        opModeName = opModeManager.getActiveOpMode().getName();
                        opModeStatus = opModeManager.getActiveOpMode().getOpModeStatus();
                    }

                    send(new ReceiveRobotStatus(
                            new RobotStatus(core.enabled, true, opModeName, opModeStatus, "", "", 12.0)
                    ));
                    break;
                }
                case INIT_OP_MODE: {
                    InitOpMode initOpMode = (InitOpMode) msg;
                    opModeManager.initOpMode(initOpMode.getOpModeName());
                    break;
                }
                case START_OP_MODE:
                    opModeManager.startOpMode();
                    break;
                case STOP_OP_MODE:
                    opModeManager.stopOpMode();
                    break;
                default:
                    System.out.println(msg.getType());
            }
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {

        }
    }

    public static DashboardInstance getInstance() {
        return instance;
    }

    public void start() throws InterruptedException {
        System.out.println("Starting Dashboard instance");

        core.enabled = true;

        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            opModeManager.loop();
            Thread.yield();
        }
    }

    public void addData(String x, Object o) {
        if (currentPacket == null) {
            currentPacket = new TelemetryPacket();
        }

        currentPacket.put(x, o);
    }

    public void update() {
        if (currentPacket != null) {
            core.sendTelemetryPacket(currentPacket);
            currentPacket = null;
        }
    }

    public void sendTelemetryPacket(TelemetryPacket t) {
        core.sendTelemetryPacket(t);
    }
}
