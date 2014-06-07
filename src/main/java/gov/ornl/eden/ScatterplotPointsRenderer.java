package gov.ornl.eden;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScatterplotPointsRenderer extends Renderer {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	protected Column xColumn;
	protected Column yColumn;
	protected int xColumnIndex;
	protected int yColumnIndex;
	protected int size;
	protected DataModel dataModel;
	protected ScatterplotConfiguration config;
	protected Graphics2D g2;
	protected int left, right, bottom, top;
	protected Rectangle correlationRect;
	protected Color correlationColor;
	protected SimpleRegression simpleRegression;
	protected boolean showFocusPoints;
	protected boolean showContextPoints;
	protected boolean antialias;

	public ScatterplotPointsRenderer(DataModel dataModel, Column xColumn,
			Column yColumn, int plotSize, int axisSize,
			ScatterplotConfiguration config, SimpleRegression simpleRegression,
			boolean showFocusPoints, boolean showContextPoints,
			boolean antialias) {
		this.antialias = antialias;
		this.showFocusPoints = showFocusPoints;
		this.showContextPoints = showContextPoints;
		this.config = config;
		this.xColumn = xColumn;
		this.yColumn = yColumn;
		this.size = plotSize - axisSize - 4;
		this.dataModel = dataModel;
		this.simpleRegression = simpleRegression;

		xColumnIndex = dataModel.getColumnIndex(xColumn);
		yColumnIndex = dataModel.getColumnIndex(yColumn);

		// setup image
		image = new BufferedImage(size + 4, size + 4,
				BufferedImage.TYPE_INT_ARGB);
		g2 = (Graphics2D) image.getGraphics();
		if (antialias) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		g2.setFont(config.labelFont);

		// calculate lower left point
		left = 2;
		// bottom = (plotSize-axisSize)-1;
		// bottom = plotSize - axisSize;
		bottom = size - 2;

		// calculate upper right point
		// right = (plotSize-axisSize)-1;
		// right = plotSize - axisSize - 1;
		right = size - 2;
		top = 2;
	}

	public Column getXColumn() {
		return xColumn;
	}

	public Column getYColumn() {
		return yColumn;
	}

	public int getSize() {
		return size;
	}

	public DataModel getDataModel() {
		return dataModel;
	}

	public ScatterplotConfiguration getConfig() {
		return config;
	}

	public void run() {
		isRunning = true;
		// log.debug("Renderer thread " + this.getId() + " running...");

		// Draw a center line for x and y (diagnostic)
		// g2.setColor(Color.blue);
		// g2.drawLine(left, size/2, right, size/2);
		// g2.drawLine(size/2, top, size/2, bottom);
		//

		g2.setStroke(new BasicStroke(2.f));
		g2.setColor(config.pointColor);
		// log.debug(config.pointColor.getRed() + ", " +
		// config.pointColor.getGreen() + ", "
		// + config.pointColor.getBlue() + ", " + config.pointColor.getAlpha());

		// draw points
		for (int ituple = 0; ituple < dataModel.getTupleCount(); ituple++) {
			if (isRunning == false) {
				return;
			}
			Tuple currentTuple = dataModel.getTuple(ituple);

			if ((currentTuple.getQueryFlag() && !showFocusPoints)
					|| (!currentTuple.getQueryFlag() && !showContextPoints)) {
				continue;
			}

			float xValue;
			try {
				xValue = currentTuple.getElement(xColumnIndex);
			} catch (Exception ex) {
				ex.printStackTrace();
				continue;
			}
			// double normValue = (xValue - xColumn.getMinValue()) /
			// (xColumn.getMaxValue() - xColumn.getMinValue());
			// int x = x0 + (int)(normValue * plotAreaSize);
			int x = toScreenX(xValue, xColumn.getMinValue(),
					xColumn.getMaxValue(), left, size);
			// int x = toScreenX2(xValue, xColumn.getMinValue(),
			// xColumn.getMaxValue(), size);
			float yValue = currentTuple.getElement(yColumnIndex);
			// normValue = (yValue - yColumn.getMinValue()) /
			// (yColumn.getMaxValue() - yColumn.getMinValue());
			// int y = y0 - (int)(normValue * plotAreaSize);
			int y = toScreenY(yValue, yColumn.getMinValue(),
					yColumn.getMaxValue(), top, size);
			// int y = toScreenY2(yValue, yColumn.getMinValue(),
			// yColumn.getMaxValue(), size);

			// log.debug("x="+x +" y="+y+" x>size="+ (x>size) + " y<0" + (y<0));

			// if (y<0) {
			// log.debug("x="+x +" y="+y+" size="+size+
			// " left="+left+" bottom="+bottom+" right="+right+" top="+top);
			// int test = toScreenY2(yValue, yColumn.getMinValue(),
			// yColumn.getMaxValue(), size);
			// }

			int offsetX = x
					- (int) (config.pointShape.getBounds2D().getWidth() / 2.);
			int offsetY = y
					- (int) (config.pointShape.getBounds2D().getWidth() / 2.);

			// log.debug("x=" + offsetX + " y="+offsetY);
			g2.translate(offsetX, offsetY);
			g2.draw(config.pointShape);
			g2.translate(-offsetX, -offsetY);
		}

		// draw regression line (trendline)
		if (config.showRegressionLine && simpleRegression != null) {
			if (isRunning == false) {
				return;
			}

			double startX = xColumn.getMinValue();
			double startY = simpleRegression.predict(startX);
			double endX = xColumn.getMaxValue();
			double endY = simpleRegression.predict(endX);

			if (endY < yColumn.getMinValue()) {
				double test = -(simpleRegression.getIntercept() / simpleRegression
						.getSlope());
			}

			int start_ix = toScreenX((float) startX, xColumn.getMinValue(),
					xColumn.getMaxValue(), left, size);
			int start_iy = toScreenY((float) startY, yColumn.getMinValue(),
					yColumn.getMaxValue(), top, size);
			int end_ix = toScreenX((float) endX, xColumn.getMinValue(),
					xColumn.getMaxValue(), left, size);
			int end_iy = toScreenY((float) endY, yColumn.getMinValue(),
					yColumn.getMaxValue(), top, size);

			g2.setColor(Color.black);
			g2.drawLine(start_ix, start_iy, end_ix, end_iy);

			// g2.drawLine(start_ix, start_iy, start_ix, start_iy);
			// g2.drawLine(start_ix, start_iy, start_ix, start_iy);
			// log.debug("Drawing line from ("+start_ix+","+start_iy+") to ("+end_ix+","+end_iy+")");
			// log.debug("Scatterplot x0="+x0+" y0="+y0+
			// " x1="+(x0+plotAreaSize)+" y1="+(y0+plotAreaSize));

		}

		isRunning = false;
		fireRendererFinished();
	}

	private int toScreenY2(float value, float minValue, float maxValue,
			int plotHeight) {
		float normVal = 1.f - ((value - minValue) / (maxValue - minValue));
		int y = (int) Math.round(normVal * plotHeight);
		return y;
	}

	private int toScreenX2(float value, float minValue, float maxValue,
			int plotWidth) {
		float normVal = (value - minValue) / (maxValue - minValue);
		int x = (int) (normVal * plotWidth);
		return x;
	}

	private int toScreenY(float value, float minValue, float maxValue,
			int offset, int plotHeight) {
		// float normVal = (value - minValue) / (maxValue - minValue);
		// int y = offset - (int)(normVal * plotHeight);
		float normVal = 1.f - ((value - minValue) / (maxValue - minValue));
		int y = offset + (int) (normVal * plotHeight);
		return y;
	}

	private int toScreenX(float value, float minValue, float maxValue,
			int offset, int plotWidth) {
		float normVal = (value - minValue) / (maxValue - minValue);
		int x = offset + (int) Math.round(normVal * plotWidth);
		return x;
	}

	/*
	 * public void run() { isRunning = true;
	 * 
	 * int xColumnIndex = dataModel.getColumnIndex(xColumn); int yColumnIndex =
	 * dataModel.getColumnIndex(yColumn);
	 * 
	 * image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	 * Graphics2D g2 = (Graphics2D) image.getGraphics();
	 * g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	 * RenderingHints.VALUE_ANTIALIAS_ON); //
	 * g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	 * RenderingHints.VALUE_TEXT_ANTIALIAS_ON); g2.setFont(AXIS_LABEL_FONT); //
	 * g2.setColor(Color.DARK_GRAY);
	 * 
	 * // Adjust lower left point by axes gutters, border, and shape size int x0
	 * = BUFFER_SIZE + g2.getFontMetrics().getHeight() +
	 * (int)(config.pointShape.getBounds2D().getWidth()/2); int y0 = size -
	 * BUFFER_SIZE - g2.getFontMetrics().getHeight() -
	 * (int)(config.pointShape.getBounds2D().getHeight()/2); // int x1 = size -
	 * BUFFER_SIZE; // int y1 = BUFFER_SIZE; // int maxShapeDimension =
	 * (int)Math.max(config.pointShape.getBounds2D().getWidth(),
	 * config.pointShape.getBounds2D().getHeight()); int actualPlotSize = size -
	 * ((BUFFER_SIZE*2) + g2.getFontMetrics().getHeight() + maxShapeDimension);
	 * 
	 * // g2.drawLine(x0, y0, x0, y1); // g2.drawLine(x0, y0, x1, y0);
	 * 
	 * // Arrow Drawing Code Below // Polygon p = new Polygon(); //
	 * p.addPoint(x1, y0); // p.addPoint(x1-ARROW_HEIGHT-1, y0-ARROW_WIDTH/2);
	 * // p.addPoint(x1-ARROW_HEIGHT-1, y0+ARROW_WIDTH/2); // g2.fill(p); //
	 * g2.draw(p); // // p = new Polygon(); // p.addPoint(x0, y1); //
	 * p.addPoint(x0-ARROW_WIDTH/2, y1+ARROW_HEIGHT+1); //
	 * p.addPoint(x0+ARROW_WIDTH/2, y1+ARROW_HEIGHT+1); // g2.fill(p); //
	 * g2.draw(p);
	 * 
	 * // Axis Label Drawing // String yAxisString = yColumn.getName(); //
	 * String xAxisString = xColumn.getName(); // int stringWidth =
	 * g2.getFontMetrics().stringWidth(yAxisString); // int yAxisCenter = y1 +
	 * ((y0 - y1) / 2); // int xAxisCenter = x0 + ((x1 - x0) / 2); //
	 * g2.rotate(-NINETY_DEGREES, x0-(g2.getFontMetrics().getHeight()/2),
	 * yAxisCenter+(stringWidth/2)); // g2.drawString(yAxisString,
	 * x0-(g2.getFontMetrics().getHeight()/2), yAxisCenter+(stringWidth/2)); //
	 * g2.rotate(NINETY_DEGREES, x0-(g2.getFontMetrics().getHeight()/2),
	 * yAxisCenter+(stringWidth/2)); // stringWidth =
	 * g2.getFontMetrics().stringWidth(xAxisString); //
	 * g2.drawString(xAxisString, xAxisCenter-(stringWidth/2), y0 +
	 * g2.getFontMetrics().getHeight()); // g2.setStroke(new BasicStroke(2.f));
	 * // draw points for (int ituple = 0; ituple < dataModel.getTupleCount();
	 * ituple++) { Tuple currentTuple = dataModel.getTuple(ituple);
	 * 
	 * if (!currentTuple.getQueryFlag()) { continue; }
	 * 
	 * float xValue = currentTuple.getElement(xColumnIndex); double normValue =
	 * (xValue - xColumn.getMinValue()) / (xColumn.getMaxValue() -
	 * xColumn.getMinValue()); int x = x0 + (int)(normValue * actualPlotSize);
	 * float yValue = currentTuple.getElement(yColumnIndex); normValue = (yValue
	 * - yColumn.getMinValue()) / (yColumn.getMaxValue() -
	 * yColumn.getMinValue()); int y = y0 - (int)(normValue * actualPlotSize);
	 * 
	 * g2.setColor(FocusLineRenderer.LINE_COLOR);
	 * 
	 * int offsetX = x - (int)(config.pointShape.getBounds2D().getWidth() / 2.);
	 * int offsetY = y - (int)(config.pointShape.getBounds2D().getWidth() / 2.);
	 * 
	 * g2.translate(offsetX, offsetY); g2.draw(config.pointShape);
	 * g2.translate(-offsetX, -offsetY); }
	 * 
	 * isRunning = false; fireRendererFinished(); }
	 */
}
