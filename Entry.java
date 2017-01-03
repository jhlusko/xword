package dictionary;

import java.awt.Point;

public class Entry {
	
	Clue wordGroup;
	Point point; 
	boolean isHorizontal;
	int offset;
	Point origin;

	public Entry(Clue wordGroup, Point point, int offset, boolean isHorizontal){
		this.wordGroup = wordGroup;
		this.point = point;
		this.offset = offset;
		this.isHorizontal = isHorizontal;
		this.origin = getOrigin(point, offset, isHorizontal);
		
	}

	private Point getOrigin(Point point, int offset, boolean isHorizontal) {
		Point origin = new Point(point.x - offset*((isHorizontal)?1:0), point.y - offset*((isHorizontal)?0:1));
		return origin;
		
	}
	
	

}
