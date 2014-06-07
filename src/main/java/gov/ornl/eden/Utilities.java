package gov.ornl.eden;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Utilities {
	private static final Logger log = LoggerFactory.getLogger(Utilities.class);

	private static final String MONTHS[] = { "january", "february", "march",
			"april", "may", "june", "july", "august", "september", "october",
			"november", "december" };

	public static Color getColorForCorrelationCoefficient(double corrcoef,
			double threshold) {
		Color c;
		if (Double.isNaN(corrcoef)) {
			return null;
		}

		if (corrcoef > 0.f) {
			float norm = 1.f - (float) Math.abs(corrcoef);
			Color c0 = new Color(211, 37, 37); // high pos. corr.
			Color c1 = new Color(240, 240, 240); // low pos. corr.

			if (Math.abs(corrcoef) > threshold) {
				c = c0;
			} else {
				int r = c0.getRed()
						+ (int) (norm * (c1.getRed() - c0.getRed()));
				int green = c0.getGreen()
						+ (int) (norm * (c1.getGreen() - c0.getGreen()));
				int b = c0.getBlue()
						+ (int) (norm * (c1.getBlue() - c0.getBlue()));
				c = new Color(r, green, b);
			}
		} else {
			float norm = 1.f - (float) Math.abs(corrcoef);
			Color c0 = new Color(44, 110, 211/* 177 */); // high neg. corr.
			Color c1 = new Color(240, 240, 240);// low neg. corr.

			if (Math.abs(corrcoef) > threshold) {
				c = c0;
			} else {
				int r = c0.getRed()
						+ (int) (norm * (c1.getRed() - c0.getRed()));
				int green = c0.getGreen()
						+ (int) (norm * (c1.getGreen() - c0.getGreen()));
				int b = c0.getBlue()
						+ (int) (norm * (c1.getBlue() - c0.getBlue()));
				c = new Color(r, green, b);
			}
		}
		return c;
	}

	public static DateTime parseUSGSDateString(String dateString) {

		String tokens[] = dateString.split("[,: ]+");

		int month = tokenToMonthNumber(tokens[1]); // month
		int dayOfMonth = Integer.parseInt(tokens[2]); // day of the month
		int year = Integer.parseInt(tokens[3]); // year
		int hour = Integer.parseInt(tokens[4]);
		int minute = Integer.parseInt(tokens[5]);
		int second = Integer.parseInt(tokens[6]);

		DateTime dt = new DateTime(year, month, dayOfMonth, hour, minute,
				second, DateTimeZone.forID("UTC"));

		return dt;
	}

	private static int tokenToMonthNumber(String token) {
		String test = token.toLowerCase();
		for (int i = 0; i < MONTHS.length; i++) {
			if (test.equals(MONTHS[i])) {
				return i + 1;
			}
		}
		return -1;
	}

	public static ArrayList<Tuple> readUSGSQuakeWWWAsTuples(String urlString)
			throws Exception {
		URL url = new URL(urlString);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				url.openStream()));
		DateTimeFormatter dtFormatter = DateTimeFormat.forPattern(
				"EEEE, MMMM dd, yyyy HH:mm:ss 'UTC'").withZone(
				DateTimeZone.forID("UTC"));
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();

		int line_counter = 0;
		boolean skip_line = false;

		String line = in.readLine();
		while (line != null) {
			if (line_counter == 0) {
				// The first line contains the column headers.

				// int token_counter = 0;
				// StringTokenizer st = new StringTokenizer(line);
				// while (st.hasMoreTokens()) {
				// String token = st.nextToken(",");
				// Column column = new Column();
				// column.setName(token.trim());
				// columns.add(column);
				// token_counter++;
				// }

				line_counter++;
				line = in.readLine();
				continue;
			}

			Tuple tuple = new Tuple();
			StringTokenizer st = new StringTokenizer(line);
			int token_counter = 0;

			skip_line = false;
			while (st.hasMoreTokens()) {
				String token;
				if (token_counter == 3) {
					token = st.nextToken("\"");
					token = st.nextToken("\"");
					st.nextToken(",");
				} else {
					token = st.nextToken(",");
				}

				if (token_counter == 4 || token_counter == 5
						|| token_counter == 6 || token_counter == 7
						|| token_counter == 8) {
					// Add version number
					tuple.addElement(Float.parseFloat(token));
				} else if (token_counter == 3) {
					// String dtString = token;
					// while(true) {
					// token = st.nextToken();
					// dtString += token;
					// }
					// Parse date-time string and add elements
					// log.debug("token: " + token);
					// DateTime dt = dtFormatter.parseDateTime(token);
					DateTime dt = parseUSGSDateString(token);
					// log.debug(" dt.toString() " + dt.toString());
					tuple.addElement(dt.getYear());
					tuple.addElement(dt.getMonthOfYear());
					tuple.addElement(dt.getDayOfMonth());
					tuple.addElement(dt.getHourOfDay());
					tuple.addElement(dt.getMinuteOfHour());
				}

				token_counter++;
				// try {
				// float value = Float.parseFloat(token);
				// tuple.addElement(value);
				// token_counter++;
				// } catch (NumberFormatException ex) {
				// System.out.println("DataSet.readCSV(): NumberFormatException caught so skipping record. "
				// + ex.fillInStackTrace());
				// skip_line = true;
				// break;
				// }
			}

			if (!skip_line) {
				// log.debug("tuple.getElementCount() " +
				// tuple.getElementCount());
				tuples.add(tuple);
				line_counter++;
			}

			line = in.readLine();
		}

		in.close();

		return tuples;
	}

	public static void readUSGSQuakeWWW(String urlString, DataModel dataModel)
			throws Exception {
		URL url = new URL(urlString);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				url.openStream()));
		DateTimeFormatter dtFormatter = DateTimeFormat.forPattern(
				"EEEE, MMMM  d, yyyy HH:mm:ss 'UTC'").withZone(
				DateTimeZone.forID("UTC"));
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();

		int line_counter = 0;
		boolean skip_line = false;

		String line = in.readLine();
		while (line != null) {
			if (line_counter == 0) {
				// The first line contains the column headers.

				// int token_counter = 0;
				// StringTokenizer st = new StringTokenizer(line);
				// while (st.hasMoreTokens()) {
				// String token = st.nextToken(",");
				// Column column = new Column();
				// column.setName(token.trim());
				// columns.add(column);
				// token_counter++;
				// }

				line_counter++;
				line = in.readLine();
				continue;
			}

			Tuple tuple = new Tuple();
			StringTokenizer st = new StringTokenizer(line);
			int token_counter = 0;

			skip_line = false;
			while (st.hasMoreTokens()) {
				String token;
				if (token_counter == 3) {
					token = st.nextToken("\"");
					token = st.nextToken("\"");
					st.nextToken(",");
				} else {
					token = st.nextToken(",");
				}

				if (token_counter == 2 || token_counter == 4
						|| token_counter == 5 || token_counter == 6
						|| token_counter == 7 || token_counter == 8) {
					// Add version number
					tuple.addElement(Float.parseFloat(token));
				} else if (token_counter == 3) {
					// String dtString = token;
					// while(true) {
					// token = st.nextToken();
					// dtString += token;
					// }
					// Parse date-time string and add elements
					// log.debug("token: " + token);
					DateTime dt = dtFormatter.parseDateTime(token);
					// log.debug(" dt.toString() " + dt.toString());
					tuple.addElement(dt.getYear());
					tuple.addElement(dt.getMonthOfYear());
					tuple.addElement(dt.getDayOfMonth());
					tuple.addElement(dt.getHourOfDay());
					tuple.addElement(dt.getMinuteOfHour());
				}

				token_counter++;
				// try {
				// float value = Float.parseFloat(token);
				// tuple.addElement(value);
				// token_counter++;
				// } catch (NumberFormatException ex) {
				// System.out.println("DataSet.readCSV(): NumberFormatException caught so skipping record. "
				// + ex.fillInStackTrace());
				// skip_line = true;
				// break;
				// }
			}

			if (!skip_line) {
				log.debug("tuple.getElementCount() " + tuple.getElementCount());
				tuples.add(tuple);
				line_counter++;
			}

			line = in.readLine();
		}

		in.close();

		// Setup data model columns
		ArrayList<Column> columns = new ArrayList<Column>();
		columns.add(new Column("Version"));
		columns.add(new Column("year"));
		columns.add(new Column("month"));
		columns.add(new Column("day"));
		columns.add(new Column("hour"));
		columns.add(new Column("minute"));
		columns.add(new Column("lat"));
		columns.add(new Column("lon"));
		columns.add(new Column("depth"));
		columns.add(new Column("magnitude"));
		columns.add(new Column("num-stations"));

		dataModel.setData(tuples, columns);
	}

	public static void readXML(File f, DataModel dataModel) throws Exception {
		// Read data from the file
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(f);

		// normalize text representation
		doc.getDocumentElement().normalize();
		log.debug("Root element of the document is "
				+ doc.getDocumentElement().getNodeName());

		NodeList listOfEvents = doc.getElementsByTagName("event");
		int totalEvents = listOfEvents.getLength();
		log.debug("Total number of events: " + totalEvents);

		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		for (int i = 0; i < listOfEvents.getLength(); i++) {
			Node eventNode = listOfEvents.item(i);
			Tuple tuple = new Tuple();

			if (eventNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eventElement = (Element) eventNode;

				String id = eventElement.getAttribute("id");
				String networkCode = eventElement.getAttribute("network-code");
				log.debug(i + "-  id:" + id);

				NodeList paramList = eventElement.getElementsByTagName("param");
				for (int j = 0; j < paramList.getLength(); j++) {
					Node paramNode = paramList.item(j);
					if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
						Element paramElement = (Element) paramNode;
						String name = paramElement.getAttribute("name");
						String value = paramElement.getAttribute("value");
						// log.debug(name + ": " + value);
						if (name.equals("year")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("month")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("hour")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("day")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("minute")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("latitude")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("longitude")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("depth")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("magnitude")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("num-stations")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("num-phases")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("dist-first-station")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("rms-error")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("hor-error")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("ver-error")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("azimuthal-gap")) {
							tuple.addElement(Float.parseFloat(value));
						} else if (name.equals("num-stations-mag")) {
							tuple.addElement(Float.parseFloat(value));
						}
					}
				}
			}
			log.debug(" tuple size: " + tuple.getElementCount());
			tuples.add(tuple);
		}

		// Setup data model columns
		ArrayList<Column> columns = new ArrayList<Column>();
		// columns.add(new Column("id"));
		// columns.add(new Column("network-code"));
		columns.add(new Column("year"));
		columns.add(new Column("month"));
		columns.add(new Column("day"));
		columns.add(new Column("hour"));
		columns.add(new Column("minute"));
		columns.add(new Column("latitude"));
		columns.add(new Column("longitude"));
		columns.add(new Column("depth"));
		columns.add(new Column("magnitude"));
		columns.add(new Column("num-stations"));
		columns.add(new Column("num-phases"));
		columns.add(new Column("dist-first-station"));
		columns.add(new Column("rms-error"));
		columns.add(new Column("hor-error"));
		columns.add(new Column("ver-error"));
		columns.add(new Column("azimuthal-gap"));
		// columns.add(new Column("magnitude-type"));
		// columns.add(new Column("magnitude-type-ext"));
		columns.add(new Column("num-stations-mag"));
		// columns.add(new Column("location-method"));
		// columns.add(new Column("location-method-ext"));

		dataModel.setData(tuples, columns);
	}

	public static void readCSVSample(File f, DataModel dataModel,
			double sampleFactor) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		int totalLineCount = 0;
		String line = null;
		while ((line = reader.readLine()) != null) {
			totalLineCount++;
		}
		totalLineCount -= 1; // remove header line
		reader.close();

		log.debug("totalLineCount is " + totalLineCount);

		int sampleSize = (int) (sampleFactor * totalLineCount);
		log.debug("sample size is " + sampleSize);

		int sampleIndices[] = new int[sampleSize];
		boolean sampleSelected[] = new boolean[totalLineCount];
		Arrays.fill(sampleSelected, false);
		Random rand = new Random();
		for (int i = 0; i < sampleIndices.length; i++) {
			int index = rand.nextInt(totalLineCount);
			while (sampleSelected[index]) {
				log.debug("got a duplicate");
				index = rand.nextInt(totalLineCount);
			}
			sampleSelected[index] = true;
			sampleIndices[i] = index;
		}

		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		ArrayList<Column> columns = new ArrayList<Column>();
		reader = new BufferedReader(new FileReader(f));

		// Read the header line
		line = reader.readLine();
		int tokenCounter = 0;
		StringTokenizer st = new StringTokenizer(line);
		while (st.hasMoreTokens()) {
			String token = st.nextToken(",");
			Column column = new Column();
			column.setName(token.trim());
			columns.add(column);
			tokenCounter++;
		}

		// Read the data tuples
		int lineCounter = 0;
		boolean skipLine = false;
		while ((line = reader.readLine()) != null) {
			// is the current line selected to be read
			if (sampleSelected[lineCounter]) {
				// read the line as a tuple
				Tuple tuple = new Tuple();
				st = new StringTokenizer(line);
				tokenCounter = 0;

				skipLine = false;
				while (st.hasMoreTokens()) {
					String token = st.nextToken(",");
					try {
						float value = Float.parseFloat(token);

						// data attribute
						tuple.addElement(value);

						tokenCounter++;
					} catch (NumberFormatException ex) {
						log.debug("NumberFormatException caught so skipping record. "
								+ ex.fillInStackTrace());
						skipLine = true;
						break;
					}
				}

				if (!skipLine) {
					// log.debug("added tuple at index " + lineCounter);
					tuples.add(tuple);
				}

				// line = reader.readLine();
			}

			lineCounter++;
		}

		reader.close();
		dataModel.setData(tuples, columns);
	}

	public static void readCSV(File f, DataModel dataModel) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));

		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		ArrayList<Column> columns = new ArrayList<Column>();

		String line = reader.readLine();
		int line_counter = 0;

		boolean skip_line = false;
		while (line != null) {
			if (line_counter == 0) {
				// The first line contains the column headers.

				int token_counter = 0;
				StringTokenizer st = new StringTokenizer(line);
				while (st.hasMoreTokens()) {
					String token = st.nextToken(",");
					Column column = new Column();
					column.setName(token.trim());
					columns.add(column);
					token_counter++;
				}

				line_counter++;
				line = reader.readLine();
				continue;
			}

			Tuple tuple = new Tuple();
			StringTokenizer st = new StringTokenizer(line);
			int token_counter = 0;

			skip_line = false;
			while (st.hasMoreTokens()) {
				String token = st.nextToken(",");

				try {
					float value = Float.parseFloat(token);

					// data attribute
					tuple.addElement(value);
					token_counter++;
				} catch (NumberFormatException ex) {
					System.out
							.println("DataSet.readCSV(): NumberFormatException caught so skipping record. "
									+ ex.fillInStackTrace());
					skip_line = true;
					break;
				}
			}

			if (tuple.getElementCount() != columns.size()) {
				log.debug("Row ignored because it has "
						+ (columns.size() - tuple.getElementCount())
						+ " column values missing.");
				skip_line = true;
			}

			if (!skip_line) {
				tuples.add(tuple);
			}

			line_counter++;
			line = reader.readLine();
		}

		reader.close();

		dataModel.setData(tuples, columns);

		// dataset.setData(data);
		// dataset.setAxisNames(axisNames);
		// dataset.setDataMax(maxData);
		// dataset.setDataMin(minData);
		// dataset.setNumberOfDimensions(data.get(0).getElementCount());

		// return dataset;
	}

	public static void main(String args[]) {
		// File f = new File("resources/data/merged_catalog.xml");
		try {
			DataModel dm = new DataModel();
			File f = new File(
					"/Users/csg/Documents/projects/dtra-diseases/data/SIR1_data.csv");
			Utilities.readCSVSample(f, dm, .01f);
			System.out.println("Column count is " + dm.getColumnCount());
			System.out.println("Tuple count is " + dm.getTupleCount());
			// Utilities.readUSGSQuakeWWW("http://earthquake.usgs.gov/earthquakes/catalogs/eqs1day-M0.txt",
			// dm);
			// Utilities.readXML(f, dm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
