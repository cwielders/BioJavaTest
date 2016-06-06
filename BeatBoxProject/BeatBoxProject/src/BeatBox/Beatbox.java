package BeatBox;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;



public class Beatbox implements Serializable{

	JFrame theFrame;
	JPanel mainPanel;
	JPanel background;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	JList incomingList;
	JTextField userMessage;
	int nextNum;
	Vector<String> listVector = new Vector<String>();
	static String userName;
	ObjectOutputStream out;
	ObjectInputStream in;

	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	boolean buttonclicked = false;
			
	String[] instrumentNames = {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
	int[] instrumentCodes = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	
	static final int beats = 16;
	
	public static void main(String[] args) {
		Beatbox beatbox = new Beatbox();
		String name = JOptionPane.showInputDialog("Username");
		beatbox.startUp(name);
	}
	
	public void startUp(String name){
		userName = name;
		try{
			Socket sock = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		}catch(Exception ex){
				System.out.println("could not connect: you have to play alone");
			}
		setUpMidi();		
		buildGui();
	}
	
	public class RemoteReader implements Runnable {
		boolean[] cheboxState = null;
		String nameToShow = null;
		Object obj = null;
		public void run(){
			try{
				while ((obj = in.readObject()) != null){
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					cheboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, cheboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			}catch(Exception ex){ex.printStackTrace();
			}
		}
	}
	
	public void buildGui() {
		
		theFrame = new JFrame("prutsboxje");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		Box buttonBox = new Box(BoxLayout.Y_AXIS);
				
		JButton start = new JButton("start");
		start.addActionListener(new MyStartListner());
		buttonBox.add(start);
		
		JButton stop = new JButton("stop");
		stop.addActionListener(new MyStopListner());
		buttonBox.add(stop);
		
		JButton sneller = new JButton("sneller");
		sneller.addActionListener(new MySnellerListner());
		buttonBox.add(sneller);
		
		JButton trager = new JButton("trager");
		trager.addActionListener(new MyTragerListner());
		buttonBox.add(trager);
		
		JButton removeAll = new JButton("alles weg");
		removeAll.addActionListener(new MyRemoveAllListner());
		buttonBox.add(removeAll);
		
		JButton sendIt = new JButton("zenden");
		sendIt.addActionListener(new MySendListner());
		buttonBox.add(sendIt);
		
		JButton save = new JButton("save");
		save.addActionListener(new MySaveListner());
		buttonBox.add(save);
		
		JButton open = new JButton("open");
		open.addActionListener(new MyOpenListner());
		buttonBox.add(open);
		
		userMessage = new JTextField("Stuur boodschap en sequence" );
		buttonBox.add(userMessage);
		
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionlistener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < instrumentNames.length; i++) {
			Label l = new Label(instrumentNames[i]);
			nameBox.add(l);
		}
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(background);
		
		GridLayout grid = new GridLayout(instrumentNames.length,beats);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);
			
		checkboxList = new ArrayList<JCheckBox>();
		for (int i = 0; i < beats*instrumentNames.length; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			c.addActionListener(new MyChechboxChangeListner());
			checkboxList.add(c);
			mainPanel.add(c);
		}
		theFrame.setBounds(50,50,300,300);
		theFrame.pack();
		theFrame.setVisible(true);
		
		setUpMidi();		
		
	}
	
		public void allesWeg()	{
		for (JCheckBox c : checkboxList){
			c.setSelected(false);
		}
	}
	
		
		
		
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ,4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void buildTrackAndStart() {
	
		int[] trackList = null;
		int[] lastBeat = {127};
		sequencer.addControllerEventListener(new LastBeatListner(),lastBeat);
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		
		for (int i = 0; i<16; i++) {
			trackList = new int[16];
			int key = instrumentCodes[i];
			for (int j=0; j<16; j++){
				JCheckBox jc = (JCheckBox) checkboxList.get(j+(16*i));
				if (jc.isSelected()) {
					trackList[j] = key;
				}
					else {
						trackList[j] = 0;
					}
				
			}
			makeTracks(trackList);
		}
		track.add(makeEvent(192,9,1,0,15));
		track.add(makeEvent(176,1,127,0,15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch(Exception e) {
			e.printStackTrace();
		}
	
	}
	
	public void makeTracks(int[] list) {
		for (int i = 0; i<16; i++) {
			int key = list[i];
			if (key != 0)
				track.add(makeEvent(144,9,key,100,i));
				track.add(makeEvent(128,9,key,100,i+1));
		}
	}
	
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int beat) {
		MidiEvent event = null;
		try{
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, beat);
		} catch(Exception e){};
		return event;
	}
	
