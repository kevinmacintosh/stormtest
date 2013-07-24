package storm.starter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import storm.starter.spout.TwitterSpout;
import twitter4j.Status;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * This topology is used to demonstrate output from twitter as well as xTreme
 * processing
 * 
 */
public class ElDragonTopology {

	static File file = new File("/root/results.html");
	static Map<String, Integer> mapa = new HashMap<String, Integer>();
	
	public static class FilterTweet extends BaseBasicBolt {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void execute(Tuple tuple, BasicOutputCollector collector) {
			Status status = (Status) tuple.getValue(0);
			
			if(status.getMediaEntities().length != 0){ //Filter out tweets that have images
				collector.emit(new Values(status.getMediaEntities()[0].getMediaURL()));
			}
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("url"));
		}
	}

	public static class CountImage extends BaseBasicBolt {
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(Tuple input, BasicOutputCollector collector) {
			String mediaURL = input.getString(0);
			
			Integer pop = mapa.get(mediaURL);
			if(pop == null) pop = 0;
			pop++;
			mapa.put(mediaURL, pop);
			
			collector.emit(new Values(mapa.toString()));
			
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("done"));
			
		}
		
	}
	
	public static void main(String[] args) throws Exception {

		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout("tweets", new TwitterSpout(), 5);
		builder.setBolt("filter", new FilterTweet(), 8).shuffleGrouping("tweets");
		builder.setBolt("images", new CountImage(), 10).fieldsGrouping("filter" , new Fields("url"));

		Config conf = new Config();
		conf.setDebug(true);

		conf.setMaxTaskParallelism(3);
		
		createHttpHeaders();
		
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("HonestyHourAnalysis", conf, builder.createTopology());
		Thread.sleep(500000);
		createImages();
		createHttpClosers();
		cluster.shutdown();

	}
	
	public static void createHttpHeaders(){
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			//Clear out contents from the previous file if it existed
			try {
				PrintWriter empty = new PrintWriter(file);
				empty.print("");
				empty.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("<html><head><title>Images related to #BuyBabyIOniTunes</title></head><body>\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void createImages(){
		for(String i: mapa.keySet()){
			try {
				FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("<img src='" + i + "' width='" + mapa.get(i)*10 + "%' />\n");
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void createHttpClosers(){

		try {
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("</body></html>");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}