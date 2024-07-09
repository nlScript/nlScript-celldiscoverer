package de.nlScript.celldiscoverer;

import nlScript.mic.Microscope;

import java.io.IOException;
import java.net.ConnectException;

public class TCPMicroscope extends Microscope {

	private final TCPClient client = new TCPClient();

	private void connect() {
		if(client.isConnected())
			return;
		for(int i = 0; i < 10; i++) {
			try {
				client.startConnection("localhost", 65432);
				// client.startConnection("10.210.17.6", 65432);
				System.out.println("RealCellDiscoverer connected");
				System.out.println(client.readLine());
				break;
			} catch (ConnectException ce) {
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if(!client.isConnected())
			throw new RuntimeException("Cannot connect to microscope");
	}

	private String send(String command) {
		try {
//			LogWindow.instance().addSent(command);
			System.out.println("SENDING: " + command);
			String received = client.sendMessage(command);
//			LogWindow.instance().addReceived(received);
			System.out.println("RECEIVED: " + received);
			return received;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void disconnect() {
		try {
			client.stopConnection();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}



	@Override
	public void reset() {
		super.reset();
		System.out.println("start experiment");
		connect();
	}

	@Override
	public void setCO2Concentration(double co2Concentration) {
		super.setCO2Concentration(co2Concentration);
		send("setTargetCO2Concentration " + co2Concentration);
	}

	@Override
	public void setTemperature(double temperature) {
		super.setTemperature(temperature);
		send("setTargetTemperature " + temperature);
	}

	public void stopExperiment() {
		send("stopExperiment");
		disconnect();
	}

	@Override
	public void acquirePositionsAndChannels(Position[] positions, Channel[] channels, double dz) {
		// binning
		// FOV
		// lens
		// magnification changer
		System.out.println("Set microscope optics:");
		System.out.println("  - change lens to " + getLens());
		System.out.println("  - change magnification changer to " + getMagnificationChanger());
		send("setLensAndMagnificationChanger " + getLens().label + " " + getMagnificationChanger().label);

		for(Position position : positions) {
			System.out.println("Move microscope to position " + position);
			int stageX = (int)Math.round(position.center.x);
			int stageY = (int)Math.round(position.center.y);
			send("setStagePosition " + stageX + " " + stageY);
			for(Channel channel : channels) {
				acquireSinglePositionAndChannel(position, channel);
			}
		}
	}

	@Override
	public void acquireSinglePositionAndChannel(Position position, Channel channel) {
		send(String.format("setStackCenter %.02f", position.center.z));
		System.out.println("Apply channel settings:");
		System.out.println("  - change camera exposure time to " + channel.getExposureTime());
		// TODO send("setExposureTime") or something
		for(LED led : LED.values()) {
			LEDSetting ledSetting = channel.getLEDSetting(led);
			if(ledSetting == null) {
				System.out.println("  - switch LED " + led.WAVELENGTH + " off");
				send("disableLED " + led.WAVELENGTH);
			}
			else {
				System.out.println("  - set LED " + led.WAVELENGTH + " to " + ledSetting.getIntensity() + "%");
				send("enableLED " + led.WAVELENGTH + " " + ledSetting.getIntensity());
			}
		}


		System.out.println("Acquire stack");
		send("acquire");
	}
}