	public void changeSequence(boolean[] checkboxState) {
		for (int i = 0; i<(beats*instrumentNames.length); i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if (checkboxState[i]) {
				check.setSelected(true);
			}
			else {
				check.setSelected(false);
			}
		}
	}
	
	public class MyStartListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			buildTrackAndStart();
		}
	}
	public class MyStopListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
		}
	}
	public class MyRemoveAllListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
			allesWeg();
		}
	}
	
	public class MySnellerListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			tempoFactor = (float) (tempoFactor * 1.03);
			sequencer.setTempoFactor(tempoFactor);
		}
	}
	public class MyTragerListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			tempoFactor = (float) (tempoFactor * 0.97);
			sequencer.setTempoFactor(tempoFactor);
		}
	}

	public class LastBeatListner implements ControllerEventListener {
		public void controlChange(ShortMessage event){
			if (buttonclicked == true){
				sequencer.stop();
				buildTrackAndStart();
				buttonclicked = false;
			}
		}
	}
	public class MyChechboxChangeListner implements  ActionListener  {
	    public void actionPerformed(ActionEvent e) {
	    	buttonclicked = true;
	    }
	}
	public class MySendListner implements ActionListener  {
	    public void actionPerformed(ActionEvent e) {
	    	boolean[] checkboxState = new boolean[beats*instrumentNames.length];
	    	for (int i = 0; i<(beats*instrumentNames.length); i++){
	    	JCheckBox check = (JCheckBox) checkboxList.get(i);
	    		if (check.isSelected()) {
					checkboxState[i] = true;
				}
	    	}
//	    	String messageToSend = null;
	    	try {
	    		out.writeObject(userName + nextNum++ + ":" + userMessage.getText());
	    		out.writeObject(checkboxState);
	    	} 
	    	catch (Exception Ex) {
	    		System.out.println("Server ligt plat");
	    	}
	    	userMessage.setText("");
	    }
	}
	
	public class MyListSelectionlistener implements ListSelectionListener  {

			public void valueChanged(ListSelectionEvent le) {
			if (le.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if (selected != null) {
					boolean[] selectedstate = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedstate);
					sequencer.stop();
					buildTrackAndStart();
					
				}
			}
			
		}
	}
	
	public class MySaveListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			
			boolean[] checkboxState = new boolean[beats*instrumentNames.length];
	    	for (int i = 0; i<(beats*instrumentNames.length); i++){
	    	JCheckBox check = (JCheckBox) checkboxList.get(i);
	    		if (check.isSelected()) {
					checkboxState[i] = true;
				}
	    	}
	    	try {
	    		FileOutputStream fout = new FileOutputStream("user/patroon.bbox");
	    		ObjectOutputStream oos = new ObjectOutputStream(fout);
	    		oos.writeObject(checkboxState);
	    		oos.close();
	    	}
			catch (FileNotFoundException e){
				System.out.println("file not found");
			}
				
			catch (IOException e){System.out.println("file not found");
			}
		}
	}
	
	public class MyOpenListner implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			boolean[] checkboxState = new boolean[beats*instrumentNames.length];
			try {
				FileInputStream fin = new FileInputStream("user/patroon.bbox");
	    		ObjectInputStream ois = new ObjectInputStream(fin);
	    		checkboxState = (boolean[]) ois.readObject();
	    		ois.close();
	    	}
			catch(Exception ex){ex.printStackTrace();}
			changeSequence(checkboxState);
			sequencer.stop();
			buildTrackAndStart();
	    }
	}
}
