package gov.ornl.eden;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class DataModel {
	private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this
			.getClass());

	private static final int DEFAULT_NUM_HISTOGRAM_BINS = 10;

	protected ArrayList<Tuple> tuples = new ArrayList<Tuple>();
	protected ArrayList<Tuple> queriedTuples = new ArrayList<Tuple>();
	protected ArrayList<Column> columns = new ArrayList<Column>();
	private ArrayList<DataModelListener> listeners = new ArrayList<DataModelListener>();
	private Column highlightedColumn = null;
	private Column regressionYColumn = null;
	protected ArrayList<Column> disabledColumns = new ArrayList<Column>();

	protected OLSMultipleLinearRegression regression;

	public DataModel() {
	}

	public OLSMultipleLinearRegression getOLSMultipleLinearRegression() {
		return regression;
	}

	public Column getOLSMultipleLinearRegressionDependentColumn() {
		return regressionYColumn;
	}

	public void setHighlightedColumn(Column column) {
		if (columns.contains(column)) {
			highlightedColumn = column;
		}
		fireHighlightedColumnChanged();
	}

	public Column getHighlightedColumn() {
		return highlightedColumn;
	}

	public int runMulticollinearityFilter(Column dependentColumn,
			boolean useQueryCorrelations, float significantCorrelationThreshold) {
		if (dependentColumn == null) {
			return -1;
		}

		int dependentColumnIdx = getColumnIndex(dependentColumn);
		if (dependentColumnIdx == -1) {
			return -1;
		}

		ArrayList<ColumnSortRecord> sortedColumnList = new ArrayList<ColumnSortRecord>();
		for (Column column : columns) {
			if (column == dependentColumn) {
				continue;
			}

			if (!column.isEnabled()) {
				continue;
			}

			float corrCoef;
			if (useQueryCorrelations) {
				corrCoef = column.getQueryCorrelationCoefficients().get(
						dependentColumnIdx);
			} else {
				corrCoef = column.getCorrelationCoefficients().get(
						dependentColumnIdx);
			}

			ColumnSortRecord rec = new ColumnSortRecord(column,
					(float) Math.abs(corrCoef));

			sortedColumnList.add(rec);
		}

		Object sortedRecords[] = sortedColumnList.toArray();
		Arrays.sort(sortedRecords);

		ArrayList<Column> removeColumnList = new ArrayList<Column>();

		log.debug("Sorted enabled columns by correlation coefficients with the dependent column");
		for (int i = 0; i < sortedRecords.length; i++) {
			ColumnSortRecord colRecord = (ColumnSortRecord) sortedRecords[i];
			log.debug(i + ": " + colRecord.column.getName() + " - "
					+ colRecord.sortValue);

			if (removeColumnList.contains(colRecord.column)) {
				continue;
			}

			log.debug("Inspecting column '" + colRecord.column.getName());

			for (int j = 0; j < columns.size(); j++) {
				if (j == dependentColumnIdx) {
					continue;
				}
				Column column = columns.get(j);
				if (removeColumnList.contains(column)) {
					continue;
				}
				if (column == colRecord.column) {
					continue;
				}
				if (!column.isEnabled()) {
					continue;
				}

				float corrCoef;
				if (useQueryCorrelations) {
					corrCoef = (float) Math.abs(colRecord.column
							.getQueryCorrelationCoefficients().get(j));
				} else {
					corrCoef = (float) Math.abs(colRecord.column
							.getCorrelationCoefficients().get(j));
				}

				if (corrCoef > significantCorrelationThreshold) {
					log.debug("Removed column '" + column.getName() + "'"
							+ "corrCoef=" + corrCoef);
					removeColumnList.add(column);
				}
			}
		}

		disableColumns(removeColumnList);

		return removeColumnList.size();
	}

	public void setData(ArrayList<Tuple> tuples, ArrayList<Column> columns) {
		if (columns.isEmpty()) {
			return;
		}

		highlightedColumn = null;
		this.tuples.clear();
		this.tuples.addAll(tuples);
		this.columns.clear();
		this.columns.addAll(columns);
		this.disabledColumns.clear();
		this.queriedTuples.clear();
		this.queriedTuples.addAll(tuples);
		this.regression = null;
		this.regressionYColumn = null;
		this.highlightedColumn = null;

		calculateStatistics();
		fireDataModelChanged();
	}

	public void setColumns(ArrayList<Column> columns) {
		highlightedColumn = null;
		this.columns.clear();
		this.columns.addAll(columns);
		this.tuples.clear();
		fireDataModelChanged();
	}

	public void addTuples(ArrayList<Tuple> newTuples) {
		this.tuples.addAll(newTuples);
		calculateStatistics();
		fireTuplesAdded(newTuples);
	}

	public void clear() {
		this.tuples.clear();
		this.queriedTuples.clear();
		this.columns.clear();
		this.disabledColumns.clear();
		this.regression = null;
		this.regressionYColumn = null;
		this.highlightedColumn = null;
		fireDataModelChanged();
	}

	public void setColumnName(Column column, String name) {
		if (columns.contains(column)) {
			column.setName(name);
			fireDataModelChanged();
		}
		// } else if (disabledColumns.contains(column)) {
		// column.setName(name);
		// fireDataModelChanged();
		// }
	}

	private void calculateQueryStatistics() {
		double[][] data = new double[columns.size()][];

		for (int icolumn = 0; icolumn < columns.size(); icolumn++) {
			Column column = columns.get(icolumn);
			data[icolumn] = getColumnQueriedValues(icolumn);
			DescriptiveStatistics stats = new DescriptiveStatistics(
					data[icolumn]);

			column.setQueryMean((float) stats.getMean());
			column.setQueryMedian((float) stats.getPercentile(50));
			column.setQueryVariance((float) stats.getVariance());
			column.setQueryStandardDeviation((float) stats
					.getStandardDeviation());
			column.setQueryQ1((float) stats.getPercentile(25));
			column.setQueryQ3((float) stats.getPercentile(75));
			column.setQuerySkewness((float) stats.getSkewness());
			column.setQueryKurtosis((float) stats.getKurtosis());
			column.setQueryMaxValue((float) stats.getMax());
			column.setQueryMinValue((float) stats.getMin());

			// calculate whiskers for box plot 1.5 of IQR
			float iqr_range = 1.5f * column.getQueryIQR();
			float lowerFence = column.getQueryQ1() - iqr_range;
			float upperFence = column.getQueryQ3() + iqr_range;
			double sorted_data[] = Arrays.copyOf(data[icolumn],
					data[icolumn].length);
			Arrays.sort(sorted_data);

			// find upper datum that is not greater than upper fence
			if (upperFence >= column.getMaxQueryValue()) {
				column.setQueryUpperWhisker(column.getMaxQueryValue());
			} else {
				// find largest datum not larger than upper fence value
				for (int i = sorted_data.length - 1; i >= 0; i--) {
					if (sorted_data[i] <= upperFence) {
						column.setQueryUpperWhisker((float) sorted_data[i]);
						break;
					}
				}
			}

			if (lowerFence <= column.getMinQueryValue()) {
				column.setQueryLowerWhisker(column.getMinQueryValue());
			} else {
				// find smallest datum not less than lower fence value
				for (int i = 0; i < sorted_data.length; i++) {
					if (sorted_data[i] >= lowerFence) {
						column.setQueryLowerWhisker((float) sorted_data[i]);
						break;
					}
				}
			}

			// calculate frequency information for column
			Histogram histogram = new Histogram(column.getName(),
					DEFAULT_NUM_HISTOGRAM_BINS, column.getMinValue(),
					column.getMaxValue());
			column.setQueryHistogram(histogram);
			for (double value : data[icolumn]) {
				histogram.fill(value);
			}
		}

		PearsonsCorrelation pCorr = new PearsonsCorrelation();

		for (int ix = 0; ix < columns.size(); ix++) {
			Column column = columns.get(ix);
			ArrayList<Float> coefList = new ArrayList<Float>();

			for (int iy = 0; iy < columns.size(); iy++) {
				try {
					double coef = pCorr.correlation(data[ix], data[iy]);
					coefList.add((float) coef);
				} catch (Exception ex) {
					coefList.add(0.f);
				}
			}
			column.setQueryCorrelationCoefficients(coefList);

			// for (Float val : coefList) {
			// System.out.print(val + " ");
			// }
			// System.out.print("\n");
		}
	}

	public ArrayList<Column> getColumns() {
		return columns;
	}

	public ArrayList<Tuple> getTuples() {
		return tuples;
	}

	private void calculateStatistics() {
		double[][] data = new double[columns.size()][];

		for (int icolumn = 0; icolumn < columns.size(); icolumn++) {
			Column column = columns.get(icolumn);

			data[icolumn] = getColumnValues(icolumn);

			// calculate descriptive statistics
			DescriptiveStatistics stats = new DescriptiveStatistics(
					data[icolumn]);

			column.setMean((float) stats.getMean());
			column.setMedian((float) stats.getPercentile(50));
			column.setVariance((float) stats.getVariance());
			column.setStandardDeviation((float) stats.getStandardDeviation());
			column.setMaxValue((float) stats.getMax());
			column.setMinValue((float) stats.getMin());
			column.setMaxQueryValue(column.getMaxValue());
			column.setMinQueryValue(column.getMinValue());
			column.setQ1((float) stats.getPercentile(25));
			column.setQ3((float) stats.getPercentile(75));
			column.setSkewness((float) stats.getSkewness());
			column.setKurtosis((float) stats.getKurtosis());

			// calculate whiskers for box plot 1.5 of IQR
			float iqr_range = 1.5f * column.getIQR();
			float lowerFence = column.getQ1() - iqr_range;
			float upperFence = column.getQ3() + iqr_range;
			double sorted_data[] = Arrays.copyOf(data[icolumn],
					data[icolumn].length);
			Arrays.sort(sorted_data);

			// find upper datum that is not greater than upper fence
			if (upperFence >= column.getMaxValue()) {
				column.setUpperWhisker(column.getMaxValue());
			} else {
				// find largest datum not larger than upper fence value
				for (int i = sorted_data.length - 1; i >= 0; i--) {
					if (sorted_data[i] <= upperFence) {
						column.setUpperWhisker((float) sorted_data[i]);
						break;
					}
				}
			}

			if (lowerFence <= column.getMinValue()) {
				column.setLowerWhisker(column.getMinValue());
			} else {
				// find smallest datum not less than lower fence value
				for (int i = 0; i < sorted_data.length; i++) {
					if (sorted_data[i] >= lowerFence) {
						column.setLowerWhisker((float) sorted_data[i]);
						break;
					}
				}
			}

			// calculate frequency information for column
			Histogram histogram = new Histogram(column.getName(),
					DEFAULT_NUM_HISTOGRAM_BINS, column.getMinValue(),
					column.getMaxValue());
			column.setHistogram(histogram);
			for (double value : data[icolumn]) {
				histogram.fill(value);
			}
			// histogram.show();
		}

		PearsonsCorrelation pCorr = new PearsonsCorrelation();

		for (int ix = 0; ix < columns.size(); ix++) {
			Column column = columns.get(ix);
			ArrayList<Float> coefList = new ArrayList<Float>();

			for (int iy = 0; iy < columns.size(); iy++) {
				double coef = pCorr.correlation(data[ix], data[iy]);
				coefList.add((float) coef);
			}
			column.setCorrelationCoefficients(coefList);

			// for (Float val : coefList) {
			// System.out.print(val + " ");
			// }
			// System.out.print("\n");
		}
	}

	public OLSMultipleLinearRegression calculateOLSMultipleLinearRegression(
			Column yColumn) {
		regression = new OLSMultipleLinearRegression();
		regressionYColumn = yColumn;

		int yItemIndex = getColumnIndex(highlightedColumn);

		double[] y = new double[getTupleCount()];
		double[][] x = new double[getTupleCount()][getColumnCount() - 1];

		for (int i = 0; i < tuples.size(); i++) {
			Tuple tuple = tuples.get(i);
			y[i] = tuple.getElement(yItemIndex);

			for (int j = 0, k = 0; j < getColumnCount(); j++) {
				if (j == yItemIndex) {
					continue;
				}
				x[i][k++] = tuple.getElement(j);
			}
		}

		regression.newSampleData(y, x);

		log.debug("Regression results:");
		log.debug("rSquared: " + regression.calculateRSquared()
				+ " rSquaredAdj: " + regression.calculateAdjustedRSquared());
		double[] beta = regression.estimateRegressionParameters();
		for (int i = 0; i < beta.length; i++) {
			log.debug("b[" + i + "]: " + beta[i]);
		}

		fireDataModelChanged();
		return regression;
	}

	public double[] getColumnValues(int columnIndex) {
		Column column = columns.get(columnIndex);

		double[] values = new double[tuples.size()];

		for (int ituple = 0; ituple < tuples.size(); ituple++) {
			Tuple tuple = tuples.get(ituple);
			values[ituple] = tuple.getElement(columnIndex);
		}

		return values;
	}

	public double[] getColumnQueriedValues(int columnIndex) {
		Column column = columns.get(columnIndex);

		double[] values = new double[queriedTuples.size()];

		for (int ituple = 0; ituple < queriedTuples.size(); ituple++) {
			Tuple tuple = queriedTuples.get(ituple);
			values[ituple] = tuple.getElement(columnIndex);
		}

		return values;
	}

	public void addDataModelListener(DataModelListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public Tuple getTuple(int idx) {
		return tuples.get(idx);
	}

	public Column getColumn(int idx) {
		return columns.get(idx);
	}

	// public Column getDisabledColumn(int idx) {
	// return disabledColumns.get(idx);
	// }
	//
	public Column getColumn(String columnName) {
		for (Column column : columns) {
			if (column.getName().equals(columnName)) {
				return column;
			}
		}
		return null;
	}

	// public Column getDisabledColumn(String columnName) {
	// for (Column column : disabledColumns) {
	// if (column.getName().equals(columnName)) {
	// return column;
	// }
	// }
	// return null;
	// }
	//
	public int getColumnIndex(Column column) {
		return columns.indexOf(column);
	}

	// public int getDisabledColumnIndex(Column column) {
	// return disabledColumns.indexOf(column);
	// }
	//
	public int getTupleCount() {
		return tuples.size();
	}

	public int getColumnCount() {
		return columns.size();
	}

	public int getEnabledColumnCount() {
		return columns.size() - disabledColumns.size();
	}

	public void disableColumn(Column column) {
		// int colIndex = columns.indexOf(column);
		//
		// for (int i = 0; i < tuples.size(); i++) {
		// Tuple tuple = tuples.get(i);
		// float elementValue = tuple.getElement(colIndex);
		// tuple.removeElement(colIndex);
		//
		// if (disabledColumnTuples.size() != tuples.size()) {
		// Tuple disabledTuple = new Tuple();
		// disabledTuple.addElement(elementValue);
		// disabledColumnTuples.add(disabledTuple);
		// } else {
		// Tuple disabledTuple = disabledColumnTuples.get(i);
		// disabledTuple.addElement(elementValue);
		// }
		// }
		//
		// column.setEnabled(false);
		// columns.remove(column);
		// disabledColumns.add(column);
		//
		// if (column == highlightedColumn) {
		// highlightedColumn = null;
		// }
		if (!disabledColumns.contains(column)) {
			column.setEnabled(false);
			column.setQueryFlag(false);
			column.setMaxQueryValue(column.getMaxValue());
			column.setMinQueryValue(column.getMinValue());
			if (column == this.highlightedColumn) {
				highlightedColumn = null;
			}
			disabledColumns.add(column);
			fireColumnDisabled(column);
		}
	}

	public void disableColumns(ArrayList<Column> columns) {
		for (Column column : columns) {
			if (!disabledColumns.contains(column)) {
				column.setEnabled(false);
				column.setQueryFlag(false);
				column.setMaxQueryValue(column.getMaxValue());
				column.setMinQueryValue(column.getMinValue());
				if (column == this.highlightedColumn) {
					highlightedColumn = null;
				}
				disabledColumns.add(column);
			}
		}

		fireColumnsDisabled(columns);
	}

	public void enableColumn(Column column) {
		// int colIndex = disabledColumns.indexOf(column);
		// if (colIndex != -1) {
		// for (int i = 0; i < disabledColumnTuples.size(); i++) {
		// Tuple disabledTuple = disabledColumnTuples.get(i);
		// float elementValue = disabledTuple.getElement(colIndex);
		// disabledTuple.removeElement(colIndex);
		//
		// if (disabledColumnTuples.size() != tuples.size()) {
		// Tuple tuple = new Tuple();
		// tuple.addElement(elementValue);
		// tuples.add(tuple);
		// } else {
		// Tuple tuple = tuples.get(i);
		// tuple.addElement(elementValue);
		// }
		// }
		//
		// column.setEnabled(true);
		// disabledColumns.remove(column);
		// columns.add(column);
		// fireDataModelChanged();
		// }
		if (disabledColumns.contains(column)) {
			disabledColumns.remove(column);
			column.setEnabled(true);
			// fireDataModelChanged();
			fireColumnEnabled(column);
		}
	}

	public int getDisabledColumnCount() {
		return disabledColumns.size();
	}

	public ArrayList<Column> getDisabledColumns() {
		return disabledColumns;
	}

	public int removeUnselectedTuples() {
		ArrayList<Tuple> tuplesRemoved = new ArrayList<Tuple>();

		for (Tuple tuple : tuples) {
			if (!tuple.getQueryFlag()) {
				tuplesRemoved.add(tuple);
			}
		}

		if (!tuplesRemoved.isEmpty()) {
			tuples.removeAll(tuplesRemoved);
			clearAllColumnQueries();
			calculateStatistics();
			fireDataModelChanged();
		}

		return tuplesRemoved.size();
	}

	public int removeSelectedTuples() {
		ArrayList<Tuple> tuplesRemoved = new ArrayList<Tuple>();

		for (Tuple tuple : tuples) {
			if (tuple.getQueryFlag()) {
				tuplesRemoved.add(tuple);
			}
		}

		if (!tuplesRemoved.isEmpty()) {
			tuples.removeAll(tuplesRemoved);
			clearAllColumnQueries();
			calculateStatistics();
			fireDataModelChanged();
		}

		return tuplesRemoved.size();
	}

	private void clearAllColumnQueries() {
		// boolean fireQueryChangedFlag = false;
		for (Column column : columns) {
			// if (column.isQuerySet()) {
			column.setQueryFlag(false);
			// fireQueryChangedFlag = true;
			// }
		}

		// if (fireQueryChangedFlag) {
		// fireQueryChanged();
		// }
	}

	/*
	 * public void moveElements(int currentElementIndex, int newElementIndex) {
	 * // dataset.moveElements(currentElementIndex, newElementIndex); if
	 * (currentElementIndex == newElementIndex) { return; }
	 * 
	 * Column tmpColumn = columns.get(currentElementIndex); if
	 * (currentElementIndex < newElementIndex) { for (int i =
	 * currentElementIndex; i < newElementIndex; i++) { columns.set(i,
	 * columns.get(i+1)); } } else { for (int i = currentElementIndex; i >
	 * newElementIndex; i--) { columns.set(i, columns.get(i-1)); } }
	 * columns.set(newElementIndex, tmpColumn);
	 * 
	 * // swap all tuple elements for (Tuple tuple : tuples) {
	 * tuple.moveElement(currentElementIndex, newElementIndex); }
	 * 
	 * calculateStatistics();
	 * 
	 * fireDataModelChanged(); }
	 */

	private void fireColumnDisabled(Column column) {
		for (DataModelListener listener : listeners) {
			listener.columnDisabled(this, column);
		}
	}

	private void fireColumnsDisabled(ArrayList<Column> disabledColumns) {
		for (DataModelListener listener : listeners) {
			listener.columnsDisabled(this, disabledColumns);
		}
	}

	private void fireColumnEnabled(Column column) {
		for (DataModelListener listener : listeners) {
			listener.columnEnabled(this, column);
		}
	}

	private void fireDataModelChanged() {
		for (DataModelListener listener : listeners) {
			listener.dataModelChanged(this);
		}
	}

	private void fireTuplesAdded(ArrayList<Tuple> newTuples) {
		for (DataModelListener listener : listeners) {
			listener.tuplesAdded(this, newTuples);
		}
	}

	public void fireHighlightedColumnChanged() {
		for (DataModelListener listener : listeners) {
			listener.highlightedColumnChanged(this);
		}
	}

	public void fireQueryChanged() {
		for (DataModelListener listener : listeners) {
			listener.queryChanged(this);
		}
	}

	public int getQueriedTupleCount() {
		return queriedTuples.size();
	}

	public ArrayList<Tuple> getQueriedTuples() {
		return queriedTuples;
	}

	public boolean isColumnQuerySet() {
		for (Column column : columns) {
			if (column.isEnabled() && column.isQuerySet()) {
				return true;
			}
		}
		return false;
	}

	public void setQueriedTuples() {
		log.debug("setting queried tuples");
		queriedTuples.clear();

		if (getTupleCount() == 0) {
			return;
		}

		boolean querySet = true;
		// for (Column column : columns) {
		// if (column.isQuerySet()) {
		// querySet = true;
		// }
		// }

		if (querySet) {
			for (int ituple = 0; ituple < getTupleCount(); ituple++) {
				Tuple currentTuple = getTuple(ituple);
				currentTuple.setQueryFlag(true);

				for (int icolumn = 0; icolumn < columns.size(); icolumn++) {
					Column column = columns.get(icolumn);
					if (column.isQuerySet()) {
						if ((currentTuple.getElement(icolumn) > column
								.getMaxQueryValue())
								|| (currentTuple.getElement(icolumn) < column
										.getMinQueryValue())) {
							currentTuple.setQueryFlag(false);
							break;
						}
					}
				}

				if (currentTuple.getQueryFlag()) {
					queriedTuples.add(currentTuple);
				}
			}
			calculateQueryStatistics();
			fireQueryChanged();
		}
		log.debug("Finished setting queried tuples");
	}
}
