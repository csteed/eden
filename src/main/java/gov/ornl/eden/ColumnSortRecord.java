package gov.ornl.eden;

public class ColumnSortRecord implements Comparable<ColumnSortRecord> {
	public Column column;
	public float sortValue;

	public ColumnSortRecord(Column column, float sortValue) {
		this.column = column;
		this.sortValue = sortValue;
	}

	public int compareTo(ColumnSortRecord that) {
		if (this.sortValue == that.sortValue) {
			return 0;
		} else if (this.sortValue < that.sortValue) {
			return 1;
		}
		return -1;
	}
}
