package de.nlScript.celldiscoverer;

import nlScript.mic.LanguageControl;
import nlScript.Parser;
import nlScript.ui.ACEditor;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.io.File;

public class Main implements PlugIn {

	public static void main(String[] args) {
		new Main().startNLS(null);
	}

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Cell Discoverer");
		gd.addFileField("Script", "");
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		String pathToScript = gd.getNextString();
		startNLS(pathToScript);
	}

	public void startNLS(String pathToScript) {
		String script = loadScript(pathToScript);
		TCPMicroscope microscope = new TCPMicroscope();
		LanguageControl lc = new LanguageControl(microscope);
		Parser parser = lc.initParser();
		ACEditor editor = new ACEditor(parser);
		editor.getTextArea().setText(script);
		editor.setBeforeRun(lc::reset);
		editor.setAfterRun(() -> {
			System.out.println("lc.timeline = " + lc.getTimeline());
			lc.getTimeline().process(Runnable::run);
			lc.getTimeline().waitForProcessing();
			microscope.stopExperiment();
		});
		editor.setVisible(true);
	}

	private static String loadScript(String pathToScript) {
		if(pathToScript != null && !pathToScript.trim().isEmpty()) {
			File f = new File(pathToScript);
			if(f.exists()) {
				String text = IJ.openAsString(f.getAbsolutePath());
				if(text == null || text.trim().isEmpty())
					return "";
				return text;
			}
		}
		return "";
	}
}
